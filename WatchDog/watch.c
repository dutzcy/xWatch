/*
 * watch.c
 *
 *  Created on: 2013-4-17
 *      Author: redsun
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <time.h>
#include <string.h>

#include <sys/unistd.h>
#include <sys/types.h>
#include <winsock2.h>
#include <unistd.h>
#include <errno.h>

#include "opencv2/imgproc/imgproc_c.h"
#include "opencv2/highgui/highgui_c.h"

#include "tools.h"

const CvSize FRAME_SIZE = { 640, 480 };  //摄像头的帧的宽高

//帧的互斥量，用于同步线程
typedef struct Frame_mutex_tag {
	pthread_mutex_t mutex; /* Protects access to value */
	pthread_cond_t cond; /* Signals change to value */
	IplImage* capturedFrame; /* Access protected by mutex */
	int flag; /* 帧有效标志 */
} Frame_mutex_t;

//用于获取帧差图像
IplImage* frame1;
IplImage* frame2;
IplImage* diff;

void* captureService(void* args); //帧抓取线程
void* detectService(void* args);  //监测线程
void* socketService(void* args); //响应php socket请求线程

//用于获取帧和处理帧的互斥量
Frame_mutex_t *frame_lock;

//压缩后的帧数据
uchar* loadFrame;
uchar* sendFrame;
const int frameDataSize = 30 * 1024; //帧数据大小是动态变化的但不会超过30k

// 录制视频开关
CvVideoWriter* camWriter = NULL;
volatile int recordVideoSwitch = 0;

////////////////////////////////////EVENT TEST
// 事件结构体
typedef struct Event_tag {
	int threadId;
	int level;
	char* title;
	char* description;
	char* resouce_path;
} Event;

// Event level
#define ERROR    0
#define WARNING  1
#define INFO     2

// Functions
void* eventService(void* args); //事件捕获线程
Event* produceEvent();
int recordEvent(Event* event);

int main(int argc, char** argv) {

	//互斥量和条件量初始化
	frame_lock = (Frame_mutex_t*) malloc(sizeof(struct Frame_mutex_tag));
	frame_lock->capturedFrame = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 3);
	pthread_mutex_init(&(frame_lock->mutex), NULL); //??这里会不会泄漏内存
	pthread_cond_init(&(frame_lock->cond), NULL);
	frame_lock->flag = -1;

	frame1 = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 3);
	frame2 = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 3);
	diff = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 1);
	sendFrame = (uchar*) malloc(frameDataSize);

	pthread_t t1;
	pthread_t t2;
	//pthread_t t3;
	pthread_t t4;
	pthread_create(&t1, NULL, captureService, NULL);
	pthread_create(&t2, NULL, socketService, NULL);
	//pthread_create(&t3, NULL, eventService, NULL);
	pthread_create(&t4, NULL, detectService, NULL);

	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
	//pthread_join(t3, NULL);
	pthread_join(t4, NULL);

	cvReleaseImage(&frame_lock->capturedFrame);
	cvReleaseImage(&frame1);
	cvReleaseImage(&frame2);
	cvReleaseImage(&diff);

	if (sendFrame != NULL) {
		free(sendFrame);
		sendFrame = NULL;
	}

	return 0;
}

void* captureService(void* args) {

	CvCapture* capture = cvCaptureFromCAM(0);
	IplImage* frame; //捕捉到的图像

	cvNamedWindow("摄像", CV_WINDOW_AUTOSIZE);

	// 初始化两个帧
	frame = cvQueryFrame(capture);
	cvCopy(frame, frame1, NULL);
	cvCopy(frame, frame2, NULL);

	for (;;) {
		frame = cvQueryFrame(capture);
		if (frame != NULL) {

			// 开关打开，记录视频
			if (recordVideoSwitch) {
				cvWriteFrame(camWriter, frame);
			}

			//cvShowImage("摄像", frame);
			cvShowImage("帧差", diff);
			cvShowImage("帧1", frame1);
			cvShowImage("帧2", frame2);

			// 捕获到的帧数据存入加载帧
			pthread_mutex_lock(&(frame_lock->mutex)); //加锁

			cvCopy(frame, frame_lock->capturedFrame, NULL);
			frame_lock->flag = 1;

			//pthread_cond_signal(&(frame_lock->cond)); //处理锁->发信号
			pthread_mutex_unlock(&(frame_lock->mutex)); //解锁

			int key = cvWaitKey(20);
			if (key == 27)
				break;
		}
	}

	cvReleaseCapture(&capture);
	cvDestroyWindow("摄像");

	exit(0);
	return NULL;
}

