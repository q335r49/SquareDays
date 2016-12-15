package com.q335.r49.squaredays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private ScaleView mView;
    private View fragView;
    private Queue<String> EntryBuffer = new LinkedList<>();
    public void procMess(String E) { //TODO: isn't procMessage only processing on NEW entries?!
        if (mView == null) {
            EntryBuffer.add(E);
            Log.e("tracker:","Empty mView: buffer size: " + Integer.toString(EntryBuffer.size()) + " / Entry: " + E);
        } else {
            for (String s = EntryBuffer.poll(); s != null; EntryBuffer.poll())
                mView.procMess(s);
            mView.procMess(E);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragView = inflater.inflate(R.layout.fragment_calendar,container,false);
        mView = (ScaleView) (fragView.findViewById(R.id.drawing));
        ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        String Task = mView.getCurTask();
        if (Task != null && bar != null) {
            bar.setTitle(Task);
            int color = mView.getCurTaskColor();
            mListener.receiveCurBG(color);
            bar.setBackgroundDrawable(new ColorDrawable(color));
        }
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
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public interface OnFragmentInteractionListener {
        void receiveCurBG(int i);
    }
}
class CalendarWin {
    private ArrayList<CalendarRect> shapes;
    private CalendarRect curTask;
        public String getCurComment() { return curTask.end == -1 ? curTask.comment  : null; }
        public int getCurColor() { return curTask.getColor(); }
    private long orig;
        long getOrig() { return orig; }
    private int screenH;
    private int screenW;
    private float g0x;
    private float g0y;
    private float gridW;
    private float gridH;
    private float unit_width;
        float getUnitWidth() {
            return unit_width;
        }
    private float ratio_grid_screen_W;
    private float ratio_grid_screen_H;
    private Paint textStyle;
    private String statusText;
        void setStatusText(String s) { statusText = s; }
    float[] conv_ts_screen(long ts) {
        long days = ts >= orig ? (ts - orig)/86400L : (ts - orig + 1) / 86400L - 1L;
        float dow = (float) ((days + 4611686018427387900L)%7);
        float weeks = (float) (days >= 0? days/7 : (days + 1) / 7 - 1) + ((float) ((ts - orig +4611686018427360000L)%86400) / 86400);
        return new float[] {(dow - g0x)/ ratio_grid_screen_W, (weeks - g0y)/ ratio_grid_screen_H};
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
        g0x = (7-gridW)*0.8f;
        g0y = -1f;
        this.gridW = gridW;
        this.gridH = gridH;
        textStyle = new Paint();
            textStyle.setStyle(Paint.Style.FILL);
            textStyle.setColor(0xFFFFFFFF);
            textStyle.setTextSize(24f);
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
            textStyle.setTextSize(LINE_WIDTH*2);
        }
    void loadEntry(String line) {
        long ts;
        String[] args = line.split(">",-1);
        if (args.length < ARG_LEN) {
            Log.e("tracker:","Insufficient args: "+line);
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
                    Log.e("tracker:","Empty start and end: "+line);
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
            Log.e("tracker:","Bad color or number format: "+line);
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
        screenW = canvas.getWidth();
        screenH = canvas.getHeight();
        this.unit_width = screenW/ gridW;
        ratio_grid_screen_W = gridW/screenW;
        ratio_grid_screen_H = gridH/screenH;
        float scaleX = 1f - LINE_WIDTH*ratio_grid_screen_W;
        float scaleY = 1f - LINE_WIDTH*ratio_grid_screen_H;
        CalendarRect.setRectScalingFactors(scaleX,scaleY);
        float startDate = (float) Math.floor(g0y);
        long start = conv_grid_ts(0f,(float) (Math.floor(g0y)-1));
        long end = conv_grid_ts(7f,(float) (Math.ceil(g0y+gridH)+1));
        CalendarRect BG = new CalendarRect();
        BG.start = start;
        BG.end = end;
        BG.setColor("darkgrey");
        BG.draw(this,canvas);
//        for (CalendarRect s : shapes)
//            s.draw(this,canvas);
        int num_shapes = shapes.size();
        for (int i = 0; i < num_shapes; i++)
            shapes.get(i).draw(this,canvas);
        for (int i = 0; i< gridH +1; i++ ) {
            float[] lblXY = conv_grid_screen((float) -0.5,(float) (startDate+i+0.5));
            canvas.drawText((new SimpleDateFormat("MMM d").format(new Date(conv_grid_ts(-1,startDate+i)*1000))), 25, lblXY[1], textStyle);
        }
        if (!statusText.isEmpty())
            canvas.drawText(statusText,20,screenH-150,textStyle);

        if (curTask.end == -1) {
            curTask.end = System.currentTimeMillis() / 1000L;
            Paint temp = new Paint(curTask.paint);
            curTask.paint.setStyle(Paint.Style.STROKE);
            curTask.paint.setStrokeWidth(LINE_WIDTH/2);
            curTask.draw(this,canvas);
            curTask.paint = temp;
            curTask.end = -1;
        }
    }
}
class CalendarRect {
    private static final float MIN_SCALE = 0.7f;
    private static float RECT_SCALING_FACTOR_X = 0.86f;
    private static float RECT_SCALING_FACTOR_Y = 0.94f;
    public static float getRectScalingFactorY() { return RECT_SCALING_FACTOR_Y; }
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
    String comment=null;
    public Paint paint;
        public int getColor() { return paint.getColor();}

    CalendarRect() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("darkgrey"));
    }
    public void setColor(String color) {
        try {
            paint.setColor(Color.parseColor(color));
        } catch (IllegalArgumentException e) {
            Log.e("tracker:","Bad color format: "+color);
        }
    }
    void draw(CalendarWin cv, Canvas canvas) {
        float[] rectC1;
        float[] rectC2;
        float[] center;
        if (start == -1 || end == -1 || end <= start)
            return;
        long rect0 = start;
        long nextMidnight = start-(start-cv.getOrig() +4611686018427360000L)%86400L+86399L;
        for (; nextMidnight < end; nextMidnight += 86400L) {
            rectC1 = cv.conv_ts_screen(rect0);
            rectC2 = cv.conv_ts_screen(nextMidnight);
            rectC2[0] += cv.getUnitWidth();
            center=cv.conv_ts_screen(nextMidnight-43199L);
            center[0] += cv.getUnitWidth()/2;

            drawRect(rectC1,rectC2,center,canvas);
            //canvas.drawRect(rectC1[0], rectC1[1], rectC2[0] + cv.getUnitWidth(), rectC2[1], paint);
            rect0 = nextMidnight+1;
        }
        rectC1 = cv.conv_ts_screen(rect0);
        rectC2 = cv.conv_ts_screen(end);
        rectC2[0] += cv.getUnitWidth();
        center = cv.conv_ts_screen(nextMidnight-43199L);
        center[0] += cv.getUnitWidth()/2;

        drawRect(rectC1,rectC2,center,canvas);
        //canvas.drawRect(rectC1[0], rectC1[1], rectC2[0] + cv.getUnitWidth(), rectC2[1], paint);
    }
    private void drawRect(float[] r0, float[] r1,float[] rC, Canvas canvas) {
        float n0x=(r0[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0];
        float n0y=(r0[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1];
        float n1x=(r1[0]-rC[0])*RECT_SCALING_FACTOR_X+rC[0];
        float n1y=(r1[1]-rC[1])*RECT_SCALING_FACTOR_Y+rC[1];
        canvas.drawRect(n0x,n0y,n1x,n1y,paint);
    }
}