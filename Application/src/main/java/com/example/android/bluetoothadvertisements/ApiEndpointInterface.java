package com.example.android.bluetoothadvertisements;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiEndpointInterface {
    @POST("/receivekeys")
    Call<List<ReceiveKeyModel>> getKeys(@Body DateModel date);

    @POST("/sendkeys")
    Call<String> sendKey(@Body SendKeyModel sendKeyModel);
}
