/*
 * tools.h
 *
 *  Created on: 2013-7-3
 *      Author: redsun
 */

#ifndef TOOLS_H_
#define TOOLS_H_

#include "opencv2/imgproc/imgproc_c.h"
#include "opencv2/highgui/highgui_c.h"

///检测肤色区域
void detecSkin(const IplImage *frame, IplImage *dst);

///预处理帧
void preProcFrame(const IplImage *frame, IplImage *dst);

///得出两帧差异图像, 并返回差异的像素数目
int framesDiff(const IplImage *frame1, const IplImage *frame2, IplImage *diff);


#endif /* TOOLS_H_ */
