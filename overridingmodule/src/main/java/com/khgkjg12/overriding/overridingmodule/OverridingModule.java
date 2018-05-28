package com.khgkjg12.overriding.overridingmodule;

import android.bluetooth.BluetoothDevice;

public class OverridingModule {

    BluetoothDevice mDevice;
    Group mGroup;

    OverridingModule(BluetoothDevice device){
        mDevice = device;
    };

    String getMac(){
        return mDevice.getAddress();
    }

    BluetoothDevice getGroup(){
        return mDevice;
    }

    void setGroup(){

    }

    public BluetoothDevice getName(){

    }
}
