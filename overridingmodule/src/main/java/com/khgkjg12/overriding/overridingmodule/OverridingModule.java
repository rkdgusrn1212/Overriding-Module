package com.khgkjg12.overriding.overridingmodule;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class OverridingModule{

    BluetoothDevice mDevice;
    String mName;

    OverridingModule(BluetoothDevice device){
        mDevice = device;
        mName = device.getAddress();
    }

    OverridingModule(BluetoothDevice device, String name){
        mDevice = device;
        mName = name;
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
