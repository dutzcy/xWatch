package org.redsun.xwatch.xwatch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.kobjects.base64.Base64;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.FileOutputStream;
import java.io.IOException;

public class EventServerConnection {

	private final static String TAG = "EventServerConnection";

	private static EventServerConnection mServerConn;

	public final static String HOST_NAME = "http://192.168.2.110";
	//public final static String HOST_NAME = "http://muhaha.oicp.net";
	public final static String SERVICE_FILE = "/nusoap/server.php";

	// 已注册的WebService
	private final static String API_LOADING_SERVER = "loadingServer";
	private final static String API_START_SERVER = "startServer";
	private final static String API_CHECK_SERVER = "checkServer";
	private final static String API_LOAD_IMAGE = "loadImage";
	private final static String API_LOAD_VIDEO = "loadVideo";
	private final static String API_CAPTURE_FRAME = "captureFrame";
	private final static String API_SWITCH_RECORD_VIDEO = "switchRecordVideo";
	private final static String API_STOP_SERVER = "stopServer";

	private String mPWD = ""; // 登录服务器密码

	public class ServerProcess {
		public final static String WATCH_DOG = "WatchDog";
		public final static String CAPTURE_FRAME = "CaptureFrame";
		// ...
	}

	private EventServerConnection() {
	}

	synchronized static EventServerConnection getInstance() {
		if (mServerConn == null)
			mServerConn = new EventServerConnection();

		return mServerConn;
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

	// 登录事件服务器
	public int loadingEventServer(String pwd) {

		String[] params = { pwd };
		Object a = callWebService(API_LOADING_SERVER, params);
		String result = a.toString();
		Log.d(TAG, "loading..." + result);

		if (result.equals("true")) {
			mPWD = pwd; // 保存密码
			return 1;
		} else if (result.equals("false")) {
			return 0;
		} else
			return -1;
	}

	// 开启某个监听服务
	public void startServerProcess(String name) {

		String[] params = { mPWD, name };
		Object a = callWebService(API_START_SERVER, params);
		String result = a.toString();

		Log.d(TAG, "start server: " + result);
	}

	// 加载事件图像
	public Bitmap loadEventImage(String filepath) {

		Bitmap bitmap = null;

		String[] params = { mPWD, filepath };
		Object a = callWebService(API_LOAD_IMAGE, params);

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
	public boolean switchRecordVideo(boolean flag) {

		String[] params = { mPWD, (flag ? "true" : "false") };
		Object a = callWebService(API_SWITCH_RECORD_VIDEO, params);

		if (a != null && a.toString().equals("true"))
			return true;
		else
			return false;
	}

	// 加载并保存视频(分块加载与保存)
	public boolean loadAndSaveVideo(String filepath, int offset, int size) {

		String[] params = { mPWD, Integer.toString(offset),  Integer.toString(size)};
		Object a = callWebService(API_LOAD_VIDEO, params);

		if (a != null) {
			// 解码使用Base64编码的图像数据
			String content = a.toString();
			if (content.equals("end"))  //文件写入完毕
				return true;
			byte[] data = Base64.decode(content);

			// 将byte数组写入文件
			try {
				FileOutputStream fos = new FileOutputStream(filepath, true); // 追加方式写
				fos.write(data);
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}

		return false;
	}

	// 检查有无事件发生
	public Object checkEventServer() {

		Object event = null;
		String[] params = { mPWD };
		event = callWebService(API_CHECK_SERVER, params);

		return event;
	}

	// 停止某个监听服务
	public boolean stopServerProcess(String name) {

		String[] params = { mPWD, name };
		Object a = callWebService(API_STOP_SERVER, params);
		if (a != null)
			return true;
		else
			return false;
	}
}
