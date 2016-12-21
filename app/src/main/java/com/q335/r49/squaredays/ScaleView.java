package com.q335.r49.squaredays;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
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

    private PaletteRing palette;
        void setPalette(PaletteRing palette) { this.palette = palette; }

    private String curTask;
        public String getCurTask() { return curTask; }
    private int curTaskColor;
        public int getCurTaskColor() { return curTaskColor; }

    public static String MESS_RELOAD_LOG = "##MESS RELOAD LOG";
    public static String MESS_REDRAW = "##MESS REDRAW";
    public void procMess(String s) {
        if (s == MESS_RELOAD_LOG) {
            loadCalendarView(appContext, palette);
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
    }
    public ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SL = new ScaleListener();
        mScaleDetector = new ScaleGestureDetector(context, SL);
        appContext = context;
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
            Log.d("SquareDays","Log file not found!");
            return new ArrayList<>();
        } catch (UnsupportedEncodingException e) {
            Log.d("SquareDays","Log file bad encoding!");
            return new ArrayList<>();
        } catch (IOException e) {
            Log.d("SquareDays","Log file IO exception!");
            return new ArrayList<>();
        }
    }
    void loadCalendarView(Context context, PaletteRing pal) { //TODO: suspect proc cur entry not doing well on GC
        palette = pal;
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.DAY_OF_WEEK,1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        CV = new CalendarWin(cal.getTimeInMillis()/1000,10f,1.5f,palette);
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
    private float mLastTouchX, mLastTouchY;
    private boolean has_run;
    private final Handler handler = new Handler();
    private Runnable mLongPressed = new Runnable() { public void run() {
        has_run = true;
        CV.onLongPress(mLastTouchX,mLastTouchY,appContext);
    }};;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScaleDetector.onTouchEvent(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        switch (ev.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                has_run = false;
                handler.postDelayed(mLongPressed,1200);
                mLastTouchX = x;
                mLastTouchY = y;
                return true;
            case (MotionEvent.ACTION_MOVE):
                if (has_run) {
                    return false;
                } else {
                    handler.removeCallbacks(mLongPressed);
                    if (Math.abs(x - mLastTouchX) + Math.abs(y - mLastTouchY) < 150) {
                        CV.onMove(x - mLastTouchX, y - mLastTouchY);
                        invalidate();
                    }
                    mLastTouchX = x;
                    mLastTouchY = y;
                    return true;
                }
            case (MotionEvent.ACTION_UP):
                if (has_run) {
                    return false;
                } else {
                    handler.removeCallbacks(mLongPressed);
                    CV.onPress(x, y);
                    invalidate();
                    return false;
                }
            default:
                return true;
        }
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            CV.onScale(detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());
            return true;
        }
    }
}

