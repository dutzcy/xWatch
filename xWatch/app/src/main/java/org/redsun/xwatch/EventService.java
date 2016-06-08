package org.redsun.xwatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class EventService extends Service implements Runnable {

    private final static String TAG = "EventService";
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

	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case EventServerConnection.CHECK_SERVER:
					if (msg.obj != null) {
						String result = msg.obj.toString();
						Log.d("zcy", "serviece - length" + result.length());
						String[] eventBriefs = result.split("\n");

						for (int i = 0; i < eventBriefs.length; i++) {
							Event event = new Event(eventBriefs[i]);
							showNotification(event.getSegment(Event.TITLE));

							// 通知EventListActivity保存事件信息并更新列表视图
							Message msg1 = EventListActivity.mHandler
									.obtainMessage(EventServerConnection.NEW_EVENT_COMMING);
							Bundle b = new Bundle();
							b.putString("eventBrief", event.getBrief());
							msg1.setData(b);

							EventListActivity.mHandler.sendMessage(msg1);
						}
					}
                    break;
                default:
            }
            ;
        }
    };

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
                NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

                if (activeNetwork != null) { // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi
                        Toast.makeText(context, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                        success = true;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        Toast.makeText(context, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                        success = true;
                    }
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
                while (mIsRunning) {
                    EventServerConnection conn = new EventServerConnection(mHandler);
                    conn.switchCurrentService(EventServerConnection.CHECK_SERVER);
                    String[] params = {};
                    conn.execute(params);
                    Log.d(TAG, "Running EventServerConnection.CHECK_SERVER");
                    Thread.sleep(1000);
                }

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
