package com.khgkjg12.overriding.overridingmodule;

import android.content.pm.PermissionGroupInfo;
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
    User(long phone, String name, String picture){
        mPhone = phone;
        mName = name;
        if(picture == null){
            mPicture  = null;
        }else{
            mPicture = Uri.parse(picture);
        }
    }

    public String getPicturePath(){
        if(mPicture == null){
            return null;
        }else{
            return mPicture.getPath();
        }
    }

    void setPicture(String path){
        if(path == null){
            mPicture = null;
        }else{
            mPicture = Uri.parse(path);
        }
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
