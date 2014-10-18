package com.xfgryujk.longexposurecamera;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public CameraPreview mCameraPreview;
	public ImageView mResultPreview;
	public TextView mOutputText;
	public Button mButtonShutter;
	public Button mButtonSetting;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Full screen, keep screen on
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initialize UI
		setContentView(R.layout.main_activity);
		mCameraPreview = (CameraPreview)findViewById(R.id.camera_preview);
		mResultPreview = (ImageView)findViewById(R.id.result_preview);
		mOutputText    = (TextView)findViewById(R.id.output_text);
		mButtonShutter = (Button)findViewById(R.id.shutter_button);
		mButtonSetting = (Button)findViewById(R.id.setting_button);
		
		mCameraPreview.setOnClickListener(mCameraPreview);
		
		mButtonShutter.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View view) { 
				mCameraPreview.onShutterClick();
			}
		});
		
		SettingsManager.initialize(this);
		mButtonSetting.setOnClickListener(SettingsManager.mOnButtonSettingClick);
	}

	private long mLastBackTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
	    {
	    	if(mCameraPreview.isExposing())
	    		mCameraPreview.onShutterClick();
	    	else if(System.currentTimeMillis() - mLastBackTime >= 2000)
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
}
