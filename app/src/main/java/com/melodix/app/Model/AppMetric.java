package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class AppMetric {
    @SerializedName("metric_key")
    private String metricKey;

    @SerializedName("metric_value")
    private String metricValue;

    public String getMetricKey() {
        return metricKey;
    }

    public String getMetricValue() {
        return metricValue;
    }
}