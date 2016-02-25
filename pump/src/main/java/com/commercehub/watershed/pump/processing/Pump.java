package com.commercehub.watershed.pump.processing;

import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.commercehub.watershed.pump.model.DrillResultRow;
import com.commercehub.watershed.pump.model.PumpRecord;
import com.commercehub.watershed.pump.model.PumpRecordResult;
import com.commercehub.watershed.pump.model.PumpSettings;
import com.commercehub.watershed.pump.repositories.DrillRepository;
import com.commercehub.watershed.pump.service.KinesisService;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observable.ListenableFutureObservable;
import rx.schedulers.Schedulers;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Take from the Drill, give to the Kinesis.
 */
public class Pump {
    private static final Logger log = LoggerFactory.getLogger(Pump.class);

    //TODO produce metrics

    private Connection connection;
    private int shardCount;
    private Function<byte[], byte[]> recordTransformer;
    private KinesisProducer kinesisProducer;
    private PumpSettings pumpSettings;
    private int maxRecordsPerShardPerSecond; //Kinesis service limit, at least prior to aggregation
    private int producerRateLimit;

    /**
     *
     * @param connection                    Database connection.
     * @param kinesisProducer               Produces records for Kinesis.
     * @param kinesisService                Communicates with Kinesis to retrieve information about Kinesis streams.
     * @param maxRecordsPerShardPerSecond   The maximum number of records per shard per second that Pump is allowed to handle.
     * @param pumpSettings                  The settings that determine where Pump will look for records and where to send them.
     * @param recordTransformer             A {@code Function} that will transform records on the byte level.
     */
    @Inject
    public Pump(
            Connection connection,
            KinesisProducer kinesisProducer,
            KinesisService kinesisService,
            @Named("maxRecordsPerShardPerSecond") int maxRecordsPerShardPerSecond,
            @Named("producerRateLimit") int producerRateLimit,
            @Assisted PumpSettings pumpSettings,
            @Assisted Function<byte[], byte[]> recordTransformer) {

        this.connection = connection;
        this.kinesisProducer = kinesisProducer;
        this.maxRecordsPerShardPerSecond = maxRecordsPerShardPerSecond;
        this.producerRateLimit = producerRateLimit;
        this.pumpSettings = pumpSettings;
        this.recordTransformer = recordTransformer;
        this.shardCount = kinesisService.countShardsInStream(pumpSettings.getStreamOut());
    }

    /**
     * Defines an Observable pipeline to issue a query, transform records, and publish records to Kinesis. Does not
     * actually start pumping until the caller subscribes. The caller should monitor the Subscription for
     * non-recoverable errors by implementing {@code onError}, as well as checking every result for errors if they are
     * to be reported. To cancel pumping, unsubscribe.
     */
    public Observable<PumpRecordResult> build() {

        // Can't actually use rxjava-jdbc with Drill at the moment: Drill's JDBC client is broken wrt PreparedStatements (DRILL-3566)
        // Also not sure whether rxjava-jdbc supports backpressure.
        /*
        Observable<Record> dbRecords = database.select(sql).get(new ResultSetMapper<Record>() {
            @Override
            public Record call(ResultSet resultSet) throws SQLException {
                return new Record().withPartitionKey(resultSet.getString("partitionKey")).
                        withData(ByteBuffer.wrap(resultSet.getBytes("data")));
            }
        });
        */

        if(this.pumpSettings == null){
            throw new InvalidParameterException("PumpSettings were not provided.");
        }


        Observable<PumpRecord> dbRecords = Observable.create(new Observable.OnSubscribe<PumpRecord>() {
            @Override
            public void call(final Subscriber<? super PumpRecord> subscriber) {
                subscriber.onStart();
                final ResultSet resultSet = executeQuery(subscriber, connection);

                JdbcRecordProducer jdbcRecordProducer = new JdbcRecordProducer(subscriber, resultSet, connection, pumpSettings);
                subscriber.setProducer(jdbcRecordProducer);

                jdbcRecordProducer.request(shardCount * maxRecordsPerShardPerSecond * producerRateLimit / 200);
            }

            private ResultSet executeQuery(Subscriber<?> subscriber, Connection connection) {
                ResultSet resultSet;
                try {
                    log.info("Executing JDBC query {}", pumpSettings.getQueryIn());
                    resultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(pumpSettings.getQueryIn());
                    log.info("Got a JDBC ResultSet, streaming results.");
                    resultSet.setFetchSize(Integer.MIN_VALUE);
                }
                catch (Exception e) {
                    resultSet = null;
                    subscriber.onError(e);
                }

                return resultSet;
            }
        }).subscribeOn(Schedulers.io());

        Observable<PumpRecord> transformedRecords;
        if (recordTransformer != null) {
            transformedRecords = dbRecords.map(new Func1<PumpRecord, PumpRecord>() {
                @Override
                public PumpRecord call(PumpRecord pumpRecord) {
                    log.trace("Transforming record");
                    byte[] transformedData = recordTransformer.apply(pumpRecord.getKinesisRecord().getData().array());

                    Record transformedKinesisRecord = pumpRecord.getKinesisRecord()
                            .clone()
                            .withData(ByteBuffer.wrap(transformedData));

                    return new PumpRecord(transformedKinesisRecord, pumpRecord.getDrillResultRow());
                }
            });
        }
        else {
            transformedRecords = dbRecords;
        }

        Observable<PumpRecordResult> pubResults = transformedRecords.flatMap(
                new Func1<PumpRecord, Observable<PumpRecordResult>>() {
                    @Override
                    public Observable<PumpRecordResult> call(PumpRecord pumpRecord) {
                        log.debug("Adding record to Kinesis Producer");

                        Record kinesisRecord = pumpRecord.getKinesisRecord();

                        return ListenableFutureObservable.from(
                                combine(kinesisProducer.addUserRecord(pumpSettings.getStreamOut(), kinesisRecord.getPartitionKey(), kinesisRecord.getData()), pumpRecord.getDrillResultRow()),
                                Schedulers.io());
                    }
                });

        return pubResults;
    }

