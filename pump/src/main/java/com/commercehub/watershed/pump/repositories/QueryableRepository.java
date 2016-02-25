package com.commercehub.watershed.pump.repositories;


import com.commercehub.watershed.pump.model.JobPreview;
import com.commercehub.watershed.pump.model.PreviewSettings;

import java.sql.SQLException;

/**
 * Defines implementation on how we want to talk to Drill (or another repository).
 */
public interface QueryableRepository {

    /**
     * Queries for record count and a few preview records
     * @param previewSettings
     * @return JobPreview
     * @throws SQLException
     */
    JobPreview getJobPreview(PreviewSettings previewSettings) throws SQLException;
}
