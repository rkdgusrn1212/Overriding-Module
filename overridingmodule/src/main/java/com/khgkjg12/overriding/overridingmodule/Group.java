package com.khgkjg12.overriding.overridingmodule;

import android.util.Log;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

class Group {
    private String mEssid;
    private String mName;
    private int mChannel;
    private Map<String, String> ipTable; // K : phone V: ip-address;

    public Group(String name, ArrayList<User> friends){
        mChannel = 7;
        mEssid = UUID.randomUUID().toString().replaceAll("-","").substring(0,20);
        Log.i(getClass().getSimpleName(), "new Group essid : " + mEssid);
    }
}
