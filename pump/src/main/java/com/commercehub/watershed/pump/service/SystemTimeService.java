package com.commercehub.watershed.pump.service;

/**
 * Implementation of TimeService using System level calls.
 */
public class SystemTimeService implements TimeService{

    /**
     * {@inheritDoc}
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
