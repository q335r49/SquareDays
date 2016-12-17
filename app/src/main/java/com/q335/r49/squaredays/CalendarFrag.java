package com.q335.r49.squaredays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
    private ScaleView calView;
    private View fragView;
    private Queue<String> EntryBuffer = new LinkedList<>();
    public void procMess(String E) {
        if (calView == null) {
            EntryBuffer.add(E);
            Log.e("SquareDays","Empty calView: buffer size: " + Integer.toString(EntryBuffer.size()) + " / Entry: " + E);
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
        ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        String Task = calView.getCurTask();
        if (Task != null && bar != null) {
            bar.setTitle(Task);
            int color = calView.getCurTaskColor();
            mListener.receiveCurBG(color);
            bar.setBackgroundDrawable(new ColorDrawable(color));
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
        void receiveCurBG(int i);
        void setGF(CalendarFrag cf);
    }
}
class CalendarWin {
    private ArrayList<CalendarRect> shapes;
    private CalendarRect curTask;
        public String getCurComment() { return curTask.end == -1 ? curTask.comment  : null; }
        public int getCurColor() { return curTask.getColor(); }
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
            textStyle.setColor(0xFF2E3E45);
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
        public float getLineWidth() { return LINE_WIDTH;}
    void loadEntry(String line) {
        long ts;
        String[] args = line.split(">",-1);
        if (args.length < ARG_LEN) {
            Log.e("SquareDays","Insufficient args: "+line);
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
                    Log.e("SquareDays","Empty start and end: "+line);
            } else if (args[START_POS].isEmpty()) {
                curTask.end = ts + Long.parseLong(args[END_POS]);
            } else {
                CalendarRect markTD = new CalendarRect();
                markTD.start = ts + Long.parseLong(args[START_POS]);
                markTD.end = ts + Long.parseLong(args[END_POS]);
                markTD.setColor(args[COLOR_POS]);
                markTD.comment = args[COMMENT_POS];
                shapes.add(markTD);
            }
        } catch (IllegalArgumentException e) {
            Log.e("SquareDays","Bad color or number format: "+line);
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
        ratio_grid_screen_W = gridW/canvas.getWidth();
        ratio_grid_screen_H = gridH/canvas.getHeight();

        float scaleX = 1f - LINE_WIDTH*ratio_grid_screen_W;
        float scaleY = 1f - LINE_WIDTH*ratio_grid_screen_H;
        CalendarRect.setRectScalingFactors(scaleX,scaleY);

        long start = conv_grid_ts(0f,(float) (Math.floor(g0y)-1));
        long end = conv_grid_ts(7f,(float) (Math.ceil(g0y+gridH)+1));
        CalendarRect BG = new CalendarRect();
        BG.start = start;
        BG.end = end;
        BG.setColor("darkgrey");
        BG.draw(this,canvas);

        for (CalendarRect s : shapes)
            s.draw(this,canvas);

        if (gridH >= 3f) {
            final float GRID = 1f;
            float startDate = (float) Math.floor(g0y * GRID) / GRID;
            for (int i = 0; i < gridH + 1; i++) {
                float[] lblXY = conv_grid_screen(-0.5f, startDate + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" M.d").format(new Date(conv_grid_ts(-1, startDate + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        } else if (gridH >= 1f) {
            final float GRID = 6f;
            float startHour = (float) Math.floor(g0y * GRID) / GRID;
            for (float i = 0; i < gridH + 1f/GRID; i += 1/GRID) {
                float[] lblXY = conv_grid_screen(-0.5f, startHour + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" h:mm").format(new Date(conv_grid_ts(-1, startHour + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        } else if (gridH >= 1f/6f) {
            final float GRID = 24f;
            float startHour = (float) Math.floor(g0y * GRID) / GRID;
            for (float i = 0; i < gridH + 1f/GRID; i += 1/GRID) {
                float[] lblXY = conv_grid_screen(-0.5f, startHour + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" h:mm").format(new Date(conv_grid_ts(-1, startHour + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        } else if (gridH >= 1f/24f) {
            final float GRID = 144f;
            float startHour = (float) Math.floor(g0y * GRID) / GRID;
            for (float i = 0; i < gridH + 1f/GRID; i += 1/GRID) {
                float[] lblXY = conv_grid_screen(-0.5f, startHour + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" h:mm").format(new Date(conv_grid_ts(-1, startHour + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        } else if (gridH >= 1f/144f) {
            final float GRID = 720f;
            float startHour = (float) Math.floor(g0y * GRID) / GRID;
            for (float i = 0; i < gridH + 1f/GRID; i += 1/GRID) {
                float[] lblXY = conv_grid_screen(-0.5f, startHour + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" h:mm").format(new Date(conv_grid_ts(-1, startHour + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        } else {
            final float GRID = 2880f;
            float startHour = (float) Math.floor(g0y * GRID) / GRID;
            for (float i = 0; i < gridH + 1f/GRID; i += 1/GRID) {
                float[] lblXY = conv_grid_screen(-0.5f, startHour + i);
                canvas.drawLine(0f,lblXY[1],LINE_WIDTH * 6f,lblXY[1],textStyle);
                canvas.drawText((new SimpleDateFormat(" h:mm:ss").format(new Date(conv_grid_ts(-1, startHour + i) * 1000))), 0, lblXY[1] + LINE_WIDTH * 2.1f, textStyle);
            }
        }

        if (!statusText.isEmpty())
            canvas.drawText(statusText,20,LINE_WIDTH*2,textStyle);

        curTask.drawCur(this,canvas);
    }
}
class CalendarRect {
    private static final float MIN_SCALE = 0.7f;
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

    long start=-1;
    long end=-1;
    private Paint paint;
        public int getColor() { return paint.getColor();}
        public void setColor(String color) { try {paint.setColor(Color.parseColor(color));} catch (Exception e) {Log.e("SquareDays","Bd color: " + color);} }
    String comment=null;

    CalendarRect() {
        paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("darkgrey"));
    }

    void draw(CalendarWin cv, Canvas canvas) {
        if (start == -1 || end == -1 || end <= start)
            return;
        long corner = start;
        long midn = start - (start - cv.getOrig() + 864000000000000000L) % 86400L + 86399L;
        for (; midn < end; midn += 86400L) {
            drawScaledRect(cv.conv_ts_screen(corner, 0),cv.conv_ts_screen(midn, 1f),cv.conv_ts_screen(midn-43199L, 0.5f),canvas);
            corner = midn+1;
        }
        drawScaledRect(cv.conv_ts_screen(corner, 0),cv.conv_ts_screen(end, 1f),cv.conv_ts_screen(midn-43199L, 0.5f),canvas);
    }
    private void drawScaledRect(float[] r0, float[] r1, float[] rC, Canvas canvas) {
        canvas.drawRect((r0[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r0[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],
                (r1[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0],
                (r1[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1],paint);
    }
    void drawCur(CalendarWin cv, Canvas canvas) {
        if (end == -1) {
            end = System.currentTimeMillis() / 1000L;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cv.getLineWidth()/4);
            draw(cv,canvas);
            paint.setStyle(Paint.Style.FILL);
            end = -1;
        }
    }
}