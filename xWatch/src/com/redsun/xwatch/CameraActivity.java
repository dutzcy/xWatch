package com.redsun.xwatch;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CameraActivity extends Activity {

	private Context mContext;

	// 服务器连接实例
	private EventServerConnection mServerConn;

	private Button mRecordVideoButton, mPlayVideoButton;

	// SurfaceView绘制
	private SurfaceView mSFV;
	private SurfaceHolder mSFH;

	private Handler mHandler;
	private final static int FRAME_REFRESH = 0;
	private final static int SAVE_VIDEO_FINISHED = 1;

	// 用于帧抓取线程控制
	private volatile boolean misThreadEnded = false;

	// 视频存储路径
	private final static String mImagePath = "/sdcard/xWatch/video/";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		mRecordVideoButton = (Button) this
				.findViewById(R.id.button_record_video);
		mPlayVideoButton = (Button) this.findViewById(R.id.button_play_video);

		mRecordVideoButton.setOnClickListener(new ClickEvent());
		mPlayVideoButton.setOnClickListener(new ClickEvent());

		mSFV = (SurfaceView) this.findViewById(R.id.surface_frame);
		mSFH = mSFV.getHolder();

		mContext = this;

		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch (msg.what) {
				case FRAME_REFRESH:
					Log.d("zcy", "draw frame~");
					drawFrame((Bitmap) msg.obj);
					break;

				case SAVE_VIDEO_FINISHED:
					Toast.makeText(mContext, "视频保存成功！", Toast.LENGTH_SHORT)
							.show();
					break;

				default:

				}
			}
		};

		// 获得唯一服务连接实例
		mServerConn = EventServerConnection.getInstance();
		// mServerConn.startServerProcess(ServerProcess.CAPTURE_FRAME);
		// //不知为何，使用OpenCV启动不了摄像头
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		misThreadEnded = false;
		new CaptureThread().start();
	}

	class ClickEvent implements View.OnClickListener {

		@Override
		public void onClick(View v) {

			if (v == mRecordVideoButton) {
				Log.d("zcy", "点击设置视频按钮!");
				
				String state = mRecordVideoButton.getText().toString();
				if (state.equals(getResources().getString(
						R.string.button_record_video))) {
					if (mServerConn.switchRecordVideo(true)) {
						Log.d("zcy", "开始摄制视频~");
						mRecordVideoButton
								.setText(R.string.button_stop_record_video);
					}
				} else if (state.equals(getResources().getString(
						R.string.button_stop_record_video))) {

					if (mServerConn.switchRecordVideo(false)) {
						Log.d("zcy", "停止摄制视频~");
						mRecordVideoButton
								.setText(R.string.button_record_video);

						AlertDialog.Builder builder = new AlertDialog.Builder(
								mContext);
						builder.setTitle(R.string.dialog_save_video_title);
						builder.setMessage(R.string.dialog_save_video_message);
						builder.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// 开启新线程保存视频
										Toast.makeText(mContext, "开始保存视频！",
												Toast.LENGTH_SHORT).show();
										new Thread(new Runnable() {

											@Override
											public void run() {
												// TODO Auto-generated method stub
												int offset = 0;
												int size = 100 * 1024; //一次100k
												String filename = mImagePath + "test.avi";
												
												// 先删除老文件
												File fs =new File(filename);
												fs.delete();
												
												while (!(mServerConn.loadAndSaveVideo(filename, offset, size))) {
													offset += size;
												}
												
												Log.d("zcy", "保存视频成功~");
												Message msg = mHandler
														.obtainMessage(SAVE_VIDEO_FINISHED);
												mHandler.sendMessage(msg);
											}
										}).start();
									}
								});
						builder.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								});
						builder.create().show();
					}
				}
			} else if (v == mPlayVideoButton) {
					Log.d("zcy", "点击播放视频按钮!");
					
					//android获取一个用于打开视频文件的intent
					Intent intent = new Intent("android.intent.action.VIEW");
					intent.putExtra("oneshot", 0);
					intent.putExtra("configchange", 0);
					Uri uri = Uri.fromFile(new File(mImagePath+"test.avi"));
					intent.setDataAndType(uri, "video/avi");
					startActivity(intent);
					
				}
			}
	}

	/*
	 * 绘制指定区域
	 */
	void drawFrame(Bitmap frame) {

		Canvas canvas = mSFH.lockCanvas(new Rect(0, 0, 480, 600));// 关键:获取画布
		if (canvas == null)
			return;
		if (frame != null)
			canvas.drawBitmap(frame, 0, 0, null);
		mSFH.unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

		misThreadEnded = true;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
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
							// 代替不安全的 Thread.stop()办法
							misThreadEnded = true;
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

	private class CaptureThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!misThreadEnded) {

				// 获取帧
				Bitmap frame = mServerConn.captureFrame();
				//Bitmap frame = null;

				// 发送消息更新帧
				if (frame != null) {
					Bitmap bmp = Bitmap.createBitmap(frame);
					Message msg = new Message();
					msg.what = FRAME_REFRESH;
					msg.obj = bmp;
					mHandler.sendMessage(msg);
				}
			}
		}
	}
}
