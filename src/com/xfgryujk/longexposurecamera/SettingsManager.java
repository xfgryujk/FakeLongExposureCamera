package com.xfgryujk.longexposurecamera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
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
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SettingsManager {
	protected static MainActivity mMainActivity;
	
	public static int mBlendingMode;
	public static int mAlpha;
	public static int mEV;
	public static String mWhiteBalance;
	public static int mResolution;
	public static String mISO;
	/** 0 No auto stop, 1 frame, 2 second */
	public static int mAutoStop;
	public static int mAutoStopTime;
	public static long mAutoStopTimeMS;
	public static long mMinDelay;
	public static int mMaxThreadCount;
	public static String mPath;
	
	
	/** Load settings */
	public static void initialize(MainActivity mainActivity) {
		mMainActivity = mainActivity;
		
		// Load settings
        Resources res = mMainActivity.getResources();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mMainActivity);
        mBlendingMode   = pref.getInt(res.getString(R.string.pref_blending_mode), 0);
        mAlpha          = pref.getInt(res.getString(R.string.pref_alpha), 20);
        mEV             = pref.getInt(res.getString(R.string.pref_EV), 0);
        mWhiteBalance   = pref.getString(res.getString(R.string.pref_white_balance), "auto");
        mResolution     = pref.getInt(res.getString(R.string.pref_resolution), 0);
        mISO            = pref.getString(res.getString(R.string.pref_ISO), "auto");
        mAutoStop       = pref.getInt(res.getString(R.string.pref_auto_stop), 0);
        mAutoStopTime   = pref.getInt(res.getString(R.string.pref_auto_stop_time), 0);
        if(mAutoStop == 2)
        	mAutoStopTimeMS = mAutoStopTime * 1000;
        mMinDelay       = pref.getLong(res.getString(R.string.pref_min_delay), 0);
        mMaxThreadCount = pref.getInt(res.getString(R.string.pref_thread_count), Math.min(16, Runtime.getRuntime().availableProcessors() * 2));
        mPath           = pref.getString(res.getString(R.string.pref_path), Environment.getExternalStorageDirectory().getPath() + "/FakeLongExposureCamera");
	}

	/** Show settings dialog */
	public static final OnClickListener mOnButtonSettingClick = new OnClickListener() { 
		@SuppressLint("InflateParams")
		@Override
		public void onClick(View buttonView) {
			View view = LayoutInflater.from(mMainActivity).inflate(R.layout.settings_dialog, null);

			// Set list
			ListView listView = (ListView)view.findViewById(R.id.settings_list);
			listView.setOnItemClickListener(mOnDialogItemClick);
			
			// Set items
			List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> item;
	        Resources res = mMainActivity.getResources();
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.blending_mode));
			item.put("value", res.getStringArray(R.array.blending_modes_description)[mBlendingMode]);
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
			item.put("value", mISO);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.auto_stop));
			if(mAutoStop == 0)
				item.put("value", res.getStringArray(R.array.auto_stop_types)[0]);
			else
				item.put("value", Integer.toString(mAutoStopTime) + " " + res.getStringArray(R.array.auto_stop_types)[mAutoStop]);
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.min_delay_per_frame));
			item.put("value", Long.toString(mMinDelay) + " ms");
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.thread_count));
			item.put("value", Integer.toString(mMaxThreadCount));
			data.add(item);
			
			item = new HashMap<String, String>();
			item.put("name", res.getString(R.string.path));
			item.put("value", mPath);
			data.add(item);
			
			SimpleAdapter adapter = new SimpleAdapter(mMainActivity, 
					data, R.layout.settings_list, 
					new String[] {"name", "value"}, 
					new int[] {R.id.setting_name, R.id.setting_value}
			);
			listView.setAdapter(adapter);
			
			// Set dialog
			Dialog dialog = new Dialog(mMainActivity, R.style.settingsDialog);
			dialog.setContentView(view);
			Window dialogWindow = dialog.getWindow();
			DisplayMetrics dm = new DisplayMetrics();
			mMainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
	        LayoutParams lp = dialogWindow.getAttributes();
	        lp.gravity = Gravity.LEFT | Gravity.TOP;
	        lp.x       = 30;
	        lp.y       = 20;
	        lp.height  = Math.min(dm.heightPixels - 40, (int)((data.size() * 36 + 3.5) * dm.density));
	        dialog.setCanceledOnTouchOutside(true);
	        dialog.show();
	        dialogWindow.setAttributes(lp);
		}
	};
	
	protected static OnItemClickListener mOnDialogItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, final View view, int position, long id) {
			final Resources res = mMainActivity.getResources();
	        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mMainActivity);
			final Camera camera = mMainActivity.mCameraPreview.getCamera();
			final Parameters params = camera.getParameters();
			final AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
			
			switch(position)
			{
			case 0: // Blending mode
				final String[] blendingModes = res.getStringArray(R.array.blending_modes_description);
				builder.setTitle(res.getString(R.string.blending_mode))
				.setItems(blendingModes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, final int which) {
						if(which < 4)
						{
							mBlendingMode = which;
							pref.edit().putInt(res.getString(R.string.pref_blending_mode), mBlendingMode).commit();
							((TextView)view.findViewById(R.id.setting_value)).setText(blendingModes[which]);
						}
						else // Set alpha
							showSeekBarDialog(res.getString(R.string.opacity), 1, 254, mAlpha, 
									new OnSeekBarDialogPositiveButton() {
								@Override
								public void onClick(int progress) {
									mBlendingMode = which;
									pref.edit().putInt(res.getString(R.string.pref_blending_mode), mBlendingMode).commit();
									((TextView)view.findViewById(R.id.setting_value)).setText(blendingModes[which]);
									mAlpha = progress;
									pref.edit().putInt(res.getString(R.string.pref_alpha), mAlpha).commit();
								}
							});
					}
				})
				.create()
				.show();
				break;
				
			case 1: // EV
				showSeekBarDialog(res.getString(R.string.EV), params.getMinExposureCompensation(), 
						params.getMaxExposureCompensation(), mEV, new OnSeekBarDialogPositiveButton() {
					@Override
					public void onClick(int progress) {
						mEV = progress;
						pref.edit().putInt(res.getString(R.string.pref_EV), mEV).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(Integer.toString(mEV));
						params.setExposureCompensation(mEV);
						camera.setParameters(params);
					}
				});
				break;
				
			case 2: // White balance
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
				
			case 3: // Resolution
				final List<Size> sizes = params.getSupportedPreviewSizes();
				final String[] sizesString = new String[sizes.size()];
				for(int i = 0; i < sizes.size(); i++)
					sizesString[i] = sizes.get(i).width + " * " + sizes.get(i).height;
				builder.setTitle(res.getString(R.string.resolution))
				.setItems(sizesString, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which != mResolution)
						{
							mResolution = which;
							pref.edit().putInt(res.getString(R.string.pref_resolution), mResolution).commit();
							((TextView)view.findViewById(R.id.setting_value)).setText(sizesString[which]);
							mMainActivity.mCameraPreview.resetCamera();
						}
					}
				})
				.create()
				.show();
				break;
				
			case 4: // ISO ***** Not every device support!
				// Test
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
				
				final String[] ISOs = {"auto", "50", "100", "200", "400", "800"
						, "1600", "3200", "sports", "night", "movie"};
				builder.setTitle(res.getString(R.string.ISO))
				.setItems(ISOs, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mISO = ISOs[which];
						pref.edit().putString(res.getString(R.string.pref_ISO), mISO).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(mISO);
						params.set("iso", mISO);
						params.set("iso-speed", mISO);
						params.set("nv-picture-iso", mISO);
						camera.setParameters(params);
					}
				})
				.create()
				.show();
				break;
				
			case 5: // Auto stop
				final String[] types = res.getStringArray(R.array.auto_stop_types);
				// Unit dialog
				new AlertDialog.Builder(mMainActivity)
				.setTitle(res.getString(R.string.auto_stop))
				.setItems(types, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, final int autoStop) {
						if(autoStop == 0) // No auto stop
						{
							mAutoStop = 0;
							pref.edit().putInt(res.getString(R.string.pref_auto_stop), mAutoStop).commit();
							((TextView)view.findViewById(R.id.setting_value)).setText(types[0]);
							return;
						}
						
						final EditText edit2 = new EditText(mMainActivity);
						edit2.setText(Integer.toString(mAutoStopTime));
						edit2.setInputType(InputType.TYPE_CLASS_NUMBER);
						// Time dialog
						builder.setTitle(res.getString(R.string.time))
						.setView(edit2)
						.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int time;
								try {
									time = Integer.parseInt(edit2.getText().toString());
								} catch (NumberFormatException e) {
									return;
								}
								if(time > 1000000 || time < 1)
									return;
								mAutoStop = autoStop;
								pref.edit().putInt(res.getString(R.string.pref_auto_stop), mAutoStop).commit();
								mAutoStopTime   = time;
								mAutoStopTimeMS = mAutoStopTime * 1000;
								pref.edit().putInt(res.getString(R.string.pref_auto_stop_time), mAutoStopTime).commit();
								((TextView)view.findViewById(R.id.setting_value))
									.setText(Integer.toString(mAutoStopTime) + " " + types[mAutoStop]);
							}
						})
						.setNegativeButton(res.getString(R.string.cancel), null)
						.show();
					}
				})
				.create()
				.show();
				break;
				
			case 6: // Min delay per frame
				final EditText edit3 = new EditText(mMainActivity);
				edit3.setText(Long.toString(mMinDelay));
				edit3.setInputType(InputType.TYPE_CLASS_NUMBER);
				builder.setTitle(res.getString(R.string.min_delay_per_frame))
				.setView(edit3)
				.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						long time;
						try {
							time = Long.parseLong(edit3.getText().toString());
						} catch (NumberFormatException e) {
							time = 0;
						}
						if(time > 1000000000)
							return;
						mMinDelay = time;
						pref.edit().putLong(res.getString(R.string.pref_min_delay), mMinDelay).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(Long.toString(mMinDelay) + " ms");
					}
				})
				.setNegativeButton(res.getString(R.string.cancel), null)
				.show();
				break;
				
			case 7: // Thread count
				showSeekBarDialog(res.getString(R.string.thread_count), 1, 16, mMaxThreadCount, 
						new OnSeekBarDialogPositiveButton() {
					@Override
					public void onClick(int progress) {
						mMaxThreadCount = progress;
						pref.edit().putInt(res.getString(R.string.pref_thread_count), mMaxThreadCount).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(Integer.toString(mMaxThreadCount));
					}
				});
				break;
				
			case 8: // Path
				final EditText edit = new EditText(mMainActivity);
				edit.setText(mPath);
				edit.setSingleLine();
				builder.setTitle(res.getString(R.string.path))
				.setView(edit)
				.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPath = edit.getText().toString();
						pref.edit().putString(res.getString(R.string.pref_path), mPath).commit();
						((TextView)view.findViewById(R.id.setting_value)).setText(mPath);
					}
				})
				.setNegativeButton(res.getString(R.string.cancel), null)
				.show();
				break;
			}
		}
	};
	
	protected interface OnSeekBarDialogPositiveButton {
		public void onClick(int progress);
	}
	@SuppressLint("InflateParams")
	protected static void showSeekBarDialog(String title, final int min, int max, int initial, 
			final OnSeekBarDialogPositiveButton onPositiveButton) {
		Resources res = mMainActivity.getResources();
		View view = LayoutInflater.from(mMainActivity).inflate(R.layout.seekbar_dialog, null);
		
		// Set TextView and SeekBar
		final TextView progressText = (TextView)view.findViewById(R.id.progress_text);
		progressText.setText(Integer.toString(initial));
		
		final SeekBar seekBar = (SeekBar)view.findViewById(R.id.seekBar);
		seekBar.setMax(max - min);
		seekBar.setProgress(initial - min);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar thiz, int progress, boolean fromUser) {
				progressText.setText(Integer.toString(progress + min));
			}
			@Override
			public void onStartTrackingTouch(SeekBar thiz) {}
			@Override
			public void onStopTrackingTouch(SeekBar thiz) {}
		});
		
		// Set dialog
		new AlertDialog.Builder(mMainActivity)
		.setTitle(title)
		.setView(view)
		.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onPositiveButton.onClick(seekBar.getProgress() + min);
			}
		})
		.setNegativeButton(res.getString(R.string.cancel), null)
		.show();
	}
}
