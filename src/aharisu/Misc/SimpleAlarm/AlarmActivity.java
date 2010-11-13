package aharisu.Misc.SimpleAlarm;


import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarm;
import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarms;
import aharisu.Misc.SimpleAlarm.AlarmSystem.ToastMaster;
import aharisu.Misc.SimpleAlarm.R.drawable;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView.ScaleType;

public class AlarmActivity extends Activity implements View.OnCreateContextMenuListener {
	
	private class AlarmTimeAdapter extends CursorAdapter {
		private static final int TextTimeId = 1;
		private static final int TextVolumeId = 4;
		private static final int TextTitleId = 5;
		private static final int CheckEnableId = 2;
		
		private Typeface _typeFace;
		
		public AlarmTimeAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			
			_typeFace = Typeface.createFromAsset(context.getAssets(), "fonts/Clockopia.ttf");
		}
		
		@Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
			RelativeLayout layout = new RelativeLayout(context);
			
			RelativeLayout.LayoutParams params;
			
			TextView tvwTime = new TextView(context);
			params = new RelativeLayout.LayoutParams(360,70);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			tvwTime.setLayoutParams(params);
			tvwTime.setTextSize(45);
			tvwTime.setGravity(Gravity.CENTER);
			tvwTime.setTypeface(_typeFace);
			tvwTime.setId(TextTimeId);
			layout.addView(tvwTime);
			
			ImageView ivwVolumeIcon = new ImageView(context);
			params = new RelativeLayout.LayoutParams(32, 24);
			params.addRule(RelativeLayout.BELOW, tvwTime.getId());
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			params.topMargin = 3;
			ivwVolumeIcon.setLayoutParams(params);
			ivwVolumeIcon.setScaleType(ScaleType.CENTER_INSIDE);
			ivwVolumeIcon.setImageResource(drawable.speaker_on);
			ivwVolumeIcon.setId(10);
			layout.addView(ivwVolumeIcon);
			
			TextView tvwVolume = new TextView(context);
			params = new RelativeLayout.LayoutParams(50, 40);
			params.addRule(RelativeLayout.BELOW, tvwTime.getId());
			params.addRule(RelativeLayout.RIGHT_OF, ivwVolumeIcon.getId());
			params.leftMargin = 5;
			tvwVolume.setLayoutParams(params);
			tvwVolume.setTextSize(15);
			tvwVolume.setTextColor(0xffcccccc);
			tvwVolume.setId(TextVolumeId);
			layout.addView(tvwVolume);
			
			TextView tvwTitle = new TextView(context);
			params = new RelativeLayout.LayoutParams(
					245, 40);
			params.addRule(RelativeLayout.BELOW, tvwTime.getId());
			params.addRule(RelativeLayout.RIGHT_OF, tvwVolume.getId());
			params.leftMargin = 10;
			tvwTitle.setLayoutParams(params);
			tvwTitle.setTextSize(15);
			tvwTitle.setTextColor(0xff43a4cb);
			tvwTitle.setId(TextTitleId);
			layout.addView(tvwTitle);
			
			CheckBox chkEnable = new CheckBox(context);
			params = new RelativeLayout.LayoutParams(150,110);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			params.addRule(RelativeLayout.RIGHT_OF, tvwTime.getId());
			params.addRule(RelativeLayout.RIGHT_OF, tvwTitle.getId());
			chkEnable.setLayoutParams(params);
			chkEnable.setClickable(true);
			chkEnable.setFocusable(false);
			chkEnable.setButtonDrawable(drawable.alarm_enable_button);
			chkEnable.setId(CheckEnableId);
			layout.addView(chkEnable);
			
			return layout;
		}
		
		@Override public void bindView(View view, final Context context, Cursor cursor) {
			final Alarm alarm = new Alarm(cursor, context);
			
			((TextView)view.findViewById(TextTimeId)).setText(
					String.format("%02d:%02d" , alarm.hour, alarm.minutes));
			
			((TextView)view.findViewById(TextVolumeId)).setText(Integer.toString(alarm.volume));
			
			((TextView)view.findViewById(TextTitleId)).setText(getSoundTitle(alarm.alert));
			
			CheckBox chkEnable = (CheckBox)view.findViewById(CheckEnableId);
			chkEnable.setOnCheckedChangeListener(null);
			chkEnable.setChecked(alarm.enabled);
			chkEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				final Alarm _alarm = alarm;
				
				@Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					_alarm.time = 0;
					Alarms.enableAlarm(context, _alarm.id, isChecked);
					if(isChecked) {
						SetAlarmActivity.popAlarmSetToast(getApplicationContext(), _alarm.hour, _alarm.minutes);
					}
				}
			});
		}
		
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initializeComponents();
    }
    
    private void initializeComponents() {
    	Context context = this;
    	
    	LayoutInflater inflater = LayoutInflater.from(context);
    	setContentView(inflater.inflate(R.layout.alarm_activity_layout, null));
    	
    	findViewById(R.id_alarm_activity.CreateNew).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
    			startActivity(new Intent(AlarmActivity.this, SetAlarmActivity.class));
			}
		});
    	
    	ListView list = (ListView)findViewById(R.id_alarm_activity.AlarmList);
    	list.setAdapter(new AlarmTimeAdapter(context, Alarms.getAlarmsCursor(getContentResolver())));
    	list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    		@Override public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
    			Intent intent = new Intent(AlarmActivity.this, SetAlarmActivity.class);
    			intent.putExtra(Alarms.ALARM_ID, (int)id);
    			startActivity(intent);
    		}
		});
    	list.setOnCreateContextMenuListener(this);
    }
    
    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("削除").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override public boolean onMenuItemClick(MenuItem item) {
				final AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
				Alarms.deleteAlarm(AlarmActivity.this, (int)info.id);
				
				return true;
			}
		});
    }
    
	private String getSoundTitle(Uri uri) {
		Cursor cursor = managedQuery(uri, null, null, null, null);
		if(cursor.moveToFirst()) {
			int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			return cursor.getString(titleIndex);
		}
		
		return "Unknown Title";
	}
}
