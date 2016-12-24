package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeSet;

public class CalendarFrag extends Fragment {
    static int COLOR_NO_TASK;

    private ScaleView calView;
    private View fragView;
    private Queue<String> EntryBuffer = new LinkedList<>();
    public void procMess(String E) {
        if (calView == null) {
            EntryBuffer.add(E);
            Log.d("SquareDays","Empty calView: buffer size: " + Integer.toString(EntryBuffer.size()) + " / Entry: " + E);
        } else {
            for (String s = EntryBuffer.poll(); s != null; EntryBuffer.poll())
                calView.procMess(s);
            calView.procMess(E);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser)
            procMess(ScaleView.MESS_REDRAW);
    }

    PaletteRing palette;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragView = inflater.inflate(R.layout.fragment_calendar,container,false);
        calView = (ScaleView) (fragView.findViewById(R.id.drawing));
        String Task = calView.getCurTask();
        if (Task != null) {
            int color = calView.getCurTaskColor();
            mListener.setPermABState(color,Task);
        } else {
            mListener.setPermABState(COLOR_NO_TASK,"No active task");
        }
        mListener.setGF(this);
        palette = mListener.getPalette();
        calView.loadCalendarView(getContext(),palette);
        return fragView;
    }

    public CalendarFrag() { } // Required empty public constructor
    private OnFragmentInteractionListener mListener;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
            mListener = (OnFragmentInteractionListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public String commentCurTask(String comment) {
        //TODO:
    }

    public interface OnFragmentInteractionListener {
        void setPermABState(int color, String task);
        void setGF(CalendarFrag cf);
        PaletteRing getPalette();
    }
}
class CalendarWin {
    static int COLOR_SCALE_TEXT;
    static int COLOR_GRID_BACKGROUND;
    static int COLOR_NOW_LINE;
    static int COLOR_STATUS_BAR;
    static int COLOR_SELECTION;
    private Paint textStyle, boldtextStyle, nowLineStyle, statusBarStyle, selectionStyle;
    CalendarWin(long orig, float gridW, float gridH) {
        shapes = new ArrayList<>();
        shapeIndex = new TreeSet<>(new Comparator<CalendarRect>() {
            @Override
            public int compare(CalendarRect o1, CalendarRect o2) {
                return o1.start > o2.start ? 1 : o1.start == o2.start ? 0 : -1;
            }
        });

        curTask = new CalendarRect();
        shapes.add(curTask);
        shapeIndex.add(curTask);

        this.orig = orig;
        g0x = (7f-gridW)*0.8f;
        g0y = -gridH*0.1f;
        this.gridW = gridW;
        this.gridH = gridH;
        textStyle = new Paint();
        textStyle.setStyle(Paint.Style.FILL);
        textStyle.setColor(COLOR_SCALE_TEXT);
        textStyle.setTypeface(Typeface.DEFAULT);
        boldtextStyle = new Paint();
        boldtextStyle.setStyle(Paint.Style.FILL);
        boldtextStyle.setColor(COLOR_SCALE_TEXT);
        boldtextStyle.setTypeface(Typeface.DEFAULT_BOLD);
        nowLineStyle = new Paint();
        nowLineStyle.setStyle(Paint.Style.FILL);
        nowLineStyle.setColor(COLOR_NOW_LINE);
        statusBarStyle = new Paint();
        statusBarStyle.setStyle(Paint.Style.FILL);
        statusBarStyle.setColor(COLOR_STATUS_BAR);
        statusBarStyle.setTextAlign(Paint.Align.LEFT);
        selectionStyle = new Paint();
        selectionStyle.setStyle(Paint.Style.STROKE);
        selectionStyle.setColor(COLOR_SELECTION);
        statusText = "";
    }
    private static float LINE_WIDTH = 10;
    void setLineWidth(float f) {
        LINE_WIDTH = f;
        textStyle.setTextSize(LINE_WIDTH*2f);
        textStyle.setStrokeWidth(LINE_WIDTH/5f);
        boldtextStyle.setTextSize(LINE_WIDTH*2.5f);
        boldtextStyle.setStrokeWidth(LINE_WIDTH/5f);
        statusBarStyle.setTextSize(LINE_WIDTH*2f);
        selectionStyle.setStrokeWidth(LINE_WIDTH/4f);
        nowLineStyle.setStrokeWidth(LINE_WIDTH/4f);
    }

