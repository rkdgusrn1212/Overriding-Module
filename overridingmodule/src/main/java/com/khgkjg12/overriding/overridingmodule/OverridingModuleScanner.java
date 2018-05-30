package com.khgkjg12.overriding.overridingmodule;


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

class OverridingModuleScanner {

    private Application mApplication;
    private BroadcastReceiver mReceiver;
    private final String LOG_TAG = getClass().getSimpleName();
    private OverridingModuleController.OnScanListener mListener;
    private boolean mIsOpened = false;
    private BluetoothAdapter mBluetoothAdapter;

    OverridingModuleScanner(Application app, BluetoothAdapter bluetoothAdapter){
        mApplication = app;
        mBluetoothAdapter = bluetoothAdapter;
    }

    void setOnScanListener(OverridingModuleController.OnScanListener onScanListener){
        mListener = onScanListener;
    }

    void open(){
        if(mIsOpened){
            Log.i(LOG_TAG, "already opened");
            return;
        }

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(LOG_TAG, "received something");
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    Log.i(LOG_TAG, "scanned : " + "\naddress: "+device.getAddress()+"\nName: "+device.getName());
                    if(device.getUuids()!=null){
                        for(int i =0 ;i<device.getUuids().length; i++){
                            Log.i(LOG_TAG, "UUID : "+device.getUuids()[i]);
                        }
                    }
                    if(mListener != null) {
                        mListener.onScanEachModule(new OverridingModule(device));
                    }
                }
            }
        };
        mApplication.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mIsOpened = true;
    }

    /**
     * scan()을 호출할 엑티비티에서 동적권한 할당과 Bluetooth 켜기를 구현해야함.
     * @return 스켄이 시작되었는지 여부를 반환
     */
    boolean scan(){

        if(mBluetoothAdapter.isDiscovering()){
            Log.i(LOG_TAG, "scanning is progressing... cancel scanning");
            mBluetoothAdapter.cancelDiscovery();

        }

        Log.i(LOG_TAG, "scanning start");

        if(mBluetoothAdapter.startDiscovery()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                ArrayList<OverridingModule> mPairedList = new ArrayList<>();
                for (BluetoothDevice device : pairedDevices) {
                    for(int i=0 ; i<device.getUuids().length;i++){
                        Log.d("hyungu","UUID : "+device.getUuids()[i].toString());
                    }
                    // Add the name and address to an array adapter to show in a ListView
                    mPairedList.add(new OverridingModule(device));
                }
                mListener.onScanPairedList(mPairedList);
            }
            return true;
        }else{
            return false;
        }
    }

    void close(){
        mApplication.unregisterReceiver(mReceiver);
        mIsOpened = false;
    }
}
