package aharisu.Misc.SimpleAlarm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

public class TimeInputView extends LinearLayout{
	
	private static class DigitDisplayView extends View {
		public static interface OnClickDigitListener {
			public void onClickDigit(View v, int digit);
		}
		
		private static final int DigitMargin = 10;
		private static final int HorizontalMargin = 38;
		
		private final float _digitHeight;
		private final float _digitWidth;
		private int _selectedDigits;
		private final int[] _digits = new int[4];
		private final boolean[] _enableDigit1 = new boolean[] {true, true, false, false, false, false, false,false,false, true};
		private final boolean[] _enableDigit2= new boolean[] {true, true, true, false, false, false, false,false,false, true};
		private final boolean[] _enableDigit3 = new boolean[] {true, true, true, true, true, false, false,false,false, true};
		private final boolean[] _enableDigitAll = new boolean[] {true, true, true, true, true, true, true,true,true, true};
		
		private final OnClickDigitListener _listener;
		
		private final Paint _paint;
		
		public DigitDisplayView(Context context, OnClickDigitListener listener) {
			super(context);
			
			_listener = listener;
			
			_paint = new Paint();
			_paint.setAntiAlias(true);
			_paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/liqc____.ttf"));
			_paint.setTextSize(150);
			_paint.setColor(0xffffffff);
			_paint.setStrokeWidth(5);
			
			_digitHeight = - _paint.ascent() - 20;
			_digitWidth = _paint.measureText("0");
		}
		
		public void setNumber(int number) {
			_digits[_selectedDigits] = number;
			
			if(_selectedDigits == 0 && number == 2 && _digits[1] >= 5) {
				_digits[1] = 3;
			}
		}
		
		public void setSelectedDigit(int digit) {
			if(digit < 4) {
				_selectedDigits = digit;
			}
		}
		
		public int getSelectedDigit() {
			return _selectedDigits;
		}
		
		public int getHour() {
			return _digits[0] * 10 + _digits[1];
		}
		
		public int getMinutes() {
			return _digits[2] * 10 + _digits[3];
		}
		
		public boolean[] getEnableDigits(int digit) {
			switch(digit) {
			case 0:
				return _enableDigit1;
			case 1:
				return (_digits[0] == 2) ? _enableDigit2 : _enableDigitAll;
			case 2:
				return _enableDigit3;
			default:
				return _enableDigitAll;
			}
		}
		
		
		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			
			float left = HorizontalMargin;
			float top = _digitHeight;
			
			for(int i = 0;i < 2;++i) {
				if(i == _selectedDigits) {
					int color = _paint.getColor();
					
					_paint.setColor(0xff53546a);
					canvas.drawRoundRect(new RectF(left - 5, 20, left + _digitWidth + 5, getHeight() + 15), 16, 16, _paint);
					_paint.setColor(0xff777777);
					_paint.setStyle(Style.STROKE);
					canvas.drawRoundRect(new RectF(left - 5, 20, left + _digitWidth + 5, getHeight() + 15), 16, 16, _paint);
					_paint.setStyle(Style.FILL);
					
					_paint.setColor(color);
				}
				
				float x = left;
				if(_digits[i] == 1) {
					x += 45;
				}
				
				canvas.drawText(Integer.toString(_digits[i]), x, top, _paint);
				
				left += _digitWidth + DigitMargin;
			}
			
			canvas.drawText(":", left + 5, top, _paint);
			left += + _digitWidth - 30;
			
			for(int i = 2;i < 4;++i) {
				if(i == _selectedDigits) {
					int color = _paint.getColor();
					
					_paint.setColor(0xff53546a);
					canvas.drawRoundRect(new RectF(left - 5, 20, left + _digitWidth + 5, getHeight() + 15), 16, 16, _paint);
					_paint.setColor(0xff777777);
					_paint.setStyle(Style.STROKE);
					canvas.drawRoundRect(new RectF(left - 5, 20, left + _digitWidth + 5, getHeight() + 15), 16, 16, _paint);
					_paint.setStyle(Style.FILL);
					
					_paint.setColor(color);
				}
				float x = left;
				if(_digits[i] == 1) {
					x += 45;
				}
				canvas.drawText(Integer.toString(_digits[i]), x, top, _paint);
				
				left += _digitWidth + DigitMargin;
			}
		}
		
