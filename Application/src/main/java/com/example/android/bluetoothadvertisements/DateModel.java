package com.example.android.bluetoothadvertisements;

import com.google.gson.annotations.SerializedName;

public class DateModel {
    @SerializedName("date")
    String mDate;

    public DateModel(String date) {
        this.mDate = date;
    }
}