void* socketService(void* args) {

	int sock;
	struct sockaddr_in server, client;
	int recvd, snd;
	unsigned long int structlength;
	char * server_ip = "127.0.0.1";
	int port = 8888;
	char recvbuf[2];

	//Winsows下启用socket
	WSADATA wsadata;
	if (WSAStartup(MAKEWORD(1,1),&wsadata) == SOCKET_ERROR)
	{
	    printf("WSAStartup() fail\n");
	    exit(0);
	}

	memset((char *) &server, 0, sizeof(server));
	server.sin_family = AF_INET;
	server.sin_addr.s_addr = inet_addr(server_ip);
	server.sin_port = htons(port);

	memset((char *) &client, 0, sizeof(client));
	client.sin_family = AF_INET;
	client.sin_addr.s_addr = htonl(INADDR_ANY);
	client.sin_port = htons(port);

	if ((sock = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
		printf("socket create error!\n");
		exit(1);
	}

	structlength = sizeof(server);
	if (bind(sock, (struct sockaddr *) &server, structlength) < 0) {
		printf("socket bind error!\n");
		perror("bind");
		exit(1);
	}

	for (;;) {

		structlength = sizeof(client);

		printf("waiting.......\n");
		memset(recvbuf, 0, sizeof(recvbuf));
		recvd = recvfrom(sock, recvbuf, sizeof(recvbuf), 0,
				(struct sockaddr *) &client, &structlength);

		if (recvd < 0) {

			perror("recvfrom");
			exit(EXIT_FAILURE);

		} else {
			printf("received:%s\n", recvbuf);

			int params[3] = { 0 };
			int actualSize = 0;
			// 根据接收到的消息判断请求类型
			switch (recvbuf[0]) {
			case '0':

				//// 压缩捕获到的帧并读取数据到发送帧
				// 压缩质量参数
				params[0] = CV_IMWRITE_JPEG_QUALITY;
				params[1] = 30;
				memset(sendFrame, 0, frameDataSize);

				pthread_mutex_lock(&(frame_lock->mutex)); //加锁

				//	if (frame_lock->flag != 1) //没有数据
				//		pthread_cond_wait(&(frame_lock->cond),
				//			&(frame_lock->mutex)); //获取锁->等待信号

				if (frame_lock->flag == 1) {
					CvMat* jpgMat = cvEncodeImage(".jpeg", frame_lock->capturedFrame, params); //压缩图像
					actualSize = jpgMat->rows * jpgMat->cols;

					// 压缩后存入文件
					//FILE* fp = fopen("test.jpg", "w+");
					//fwrite(jpgMat->data.ptr, 1, actualSize, fp);
					//fclose(fp);
					//cvSaveImage("capture/frame.jpg", frame, NULL);

					memcpy(sendFrame, jpgMat->data.ptr, actualSize);
					frame_lock->flag = 0;

					cvReleaseMat(&jpgMat); //不确定这里是不是会内存泄漏?
				}

				pthread_mutex_unlock(&(frame_lock->mutex)); //解锁

				snd = sendto(sock, sendFrame, actualSize, 0,
						(struct sockaddr *) &client, structlength);

				break;

			case '1':
				recordVideoSwitch = 1; //打开录制视频开关

				//声明视频文件结构，创建视频写入器
				int isColor = 1;
				int frameW = 640;
				int frameH = 480;
				int fps = 20;

				if (camWriter != NULL) {
					cvReleaseVideoWriter(&camWriter);
				} else {
					//CV_FOURCC()视频压缩的编码格式，fps播放的帧率，cvSize(frameW,frameH)视频图像的大小，isColor为1即为彩色图像
					camWriter = cvCreateVideoWriter("out.avi",
							CV_FOURCC('X', 'V', 'I', 'D'), fps,
							cvSize(frameW, frameH), isColor);
				}

				// 成功创建
				if (camWriter != NULL) {
					snd = sendto(sock, "1", 1, 0, //发送成功标志
							(struct sockaddr *) &client, structlength);
				}

				break;

			case '2':
				recordVideoSwitch = 0; //关闭录制视频开关

				if (camWriter != NULL) {
					cvReleaseVideoWriter(&camWriter);
					snd = sendto(sock, "1", 1, 0, (struct sockaddr *) &client,
							structlength);
				}

				break;

			default:
				break;
			}
		}

		if (snd < 0) {
			perror("sendto");
			exit(1);
		}
		printf("sendok!\n");
	}

	close(sock);

	return NULL;
}

/////////////////////////////////////EVENT TEST
void* detectService(void* args) {

	int frameNum = 0;

	for (;;) {

		pthread_mutex_lock(&(frame_lock->mutex)); //加锁

		if (frame_lock->flag != -1) {
			cvCopy(frame_lock->capturedFrame, frame1, NULL);
		}

		pthread_mutex_unlock(&(frame_lock->mutex)); //解锁

		////计算帧差

		IplImage* gframe1 = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 1);
		IplImage* gframe2 = cvCreateImage(FRAME_SIZE, IPL_DEPTH_8U, 1);

		cvSmooth(frame1, frame1, CV_GAUSSIAN, 5, 5, 0, 0);   ///< 平滑，滤除噪声
		cvSmooth(frame2, frame2, CV_GAUSSIAN, 5, 5, 0, 0);
		cvCvtColor(frame1, gframe1, CV_BGR2GRAY);
		cvCvtColor(frame2, gframe2, CV_BGR2GRAY);
		int diffNum = framesDiff(gframe1, gframe2, diff);
		cvCopy(frame1, frame2, NULL);

		cvReleaseImage(&gframe1);
		cvReleaseImage(&gframe2);

		////产生事件
		if (diffNum > 100) {
			if (frameNum++ >= 30) { //如果有30帧都监测到移动，产生一个事件
		        Event* event = produceEvent();
		        recordEvent(event);
		        frameNum = 0;
			}
		}

		usleep(33000);  //以微秒为单位：需等待0.33秒以有效地计算帧差
	}

	return NULL;
}

