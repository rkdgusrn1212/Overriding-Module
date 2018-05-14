package com.khgkjg12.overriding;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.khgkjg12.overriding.overridingmodule.OverridingModule;
import com.khgkjg12.overriding.overridingmodule.OverridingModuleScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends AppCompatActivity implements OverridingModuleScanner.OnScanListener{


    private ListView mListView;
    private Button mScanButton;
    private final int SCAN_PERMISSION = 0;
    private final static int ENABLE_BLUETOOTH = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mListView = findViewById(R.id.listview);
        mListView.setAdapter(new ModuleListAdapter(new ArrayList<OverridingModule>()));

        mScanButton = findViewById(R.id.button);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
        OverridingModuleScanner scanner = OverridingModuleScanner.getInstance();
        scanner.init(getApplication());
        scanner.setOnScanListner(this);
    }

    private void scan(){
        if(checkScanPermission()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter.isEnabled()){
                ((ModuleListAdapter)mListView.getAdapter()).clearList();
                OverridingModuleScanner scanner = OverridingModuleScanner.getInstance();
                scanner.scan();
            }else{
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH);
            }
        }
    }

    private boolean checkScanPermission(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionList = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (permissionList.size() > 0) {

                requestPermissions(Arrays.copyOf(permissionList.toArray(), permissionList.size(), String[].class), SCAN_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==ENABLE_BLUETOOTH){
            if(resultCode == RESULT_OK){
                scan();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == SCAN_PERMISSION){
            boolean isGranted = true;
            for(int i=0 ;i<grantResults.length; i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                }
            }
            if(isGranted){
                scan();
            }
        }
    }

    @Override
    public void onScan(OverridingModule module) {
        ((ModuleListAdapter)mListView.getAdapter()).addModule(module);
    }

    private class ModuleListAdapter extends BaseAdapter{

        private ArrayList<OverridingModule> list;

        ModuleListAdapter(ArrayList<OverridingModule> list) {
            this.list = list;
        }

        void updateList(ArrayList<OverridingModule> list){
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
            textView.setText(list.get(position).getMac());
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        OverridingModuleScanner scanner = OverridingModuleScanner.getInstance();
        scanner.close();
        super.onDestroy();
    }
}
