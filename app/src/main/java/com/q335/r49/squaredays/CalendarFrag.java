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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragView = inflater.inflate(R.layout.fragment_calendar,container,false);
        calView = (ScaleView) (fragView.findViewById(R.id.drawing));
        String Task = calView.getCurTask();
        if (Task != null) {
            mListener.procMess(OnFragmentInteractionListener.AB_SETTEXT,Task);
            int color = calView.getCurTaskColor();
            mListener.procMess(OnFragmentInteractionListener.AB_SETCOLOR,color);
        } else {
            mListener.procMess(OnFragmentInteractionListener.AB_SETTEXT,"No active task");
            mListener.procMess(OnFragmentInteractionListener.AB_SETCOLOR, COLOR_NO_TASK);
        }
        mListener.setGF(this);
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
    public static CalendarFrag newInstance(String param1, String param2) {
        CalendarFrag fragment = new CalendarFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
    public interface OnFragmentInteractionListener {
        int PROC_ENTRY = 9;
        int AB_SETCOLOR = 10;
        int AB_SETTEXT = 11;
        int AB_SAVESTATE = 13;
        int AB_RESTORESTATE = 14;
        void procMess(int code, int arg);
        void procMess(int code, String arg);
        void setGF(CalendarFrag cf);
    }
}
class CalendarWin {
    static int COLOR_SCALE_TEXT;
    static int COLOR_GRID_BACKGROUND;

    private ArrayList<CalendarRect> shapes;
    private CalendarRect curTask;
        String getCurComment() { return curTask.end == -1 ? curTask.comment  : null; }
        int getCurColor() { return curTask.getColor(); }
    private long orig;
        long getOrig() { return orig; }
    private float g0x;
    private float g0y;
    private float gridW;
    private float gridH;
    private float ratio_grid_screen_W;
    private float ratio_grid_screen_H;
    private Paint textStyle;
    private String statusText;
        void setStatusText(String s) { statusText = s; }
    float[] conv_ts_screen(long ts, float offset) {
        long days = ts >= orig ? (ts - orig)/86400L : (ts - orig + 1) / 86400L - 1L;
        float dow = (float) ((days + 4611686018427387900L)%7);
        float weeks = (float) (days >= 0? days/7 : (days + 1) / 7 - 1) + ((float) ((ts - orig +4611686018427360000L)%86400) / 86400);
        return new float[] {(dow - g0x)/ ratio_grid_screen_W + offset / ratio_grid_screen_W, (weeks - g0y)/ ratio_grid_screen_H};
    }
    float[] conv_grid_screen(float gx, float gy) {
        return new float[] { (gx - g0x)/ ratio_grid_screen_W, (gy - g0y)/ ratio_grid_screen_H};
    }
    float[] conv_screen_grid(float sx, float sy) {
        return new float[] {sx*ratio_grid_screen_W+g0x, sy*ratio_grid_screen_H+g0y};
    }
    float conv_grid_num(float gx, float gy) {
        float dow = gx < 0 ?  0 : gx >= 6 ? 6 : gx;
        float weeks = (float) Math.floor(gy)*7;
        return (float) (weeks + dow + (gy-Math.floor(gy)));
    }
    long conv_grid_ts(float gx, float gy) {
        return (long) (conv_grid_num(gx,gy)*86400) + orig;
    }

    CalendarWin(long orig, float gridW, float gridH) {
        shapes = new ArrayList<>();
        curTask = new CalendarRect();
        shapes.add(curTask);
        this.orig = orig;
        g0x = (7f-gridW)*0.8f;
        g0y = -gridH*0.1f;
        this.gridW = gridW;
        this.gridH = gridH;
        textStyle = new Paint();
            textStyle.setStyle(Paint.Style.FILL);
            textStyle.setColor(COLOR_SCALE_TEXT);
            textStyle.setTypeface(Typeface.DEFAULT);
            textStyle.setTextSize(LINE_WIDTH);
            textStyle.setTextAlign(Paint.Align.LEFT);
        statusText = "";
    }
    void shiftWindow(float x, float y) {
        // g0x -= x * ratio_grid_screen_W;
        g0y -= y * ratio_grid_screen_H;
    }
    void reScale(float scale, float x0, float y0) {
        float borderScale = (scale - 1 + CalendarRect.getRectScalingFactorY())/scale/CalendarRect.getRectScalingFactorY();
        if (CalendarRect.isLegalScaling(1,borderScale)) {
            float[] newGridOrig = conv_screen_grid(x0 - x0 / scale, y0 - y0 / scale);
            //g0x = newGridOrig[0];
            g0y = newGridOrig[1];
            //gridW /=scale;
            gridH /= scale;
            //ratio_grid_screen_W = gridW/screenW;
            ratio_grid_screen_H /= scale;
            CalendarRect.scaleRectScalingFactor(1, borderScale);
        }
    }

