package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.TreeSet;

//TODO: Fix the bezier visualization

public class CalendarFrag extends Fragment {
    PaletteRing palette;
    private ScaleView inputLayer;
    public interface OnFragmentInteractionListener {
        void setPermABState(logEntry task);
        void setGF(CalendarFrag cf);
        void popTasksInitial();
        PaletteRing getPalette();
    }
    private OnFragmentInteractionListener mListener;

    void procTask(logEntry le) { mListener.setPermABState(inputLayer.procTask(le)); }
    logEntry getCurrentTask() {return inputLayer == null ? null : inputLayer.getCurTask();}
    List<String> getWritableShapes() {return inputLayer.getWritableShapes(); }
    boolean activityCreated;
    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        mListener.popTasksInitial();
        activityCreated = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        mListener.popTasksInitial();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frame = inflater.inflate(R.layout.fragment_calendar,container,false);
        inputLayer = (ScaleView) (frame.findViewById(R.id.drawing));
        mListener.setGF(this);
        palette = mListener.getPalette();
        inputLayer.loadCalendarView(palette);
        return frame;
    }
    public CalendarFrag() { }
    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }
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
}
class CalendarWin {
    static int COLOR_SCALE_TEXT, COLOR_GRID_BACKGROUND, COLOR_NOW_LINE, COLOR_STATUS_BAR, COLOR_SELECTION;
    private Paint minorTickStyle, majorTickStyle, nowLineStyle, statusBarStyle, selectionStyle, markerStyle, gridStyle;
    private Path marker;
    private float markerSize;
    private String statusText;
    void setStatusText(String s) { statusText = s; }
    private logEntry curTask;
        @Nullable
        logEntry getCurTask() { return curTask; }
    private ArrayList<logEntry> shapes;
    private NavigableSet<logEntry> shapeIndex;
    CalendarWin(long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        this.orig = tsOrigin;
        this.gridW = widthDays;
        this.gridH = heightWeeks;
        g0x = xMin;
        g0y = yMin;
        shapes = new ArrayList<>();
        shapeIndex = new TreeSet<>(new Comparator<logEntry>() {
            @Override
            public int compare(logEntry o1, logEntry o2) { return o1.start > o2.start ? 1 : o1.start == o2.start ? 0 : -1; }
        });
        minorTickStyle = new Paint();
            minorTickStyle.setColor(COLOR_SCALE_TEXT);
        majorTickStyle = new Paint();
            majorTickStyle.setColor(COLOR_SCALE_TEXT);
            majorTickStyle.setTypeface(Typeface.DEFAULT_BOLD);
        nowLineStyle = new Paint();
            nowLineStyle.setColor(COLOR_NOW_LINE);
        statusBarStyle = new Paint();
            statusBarStyle.setColor(COLOR_STATUS_BAR);
            statusBarStyle.setTextAlign(Paint.Align.LEFT);
        selectionStyle = new Paint();
            selectionStyle.setStyle(Paint.Style.STROKE);
            selectionStyle.setColor(COLOR_SELECTION);
        markerStyle = new Paint();
            markerStyle.setColor(COLOR_NOW_LINE);
            markerStyle.setStyle(Paint.Style.FILL);
        gridStyle = new Paint();
            gridStyle.setColor(COLOR_GRID_BACKGROUND);
            gridStyle.setStyle(Paint.Style.FILL);
        statusText = "";
        markerSize = 0.5f;
        marker = new Path();
            marker.moveTo(0f,0f);
            marker.lineTo(-markerSize*LINE_WIDTH, markerSize*LINE_WIDTH);
            marker.lineTo(-markerSize*LINE_WIDTH, -markerSize*LINE_WIDTH);
            marker.lineTo(0f,0f);
            marker.close();
    }
    private static float LINE_WIDTH = 10;
    void setDPIScaling(float f) {
        LINE_WIDTH = f;
        minorTickStyle.setTextSize(LINE_WIDTH*2f);
            minorTickStyle.setStrokeWidth(LINE_WIDTH/5f);
        majorTickStyle.setTextSize(LINE_WIDTH*2.5f);
            majorTickStyle.setStrokeWidth(LINE_WIDTH/5f);
        statusBarStyle.setTextSize(LINE_WIDTH*2f);
        selectionStyle.setStrokeWidth(LINE_WIDTH/4f);
        nowLineStyle.setStrokeWidth(LINE_WIDTH/4f);
        marker = new Path();
            marker.moveTo(0f,0f);
            marker.lineTo(-markerSize*LINE_WIDTH, markerSize*LINE_WIDTH);
            marker.lineTo(-markerSize*LINE_WIDTH, -markerSize*LINE_WIDTH);
            marker.lineTo(0f,0f);
            marker.close();
    }

