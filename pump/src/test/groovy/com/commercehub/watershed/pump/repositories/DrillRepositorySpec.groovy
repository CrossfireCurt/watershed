package com.commercehub.watershed.pump.repositories

import com.commercehub.watershed.pump.model.JobPreview
import com.commercehub.watershed.pump.model.PreviewSettings
import com.google.inject.Provider
import spock.lang.Specification

import java.sql.Connection
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Statement

class DrillRepositorySpec extends Specification{
    DrillRepository drillRepository
    Provider<Connection> connectionProvider
    Connection connection
    Statement statement
    ResultSet resultSet
    ResultSetMetaData resultSetMetaData

    def setup() {
        connectionProvider = Mock(Provider)
        connection = Mock(Connection)
        statement = Mock(Statement)
        resultSet = Mock(ResultSet)
        connectionProvider.get() >> connection
        connection.createStatement(*_) >> statement
        resultSetMetaData = Mock(ResultSetMetaData)
        resultSet.getMetaData() >> resultSetMetaData

        drillRepository = new DrillRepository(
                connectionProvider: connectionProvider)
    }

    def "getJobPreview uses 'count' query to determine row count"(){
        setup:
        String query = "select * from foo"
        PreviewSettings previewSettings = new PreviewSettings(queryIn: query, previewCount: 3)
        String expectedCountQuery = "SELECT count(*) as total FROM (" + query + ")"
        resultSet.next() >> false
        resultSetMetaData.getColumnCount() >> 1

        when:
        JobPreview jobPreview = drillRepository.getJobPreview(previewSettings)

        then:
        1 * statement.executeQuery(expectedCountQuery) >> resultSet
        1 * resultSet.getInt("total") >> 10
        1 * statement.executeQuery(previewSettings.getQueryIn()) >> resultSet

        then:
        jobPreview.count == 10
    }

}
