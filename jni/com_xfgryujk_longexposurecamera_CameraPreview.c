#undef __cplusplus
#include <jni.h>
#include "com_xfgryujk_longexposurecamera_CameraPreview.h"

#define NULL ((void*)0)


// decodeYUV420SP
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_decodeYUV420SP
  (JNIEnv * env, jclass thiz, jintArray jiaRgb, jbyteArray jbaYuv420sp, jint width, jint height)
{
	unsigned char* yuv420sp = (unsigned char*) (*env)->GetByteArrayElements(env, jbaYuv420sp, NULL);
	jint* rgb = (*env)->GetIntArrayElements(env, jiaRgb, NULL);

	const int frameSize = width * height;

	int j, yp;
	for (j = 0, yp = 0; j < height; j++)
	{
		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
		int i;
		for (i = 0; i < width; i++, yp++)
		{
			int y = (0xff & ((int) yuv420sp[yp])) - 16;
			if (y < 0) y = 0;
			if ((i & 1) == 0)
			{
				v = (0xff & yuv420sp[uvp++]) - 128;
				u = (0xff & yuv420sp[uvp++]) - 128;
			}

			int y1192 = 1192 * y;
			int r = (y1192 + 1634 * v);
			int g = (y1192 - 833 * v - 400 * u);
			int b = (y1192 + 2066 * u);

			if (r < 0) r = 0; else if (r > 262143) r = 262143;
			if (g < 0) g = 0; else if (g > 262143) g = 262143;
			if (b < 0) b = 0; else if (b > 262143) b = 262143;

			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
	}

	(*env)->ReleaseByteArrayElements(env, jbaYuv420sp, yuv420sp, JNI_ABORT);
	(*env)->ReleaseIntArrayElements(env, jiaRgb, rgb, 0);
}

// blendAverage
JNIEXPORT jintArray JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendAverage
  (JNIEnv * env, jclass thiz, jint width, jint height, jintArray jiamPictureData, jintArray jiaPreviewRGBData, jint frameCount)
{
	jint* mPictureData = (*env)->GetIntArrayElements(env, jiamPictureData, NULL);
	jint* previewRGBData = (*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);

	int length = width * height;
	jintArray jiaData = (*env)->NewIntArray(env, length);
	/** RGB1 RGB2 ... */
	jint* data = (*env)->GetIntArrayElements(env, jiaData, NULL);
	int i;
	for(i = 0; i < length; i++)
	{
		mPictureData[i * 3]     += (previewRGBData[i] & 0x00FF0000) >> 16;
		mPictureData[i * 3 + 1] += (previewRGBData[i] & 0x0000FF00) >> 8;
		mPictureData[i * 3 + 2] += (previewRGBData[i] & 0x000000FF);
		
		data[i] = 0xFF000000;
		data[i] |= (mPictureData[i * 3]     / frameCount) << 16;
		data[i] |= (mPictureData[i * 3 + 1] / frameCount) << 8;
		data[i] |=  mPictureData[i * 3 + 2] / frameCount;
	}
	
	(*env)->ReleaseIntArrayElements(env, jiamPictureData, mPictureData, 0);
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, previewRGBData, JNI_ABORT);
	
	(*env)->ReleaseIntArrayElements(env, jiaData, data, 0);
	return jiaData;
}

// blendMax1
JNIEXPORT jintArray JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendMax1
  (JNIEnv * env, jclass thiz, jint width, jint height, jintArray jiamPictureData, jintArray jiaPreviewRGBData, jint frameCount)
{
	jint* mPictureData = (*env)->GetIntArrayElements(env, jiamPictureData, NULL);
	jint* previewRGBData = (*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	
	int length = width * height;
	jintArray jiaData = (*env)->NewIntArray(env, length);
	/** RGB1 RGB2 ... */
	jint* data = (*env)->GetIntArrayElements(env, jiaData, NULL);
	int i;
	for(i = 0; i < length; i++)
	{
		int r2 = (previewRGBData[i] & 0x00FF0000) >> 16;
		int g2 = (previewRGBData[i] & 0x0000FF00) >> 8;
		int b2 = previewRGBData[i] & 0x000000FF;
		if(r2 + g2 + b2 > mPictureData[i * 3] + mPictureData[i * 3 + 1] + mPictureData[i * 3 + 2])
		{
			mPictureData[i * 3]     = r2;
			mPictureData[i * 3 + 1] = g2;
			mPictureData[i * 3 + 2] = b2;
		 }
		data[i] = 0xFF000000;
		data[i] |= mPictureData[i * 3]     << 16;
		data[i] |= mPictureData[i * 3 + 1] << 8;
		data[i] |= mPictureData[i * 3 + 2];
	}
	
	(*env)->ReleaseIntArrayElements(env, jiamPictureData, mPictureData, 0);
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, previewRGBData, JNI_ABORT);
	
	(*env)->ReleaseIntArrayElements(env, jiaData, data, 0);
	return jiaData;
}

