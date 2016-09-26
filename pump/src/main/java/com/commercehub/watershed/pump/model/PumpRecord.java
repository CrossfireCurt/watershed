package com.commercehub.watershed.pump.model;

import com.amazonaws.services.kinesis.model.Record;

/**
 * Holds both a KinesisRecord (pre-emission) and the DrillResultRow retrieved from Drill.
 */
public class PumpRecord {
    Record kinesisRecord;
    DrillResultRow drillResultRow;

    public PumpRecord(Record kinesisRecord, DrillResultRow drillResultRow) {
        this.kinesisRecord = kinesisRecord;
        this.drillResultRow = drillResultRow;
    }

    /**
     * Retrieve the Kinesis record
     * @return Record
     */
    public Record getKinesisRecord() {
        return kinesisRecord;
    }

    /**
     * Retrieve the Drill row
     * @return DrillResultRow
     */
    public DrillResultRow getDrillResultRow() {
        return drillResultRow;
    }
}