    private long orig;
    private float g0x, g0y;
    private float gridW, gridH;
    private float rGridScreenW, rGridScreenH;
    private static float RECT_SCALING_FACTOR_X = 0.86f;
    private static float RECT_SCALING_FACTOR_Y = 0.94f;
    private float[] tsToScreen(long ts, float offset) {
        long days = ts >= orig ? (ts - orig)/86400L : (ts - orig + 1) / 86400L - 1L;
        float dow = (float) ((days + 4611686018427387900L)%7);
        float weeks = (float) (days >= 0? days/7 : (days + 1) / 7 - 1) + ((float) ((ts - orig +4611686018427360000L)%86400) / 86400);
        return new float[] {(dow - g0x)/ rGridScreenW + offset / rGridScreenW, (weeks - g0y)/ rGridScreenH};
    }
    private float[] gridToScreen(float gx, float gy) { return new float[] { (gx - g0x)/ rGridScreenW, (gy - g0y)/ rGridScreenH}; }
    private float screenToGridX(float sx) {
        float gx = sx* rGridScreenW +g0x;
        float cx = (float) Math.floor(gx) + 0.5f;
        gx = (gx-cx)/RECT_SCALING_FACTOR_X;
        return gx > 0.5f ? 0.5f + cx : gx < -0.5f? -0.5f + cx : gx + cx;
    }
    private float screenToGridY(float sy) {
        float gy = sy* rGridScreenH +g0y;
        float cy = (float) Math.floor(gy) + 0.5f;
        gy = (gy-cy)/RECT_SCALING_FACTOR_Y;
        return gy > 0.5f ? 0.5f + cy : gy < -0.5f? -0.5f + cy : gy + cy;
    }
    private long screenToTs(float sx, float sy) { return gridToTs(screenToGridX(sx), screenToGridY(sy)); }
    private long gridToTs(float gx, float gy) { return (long) (((float) Math.floor(gy)*7 + (gx < 0f ?  0f : gx >= 6f ? 6f : (float) Math.floor(gx)) + (gy - (float) Math.floor(gy)))*86400f) + orig; }
    void shift(float x, float y) {
        g0y -= y * rGridScreenH;
    }
    void scale(float scale, float x0, float y0) {
        float borderScale = (scale - 1 + RECT_SCALING_FACTOR_Y)/scale/RECT_SCALING_FACTOR_Y;
        if (borderScale*RECT_SCALING_FACTOR_Y > 0.7f || borderScale > 1) {
            g0y = (y0 - y0 / scale) * rGridScreenH + g0y;
            gridH /= scale;
            rGridScreenH /= scale;
            RECT_SCALING_FACTOR_Y *= borderScale;
        }
    }
    logEntry getSelectedShape(float sx, float sy) {
        long ts = screenToTs(sx, sy);
        logEntry closest = shapeIndex.floor(logEntry.newStartTime(ts));
        return closest == null? null : closest.end < ts ? null : closest;
    }

