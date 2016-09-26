package com.commercehub.watershed.pump.application;


import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard bootstrap for Pump (Initializes Guice, Jersey, etc).
 */
public class PumpApplication extends Application<PumpConfiguration> {

    private static Logger log = LoggerFactory.getLogger(PumpApplication.class);
    private static final String[] defaultArgs = {"server", "./src/main/resources/pump.yaml"};

    protected GuiceBundle<PumpConfiguration> guiceBundle;

    /**
     * Pump starts here.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.warn("No arguments set, running in development mode.");
            args = defaultArgs;
        }

        new PumpApplication().run(args);
    }

    /**
     * Initializes things that need to be initialized (Guice).
     * @param bootstrap
     */
    @Override
    public void initialize(Bootstrap<PumpConfiguration> bootstrap) {
        guiceBundle = buildGuiceBundleForApplication();
        bootstrap.addBundle(guiceBundle);
    }

    /**
     * Run Jersey.
     * @param configuration
     * @param environment
     * @throws Exception
     */
    @Override
    public void run(PumpConfiguration configuration, Environment environment) throws Exception {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");

        GuiceBridge.setOverrideInjector(guiceBundle.getInjector());
        if (environment.jersey().getResourceConfig().getEndpointsInfo().contains("NONE")) {
            log.info("No resources registered, disabling jersey.");
            environment.jersey().disable();
        }
    }

    /**
     * Create a new GuiceBundle with our configuration, setup auto configuration.
     * @return a configured GuiceBundle
     */
    protected GuiceBundle<PumpConfiguration> buildGuiceBundleForApplication() {
        return GuiceBundle.<PumpConfiguration>newBuilder()
                .addModule(new PumpGuiceModule())
                .enableAutoConfig("com.commercehub.watershed.pump").setConfigClass(PumpConfiguration.class)
                .build();
    }
}
