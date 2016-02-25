package com.commercehub.watershed.pump.repositories;


import com.commercehub.watershed.pump.model.DrillResultRow;
import com.commercehub.watershed.pump.model.JobPreview;
import com.commercehub.watershed.pump.model.PreviewSettings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository that communicates with Drill.
 */
public class DrillRepository implements QueryableRepository {
    private static final Logger log = LoggerFactory.getLogger(DrillRepository.class);

    @Inject
    private Provider<Connection> connectionProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public JobPreview getJobPreview(PreviewSettings previewSettings) throws SQLException {
        Connection connection = connectionProvider.get();

        String countSql = "SELECT count(*) as total FROM (" + previewSettings.getQueryIn() + ")";
        Integer count = null;
        List<Map<String, String>> rows = null;
        try{
            ResultSet resultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(countSql);
            resultSet.setFetchSize(1);
            resultSet.next();
            count = resultSet.getInt("total");

            resultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(previewSettings.getQueryIn());
            resultSet.setFetchSize(previewSettings.getPreviewCount());

            rows = resultSetToList(resultSet, previewSettings.getPreviewCount());
        }
        finally {
            try {
                connection.close();
            }
            catch (Exception e) {
                log.warn("Failed to close database connection.", e);
            }
        }

        return new JobPreview(count, rows);
    }

    /**
     * Converts a ResultSet to a list of {@code Map<String, String>}
     * @param resultSet
     * @param rowLimit
     * @return {@code List<Map<String, String>>}
     * @throws SQLException
     */
    private List<Map<String, String>> resultSetToList(ResultSet resultSet, Integer rowLimit) throws SQLException{
        List<Map<String, String>> list = new ArrayList<>();
        while (resultSet.next() && resultSet.getRow() < rowLimit){
            list.add(mapResultRow(resultSet));
        }

        return list;
    }


    public static DrillResultRow mapResultRow(ResultSet resultSet) throws SQLException{
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        DrillResultRow drillResultRow = new DrillResultRow();
        for(int i = 1; i <= resultSetMetaData.getColumnCount(); i++){
            drillResultRow.put(resultSetMetaData.getColumnName(i), resultSet.getString(i));
        }

        return drillResultRow;
    }
}
