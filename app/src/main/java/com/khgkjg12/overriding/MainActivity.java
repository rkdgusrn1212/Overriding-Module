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

import com.khgkjg12.overriding.overridingmodule.Group;
import com.khgkjg12.overriding.overridingmodule.OverridingModule;
import com.khgkjg12.overriding.overridingmodule.OverridingModuleController;
import com.khgkjg12.overriding.overridingmodule.OverridingModuleScanner;
import com.khgkjg12.overriding.overridingmodule.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OverridingModuleScanner.OnScanListener {


    private ListView mListView1;
    private ListView mListView2;
    private ListView mListView3;
    private ListView mListView4;
    private ListView mListView5;

    private UserListAdapter mUserAdapter3 , mUserAdapter4;

    private GroupListAdapter mGroupAdapter5;
    private Button mScanButton;
    private OverridingModuleController mController;
    private TextView mProfileView;
    private Button mButton2;
    private Button mButton3;
    private Button mButton4;
    private EditText editText1;
    private EditText editText2;

    private TextView currentGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        editText1 = findViewById(R.id.edit1);
        editText2 = findViewById(R.id.edit2);
        currentGroup = findViewById(R.id.current_group);

        mListView2 = findViewById(R.id.listview2);
        mListView2.setAdapter(new ModuleListAdapter(new ArrayList<OverridingModule>()));
        mListView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mController.connect(getApplicationContext(),(OverridingModule) parent.getItemAtPosition(position));
            }
        });

        mListView1 = findViewById(R.id.listview1);
        mListView1.setAdapter(new ModuleListAdapter(new ArrayList<OverridingModule>()));
        mListView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mController.connect(getApplicationContext(), (OverridingModule) parent.getItemAtPosition(position));
            }
        });



        mListView3 = findViewById(R.id.listview3);
        mListView3.setAdapter(mUserAdapter3 = new UserListAdapter());
        mListView3.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    ((UserListAdapter)parent.getAdapter()).deleteUser(position);
            }
        });


        mListView4 = findViewById(R.id.listview4);
        mListView4.setAdapter(mUserAdapter4 = new UserListAdapter());
        mListView4.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mUserAdapter3.addUser((User)parent.getAdapter().getItem(position));
            }
        });

        mListView5 = findViewById(R.id.listview5);
        mListView5.setAdapter(mGroupAdapter5 = new GroupListAdapter());
        mListView5.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mController.startGroupVoiceChat((Group)mGroupAdapter5.getItem(position));
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
                String phone = editText1.getText().toString();
                String name = editText2.getText().toString();
                if(phone.length()>0){
                    mController.setCurrentUser(getApplicationContext(), phone, null , null);
                }
                if(name.length()>0){
                    mController.setCurrentUser(getApplicationContext(), null, name , null);
                }
                User user = mController.getCurrentUser();
                mProfileView.setText("사용자 정보 : phone = "+user.getPhone()+",name = "+user.getName());
            }
        });
        mButton3 = findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = editText1.getText().toString();
                String name = editText2.getText().toString();
                if(phone.length()<1){
                    return;
                }
                if(name.length()<1){
                    name=null;
                }
                mController.putUser(phone,name,null);
                mUserAdapter4.updateList(mController.getUserList());
            }
        });


        mButton4 = findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = editText2.getText().toString();
                if(groupName.length()>0){
                    mController.createGroup(groupName, mUserAdapter3.getList());
                    Group group = mController.getCurrentGroup();
                    if(group!=null){
                        Set<User> groupUsers = group.getUserSet();
                        String str = "현재 그룹: "+group.getGroupName()+" ";
                        for(User user : groupUsers){
                            str +=user.getPhone()+", ";
                        }
                        currentGroup.setText(str);
                        mController.putGroup(group);
                        mGroupAdapter5.updateGroup(mController.getGroupList());
                    }
                }
            }
        });
        mController = OverridingModuleController.getInstance(this);
        User user = mController.getCurrentUser();
        mProfileView.setText("사용자 정보: phone = "+user.getPhone()+",name = "+user.getName());
        mUserAdapter4.updateList(mController.getUserList());
        mController.enableScanner(getApplicationContext(), this);
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

        mGroupAdapter5.updateGroup(mController.getGroupList());
    }

    private void scan(){
        mController.scan(this);
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

    private class UserListAdapter extends BaseAdapter{

        private ArrayList<User> users= new ArrayList<>();;

        void addUser(User user){
            users.add(user);
            notifyDataSetChanged();
        }

        void updateList(List<User> users){
            this.users.clear();
            this.users.addAll(users);
            notifyDataSetChanged();
        }
        void deleteUser(int i){
            users.remove(i);
            notifyDataSetChanged();
        }
        void clearList(){
            users.clear();
            notifyDataSetChanged();
        }

        List<User> getList(){
            return users;
        }

        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public Object getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = getLayoutInflater().inflate(R.layout.main_activity_listview2_item, parent,false);
            }
            TextView textView = convertView.findViewById(R.id.text_phone);
            textView.setText(users.get(position).getPhone());
            TextView textView2 = convertView.findViewById(R.id.text_name);
            textView2.setText(users.get(position).getName());
            return convertView;
        }
    }

    private class GroupListAdapter extends BaseAdapter{

        private ArrayList<Group> groups= new ArrayList<>();;

        void addGroup(Group group){
            groups.add(group);
            notifyDataSetChanged();
        }

        void updateGroup(List<Group> groups){
            this.groups.clear();
            this.groups.addAll(groups);
            notifyDataSetChanged();
        }
        void deleteGroup(int i){
            groups.remove(i);
            notifyDataSetChanged();
        }
        void clearList(){
            groups.clear();
            notifyDataSetChanged();
        }

        List<Group> getList(){
            return groups;
        }

        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Object getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = getLayoutInflater().inflate(R.layout.main_activity_group_list_item, parent,false);
            }
            TextView textView = convertView.findViewById(R.id.text_name);
            textView.setText(groups.get(position).getGroupName());
            return convertView;
        }
    }
    @Override
    protected void onDestroy() {
        mController.destroy(this);
        super.onDestroy();
    }
}
