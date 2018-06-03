package com.khgkjg12.overriding.overridingmodule;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OverridingModuleScanner {

    private BroadcastReceiver mReceiver;
    private final String LOG_TAG = getClass().getSimpleName();
    private OnScanListener mListener;
    private static OverridingModuleScanner mScanner;

    /**
     * @return 이미 인스턴스가 존재하면 null을 반환.
     **/
    static OverridingModuleScanner createInstance(Context context, OnScanListener onScanListener){
        if(mScanner != null) {
            return null;
        }
        mScanner = new OverridingModuleScanner(context.getApplicationContext(), onScanListener);
        return mScanner;
    }

    static OverridingModuleScanner getInstance(){
        return mScanner;
    }

    private OverridingModuleScanner(Context context, OnScanListener onScanListener){
        mListener = onScanListener;
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
        context.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }
    /**
     * scan()을 호출할 엑티비티에서 동적권한 할당과 Bluetooth 켜기를 구현해야함.
     * @return 스켄이 시작되었는지 여부를 반환
     */
    boolean scan(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            return false;
        }
        if(!bluetoothAdapter.enable()){
            return false;
        };
        if(bluetoothAdapter.isDiscovering()){
            Log.i(LOG_TAG, "scanning is progressing... cancel scanning");
            bluetoothAdapter.cancelDiscovery();

        }

        Log.i(LOG_TAG, "scanning start");

        if(bluetoothAdapter.startDiscovery()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
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

    /**
     * @return false if no Instance;
     * */
    static void destroyInstance(Context context){
        if(mScanner!=null){
            mScanner.destroy(context);
        }
    }

    /**
     * destory scanner
     * */
    private void destroy(Context context){
        context.getApplicationContext().unregisterReceiver(mReceiver);
        mScanner = null;
    }

    public interface OnScanListener{
        void onScanEachModule(OverridingModule module);
        void onScanPairedList(List<OverridingModule> modules);
    }
}
