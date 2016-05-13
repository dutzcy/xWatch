package org.redsun.xwatch.xwatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.redsun.xwatch.R;

public class EventService extends Service implements Runnable {

	private final int ID_NOTIFICATION = 1;
	private NotificationManager mNotiManager;

	private int mStartMode; // indicates how to behave if the service is killed
	private IBinder mBinder; // interface for clients that bind
	private boolean mAllowRebind; // indicates whether onRebind should be used

	// 监测服务器事件
	private Thread mWatchThread;
	private boolean mIsRunning = true; // 用于线程控制
	private BroadcastReceiver mNetworkStateReceiver;
	private static boolean mFirstInit = true;

	@Override
	public void onCreate() {
		// The service is being created
		Log.d("zcy", "onCreate");
		mNotiManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mWatchThread = new Thread(this);
		mWatchThread.start();

		// 注册网络监听
		mNetworkStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				Log.d("zcy", "网络状态改变");
				boolean success = false;

				// 获得网络连接服务
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				// State state = connManager.getActiveNetworkInfo().getState();
				State state = connManager.getNetworkInfo(
						ConnectivityManager.TYPE_WIFI).getState(); // 获取网络连接状态
				if (State.CONNECTED == state) { // 判断是否正在使用WIFI网络
					success = true;
				}

				state = connManager.getNetworkInfo(
						ConnectivityManager.TYPE_MOBILE).getState(); // 获取网络连接状态
				if (State.CONNECTED == state) { // 判断是否正在使用GPRS网络
					success = true;
				}

				if (!success) {
					Toast.makeText(context, "您的网络连接已中断", Toast.LENGTH_LONG)
							.show();
					if (mWatchThread.isAlive())
						mWatchThread.suspend();
				} else {
					// 开始监测
					if (!mWatchThread.isAlive()) {
						if (mFirstInit) {
							mWatchThread.start();
							mFirstInit = false;
						} else
							mWatchThread.resume();
					}
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mNetworkStateReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
		Log.d("zcy", "onStartCommand");

		return mStartMode;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// A client is binding to the service with bindService()
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// All clients have unbound with unbindService()
		return mAllowRebind;
	}

	@Override
	public void onRebind(Intent intent) {
		// A client is binding to the service with bindService(),
		// after onUnbind() has already been called
	}

	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed
		Log.d("zcy", "Service-onDestroy");
		mNotiManager.cancel(ID_NOTIFICATION);
		mIsRunning = false;
		unregisterReceiver(mNetworkStateReceiver); // 取消监听
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (mIsRunning) {
			try {
				Object a = EventServerConnection.getInstance().checkEventServer();

				if (a != null) {
					String result = a.toString();
					Log.d("zcy", "serviece - length" + result.length());
					String[] eventBriefs = result.split("\n");
					
					for (int i = 0; i < eventBriefs.length; i++) {
						Event event = new Event(eventBriefs[i]);
						showNotification(event.getSegment(Event.TITLE));

						// 通知EventListActivity保存事件信息并更新列表视图
						Message msg = EventListActivity.mHandler
								.obtainMessage(EventListActivity.NEW_EVENT_COMMING);
						Bundle b = new Bundle();
						b.putString("eventBrief", event.getBrief());
						msg.setData(b);

						EventListActivity.mHandler.sendMessage(msg);
					}
				}

				Thread.sleep(1000);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void showNotification(String message) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

		// 设置通知的基本信息：icon、标题、内容
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle(message);
		builder.setContentText(message);
		builder.setDefaults(Notification.DEFAULT_ALL);

        // 设置通知的点击行为：这里启动一个 Activity
		Intent intent = new Intent(this, EventListActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

        // 发送通知 id 需要在应用内唯一
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION, builder.build());
	}
}
