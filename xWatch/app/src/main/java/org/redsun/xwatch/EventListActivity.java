package org.redsun.xwatch;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventListActivity extends ListActivity {

	private final static String TAG = "EventListActivity";

	// Event 图像存储路径
	private final static String mImagePath = "/sdcard/xWatch/image/";

	private Intent mWatchServiceIntent = new Intent("com.zcy.android.WATCH");
	private EventServerConnection mServerConn;

	// 消息处理
	public static Handler mHandler;

	private EventListAdapter mEventListAdapter;
	private Cursor mEventCursor;
	private EventDBService mEventDBService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// 获取事件
				switch (msg.what) {
					case EventServerConnection.NEW_EVENT_COMMING:
						Log.d(TAG, "NEW_EVENT_COMMING");
						addData(msg.getData());
						// mEventListAdatper.notifyDataSetChanged(); //更新事件列表
						break;
					case EventServerConnection.LOAD_IMAGE:
						Bitmap bmp = (Bitmap) msg.obj;
						if (bmp != null) {
                            SimpleDateFormat simpleDateFormat;
                            simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                            Date date = new Date();
                            String filename = simpleDateFormat.format(date);
							String filepath = mImagePath + filename;
							saveBitmap(bmp, filepath);
						}
						break;
					default:
				}
			}
		};

		File file = new File(mImagePath);
		file.mkdirs();

		// 开启服务器监测程序
		mServerConn = new EventServerConnection(mHandler);
		//mServerConn.startServerProcess(ServerProcess.WATCH_DOG);

		// 开启事件监听服务
		Bundle bundle = new Bundle();
		bundle.putInt("op", 0);
		mWatchServiceIntent.putExtras(bundle);
        mWatchServiceIntent.setPackage(getPackageName());
		startService(mWatchServiceIntent);

		mEventDBService = new EventDBService(this);

		// 查询数据--未读事件
		mEventCursor = mEventDBService.query(
				"select * from event where state='1'", null);
		if (mEventCursor != null) {
			// 绑定数据
			mEventListAdapter = new EventListAdapter(this,
					R.layout.event_list_item, mEventCursor, new String[] {
							"_id", "title", "description" }, new int[] {
							R.id.key_id, R.id.title, R.id.description });
			setListAdapter(mEventListAdapter);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);

		TextView idView = (TextView) v.findViewById(R.id.key_id);
		String _id = idView.getText().toString();
		String[] args = { _id };
		Log.d("zcy", "_id=" + _id);

		// 下载并保存图片
		Cursor cursor = mEventDBService.query(
				"select * from event where _id=?", args);
		int sourcePathIndex = cursor.getColumnIndex("source_path");
		Log.d("zcy", "sourcePathIndex==" + sourcePathIndex);

		if (cursor.moveToNext()) {
			String file = cursor.getString(sourcePathIndex);
			mServerConn.switchCurrentService(EventServerConnection.LOAD_IMAGE);
			String[] params = { file };
			mServerConn.execute(params);
		}
		cursor.close();

		// 标记事件数据为已读
		ContentValues values = new ContentValues();
		values.put("state", "0"); // 标记为已读

		mEventDBService.update("event", values, "_id=?", args);

		// 更新列表
		new RefreshList().execute();
	}

	private void saveBitmap(Bitmap bm, String filename) {
		String sdStatus = Environment.getExternalStorageState();
		if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
			Log.v("TestFile", "SD card is not avaiable/writeable right now.");
			return;
		}

		FileOutputStream b = null;
		try {
			b = new FileOutputStream(filename);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				b.flush();
				b.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d("zcy", "onDestroy-database closed!");
		mEventCursor.close(); // 我们在onCreate()中没有关闭游标，因为需要和ListView进行数据关联，关闭curosr，会导致List无数据，故在最后释放资源
		mEventDBService.close(); // 断开和数据库的连接，释放相关资源
		//mServerConn.stopServerProcess(ServerProcess.WATCH_DOG);
		stopService(mWatchServiceIntent);
	}

	// 步骤1：通过后台线程AsyncTask来读取数据库，放入更换Cursor
	private class RefreshList extends AsyncTask<Void, Void, Cursor> {
		// 步骤1.1：在后台线程中从数据库读取，返回新的游标newCursor
		protected Cursor doInBackground(Void... params) {
			Cursor newCursor = mEventDBService.query(
					"select * from event where state='1'", null);
			return newCursor;
		}

		// 步骤1.2：线程最后执行步骤，更换adapter的游标，并将原游标关闭，释放资源
		protected void onPostExecute(Cursor newCursor) {
			mEventListAdapter.changeCursor(newCursor);// 网上看到很多问如何更新ListView的信息，采用CusorApater其实很简单，换cursor就可以
			mEventCursor.close();
			mEventCursor = newCursor;
		}
	}

	// 步骤1：通过后台线程AsyncTask来读取数据库，放入更换Cursor
	private class LoadEventImage extends AsyncTask<Void, Void, Cursor> {
		// 步骤1.1：在后台线程中从数据库读取，返回新的游标newCursor
		protected Cursor doInBackground(Void... params) {
			Cursor newCursor = mEventDBService.query(
					"select * from event where state='1'", null);
			return newCursor;
		}

		// 步骤1.2：线程最后执行步骤，更换adapter的游标，并将原游标关闭，释放资源
		protected void onPostExecute(Cursor newCursor) {
			mEventListAdapter.changeCursor(newCursor);// 网上看到很多问如何更新ListView的信息，采用CusorApater其实很简单，换cursor就可以
			mEventCursor.close();
			mEventCursor = newCursor;
		}
	}

	// 步骤2：取缔requrey的方式，采用后台线程更新形式
	private void addData(Bundle bundle) {

		// 获取事件简讯各个字段内容
		String brief = bundle.getString("eventBrief");
		Event event = new Event(brief);

		// 将事件简讯记录入数据库
		ContentValues values = new ContentValues();
		values.put("thread_id", event.getSegment(Event.THREAD_ID));
		values.put("level", event.getSegment(Event.LEVEL));
		values.put("title", event.getSegment(Event.TITLE));
		values.put("description", event.getSegment(Event.DESCRIPTION));
		values.put("source_path", event.getSegment(Event.RESOUCE_PATH));
		values.put("state", "1"); // 设置未读

		Log.d("zcy", "insert da length="
				+ event.getSegment(Event.RESOUCE_PATH).length());
		mEventDBService.insert("event", null, values);

		// cursor.requery();
		new RefreshList().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.event_list_activity_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		switch (item.getItemId()) {
		// 响应每个菜单项(通过菜单项的ID)
		case R.id.menu_clear_all:
			// 标记事件数据为已读
			String[] args = { "1" };
			ContentValues values = new ContentValues();
			values.put("state", "0"); // 标记为已读

			mEventDBService.update("event", values, "state=?", args);

			// 更新列表
			new RefreshList().execute();

			break;
		default:
			// 对没有处理的事件，交给父类来处理
			return super.onOptionsItemSelected(item);
		}
		// 返回true表示处理完菜单项的事件，不需要将该事件继续传播下去了
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("提示");
			builder.setMessage("确定要退出？");
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							finish();
						}
					});
			builder.setNegativeButton("取消",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			builder.create().show();
			return true;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}
}
