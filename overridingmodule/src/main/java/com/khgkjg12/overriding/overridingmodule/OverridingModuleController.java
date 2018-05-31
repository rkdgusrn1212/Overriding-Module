package com.khgkjg12.overriding.overridingmodule;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OverridingModuleController{

    private OverridingModule mOverridingModule;
    private BluetoothAdapter mBluetoothAdapter;
    private static OverridingModuleController mOverridingModuleController;
    private OverridingModuleScanner mScanner;
    private String LOG_TAG = getClass().getSimpleName();
    private Application mApplication;
    private OnScanListener mListener;
    private CommunicationThread mConnection;
    private OnConnectListener mOnConnectListener;
    private Handler mHandler;
    private User mUser;
    private OverridingDbHelper mDBHelper;
    private Group mGroup;
    public static final int REQUEST_PERMISSION_BLUETOOTH = 0x0101;
    public static final int REQUEST_PERMISSION_BLUETOOTH_ADMIN = 0x0102;
    public static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 0x0103;
    public static final int REQUEST_ENABLE_BT = 0x0201;
    private static final int MESSAGE_READ = 0x0301;

    /**
     * @return 권한이 없거나, 블루투스를 지원하지 않거나, 블루투스가 꺼져있을때 null 반환
     */
    public static OverridingModuleController getInstance(final AppCompatActivity appCompatActivity) throws RuntimeException{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (appCompatActivity.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                if (appCompatActivity.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)) {
                    PermissionExplainDialog.newInstance("블루투스 권한", "디바이스의 블루투스를 사용하기 위해 필요합니다.", new PermissionExplainDialog.OnResultListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void agreeToPermissionExplainDialog() {
                            appCompatActivity.requestPermissions(
                                    new String[]{Manifest.permission.BLUETOOTH}, REQUEST_PERMISSION_BLUETOOTH);
                        }
                    }).show(appCompatActivity.getSupportFragmentManager(), "permissionexplaindialog");
                    return null;
                } else {
                    appCompatActivity.requestPermissions(
                            new String[]{Manifest.permission.BLUETOOTH},
                            REQUEST_PERMISSION_BLUETOOTH);
                    return null;
                }
            }

            if (appCompatActivity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                if (appCompatActivity.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_ADMIN)) {
                    PermissionExplainDialog.newInstance("블루투스 관리자 권한", "디바이스의 블루투스를 사용하기 위해 필요합니다.", new PermissionExplainDialog.OnResultListener() {

                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void agreeToPermissionExplainDialog() {
                            appCompatActivity.requestPermissions(
                                    new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSION_BLUETOOTH_ADMIN);
                        }
                    }).show(appCompatActivity.getSupportFragmentManager(), "permissionexplaindialog");
                    return null;
                } else {
                    appCompatActivity.requestPermissions(
                            new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                            REQUEST_PERMISSION_BLUETOOTH_ADMIN);
                    return null;
                }
            }

            if (appCompatActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (appCompatActivity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    PermissionExplainDialog.newInstance("위치데이터 접근 권한", "안드로이드 버전 M부터 블루투스 디바이스의 스켄을 위해 사용됩니다.", new PermissionExplainDialog.OnResultListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void agreeToPermissionExplainDialog() {
                            appCompatActivity.requestPermissions(
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
                        }
                    }).show(appCompatActivity.getSupportFragmentManager(), "permissionexplaindialog");
                    return null;
                } else {
                    appCompatActivity.requestPermissions(
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
                    return null;
                }
            }
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            return null;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            appCompatActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return null;
        }
        if(mOverridingModuleController == null){
            mOverridingModuleController = new OverridingModuleController(appCompatActivity, bluetoothAdapter);
        }
        return mOverridingModuleController;
    }


    private OverridingModuleController(AppCompatActivity appCompatActivity, BluetoothAdapter bluetoothAdapter){
        mHandler = new Handler(new InputMessageHandler());
        mApplication = appCompatActivity.getApplication();
        mBluetoothAdapter = bluetoothAdapter;
        mDBHelper = new OverridingDbHelper(mApplication);
        mScanner = new OverridingModuleScanner(mApplication, mBluetoothAdapter);
        SharedPreferences sp = mApplication.getSharedPreferences("my_profile", Context.MODE_PRIVATE);
        String path = sp.getString("picture",null);
        Uri uri;
        if(path !=null){
            uri = Uri.parse(path);
        }else{
            uri = null;
        }
        mUser = new User(sp.getLong("phone", 0), sp.getString("name", null), uri);
    }

    public void setOnScanListener(OnScanListener onScanListener){
        mScanner.setOnScanListener(onScanListener);
    }

    public void scannerOn(){
        mScanner.open();
    }

    public void scannerOff(){
        mScanner.close();
    }

    public boolean scan(){
        return mScanner.scan();
    }

    public boolean connect(OverridingModule overridingModule){
        //모듈 멤버 초기화, 연결해제 등등

        int bondState =  overridingModule.mDevice.getBondState();
        if(bondState==BluetoothDevice.BOND_BONDED){
            Log.d("hyungu","BONDED");
            ConnectThread connectThread = new ConnectThread(overridingModule);
            connectThread.start();
            return true;
        }else if(bondState==BluetoothDevice.BOND_NONE){
            overridingModule.mDevice.createBond();
        }
        return false;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OverridingModule mmModule;
        private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//UUID for SerialPortServiceClass_UUID
        private boolean errorOccured;

        private ConnectThread(OverridingModule module) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            errorOccured = false;
            if(mOnConnectListener!=null){
                mOnConnectListener.onStarted();
            }
            BluetoothSocket tmp = null;
            mmModule = module;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                tmp = module.mDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                errorOccured = true;
                if(mOnConnectListener!=null){
                    mOnConnectListener.onError();
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            if (errorOccured) {
                return;
            }
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                mOverridingModule = mmModule;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnConnectListener.onSuccess();
                    }
                });
            } catch (IOException connectException) {
                if (mOnConnectListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnConnectListener.onError();
                        }
                    });
                }
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {

                }
                return;
            }

            mConnection = new CommunicationThread(mmSocket);
        }
    }

    private class CommunicationThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean isError;

        CommunicationThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            isError = false;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                isError = true;
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if(isError){
                close();
                return;
            }
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
            close();
        }

        /* Call this from the main activity to send data to the remote device */
        private void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        private void close() {
            mConnection = null;
            try {
                mmSocket.close();
            } catch (IOException e) { }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnConnectListener.onClosed();
                }
            });
        }
    }

    private class InputMessageHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what ==MESSAGE_READ) {
                return true;
            }
            return false;
        }
    }

    /**
     * 자신을 제외한 다른 구성원들을 friends로 전달.
    * */
    public void createGroup(String name, List<User> friends) {
        if(mUser.mPhone == 0){
            return;
        }
        if(name!=null && friends!=null) {
            friends.add(mUser);
            joinGroup(new Group(name, friends));
        }
    }

    public void joinGroup(Group group){
        if(mGroup!=null){
            return;
        }
        String ip = group.getIPAddress(mUser);
        if(ip == null){
            return;
        }
        if(mConnection!=null){
            String str = "EWST "+ip+" "+group.mEssid+" -1";
            mConnection.write(str.getBytes());
            mGroup = group;
        }
    }

    public void leaveGroup(){
        if(mGroup == null){
            return;
        }
        if(mConnection!=null){
            String str = "EWST "+Group.baseIp+"100 "+""+" -1";
            mConnection.write(str.getBytes());
            mGroup = null;
        }
    }

    public Group getCurrentGroup(){
        return mGroup;
    }

    public void setCurrentUser(String phone, String name, Uri picture){
        SharedPreferences sp = mApplication.getSharedPreferences("my_profile", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        if(phone!=null) {
            if(mGroup==null){
                mUser.mPhone = Long.valueOf(phone);
                edit.putLong("phone", mUser.mPhone);
            }
        }
        if(name!=null){
            mUser.mName = name;
            edit.putString("name", mUser.mName);
            if(mConnection!=null&&mGroup!=null){
                mConnection.write(("NMBR "+name+" -1").getBytes());
            }
        }
        if(picture!=null){
            mUser.mPicture = picture;
            edit.putString("picture", mUser.mPicture.getPath());
            if(mConnection!=null&&mGroup!=null){
                mConnection.write(("IMBR "+name+" -1").getBytes());
            }
        }
        edit.apply();
    }

    public User getCurrentUser(){
        return mUser;
    }

    public void putUser(String phone, String name, Uri picture){
        if(phone != null&&phone.length()>0){
            mDBHelper.putUser(new User(Long.valueOf("1"+phone) ,name, picture));
        }
    }

    public void updateUser(User user){

    }

    public void deleteUser(User user){

    }

    public List<User> getUserList(){
        return mDBHelper.getUserList();
    }

    public void setOnConnectListener(OnConnectListener onConnectListener){
        mOnConnectListener = onConnectListener;
    }

    public interface OnScanListener{
        void onScanEachModule(OverridingModule module);
        void onScanPairedList(List<OverridingModule> modules);
    }

    public interface OnConnectListener{
        @AnyThread
        void onError();
        void onStarted();
        @AnyThread
        void onSuccess();
        @AnyThread
        void onClosed();
    }
}