/*
 * tools.c
 *
 *  Created on: 2013-7-3
 *      Author: redsun
 */

#include "tools.h"

///探测图像和结果图像必须为3通道的
void detecSkin(const IplImage *frame, IplImage *dst)
{
    IplImage *temp = cvCreateImage(cvSize(frame->width, frame->height), IPL_DEPTH_8U, 3);
    //cvSmooth(frame, temp, CV_GAUSSIAN, 5, 5, 0, 0);   ///< 平滑，滤除噪声
    cvCvtColor(frame, temp, CV_BGR2HSV);  //将RGB图像转化为HSV图像
    assert(dst != NULL);

    uchar *ptr = (uchar*)temp->imageData;
    int x, y;
    float H, S, V;
    for (y=0; y<temp->height; y++)
    {
    	for (x=0; x<temp->width; x++)
    	{
    		/**
    		  *简单的肤色检测算法
    		  */

    		/*//Skin color according to threshold value H[30, 45], S[35, 200]
    		if ((ptr[3*x]>=0 && ptr[3*x]<=25)
    					&& (ptr[3*x+1]>=35 && ptr[3*x+1]<=200))
    					{
    						ptr[3*x]   = 255;
    						ptr[3*x+1] = 255;
    						ptr[3*x+2] = 255;
    					}
    		else
    		{
    			ptr[3*x]   = 0;
    			ptr[3*x+1] = 0;
    			ptr[3*x+2] = 0;
    		}*/

    		/**
    		  *下面算法出自论文:
    		  * <<Skin Color Enhancement Based on
    		  * Favorite Skin Color in HSV Color Space>>
    		  */

    		///调整三个分量
    		H = (float)ptr[3*x] * 2;   //[0~360]
    		S = (float)ptr[3*x+1] / (float)255;  //[0~1]
    		V = (float)ptr[3*x+2] / (float)255;   //[0~1]
    		if ( (S >= 0.1f) && (S <= 0.68f) )
    		{
    			if ( ((V >= 0.13f) && (V <= 0.25f) &&
    						(H >= ((0.4f-V)/0.014f)) &&
    						(H <= ((V+0.062f)/0.01f))) ||
    				 ((V >= 0.25f) && (V <= 0.38f) &&
    						(H >= ((0.4f-V)/0.014f)) &&
    						(H <= ((0.67f-V)/0.014f))) ||
    				 ((V >= 0.38f) && (V <= 0.46f) &&
    						(H >= ((V-0.34f)/0.03f)) &&
    						(H <= ((0.67f-V)/0.014f))) ||
    				 ((V >= 0.46f) && (V <= 0.6f) &&
    						(H >= ((V-0.34)/0.03f)) &&
    						(H <= ((V-0.31)/0.009f))) ||
    				 ((V >= 0.6f) && (V <= 0.76f) &&
    						(H >= ((0.91f-V)/0.14f)) &&
    						(H <= ((V-0.31f)/0.009f))) ||
    				 ((V >= 0.76f) && (V <= 0.91f) &&
    						(H >= ((0.91f-V)/0.14f)) &&
    						(H <= ((1.17f-V)/0.0082f))) ||
    				 ((V >= 0.91f) && (V <= 1.0f) &&
    						(H >= ((V-0.91f)/0.0041f)) &&
    						(H <= ((1.17f-V)/0.0082f))) )
    			{
    				ptr[3*x]   = 255;
    				ptr[3*x+1] = 255;
    				ptr[3*x+2] = 255;
    			}
    			else
    			{
    				ptr[3*x]   = 0;
    				ptr[3*x+1] = 0;
    				ptr[3*x+2] = 0;
    			}

    		}
    		else
    		{
    			ptr[3*x]   = 0;
    			ptr[3*x+1] = 0;
    			ptr[3*x+2] = 0;
    		}
    	}
    	ptr += temp->widthStep;
    }

    cvCopy(temp, dst, NULL);

    cvReleaseImage(&temp);
}

void preProcFrame(const IplImage *frame, IplImage *dst)
{
    cvSmooth(frame, dst, CV_GAUSSIAN, 5, 5, 0, 0);   ///< 平滑，滤除噪声
/*    ///指向帧图像第一个像素
    uchar *pFramePixel = (uchar*)frame->imageData;
    uchar *pDstPixel  = (uchar*)dst->imageData;

    int x, y;
    int offset = 0;
    for (y=0; y<frame->height; ++y)
    {
    	offset = 0;
    	for (x=0; x<frame->width; ++x)
    	{
    		*(pDstPixel+offset) = *(pFramePixel+offset);   ///< B通道
    		++offset;
    		*(pDstPixel+offset) = *(pFramePixel+offset);   ///< G通道
    		++offset;
    		*(pDstPixel+offset) = *(pFramePixel+offset);   ///< R通道
    		++offset;
    	}
    	pFramePixel += frame->widthStep;
    	pDstPixel   += dst->widthStep;
    }*/
}

int framesDiff(const IplImage *frame1, const IplImage *frame2, IplImage *diff)
{
    assert( (frame1->width == frame2->width) && (frame1->height == frame2->height) );   ///<两幅图片尺寸相
    ///指向帧图像第一个像素
    uchar *pFrame1Pixel = (uchar*)frame1->imageData;
    uchar *pFrame2Pixel = (uchar*)frame2->imageData;
    uchar *pDiffPixel   = (uchar*)diff->imageData;

    int x, y;
    int N = 0;
    for (y=0; y<diff->height; ++y)
    {
    	for (x=0; x<diff->width; ++x)
    	{
    		if (abs(*(pFrame1Pixel+x) - *(pFrame2Pixel+x)) > 30) {
    		    *(pDiffPixel+x) = 255;
    		    N++;
    		}
    		else
    			*(pDiffPixel+x) = 0;
    	}
    	pFrame1Pixel += frame1->widthStep;
    	pFrame2Pixel += frame2->widthStep;
    	pDiffPixel   += diff->widthStep;
    }

    return N;
}


