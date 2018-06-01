package com.khgkjg12.overriding.overridingmodule;

import android.support.annotation.NonNull;
import android.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Group {
    String mEssid;
    String mName;
    Map<User, String> ipTable; // K : address V: User;
    static final String baseIp = "10.10.100.";

    Group(String name, Set<User> users){
        mEssid = UUID.randomUUID().toString().replaceAll("-","").substring(0,20);
        Log.i(getClass().getSimpleName(), "new Group essid : " + mEssid);
        mName = name;

        ipTable = new HashMap<>();
        int i=0;
        for(User user : users){
            ipTable.put(user, baseIp+(101+i));
        }
    }

    //해당 user가 그룹에 없으면 null
    String getIPAddress(User user){
        return ipTable.get(user);
    }

    public String getGroupName(){
        return mName;
    }

    public Set<User> getUserSet(){
        return new HashSet<>(ipTable.keySet());
    }
}
