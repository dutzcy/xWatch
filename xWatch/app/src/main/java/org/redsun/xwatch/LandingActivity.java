package org.redsun.xwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LandingActivity extends Activity {

	public final static String TAG = "LandingActivity";

	// Message定义
	public final static int PASSWORD_WRONG = 0;
	public final static int SERVER_CONNECTION_FAILD = 1;

	private Context mContext;

	public static Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading);

		mContext = this;

		Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				EditText pwdtext = (EditText) findViewById(R.id.pwd);
				final String pwd = pwdtext.getText().toString();
				EventServerConnection conn = new EventServerConnection(mHandler) {
					@Override
					protected void onPostExecute(Object result) {
						int state = (int) result;
						Log.d(TAG, "state: " + state);
						if (state == 1) {
							// 打开事件列表
							Intent intent = new Intent(mContext,
									MainActivity.class);
							startActivity(intent);
						} else if (state == 0) {
							Message msg = mHandler.obtainMessage(PASSWORD_WRONG);
							mHandler.sendMessage(msg);
						} else {
							Message msg = mHandler
									.obtainMessage(SERVER_CONNECTION_FAILD);
							mHandler.sendMessage(msg);
						}
					}
				};
				conn.switchCurrentService(EventServerConnection.LOADING_SERVER);
                String[] params = { pwd };
                conn.execute(params);
			}
		});

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case EventServerConnection.LOADING_SERVER:
                        int state = (int) msg.obj;
                        Log.d(TAG, "state: " + state);
                        if (state == 1) {
                            // 打开事件列表
                            Intent intent = new Intent(mContext,
                                    MainActivity.class);
                            startActivity(intent);
                        } else if (state == 0) {
                            Log.d("zcy", "Password Wrong!");
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.password_wrong),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(
                                    mContext,
                                    mContext.getResources().getString(
                                            R.string.server_connection_faild),
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                }
            }
        };
    }

	private boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

}
