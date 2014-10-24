package com.xfgryujk.longexposurecamera;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, OnClickListener {
	private static final String TAG = "CameraPreview";
	
	protected SurfaceHolder mHolder;
	protected MainActivity mActivity;
	protected Camera mCamera = null;
	protected int mPictureWidth, mPictureHeight;
	
	protected volatile boolean mIsExposing = false;
	protected long mStartTime;
	
	/** YUV */
	protected byte[] mPreviewData;
	protected volatile int mFrameCount;
	
	public static Bitmap mResultBitmap;
	
	
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
	
	public void resize(int centreX, int centreY, int maxWidth, int maxHeight) {
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
		float scale1  = (float)maxWidth / (float)mPictureWidth;
		float scale2  = (float)maxHeight / (float)mPictureHeight;
		float scale   = scale1 < scale2 ? scale1 : scale2;
		lp.width      = (int)(mPictureWidth * scale);
		lp.height     = (int)(mPictureHeight * scale);
		lp.leftMargin = centreX - lp.width / 2;
		lp.topMargin  = centreY - lp.height / 2;
		setLayoutParams(lp);
	}
	
	/** Full screen */
	public void resize() {
		DisplayMetrics dm = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		resize(dm.widthPixels / 2, dm.heightPixels / 2, dm.widthPixels, dm.heightPixels);
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
		params.set("iso", SettingsManager.mISO);
		params.set("iso-speed", SettingsManager.mISO);
		params.set("nv-picture-iso", SettingsManager.mISO);
		
		Log.i(TAG, "EV " + SettingsManager.mEV);
		Log.i(TAG, "white balance " + SettingsManager.mWhiteBalance);
		Log.i(TAG, "preview size " + mPictureWidth + " * " + mPictureHeight);
		Log.i(TAG, "ISO " + SettingsManager.mISO);

		mCamera.setParameters(params);
		
		// Resize
		if(mIsExposing)
			resize(120, 120, 200, 200);
		else
			resize();
		
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
			mActivity.mFocusResult.setVisibility(INVISIBLE);
			mActivity.mFocusResult.setImageBitmap(null);
		}
	}
	
	/** Auto focus */
	@Override
	public void onClick(View view) { 
		if(!mIsExposing)
		{
			mCamera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean successful, Camera camera) {
					mActivity.mFocusResult.setImageResource(successful ? R.drawable.focus_succeeded : R.drawable.focus_failed);
					mActivity.mFocusResult.setVisibility(VISIBLE);
				}
			});
			mActivity.mFocusResult.setVisibility(INVISIBLE);
		}
	}
	
	/** Start or stop exposing */
	public void onShutterClick() {
		if(mIsExposing)
		{
			Log.i(TAG, "Stop exposing");
			mIsExposing = false;
			// Wait for exposing threads
			mActivity.mButtonShutter.setVisibility(INVISIBLE);
    		synchronized (this) {
    			notifyAll();
    		}
		}
		else
		{
			Log.i(TAG, "Start exposing");
			mResultBitmap   = Bitmap.createBitmap(mPictureWidth, mPictureHeight, Config.ARGB_8888);
			mActivity.mResultPreview.setImageBitmap(mResultBitmap);
			blenderInitialize(mResultBitmap, SettingsManager.mBlendingMode);
			mFrameCount     = 0;
			
			mLastFPSTime    = System.currentTimeMillis();
			mLastFrameCount = 0;
			
			// Start exposing threads
			mIsExposing     = true;
			mStartTime      = System.currentTimeMillis();
			//mLastFrameTime  = 0;
			for(int i = 0; i < SettingsManager.mMaxThreadCount; i++)
				(new Thread(new ExposingThread())).start();
			
			mActivity.mResultPreview.setVisibility(VISIBLE);
			mActivity.mOutputText.setVisibility(VISIBLE);
			mActivity.mButtonSetting.setVisibility(INVISIBLE);
			mActivity.mFocusResult.setVisibility(INVISIBLE);
			mActivity.mFocusResult.setImageBitmap(null);
			resize(120, 120, 200, 200);
		}
	}
	
	protected int mThreadCount = 0;
	protected long mNextFrameTime;
	protected class ExposingThread implements Runnable {
		@Override
		public void run() {
			long id = Thread.currentThread().getId();
			synchronized (CameraPreview.this) {
				mThreadCount++;
				Log.i(TAG, id + " starts, mThreadCount = " + mThreadCount);
			}
			/** YUV */
			byte[] previewData;
			/** RGB1 RGB2 ... */
			int[] previewRGBData = new int[mPictureWidth * mPictureHeight];
			int frameCount;
			boolean shouldStop = false;
			int n;
			
			while(mIsExposing)
			{
				synchronized (CameraPreview.this) {
					//Log.i(TAG, id + " is getting preview data");
					// Wait for preview data
		            try {
		            	CameraPreview.this.wait();
		            } catch(InterruptedException e) {
		                e.printStackTrace();
		            }
		            
					// Calculate delay time
					long time = System.currentTimeMillis();
			        n = (int)Math.ceil((double)(mNextFrameTime - time) / 200.0d);
			        mNextFrameTime = (n < 0 ? time : mNextFrameTime) + SettingsManager.mMinDelayMS;
				}
				// Delay
	            for(int i = 0; i < n && mIsExposing; i++)
	            	try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		            
				synchronized (CameraPreview.this) {
		            frameCount = ++mFrameCount;
		            if(!mIsExposing && frameCount > 1) // 1 frame at least
		            	break;
		            
		            // Auto stop
		            switch(SettingsManager.mAutoStop)
		            {
		            case 1: // frame
		            	shouldStop = frameCount >= SettingsManager.mAutoStopTime;
		            	break;
		            	
		            case 2: // second
		            	shouldStop = System.currentTimeMillis() - mStartTime >= SettingsManager.mAutoStopTimeMS;
		            	break;
		            }
	            	if(shouldStop)
	            	{
	            		if(!mHandler.hasMessages(MSG_UPDATE_PREVIEW))
	            			mHandler.sendEmptyMessage(MSG_STOP_EXPOSING);
	            		if(frameCount > 1) // 1 frame at least
	            			break;
	            	}
		            
		            // Get preview data
		            previewData = mPreviewData;
		        }
				//Log.i(TAG, id + " is decoding");
		        decodeYUV420SP(previewRGBData, previewData, mPictureWidth, mPictureHeight);
				
				// Blend pictures and show
				synchronized (mResultBitmap) {
					//Log.i(TAG, id + " is blending");
					mPictureBlender[SettingsManager.mBlendingMode].blend(previewRGBData, frameCount);
					if(!mHandler.hasMessages(MSG_UPDATE_PREVIEW))
						mHandler.sendEmptyMessage(MSG_UPDATE_PREVIEW);
				}
			}
			
			synchronized (CameraPreview.this) {
				Log.i(TAG, id + " is exiting, mThreadCount = " + mThreadCount);
				if(--mThreadCount > 0) // Is not the last thread
					return;
			}
			
			// For the last thread
			
			// Exposing finish
			mHandler.sendEmptyMessage(MSG_EXPOSING_FINISH);
			
			blenderUninitialize();
		}
	}
	
	protected static final int MSG_UPDATE_PREVIEW = 1;
	protected static final int MSG_EXPOSING_FINISH = 2;
	protected static final int MSG_STOP_EXPOSING = 3;
	private long mLastFPSTime;
	private int mLastFrameCount;
	private String mFPSString = "";
	@SuppressLint("HandlerLeak")
	protected Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case MSG_UPDATE_PREVIEW:
				long time = System.currentTimeMillis();
				if(time - mLastFPSTime >= 3000) {
					mFPSString = String.format("  %.2fFPS", 
							(float)(mFrameCount - mLastFrameCount) / (float)(time - mLastFPSTime) * 1000);
					mLastFPSTime    = time;
					mLastFrameCount = mFrameCount;
				}
				mActivity.mOutputText.setText(String.format("%.3fs  %d%s", 
						(System.currentTimeMillis() - mStartTime) / 1000.0f, 
						mFrameCount, 
						mFPSString));
				synchronized (mResultBitmap) {
					mActivity.mResultPreview.invalidate();
				}
				break;
				
			case MSG_EXPOSING_FINISH:
				// Preview and save
		        mActivity.startActivity(new Intent(mActivity, PreviewActivity.class));
		        
		        // Restore
		        mActivity.mOutputText.setVisibility(INVISIBLE);
		        mActivity.mOutputText.setText("");
		        mFPSString = "";
				mActivity.mResultPreview.setVisibility(INVISIBLE);
				mActivity.mResultPreview.setImageBitmap(null);
		        mActivity.mButtonSetting.setVisibility(VISIBLE);
				mActivity.mButtonShutter.setVisibility(VISIBLE);
				
				// Release
				mPreviewData = null;
				
				// Resize
				resize();
				break;
				
			case MSG_STOP_EXPOSING:
				onShutterClick();
				break;
			}
		}
	};
	
	protected interface PictureBlender {
		/** Called by exposing thread */
		public void blend(int[] previewRGBData, int frameCount);
	}
	protected final PictureBlender[] mPictureBlender = {
		// Average
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				blendAverage(mResultBitmap, previewRGBData, frameCount);
			}
		},

		// Max1
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				blendMax1(mResultBitmap, previewRGBData);
			}
		},

		// Max2
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				blendMax2(mResultBitmap, previewRGBData);
			}
		},

		// Screen
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				blendScreen(mResultBitmap, previewRGBData);
			}
		},

		// Translucence
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				if(frameCount > 1)
					blendTranslucence(mResultBitmap, previewRGBData, SettingsManager.mAlpha);
				else
					mResultBitmap.setPixels(previewRGBData, 0, mPictureWidth, 0, 0, mPictureWidth, mPictureHeight);
			}
		},

		// Screen + Translucence
		new PictureBlender() {
			@Override
			public void blend(int[] previewRGBData, int frameCount) {
				blendScreenTranslucence(mResultBitmap, previewRGBData, SettingsManager.mAlpha);
			}
		}
	};

	
	static {
		System.loadLibrary("ImageJni");
	}
	
	protected static final native void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height);
	protected static final native void blenderInitialize(Bitmap result, int blendingMode);
	protected static final native void blenderUninitialize();
	protected static final native void blendAverage(Bitmap resultBitmap, int[] previewRGBData, int frameCount);
	protected static final native void blendMax1(Bitmap resultBitmap, int[] previewRGBData);
	protected static final native void blendMax2(Bitmap resultBitmap, int[] previewRGBData);
	protected static final native void blendScreen(Bitmap resultBitmap, int[] previewRGBData);
	protected static final native void blendTranslucence(Bitmap resultBitmap, int[] previewRGBData, int alpha);
	protected static final native void blendScreenTranslucence(Bitmap resultBitmap, int[] previewRGBData, int alpha);
}
