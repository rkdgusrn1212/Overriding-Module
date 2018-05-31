package com.khgkjg12.overriding;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.khgkjg12.overriding.overridingmodule.OverridingModule;
import com.khgkjg12.overriding.overridingmodule.OverridingModuleController;
import com.khgkjg12.overriding.overridingmodule.User;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OverridingModuleController.OnScanListener {


    private ListView mListView1;
    private ListView mListView2;
    private Button mScanButton;
    private OverridingModuleController mController;
    private TextView mProfileView;
    private Button mButton2;
    private Button mButton3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mListView2 = findViewById(R.id.listview2);
        mListView2.setAdapter(new ModuleListAdapter(new ArrayList<OverridingModule>()));
        mListView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mController.connect((OverridingModule) parent.getItemAtPosition(position));
            }
        });

        mListView1 = findViewById(R.id.listview1);
        mListView1.setAdapter(new ModuleListAdapter(new ArrayList<OverridingModule>()));
        mListView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mController.connect((OverridingModule) parent.getItemAtPosition(position));
            }
        });


        mScanButton = findViewById(R.id.button);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
        mProfileView = findViewById(R.id.profile);
        mButton2 = findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.setProfile("01011111111","더미", Uri.parse("amude"));
                User user = mController.getProfile();
                mProfileView.setText("phone:"+user.getPhone()+",name:"+user.getName());
            }
        });
        mButton3 = findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.putUser("34827273737","더미1",null);
                mController.putUser("12347328473","더미2" , null);
                mController.createGroup("더미 그룹", mController.getUserList());
                User user = mController.getProfile();
                mProfileView.setText("phone:"+user.getPhone()+",name:"+user.getName());
            }
        });
        init();
    }

    private void init(){
        mController = OverridingModuleController.getInstance(this);
        if(mController != null) {
            User user = mController.getProfile();
            mProfileView.setText("phone:"+user.getPhone()+",name:"+user.getName());
            mController.scannerOn();
            mController.setOnScanListener(this);
            mController.setOnConnectListener(new OverridingModuleController.OnConnectListener() {
                @Override
                public void onError() {
                    Toast.makeText(getApplicationContext(), "애러가 발생하였습니다.",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStarted() {

                    Toast.makeText(getApplicationContext(), "연결 시작.",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess() {

                    Toast.makeText(getApplicationContext(), "연결 성공.",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onClosed() {
                    Toast.makeText(getApplicationContext(), "연결 종료.",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void scan(){
        if(mController==null){
            init();
            return;
        }
        mController.scan();
    }

    @Override
    public void onScanEachModule(OverridingModule module) {
        ((ModuleListAdapter)mListView2.getAdapter()).addModule(module);
    }

    @Override
    public void onScanPairedList(List<OverridingModule> modules) {
        ((ModuleListAdapter)mListView1.getAdapter()).updateList(modules);
    }

    private class ModuleListAdapter extends BaseAdapter{

        private ArrayList<OverridingModule> list;

        ModuleListAdapter(ArrayList<OverridingModule> list) {
            this.list = list;
        }

        void updateList(List<OverridingModule> list){
            this.list.clear();
            this.list.addAll(list);
            notifyDataSetChanged();
        }

        void addModule(OverridingModule module){
            list.add(module);
            notifyDataSetChanged();
        }

        void clearList(){
            list.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = getLayoutInflater().inflate(R.layout.main_activity_listview_item, parent,false);
            }
            TextView textView = convertView.findViewById(R.id.textview);
            textView.setText(list.get(position).getName());
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        mController.scannerOff();
        super.onDestroy();
    }
}
