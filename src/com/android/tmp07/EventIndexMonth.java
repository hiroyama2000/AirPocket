package com.android.tmp07;

import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.FrameLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import android.graphics.Color;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;
import java.util.LinkedList;
import java.util.Calendar;
import java.util.ArrayList;
import java.lang.Integer;
import java.lang.Exception;

import java.io.File;

public class EventIndexMonth extends Activity
{
	private static final String[] weekLabelJp = {"日", "月", "火", "水", "木", "金", "土"};
	private static final String TAG = "Tmp12";

	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

	/* current activity status */
	private static int statusSuspend = 0;
	private static int statusActive = 1;
	private static final float moveThreashold = 30f;

	private float motionX;
	private int motionStatus = 0;

	protected final Calendar currentDate = Calendar.getInstance();

	/* View size parameter */
	private static int columnHeight = 0;
	private static final int labelHeight = 25;
	private static final float labelSize = 18f;
	private static final float dateTextSize = 20f;
	private static final float columnSize = 20f;
	private static final float outsideColumnSize = 15f;

	/* following member is used at board-switch */
	private ObjectContainer currentContainer;
	
	private int activityStatus;

	OnTouchListener moveMonthMotion = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent e) {
			Integer backgroundColor = (Integer) v.getTag(R.string.keyBackgroundColor);

			if(e.getAction() == MotionEvent.ACTION_DOWN) {
				motionX = e.getX();

				if(backgroundColor != null) {
					v.setBackgroundColor(getResources().getColor(R.color.selected_event));
				}
			} else if(e.getAction() == MotionEvent.ACTION_MOVE) {
				if((e.getX() - motionX) > moveThreashold) {
					motionStatus = -1;
				} else if((e.getX() - motionX) < (moveThreashold * -1)) {
					motionStatus = 1;
				}
			} else if(e.getAction() == MotionEvent.ACTION_UP) {
				if(motionStatus == 0) {
					try {
						int numOfDay = (Integer) v.getTag(R.string.keyWeekCount);

						Calendar sendCal = (Calendar) currentDate.clone();
						Intent intent = new Intent(EventIndexMonth.this, EventIndexDay.class);
			
						sendCal.set(Calendar.DAY_OF_MONTH, numOfDay);
			
						intent.putExtra(EventIndexDay.KEY_DATE, sendCal);
			
						startActivity(intent);
					} catch(Exception exception) {
						Log.d(TAG, "[moveMonth] ERROR:"+exception.getMessage());
					}
				} else if((motionStatus == 1) || (motionStatus == -1)){
					doMoveMonth(motionStatus);
				
					motionX = e.getX();
					motionStatus = 0;
			
					if(backgroundColor != null) {
						v.setBackgroundColor(backgroundColor);
					}
				}
			}
	
			return true;
		}
	};

	public void setFullscreen(int screenWidth, int screenHeight) {
		LinkedList<TextView> contents = currentContainer.contents;
		this.columnHeight = (screenHeight - labelHeight) / 6;

		for(int i=0; i<contents.size(); i++) {
			TextView viewDay = contents.get(i);

			viewDay.setHeight(columnHeight);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(activityStatus == statusSuspend){
			FrameLayout mainFrame = (FrameLayout) findViewById(R.id.evMonth_mainFrame);

			mainFrame.removeView(currentContainer.canvas);
	
			currentContainer.contents.clear();
		
			currentContainer = initFrameLayout();
			constructScreen(currentContainer);

			activityStatus = statusActive;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		activityStatus = statusSuspend;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		ServerInterface serverInterface = new ServerInterface();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.eventindex_month);

		//serverInterface.doSync(this);

		setHoliday();
	
		/* initialize current FrameLayout */
		currentContainer = initFrameLayout();
		constructScreen(currentContainer);

		activityStatus = statusActive;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;

		switch(item.getItemId()) {
			case R.id.menuLogout:
				Log.d(TAG, "[onOptionsItemSelected] menuLogout is selected");

				File file = new File(ServerInterface.InfoFilepath);
				if(file.exists()) {
					Log.d(TAG, "[clickEvent] do delete file");
	
					file.delete();
				}

				EventIndexMonth.this.finish();

				ret = true;
				break;
		}

		return ret;
	}

	private void setHoliday() {
		/* static case */
		new Holiday(1, 1, "元旦");
		new Holiday(2, 11, "建国記念日");
		new Holiday(4, 29, "昭和の日");
		new Holiday(5, 3, "憲法記念日");
		new Holiday(5, 4, "みどりの日");
		new Holiday(5, 5, "こどもの日");
		new Holiday(11, 3, "文化の日");
		new Holiday(11, 23, "勤労感謝の日");
		new Holiday(12, 23, "天皇誕生日");

		/* dynamic case */
		new Holiday(1, 1, 1, "成人の日");
		new Holiday(7, 2, 1, "海の日");
		new Holiday(9, 2, 1, "敬老の日");
		new Holiday(10, 1, 1, "体育の日");

		/* special case */
		new Holiday(Holiday.DayOfSpring , "春分の日");
		new Holiday(Holiday.DayOfFall , "秋分の日");
	}

	private void constructScreen(ObjectContainer container) {

		/*to set date label "${year} / ${month}" */
		setDateLabel(container.canvas);

		/* 
		 * Main processing to show each day column.
		 * NOTE: Now, the View is replaced into alternative which is TextView. 
		 * 
		 */
		generateMonthView(container.canvas, container.contents);
	}

	private ObjectContainer initFrameLayout() {
		FrameLayout mainFrame = (FrameLayout) findViewById(R.id.evMonth_mainFrame);
		ObjectContainer container = new ObjectContainer(this);

		container.canvas.setOrientation(LinearLayout.VERTICAL);
		container.canvas.setVisibility(View.VISIBLE);

		mainFrame.addView(container.canvas, new ViewGroup.LayoutParams(FP, FP));

		return container;
	}

	private void setDateLabel(LinearLayout canvas) {
		TextView currentDateView = new TextView(this);

		currentDateView.setGravity(Gravity.CENTER);
		currentDateView.setTextSize(dateTextSize);
		currentDateView.setBackgroundResource(R.drawable.headline_date);
		currentDateView.setTextColor(getResources().getColor(R.color.normal_text));
		currentDateView.setText(String.format("%d/%02d",
					currentDate.get(Calendar.YEAR),
					currentDate.get(Calendar.MONTH) + 1));

		canvas.addView(currentDateView, new ViewGroup.LayoutParams(FP, WC));
	}

	private void generateMonthView(LinearLayout canvas, LinkedList<TextView> contents) {
		try {
			EventIndexMonthView mainBoard = new EventIndexMonthView(this);
			Calendar tmpCal = (Calendar) currentDate.clone();
			Calendar now = Calendar.getInstance();
			Display d = getWindowManager().getDefaultDisplay();
			boolean lastMonthFlag = true;
			boolean endOfMonth = false;
			boolean isMarkupHoliday = false;
			int columnNum = 6;
			int weekCount;
			int dayCount = 1;
			int daysOfMonth = getDaysOfMonth(tmpCal.get(Calendar.MONTH));
			int daysOfLastMonth = getDaysOfMonth(tmpCal.get(Calendar.MONTH) - 1);
			int columnWidth = (d.getWidth() / 7);
			int columnWidthLeftover = (d.getWidth() % 7);

			//mainBoard.setStretchAllColumns(true);
			mainBoard.setGravity(Gravity.CENTER);
			tmpCal.set(Calendar.DAY_OF_MONTH, 1);
			weekCount = tmpCal.get(Calendar.DAY_OF_WEEK);

			/* This routine draws each week-label */
			generateWeekLabel(mainBoard);
	
			for(int i=0; i<columnNum; i++){
				TableRow weekRow = new TableRow(this);
		
				/*implementation for drawing last month*/
				if(lastMonthFlag){
					lastMonthFlag = false;
					for(int j=(weekCount-1); j>0; j--){
						TextView day = new TextView(this);
						int color = getResources().getColor(R.color.outside_background);

						day.setText(String.format("%s", daysOfLastMonth - j + 1));
						day.setBackgroundColor(color);
						day.setGravity(Gravity.CENTER_HORIZONTAL);
						day.setTextSize(outsideColumnSize);
						day.setOnTouchListener(moveMonthMotion);
						day.setPadding(0, (int)(columnSize-outsideColumnSize), 0, 0);
						day.setWidth(columnWidth);
						if(columnHeight > 0) {
							day.setHeight(columnHeight);
						}

						contents.add(day);
						weekRow.addView(day);
					}
				}
	
				for(; weekCount<8; weekCount++){
					if(endOfMonth == false){
						FrameLayout frameLayout = new FrameLayout(this);
						TextView day = new TextView(this);
						boolean isHoliday = Holiday.isHoliday(tmpCal.getTime());
						ArrayList documents = ScheduleContent.grepScheduleFromDate(tmpCal.getTime());
						int color;

						if(isHoliday || isMarkupHoliday || weekCount == 1) {
							color = getResources().getColor(R.color.holiday_background);

							if(! (isMarkupHoliday && (isHoliday || weekCount == 1))) {
								isMarkupHoliday = false;
							}
						} else if(weekCount == 7) {
							color = getResources().getColor(R.color.satuaday_background);
						} else {
							color = getResources().getColor(R.color.weekday_background);
						}

						day.setTag(R.string.keyWeekCount, new Integer(dayCount));
						day.setTag(R.string.keyBackgroundColor, new Integer(color));
						day.setText(String.format("%s", dayCount++));
						day.setBackgroundColor(color);
						day.setTextColor(getResources().getColor(R.color.normal_text));
						day.setTextSize(columnSize);
						day.setGravity(Gravity.CENTER_HORIZONTAL);
						day.setOnTouchListener(moveMonthMotion);
						day.setWidth(columnWidth);
						if(columnHeight > 0) {
							day.setHeight(columnHeight);
						}

						if((weekCount == 1) && isHoliday) {
							isMarkupHoliday = true;
						}

						if(weekCount == 7) {
							day.setWidth(columnWidth + columnWidthLeftover);
						}

						contents.add(day);
						frameLayout.addView(day);
				
						if((now.get(Calendar.MONTH) == tmpCal.get(Calendar.MONTH)) && 
							(now.get(Calendar.YEAR) == tmpCal.get(Calendar.YEAR)) && 
							(tmpCal.get(Calendar.DAY_OF_MONTH) == (now.get(Calendar.DAY_OF_MONTH))))
						{
							TextView event = new TextView(this);

							event.setBackgroundResource(R.drawable.event_today);
							event.setWidth(columnWidth);
							if(columnHeight > 0) {
								event.setHeight(columnHeight);
							}

							if(weekCount == 1) {
								event.setWidth(columnWidth + columnWidthLeftover);
							}

							contents.add(event);
							frameLayout.addView(event);
						}else if(documents.size() > 0) {
							TextView event = new TextView(this);

							event.setBackgroundResource(R.drawable.event_existence);
							event.setWidth(columnWidth);
							if(columnHeight > 0) {
								event.setHeight(columnHeight);
							}

							if(weekCount == 1) {
								event.setWidth(columnWidth + columnWidthLeftover);
							}

							contents.add(event);
							frameLayout.addView(event);

							/* set for labels */
							//int labelCount = ((columnHeight - columnSize) / columnWidth) * 2;
							LinearLayout labelContainer = new LinearLayout(this);
							labelContainer.setOrientation(LinearLayout.HORIZONTAL);
							labelContainer.setPadding(0, (int)(columnSize + outsideColumnSize), 0, 0);

							for(int j=0, k=0; (k<2 && j<documents.size()); j++) {
								ScheduleContent doc = (ScheduleContent) documents.get(j);
								String labelName = doc.getResourceLabel();

								if(labelName != null) {
									int resourceId = getResources().getIdentifier(labelName, "drawable", "com.android.tmp07");
									int labelSize = columnWidth / 2;
									ImageView label = new ImageView(this);
	
									label.setImageResource(resourceId);
									label.setAdjustViewBounds(true);
									label.setMaxHeight(labelSize);
									label.setMaxWidth(labelSize);
	
									labelContainer.addView(label, new ViewGroup.LayoutParams(WC, WC));
									k++;
								}
							}

							frameLayout.addView(labelContainer, new ViewGroup.LayoutParams(FP, FP));
						}
		
						weekRow.addView(frameLayout);
		
						if(dayCount > daysOfMonth){
							endOfMonth = true;
							dayCount = 1;
						}
					}else{
						TextView day = new TextView(this);
						int color = getResources().getColor(R.color.outside_background);

						day.setText(String.format("%s", dayCount++));
						day.setBackgroundColor(color);
						day.setTextSize(outsideColumnSize);
						day.setGravity(Gravity.CENTER_HORIZONTAL);
						day.setOnTouchListener(moveMonthMotion);
						day.setPadding(0, (int)(columnSize-outsideColumnSize), 0, 0);
						day.setWidth(columnWidth);
						if(columnHeight > 0) {
							day.setHeight(columnHeight);
						}

						if(weekCount == 1) {
							day.setWidth(columnWidth + columnWidthLeftover);
						}
		
						contents.add(day);
						weekRow.addView(day);
					}

					tmpCal.roll(Calendar.DAY_OF_MONTH, true);
				}
				weekCount = 1;
	
				mainBoard.addView(weekRow);
			}
		
			canvas.addView(mainBoard, new ViewGroup.LayoutParams(FP, FP));
		} catch (Exception e) {
			Log.e(TAG, "[generateMonthView] ERROR:"+e.getMessage());
		}
	}

	private int getDaysOfMonth(int currentMonth) {
		Calendar cal = Calendar.getInstance();
		int nextMonth = currentMonth + 1;
		int retDays = -1;

		if(currentMonth > 11){
			currentMonth = 0;
		}else if(currentMonth < 0){
			currentMonth = 11;
		}

		if(currentMonth == 11){
			/*set days for December*/
			retDays = 31;
		}else{
			cal.set(Calendar.MONTH, nextMonth);
			retDays = cal.get(Calendar.DAY_OF_YEAR);
	
			cal.set(Calendar.MONTH, currentMonth);
			retDays -= cal.get(Calendar.DAY_OF_YEAR);
		}

		return retDays;
	}

	private void generateWeekLabel(TableLayout mainBoard) {
		Display d = getWindowManager().getDefaultDisplay();
		try {
			//mainBoard.setStretchAllColumns(true);
	
			TableRow labelRow = new TableRow(this);
	
			for(int i=0; i<weekLabelJp.length; i++){
				TextView tv = new TextView(this);
				tv.setText(weekLabelJp[i]);
				tv.setTextSize(labelSize);
				tv.setTextColor(getResources().getColor(R.color.normal_text));
				tv.setHeight(labelHeight);
				tv.setBackgroundColor(getResources().getColor(R.color.weeklabel_background));
				tv.setGravity(Gravity.CENTER_HORIZONTAL);
				tv.setWidth(d.getWidth() / 7);

				if(i == 6) { /* for Satuaday */
					tv.setBackgroundColor(getResources().getColor(R.color.satuaday_label));
				} else if(i == 0) { /* for Sunday */
					tv.setBackgroundColor(getResources().getColor(R.color.holiday_label));
				}
		
				labelRow.addView(tv);
			}
			mainBoard.addView(labelRow);
		} catch (Exception e) {
			Log.d(TAG, "[generateWeekLabel] ERROR:"+e.getMessage());
		}
	}

	private void doMoveMonth(int direction) {
		FrameLayout mainFrame = (FrameLayout) findViewById(R.id.evMonth_mainFrame);
		ObjectContainer replaceContainer = currentContainer;
		int currentMonth = currentDate.get(Calendar.MONTH);
		int currentYear = currentDate.get(Calendar.YEAR);
		Animation currentSlide;
		Animation replaceSlide;

		if(direction > 0) {
			if(++currentMonth > 11){
				currentYear++;
				currentMonth = 0;
			}

			currentSlide = AnimationUtils.loadAnimation(this, R.anim.slide_center2left);
			replaceSlide = AnimationUtils.loadAnimation(this, R.anim.slide_right2center);
			Log.d(TAG, "[onClick] clicked prev button");
		} else {
			if(--currentMonth < 0){
				currentYear--;
				currentMonth = 11;
			}

			currentSlide = AnimationUtils.loadAnimation(this, R.anim.slide_center2right);
			replaceSlide = AnimationUtils.loadAnimation(this, R.anim.slide_left2center);
			Log.d(TAG, "[onClick] clicked next button");
		}

		currentDate.set(Calendar.MONTH, currentMonth);
		currentDate.set(Calendar.YEAR, currentYear);

		currentContainer = initFrameLayout();
		constructScreen(currentContainer);

		replaceContainer.canvas.setAnimation(currentSlide);
		currentContainer.canvas.setAnimation(replaceSlide);
		
		mainFrame.removeView(replaceContainer.canvas);
		replaceContainer = null;
	}

	class ObjectContainer implements Cloneable {
		public LinearLayout canvas;
		public LinkedList<TextView> contents;

		ObjectContainer(Context context) {
			this.canvas = new LinearLayout(context);
			this.contents = new LinkedList<TextView>();
		}

		public ObjectContainer clone() {
			ObjectContainer r = null;

			try {
				r = (ObjectContainer)super.clone();
			} catch (CloneNotSupportedException ce) {
				ce.printStackTrace();
			}

			return r;
		}
	};
}
