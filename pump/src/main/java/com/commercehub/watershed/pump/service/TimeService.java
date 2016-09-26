package com.commercehub.watershed.pump.service;

/**
 * Defines which methods we need to retrieve time-specific information.
 */
public interface TimeService {
    /**
     * Returns the current time in milliseconds.
     *
     * @return  the difference, measured in milliseconds, between
     *          the current time and midnight, January 1, 1970 UTC.
     */
    long currentTimeMillis();
}
