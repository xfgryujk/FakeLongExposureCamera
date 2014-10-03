package com.xfgryujk.longexposurecamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, OnClickListener, 
Runnable {
	private static final String TAG = "CameraPreview";
	
	protected SurfaceHolder mHolder;
	protected MainActivity mActivity;
	protected Camera mCamera;
	protected int mPictureWidth, mPictureHeight;
	
	protected boolean mIsExposing = false;
	
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
		mActivity = (MainActivity)context;
		mHolder = getHolder();
		mHolder.addCallback(this);
	}
	
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		mActivity = (MainActivity)context;
		mHolder = getHolder();
		mHolder.addCallback(this);
	}
	
	public Camera getCamera() {
		return mCamera;
	}
	
	public boolean isExposing() {
		return mIsExposing;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
		Log.i(TAG, "surfaceChanged()");
	}
	
	/** Reset camera */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated()");
		resetCamera();
	}
	
	/** Release camera */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed()");
		releaseCamera();
	}

	/** Open camera, set parameters */
	public void resetCamera() {
		releaseCamera();
		
		// Open camera
		try {
		mCamera = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
			mCamera = null;
		}
		if(mCamera == null)
		{
			new AlertDialog.Builder(getContext())
				.setTitle(getResources().getString(R.string.error))
				.setMessage(getResources().getString(R.string.failed_to_open_camera))
				.setPositiveButton(getResources().getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mActivity.finish();
						}
					}
				)
				.show();
			return;
		}
		
		// Set parameters
		Camera.Parameters params = mCamera.getParameters();
		
		params.setExposureCompensation(SettingsManager.mEV);
		params.setWhiteBalance(SettingsManager.mWhiteBalance);
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		mPictureWidth  = sizes.get(SettingsManager.mResolution).width;
		mPictureHeight = sizes.get(SettingsManager.mResolution).height;
		params.setPreviewSize(mPictureWidth, mPictureHeight);
		
		Log.i(TAG, "EV " + SettingsManager.mEV);
		Log.i(TAG, "white balance " + SettingsManager.mWhiteBalance);
		Log.i(TAG, "preview size " + mPictureWidth + " * " + mPictureHeight);

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
	
	public void releaseCamera() {
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
		if(!mIsExposing)
			mCamera.autoFocus(null);
	}
	
	/** Start or stop exposing */
	public void onShutterClick() {
		if(mIsExposing)
		{
			Log.i(TAG, "Stop exposing");
			mIsExposing = false;
			// Wait for exposing thread
			mActivity.mButtonShutter.setVisibility(INVISIBLE);
    		synchronized (this)
    		{
    			notify();
    		}
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

	        mActivity.mResultPreview.setVisibility(VISIBLE);
	        mActivity.mButtonSetting.setVisibility(INVISIBLE);
			
			// Resize
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
			lp.leftMargin = 20;
			lp.topMargin  = 20;
			if(mPictureWidth > mPictureHeight)
			{
				lp.width  = 200;
				lp.height = mPictureHeight * 200 / mPictureWidth;
			}
			else
			{
				lp.width  = mPictureWidth * 200 / mPictureHeight;
				lp.height = 200;
			}
			setLayoutParams(lp);
		}
	}
	
	/** Exposing thread */
	@SuppressLint("SimpleDateFormat")
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
			
			// Blend pictures and show
			mResultBitmap = mPictureBlender[SettingsManager.mBlendingMode].blend();
			mHandler.sendEmptyMessage(MSG_UPDATA_RESULT_PREVIEW);
		}
		Log.i(TAG, "mFrameCount " + mFrameCount);
		
		// Exposing finish
		mHandler.sendEmptyMessage(MSG_EXPOSING_FINISH);

		// Save the picture
		try {
        	Bundle bundle = new Bundle();
        	Message msg = new Message();
        	msg.what = MSG_TOAST;
        	
        	// Create file
            File dir = new File(SettingsManager.mPath);
            if(!dir.exists())
                if(!dir.mkdirs()) {
                    bundle.putString("msg", getResources().getString(R.string.failed_to_create_directory));
                	msg.setData(bundle);
                	mHandler.sendMessage(msg);
                    return;
                }
            File file = new File(SettingsManager.mPath + "/" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
            
            // Write file
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            if(mResultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos))
            {
	            bos.flush();
	            bundle.putString("msg", getResources().getString(R.string.saved_to) + " " + file.getPath());
            }
            else
            	bundle.putString("msg", getResources().getString(R.string.failed_to_write_file) + file.getPath());
        	msg.setData(bundle);
        	mHandler.sendMessage(msg);
        	
            bos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
	}
	
	protected static final int MSG_UPDATA_RESULT_PREVIEW = 1;
	protected static final int MSG_EXPOSING_FINISH = 2;
	protected static final int MSG_RESTORE = 3;
	protected static final int MSG_TOAST = 4;
	/** Save the picture, resize this view */
	@SuppressLint("HandlerLeak")
	protected Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case MSG_UPDATA_RESULT_PREVIEW:
				mActivity.mResultPreview.setImageBitmap(mResultBitmap);
				break;
				
			case MSG_EXPOSING_FINISH:
				// Preview
				CameraPreview.this.setVisibility(INVISIBLE);
				
				// Resize
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
				lp.leftMargin = 0;
				lp.topMargin  = 0;
				lp.width      = FrameLayout.LayoutParams.MATCH_PARENT;
				lp.height     = FrameLayout.LayoutParams.MATCH_PARENT;
				setLayoutParams(lp);

				sendEmptyMessageDelayed(MSG_RESTORE, 2000);
				break;
				
			case MSG_RESTORE:
				CameraPreview.this.setVisibility(VISIBLE);
				mActivity.mResultPreview.setVisibility(INVISIBLE);
				mActivity.mResultPreview.setImageBitmap(null);
		        mActivity.mButtonSetting.setVisibility(VISIBLE);
				mActivity.mButtonShutter.setVisibility(VISIBLE);
				break;
				
			case MSG_TOAST:
				Toast.makeText(getContext(), msg.getData().getString("msg"), 
	            		Toast.LENGTH_SHORT).show();
				break;
			}
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
		},

		// Screen
		new PictureBlender() {
			@Override
			public Bitmap blend() {
			    for(int i = 0; i < mPreviewRGBData.length; i++)
			    {
			        mPictureData[i * 3]     = 255 - (255 - mPictureData[i * 3]) 
			        		* (255 - ((mPreviewRGBData[i] & 0x00FF0000) >> 16)) / 255;
			        mPictureData[i * 3 + 1] = 255 - (255 - mPictureData[i * 3 + 1]) 
			        		* (255 - ((mPreviewRGBData[i] & 0x0000FF00) >> 8)) / 255;
			        mPictureData[i * 3 + 2] = 255 - (255 - mPictureData[i * 3 + 2]) 
			        		* (255 - (mPreviewRGBData[i] & 0x000000FF)) / 255;
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
