package com.example.android.bluetoothadvertisements;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReceiveKeyModel {
    @SerializedName("secret_key")
    @Expose
    private String secretKey;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