    /**
     * destroys the kinesis producer, stops emission
     */
    void destroy() {
        if (kinesisProducer != null) {
            kinesisProducer.destroy();
            kinesisProducer = null;
        }
    }

    /**
     * Instructs the kinesisProducer to flush all records and waits until all
     * records are complete (either succeeding or failing).
     */
    void flushSync() {
        log.info("Attempting sync flush of about {} records", kinesisProducer.getOutstandingRecordsCount());
        kinesisProducer.flushSync();
    }

    /**
     *
     * @return outstanding record count that hasn't been emitted yet
     */
    long countPending() {
        return kinesisProducer.getOutstandingRecordsCount();
    }

    //Combine Future<UserRecordResult> and Record
    private ListenableFuture<PumpRecordResult> combine(ListenableFuture<UserRecordResult> futureUserRecordResult, final DrillResultRow drillResultRow) {

        return Futures.transform(futureUserRecordResult, new AsyncFunction<UserRecordResult, PumpRecordResult>() {

            public ListenableFuture<PumpRecordResult> apply(final UserRecordResult userRecordResult) throws Exception {
                return Futures.immediateFuture(new PumpRecordResult(userRecordResult, drillResultRow));
            }

        });

    }

    /**
     * Producer that takes a ResultSet and turns it into Records for Kinesis emission.
     */
    private static class JdbcRecordProducer extends SerializingProducer {
        private final Subscriber<? super PumpRecord> subscriber;
        private final ResultSet resultSet;
        private final Connection connection;
        private final PumpSettings pumpSettings;

        public JdbcRecordProducer(Subscriber<? super PumpRecord> subscriber, ResultSet resultSet, Connection connection, PumpSettings pumpSettings) {
            this.subscriber = subscriber;
            this.resultSet = resultSet;
            this.connection = connection;
            this.pumpSettings = pumpSettings;
        }

        /**
         * @return whether it is okay to continue requesting items
         */
        @Override
        protected boolean onItemRequested() {
            boolean keepGoing = true;
            try {
                if (subscriber.isUnsubscribed()) {
                    keepGoing = false;
                    closeConnection();
                } else {
                    if (resultSet.next()) {
                        log.trace("Got a JDBC result record");

                        subscriber.onNext(getRecord(resultSet, pumpSettings.getPartitionKeyColumn(), pumpSettings.getRawDataColumn()));
                    } else {
                        subscriber.onCompleted();
                        keepGoing = false;
                    }
                }
            } catch (Exception e) {
                subscriber.onError(e);
                keepGoing = false;
            }
            return keepGoing;
        }

        /**
         * closes database connection
         */
        private void closeConnection() {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Failed to close database connection.", e);
            }
        }

        /**
         *
         * @param resultSet from the database query
         * @param partitionKeyColumn column to use for the partition key value
         * @param rawDataColumn column to use for the raw data
         * @return
         * @throws SQLException
         */
        private PumpRecord getRecord(ResultSet resultSet, String partitionKeyColumn, String rawDataColumn) throws SQLException{
            Record record = new Record();
            record.withPartitionKey(resultSet.getString(partitionKeyColumn));
            record.withData(ByteBuffer.wrap(resultSet.getBytes(rawDataColumn)));

            return new PumpRecord(record, DrillRepository.mapResultRow(resultSet));
        }
    }
}
