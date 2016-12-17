package com.q335.r49.squaredays;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class ScaleView extends View {
    private static final String LOG_FILE = "log.txt";
    private CalendarWin CV;
    private ScaleListener SL;
    private ScaleGestureDetector mScaleDetector;
    private Context appContext;

    private String curTask;
        public String getCurTask() { return curTask; }
    private int curTaskColor;
        public int getCurTaskColor() { return curTaskColor; }

    public static String MESS_RELOAD_LOG = "##MESS RELOAD LOG";
    public static String MESS_REDRAW = "##MESS REDRAW";
    public void procMess(String s) {
        if (s == MESS_RELOAD_LOG) {
            loadCalendarView(appContext);
        } else if (s == MESS_REDRAW) {
            invalidate();
        } else {
            CV.loadEntry(s);
            invalidate();
        }
    }
    public ScaleView(Context context) {
        super(context);
        SL = new ScaleListener();
        mScaleDetector = new ScaleGestureDetector(context, SL);
        appContext = context;
        loadCalendarView(context);
    }
    public ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SL = new ScaleListener();
        mScaleDetector = new ScaleGestureDetector(context, SL);
        appContext = context;
        loadCalendarView(context);
    }
    public static List<String> read_file(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            ArrayList<String> sb = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.add(line);
            }
            return sb;
        } catch (FileNotFoundException e) {
            Log.e("SquareDays","Log file not found!");
            return new ArrayList<>();
        } catch (UnsupportedEncodingException e) {
            Log.e("SquareDays","Log file bad encoding!");
            return new ArrayList<>();
        } catch (IOException e) {
            Log.e("SquareDays","Log file IO exception!");
            return new ArrayList<>();
        }
    }
    private void loadCalendarView(Context context) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.DAY_OF_WEEK,1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        CV = new CalendarWin(cal.getTimeInMillis()/1000,10f,1.5f);
        CV.loadAllEntries(read_file(context.getApplicationContext(), LOG_FILE));
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        CV.setLineWidth(Math.round(6 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
        curTask = CV.getCurComment();
        curTaskColor = CV.getCurColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        CV.draw(canvas); //XTODO: Investigate why draw is happening multiple times
    }
    private float mLastTouchX=-1;
    private float mLastTouchY=-1;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScaleDetector.onTouchEvent(ev);
        float x = ev.getX();
        float y = ev.getY();
        if (ev.getAction() == MotionEvent.ACTION_MOVE && (Math.abs(x-mLastTouchX) + Math.abs(y-mLastTouchY) < 150)) {
            CV.shiftWindow(x-mLastTouchX,y-mLastTouchY);
            invalidate();
        }
        mLastTouchX = x;
        mLastTouchY = y;
        return true;
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            CV.reScale(detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());
            return true;
        }
    }
}

