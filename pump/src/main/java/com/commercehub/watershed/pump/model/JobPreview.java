package com.commercehub.watershed.pump.model;

import java.util.List;
import java.util.Map;

/**
 * Result after querying Drill for a "preview" of a Job
 */
public class JobPreview {
    private Integer count;
    private List<Map<String, String>> rows;

    /**
     *
     * @param count
     * @param rows
     */
    public JobPreview(Integer count, List<Map<String, String>> rows) {
        this.count = count;
        this.rows = rows;
    }

    /**
     *
     * @return the count of all records in the query
     */
    public Integer getCount() {
        return count;
    }
    /**
     *
     * @return a preview of rows that will be processed (usually a subset of the full query results)
     */
    public List<Map<String, String>> getRows() {
        return rows;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobPreview that = (JobPreview) o;

        if (!count.equals(that.count)) return false;
        return rows.equals(that.rows);

    }

    @Override
    public int hashCode() {
        int result = count.hashCode();
        result = 31 * result + rows.hashCode();
        return result;
    }
}
