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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScaleView extends View {
    private CalendarWin CW;
    private ScaleListener SL;
    private ScaleGestureDetector mScaleDetector;
    private Context appContext;

    private PaletteRing palette;

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
            CW.addShape(s);
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
    void loadCalendarView(Context context, PaletteRing pal) {
        //TODO: suspect proc cur entry not doing well on GC
        //TODO: Move to MainActivity
        palette = pal;
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.DAY_OF_WEEK,1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        CW = new CalendarWin(cal.getTimeInMillis()/1000,10f,1.5f);
        CW.setLog(read_file(context.getApplicationContext(), MainActivity.LOG_FILE));
        CW.setLineWidth(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
        curTask = CW.getCurComment();
        curTaskColor = CW.getCurColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        CW.draw(canvas); //XTODO: Investigate why draw is happening multiple times
    }
    private float mLastTouchX, mLastTouchY;
    private boolean has_run;
    private final Handler handler = new Handler();
    private Runnable mLongPressed = new Runnable() { public void run() {
        has_run = true;
        final CalendarRect selection = CW.getShape(mLastTouchX,mLastTouchY);
        if (selection != null) {
            LayoutInflater inflater = LayoutInflater.from(appContext);
            View promptView = inflater.inflate(R.layout.edit_interval, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(appContext);
            alertDialogBuilder.setView(promptView); //TODO: Move this out?

            final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
            commentEntry.setText(selection.comment);
            final EditText startEntry = (EditText) promptView.findViewById(R.id.startEdit); //TODO: Allow editing of "natural" text
            startEntry.setText(Long.toString(selection.start));
            final EditText endEntry = (EditText) promptView.findViewById(R.id.endEdit);
            endEntry.setText(Long.toString(selection.end));

            final View curColorV = promptView.findViewById(R.id.CurColor);
            try { curColorV.setBackgroundColor(selection.paint.getColor());
            } catch (Exception e) { curColorV.setBackgroundColor(CalendarRect.COLOR_ERROR); }

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

            final Context finalContext = appContext;
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
                            selection.set(Long.parseLong(startEntry.getText().toString()),
                                    Long.parseLong(endEntry.getText().toString()),
                                    ((ColorDrawable)  curColorV.getBackground()).getColor(),
                                    commentEntry.getText().toString());
                            List<String> newLogEntries = CW.getLog(selection);
                            File internalFile = new File(finalContext.getFilesDir(), MainActivity.LOG_FILE);
                            try {
                                internalFile.delete();
                                FileOutputStream out = new FileOutputStream(internalFile, true);
                                for (String s : newLogEntries) {
                                    out.write(s.getBytes());
                                    out.write(System.getProperty("line.separator").getBytes());
                                }
                                out.close();
                            } catch (Exception e) {
                                Log.d("SquareDays", e.toString());
                                Toast.makeText(finalContext, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
                            }
                            CW.setLog(newLogEntries);
                            invalidate();
                        }
                    })
                    .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            selection.set(-1,-1,0,"");
                            List<String> newLogEntries = CW.getLog(selection);
                            File internalFile = new File(finalContext.getFilesDir(), MainActivity.LOG_FILE);
                            try {
                                internalFile.delete();
                                FileOutputStream out = new FileOutputStream(internalFile, true);
                                for (String s : newLogEntries) {
                                    out.write(s.getBytes());
                                    out.write(System.getProperty("line.separator").getBytes());
                                }
                                out.close();
                            } catch (Exception e) {
                                Log.d("SquareDays", e.toString());
                                Toast.makeText(finalContext, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
                            }
                            CW.setLog(newLogEntries);
                            invalidate();
                            //TODO: invalidate on import log as well
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
                has_run = false;
                handler.postDelayed(mLongPressed,1200);
                mLastTouchX = x;
                mLastTouchY = y;
                //TODO: Selection box
                return true;
            case (MotionEvent.ACTION_MOVE):
                if (has_run) {  //TODO: need a buffer zone for move detection
                    return false;
                } else {
                    handler.removeCallbacks(mLongPressed);
                    if (Math.abs(x - mLastTouchX) + Math.abs(y - mLastTouchY) < 150) {
                        CW.shift(x - mLastTouchX, y - mLastTouchY);
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
                    CalendarRect selection = CW.getShape(x,y);
                    if (selection != null) {
                        long duration = 1000L* (selection.end - selection.start);
                        CW.setStatusText(selection.comment + ":"
                                + new SimpleDateFormat(" h:mm-").format(new Date(selection.start*1000L))
                                + new SimpleDateFormat("h:mm").format(new Date(selection.end*1000L))
                                + String.format(" (%d:%02d)", TimeUnit.MILLISECONDS.toHours(duration),
                                TimeUnit.MILLISECONDS.toMinutes(duration)%60));
                    } else
                        CW.setStatusText("");
                }
                invalidate();
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
//TODO: Bug on startup: when there is an active task running, and the app is closed then opened
//TODO: ERROR now-line or ERROR task??
