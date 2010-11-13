package aharisu.Misc.SimpleAlarm;


import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarm;
import aharisu.Misc.SimpleAlarm.AlarmSystem.Alarms;
import aharisu.Misc.SimpleAlarm.R.drawable;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class AlarmWidget extends AppWidgetProvider{
	public static final String ActionViewUpdate = "aharisu.misc.SimpleAlarm.VIEW_UPDATE";
	
	private static final String ActionWidgetControl = "aharisu.misc.SimpleAlarm.WIDGET_CONTROL";
	private static final String UriScheme="alarm_widget";
	
	private static final String PreferencesName = "AlarmWidgetPrefs";
	private static final String PreferencesShowOriginIndexPattern = "origin-%d";
	private static final String PreferencesAppWidgetIds = "ids";
	
	private static final String ControlPrev = "prev";
	private static final String ControlNext = "next";
	private static final String ControlAlarm1 = "alarm1";
	private static final String ControlAlarm2 = "alarm2";
	private static final String ControlAlarm3 = "alarm3";
	private static final String ControlStartActivity = "activity";
	
	private static final int TotalAlarmView = 3;
	private final int[] _remoteViewLayoutIds = new int[] {
			R.id.widget_alarm_1,
			R.id.widget_alarm_2,
			R.id.widget_alarm_3
	};
	private final int[] _remoteViewTimeIds = new int[] {
			R.id.widget_alarm_1_time_text,
			R.id.widget_alarm_2_time_text,
			R.id.widget_alarm_3_time_text,
	};
	private final int[] _remoteViewEnableIds = new int[] {
			R.id.widget_alarm_1_alarm_enable,
			R.id.widget_alarm_2_alarm_enable,
			R.id.widget_alarm_3_alarm_enable,
	};
	
	
	@Override public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if(ActionViewUpdate.equals(action)) {
			//widgetすべてを更新する
			int[] appWidgetIds = getAppWidgetIds(context);
			for(int id : appWidgetIds) {
				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
				updateLayout(context, views, id);
				
				AppWidgetManager.getInstance(context).updateAppWidget(id, views);
			}
		} else if(ActionWidgetControl.equals(action)) {
			final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				onHandleAction(context, appWidgetId, intent.getData());
			}
		} else {
			super.onReceive(context, intent);
		}
		
	}
	
	private void onHandleAction(Context context, int appWidgetId, Uri data) {
		String controlType = data.getFragment();
		
		boolean clickAlarm = false;
		boolean updateView = false;
		int origin = getOrigin(context, appWidgetId);
		
		if(controlType.equals(ControlPrev)) {
			--origin;
			setOrigin(origin, context, appWidgetId);
			updateView = true;
		} else if(controlType.equals(ControlNext)) {
			++origin;
			setOrigin(origin, context, appWidgetId);
			updateView = true;
		} else if(controlType.equals(ControlStartActivity)) {
			//イベント送信インテントを設定しなおす
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widget_start_activity, makeControlPendingIntent(context, ControlStartActivity, appWidgetId));
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
			
			//activityを起動
			Intent intent = new Intent();
			intent.setClassName("aharisu.Misc.SimpleAlarm", "aharisu.Misc.SimpleAlarm.AlarmActivity");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
					Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			context.startActivity(intent);
		} else if(controlType.equals(ControlAlarm1)) {
			updateView = clickAlarm = true;
		} else if(controlType.equals(ControlAlarm2)) {
			updateView = clickAlarm = true;
			origin += 1;
		} else if(controlType.equals(ControlAlarm3)) {
			updateView = clickAlarm = true;
			origin += 2;
		}
		
		if(clickAlarm) {
			Cursor cursor = null;
			try {
				cursor = Alarms.getAlarmsCursor(context.getContentResolver());
				if(cursor.moveToPosition(origin)) {
					Alarm alarm = new Alarm(cursor, context);
					alarm.time = 0;
					Alarms.enableAlarm(context, alarm.id, !alarm.enabled);
				}
			}finally {
				if(cursor != null) {
					cursor.close();
				}
			}
		}
		
		if(updateView) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			updateLayout(context, views, appWidgetId);
			
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
		}
		
	}
	
	@Override public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		for(int id : appWidgetIds) {
			registerAppWidgetId(context, id);
			
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			updateLayout(context, views, id);
			
			AppWidgetManager.getInstance(context).updateAppWidget(id, views);
		}
		
	}
	
	private void updateLayout(Context context, RemoteViews views, int appWidgetId) {
		//イベント送信登録
		views.setOnClickPendingIntent(R.id.widget_prev, makeControlPendingIntent(context, ControlPrev, appWidgetId));
		views.setOnClickPendingIntent(R.id.widget_next, makeControlPendingIntent(context, ControlNext, appWidgetId));
		views.setOnClickPendingIntent(R.id.widget_start_activity, makeControlPendingIntent(context, ControlStartActivity, appWidgetId));
		views.setOnClickPendingIntent(R.id.widget_alarm_1, makeControlPendingIntent(context, ControlAlarm1, appWidgetId));
		views.setOnClickPendingIntent(R.id.widget_alarm_2, makeControlPendingIntent(context, ControlAlarm2, appWidgetId));
		views.setOnClickPendingIntent(R.id.widget_alarm_3, makeControlPendingIntent(context, ControlAlarm3, appWidgetId));
		
		int origin = getOrigin(context, appWidgetId);
		if(origin < 0) {
			origin = 0;
		}
		
		Cursor cursor = null;
		try {
			int index = 0;
			
			cursor = Alarms.getAlarmsCursor(context.getContentResolver());
			int count = cursor.getCount();
			if(count != 0) {
				if(TotalAlarmView <= count && count - origin < TotalAlarmView) {
					//エラー修正
					origin = origin - (TotalAlarmView - (count - origin));
					setOrigin(origin, context, appWidgetId);
				}
				
				if(cursor.moveToPosition(origin)) {
					for(;index < 3 && !cursor.isAfterLast(); ++index, cursor.moveToNext()) {
						Alarm alarm = new Alarm(cursor, context);
						
						views.setBoolean(_remoteViewLayoutIds[index], "setEnabled", true);
						views.setViewVisibility(_remoteViewEnableIds[index], View.VISIBLE);
						views.setViewVisibility(_remoteViewTimeIds[index], View.VISIBLE);
					
						views.setTextViewText(_remoteViewTimeIds[index], 
								String.format("%02d:%02d", alarm.hour,  alarm.minutes));
						views.setImageViewResource(_remoteViewEnableIds[index],
								alarm.enabled ? drawable.alarm_enable : drawable.alarm_disable);
					}
				}
			}
				
			views.setBoolean(R.id.widget_next, "setEnabled", (index + origin < count));
			views.setBoolean(R.id.widget_prev, "setEnabled", origin != 0);
			
			for(;index < 3; ++index) {
				views.setBoolean(_remoteViewLayoutIds[index], "setEnabled", false);
				views.setViewVisibility(_remoteViewEnableIds[index], View.INVISIBLE);
				views.setViewVisibility(_remoteViewTimeIds[index], View.INVISIBLE);
			}
			
			
		} finally {
			if(cursor != null)
				cursor.close();
		}
		
	}
	
	@Override public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		//プリファレンスの削除
		for(int id : appWidgetIds) {
			deleteDataForId(context, id);
			unregisterAppWidgetId(context, id);
		}
	}
	
	private int getOrigin(Context context, int appWidgetId) {
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		return config.getInt(String.format(PreferencesShowOriginIndexPattern, appWidgetId), 0);
	}
	
	private void setOrigin(int origin, Context context, int appWidgetId) {
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = config.edit();
		
		edit.putInt(String.format(PreferencesShowOriginIndexPattern, appWidgetId), origin);
		
		edit.commit();
	}
	
	private void deleteDataForId(Context context, int appWidgetId) {
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = config.edit();
		
		edit.remove(String.format(PreferencesShowOriginIndexPattern, appWidgetId));
		
		edit.commit();
	}
	
	private void registerAppWidgetId(Context context, int appWidgetId){
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		String ids = config.getString(PreferencesAppWidgetIds, "");
		
		String[] strIds = ids.split(",");
		boolean isFound = false;
		for(String id : strIds) {
			if(!id.equals("") && Integer.valueOf(id) == appWidgetId) {
				isFound = true;
				break;
			}
		}
		
		if(!isFound) {
			SharedPreferences.Editor edit = config.edit();
			if(1 < strIds.length || strIds[0].equals("")) {
				edit.putString(PreferencesAppWidgetIds, String.valueOf(appWidgetId));
			} else {
				edit.putString(PreferencesAppWidgetIds, ids + "," + String.valueOf(appWidgetId));
			}
			edit.commit();
		}
	}
	
	private void unregisterAppWidgetId(Context context, int appWidgetId) {
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		String[]strIds = config.getString(PreferencesAppWidgetIds, "").split(",");
		
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for(String id : strIds) {
			if(!id.equals("") && Integer.valueOf(id) != appWidgetId) {
				if(!isFirst) {
					builder.append(",");
				} else {
					isFirst = false;
				}
				builder.append(id);
			}
		}
		
		SharedPreferences.Editor edit = config.edit();
		if(isFirst) {
			edit.remove(PreferencesAppWidgetIds);
		} else {
			edit.putString(PreferencesAppWidgetIds, builder.toString());
		}
		edit.commit();
	}
	
	private int[] getAppWidgetIds(Context context) {
		SharedPreferences config = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
		String[] strIds = config.getString(PreferencesAppWidgetIds, "").split(",");
		if(strIds.length == 1 && strIds[0].equals("")) {
			return new int[0];
		}
		
		int[] ids = new int[strIds.length];
		int index = 0;
		for(String strId : strIds) {
			ids[index] = Integer.valueOf(strId);
			++index;
		}
		
		return ids;
	}
	
	private PendingIntent makeControlPendingIntent(Context context, String command, int appWidgetId) {
		Intent intent = new Intent();
		intent.setAction(ActionWidgetControl);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		Uri data = Uri.withAppendedPath(Uri.parse(
				new StringBuilder().append(UriScheme).append("://widget/id/#").append(command).toString()),
				String.valueOf(appWidgetId));
		intent.setData(data);
		
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
	}

}