    private long orig;
    private float g0x, g0y;
    private float gridW, gridH;
    private float ratio_grid_screen_W, ratio_grid_screen_H;
    private static float RECT_SCALING_FACTOR_X = 0.86f;
    private static float RECT_SCALING_FACTOR_Y = 0.94f;
    private String statusText;
        void setStatusText(String s) { statusText = s; }
    private float[] conv_ts_screen(long ts, float offset) {
        long days = ts >= orig ? (ts - orig)/86400L : (ts - orig + 1) / 86400L - 1L;
        float dow = (float) ((days + 4611686018427387900L)%7);
        float weeks = (float) (days >= 0? days/7 : (days + 1) / 7 - 1) + ((float) ((ts - orig +4611686018427360000L)%86400) / 86400);
        return new float[] {(dow - g0x)/ ratio_grid_screen_W + offset / ratio_grid_screen_W, (weeks - g0y)/ ratio_grid_screen_H};
    }
    private float[] conv_grid_screen(float gx, float gy) { return new float[] { (gx - g0x)/ ratio_grid_screen_W, (gy - g0y)/ ratio_grid_screen_H}; }
    private float   conv_screen_grid_X(float sx) {
        float gx = sx*ratio_grid_screen_W+g0x;
        float cx = (float) Math.floor(gx) + 0.5f;
        gx = (gx-cx)/RECT_SCALING_FACTOR_X;
        return gx > 0.5f ? 0.5f + cx : gx < -0.5f? -0.5f + cx : gx + cx;
    }
    private float   conv_screen_grid_Y(float sy) {
        float gy = sy*ratio_grid_screen_H+g0y;
        float cy = (float) Math.floor(gy) + 0.5f;
        gy = (gy-cy)/RECT_SCALING_FACTOR_Y;
        return gy > 0.5f ? 0.5f + cy : gy < -0.5f? -0.5f + cy : gy + cy;
    }
    private long    conv_screen_ts(float sx, float sy) { return conv_grid_ts(conv_screen_grid_X(sx), conv_screen_grid_Y(sy)); }
    private long    conv_grid_ts  (float gx, float gy) { return (long) (((float) Math.floor(gy)*7 + (gx < 0f ?  0f : gx >= 6f ? 6f : (float) Math.floor(gx)) + (gy - (float) Math.floor(gy)))*86400f) + orig; }
    void shift(float x, float y) {
        g0y -= y * ratio_grid_screen_H;
    }
    void scale(float scale, float x0, float y0) { //TODO: Increase scaling speed?
        float borderScale = (scale - 1 + RECT_SCALING_FACTOR_Y)/scale/RECT_SCALING_FACTOR_Y;
        if (borderScale*RECT_SCALING_FACTOR_Y > 0.7f || borderScale > 1) {
            g0y = (y0 - y0 / scale) * ratio_grid_screen_H + g0y;
            gridH /= scale;
            ratio_grid_screen_H /= scale;
            RECT_SCALING_FACTOR_Y *= borderScale;
        }
    }
    CalendarRect getShape(float sx, float sy) {
        long ts = conv_screen_ts(sx, sy);
        CalendarRect closest = shapeIndex.floor(new CalendarRect(ts));
        return closest.end < ts ? null : closest;
    }
    private Canvas mCanvas;
    void draw(Canvas canvas) {
        int screenH = canvas.getHeight();
        int screenW = canvas.getWidth();
        long start_ts = conv_screen_ts(0f,0f);
        long end_ts = conv_screen_ts(screenW, screenH);
        ratio_grid_screen_W = gridW/screenW;
        ratio_grid_screen_H = gridH/screenH;
        RECT_SCALING_FACTOR_X = 1f - LINE_WIDTH*ratio_grid_screen_W;
        RECT_SCALING_FACTOR_Y = 1f - LINE_WIDTH*ratio_grid_screen_H;
        mCanvas = canvas;

        drawInterval(new CalendarRect(start_ts, end_ts, COLOR_GRID_BACKGROUND, ""));
        for (CalendarRect s : shapes)
            drawInterval(s);

        float gridSize;
        String timeFormat;
        if (gridH > 3f) {
            gridSize = 1f;
            timeFormat = "M.d";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.6f, boldtextStyle);
                }
            }
        } else if (gridH > 1f) {
            gridSize = 1f/6f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                if (startGrid - Math.floor(startGrid) < 0.01f) {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                        canvas.drawText((new SimpleDateFormat("M.d",Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.6f, boldtextStyle);
                    }
                } else {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                        canvas.drawText((new SimpleDateFormat(" h:mm",Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
                    }
                }
            }
        } else if (gridH > 1f/6f) {
            gridSize = 1f/24f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
                }
            }
        } else if (gridH > 1f/24f) {
            gridSize = 1f/144f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
                }
            }
        } else if (gridH > 1f/144f) {
            gridSize = 1f/720f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
                }
            }
        } else {
            gridSize = 1f/2880f;
            timeFormat = " h:mm:ss";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
                }
            }
        }

        if (selection!=null)
            drawInterval(selection,selectionStyle);

        RECT_SCALING_FACTOR_X = 0.7f;
        long now = System.currentTimeMillis() / 1000L;
        if (curTask.end == -1) {
            curTask.end = now;
            drawInterval(curTask);
            drawNowLine(now, curTask.paint.getColor());
            curTask.end = -1;
        } else
            drawNowLine(now);

        if (!statusText.isEmpty())
            canvas.drawText(statusText,LINE_WIDTH,screenH-LINE_WIDTH,statusBarStyle);
    }
    private void drawInterval(CalendarRect iv) {
        if (iv.start == -1 || iv.end == -1 || iv.end <= iv.start)
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < iv.end; midn += 86400L) {
            a = conv_ts_screen(corner, 0);
            b = conv_ts_screen(midn, 1f);
            c = conv_ts_screen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
            corner = midn+1;
        }
        a = conv_ts_screen(corner, 0);
        b = conv_ts_screen(iv.end, 1f);
        c = conv_ts_screen(midn-43199L, 0.5f);
        mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
    }
    private void drawInterval(CalendarRect iv, Paint paint) {
        if (iv.start == -1 || iv.end == -1 || iv.end <= iv.start)
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < iv.end; midn += 86400L) {
            a = conv_ts_screen(corner, 0);
            b = conv_ts_screen(midn, 1f);
            c = conv_ts_screen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
            corner = midn+1;
        }
        a = conv_ts_screen(corner, 0);
        b = conv_ts_screen(iv.end, 1f);
        c = conv_ts_screen(midn-43199L, 0.5f);
        mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
    }
    private void drawNowLine(long ts) {
        nowLineStyle.setColor(COLOR_NOW_LINE);
        long noon = ts - (ts - orig + 864000000000000000L) % 86400L + 43200;
        float[] a = conv_ts_screen(ts,0f);
        float[] b = conv_ts_screen(ts,1f);
        float[] c = conv_ts_screen(noon,0.5f);
        mCanvas.drawLine((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],nowLineStyle);
    }
    private void drawNowLine(long ts, int color) {
        nowLineStyle.setColor(color);
        long noon = ts - (ts - orig + 864000000000000000L) % 86400L + 43200;
        float[] a = conv_ts_screen(ts,0f);
        float[] b = conv_ts_screen(ts,1f);
        float[] c = conv_ts_screen(noon,0.5f);
        mCanvas.drawLine((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],nowLineStyle);
    }

    private CalendarRect curTask;
        String getCurComment() { return curTask.end == -1 ? curTask.comment  : null; }
        int getCurColor() { return curTask.paint.getColor(); }
    private ArrayList<CalendarRect> shapes;
    private NavigableSet<CalendarRect> shapeIndex;
    private final static int TIMESTAMP_POS = 0;
    //private final static int READABLE_TIME_POS = 1;
    private final static int COLOR_POS = 2;
    private final static int START_POS = 3;
    private final static int END_POS = 4;
    private final static int COMMENT_POS = 5;
    private final static int ARG_LEN = 6;

    void addShape(String line) {    // TODO: Really should not process log. Curse that message buffer!
        long ts;
        String[] args = line.split(">",-1);
        if (args.length < ARG_LEN) {
            Log.d("SquareDays","Insufficient args: "+line);
            return;
        }
        try {
            ts = Long.parseLong(args[TIMESTAMP_POS]);
            if (args[END_POS].isEmpty()) {
                if (!args[START_POS].isEmpty()) {
                    if (curTask.end == -1)
                        curTask.end = ts + Long.parseLong(args[START_POS]);
                    curTask = new CalendarRect(ts + Long.parseLong(args[START_POS]),-1,args[COLOR_POS],args[COMMENT_POS]);
                    shapes.add(curTask);
                    shapeIndex.add(curTask);
                } else if (!args[COMMENT_POS].isEmpty())
                    curTask.comment += args[COMMENT_POS];
                else
                    Log.d("SquareDays","Empty command: "+line);
            } else if (args[START_POS].isEmpty()) {
                curTask.end = ts + Long.parseLong(args[END_POS]);
                curTask.comment += args[COMMENT_POS];
            } else {
                CalendarRect markTD = new CalendarRect(ts + Long.parseLong(args[START_POS]), ts + Long.parseLong(args[END_POS]), args[COLOR_POS], args[COMMENT_POS]);
                shapes.add(markTD);
                shapeIndex.add(markTD);
            }
        } catch (IllegalArgumentException e) {
            Log.d("SquareDays","Bad color or number format: "+line);
        }
    }
    void setLog(List<String> log) {
        shapes = new ArrayList<>();
        shapeIndex = new TreeSet<>(new Comparator<CalendarRect>() {
            @Override
            public int compare(CalendarRect o1, CalendarRect o2) {
                return o1.start > o2.start ? 1 : o1.start == o2.start ? 0 : -1;
            }
        });
        curTask = new CalendarRect();
        shapes.add(curTask);
        shapeIndex.add(curTask);
        for (String line : log)
            addShape(line);
    }
    List<String> getLog(CalendarRect edited) {
        shapes.remove(edited);
        shapes.add(edited);
        List<String> LogList = new ArrayList<>();
        for (CalendarRect r : shapes) {
            if (r.start != -1 && r.end != -1)
                LogList.add(Long.toString(r.start) + ">" + (new Date(r.start*1000L)).toString()
                        + ">" + String.format("#%06X", 0xFFFFFF & r.paint.getColor()) + ">0>" + Long.toString(r.end-r.start) + ">" + r.comment);
        }
        if (curTask.end == -1L && curTask.start!= -1)
            LogList.add(Long.toString(curTask.start) + ">" + (new Date(curTask.start*1000L)).toString()
                    + ">" + String.format("#%06X", 0xFFFFFF & curTask.paint.getColor()) + ">0>-1>" + curTask.comment);
        return LogList;
    }

    private CalendarRect selection;
    void setSelected(CalendarRect selection) {
        this.selection = selection;
    }
}
class CalendarRect {
    static int COLOR_ERROR;
    long start;
    long end;
    String comment;
        public void setComment(String s) {comment = s;}
    Paint paint;
    void set(long start, long end, int color, String comment) {
        this.start = start;
        this.end = end;
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        this.comment = comment;
    }
    CalendarRect() { set(-1,-1,COLOR_ERROR,null); }
    CalendarRect(long start) { set(start,-1,0,null); }
    CalendarRect(long start, long end, String color, String comment) { set(start,end,MainActivity.parseColor(color),comment); }
    CalendarRect(long start, long end, int color, String comment) { set(start,end,color,comment); }
}