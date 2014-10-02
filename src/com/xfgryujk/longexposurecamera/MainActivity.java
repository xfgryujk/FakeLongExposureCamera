package com.xfgryujk.longexposurecamera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private CameraPreview mCameraPreview;
	private ImageView mResultPreview;
	private Button mButtonShutter;
	private Button mButtonSetting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Landscape
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Full screen, keep screen on
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				, WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initialize UI
		setContentView(R.layout.activity_main);
		mCameraPreview = (CameraPreview)findViewById(R.id.camera_preview);
		mResultPreview = (ImageView)findViewById(R.id.result_preview);
		mButtonShutter = (Button)findViewById(R.id.shutter_button);
		mButtonSetting = (Button)findViewById(R.id.setting_button);
		
		mCameraPreview.setResultPreview(mResultPreview);
		mCameraPreview.setOnClickListener(mCameraPreview);
		
		mButtonShutter.setOnClickListener(new OnClickListener() { 
			public void onClick(View view) { 
				mCameraPreview.onShutterClick();
			}
		});
		
		mButtonSetting.setOnClickListener(new OnClickListener() { 
			public void onClick(View view) { 
				// Unfinished
			}
		});
	}
}
