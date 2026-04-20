package com.melodix.app.Model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class AuditLog {
    @SerializedName("id")
    private String id;

    @SerializedName("table_name")
    private String tableName;

    @SerializedName("record_id")
    private String recordId;

    @SerializedName("action")
    private String action;

    @SerializedName("old_data")
    private JsonElement oldData;

    @SerializedName("new_data")
    private JsonElement newData;

    @SerializedName("changed_by")
    private String changedBy;

    @SerializedName("changed_at")
    private String changedAt;

    // Getters
    public String getId() { return id; }
    public String getTableName() { return tableName; }
    public String getRecordId() { return recordId; }
    public String getAction() { return action; }
    public JsonElement getOldData() { return oldData; }
    public JsonElement getNewData() { return newData; }
    public String getChangedBy() { return changedBy; }
    public String getChangedAt() { return changedAt; }
}