    //TS>READABLE>COLOR>S>E>COMMENT
    private final static int TIMESTAMP_POS = 0;
    private final static int COLOR_POS = 2;
    private final static int START_POS = 3;
    private final static int END_POS = 4;
    private final static int COMMENT_POS = 5;
    private final static int ARG_LEN = 6;
    private static float LINE_WIDTH = 10;
        public void setLineWidth(float f) {
            LINE_WIDTH = f;
            textStyle.setTextSize(LINE_WIDTH*2f);
            textStyle.setStrokeWidth(LINE_WIDTH/5f);
        }
    void loadEntry(String line) {
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
                    curTask = new CalendarRect();
                    shapes.add(curTask);
                    curTask.start = ts + Long.parseLong(args[START_POS]);
                    curTask.setColor(args[COLOR_POS]);
                    curTask.comment = args[COMMENT_POS];
                } else
                    Log.d("SquareDays","Empty start and end: "+line);
            } else if (args[START_POS].isEmpty()) {
                curTask.end = ts + Long.parseLong(args[END_POS]);
                curTask.comment += args[COMMENT_POS];
            } else {
                CalendarRect markTD = new CalendarRect();
                markTD.start = ts + Long.parseLong(args[START_POS]);
                markTD.end = ts + Long.parseLong(args[END_POS]);
                markTD.setColor(args[COLOR_POS]);
                markTD.comment = args[COMMENT_POS];
                shapes.add(markTD);
            }
        } catch (IllegalArgumentException e) {
            Log.d("SquareDays","Bad color or number format: "+line);
        }
    }
    void loadAllEntries(List<String> log) {
        shapes = new ArrayList<>();
        curTask = new CalendarRect();
        shapes.add(curTask);
        for (String line : log) {
            loadEntry(line);
        }
    }
    void draw(Canvas canvas) {
        int screenH = canvas.getHeight();
        ratio_grid_screen_W = gridW/canvas.getWidth();
        ratio_grid_screen_H = gridH/screenH;
        CalendarRect.setRectScalingFactors(1f - LINE_WIDTH*ratio_grid_screen_W,1f - LINE_WIDTH*ratio_grid_screen_H);
        CalendarRect.setCanvas(canvas);
        CalendarRect.setCv(this);

        long start = conv_grid_ts(0f,(float) (Math.floor(g0y)-1));
        long end = conv_grid_ts(7f,(float) (Math.ceil(g0y+gridH)+1));
        CalendarRect BG = new CalendarRect();
        BG.start = start;
        BG.end = end;
        BG.setColor(COLOR_GRID_BACKGROUND);

        CalendarRect.setRectScalingFactors(0.7f, 0.94f);
        BG.draw();

        CalendarRect.setRectScalingFactors(0.86f, 0.94f);
        for (CalendarRect s : shapes)
            s.draw();

        float gridSize;
        String timeFormat;
        if (gridH > 3f) {
            gridSize = 1f;
            timeFormat = " M.d";
        } else if (gridH > 1f) {
            gridSize = 1f/6f;
            timeFormat = " h:mm";
        } else if (gridH > 1f/6f) {
            gridSize = 1f/24f;
            timeFormat = " h:mm";
        } else if (gridH > 1f/24f) {
            gridSize = 1f/144f;
            timeFormat = " h:mm";
        } else if (gridH > 1f/144f) {
            gridSize = 1f/720f;
            timeFormat = " h:mm";
        } else {
            gridSize = 1f/2880f;
            timeFormat = " h:mm:ss";
        }
        int counter = 0;
        float scaledMark = 0;
        float startGrid = g0y + (1f - CalendarRect.getRectScalingFactorY()) * (g0y - (float) Math.floor(g0y) - 0.5f);
        for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
            scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * CalendarRect.getRectScalingFactorY() + (float) Math.floor(startGrid) + 0.5f - g0y) / ratio_grid_screen_H;
            if (scaledMark > 0f) {
                canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, textStyle);
                canvas.drawText((new SimpleDateFormat(timeFormat).format(new Date(conv_grid_ts(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, textStyle);
            }
            counter++;
        }
        Log.d("SquareDays",Integer.toString(counter));

        CalendarRect.setRectScalingFactors(0.7f, 0.94f);
        curTask.drawCur();

        if (!statusText.isEmpty())
            canvas.drawText(statusText,20,LINE_WIDTH*2,textStyle);
    }
}
class CalendarRect {

