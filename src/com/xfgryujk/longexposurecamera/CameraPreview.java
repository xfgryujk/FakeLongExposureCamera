package com.xfgryujk.longexposurecamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, OnClickListener, 
Runnable {
	private final String TAG = "CameraPreview";
	
	protected SurfaceHolder mHolder;
	protected ImageView mResultPreview;
	protected Camera mCamera;
	protected int mPictureWidth, mPictureHeight;
	
	protected boolean mIsExposing       = false;
	protected boolean mIsShutterForbidden = false;
	
	/** YUV */
	protected byte[] mPreviewData;
	/** RGB1 RGB2 ... */
	protected int[] mPreviewRGBData;
	/** R1 G1 B1 R2 G2 B2 ... */
	protected int[] mPictureData;
	protected int mFrameCount;
	
	Bitmap mResultBitmap;
	
	
	public CameraPreview(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
	}
	
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		mHolder = getHolder();
		mHolder.addCallback(this);
	}
	
	public void setResultPreview(ImageView imagePreview) {
		mResultPreview = imagePreview;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
		
	}
	
	/** Open camera, set parameters */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Open camera
		mCamera = Camera.open();
		if(mCamera == null)
		{
			Toast.makeText(this.getContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
			((Activity)this.getContext()).finish();
			return;
		}
		
		// Set parameters
		Camera.Parameters params = mCamera.getParameters();

		// Max preview size
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		int maxPixels = 0, pixels;
		int largestWidth = 0, largestHeight = 0;
		for(Camera.Size size : sizes)
		{
			pixels = size.width * size.height;
			if(maxPixels < pixels)
			{
				maxPixels = pixels;
				largestWidth  = size.width;
				largestHeight = size.height;
			}
		}
		mPictureWidth  = largestWidth;
		mPictureHeight = largestHeight;
		params.setPreviewSize(largestWidth, largestHeight);
		Log.i(TAG, "preview size " + largestWidth + " * " + largestHeight);

		mCamera.setParameters(params);
		
		// Set preview display
		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// Set preview callback
		mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
            	if(mIsExposing)
            	{
            		synchronized (CameraPreview.this)
            		{
            			mPreviewData = data;
            			CameraPreview.this.notify();
                    }
            	}
            }
        });
        
        // Start preview
        mCamera.startPreview();
	}
	
	/** Release camera */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(mCamera != null)
		{
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}
	
	/** Auto focus */
	@Override
	public void onClick(View view) { 
		mCamera.autoFocus(null);
	}
	
	/** Start or stop exposing */
	public void onShutterClick() {
		if(!mIsShutterForbidden)
			if(mIsExposing)
			{
				Log.i(TAG, "Stop exposing");
				mIsExposing         = false;
				// Wait for exposing thread
				mIsShutterForbidden = true;
			}
			else
			{
				Log.i(TAG, "Start exposing");
				mPreviewRGBData = new int[mPictureWidth * mPictureHeight];
				mPictureData    = new int[mPictureWidth * mPictureHeight * 3];
				mFrameCount     = 0;
				mIsExposing     = true;
				// Start exposing thread
		        (new Thread(this)).start();

				// Resize
				LayoutParams lp = getLayoutParams();
				lp.width  = mPictureWidth / 4;
				lp.height = mPictureHeight / 4;
				setLayoutParams(lp);
				setPadding(50, 50, 0, 0);
			}
	}
	
	/** Exposing thread */
	@Override
	public void run() {
		while(mIsExposing)
		{
			synchronized (this) {
	            try {
	                this.wait();
	            } catch(InterruptedException e) {
	                e.printStackTrace();
	            }
	            
	            // Get preview data in RGB
	            decodeYUV420SP(mPreviewRGBData, mPreviewData, mPictureWidth, mPictureHeight);
	        }
	        mFrameCount++;
	        
			Log.i(TAG, "Dealing picture");
			if(mFrameCount == 0)
			{
				Log.e(TAG, "mFrameCount == 0");
				mIsShutterForbidden = false;
				continue;
			}
			Log.i(TAG, "mFrameCount " + mFrameCount);
			
			// Blend pictures and show
			mResultBitmap = mPictureBlender[mPictureBlenderIndex].blend();
			mUpdateResultPreview.sendEmptyMessage(0);
		}
		mExposingFinish.sendEmptyMessage(0);
	}
	
	@SuppressLint("HandlerLeak")
	protected Handler mUpdateResultPreview = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			mResultPreview.setImageBitmap(mResultBitmap);
		}
	};
	
	@SuppressLint("HandlerLeak")
	protected Handler mExposingFinish = new Handler() {
		@SuppressLint("SimpleDateFormat")
		@Override
        public void handleMessage(Message msg) {
			// Save the picture
			// Temporary path
			final String storagePath = Environment.getExternalStorageDirectory().getPath() + "/FakeLongExposureCamera";
			try {
	            File dir = new File(storagePath);
	            if(!dir.exists())
	                if(!dir.mkdirs()) {
	                    Toast.makeText(getContext(), "创建文件夹失败", Toast.LENGTH_SHORT).show();
	                    return;
	                }
	            File file = new File(storagePath + "/" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
	            
	            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
	            mResultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
	            bos.flush();
	            bos.close();
	            
	            Toast.makeText(getContext(), "已保存到" + file.getPath(), Toast.LENGTH_SHORT).show();
	        } catch(Exception e) {
	            e.printStackTrace();
	        }
			
			// Resize
			LayoutParams lp = getLayoutParams();
			lp.width  = LayoutParams.MATCH_PARENT;
			lp.height = LayoutParams.MATCH_PARENT;
			setLayoutParams(lp);
			setPadding(0, 0, 0, 0);
			
			mIsShutterForbidden = false;
		}
	};
	
	private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++)
		{
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++)
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

	protected interface PictureBlender {
		/** Called by exposing thread */
		Bitmap blend();
	}
	
	protected int mPictureBlenderIndex = 0;
	protected final PictureBlender[] mPictureBlender = {
			// Average
			new PictureBlender() {
				@Override
				public Bitmap blend() {
			        for(int i = 0; i < mPreviewRGBData.length; i++)
			        {
			            mPictureData[i * 3]     += (mPreviewRGBData[i] & 0x00FF0000) >> 16;
			            mPictureData[i * 3 + 1] += (mPreviewRGBData[i] & 0x0000FF00) >> 8;
			            mPictureData[i * 3 + 2] += (mPreviewRGBData[i] & 0x000000FF);
			        }
			        
					/** RGB1 RGB2 ... */
					int[] data = new int[mPictureWidth * mPictureHeight];
					for(int i = 0; i < data.length; i++)
					{
						data[i] = 0xFF000000;
						data[i] |= (mPictureData[i * 3]     / mFrameCount) << 16;
						data[i] |= (mPictureData[i * 3 + 1] / mFrameCount) << 8;
						data[i] |=  mPictureData[i * 3 + 2] / mFrameCount;
					}
					Bitmap bmp = Bitmap.createBitmap(mPictureWidth, mPictureHeight, Config.RGB_565);
					bmp.setPixels(data, 0, mPictureWidth, 0, 0, mPictureWidth, mPictureHeight);
					return bmp;
				}
			},

			// Max
			new PictureBlender() {
				@Override
				public Bitmap blend() {
			        for(int i = 0; i < mPreviewRGBData.length; i++)
			        {
			        	int sum1 = mPictureData[i * 3] + mPictureData[i * 3 + 1] + mPictureData[i * 3 + 2];
			        	int r = (mPreviewRGBData[i] & 0x00FF0000) >> 16;
			        	int g = (mPreviewRGBData[i] & 0x0000FF00) >> 8;
						int b = mPreviewRGBData[i] & 0x000000FF;
			        	if(r + g + b > sum1)
			        	{
			        		mPictureData[i * 3]     = r;
			        		mPictureData[i * 3 + 1] = g;
			        		mPictureData[i * 3 + 2] = b;
			        	}
			        }

					/** RGB1 RGB2 ... */
					int[] data = new int[mPictureWidth * mPictureHeight];
					for(int i = 0; i < data.length; i++)
					{
						data[i] = 0xFF000000;
						data[i] |= mPictureData[i * 3]     << 16;
						data[i] |= mPictureData[i * 3 + 1] << 8;
						data[i] |= mPictureData[i * 3 + 2];
					}
					Bitmap bmp = Bitmap.createBitmap(mPictureWidth, mPictureHeight, Config.RGB_565);
					bmp.setPixels(data, 0, mPictureWidth, 0, 0, mPictureWidth, mPictureHeight);
					return bmp;
				}
			}
	};
}
