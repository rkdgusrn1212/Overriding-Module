package com.khgkjg12.overriding.overridingmodule;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RequestPermissionActivity extends AppCompatActivity{

    public static final int REQUEST_PERMISSION = 0x0101;
    private ArrayList<String> grantedPermissions = new ArrayList<>();
    private int mode;
    public static final int MODE_REQUEST_CONNECT = 1;
    public static final int MODE_REQUEST_SCAN = 2;
    private Button acceptButton;
    private BluetoothDevice mBluetoothDevice;
    private String mName;
    private ListView permissionListView;
    private PermissionListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_permission_activity);
        Bundle bundle = getIntent().getExtras();

        permissionListView = findViewById(R.id.list_view);
        mode = bundle.getInt("mode");
        mBluetoothDevice = bundle.getParcelable("device");
        mName = bundle.getString("name");

        adapter =  new PermissionListAdapter();
        permissionListView.setAdapter(adapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> requestPermission = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.add(Manifest.permission.BLUETOOTH);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (mode == 2) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
            adapter.updateList(requestPermission);
        }
        acceptButton = findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ArrayList<String> permissions = new ArrayList<>();
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.BLUETOOTH);
                    }else{
                        grantedPermissions.add(Manifest.permission.BLUETOOTH);
                    }
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
                    }else{
                        grantedPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
                    }
                    if(mode == 2) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                        }else{
                            grantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                        }
                    }
                    adapter.updateList(permissions);
                    String[] resultPermission = new String[permissions.size()];

                    for(int i =0; i<resultPermission.length;i++){
                        resultPermission[i] = permissions.get(i);
                    }
                    requestPermissions(
                            resultPermission,
                            REQUEST_PERMISSION);
                    permissions.clear();
                }
            }
        });


    }

    private class PermissionListAdapter extends BaseAdapter {

        private ArrayList<String> permissions = new ArrayList<>();

        private void updateList(List<String> items){
            permissions.clear();
            permissions.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return permissions.size();
        }

        @Override
        public Object getItem(int position) {
            return permissions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.permission_list_item, parent, false);
            }
            ImageView iconView = convertView.findViewById(R.id.permission_icon);
            TextView nameView =convertView.findViewById(R.id.permission_name);
            TextView explainView = convertView.findViewById(R.id.permission_explain);
            PackageManager pm = getApplicationContext().getPackageManager();
            try {
                PermissionInfo info = pm.getPermissionInfo(permissions.get(position), 0);
                try{
                    PermissionGroupInfo groupInfo = pm.getPermissionGroupInfo(info.group, 0);
                    iconView.setImageResource(groupInfo.icon);
                }catch(PackageManager.NameNotFoundException e){
                    e.printStackTrace();
                }
                nameView.setText(info.labelRes);
                explainView.setText(info.descriptionRes);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0){
                    int sum = 0;
                    for(int result : grantResults) {
                        if(result == PackageManager.PERMISSION_GRANTED) {
                            sum++;
                        }
                    }
                    if(mode == MODE_REQUEST_SCAN &&sum+grantedPermissions.size()==3) {

                        OverridingModuleController.getInstance(this).scan(this);
                        finish();
                    }else if(mode == MODE_REQUEST_CONNECT &&sum+grantedPermissions.size()==2) {
                        OverridingModuleController.getInstance(this).connect(this, new OverridingModule(mBluetoothDevice, mName));
                        finish();
                    }
                }
            }
        }
    }
}
