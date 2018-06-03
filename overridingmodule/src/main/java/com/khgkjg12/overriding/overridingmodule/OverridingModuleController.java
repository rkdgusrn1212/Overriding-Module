package com.khgkjg12.overriding.overridingmodule;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatCallback;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OverridingModuleController{

    private OverridingModule mOverridingModule;
    private static OverridingModuleController mOverridingModuleController;
    private String LOG_TAG = getClass().getSimpleName();
    private CommunicationThread mConnection;
    private OnConnectListener mOnConnectListener;
    private Handler mHandler;
    private User mUser;
    private OverridingDbHelper mDBHelper;
    private Group mGroup;
    private static final int MESSAGE_READ = 0x0301;

    /**
     * @return 권한이 없거나, 블루투스를 지원하지 않거나, 블루투스가 꺼져있을때 null 반환
     */
    public static OverridingModuleController getInstance(Context context){
        if(mOverridingModuleController == null){
            mOverridingModuleController = new OverridingModuleController(context.getApplicationContext());
        }
        return mOverridingModuleController;
    }
    private OverridingModuleController(Context context){
        mDBHelper = new OverridingDbHelper(context);
        SharedPreferences sp = context.getSharedPreferences("my_profile", Context.MODE_PRIVATE);
        String path = sp.getString("picture",null);
        Uri uri;
        if(path !=null){
            uri = Uri.parse(path);
        }else{
            uri = null;
        }
        mUser = new User(sp.getLong("phone", 0), sp.getString("name", null), uri);
    }

    /**
     * @return  scanner 활성화 성공 여부 반환.
     * */
    public boolean enableScanner(Context context, OverridingModuleScanner.OnScanListener onScanListener){
        OverridingModuleScanner scanner = OverridingModuleScanner.createInstance(context, onScanListener);
        if(scanner != null){
            return true;
        }else{
            return false;
        }
    }

    public void disableScanner(Context context){
        OverridingModuleScanner.destroyInstance(context);
    }

    /**
     * @return 스켄을 시작 못했다면 false 반환
     * */
    public boolean scan(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(context.checkSelfPermission(Manifest.permission.BLUETOOTH)!=PackageManager.PERMISSION_GRANTED|context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)!=PackageManager.PERMISSION_GRANTED|context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(context, RequestPermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("mode",RequestPermissionActivity.MODE_REQUEST_SCAN);
                context.startActivity(intent);
                return false;
            }
        }
        OverridingModuleScanner scanner = OverridingModuleScanner.getInstance();
        if(scanner != null) {
            return scanner.scan();
        }
        return false;
    }

    public boolean connect(Context context, OverridingModule overridingModule){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(context.checkSelfPermission(Manifest.permission.BLUETOOTH)!=PackageManager.PERMISSION_GRANTED|context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)!=PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(context, RequestPermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("mode",RequestPermissionActivity.MODE_REQUEST_CONNECT);
                intent.putExtra("name",overridingModule.mName);
                intent.putExtra("device",overridingModule.mDevice);
                context.startActivity(intent);
                return false;
            }
        }
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
            if(mOnConnectListener!=null){
                mOnConnectListener.onStarted();
            }
            if(mHandler==null) {
                mHandler = new Handler(new InputMessageHandler());
            }
            errorOccured = false;
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

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter != null){
                bluetoothAdapter.cancelDiscovery();
            }

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
            mHandler.getLooper().quitSafely();
            mHandler = null;
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
     * putUser를 이용하여 DB에 저장된 User들을 get 하여 매개변수로 사용하세요
     * @return false if 전번 중복 or 프로필 전번 없음 or 애러
    * */
    public Group createGroup(String name, List<User> friends) {
        if(mUser.mPhone == 0){
            return null;
        }
        if(friends==null ||name==null){
            return null;
        }
        for(int i =0; i< friends.size();i++){
            for(int j=i+1; j<friends.size();j++){
                if(friends.get(i).mPhone == friends.get(j).mPhone){
                    return null;
                }
            }
        }
        friends.add(mUser);
        Group group = new Group(name, friends);
        if(mDBHelper.putGroup(group)){
            return null;
        }else{
            return group;
        }
    }

    /**
     * 먼저 putUser를 이용하여 user 앤트리를 DB에 생성한다음 리턴받은 값으로 joinGroup 하세요.
     * 자신이 포함 안되있어도 일단은 그룹생성은 됨. 그러나 실행은 안될거임. 할라면 해당 전화번호를 가진 계정으로 프로필을 바꿔야함.
     * */
    public Group joinGroup(String essid, String name, Map<User, String> ipTable, Map<Long, User> userTable){
        if(essid==null|| name == null|| ipTable==null||userTable==null){
            return null;
        }
        Group group = new Group(essid,name,ipTable,userTable);
        if(mDBHelper.putGroup(group)){
            return null;
        }else{
            return group;
        }
    }

    public boolean startGroupVoiceChat(Group group){
        if(mGroup!=null){
            return false;
        }
        String ip = group.getIPAddress(mUser);
        if(ip == null){
            return false;
        }
        if(mConnection!=null){
            String str = "EWST "+ip+" "+group.mEssid+" -1";
            mConnection.write(str.getBytes());
            mGroup = group;
            return true;
        }else{
            return false;
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

    public void putGroup(Group group){
        if(group != null){
            mDBHelper.putGroup(group);
        }
    }

    public List<Group> getGroupList(){
        return mDBHelper.getGroupList();
    }


    public Group getCurrentGroup(){
        return mGroup;
    }

    public void setCurrentUser(Context context, String phone, String name, Uri picture){
        SharedPreferences sp = context.getSharedPreferences("my_profile", Context.MODE_PRIVATE);
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


    public void setOnConnectListener(OverridingModuleController.OnConnectListener onConnectListener){
        mOnConnectListener = onConnectListener;
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

    public void destroy(Context context){
        disableScanner(context);
    }
}