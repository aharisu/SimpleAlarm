package aharisu.Misc.SimpleAlarm;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;

public class PickSoundActivity extends Activity{
	public static final String SelectedSoundURIExtra = "selected_sound";
	
	private static final int RingtoneListViewId = 101;
	private static final int MyMusicListViewId = 102;
	
	private static final int KindRingtone = 1;
	private static final int KindMyMusic = 2;
	
	private static final String ShowSelectedSoundTitleFormat = "選択アラーム#%s";
	
	private static class SoundData {
		public final Uri uri;
		public final String title;
		public final int pos;
		public final int kind;
		
		public SoundData(Uri uri, String title, int pos, int kind) {
			this.uri = uri;
			this.title = title;
			this.pos = pos;
			this.kind = kind;
		}
		
		@Override public String toString() {
			return title;
		}
	}
	
	private TelephonyManager _telephonyManager;
	private int _initialCallState;
	private PhoneStateListener _phoneStateListener = new PhoneStateListener() {
		public void onCallStateChanged(int state, String incomingNumber){
			if(state != TelephonyManager.CALL_STATE_IDLE && state != _initialCallState) {
				soundStop();
			}
		}
	};
	
	private MediaPlayer _mediaPlayer;
	
	private SoundData[] _ringtoneSoundDatas;
	private SoundData[] _myMusicSoundDatas;
	
	private SoundData _curSelectedData;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		_telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		_telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		_initialCallState = _telephonyManager.getCallState();
		