void* eventService(void* args) {

	/*for (;;) {
		printf("Produce an event!\n");
		Event* event = produceEvent();
		recordEvent(event);
		sleep(10);
	}*/

	return NULL;
}

Event* produceEvent() {

	Event* event = (Event*) malloc(sizeof(Event));

	event->threadId = 111/*pthread_self()*/;
	event->level = WARNING;

	event->title = (char*) malloc(sizeof(char) * 64);
	char* title = "Warning";
	memcpy(event->title, title, strlen(title) + 1);

	event->description = (char*) malloc(sizeof(char) * 256);
	char* description = "This is a test event~";
	memcpy(event->description, description, strlen(description) + 1);

	time_t t = time(0);
	char path[64];
	strftime(path, sizeof(path), "capture/%Y_%m_%d_%H_%M_%S.jpg",
			localtime(&t));
	printf("%s\n", path);

	event->resouce_path = (char*) malloc(sizeof(char) * 64);
	memcpy(event->resouce_path, path, strlen(path) + 1);

	return event;
}

int recordEvent(Event* event) {

	FILE* eventFile = NULL;

	// 保存事件图像到文件
	pthread_mutex_lock(&(frame_lock->mutex)); //加锁

	printf("%d\n", frame_lock->capturedFrame->imageSize);
	if (frame_lock->flag != -1) {
		cvSaveImage(event->resouce_path, frame_lock->capturedFrame, NULL); //把图像写入文件
	}

	pthread_mutex_unlock(&(frame_lock->mutex)); //解锁

	// 事件相关操作
	if ((eventFile = fopen("event_new", "at")) == NULL) {
		printf("Open file failure!\n");
		return 0;
	}

	fprintf(eventFile, "%d:", event->threadId);
	fprintf(eventFile, "%d:", event->level);
	fprintf(eventFile, "%s:", event->title);
	fprintf(eventFile, "%s:", event->description);
	fprintf(eventFile, "%s\n", event->resouce_path);

	fclose(eventFile);

	if (event->resouce_path != NULL) {
		free(event->resouce_path);
		event->resouce_path = NULL;
	}

	if (event->description != NULL) {
		free(event->description);
		event->description = NULL;
	}

	if (event->title != NULL) {
		free(event->title);
		event->title = NULL;
	}

	// Free
	if (event != NULL) {
		free(event);
		event = NULL;
	}

	return 1;
}
