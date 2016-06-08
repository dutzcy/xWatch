package org.redsun.xwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.io.File;

public class CameraActivity extends Activity {

    private final static String TAG = "CameraActivity";
    private Context mContext;

    private Button mRecordVideoButton, mPlayVideoButton;

    // SurfaceView绘制
    private SurfaceView mSFV;
    private SurfaceHolder mSFH;

    private Handler mHandler;
    private Thread mThread;
    private boolean mIsRunning = true; // 用于线程控制
    private final static int SAVE_VIDEO_FINISHED = 1;

    // 视频存储路径
    private final static String mImagePath = "/sdcard/xWatch/video/";

    private class MySurfaceView extends SurfaceView {

        public MySurfaceView(Context context) {
            super(context);
        }
    }

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
                    case EventServerConnection.SWITCH_RECORD_VIDEO:
                        boolean result = (boolean) msg.obj;
                        if (result) {
                            Log.d("zcy", "开始摄制视频~");
                            mRecordVideoButton
                                    .setText(R.string.button_stop_record_video);
                        } else {
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

                                                    String filename = mImagePath + "test.avi";

                                                    // 先删除老文件
                                                    File fs = new File(filename);
                                                    fs.delete();

                                                    EventServerConnection conn = new EventServerConnection(mHandler);
                                                    conn.switchCurrentService(EventServerConnection.LOAD_VIDEO);
                                                    String[] params = { filename };
                                                    conn.execute(params);
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
                        break;

                    case EventServerConnection.CAPTURE_FRAME:
                        Bitmap frame = (Bitmap) msg.obj;
                        //Bitmap frame = null;

                        if (frame != null) {
                            Log.d("zcy", "draw frame~");
                            drawFrame(frame);
                        }
                        break;

                    case EventServerConnection.LOAD_VIDEO:
                        if ((boolean) msg.obj)
                        Toast.makeText(mContext, "视频保存成功！", Toast.LENGTH_SHORT)
                                .show();
                        break;

                    default:

                }
            }
        };

        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (mIsRunning) {
                        // 获取帧
                        EventServerConnection conn = new EventServerConnection(mHandler);
                        conn.switchCurrentService(EventServerConnection.CAPTURE_FRAME);
                        String[] params = {};
                        Log.d(TAG, "Running EventServerConnection.CAPTURE_FRAME");
                        conn.execute(params);
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mThread.start();
    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            EventServerConnection conn = new EventServerConnection(mHandler);
            if (v == mRecordVideoButton) {
                Log.d("zcy", "点击设置视频按钮!");

                String state = mRecordVideoButton.getText().toString();
                if (state.equals(getResources().getString(
                        R.string.button_record_video))) {
                    conn.switchCurrentService(EventServerConnection.SWITCH_RECORD_VIDEO);
                    String[] params = { "true" };
                    conn.execute(params);
                } else if (state.equals(getResources().getString(
                        R.string.button_stop_record_video))) {
                    conn.switchCurrentService(EventServerConnection.SWITCH_RECORD_VIDEO);
                    String[] params = { "false" };
                    conn.execute(params);
                }
            } else if (v == mPlayVideoButton) {
                Log.d("zcy", "点击播放视频按钮!");

                //android获取一个用于打开视频文件的intent
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.putExtra("oneshot", 0);
                intent.putExtra("configchange", 0);
                Uri uri = Uri.fromFile(new File(mImagePath + "test.avi"));
                intent.setDataAndType(uri, "video/avi");
                startActivity(intent);
            }
        }
    }

    /*
     * 绘制指定区域
     */
    void drawFrame(Bitmap frame) {

        int[] location = new int[2];
        mSFV.getLocationOnScreen(location);
        Canvas canvas = mSFH.lockCanvas(new Rect(location[0], location[1],
                location[0] + 480, location[1] + 480));// 关键:获取画布
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
        mIsRunning = false;
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
