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
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * 오버라이딩 모듈을 스캔하는 싱글톤 패턴의 클래스이다.
 * 사용법.
 * 1. 사용전 init()함수에 애플리케이션 컨텍스트를 매개변수로 넘겨서 초기화 시켜준다.
 * 2. 스캔사용.
 * 3. close()로 사용을 종료한다.
 */
public class OverridingModuleScanner {

    private Application mApplication;
    private static OverridingModuleScanner mInstance = new OverridingModuleScanner();
    private BroadcastReceiver mReceiver;
    boolean isInitiated = false;
    private String logTag = getClass().getSimpleName();
    private ArrayList<OverridingModule> mModuleList;
    private OnScanListener mListener;
    private BluetoothAdapter mBluetoothAdapter;

    private OverridingModuleScanner(){};

    public OverridingModuleScanner getInstance(){
        return mInstance;
    }

    public void init(Application app) throws RuntimeException{
        if(isInitiated){
            Log.i(logTag,"Already initiated!");
            return;
        }
        mApplication = app;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new RuntimeException("Device does not support Bluetooth");
        }

        /**
         * 동적 권한 확인 로직. 권한 없으면 throw
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if(mApplication.checkSelfPermission(Manifest.permission.BLUETOOTH)!= PackageManager.PERMISSION_GRANTED){
                throw new RuntimeException("BLUETOOTH permission denied");
            }
            if(mApplication.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)!=PackageManager.PERMISSION_GRANTED){
                throw new RuntimeException("BLUETOOTH_ADMIN permission denied");
            }
        }

        if (!mBluetoothAdapter.isEnabled()) {
            throw new RuntimeException("Bluetooth is disabled");
        }

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    mModuleList.add( new OverridingModule(device));
                    Log.d(logTag, "scanned : "+device.getAddress());
                    //mListener.onScan(mModuleList);
                }
            }
        };
        mApplication.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        isInitiated = true;
    }

    public boolean scan(){
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        return mBluetoothAdapter.startDiscovery();
    }

    public void close(){
        mApplication.unregisterReceiver(mReceiver);
    }

    public interface OnScanListener{
        void onScan(ArrayList<OverridingModule> modules);
    }
}
