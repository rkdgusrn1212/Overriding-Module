package com.khgkjg12.overriding.overridingmodule;

import android.bluetooth.BluetoothDevice;

public class OverridingModule {

    BluetoothDevice mDevice;

    OverridingModule(BluetoothDevice device){
        mDevice = device;
    };

    public String getMac(){
        return mDevice.getAddress();
    }
}
