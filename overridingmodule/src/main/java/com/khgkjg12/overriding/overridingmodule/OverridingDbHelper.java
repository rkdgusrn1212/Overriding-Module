package com.khgkjg12.overriding.overridingmodule;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OverridingDbHelper extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Overriding.db";

    private static final String CREATE_TABLE_USER =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
                    UserEntry._ID + " INTEGER PRIMARY KEY," +
                    UserEntry.COLUMN_NAME_NAME + " TEXT," +
                    UserEntry.COLUMN_NAME_PICTURE + " TEXT)";

    private static final String CREATE_TABLE_GROUP =
            "CREATE TABLE " + GroupEntry.TABLE_NAME + " (" +
                    GroupEntry.COLUMN_NAME_ESSID + " TEXT PRIMARY KEY, " +
                    GroupEntry.COLUMN_NAME_NAME + " TEXT)";
    private static final String CREATE_TABLE_GROUP_USER =
            "CREATE TABLE " + GroupUserEntry.TABLE_NAME +
                    " (" + GroupUserEntry.COLUMN_NAME_GROUP_ID +
                    " TEXT," + GroupUserEntry.COLUMN_NAME_USER_ID +
                    " INTEGER,"+GroupUserEntry.COLUMN_NAME_IP +
                    " TEXT, PRIMARY KEY("+GroupUserEntry.COLUMN_NAME_GROUP_ID+", "+GroupUserEntry.COLUMN_NAME_USER_ID+
                    "), FOREIGN KEY("+GroupUserEntry.COLUMN_NAME_USER_ID +") REFERENCES "+UserEntry.TABLE_NAME+"("+UserEntry._ID+") ON DELETE CASCADE ON UPDATE CASCADE, FOREIGN KEY(" +
                    GroupUserEntry.COLUMN_NAME_GROUP_ID+") REFERENCES "+GroupEntry.TABLE_NAME+"("+GroupEntry.COLUMN_NAME_ESSID+") ON DELETE CASCADE ON UPDATE CASCADE )";

    private static final String DELETE_TABLE_USER =
            "DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME;

    private static final String DELETE_TABLE_GROUP =
            "DROP TABLE IF EXISTS "+ GroupEntry.TABLE_NAME;
    private static final String DELETE_TABLE_GROUP_USER =
            "DROP TABLE IF EXISTS "+ GroupUserEntry.TABLE_NAME;


    OverridingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_GROUP);
        db.execSQL(CREATE_TABLE_GROUP_USER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DELETE_TABLE_USER);
        db.execSQL(DELETE_TABLE_GROUP);
        db.execSQL(DELETE_TABLE_GROUP_USER);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    //pk = essid;
    private static class GroupEntry{
        private static final String TABLE_NAME = "group_table";
        private static final String COLUMN_NAME_NAME = "name";
        private static final String COLUMN_NAME_ESSID = "essid";
    }

    //pk = ip
    private static class GroupUserEntry{
        private static final String TABLE_NAME = "group_user";
        private static final String COLUMN_NAME_GROUP_ID = "group_id";
        private static final String COLUMN_NAME_USER_ID = "user_id";
        private static final String COLUMN_NAME_IP = "ip";
    }

    private static class UserEntry implements BaseColumns{
        private static final String TABLE_NAME = "user";
        private static final String COLUMN_NAME_NAME = "name";
        private static final String COLUMN_NAME_PICTURE = "picture";
    }

    boolean putGroup(Group group){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(GroupEntry.COLUMN_NAME_ESSID, group.mEssid);
        values.put(GroupEntry.COLUMN_NAME_NAME, group.mName);

        long result = db.insert(GroupEntry.TABLE_NAME, null, values);
        if(result == -1){
            return false;
        }
        for(Map.Entry<User, String> entry : group.ipTable.entrySet()){

            values =  new ContentValues();
            values.put(GroupUserEntry.COLUMN_NAME_GROUP_ID, group.mEssid);
            values.put(GroupUserEntry.COLUMN_NAME_GROUP_ID, entry.getKey().mPhone);
            values.put(GroupUserEntry.COLUMN_NAME_IP, entry.getValue());
            result = db.insert(GroupUserEntry.TABLE_NAME, null, values);
            if(result == -1){
                return false;
            }
        }

        db.close();
        return true;
    }

    /**
     *
     * @return null if 없으면
     */
    List<Group> getGroupList(){
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                GroupEntry.COLUMN_NAME_ESSID,
                GroupEntry.COLUMN_NAME_NAME,
        };
        String sortOrder =
                GroupEntry.COLUMN_NAME_NAME + " ASC";

        Cursor cursor = db.query(
                GroupEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        List<Group> groups = new ArrayList<>();
        while(cursor.moveToNext()) {
            String essid = cursor.getString(
                    cursor.getColumnIndexOrThrow(GroupEntry.COLUMN_NAME_ESSID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_NAME_NAME));
            String MY_QUERY = "SELECT * FROM "+
                    GroupUserEntry.TABLE_NAME+" a INNER JOIN "+UserEntry.TABLE_NAME+" b ON a."+GroupUserEntry.COLUMN_NAME_USER_ID+"=b."+UserEntry._ID+" WHERE a."+GroupUserEntry.COLUMN_NAME_GROUP_ID+"=?";

            Cursor cursorUser = db.rawQuery(MY_QUERY, new String[]{essid});
            Map<User, String> users = new HashMap<>();
            Map<Long, User> userTable = new HashMap<>();
            while(cursorUser.moveToNext()) {
                long phone = cursorUser.getLong(
                        cursorUser.getColumnIndexOrThrow(UserEntry._ID));
                String ip = cursorUser.getString(cursorUser.getColumnIndexOrThrow(GroupUserEntry.COLUMN_NAME_IP));
                String userName = cursorUser.getString(cursorUser.getColumnIndexOrThrow(UserEntry.COLUMN_NAME_NAME));
                String picture = cursorUser.getString(cursorUser.getColumnIndexOrThrow(UserEntry.COLUMN_NAME_PICTURE));
                User user = new User(phone, userName, picture);
                users.put(user,ip);
                userTable.put(phone, user);
            }
            groups.add(new Group(essid,name, users, userTable));
        }
        cursor.close();
        db.close();
        return groups;
    }

    /**
    *@return -1 if 이미 존재할때, 또는 트렌젝션 애러.
     */
    long putUser(User user){
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserEntry._ID, user.mPhone);
        values.put(UserEntry.COLUMN_NAME_NAME, user.mName);
        if(user.mPicture != null){
            values.put(UserEntry.COLUMN_NAME_PICTURE, user.mPicture.getPath());
        }

        long newRowId = db.insert(UserEntry.TABLE_NAME, null, values);

        db.close();
        return newRowId;
    }

    /**
     *
     * @return 성공여부
     */
    boolean deleteUser(User user){
        SQLiteDatabase db = getWritableDatabase();
        String selection = UserEntry._ID + " = ?";
        String[] selectionArgs = { Long.toString(user.mPhone) };
        int deletedRows = db.delete(UserEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
        if(deletedRows == 1){
            return true;
        }else{
            return false;
        }
    }

    boolean updateUser(User user, String name, String picture){
        SQLiteDatabase db = getWritableDatabase();

        String title = "MyNewTitle";
        ContentValues values = new ContentValues();
        values.put(UserEntry.COLUMN_NAME_NAME, name);
        values.put(UserEntry.COLUMN_NAME_PICTURE, picture);
        String selection = UserEntry._ID + " = ?";
        String[] selectionArgs = { Long.toString(user.mPhone) };

        int count = db.update(
                UserEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
        if(count == 1){
            return true;
        }else{
            return false;
        }
    }

    List<User> getUserList(){
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                BaseColumns._ID,
                UserEntry.COLUMN_NAME_NAME,
                UserEntry.COLUMN_NAME_PICTURE
        };
        String sortOrder =
                UserEntry.COLUMN_NAME_NAME + " ASC";

        Cursor cursor = db.query(
                UserEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        List<User> users = new ArrayList<>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(UserEntry._ID));
            String itemName = cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_NAME_NAME));
            String itemPicture = cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_NAME_NAME));
            users.add(new User(itemId,itemName, Uri.parse(itemPicture)));
        }
        cursor.close();
        db.close();
        return users;
    }
}
