package com.redsun.xwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EventDBService extends SQLiteOpenHelper {

	private final static int DATABASE_VERSION = 1;
	private final static String DATABASE_NAME = "evnet.db";

	public EventDBService(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public EventDBService(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Log.d("zcy", "database onCreate()");
		// 创建事件Event表
		String sql = "CREATE TABLE [event]("
				+ "[_id]INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "[state]INTEGER," // Event的状态，如已读(0)、未读(1)...
				+ "[thread_id]INTEGER,"
				+ "[level]INTEGER,"
				+ "[title]VARCHAR2[64],"
				+ "[description]VARCHAR2(128),"
				+ "[source_path]VARCHAR2(128))";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	public Cursor query(String sql, String[] args) {

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, args);

		return cursor;
	}

	public void insert(String table, String nullColumnHack, ContentValues values) {

		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(table, nullColumnHack, values);
	}

	public void update(String table, ContentValues values, String whereClause,
			String[] whereArgs) {

		SQLiteDatabase db = this.getWritableDatabase();
		db.update(table, values, whereClause, whereArgs);
	}
}