// blendMax2
JNIEXPORT jintArray JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendMax2
  (JNIEnv * env, jclass thiz, jint width, jint height, jintArray jiamPictureData, jintArray jiaPreviewRGBData, jint frameCount)
{
	jint* mPictureData = (*env)->GetIntArrayElements(env, jiamPictureData, NULL);
	jint* previewRGBData = (*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	
	int length = width * height;
	jintArray jiaData = (*env)->NewIntArray(env, length);
	/** RGB1 RGB2 ... */
	jint* data = (*env)->GetIntArrayElements(env, jiaData, NULL);
	int i;
	for(i = 0; i < length; i++)
	{
		int r1 = mPictureData[i * 3];
		int g1 = mPictureData[i * 3 + 1];
		int b1 = mPictureData[i * 3 + 2];
		int r2 = (previewRGBData[i] & 0x00FF0000) >> 16;
		int g2 = (previewRGBData[i] & 0x0000FF00) >> 8;
		int b2 = previewRGBData[i] & 0x000000FF;
		if(r2 * r2 + g2 * g2 + b2 * b2 > r1 * r1 + g1 * g1 + b1 * b1)
		{
			mPictureData[i * 3]     = r2;
			mPictureData[i * 3 + 1] = g2;
			mPictureData[i * 3 + 2] = b2;
		 }
		data[i] = 0xFF000000;
		data[i] |= mPictureData[i * 3]     << 16;
		data[i] |= mPictureData[i * 3 + 1] << 8;
		data[i] |= mPictureData[i * 3 + 2];
	}
	
	(*env)->ReleaseIntArrayElements(env, jiamPictureData, mPictureData, 0);
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, previewRGBData, JNI_ABORT);
	
	(*env)->ReleaseIntArrayElements(env, jiaData, data, 0);
	return jiaData;
}

// blendScreen
JNIEXPORT jintArray JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendScreen
  (JNIEnv * env, jclass thiz, jint width, jint height, jintArray jiamPictureData, jintArray jiaPreviewRGBData, jint frameCount)
{
	jint* mPictureData = (*env)->GetIntArrayElements(env, jiamPictureData, NULL);
	jint* previewRGBData = (*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);

	int length = width * height;
	jintArray jiaData = (*env)->NewIntArray(env, length);
	/** RGB1 RGB2 ... */
	jint* data = (*env)->GetIntArrayElements(env, jiaData, NULL);
	int i;
	for(i = 0; i < length; i++)
	{
		mPictureData[i * 3]     = 255 - (255 - mPictureData[i * 3]) 
				* (255 - ((previewRGBData[i] & 0x00FF0000) >> 16)) / 255;
		mPictureData[i * 3 + 1] = 255 - (255 - mPictureData[i * 3 + 1]) 
				* (255 - ((previewRGBData[i] & 0x0000FF00) >> 8)) / 255;
		mPictureData[i * 3 + 2] = 255 - (255 - mPictureData[i * 3 + 2]) 
				* (255 - (previewRGBData[i] & 0x000000FF)) / 255;
		
		data[i] = 0xFF000000;
		data[i] |= mPictureData[i * 3]     << 16;
		data[i] |= mPictureData[i * 3 + 1] << 8;
		data[i] |= mPictureData[i * 3 + 2];
	}
	
	(*env)->ReleaseIntArrayElements(env, jiamPictureData, mPictureData, 0);
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, previewRGBData, JNI_ABORT);
	
	(*env)->ReleaseIntArrayElements(env, jiaData, data, 0);
	return jiaData;
}
