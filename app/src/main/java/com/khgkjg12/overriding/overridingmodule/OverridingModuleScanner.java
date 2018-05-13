package com.khgkjg12.overriding.overridingmodule;


import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * 오버라이딩 모듈을 스캔하는 싱글톤 패턴의 클래스이다.
 * 사용법.
 * 1. 사용전 init()함수에 애플리케이션 컨텍스트를 매개변수로 넘겨서 초기화 시켜준다.
 * 2. 스캔사용.
 * 3. close()로 사용을 종료한다.
 */
public class OverridingModuleScanner {

    private Application mApplication;
    private static OverridingModuleScanner mInstance = new OverridingModuleScanner();
    private BroadcastReceiver mReceiver;

    private OverridingModuleScanner(){};

    public OverridingModuleScanner getInstance(){
        return mInstance;
    }

    public void init(Application app){
        mApplication = app;
        mApplication.registerReceiver(mReceiver, new IntentFilter().addAction());
    }

    public void close(){
        mApplication.unregisterReceiver(mReceiver);
    }
}
