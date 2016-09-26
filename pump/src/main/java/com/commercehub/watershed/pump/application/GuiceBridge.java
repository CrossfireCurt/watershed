package com.commercehub.watershed.pump.application;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * GuiceBridge gives us a way to override the Injector. Mostly used w/testing.
 */
public class GuiceBridge {
    private static Injector defaultInjector = Guice.createInjector(new PumpGuiceModule());
    private static Injector overrideInjector;

    /**
     *
     * @return the Injector currently in use.
     */
    public static Injector getInjector() {
        return overrideInjector != null? overrideInjector : defaultInjector;
    }

    /**
     * Provide an override Injector
     * @param injector
     */
    public static void setOverrideInjector(Injector injector) {
        overrideInjector = injector;
    }
}
