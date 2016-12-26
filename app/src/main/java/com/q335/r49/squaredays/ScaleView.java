package com.q335.r49.squaredays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import com.google.android.flexbox.FlexboxLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ScaleView extends View {
    private CalendarWin CW;
    private ScaleListener SL;
    private ScaleGestureDetector mScaleDetector;
    private Context appContext;

    private PaletteRing palette;

    public logEntry getCurTask() { return CW == null? null : CW.getCurTask(); }

    public logEntry procTask(logEntry le) {
            if (le.isMessage()) {
                switch (le.getMessage()) {
                    case logEntry.MESS_CLEAR_LOG:
                        CW.clearShapes();
                        break;
                }
                invalidate();
                return null;
            } else {
                invalidate();
                return CW.procCmd(le);
            }
    }
    List<String> getWritableShapes() {return CW.getWritableShapes(); }

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
    void loadCalendarView(PaletteRing pal) {
        palette = pal;
        Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.DAY_OF_WEEK,1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        CW = new CalendarWin(cal.getTimeInMillis()/1000L-52000L,8f,1.5f,-0.8f,-0.1f);
        CW.setDPIScaling(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
    }

    public static long dateToTs(String s) {
        int minPos = s.indexOf(':');
        int monthPos = s.indexOf(' ', minPos+1);
        int dayPos = s.indexOf('/', monthPos+1);
        int yearPos = s.indexOf('/', dayPos+1);
        if (minPos == -1 || monthPos == -1 || dayPos == -1 || yearPos == -1)
            return -1;
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR,Integer.parseInt(s.substring(yearPos+1,s.length())));
        cal.set(Calendar.MONTH,Integer.parseInt(s.substring(monthPos+1,dayPos))-1);
        cal.set(Calendar.DAY_OF_MONTH,Integer.parseInt(s.substring(dayPos+1,yearPos)));
        cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(s.substring(0,minPos)));
        cal.set(Calendar.MINUTE,Integer.parseInt(s.substring(minPos+1,monthPos)));
        return cal.getTimeInMillis()/1000L;
    }
    public static String tsToDate(long ts) { return new SimpleDateFormat("k:mm M/d/yyyy", Locale.US).format(new Date(ts*1000L)); }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        CW.draw(canvas); //XTODO: Investigate why draw is happening multiple times
    }
    private float lastTouchX, lastTouchY, firstTouchX, firstTouchY;
    private boolean has_run, has_dragged;
    private final Handler handler = new Handler();
    private Runnable mLongPressed = new Runnable() { public void run() {
        has_run = true;
        final logEntry selection = CW.getSelectedShape(lastTouchX, lastTouchY);
        if (selection != null) {
            LayoutInflater inflater = LayoutInflater.from(appContext);
            View promptView = inflater.inflate(R.layout.edit_interval, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(appContext);
            alertDialogBuilder.setView(promptView);

            final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
            commentEntry.setText(selection.comment);
            final EditText startEntry = (EditText) promptView.findViewById(R.id.startEdit);
            startEntry.setText(tsToDate(selection.start));
            final EditText endEntry = (EditText) promptView.findViewById(R.id.endEdit);
            endEntry.setText(tsToDate(selection.end));
            final View curColorV = promptView.findViewById(R.id.CurColor);
            try { curColorV.setBackgroundColor(selection.paint.getColor());
            } catch (Exception e) { curColorV.setBackgroundColor(MainActivity.COLOR_ERROR); }

            final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
            final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
            final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
            final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);
            seekRed.setProgress(Color.red(curColor));
            seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    curColorV.setBackgroundColor(Color.rgb(progress,seekGreen.getProgress(),seekBlue.getProgress()));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            seekGreen.setProgress(Color.green(curColor));
            seekGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),progress,seekBlue.getProgress()));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            seekBlue.setProgress(Color.blue(curColor));
            seekBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),seekGreen.getProgress(),progress));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });

            final FlexboxLayout paletteView = (FlexboxLayout) promptView.findViewById(R.id.paletteBox);
            final int childCount = paletteView.getChildCount();
            for (int i = 0; i < childCount ; i++) {
                View v = paletteView.getChildAt(i);
                v.setBackgroundColor(palette.get(i));
                final int bg = ((ColorDrawable) v.getBackground()).getColor();
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        seekRed.setProgress(Color.red(bg));
                        seekGreen.setProgress(Color.green(bg));
                        seekBlue.setProgress(Color.blue(bg));
                        curColorV.setBackgroundColor(bg);
                    }
                });
            }
            alertDialogBuilder
                    .setCancelable(true)
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            long newstart = dateToTs(startEntry.getText().toString());
                            long newend = dateToTs(endEntry.getText().toString());
                            selection.setInterval(newstart,newend);
                            //TODO: bring shape to foreground?
                            invalidate();
                        }
                    })
                    .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            selection.markForRemoval();
                            invalidate();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            alertDialogBuilder.create().show();
        }

    }};
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScaleDetector.onTouchEvent(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        switch (ev.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                has_run = has_dragged = false;
                handler.postDelayed(mLongPressed,1200);
                firstTouchX = lastTouchX = x;
                firstTouchY = lastTouchY = y;
                logEntry selection = CW.getSelectedShape(x,y);
                if (selection != null && selection.start != -1) {
                    long duration = 1000L* (selection.end - selection.start);
                    CW.setStatusText(selection.comment + ":"
                            + new SimpleDateFormat(" h:mm-", Locale.US).format(new Date(selection.start*1000L))
                            + new SimpleDateFormat("h:mm", Locale.US).format(new Date(selection.end*1000L))
                            + String.format(Locale.US, " (%d:%02d)", TimeUnit.MILLISECONDS.toHours(duration),
                            TimeUnit.MILLISECONDS.toMinutes(duration)%60));
                } else
                    CW.setStatusText("");
                CW.setSelected(selection);
                invalidate();
                return true;
            case (MotionEvent.ACTION_MOVE):
                if (!has_run) {
                    if (!has_dragged && (Math.abs(lastTouchX-firstTouchX)+Math.abs(lastTouchY-firstTouchY) > 15)) {
                        has_dragged = true;
                        handler.removeCallbacks(mLongPressed);
                    }
                    if (Math.abs(x - lastTouchX) + Math.abs(y - lastTouchY) < 150) {
                        CW.shift(x - lastTouchX, y - lastTouchY);
                        invalidate();
                    }
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                return false;
            case (MotionEvent.ACTION_UP):
            case (MotionEvent.ACTION_CANCEL):
                if (!has_run)
                    handler.removeCallbacks(mLongPressed);
                return false;
            default:
                return true;
        }
    }

    public boolean isFullyLoaded() {
        return CW != null;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            CW.scale(detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());
            return true;
        }
    }
}