    static int COLOR_NOW_LINE;
    static int COLOR_ERROR;

    //TODO: Fix min scale
    private static Canvas canvas;
        static void setCanvas(Canvas canvas) { CalendarRect.canvas = canvas; }
    private static CalendarWin cv;
        static void setCv(CalendarWin cv) { CalendarRect.cv = cv; }
    private static final float MIN_SCALE = 0.3f;
    private static float RECT_SCALING_FACTOR_X = 0.86f;
    private static float RECT_SCALING_FACTOR_Y = 0.94f;
        static float getRectScalingFactorY() { return RECT_SCALING_FACTOR_Y; }
    static void scaleRectScalingFactor(float sx, float sy) {
        float checkX = RECT_SCALING_FACTOR_X*sx;
        float checkY = RECT_SCALING_FACTOR_Y*sy;
        RECT_SCALING_FACTOR_X = checkX < MIN_SCALE ? MIN_SCALE : checkX >= 1f ? 1f : checkX;
        RECT_SCALING_FACTOR_Y = checkY < MIN_SCALE ? MIN_SCALE : checkY >= 1f ? 1f : checkY;
    }
    static void setRectScalingFactors(float checkX, float checkY) {
        RECT_SCALING_FACTOR_X = checkX < MIN_SCALE ? MIN_SCALE : checkX >= 1f ? 1f : checkX;
        RECT_SCALING_FACTOR_Y = checkY < MIN_SCALE ? MIN_SCALE : checkY >= 1f ? 1f : checkY;
    }
    static boolean isLegalScaling(float sx, float sy) {
        float checkX = RECT_SCALING_FACTOR_X*sx;
        float checkY = RECT_SCALING_FACTOR_Y*sy;
        return (checkX > MIN_SCALE && checkX <= 1f && checkY > MIN_SCALE && checkY <= 1f);
    }
    static float[] convGridScreen(float[] p) {
        float cx = (float) Math.floor(p[0]) + 0.5f;
        float cy = (float) Math.floor(p[1]) + 0.5f;
        return new float[] { (p[0]-cx)*RECT_SCALING_FACTOR_X+cx, (p[1]-cy)*RECT_SCALING_FACTOR_Y+cy};
    }

    long start=-1;
    long end=-1;
    private Paint paint;
        public int getColor() { return paint.getColor();}
        public void setColor(String color) { try {paint.setColor(Color.parseColor(color));} catch (Exception e) {Log.d("SquareDays","Bd color: " + color);} }
        public void setColor(int color) { try {paint.setColor(color);} catch (Exception e) {Log.d("SquareDays","Bd color: " + color);} }
    String comment=null;

    CalendarRect() {
        paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(COLOR_ERROR);
            paint.setStrokeWidth(2);
    }
    void draw() {
        if (start == -1 || end == -1 || end <= start)
            return;
        long corner = start;
        long midn = start - (start - cv.getOrig() + 864000000000000000L) % 86400L + 86399L;
        for (; midn < end; midn += 86400L) {
            drawScaledRect(cv.conv_ts_screen(corner, 0),cv.conv_ts_screen(midn, 1f),cv.conv_ts_screen(midn-43199L, 0.5f),paint);
            corner = midn+1;
        }
        drawScaledRect(cv.conv_ts_screen(corner, 0),cv.conv_ts_screen(end, 1f),cv.conv_ts_screen(midn-43199L, 0.5f),paint);
    }
    static void drawScaledRect(float[] r0, float[] r1, float[] rC, Paint paint) {
        canvas.drawRect((r0[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r0[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],
                (r1[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r1[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],paint);
    }
    static void drawScaledLine(float[] r0, float[] r1, float[] rC, Paint paint) {
        canvas.drawLine((r0[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r0[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],
                (r1[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r1[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],paint);
    }
    void drawCur() {
        long now = System.currentTimeMillis() / 1000L;
        if (end == -1) {
            end = now;
                draw();
                drawNowLine(now);
            end = -1;
        } else {
            int tempColor = paint.getColor();
            paint.setColor(COLOR_NOW_LINE);
            drawNowLine(now);
            paint.setColor(tempColor);
        }
    }
    private void drawNowLine(long ts) {
        long noon = ts - (ts - cv.getOrig() + 864000000000000000L) % 86400L + 43200;
        drawScaledLine(cv.conv_ts_screen(ts,0f),cv.conv_ts_screen(ts,1f),cv.conv_ts_screen(noon,0.5f),paint);
    }
}