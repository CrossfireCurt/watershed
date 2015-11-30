package com.commercehub.watershed.pump.repositories

import com.commercehub.watershed.pump.model.JobPreview
import com.commercehub.watershed.pump.model.PreviewSettings
import com.commercehub.watershed.pump.respositories.DrillRepository
import com.google.inject.Provider
import com.mockrunner.mock.jdbc.MockResultSet
import spock.lang.Specification

import java.sql.Connection
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types

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
        PreviewSettings previewSettings = new PreviewSettings(queryIn: "select * from foo", previewCount: 3)
        String expectedCountQuery = "SELECT count(*) as total FROM (select * from foo)"
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

    def "getJobPreview handles boolean values with resultSetToList"(){
        setup:
        PreviewSettings previewSettings = new PreviewSettings(queryIn: "select * from foo", previewCount: 1)
        resultSet.next() >> true
        resultSet.getInt("total") >> 1
        resultSetMetaData.getColumnCount() >> 1
        resultSetMetaData.getColumnType(1) >> Types.BOOLEAN
        resultSetMetaData.getColumnName(1) >> "bool"
        resultSet.getBoolean(1) >> true

        when:
        JobPreview jobPreview = drillRepository.getJobPreview(previewSettings)

        then:
        2 * statement.executeQuery(_) >> resultSet
        1 * resultSet.getRow() >> 0
        resultSet.next() >> true

        then:
        1 * resultSet.getRow() >> 1
        resultSet.next() >> false

        then:
        jobPreview.count == 1
        jobPreview.rows.size() == 1
        jobPreview.rows == [["bool":"true"]]
    }

}