package aharisu.Misc.SimpleAlarm;


import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarm;
import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarms;
import aharisu.Misc.SimpleAlarm.AlarmSystem.ToastMaster;
import aharisu.Misc.SimpleAlarm.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Matrix.ScaleToFit;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IInterface;
import android.preference.RingtonePreference;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class SetAlarmActivity extends Activity{
	private static final int REQUEST_PICK_SOUND = 10;
	
	//private TimeInputView _timeInputView;
	private Uri _soundUri;
	
	private Alarm _alarm;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		int id = getIntent().getIntExtra(Alarms.ALARM_ID, -1);
		
		if(id == -1) {
			_alarm = new Alarm(this);
		} else {
			_alarm = Alarms.getAlarm(this, id);
			if(_alarm == null) {
				finish();
				return;
			}
		}
		_soundUri = _alarm.alert;
		
		initializeComponents();
	}
	
	private void initializeComponents() {
		Context context = this;
		
		LayoutInflater inflater = LayoutInflater.from(context);
		setContentView(inflater.inflate(R.layout.set_alarm_activity_layout, null));
		
		TimeInputView timeInput = (TimeInputView)findViewById(R.id_set_alarm_activity.TineInput);
		timeInput.setHour(_alarm.hour);
		timeInput.setMinutes(_alarm.minutes);
		
		((TextView)findViewById(R.id_set_alarm_activity.CurrentSelectedSoundLabel)).setText(getSoundTitle(_soundUri));
		
		((SeekBar)findViewById(R.id_set_alarm_activity.VolumeSeekBar)).setProgress(_alarm.volume - 1);
		
		findViewById(R.id_set_alarm_activity.SelectSound).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				showSoundSelectDialog();
			}
		});
		
		findViewById(R.id_set_alarm_activity.OK).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				long time = saveAlarm();
				popAlarmSetToast(getApplicationContext(), time);
				finish();
			}
		});
	}
	
	private void showSoundSelectDialog() {
		Intent intent = new Intent(this, PickSoundActivity.class);
		intent.putExtra(PickSoundActivity.SelectedSoundURIExtra, _soundUri);
		startActivityForResult(intent, REQUEST_PICK_SOUND);
	}
	
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_PICK_SOUND && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			
			((TextView)findViewById(R.id_set_alarm_activity.CurrentSelectedSoundLabel)).setText(getSoundTitle(uri));
			_soundUri = uri;
			return;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private String getSoundTitle(Uri uri) {
		Cursor cursor = managedQuery(uri, null, null, null, null);
		if(cursor.moveToFirst()) {
			int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			return cursor.getString(titleIndex);
		}
		
		return "Unknown Title";
	}
	
	private long saveAlarm() {
		TimeInputView timeInput = (TimeInputView)findViewById(R.id_set_alarm_activity.TineInput);
		
		_alarm.hour = timeInput.getHour();
		_alarm.minutes = timeInput.getMinutes();
		_alarm.alert = _soundUri;
		_alarm.volume = ((SeekBar)findViewById(R.id_set_alarm_activity.VolumeSeekBar)).getProgress() + 1;
		_alarm.enabled = true;
		_alarm.time = 0;
		
		long time;
		if(_alarm.id == -1) {
			time = Alarms.addAlarm(this, _alarm);
		} else {
			time = Alarms.setAlarm(this, _alarm);
		}
		
		return time;
	}
	
    /**
     * Display a toast that tells the user how long until the alarm
     * goes off.  This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute) {
        popAlarmSetToast(context, Alarms.calculateAlarm(hour, minute).getTimeInMillis());
    }

    private static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    private static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
    
}
