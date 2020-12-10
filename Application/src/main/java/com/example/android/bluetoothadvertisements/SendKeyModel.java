package com.example.android.bluetoothadvertisements;

import com.google.gson.annotations.SerializedName;

public class SendKeyModel {
    private String secret_key;

    private String timestamp;

    private String auth_code;

    public SendKeyModel(String secret_key, String timestamp, String authCode) {
        this.secret_key = secret_key;
        this.timestamp = timestamp;
        this.auth_code = authCode;
    }
}
