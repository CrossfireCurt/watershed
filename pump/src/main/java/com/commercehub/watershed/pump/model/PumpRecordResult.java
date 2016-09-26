package com.commercehub.watershed.pump.model;

import com.amazonaws.services.kinesis.producer.UserRecordResult;

/**
 * Holds results for the KinesisRecord (post-emission) and the row that was pulled from Drill.
 */
public class PumpRecordResult {
    private UserRecordResult userRecordResult;
    private DrillResultRow drillResultRow;

    public PumpRecordResult(UserRecordResult userRecordResult, DrillResultRow drillResultRow) {
        this.userRecordResult = userRecordResult;
        this.drillResultRow = drillResultRow;
    }

    /**
     * Get the Kinesis UserRecordResult
     * @return UserRecordResult
     */
    public UserRecordResult getUserRecordResult() {
        return userRecordResult;
    }

    /**
     * Get the row that was pulled from Drill.
     * @return
     */
    public DrillResultRow getDrillResultRow() {
        return drillResultRow;
    }
}