		@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(480, (int)(-_paint.ascent()));
		}
		
		@Override public boolean onTouchEvent(MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				float x = event.getX();
				
				float halfMargin = DigitMargin / 2;
				
				x -= (HorizontalMargin + _digitWidth + halfMargin);
				if(x < 0) {
					_listener.onClickDigit(this, 0);
					return true;
				}
				
				x -= (_digitWidth + DigitMargin + _digitWidth / 2);
				if(x < 0) {
					_listener.onClickDigit(this, 1);
					return true;
				}
				
				
				x -= (_digitWidth + DigitMargin + _digitWidth / 2);
				if(x < 0) {
					_listener.onClickDigit(this, 2);
					return true;
				}
				
				_listener.onClickDigit(this, 3);
			}
			
			return true;
		}
	}
	
	private final Button[] _btnNumber = new Button[10];
	private DigitDisplayView _digitDisplay;
	
	private final View.OnClickListener _onNumberButtonClickListener = new View.OnClickListener() {
		
		@Override public void onClick(View v) {
			Integer number = (Integer)v.getTag();
			if(number != null) {
				_digitDisplay.setNumber(number);
				
				changeSelectedDigit(_digitDisplay.getSelectedDigit() + 1);
			}
		}
	};
	
	public TimeInputView(Context context) {
		super(context);
		
		initializeComponents();
		
		changeSelectedDigit(0);
	}
	
	public TimeInputView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		initializeComponents();
		
		changeSelectedDigit(0);
	}
	
	public int getHour() {
		return _digitDisplay.getHour();
	}
	
	public int getMinutes() {
		return _digitDisplay.getMinutes();
	}
	
	public void setHour(int hour) {
		int selectedDigit = _digitDisplay.getSelectedDigit();
		
		_digitDisplay.setSelectedDigit(0);
		_digitDisplay.setNumber(hour / 10);
		_digitDisplay.setSelectedDigit(1);
		_digitDisplay.setNumber(hour % 10);
		
		_digitDisplay.setSelectedDigit(selectedDigit);
		
		_digitDisplay.invalidate();
	}
	
	public void setMinutes(int minutes) {
		int selectedDigit = _digitDisplay.getSelectedDigit();
		
		_digitDisplay.setSelectedDigit(2);
		_digitDisplay.setNumber(minutes / 10);
		_digitDisplay.setSelectedDigit(3);
		_digitDisplay.setNumber(minutes % 10);
		
		_digitDisplay.setSelectedDigit(selectedDigit);
		
		_digitDisplay.invalidate();
	}
	
	private void initializeComponents() {
		final Context context = getContext();
		
		this.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout.LayoutParams params;
		
		_digitDisplay = new DigitDisplayView(context, new DigitDisplayView.OnClickDigitListener() {
			@Override public void onClickDigit(View v, int digit) {
				changeSelectedDigit(digit);
			}
		});
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(_digitDisplay, params);
		
		TableLayout table = new TableLayout(context);
		table.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.number_table_background));
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		table.setLayoutParams(params);
		table.setPadding(10, 8, 4, 10);
		this.addView(table);
		
		TableRow row = new TableRow(context);
		table.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.FILL_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT));
		for(int i = 0;i < 5;++i) {
			int number = i + 1;
			Button btn = new Button(context);
			btn.setText(Integer.toString(number));
			btn.setTextSize(30);
			btn.setTag(number);
			btn.setOnClickListener(_onNumberButtonClickListener);
			
			TableRow.LayoutParams lp = new TableRow.LayoutParams(80, 72);
			lp.setMargins(6, 12, 6, 12);
			btn.setLayoutParams(lp);
			
			row.addView(btn);
			_btnNumber[i] = btn;
		}
		
		row = new TableRow(context);
		table.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.FILL_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT));
		for(int i = 0;i < 5;++i) {
			int number = (i + 6) % 10;
			Button btn = new Button(context);
			btn.setText(Integer.toString(number));
			btn.setTextSize(30);
			btn.setTag(number);
			btn.setOnClickListener(_onNumberButtonClickListener);
			
			TableRow.LayoutParams lp = new TableRow.LayoutParams(80, 72);
			lp.setMargins(6, 12, 6, 12);
			btn.setLayoutParams(lp);
			
			row.addView(btn);
			_btnNumber[i + 5] = btn;
		}

	}
	
	private void changeSelectedDigit(int digit) {
		_digitDisplay.setSelectedDigit(digit);
		_digitDisplay.invalidate();
		
		int index = 0;
		for (boolean enable : _digitDisplay.getEnableDigits(digit)) {
			_btnNumber[index].setEnabled(enable);
			++index;
		}
	}
	
}
