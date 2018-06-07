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
import android.content.pm.PermissionGroupInfo;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private String filePath;

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
        filePath = context.getFilesDir().getPath();
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

        private boolean receiveImage = false;
        private User receiveUser = null;
        private File tempImage;
        private int imageSize;
        @Override
        public boolean handleMessage(Message msg) {
            if(mGroup==null){
                return false;
            }
            if(msg.what ==MESSAGE_READ) {
                byte[] buffer = (byte[])msg.obj;
                if(msg.arg1>3){
                    String order = "";
                    int i = 0;
                    for(i = 0;i<4;i++){
                        order+=buffer;
                    }
                    if(order.equals("NMUP")){
                        String name="";
                        String ip="";
                        i++;
                        while(buffer[i]!=' '){
                            name+=buffer[i];
                            i++;
                        }
                        i++;
                        while(buffer[i]!=' '){
                            ip+=buffer[i];
                            i++;
                        }
                        User user = mGroup.ipKeyTable.get(ip);
                        putUser(user.getPhone(), name, user.mPicture);
                        return true;
                    }
                    if(order.equals("IMUP")){
                        String imgSize="";
                        String ip="";
                        i++;
                        while(buffer[i]!=' '){
                            imgSize+=buffer[i];
                            i++;
                        }
                        i++;
                        while(buffer[i]!=' '){
                            ip+=buffer[i];
                            i++;
                        }
                        receiveUser = mGroup.ipKeyTable.get(ip);
                        imageSize = Integer.getInteger(imgSize);
                        tempImage = new File(filePath+"/"+receiveUser.getPhone()+".jpeg");
                        receiveImage = true;
                        return true;
                    }
                }
                if(receiveImage){
                    try {
                        FileOutputStream outputStream = new FileOutputStream(tempImage);
                        int i =0;
                        while(i<1024){
                            outputStream.write(buffer[i]);
                            imageSize--;
                            i++;
                            if(imageSize<1){
                                putUser(receiveUser.getPhone(),receiveUser.mName, Uri.fromFile(tempImage));
                                receiveUser = null;
                                receiveImage = false;
                                outputStream.close();
                                return true;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
            return false;
        }
    }

    public Group createGroupWithPhones(String name, List<String> phoneList){
        if(mUser.mPhone == 0){
            return null;
        }
        if(phoneList==null ||name==null){
            return null;
        }
        phoneList.add(mUser.getPhone());
        List<User> users = new ArrayList<>();
        for(int i =0; i< phoneList.size();i++){
            if(phoneList.get(i)==null||phoneList.get(i).length()==0){
                return null;
            }
            for(int j=i+1; j<phoneList.size();j++){
                if(phoneList.get(j)==null||phoneList.get(j).length()==0){
                    return null;
                }
                if(phoneList.get(i).equals(phoneList.get(j))){
                    return null;
                }
            }
            users.add(new User(Long.valueOf("1"+phoneList.get(i)),null, (Uri) null));
        }

        return createGroup(name, users);
    }

    /**
     * 자신을 제외한 다른 구성원들을 friends로 전달.
     * putUser를 이용하여 DB에 저장된 User들을 get 하여 매개변수로 사용하세요, 자기 자신도 저장해야함. 이때 user 는 phone 속성만 가지고 있으면 됨.
     * @return false if 전번 중복 or 프로필 전번 없음 or 애러
    * */
    public Group createGroupWithUsers(String name, List<User> friends) {
        if(mUser.mPhone == 0){
            return null;
        }
        if(friends==null ||name==null){
            return null;
        }
        List<User> users = new ArrayList<>(friends);
        users.add(mUser);
        for(int i =0; i< users.size();i++){
            if(users.get(i)==null){
                return null;
            }
            for(int j=i+1; j<users.size();j++){
                if(users.get(j)==null){
                    return null;
                }
                if(users.get(i).mPhone == users.get(j).mPhone){
                    return null;
                }
            }
        }
        return createGroup(name, users);
    }

    private Group createGroup(String name, List<User> users){
        for(int i=0;i<users.size();i++){
            mDBHelper.putUser(users.get(i));
            User user =mDBHelper.getUser(users.get(i).getPhone());
            if(user == null){
                return null;
            }
            users.set(i,user);
        }
        Group group = new Group(name, users);
        if(mDBHelper.putGroup(group)){
            return null;
        }else{
            return group;
        }
    }

    /**
     * ipTable의 키는 전번 값은 아이피
     * 자신이 포함 안되있어도 일단은 그룹생성은 됨. 그러나 실행은 안될거임. 할라면 해당 전화번호를 가진 계정으로 프로필을 바꿔야함.
     * */
    public Group joinGroup(String essid, String name, Map<String, String> ipTable){
        if(essid==null|| name == null|| ipTable==null){
            return null;
        }
        Map<User, String> userIpTable = new HashMap<>();
        Map<Long, User> userTable = new HashMap<>();
        for(Map.Entry<String, String> entry : ipTable.entrySet()){
            putUser(entry.getKey(),null,null);
            User user = mDBHelper.getUser(entry.getKey());
            userIpTable.put(user, entry.getValue());
            userTable.put(user.mPhone, user);
        }
        Group group = new Group(essid,name,userIpTable,userTable);
        if(mDBHelper.putGroup(group)){
            return null;
        }else{
            return group;
        }
    }

    public boolean openGroup(Group group){
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

    public boolean closeGroup(){
        if(mGroup == null){
            return false;
        }
        if(mConnection!=null){
            String str = "EWST "+Group.baseIp+"100 "+""+" -1";
            mConnection.write(str.getBytes());
            mGroup = null;
            return true;
        }else{
            return false;
        }
    }
    /**
    * @return -1 fail, 0 can't delete current chat group, 1 is success
    * */
    public int leaveGroup(Group group){
        if(group==null) {
            return -1;
        }
        if(mGroup!=null&&mGroup.mEssid.equals(group.mEssid)){
            return 0;
        }
        if(mDBHelper.deleteGroup(group)){
            return 1;
        }else{
            return -1;
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


    public Group getCurrentChatGroup(){
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
                byte[] buffer = new byte[1024];
                try {
                    File file = new File("file://"+picture.getPath());
                    FileInputStream fileInputStream = new FileInputStream(file);
                    BufferedInputStream bufferedStream = new BufferedInputStream(fileInputStream);
                    while (bufferedStream.read(buffer, 0, 1024) != -1) {
                        mConnection.write(buffer);
                    }
                    bufferedStream.close();
                    fileInputStream.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
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

    public boolean startVoiceChat(){
        if(mConnection!=null&&mGroup!=null){
            mConnection.write(("CALL -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean stopVoiceChat(){
        if(mConnection!=null&&mGroup!=null){
            mConnection.write(("CLOF -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean upMicVolumn(int volumn){
        if(mConnection!=null){
            mConnection.write(("MVST "+volumn+" -1").getBytes());
            return true;
        }else{
            return false;
        }
    }
    public boolean upMicVolumn(){
        if(mConnection!=null){
            mConnection.write(("MVUP -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean downMicVolumn(){
        if(mConnection!=null){
            mConnection.write(("MVDW -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean upSpeakerVolumn(){
        if(mConnection!=null){
            mConnection.write(("VLUP -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean downSpeakerVolumn(){
        if(mConnection!=null){
            mConnection.write(("VLDW -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    public boolean setSpeakerVolumn(int volumn){
        if(mConnection!=null){
            mConnection.write(("VLST "+volumn+" -1").getBytes());
            return true;
        }else{
            return false;
        }
    }

    /**
     *
     * @param user 변경하고자 하는 User 객체
     * @param name null 입력시 이름 제거
     * @param picturePath null입력시 기본 이미지로 변경.
     * @return 성공여부
     */
    public boolean updateUser(User user, String name, String picturePath){
        if(!mDBHelper.updateUser(user, name, picturePath)){
            return false;
        }
        if(mGroup!=null){
            User groupedUser = mGroup.userTable.get(user.mPhone);
            if(groupedUser!=null){
                groupedUser.mName = name;
                groupedUser.setPicture(picturePath);
            }
        }
        return true;
    }
    public boolean updateUserName(User user, String name){
        User savedUser = mDBHelper.getUser(user.getPhone());
        if(savedUser == null){
            return false;
        }
        String path = savedUser.getPicturePath();
        if(!mDBHelper.updateUser(user, name, path)){
            return false;
        }
        if(mGroup!=null){
            User groupedUser = mGroup.userTable.get(user.mPhone);
            if(groupedUser!=null){
                groupedUser.mName = name;
                groupedUser.setPicture(path);
            }
        }
        return true;
    }
    public boolean updateUserPicture(User user, String picturePath){
        User savedUser = mDBHelper.getUser(user.getPhone());
        if(savedUser == null){
            return false;
        }
        String name = savedUser.mName;
        if(!mDBHelper.updateUser(user, name, picturePath)){
            return false;
        }
        if(mGroup!=null){
            User groupedUser = mGroup.userTable.get(user.mPhone);
            if(groupedUser!=null){
                groupedUser.mName = name;
                groupedUser.setPicture(picturePath);
            }
        }
        return true;
    }


    public boolean deleteUser(User user){
        return mDBHelper.deleteUser(user);
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