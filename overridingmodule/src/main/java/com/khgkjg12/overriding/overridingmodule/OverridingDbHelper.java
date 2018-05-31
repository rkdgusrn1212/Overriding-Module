package com.khgkjg12.overriding.overridingmodule;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

class OverridingDbHelper extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Overriding.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
                    UserEntry._ID + " INTEGER PRIMARY KEY," +
                    UserEntry.COLUMN_NAME_NAME + " TEXT," +
                    UserEntry.COLUMN_NAME_PICTURE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME;

    OverridingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static class GroupEntry implements BaseColumns{
        private static final String TABLE_NAME = "group";
        private static final String COLUMN_NAME_TITLE = "title";
        private static final String COLUMN_NAME_SUBTITLE = "subtitle";
    }

    private static class UserEntry implements BaseColumns{
        private static final String TABLE_NAME = "user";
        private static final String COLUMN_NAME_NAME = "name";
        private static final String COLUMN_NAME_PICTURE = "picture";
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
