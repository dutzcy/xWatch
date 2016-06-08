package org.redsun.xwatch;

import java.io.FileOutputStream;
import java.io.IOException;

import org.kobjects.base64.Base64;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class EventServerConnection extends AsyncTask<String[], Integer, Object> {

	private final static String TAG = "EventServerConnection";

	public final static String HOST_NAME = "http://10.239.17.68";
	//public final static String HOST_NAME = "http://muhaha.oicp.net";
	public final static String SERVICE_FILE = "/nusoap/server.php";

    // 已注册的WebService
    public final static int DEFAULT = 0;
    public final static int LOADING_SERVER = 1;
    public final static int START_SERVER = 2;
    public final static int CHECK_SERVER = 3;
	public final static int NEW_EVENT_COMMING = 4;
    public final static int LOAD_IMAGE = 5;
    public final static int LOAD_VIDEO = 6;
    public final static int CAPTURE_FRAME = 7;
    public final static int SWITCH_RECORD_VIDEO = 8;
    public final static int STOP_SERVER = 9;

	private final static String API_LOADING_SERVER = "loadingServer";
	private final static String API_START_SERVER = "startServer";
	private final static String API_CHECK_SERVER = "checkServer";
	private final static String API_LOAD_IMAGE = "loadImage";
	private final static String API_LOAD_VIDEO = "loadVideo";
	private final static String API_CAPTURE_FRAME = "captureFrame";
	private final static String API_SWITCH_RECORD_VIDEO = "switchRecordVideo";
	private final static String API_STOP_SERVER = "stopServer";

	private static String mPWD = ""; // 登录服务器密码
    private int mWebService = DEFAULT;
    private Handler mHandler = null;

	public class ServerProcess {
		public final static String WATCH_DOG = "WatchDog";
		public final static String CAPTURE_FRAME = "CaptureFrame";
		// ...
	}

    public EventServerConnection(Handler handler) {
        mHandler = handler;
    }

    synchronized public void switchCurrentService(int webService) {
        mWebService = webService;
    }

	@Override
	protected Object doInBackground(String[]... params) {
        Object result = null;

        switch (mWebService) {
            case LOADING_SERVER:
                result = loadingEventServer(params[0]);
                break;
            case START_SERVER:
                result = startServerProcess(params[0]);
                break;
            case CHECK_SERVER:
                result = checkEventServer();
                break;
            case LOAD_IMAGE:
                result = loadEventImage(params[0]);
                break;
            case LOAD_VIDEO:
                result = loadAndSaveVideo(params[0]);
				break;
			case CAPTURE_FRAME:
				result = captureFrame();
				break;
			case SWITCH_RECORD_VIDEO:
				result = switchRecordVideo(params[0]);
				break;
			case STOP_SERVER:
				result = stopServerProcess(params[0]);
				break;
			case DEFAULT:
				break;

        }

		return result;
	}

	@Override
	protected void onPostExecute(Object result) {
        Message msg = new Message();
        msg.what = mWebService;
        msg.obj = result;
        mHandler.sendMessage(msg);
	}

	// 调用WebService API并返回Object
	private Object callWebService(String api, String[] params) {

		Object result = null;

		try {
			Log.d(TAG, "call " + api);
			SoapObject rpc = new SoapObject(HOST_NAME, api);

			for (int i = 0; i < params.length; i++) {
				rpc.addProperty("param" + i, params[i]);
			}
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.bodyOut = rpc;
			HttpTransportSE ht = new HttpTransportSE(HOST_NAME + SERVICE_FILE);
			ht.debug = true;

			ht.call(HOST_NAME + "/" + api, envelope);
			result = (Object) envelope.getResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

    private String[] addPWD2Params(String[] params) {
        String[] newParams = new String[params.length + 1];
        newParams[0] = mPWD;
        for (int i=0; i<params.length; i++) {
            newParams[i] = params[i];
        }
        return newParams;
    }

	// 登录事件服务器
	public Integer loadingEventServer(String[] params) {

		Object a = callWebService(API_LOADING_SERVER, params);
		String result = a.toString();
		Log.d(TAG, "loading..." + result);

		if (result.equals("true")) {
			mPWD = params[0]; // 保存密码
			return 1;
		} else if (result.equals("false")) {
			return 0;
		} else
			return -1;
	}

	// 开启某个监听服务
	public String startServerProcess(String[] params) {

		Object a = callWebService(API_START_SERVER, addPWD2Params(params));
		String result = a.toString();

		Log.d(TAG, "start server: " + result);
        return result;
	}

	// 加载事件图像
	public Bitmap loadEventImage(String[] params) {

		Bitmap bitmap = null;
		Object a = callWebService(API_LOAD_IMAGE, addPWD2Params(params));

		// 解码使用Base64编码的图像数据
		String content = a.toString();
		if (content != null) {
			byte[] data = Base64.decode(content);
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		}

		return bitmap;
	}

	// 快速抓取摄像头帧
	public synchronized Bitmap captureFrame() {

		Bitmap bitmap = null;

		String[] params = { mPWD };
		Object a = callWebService(API_CAPTURE_FRAME, params);

		// 解码使用Base64编码的图像数据
		if (a != null) {
			String content = a.toString();
			byte[] data = Base64.decode(content);
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		}

		return bitmap;
	}

	// 切换录制视频的开关函数
	public Boolean switchRecordVideo(String[] params) {

		Object a = callWebService(API_SWITCH_RECORD_VIDEO, addPWD2Params(params));

		if (a != null && a.toString().equals("true"))
			return true;
		else
			return false;
	}

	// 加载并保存视频(分块加载与保存)
	public Boolean loadAndSaveVideo(String[] params) {
        int offset = 0;
        int size = 100 * 1024; //一次100k
        Object a = null;

		do {
            String[] tempParams = { mPWD, Integer.toString(offset), Integer.toString(size)};
            a = callWebService(API_LOAD_VIDEO, params);

			// 解码使用Base64编码的图像数据
			String content = a.toString();
			if (content.equals("end"))  //文件写入完毕
				return true;
			byte[] data = Base64.decode(content);

			// 将byte数组写入文件
			try {
				FileOutputStream fos = new FileOutputStream(params[0], true); // 追加方式写
				fos.write(data);
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                return false;
			}

            offset += size;
		} while (a != null);

		return true;
	}

	// 检查有无事件发生
	public Object checkEventServer() {

		Object event = null;
		String[] params = { mPWD };
		event = callWebService(API_CHECK_SERVER, params);

		return event;
	}

	// 停止某个监听服务
	public Boolean stopServerProcess(String[] params) {

		Object a = callWebService(API_STOP_SERVER, addPWD2Params(params));
		if (a != null)
			return true;
		else
			return false;
	}
}
