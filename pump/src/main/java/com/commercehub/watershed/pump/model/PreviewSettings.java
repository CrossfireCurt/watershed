package com.commercehub.watershed.pump.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Holds configuration for how to run a job preview
 */
public class PreviewSettings {
    @NotNull
    private String queryIn;

    @NotNull
    @Min(0)
    private Integer previewCount;

    /**
     *
     * @return the query used to run the preview
     */
    public String getQueryIn() {
        return queryIn;
    }

    /**
     * set the query used to run the preview
     * @param queryIn
     */
    public void setQueryIn(String queryIn) {
        this.queryIn = queryIn;
    }

    /**
     *
     * @return the number of rows to return in the job preview
     */
    public Integer getPreviewCount() {
        return previewCount;
    }

    /**
     * set the number of rows to return in the job preview
     * @param previewCount
     */
    public void setPreviewCount(Integer previewCount) {
        this.previewCount = previewCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PreviewSettings that = (PreviewSettings) o;

        if (queryIn != null ? !queryIn.equals(that.queryIn) : that.queryIn != null) return false;
        return !(previewCount != null ? !previewCount.equals(that.previewCount) : that.previewCount != null);

    }

    @Override
    public int hashCode() {
        int result = queryIn != null ? queryIn.hashCode() : 0;
        result = 31 * result + (previewCount != null ? previewCount.hashCode() : 0);
        return result;
    }
}
