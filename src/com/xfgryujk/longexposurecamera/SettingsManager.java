package com.xfgryujk.longexposurecamera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SettingsManager {
	@SuppressWarnings("unused")
	private static final String TAG = "SettingsManager";
	
	protected static MainActivity mMainActivity;
	
	public static int mBlendingMode;
	public static String mPath;
	public static int mEV;
	public static String mWhiteBalance;
	public static int mResolution;
	public static int mISO;
	
	
	/** Load settings */
	public static void initialize(MainActivity mainActivity) {
		mMainActivity = mainActivity;
		
		// Load settings
        Resources res = mMainActivity.getResources();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mMainActivity);
        mBlendingMode   = pref.getInt(res.getString(R.string.pref_blending_mode), 0);
        mPath           = pref.getString(res.getString(R.string.pref_path), Environment.getExternalStorageDirectory().getPath() + "/FakeLongExposureCamera");
        mEV             = pref.getInt(res.getString(R.string.pref_EV), 0);
        mWhiteBalance   = pref.getString(res.getString(R.string.pref_white_balance), "auto");
        mResolution     = pref.getInt(res.getString(R.string.pref_resolution), 0);
        mISO            = pref.getInt(res.getString(R.string.pref_ISO), 0);
	}

	/** Show settings dialog */
	public static final OnClickListener mOnButtonSettingClick = new OnClickListener() { 
		@Override
		public void onClick(View buttonView) { 
			AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
			ListView view = new ListView(mMainActivity);
			view.setOnItemClickListener(mOnDialogItemClick);
			
			// Set items
			List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> item;
	        Resources res = mMainActivity.getResources();
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.blending_mode));
			item.put("value", res.getStringArray(R.array.blending_modes_description)[mBlendingMode]);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.path));
			item.put("value", mPath);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.EV));
			item.put("value", Integer.toString(mEV));
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.white_balance));
			item.put("value", mWhiteBalance);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.resolution));
			Camera camera = mMainActivity.mCameraPreview.getCamera();
			Parameters params = camera.getParameters();
			List<Size> sizes = params.getSupportedPreviewSizes();
			String[] sizesString = new String[sizes.size()];
			for(int i = 0; i < sizes.size(); i++)
				sizesString[i] = sizes.get(i).width + " * " + sizes.get(i).height;
			item.put("value", sizesString[mResolution]);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.ISO));
			item.put("value", "");
			data.add(item);
			
			SimpleAdapter adapter = new SimpleAdapter(mMainActivity, 
					data, R.layout.settings_list, 
					new String[] {"name", "value"}, 
					new int[] {R.id.setting_name, R.id.setting_value}
			);
			view.setAdapter(adapter);
			
			// Set dialog
			Dialog dialog = builder.setView(view).create();
			Window dialogWindow = dialog.getWindow();
	        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
	        dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);
	        lp.x      = 30;
	        lp.y      = 20;
	        lp.width  = 100;
	        lp.height = 300;
	        lp.alpha  = 0.3f;
	        dialogWindow.setAttributes(lp);
	        dialog.show();
	        
	        
			// test ISO
			/*String flat = params.flatten();
			Log.i(TAG, flat);
			String[] isoValues	= null;
			String values_keyword = null;
			String iso_keyword	= null;
			if(flat.contains("iso-values")) {
				// most used keywords
				values_keyword = "iso-values";
				iso_keyword	= "iso";
			} else if(flat.contains("iso-mode-values")) {
				// google galaxy nexus keywords
				values_keyword = "iso-mode-values";
				iso_keyword	= "iso";
			} else if(flat.contains("iso-speed-values")) {
				// micromax a101 keywords
				values_keyword = "iso-speed-values";
				iso_keyword	= "iso-speed";
			} else if(flat.contains("nv-picture-iso-values")) {
				// LG dual p990 keywords
				values_keyword = "nv-picture-iso-values";
				iso_keyword	= "nv-picture-iso";
			}
			// add other eventual keywords here...
			if(iso_keyword != null) {
				// flatten contains the iso key!!
				String iso = flat.substring(flat.indexOf(values_keyword));
				iso = iso.substring(iso.indexOf("=") + 1);
				if(iso.contains(";"))
					iso = iso.substring(0, iso.indexOf(";"));

				isoValues = iso.split(",");
				
				for(String str : isoValues)
					Log.i(TAG, str);
			} else {
				// iso not supported in a known way
				Log.i(TAG, "ISO is not supported");
			}*/
		}
	};
	
	protected static OnItemClickListener mOnDialogItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, final View view, int position, long id) {
			final Resources res = mMainActivity.getResources();
	        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mMainActivity);
			final Camera camera = mMainActivity.mCameraPreview.getCamera();
			final Parameters params = camera.getParameters();
			AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
			switch(position)
			{
			case 0: // Blending mode
				final String[] blendingModes = res.getStringArray(R.array.blending_modes_description);
				builder.setTitle(res.getString(R.string.blending_mode))
				.setItems(blendingModes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBlendingMode = which;
						pref.edit().putInt(res.getString(R.string.pref_blending_mode), mBlendingMode).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(blendingModes[which]);
					}
				})
				.create()
				.show();
				break;
				
			case 1: // Path
				final EditText edit = new EditText(mMainActivity);
				edit.setText(mPath);
				builder.setTitle(res.getString(R.string.path))
				.setView(edit)
				.setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPath = edit.getText().toString();
						pref.edit().putString(res.getString(R.string.pref_path), mPath).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(mPath);
					}
				})
				.setNegativeButton(res.getString(R.string.no), null)
				.show();
				break;
				
			case 2: // EV
				final SeekBar seekBar = new SeekBar(mMainActivity);
				seekBar.setMax(params.getMaxExposureCompensation() - params.getMinExposureCompensation());
				seekBar.setProgress(mEV - params.getMinExposureCompensation());
				builder.setTitle(res.getString(R.string.EV))
				.setView(seekBar)
				.setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mEV = seekBar.getProgress() + params.getMinExposureCompensation();
						pref.edit().putInt(res.getString(R.string.pref_EV), mEV).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(Integer.toString(mEV));
						params.setExposureCompensation(mEV);
						camera.setParameters(params);
					}
				})
				.setNegativeButton(res.getString(R.string.no), null)
				.show();
				break;
				
			case 3: // White balance
				final List<String> whiteBalances = params.getSupportedWhiteBalance();
				builder.setTitle(res.getString(R.string.white_balance))
				.setItems(whiteBalances.toArray(new String[whiteBalances.size()]), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mWhiteBalance = whiteBalances.get(which);
						pref.edit().putString(res.getString(R.string.pref_white_balance), mWhiteBalance).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(whiteBalances.get(which));
						params.setWhiteBalance(mWhiteBalance);
						camera.setParameters(params);
					}
				})
				.create()
				.show();
				break;
				
			case 4: // Resolution
				final List<Size> sizes = params.getSupportedPreviewSizes();
				final String[] sizesString = new String[sizes.size()];
				for(int i = 0; i < sizes.size(); i++)
					sizesString[i] = sizes.get(i).width + " * " + sizes.get(i).height;
				builder.setTitle(res.getString(R.string.resolution))
				.setItems(sizesString, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mResolution = which;
						pref.edit().putInt(res.getString(R.string.pref_resolution), mResolution).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(sizesString[which]);
						mMainActivity.mCameraPreview.resetCamera();
					}
				})
				.create()
				.show();
				break;
				
			case 5: // ISO
				break;
			}
		}
	};
}
