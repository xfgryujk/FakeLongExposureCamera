package com.xfgryujk.longexposurecamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PreviewActivity extends Activity {
	public ImageView mResultPreview;
	public Button mButtonCancel;
	public Button mButtonSave;
	
	protected Bitmap mResultBitmap;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN , 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// Initialize UI
		setContentView(R.layout.preview_activity);
		mResultPreview = (ImageView)findViewById(R.id.result_preview);
		mButtonCancel  = (Button)findViewById(R.id.cancel_button);
		mButtonSave	   = (Button)findViewById(R.id.save_button);
		
		mButtonCancel.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View view) { 
				PreviewActivity.this.finish();
			}
		});

		mResultBitmap = CameraPreview.mResultBitmap;
		CameraPreview.mResultBitmap = null;
		mResultPreview.setImageBitmap(mResultBitmap);
	}
	
	@SuppressLint("SimpleDateFormat")
	public void onButtonSaveClick(View view) {
		// Save the picture
		try {
			// Create file
			File dir = new File(SettingsManager.mPath);
			if(!dir.exists())
				if(!dir.mkdirs()) {
					Toast.makeText(this, getResources().getString(R.string.failed_to_create_directory), 
		            		Toast.LENGTH_SHORT).show();
					return;
				}
			File file = new File(SettingsManager.mPath + "/" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
			
			// Write file
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			
			if(mResultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos))
			{
				bos.flush();
				Toast.makeText(this, getResources().getString(R.string.saved_to) + " " + file.getPath(), 
	            		Toast.LENGTH_SHORT).show();
			}
			else
				Toast.makeText(this, getResources().getString(R.string.failed_to_write_file) + file.getPath(), 
	            		Toast.LENGTH_SHORT).show();
			
			bos.close();
			finish();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mResultBitmap.recycle();
		mResultBitmap = null;
	}
}
