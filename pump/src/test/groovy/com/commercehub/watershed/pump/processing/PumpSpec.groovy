package com.commercehub.watershed.pump.processing

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.kinesis.producer.KinesisProducer
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.commercehub.watershed.pump.model.Job
import com.commercehub.watershed.pump.model.PumpSettings
import com.commercehub.watershed.pump.service.KinesisService
import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import rx.Observable
import rx.observers.TestSubscriber
import spock.lang.Specification

import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ExecutionException


public class PumpSpec extends Specification {
    Job job
    Pump pump
    Connection connection
    KinesisProducer kinesisProducer
    KinesisService kinesisService
    PumpSettings pumpSettings
    TestSubscriber<UserRecordResult> testSubscriber
    Statement statement
    ResultSet resultSet
    ResultSetMetaData resultSetMetaData
    UserRecordResult userRecordResult
    Function recordTransformer
    Observable<UserRecordResult> results

    String partitonKeyColumnName = "partition_key"
    String rawDataColumnName = "raw_data"


    def setup(){
        job = Mock(Job)
        recordTransformer = Mock(Function)

        setupResultSet()
        setupPumpSettings()
        setupKinesis()

        pump = new Pump(connection, kinesisProducer, kinesisService, 5, 1, pumpSettings, null)

        testSubscriber = TestSubscriber.create(1)
        results = pump.build()
    }

    def setupResultSet(){
        statement = Mock(Statement)
        connection = Mock(Connection)
        resultSetMetaData = Mock(ResultSetMetaData)
        resultSet = Mock(ResultSet)
        connection.createStatement(_, _) >> statement

        resultSet.getMetaData() >> resultSetMetaData
        resultSet.next() >> true
        resultSet.getString(partitonKeyColumnName) >> "key"
        resultSet.getBytes(rawDataColumnName) >> "data".getBytes()

        statement.executeQuery("select * from foo") >> resultSet
    }

    def setupPumpSettings(){
        pumpSettings = Mock(PumpSettings)
        pumpSettings.getPartitionKeyColumn() >> partitonKeyColumnName
        pumpSettings.getRawDataColumn() >> rawDataColumnName
        pumpSettings.getStreamOut() >> "stream"

        pumpSettings.getQueryIn() >> "select * from foo"
    }

    def setupKinesis(){
        kinesisProducer = Mock(KinesisProducer)
        kinesisService = Mock(KinesisService)
        userRecordResult = Mock(UserRecordResult)
    }

    def "pump queries database when subscriber subscribes"(){
        setup:
        testSubscriber = new TestSubscriber<>()

        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        1 * resultSet.next() >> false
        testSubscriber.assertNoErrors()
        2 * pumpSettings.getQueryIn() >> "select * from foo"
        1 * statement.executeQuery("select * from foo") >> resultSet
    }

    def "error when pump queries database"(){
        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        2 * pumpSettings.getQueryIn() >> "select * from foo"
        1 * statement.executeQuery("select * from foo") >> { throw new SQLException("sql error") }

        then:
        testSubscriber.assertNotCompleted()
        testSubscriber.assertError(SQLException.class)
        testSubscriber.assertTerminalEvent()
        testSubscriber.assertNoValues()
    }

    def "subscriber receives onNext from producer"(){
        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        1 * resultSet.next() >> true

        then:
        //kinesis produces a record
        1 * kinesisProducer.addUserRecord("stream", "key", ByteBuffer.wrap("data".getBytes())) >> new Futures.ImmediateSuccessfulFuture(userRecordResult)
        1 * resultSet.next() >> false

        then:
        //subscriber is told about the record
        testSubscriber.assertNoErrors()
        testSubscriber.assertValueCount(1)
        testSubscriber.assertCompleted()
    }

    def "subscriber receives onError when producer errors"(){
        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        1 * resultSet.next() >> true

        then:
        //kinesis errors on a record
        1 * kinesisProducer.addUserRecord("stream", "key", ByteBuffer.wrap("data".getBytes())) >> new Futures.ImmediateFailedFuture(new AmazonServiceException("amazon error"))
        1 * resultSet.next() >> false

        then:
        //subscriber is told about the error
        testSubscriber.assertError(ExecutionException.class) //from the Future
        testSubscriber.assertNoValues()
        testSubscriber.assertTerminalEvent()
        testSubscriber.assertNotCompleted()
    }

    def "records transformed if transformer provided"(){
        setup:
        pump = new Pump(connection, kinesisProducer, kinesisService, 5, 1, pumpSettings, recordTransformer)
        results = pump.build()

        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        1 * resultSet.next() >> true

        then:
        //record transformer is called
        1 * recordTransformer.apply(ByteBuffer.wrap("data".getBytes()).array()) >> "transformed data".getBytes()
        1 * kinesisProducer.addUserRecord("stream", "key", ByteBuffer.wrap("transformed data".getBytes())) >> new Futures.ImmediateSuccessfulFuture(userRecordResult)
        1 * resultSet.next() >> false

        then:
        //subscriber is told about the transformed record
        testSubscriber.assertNoErrors()
        testSubscriber.assertValueCount(1)
        testSubscriber.assertCompleted()
    }

    def "subscriber receives onError when resultSet errors"(){
        when:
        results.subscribe(testSubscriber)
        testSubscriber.awaitTerminalEvent()

        then:
        //resultSet errors on a record
        1 * resultSet.next() >> { throw new SQLException() }

        then:
        //subscriber is told about the error
        testSubscriber.assertError(SQLException.class)
        testSubscriber.assertNoValues()
        testSubscriber.assertTerminalEvent()
        testSubscriber.assertNotCompleted()
    }

    def "destroy() destroys kinesis producer"(){
        when:
        pump.destroy()

        then:
        1 * kinesisProducer.destroy()
    }

    def "flushSync() calls flushSync on kinesis producer"(){
        when:
        pump.flushSync()

        then:
        1 * kinesisProducer.flushSync()
    }

    def "countPending() counts pending from kinesis producer"(){
        when:
        pump.countPending()

        then:
        1 * kinesisProducer.getOutstandingRecordsCount()
    }
}
