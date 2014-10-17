#undef __cplusplus
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <string.h>
#include "com_xfgryujk_longexposurecamera_CameraPreview.h"

typedef unsigned char BYTE;

const char* TAG = "ImageJni";
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

int width, height;
int stride;
int blendingMode;
// R1 G1 B1 R2 G2 B2 ...
int* picturesData;


// decodeYUV420SP
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_decodeYUV420SP
  (JNIEnv * env, jclass thiz, jintArray jiaRgb, jbyteArray jbaYuv420sp, jint width, jint height)
{
	BYTE* yuv420sp = (unsigned char*) (*env)->GetByteArrayElements(env, jbaYuv420sp, NULL);
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

// blenderInitialize
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blenderInitialize
  (JNIEnv * env, jclass thiz, jobject result, jint _blendingMode)
{
	LOGI("blenderInitialize()");
	AndroidBitmapInfo info;
	AndroidBitmap_getInfo(env, result, &info);
	width          = info.width;
	height         = info.height;
	stride         = info.stride;
	blendingMode   = _blendingMode;
	LOGI("width = %d height = %d stride = %d blendingMode = %d", width, height, stride, blendingMode);
	
	int size = width * height;
	if(_blendingMode == 0)
	{
		picturesData = (int*)malloc(size * 3 * sizeof(int));
		memset(picturesData, 0, size * 3 * sizeof(int));
	}

	// Initialize alpha
	// R1 G1 B1 A1 R2 G2 B2 A2 ...
	BYTE* data;
	AndroidBitmap_lockPixels(env, result, (void**)&data);
	memset(data, 0, size * 4);
	data += 3;
	int i, j;
	for(i = 0; i < height; i++, data += stride)
		for(j = 0; j < width; j++)
			data[j * 4] = 255;
	AndroidBitmap_unlockPixels(env, result);
}

// blenderUninitialize
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blenderUninitialize
  (JNIEnv * env, jclass thiz)
{
	LOGI("blenderUninitialize()");
	if(blendingMode == 0)
		free(picturesData);
}

// blendAverage
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendAverage
  (JNIEnv * env, jclass thiz, jobject resultBitmap, jintArray jiaPreviewRGBData, jint frameCount)
{
	// B1 G1 R1 A1 B2 G2 R2 A2 ...
	BYTE* previewRGBData  = (BYTE*)(*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	BYTE* pPreviewRGBData = previewRGBData;
	
	// R1 G1 B1 A1 R2 G2 B2 A2 ...
	BYTE* data;
	AndroidBitmap_lockPixels(env, resultBitmap, (void**)&data);
	int* pPicturesData = picturesData;
	int i, j;
	for(i = 0; i < height; i++, data += stride)
		for(j = 0; j < width; j++, pPreviewRGBData += 4, pPicturesData += 3)
		{
			*pPicturesData       += *(pPreviewRGBData + 2);
			data[j * 4]           = (BYTE)(*pPicturesData       / frameCount);
			*(pPicturesData + 1) += *(pPreviewRGBData + 1);
			data[j * 4 + 1]       = (BYTE)(*(pPicturesData + 1) / frameCount);
			*(pPicturesData + 2) += *pPreviewRGBData;
			data[j * 4 + 2]       = (BYTE)(*(pPicturesData + 2) / frameCount);
		}
	AndroidBitmap_unlockPixels(env, resultBitmap);
	
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, (jint*)previewRGBData, JNI_ABORT);
}

// blendMax1
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendMax1
  (JNIEnv * env, jclass thiz, jobject resultBitmap, jintArray jiaPreviewRGBData, jint frameCount)
{
	// B1 G1 R1 A1 B2 G2 R2 A2 ...
	BYTE* previewRGBData  = (BYTE*)(*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	BYTE* pPreviewRGBData = previewRGBData;
	
	// R1 G1 B1 A1 R2 G2 B2 A2 ...
	BYTE* data;
	AndroidBitmap_lockPixels(env, resultBitmap, (void**)&data);
	int i, j;
	for(i = 0; i < height; i++, data += stride)
		for(j = 0; j < width; j++, pPreviewRGBData += 4)
		{
			int r2 = *(pPreviewRGBData + 2);
			int g2 = *(pPreviewRGBData + 1);
			int b2 = *pPreviewRGBData;
			if(r2 + g2 + b2 > data[j * 4] + data[j * 4 + 1] + data[j * 4 + 2])
			{
				data[j * 4]     = r2;
				data[j * 4 + 1] = g2;
				data[j * 4 + 2] = b2;
			 }
		}
	AndroidBitmap_unlockPixels(env, resultBitmap);
	
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, (jint*)previewRGBData, JNI_ABORT);
}

// blendMax2
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendMax2
  (JNIEnv * env, jclass thiz, jobject resultBitmap, jintArray jiaPreviewRGBData, jint frameCount)
{
	// B1 G1 R1 A1 B2 G2 R2 A2 ...
	BYTE* previewRGBData  = (BYTE*)(*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	BYTE* pPreviewRGBData = previewRGBData;
	
	// R1 G1 B1 A1 R2 G2 B2 A2 ...
	BYTE* data;
	AndroidBitmap_lockPixels(env, resultBitmap, (void**)&data);
	int i, j;
	for(i = 0; i < height; i++, data += stride)
		for(j = 0; j < width; j++, pPreviewRGBData += 4)
		{
			int r1 = data[j * 4];
			int g1 = data[j * 4 + 1];
			int b1 = data[j * 4 + 2];
			int r2 = *(pPreviewRGBData + 2);
			int g2 = *(pPreviewRGBData + 1);
			int b2 = *pPreviewRGBData;
			if(r2 * r2 + g2 * g2 + b2 * b2 > r1 * r1 + g1 * g1 + b1 * b1)
			{
				data[j * 4]     = r2;
				data[j * 4 + 1] = g2;
				data[j * 4 + 2] = b2;
			 }
		}
	AndroidBitmap_unlockPixels(env, resultBitmap);
	
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, (jint*)previewRGBData, JNI_ABORT);
}

// blendScreen
JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_blendScreen
  (JNIEnv * env, jclass thiz, jobject resultBitmap, jintArray jiaPreviewRGBData, jint frameCount)
{
	// B1 G1 R1 A1 B2 G2 R2 A2 ...
	BYTE* previewRGBData  = (BYTE*)(*env)->GetIntArrayElements(env, jiaPreviewRGBData, NULL);
	BYTE* pPreviewRGBData = previewRGBData;
	
	// R1 G1 B1 A1 R2 G2 B2 A2 ...
	BYTE* data;
	AndroidBitmap_lockPixels(env, resultBitmap, (void**)&data);
	int i, j;
	for(i = 0; i < height; i++, data += stride)
		for(j = 0; j < width; j++, pPreviewRGBData += 4)
		{
			data[j * 4]     = 255 - (255 - data[j * 4])     * (255 - *(pPreviewRGBData + 2)) / 255;
			data[j * 4 + 1] = 255 - (255 - data[j * 4 + 1]) * (255 - *(pPreviewRGBData + 1)) / 255;
			data[j * 4 + 2] = 255 - (255 - data[j * 4 + 2]) * (255 - *pPreviewRGBData      ) / 255;
		}
	AndroidBitmap_unlockPixels(env, resultBitmap);
	
	(*env)->ReleaseIntArrayElements(env, jiaPreviewRGBData, (jint*)previewRGBData, JNI_ABORT);
}
