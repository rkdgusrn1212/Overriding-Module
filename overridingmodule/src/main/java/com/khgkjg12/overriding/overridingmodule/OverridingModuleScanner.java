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

import java.util.ArrayList;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * 오버라이딩 모듈을 스캔하는 싱글톤 패턴의 클래스이다.
 * 사용법.
 * 1. 사용전 init()함수에 애플리케이션 컨텍스트를 매개변수로 넘겨서 초기화 시켜준다.
 * 2. 스캔사용.
 * 3. close()로 사용을 종료한다.
 * onCreate()에서 init()과 listener를 등록하고 onDestroy()에서 close()를 선언하는것을 추천한다.
 */
public class OverridingModuleScanner {

    private Application mApplication;
    private static OverridingModuleScanner mInstance = new OverridingModuleScanner();
    private BroadcastReceiver mReceiver;
    private boolean isInitiated = false;
    private String logTag = getClass().getSimpleName();
    private OnScanListener mListener;

    private OverridingModuleScanner(){};

    public static OverridingModuleScanner getInstance(){
        return mInstance;
    }

    public void init(Application app){
        if(isInitiated){
            Log.i(logTag,"Already initiated!");
            return;
        }
        mApplication = app;

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(logTag, "received something");
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    Log.i(logTag, "scanned : " + device.getAddress());
                    mListener.onScan(new OverridingModule(device));
                }
            }
        };
        mApplication.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        isInitiated = true;
    }

    /**
     * scan()을 호출할 엑티비티에서 동적권한 할당과 Bluetooth 켜기를 구현해야함.
     * @return 스켄이 시작되었는지 여부를 반환
     * @throws RuntimeException
     */
    public boolean scan() throws RuntimeException{
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new RuntimeException("Device does not support Bluetooth");
        }

        /**
         * 동적 권한 확인 로직. 권한 없으면 throw
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(logTag, "version > M, checking permission now");

            if(mApplication.checkSelfPermission(Manifest.permission.BLUETOOTH)!= PackageManager.PERMISSION_GRANTED){
                throw new RuntimeException("BLUETOOTH permission denied");
            }
            if(mApplication.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)!=PackageManager.PERMISSION_GRANTED){
                throw new RuntimeException("BLUETOOTH_ADMIN permission denied");
            }
            if(mApplication.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                throw new RuntimeException("ACCESS_COARSE_LOCATION permission denied");
            }
        }

        /**
        *블루투스가 꺼져있으면 throw
         */
        if (!bluetoothAdapter.isEnabled()) {
            throw new RuntimeException("Bluetooth is disabled!");
        }

        if(bluetoothAdapter.isDiscovering()){
            Log.i(logTag, "scanning is progressing... cancel scanning");
            bluetoothAdapter.cancelDiscovery();
        }
        Log.i(logTag, "scanning start");
        return bluetoothAdapter.startDiscovery();
    }

    public void close(){
        mApplication.unregisterReceiver(mReceiver);
    }

    public void setOnScanListner(OnScanListener onScanListner){
        mListener = onScanListner;
    }

    public interface OnScanListener{
        void onScan(OverridingModule module);
    }
}
