/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothadvertisements;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/**
 * Scans for Bluetooth Low Energy Advertisements matching a filter and displays them to the user.
 */
public class ScannerFragment extends ListFragment {

    private static final String TAG = ScannerFragment.class.getSimpleName();

    /**
     * Stops scanning after 5 seconds.
     */
    private static final long SCAN_PERIOD = 60000;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private ScanCallback mScanCallback;

    private ScanResultAdapter mAdapter;

    private Handler mHandler;

    AlertDialog.Builder builder;

    private void setDialog(boolean show) {
        builder.setView(R.layout.progress);
        Dialog dialog = builder.create();
        if (show) dialog.show();
        else dialog.dismiss();
    }


    /**
     * Must be called after object creation by MainActivity.
     *
     * @param btAdapter the local BluetoothAdapter
     */
    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mAdapter = new ScanResultAdapter(getActivity().getApplicationContext(),
                LayoutInflater.from(getActivity()));
        mHandler = new Handler();

        builder = new AlertDialog.Builder(getActivity());

        String BASE_URL = "https://proximity-tracing.herokuapp.com/";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = super.onCreateView(inflater, container, savedInstanceState);

        setListAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        setEmptyText(getString(R.string.empty_list));

        // Trigger refresh on app's 1st load
        startScanning();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.scanner_menu, menu);

        MenuItem syncButton = menu.findItem(R.id.sync);
        MenuItem testedPositiveButton = menu.findItem(R.id.i_tested_positive);

        String title = syncButton.getTitle().toString();
        SpannableString s = new SpannableString(title);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")), 0, s.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE); // provide whatever color you want here.
        syncButton.setTitle(s);

        title = testedPositiveButton.getTitle().toString();
        s = new SpannableString(title);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")), 0, s.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE); // provide whatever color you want here.
        testedPositiveButton.setTitle(s);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.refresh:
                startScanning();
                return true;
            case R.id.sync:
                startSync();
                return true;
            case R.id.i_tested_positive:
                reportPositive();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

        } else {
            Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_SHORT);
        }
    }

    public void startSync() {
//        setDialog(true);
        final HashSet<String> storedEphIDs = EphID.getEphIDs();

        Toast.makeText(getActivity(), "Syncing...", Toast.LENGTH_SHORT).show();

        String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        DateModel dateModel = new DateModel(today);

        String BASE_URL = "https://proximity-tracing.herokuapp.com/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiEndpointInterface apiService = retrofit.create(ApiEndpointInterface.class);
        Call<List<ReceiveKeyModel>> call = apiService.getKeys(dateModel);

        call.enqueue(new Callback<List<ReceiveKeyModel>>() {
            @Override
            public void onResponse(Call<List<ReceiveKeyModel>> call, Response<List<ReceiveKeyModel>> response) {
                Log.i("Stored EphID", storedEphIDs.toString());
                if (response.body() == null) {
                    Log.e("sync", "empty response");
                    showNotContactedDialog();
                    return;
                }
                List<ReceiveKeyModel> receiveKeyModels = response.body();

                for (ReceiveKeyModel rkm : receiveKeyModels) {
                    Log.i("Received EphID", rkm.getSecretKey());
                }


                boolean contacted = false;
                for (ReceiveKeyModel receiveKeyModel : receiveKeyModels) {
                    String key = receiveKeyModel.getSecretKey();
                    ArrayList<String> ephIDs = EphID.generateEphID(key);

                    for (String s : storedEphIDs) {
                        if (ephIDs.contains(s)) {
                            showPossiblyContactedDialog();
                            contacted = true;
                            break;
                        }
                    }

                    if (contacted) {
                        break;
                    }
                }

                if (!contacted) {
                    showNotContactedDialog();
                }
            }

            @Override
            public void onFailure(Call<List<ReceiveKeyModel>> call, Throwable t) {
//                setDialog(false);
                Toast.makeText(getActivity(), "Request failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showPossiblyContactedDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Possibly affected")
                .setMessage("It appears that you were in contact with a person found COVID-19 positive. It is recommended to get tested.")

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void showNotContactedDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("You are safe!")
                .setMessage("You weren't in contact with any person found COVID-19 positive.")

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })
                .show();
    }

    String authCode = "";
    String secretKey = "";
    String timestamp = "";

    public void reportPositive() {
        secretKey = EphID.getSecretKey();
        timestamp = Long.toString(new Timestamp(System.currentTimeMillis()).getTime());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Authentication Code");
        builder.setMessage("This can be obtained from the testing center.");

        // Set up the input
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                authCode = input.getText().toString();
                sendKey();
            }
        });

        builder.show();
    }

    private void sendKey() {
        Toast.makeText(getActivity(), "Sending key...", Toast.LENGTH_SHORT).show();

        Log.i("secretKey", secretKey);
        Log.i("timestamp", timestamp);
        Log.i("authCode", authCode);

        SendKeyModel sendKeyModel = new SendKeyModel(secretKey, timestamp, authCode);

        String BASE_URL = "https://proximity-tracing.herokuapp.com/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiEndpointInterface apiService = retrofit.create(ApiEndpointInterface.class);
        Call<String> call = apiService.sendKey(sendKeyModel);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()){
                    if (response.body() != null){
                        Log.i("onSuccess", response.body().toString());
                    }else{
                        Log.i("onEmptyResponse", "Returned empty response");//Toast.makeText(getContext(),"Nothing returned",Toast.LENGTH_LONG).show();
                    }
                }

//                String result = response.body();
//                Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
//                if (result.equals("successfully written")) {
//                    new AlertDialog.Builder(getActivity())
//                            .setTitle("Secret key sent successfully")
//                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    // Continue with delete operation
//                                }
//                            })
//                            .show();
//
//                }
//
//                if (result.equals("authentication failed")) {
//                    new AlertDialog.Builder(getActivity())
//                            .setTitle("Authentication failed")
//                            .setMessage("There was an error with the authentication code. Please try again with a correct code.")
//                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    // Continue with delete operation
//                                }
//                            })
//                            .show();
//                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getActivity(), "Request failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Even if no new results, update 'last seen' times.
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                mAdapter.add(result);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            mAdapter.add(result);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }
    }
}
