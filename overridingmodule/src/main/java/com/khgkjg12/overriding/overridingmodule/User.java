package com.khgkjg12.overriding.overridingmodule;

import android.net.Uri;

public class User {
    long mPhone;
    String mName;
    Uri mPicture;


    User(long phone, String name, Uri picture){
        mPhone = phone;
        mName = name;
        mPicture = picture;
    }

    public String getPhone(){
        return Long.toString(mPhone).substring(1);
    }

    public String getName(){
        return mName;
    }

    public Uri getPicture(){
        return mPicture;
    }
}
