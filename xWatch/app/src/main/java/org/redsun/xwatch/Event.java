package org.redsun.xwatch;

public class Event {

	/*
	 * 事件简讯各段映射 简讯格式=> THREAD_ID：LEVEL：TITLE: DESCRIPTION：RESOUCE_PATH
	 */
	public static final int THREAD_ID = 0;
	public static final int LEVEL = THREAD_ID+1;
	public static final int TITLE = LEVEL+1;
	public static final int DESCRIPTION = TITLE+1;
	public static final int RESOUCE_PATH = DESCRIPTION+1;

	// 存储事件简讯各字段
	private String mBrief;
	private String[] mBriefArray;

	public Event(String brief) {
		mBrief = brief;
		mBriefArray = brief.split(":");
	}

	// 获得简讯各段
	public String getSegment(int segFlag) {
		return mBriefArray[segFlag];
	}

	// 获得简讯
	public String getBrief() {
		return mBrief;
	}

}
