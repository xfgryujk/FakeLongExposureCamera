package com.xfgryujk.longexposurecamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final String TAG = "MainActivity";
	
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
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initialize UI
		setContentView(R.layout.activity_main);
		mCameraPreview = (CameraPreview)findViewById(R.id.camera_preview);
		mResultPreview = (ImageView)findViewById(R.id.result_preview);
		mButtonShutter = (Button)findViewById(R.id.shutter_button);
		mButtonSetting = (Button)findViewById(R.id.setting_button);
		
		mCameraPreview.setResultPreview(mResultPreview);
		mCameraPreview.setOnClickListener(mCameraPreview);
		
		mButtonShutter.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View view) { 
				mCameraPreview.onShutterClick();
			}
		});
		
		mButtonSetting.setOnClickListener(mOnButtonSettingClick);
	}
	
	@Override
    protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();
		mCameraPreview.releaseCamera();
    }
	
	@Override
    protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
    }

	private long mLastBackTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
	    {   
	        if(System.currentTimeMillis() - mLastBackTime > 2000)
	        {  
	            Toast.makeText(this, getResources().getString(R.string.click_again_to_exit), 
	            		Toast.LENGTH_SHORT).show();                                
	            mLastBackTime = System.currentTimeMillis();   
	        }
	        else
	            finish();
	        return true;   
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.exit(0);
	}
	
	protected OnClickListener mOnButtonSettingClick = new OnClickListener() { 
		@Override
		public void onClick(View view) { 
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setView(LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_settings, null));
			builder.create().show();
		}
	};
}
