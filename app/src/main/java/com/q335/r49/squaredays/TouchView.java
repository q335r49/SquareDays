package com.q335.r49.squaredays;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TouchView<T extends TimeWin> extends View {
    static String CODE_CAL = "cal";
    static String CODE_EXP = "exp";
    private String gClass;
        void setClass(String s) {gClass = s;}

    private T CW;
    private ScaleListener SL;
    private ScaleGestureDetector mScaleDetector;
    private Context appContext;

    public TouchView(Context context) {
        super(context);
        SL = new ScaleListener();
        mScaleDetector = new ScaleGestureDetector(context, SL);
        appContext = context;
    }
    public TouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SL = new ScaleListener();
        mScaleDetector = new ScaleGestureDetector(context, SL);
        appContext = context;
    }
    void setDisplay(T cw) { this.CW = cw; }

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
        CW.draw(canvas);
    }
    private float lastTouchX, lastTouchY, firstTouchX, firstTouchY;
    private boolean has_run, has_dragged;
    private final Handler handler = new Handler();
    private Runnable mLongPressed = new Runnable() { public void run() {
        has_run = true;
        final cInterval selection = CW.getSelection();
        if (selection != null) {
            if (gClass.equals(CODE_CAL)) {
                LayoutInflater inflater = LayoutInflater.from(appContext);
                View promptView = inflater.inflate(R.layout.edit_interval, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(appContext);
                alertDialogBuilder.setView(promptView);

                final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                commentEntry.setText(selection.label);
                final EditText startEntry = (EditText) promptView.findViewById(R.id.startEdit);
                startEntry.setText(tsToDate(selection.start));
                final EditText endEntry = (EditText) promptView.findViewById(R.id.endEdit);
                endEntry.setText(tsToDate(selection.end));
                final View curColorV = promptView.findViewById(R.id.CurColor);
                try { curColorV.setBackgroundColor(selection.paint.getColor());
                } catch (Exception e) { curColorV.setBackgroundColor(Globals.COLOR_ERROR); }

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
                    v.setBackgroundColor(Globals.palette.get(i));
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
                alertDialogBuilder.setCancelable(true);
                alertDialogBuilder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { //TODO: This is just ignoring the comment updates, and outside of the proc pipeline
                        CW.updateEntry(selection, dateToTs(startEntry.getText().toString()), dateToTs(endEntry.getText().toString()));
                        invalidate();
                    }
                });
                alertDialogBuilder.setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CW.removeSelection();
                        invalidate();
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alertDialogBuilder.create().show();
            } else {





                LayoutInflater inflater = LayoutInflater.from(appContext);
                View promptView = inflater.inflate(R.layout.edit_expense, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(appContext);
                alertDialogBuilder.setView(promptView);

                final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                commentEntry.setText(selection.label);
                final EditText startEntry = (EditText) promptView.findViewById(R.id.startEdit);
                startEntry.setText(tsToDate(selection.start));
                final EditText endEntry = (EditText) promptView.findViewById(R.id.endEdit);
                endEntry.setText(Long.toString(selection.end));
                final View curColorV = promptView.findViewById(R.id.CurColor);
                try { curColorV.setBackgroundColor(selection.paint.getColor());
                } catch (Exception e) { curColorV.setBackgroundColor(Globals.COLOR_ERROR); }

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
                    v.setBackgroundColor(Globals.palette.get(i));
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
                alertDialogBuilder.setCancelable(true);
                alertDialogBuilder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((ExpenseWin) CW).updateEntry(dateToTs(startEntry.getText().toString()), Long.parseLong(endEntry.getText().toString()));
                        //TODO: error handling
                        invalidate();
                    }
                });
                alertDialogBuilder.setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((ExpenseWin) CW).removeSelection();
                        invalidate();
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alertDialogBuilder.create().show();
            }

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
                cInterval selection = CW.getSelectedShape(x,y);
                if (selection != null) {
                    long duration = 1000L* (selection.end - selection.start);
                    CW.setStatusText(selection.label + ":"
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
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            CW.scale(detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());
            return true;
        }
    }
}