		_ringtoneSoundDatas = getRingtoneSoundDatas();
		_myMusicSoundDatas = getMyMusicSoundData();
		
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			Uri uri =  (Uri) extras.getParcelable(SelectedSoundURIExtra);
			if(uri != null) {
				
				boolean isFound = false;
				for(int i = 0;i < _ringtoneSoundDatas.length;++i) {
					SoundData data = _ringtoneSoundDatas[i];
					if(data.uri.compareTo(uri) == 0) {
						_curSelectedData = data;
						
						isFound = true;
						break;
					}
				}
				
				if(!isFound) {
					for(int i = 0;i < _myMusicSoundDatas.length;++i) {
						SoundData data = _myMusicSoundDatas[i];
						if(data.uri.compareTo(uri) == 0) {
							_curSelectedData = data;
							break;
						}
					}
				}
			}
		}
		
		initializeComponents();
	}
	
	private void initializeComponents() {
		final Context context = this;
		
		LayoutInflater inflater = LayoutInflater.from(context);
		setContentView(inflater.inflate(R.layout.pick_sound_activity_layout, null));
		
		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();
		
		((TextView)findViewById(R.id_pick_sound_activity.SelectedAlarm)).setText(
				String.format(ShowSelectedSoundTitleFormat, 
						_curSelectedData == null ? "" : _curSelectedData.title));
				
		TabSpec pickerRingtone = tabHost.newTabSpec("ringtone");
		pickerRingtone.setIndicator("サウンド");
		pickerRingtone.setContent(createRingtoneListView());
		tabHost.addTab(pickerRingtone);
		
		TabSpec pickerMusic = tabHost.newTabSpec("music");
		pickerMusic.setIndicator("MyMusic");
		pickerMusic.setContent(createMuiscListView());
		tabHost.addTab(pickerMusic);
		
		findViewById(R.id_pick_sound_activity.OK).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				if(_curSelectedData == null) {
					setResult(Activity.RESULT_CANCELED);
				} else {
					Intent intent = new Intent();
					intent.setData(_curSelectedData.uri);
					setResult(Activity.RESULT_OK, intent);
				}
				
				finish();
			}
		});
		
		findViewById(R.id_pick_sound_activity.Cancel).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		});
		
	}
	
	private void selectAlarmSound(SoundData data) {
		if(_curSelectedData != null && _curSelectedData.kind != data.kind &&
				_curSelectedData.uri.compareTo(data.uri) != 0) {
			ListView list = (ListView)findViewById(_curSelectedData.kind == KindRingtone ?
					RingtoneListViewId :  MyMusicListViewId);
			
			if(list != null && _curSelectedData.pos < list.getAdapter().getCount()) {
				list.setItemChecked(_curSelectedData.pos, false);
			}
		}
		
		_curSelectedData = data;
		
		((TextView)findViewById(R.id_pick_sound_activity.SelectedAlarm)).setText(
			String.format(ShowSelectedSoundTitleFormat, data.title));
	}
	
	private SoundData[] getRingtoneSoundDatas() {
		RingtoneManager manager = new RingtoneManager(this);
		
		Cursor cursor = null;
		SoundData[] datas;
		try {
			cursor = manager.getCursor();
			datas = new SoundData[cursor.getCount()];
		
			if(cursor.moveToFirst()) {
				int index = 0;
				do {
					 datas[index] = new SoundData(
						Uri.withAppendedPath(
								Uri.parse(cursor.getString(RingtoneManager.URI_COLUMN_INDEX)),
								String.valueOf(cursor.getInt(RingtoneManager.ID_COLUMN_INDEX))),
						cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX),
						index + 1, KindRingtone);
					 
					 ++index;
				}while(cursor.moveToNext());
			}
		} finally {
			if(cursor != null) {
				cursor.close();
			}
		}
		
		Uri defAlarmUri =  RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
		SoundData defAlarmData = new SoundData(defAlarmUri, getSoundTitle(defAlarmUri), 0, KindRingtone);
		if(datas == null) {
			datas = new SoundData[1];
			datas[0] = defAlarmData;
		} else {
			SoundData[] tmp = new SoundData[datas.length + 1];
			tmp[0] = defAlarmData;
			for(int i = 0;i < datas.length;++i) {
				tmp[i + 1] = datas[i];
			}
			datas = tmp;
		}
		
		return datas;
	}
	
	private SoundData[] getMyMusicSoundData() {
		SoundData[] datas;
		
		Cursor cursor = null;
		try {
			cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] {
						MediaStore.Audio.Media._ID,
						MediaStore.Audio.Media.TITLE
				}, null, null, null);
			
			datas = new SoundData[cursor.getCount()];
			
			if(cursor.moveToFirst()) {
				int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
				int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
				
				int index = 0;
				do {
					 datas[index] = new SoundData(
						 Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
								 String.valueOf(cursor.getInt(idIndex))),
						cursor.getString(titleIndex),
						index, KindMyMusic);
					 
					 ++index;
				}while(cursor.moveToNext());
			}
		}finally {
			if(cursor != null) {
				cursor.close();
			}
		}
		
		if(datas == null)
			datas = new SoundData[0];
		
		return datas;
	}
	
	private AdapterView.OnItemClickListener _onItemClickListener = new AdapterView.OnItemClickListener() {
		@Override public void onItemClick(AdapterView<?> adapterView, View v, int pos, long id) {
			SoundData data = (SoundData) adapterView.getItemAtPosition(pos);
			
			soundStart(data);
			
			selectAlarmSound(data);
		}
	};
	
	private TabContentFactory createRingtoneListView() {
		final Context context = this;
		return new TabContentFactory() {
			@Override public View createTabContent(String tag) {
				ListView list  = new ListView(context);
				list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				list.setAdapter(new ArrayAdapter<SoundData>(context, 
						android.R.layout.simple_list_item_single_choice, _ringtoneSoundDatas));
				list.setOnItemClickListener(_onItemClickListener);
				list.setId(RingtoneListViewId);
				
				if(_curSelectedData != null) {
					int index = findIndex(_ringtoneSoundDatas, _curSelectedData);
					if(index != -1)
						list.setItemChecked(index, true);
				}
				
				return list;
			}
		};
	}
	
	private TabContentFactory createMuiscListView() {
		final Context context = this;
		return new TabContentFactory() {
			@Override public View createTabContent(String tag) {
				ListView list = new ListView(context);
				list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				list.setAdapter(new ArrayAdapter<SoundData>(context, 
						android.R.layout.simple_list_item_single_choice, _myMusicSoundDatas));
				list.setOnItemClickListener(_onItemClickListener);
				list.setId(MyMusicListViewId);
						
				if(_curSelectedData != null) {
					int index = findIndex(_myMusicSoundDatas, _curSelectedData);
					if(index != -1)
						list.setItemChecked(index, true);
				}
				
				return list;
			}
		};
	}
	
	private int findIndex(SoundData[] datas, SoundData data) {
		int index = 0;
		for(SoundData d : datas) {
			if(d.uri.compareTo(data.uri) == 0)
				return index;
			++index;
		}
		
		return -1;
	}
	
	private void soundStart(SoundData data) {
		soundStop();
		
		if(data.uri == null)
			return;
		if(_telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
			return;
			
		_mediaPlayer = new MediaPlayer();
		_mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			
			@Override public boolean onError(MediaPlayer mp, int what, int extra) {
				mp.stop();
				mp.release();
				_mediaPlayer = null;
				
				return true;
			}
		});
		
		try {
			_mediaPlayer.setDataSource(this, data.uri);
			if(data.kind == KindRingtone) {
				_mediaPlayer.setVolume(0.06f, 0.06f);
			} else {
				_mediaPlayer.setVolume(0.2f, 0.2f);
			}
			_mediaPlayer.setLooping(true);
			_mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			_mediaPlayer.prepare();
			_mediaPlayer.start();
		} catch(Exception ex) {
			_mediaPlayer.stop();
			_mediaPlayer.release();
			_mediaPlayer = null;
		}
	}
	
	private void soundStop() {
		if(_mediaPlayer != null) {
			_mediaPlayer.stop();
			_mediaPlayer.release();
			_mediaPlayer = null;
		}
	}
	
	@Override protected void onPause() {
		soundStop();
		
		super.onPause();
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
