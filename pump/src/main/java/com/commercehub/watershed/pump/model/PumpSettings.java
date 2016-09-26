package com.commercehub.watershed.pump.model;

import javax.validation.constraints.NotNull;

/**
 * Holds configuration for how to run a job
 */
public class PumpSettings {
    @NotNull
    private String queryIn;

    @NotNull
    private Boolean hasOverwriteFlag = false;

    @NotNull
    private Boolean hasReplayFlag = false;

    @NotNull
    private String streamOut;

    @NotNull
    private String rawDataColumn = "rawData";

    @NotNull
    private String partitionKeyColumn = "partitionKey";

    /**
     *
     * @return the query for the job
     */
    public String getQueryIn() {
        return queryIn;
    }

    /**
     * set the query for the job
     * @param queryIn
     */
    public void setQueryIn(String queryIn) {
        this.queryIn = queryIn;
    }

    /**
     *
     * @return if the overwrite flag has been set
     */
    public Boolean getHasOverwriteFlag() {
        return hasOverwriteFlag;
    }


    /**
     * set overwrite flag
     * @param hasOverwriteFlag
     */
    public void setHasOverwriteFlag(Boolean hasOverwriteFlag) {
        this.hasOverwriteFlag = hasOverwriteFlag;
    }

    /**
     *
     * @return if the replay flag has been set
     */
    public Boolean getHasReplayFlag() {
        return hasReplayFlag;
    }

    /**
     * set replay flag
     * @param hasReplayFlag
     */
    public void setHasReplayFlag(Boolean hasReplayFlag) {
        this.hasReplayFlag = hasReplayFlag;
    }

    /**
     *
     * @return the stream that Pump will emit to
     */
    public String getStreamOut() {
        return streamOut;
    }

    /**
     * set the stream that Pump will emit to
     * @param streamOut
     */
    public void setStreamOut(String streamOut) {
        this.streamOut = streamOut;
    }

    /**
     *
     * @return the column where the raw data for kinesis emission can be found
     */
    public String getRawDataColumn() {
        return rawDataColumn;
    }

    /**
     * set the raw data column name
     * @param rawDataColumn
     */
    public void setRawDataColumn(String rawDataColumn) {
        this.rawDataColumn = rawDataColumn;
    }

    /**
     *
     * @return the column where the partition key for kinesis emission can be found
     */
    public String getPartitionKeyColumn() {
        return partitionKeyColumn;
    }

    /**
     * set the partition key column name
     * @param partitionKeyColumn
     */
    public void setPartitionKeyColumn(String partitionKeyColumn) {
        this.partitionKeyColumn = partitionKeyColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PumpSettings that = (PumpSettings) o;

        if (!queryIn.equals(that.queryIn)) return false;
        if (!hasOverwriteFlag.equals(that.hasOverwriteFlag)) return false;
        if (!hasReplayFlag.equals(that.hasReplayFlag)) return false;
        if (!streamOut.equals(that.streamOut)) return false;
        if (!rawDataColumn.equals(that.rawDataColumn)) return false;
        return partitionKeyColumn.equals(that.partitionKeyColumn);

    }

    @Override
    public int hashCode() {
        int result = queryIn.hashCode();
        result = 31 * result + hasOverwriteFlag.hashCode();
        result = 31 * result + hasReplayFlag.hashCode();
        result = 31 * result + streamOut.hashCode();
        result = 31 * result + rawDataColumn.hashCode();
        result = 31 * result + partitionKeyColumn.hashCode();
        return result;
    }
}
