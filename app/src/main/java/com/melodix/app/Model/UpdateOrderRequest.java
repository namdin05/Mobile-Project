package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class UpdateOrderRequest {
    @SerializedName("order_index")
    private int orderIndex;

    public UpdateOrderRequest(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}