package com.khgkjg12.overriding.overridingmodule;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Group {
    String mEssid;
    private String mName;
    private Map<Long, String> ipTable; // K : phone V: ip-address;
    static final String baseIp = "10.10.100.";

    Group(String name, List<User> users){
        mEssid = UUID.randomUUID().toString().replaceAll("-","").substring(0,20);
        Log.i(getClass().getSimpleName(), "new Group essid : " + mEssid);

        ipTable = new HashMap<>();
        for(int i = 0; i<users.size(); i++){
            ipTable.put(users.get(i).mPhone, baseIp+(101+i));
        }
    }

    //해당 user가 그룹에 없으면 null
    String getIPAddress(User user){
        return ipTable.get(user.mPhone);
    }
}