    private Canvas mCanvas;
    private static final float scaleA = 0.25f;
    private static final float scaleB = 1f;
    private static final float scaleGrid = 0.85f;
    private static final long expansionTime = 86400L * 5L;
    private long now, expansionComplete;
    private int screenW, screenH;
    void draw(Canvas canvas) {
        mCanvas = canvas;
        Log.d("SquareDays","Draw Calendar");
        screenH = canvas.getHeight();
        screenW = canvas.getWidth();
        rGridScreenW = gridW/screenW;
        rGridScreenH = gridH/screenH;
        RECT_SCALING_FACTOR_Y = 1f - LINE_WIDTH * rGridScreenH;
        now = System.currentTimeMillis() / 1000L;
        expansionComplete = now - expansionTime;

        drawBackgroundGrid();

        for (logEntry s : shapes)
            drawInterval(s);

        float gridSize;
        String timeFormat;
        if (gridH > 3f) {
            gridSize = 1f;
            timeFormat = "M.d";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, majorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.6f, majorTickStyle);
                }
            }
        } else if (gridH > 1f) {
            gridSize = 1f/6f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                if (startGrid - Math.floor(startGrid) < 0.01f) {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, majorTickStyle);
                        canvas.drawText((new SimpleDateFormat("M.d",Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.6f, majorTickStyle);
                    }
                } else {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, minorTickStyle);
                        canvas.drawText((new SimpleDateFormat(" h:mm",Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, minorTickStyle);
                    }
                }
            }
        } else if (gridH > 1f/6f) {
            gridSize = 1f/24f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, minorTickStyle);
                }
            }
        } else if (gridH > 1f/24f) {
            gridSize = 1f/144f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, minorTickStyle);
                }
            }
        } else if (gridH > 1f/144f) {
            gridSize = 1f/720f;
            timeFormat = " h:mm";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, minorTickStyle);
                }
            }
        } else {
            gridSize = 1f/2880f;
            timeFormat = " h:mm:ss";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/1000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, LINE_WIDTH * 6f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.1f, minorTickStyle);
                }
            }
        }
        if (selection!=null)
            drawInterval(selection,selectionStyle);
        if (curTask.isOngoing())
            drawOngoingInterval(curTask,scaleA);
        if (!statusText.isEmpty())
            canvas.drawText(statusText,LINE_WIDTH,screenH-LINE_WIDTH,statusBarStyle);
    }

    //TODO: draw from start of day
    private static final long curveDuration = 86400/12;
    private static final float gridRadius = 10f;
    private long prevMidn(long ts) {return ts - (ts - orig + 864000000000000000L) % 86400L;}
    private void drawBackgroundGrid() {
        long start = Math.max(screenToTs(0f,0f),now);
        long end = screenToTs(screenW, screenH);
        if (end <= start)
            return;
        long curveLength = curveDuration;
        float[] a, b, c, e;
        long corner;
        if (start > now + curveLength) {
            corner = prevMidn(start);
            long midn = corner + 86399L;

            for (; corner < end; midn += 86400L) {
                a = tsToScreen(corner, 0);
                b = tsToScreen(midn, 1f);
                c = tsToScreen(midn - 43199L, 0.5f);
                mCanvas.drawRoundRect(new RectF ((a[0] - c[0]) * scaleGrid + c[0],
                                                 (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1],
                                                 (b[0] - c[0]) * scaleGrid + c[0],
                                                 (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1]),gridRadius,gridRadius,gridStyle);
                corner = midn + 1;
            }
            return;
        }
        long midn = prevMidn(now) + 86399L;
        if (midn - now > curveLength) {
            a = tsToScreen(now, 0);
            b = tsToScreen(now + curveLength, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            e = tsToScreen(midn, 1f);
            float x0 = (a[0] - c[0]) * scaleA + c[0];
            float x1 = (b[0] - c[0]) * scaleA + c[0];
            float x2 = (b[0] - c[0]) * scaleGrid + c[0];
            float x3 = (a[0] - c[0]) * scaleGrid + c[0];
            float y0 = (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float y1 = (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float y2 = (e[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            if (y0 < screenH && y1 > 0) {
                float y0b = y0 < -screenH ? -screenH : y0;
                float y1b = y1 > 2 * screenH ? 2 * screenH : y1;
                Path tip = new Path();
                    tip.moveTo(x0, y0b);
                    tip.lineTo(x1, y0b);
                    tip.cubicTo(x1, (y0b + y1b) / 2f, x2, (y0b + y1b) / 2f, x2, y1b);
                    tip.lineTo(x3, y1b);
                    tip.cubicTo(x3, (y0b + y1b) / 2f, x0, (y0b + y1b) / 2f, x0, y0b);
                    tip.close();
                mCanvas.drawPath(tip, gridStyle);
            }
            if (y2 - gridRadius > screenH) {
                if (y1 < screenH)
                    mCanvas.drawRect(new RectF((a[0] - c[0]) * scaleGrid + c[0], Math.max(0f,y1), (b[0] - c[0]) * scaleGrid + c[0], screenH),gridStyle);
            } else if (y2 > 0 ) {
                float y1b = Math.max(y1,0f);
                Log.d("x","y1b: " + y1b + " y2: " + y2);
                Path base = new Path();
                    base.moveTo(x2, y1b);
                    if (y2 - y1b > gridRadius) {
                        base.lineTo(x2, y2 - gridRadius);
                        base.quadTo(x2, y2, x2 - gridRadius, y2);
                        base.lineTo(x3 + gridRadius, y2);
                        base.quadTo(x3, y2, x3, y2 - gridRadius);
                    } else {
                        base.lineTo(x2, y2);
                        base.lineTo(x3, y2);
                    }
                    base.lineTo(x3, y1b);
                    base.close();
                    mCanvas.drawPath(base, gridStyle);
            }
        } else {
            a = tsToScreen(now, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            float x0 = (a[0] - c[0]) * scaleA + c[0];
            float x1 = (b[0] - c[0]) * scaleA + c[0];
            float x2 = (b[0] - c[0]) * scaleGrid + c[0];
            float x3 = (a[0] - c[0]) * scaleGrid + c[0];
            float y0 = (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float y1 = (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            Path pp = new Path();
                pp.moveTo(x0,y0);
                pp.lineTo(x1,y0);
                pp.cubicTo(x1,(y0+y1)/2f,x2,(y0+y1)/2f,x2,y1);
                pp.lineTo(x3,y1);
                pp.cubicTo(x3,(y0+y1)/2f,x0,(y0+y1)/2f,x0,y0);
                pp.close();
            mCanvas.drawPath(pp, gridStyle);
        }
        for (corner = midn + 1; corner < end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            RectF rr = new RectF((a[0] - c[0]) * scaleGrid + c[0],
                                 (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1],
                                 (b[0] - c[0]) * scaleGrid + c[0],
                                 (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1]);
            mCanvas.drawRoundRect(rr, gridRadius, gridRadius, gridStyle);
            corner = midn + 1;
        }
    }
    private void drawInterval(logEntry iv) {
        float scaleX = iv.end < expansionComplete ? scaleB : iv.end > now ? scaleA : (float) (iv.end - expansionComplete) * (scaleA - scaleB) / (float) (now - expansionComplete)  + scaleB;
        if (iv.markedForRemoval() || iv.start == -1 || iv.end == -1 || iv.end <= iv.start || iv.isOngoing())
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < iv.end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*scaleX+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*scaleX+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(iv.end, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        mCanvas.drawRect((a[0]-c[0])*scaleX+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*scaleX+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
    }
    private void drawOngoingInterval(logEntry iv, float scaleB) {
        if (iv.markedForRemoval() || iv.start == -1 || now <= iv.start)
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < now; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(now, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        Path pp = new Path();

        float y1 = (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1];
        float y2 = (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1];
        pp.moveTo((a[0]-c[0])*scaleB+c[0],y1);
        pp.lineTo((b[0]-c[0])*scaleB+c[0],y1);
        pp.lineTo((b[0]-c[0])*scaleB+c[0],y2);
        pp.lineTo((a[0]-c[0])*scaleB+c[0],y2);
        pp.close();
        Shader shader = new LinearGradient(0, y1, 0, y2, iv.paint.getColor(), COLOR_GRID_BACKGROUND, Shader.TileMode.CLAMP);
        markerStyle.setShader(shader);
        mCanvas.drawPath(pp, markerStyle);
    }
    private void drawInterval(logEntry iv, Paint paint) {
        if (iv.markedForRemoval() || iv.start == -1 || iv.end == -1 || iv.end <= iv.start)
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < iv.end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(iv.end, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        mCanvas.drawRect((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
    }
    private Path offsetMarker = new Path();
    private void drawMarker(long ts, int color) {
        markerStyle.setColor(color);
        long noon = ts - (ts - orig + 864000000000000000L) % 86400L + 43200;
        float[] a = tsToScreen(ts,0f);
        float[] c = tsToScreen(noon,0.5f);
        marker.offset((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],(a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],offsetMarker);
        mCanvas.drawPath(offsetMarker, markerStyle);
    }
    private void drawNowLine(long ts, int color) {
        nowLineStyle.setColor(color);
        long noon = ts - (ts - orig + 864000000000000000L) % 86400L + 43200;
        float[] a = tsToScreen(ts,0f);
        float[] b = tsToScreen(ts,1f);
        float[] c = tsToScreen(noon,0.5f);
        mCanvas.drawLine((a[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*RECT_SCALING_FACTOR_X+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],nowLineStyle);
    }

    logEntry procCmd(logEntry LE) {
        if (LE.isCommand()) {
            if (curTask != null)
                curTask.procCommand(LE);
        } else {
            shapes.add(LE);
            shapeIndex.add(LE);
            if (LE.isOngoing()) {
                if (curTask != null)
                    curTask.updateTask(LE);
                curTask = LE;
            }
        }
        return curTask;
    }
    List<String> getWritableShapes() {
        List<String> LogList = new ArrayList<>();
        String entry;
        for (logEntry r : shapes) {
            entry = r.toString();
            if (entry != null)
                LogList.add(entry);
        }
        return LogList;
    }
    private logEntry selection;
    void setSelected(logEntry selection) {
        this.selection = selection;
    }
    void clearShapes() {
        shapes.clear();
        shapeIndex.clear();
        curTask = new logEntry();
        shapes.add(curTask);
        shapeIndex.add(curTask);
    }
}