#include <jni.h>
#include "com_xfgryujk_longexposurecamera_CameraPreview.h"

JNIEXPORT void JNICALL Java_com_xfgryujk_longexposurecamera_CameraPreview_decodeYUV420SP
  (JNIEnv * env, jobject obj, jintArray jiaRgb, jbyteArray jbaYuv420sp, jint width, jint height)
{
	unsigned char * yuv420sp = (unsigned char*) (*env)->GetByteArrayElements(env, jbaYuv420sp, 0);
	int * rgb = (int*) (*env)->GetIntArrayElements(env, jiaRgb, 0);

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
}
