package com.commercehub.watershed.pump.application;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.commercehub.watershed.pump.application.factories.JobFactory;
import com.commercehub.watershed.pump.application.factories.JobRunnableFactory;
import com.commercehub.watershed.pump.application.factories.PumpFactory;
import com.commercehub.watershed.pump.application.factories.PumpSubscriberFactory;
import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.processing.IsolatedConnectionProvider;
import com.commercehub.watershed.pump.processing.JobRunnable;
import com.commercehub.watershed.pump.processing.Pump;
import com.commercehub.watershed.pump.processing.PumpSubscriber;
import com.commercehub.watershed.pump.repositories.DrillRepository;
import com.commercehub.watershed.pump.repositories.QueryableRepository;
import com.commercehub.watershed.pump.service.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.github.davidmoten.rx.jdbc.ConnectionProvider;
import com.github.davidmoten.rx.jdbc.Database;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Setup Pump with Guice dependency injection.
 */
public class PumpGuiceModule extends AbstractModule {
    private static final String DEFAULT_PROPERTIES_FILE = "pump.properties";
    private static final Logger log = LoggerFactory.getLogger(PumpGuiceModule.class);

    private Properties defaultProperties;
    private ObjectMapper objectMapper;

    private Map<String, Job> jobMap = new HashMap<>();

    @Override
    protected void configure() {
        defaultProperties = getDefaultPropertiesFromFile();
        objectMapper = configureObjectMapper();
        bind(JobService.class).to(JobServiceImpl.class);
        bind(QueryableRepository.class).to(DrillRepository.class);
        bind(TransformerFunctionFactory.class).to(TransformerFunctionFactoryImpl.class);
        bind(KinesisService.class).to(KinesisServiceImpl.class);
        bind(TimeService.class).to(SystemTimeService.class);

        //Assisted Injection https://github.com/google/guice/wiki/AssistedInject
        install(new FactoryModuleBuilder().implement(Job.class, Job.class).build(JobFactory.class));
        install(new FactoryModuleBuilder().implement(Pump.class, Pump.class).build(PumpFactory.class));
        install(new FactoryModuleBuilder().implement(PumpSubscriber.class, PumpSubscriber.class).build(PumpSubscriberFactory.class));
        install(new FactoryModuleBuilder().implement(JobRunnable.class, JobRunnable.class).build(JobRunnableFactory.class));
    }

    /**
     *
     * @return Application-wide Properties, defined in pump.properties
     */
    @Provides
    @Singleton
    @Named("applicationProperties")
    private Properties getProperties(){
        return defaultProperties;
    }

    /**
     *
     * @return In-memory map for the Job list.
     */
    @Provides
    @Singleton
    private Map<String, Job> getJobMap(){
        return jobMap;
    }

    /**
     *
     * @return the configured instance of ObjectMapper
     */
    @Provides
    @Singleton
    private ObjectMapper getObjectMapper(){
        return objectMapper;
    }

    /**
     *
     * @return the configured instance of ObjectMapper
     */
    private ObjectMapper configureObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        return objectMapper;
    }


    /**
     *
     * @return load application properties from pump.properties
     */
    private Properties getDefaultPropertiesFromFile() {
        Properties properties = new Properties();
        String propertyFileName = DEFAULT_PROPERTIES_FILE;
        try {
            InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFileName);
            properties.load(configStream);
            Names.bindProperties(binder(), properties);
        } catch (Exception ex) {
            log.error("Had a problem loading in application properties: " + propertyFileName, ex);
        }

        return properties;
    }



    /**
     *
     * @param kinesisProducerConfiguration from the KinesisProducerConfiguration provider
     * @return a provider for KinesisProducer
     */
    @Provides
    private KinesisProducer kinesisProducerProvider(KinesisProducerConfiguration kinesisProducerConfiguration){
        return new KinesisProducer(kinesisProducerConfiguration);
    }


    /**
     *
     * @param properties
     * @return a provider for KinesisProducerConfiguration
     */
    @Provides
    @Singleton
    private KinesisProducerConfiguration configureKinesis(@Named("applicationProperties") Properties properties) {
        KinesisProducerConfiguration kinesisConfig = new KinesisProducerConfiguration();

        kinesisConfig.setAggregationEnabled(Boolean.valueOf(properties.get("kinesisAggregationEnabled").toString()));
        kinesisConfig.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());

        kinesisConfig.setRegion(properties.get("kinesisRegion").toString());
        kinesisConfig.setRecordTtl(Integer.valueOf(properties.get("kinesisRecordTtl").toString()));  //Maybe not the best idea to use MAX_VALUE

        // Pump works more smoothly when shards are not saturated, so 95% is a good maximum rate.
        // May be lowered further to share capacity with running applications.
        kinesisConfig.setRateLimit(Integer.valueOf(properties.get("producerRateLimit").toString()));

        //TODO set more Kinesis Configuration options as appropriate
        return kinesisConfig;
    }


    /**
     *
     * @param kinesisConfig from the KinesisProducerConfiguration provider
     * @return a provider for AmazonKinesisClient
     */
    @Provides
    @Singleton
    private AmazonKinesisClient getKinesisClient(KinesisProducerConfiguration kinesisConfig){
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(kinesisConfig.getCredentialsProvider());
        kinesisClient.setRegion(Region.getRegion(Regions.fromName(kinesisConfig.getRegion())));
        return kinesisClient;
    }


    /**
     *
     * @return a provider for Database
     * @throws InterruptedException
     */
    @Provides
    @Singleton
    private Database connectDatabase() throws InterruptedException {
        String jdbcUrl = "jdbc:drill:drillbit=localhost:31010";
        String jdbcUsername = "admin";
        String jdbcPassword = "admin";
        String driverClass = "org.apache.drill.jdbc.Driver";

        Database database = null;

        int i = 0;
        log.info("Testing database connection...");
        ConnectionProvider cp = IsolatedConnectionProvider.get(jdbcUrl, jdbcUsername, jdbcPassword, driverClass);

        while(database == null && i < 10) {
            try {
                cp.get().close();
                database = Database.from(cp);
                log.info("Database connectivity verified.");
            }
            catch (Exception e) {
                log.warn("Could not establish a preliminary database connection, retrying... " + e.getMessage());
                Thread.sleep(6000);
                i++;
            }
        }

        if(database == null){
            log.error("Could not establish a preliminary database connection.");
            System.exit(2);
        }

        return database;
    }


    /**
     *
     * @param database from the database provider
     * @return a provider for Connections
     */
    @Provides
    private Connection connectionProvider(Database database){
        return database.getConnectionProvider().get();
    }


    /**
     *
     * @param numConcurrentJobs allowed number of concurrent running Pump jobs
     * @return a provider for ExecutorService
     */
    @Provides
    @Singleton
    protected ExecutorService getExecutor(@Named("numConcurrentJobs") int numConcurrentJobs) {
        return Executors.newFixedThreadPool(numConcurrentJobs);
    }
}
