package com.khgkjg12.overriding.overridingmodule;

import android.bluetooth.BluetoothDevice;
import android.os.Parcelable;

public class OverridingModule {

    BluetoothDevice mDevice;
    String mName;

    OverridingModule(BluetoothDevice device){
        mDevice = device;
        mName = device.getAddress();
    }

    public String getName(){
        return mName;
    }

    public boolean setName(String name){
        if(name != null&&name.length()>0){
            mName = name;
            return true;
        }else{
            return false;
        }
    }
}
