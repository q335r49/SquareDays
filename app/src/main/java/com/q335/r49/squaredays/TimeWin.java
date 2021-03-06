package com.q335.r49.squaredays;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

class TimeWin {
    Paint minorTickStyle, majorTickStyle, nowLineStyle, statusBarStyle, selectionStyle, ongoingStyle, gridStyle, overflowLine;
    String statusText;
    void setStatusText(String s) { statusText = s; }
    @Nullable
    private Interval curTask;
    private TreeSet<Interval> shapeIndex;
    static TimeWin newWindowClass(long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        return new TimeWin(tsOrigin,widthDays,heightWeeks,xMin,yMin);
    }
    TimeWin(long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        this.orig = tsOrigin;
        this.gridW = widthDays;
        this.gridH = heightWeeks;
        g0x = xMin;
        g0y = yMin;
        shapeIndex = new TreeSet<>(new Comparator<Interval>() {
            @Override
            public int compare(Interval o1, Interval o2) { return o1.start > o2.start ? -1 : o1.start == o2.start ? 0 : 1; }
        });
        minorTickStyle = new Paint();
            minorTickStyle.setColor(Glob.COLOR_SCALE_TEXT);
            minorTickStyle.setTextSize(Glob.rPxDp * 18f);
            minorTickStyle.setStrokeWidth(Glob.rPxDp * 2f);
        majorTickStyle = new Paint();
            majorTickStyle.setColor(Glob.COLOR_SCALE_TEXT);
            majorTickStyle.setTypeface(Typeface.DEFAULT_BOLD);
            majorTickStyle.setTextSize(Glob.rPxDp * 25f);
            majorTickStyle.setStrokeWidth(Glob.rPxDp * 2f);
        nowLineStyle = new Paint();
            nowLineStyle.setColor(Glob.COLOR_NOW_LINE);
            nowLineStyle.setStrokeWidth(Glob.rPxDp * 2.5f);
        statusBarStyle = new Paint();
            statusBarStyle.setColor(Glob.COLOR_STATUS_BAR);
            statusBarStyle.setTextAlign(Paint.Align.LEFT);
            statusBarStyle.setTypeface(Typeface.DEFAULT_BOLD);
            statusBarStyle.setTextSize(Glob.rPxDp * 20f);
        selectionStyle = new Paint();
            selectionStyle.setStyle(Paint.Style.STROKE);
            selectionStyle.setColor(Glob.COLOR_SELECTION);
            selectionStyle.setStrokeWidth(Glob.rPxDp * 2.5f);
        ongoingStyle = new Paint();
            ongoingStyle.setColor(Glob.COLOR_NOW_LINE);
            ongoingStyle.setStyle(Paint.Style.FILL);
        gridStyle = new Paint();
            gridStyle.setColor(Glob.COLOR_GRID_BACKGROUND);
            gridStyle.setStyle(Paint.Style.FILL);
        overflowLine = new Paint();
            overflowLine.setStyle(Paint.Style.STROKE);
            overflowLine.setColor(Glob.COLOR_OVERFLOW);
            overflowLine.setStrokeWidth(Glob.rPxDp * 4f);
        statusText = "";
        gridRadius = Glob.rPxDp * 2;
    }

    long orig;
    float g0x, g0y;
    float gridW, gridH;
    float rGridScreenW, rGridScreenH;
    private float RECT_SCALING_FACTOR_X = 0.86f;
    float RECT_SCALING_FACTOR_Y = 0.94f;
    float[] tsToScreen(long ts, float offset) {
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
    long screenToTs(float sx, float sy) { return gridToTs(screenToGridX(sx), screenToGridY(sy)); }
    long gridToTs(float gx, float gy) { return (long) (((float) Math.floor(gy)*7 + (gx < 0f ?  0f : gx >= 6f ? 6f : (float) Math.floor(gx)) + (gy - (float) Math.floor(gy)))*86400f) + orig; }
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
    Interval getSelectedTime(float sx, float sy) {
        long ts = screenToTs(sx, sy);
        Interval closest = shapeIndex.ceiling(Interval.newStartTime(ts));
        return selection = closest == null ? null : closest.end < ts ? null : closest;
    }
    Canvas mCanvas;
    static final float scaleGrid = 0.85f;
    long now;
    int screenW, screenH;
    void draw(Canvas canvas) {
        mCanvas = canvas;
        Log.d("SquareDays","Draw Calendar");
        screenH = canvas.getHeight();
        screenW = canvas.getWidth();
        rGridScreenW = gridW/screenW;
        rGridScreenH = gridH/screenH;
        RECT_SCALING_FACTOR_Y = 1f - Glob.rPxDp * 10 * rGridScreenH;
        now = System.currentTimeMillis() / 1000L;

        drawBackgroundGrid();

        for (Interval s : shapeIndex)
            drawInterval(s, s.paint);

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
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 50f, scaledMark, majorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat, Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 26f, majorTickStyle);
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
                        canvas.drawLine(0f, scaledMark, Glob.rPxDp * 50f, scaledMark, majorTickStyle);
                        canvas.drawText((new SimpleDateFormat("M.d",Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 26f, majorTickStyle);
                    }
                } else {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                        canvas.drawText((new SimpleDateFormat(" h:mm",Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
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
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
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
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
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
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
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
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat,Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
                }
            }
        }
        if (selection!=null)
            drawInterval(selection,selectionStyle);
        if (curTask != null)
            drawOngoingInterval(curTask);
        drawNowLine(now);
        if (!statusText.isEmpty())
            canvas.drawText(statusText,Glob.rPxDp*10,screenH-Glob.rPxDp*10,statusBarStyle);
    }
    void drawNowLine(long ts) {
        nowLineStyle.setColor(Glob.COLOR_NOW_LINE);
        long noon = ts - (ts - orig + 864000000000000000L) % 86400L + 43200;
        float[] a = tsToScreen(ts,0f);
        float[] b = tsToScreen(ts,1f);
        float[] c = tsToScreen(noon,0.5f);
        mCanvas.drawLine(0,
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                Glob.rPxDp*50f,
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],nowLineStyle);
    }
    private static final long curveLength = 86400/24;
    float gridRadius = 10f;
    private static final long maxStretch = 86400/40;
    long prevMidn(long ts) {return ts - (ts - orig + 864000000000000000L) % 86400L;}

    private static final float scaleXmin= 0.20f;
    private static final float scaleXmax= 0.65f;
    private static final long durMin    = 60*10;
    private static final long durMax    = 60*150;
    private static final float durScaleF= (scaleXmax - scaleXmin) / (durMax - durMin);
    float scaleX(Interval iv) {
        long dur = iv == null ? 0 : iv == curTask ? now - iv.start : iv.end - iv.start;
        return dur < durMin ? scaleXmin : dur > durMax ? scaleXmax : scaleXmin + durScaleF * (dur - durMin);
    }
    void drawBackgroundGrid() {
        float width = scaleX(curTask);
        long start = Math.max(screenToTs(0f,0f),now);
        long end = screenToTs(screenW, screenH);
        if (end <= start)
            return;
        float[] a, b, c, e;
        long corner = prevMidn(start);
        long midn = corner + 86399L;
        if (start > now + curveLength) {
            for (; corner < end; midn += 86400L) {
                a = tsToScreen(corner, 0);
                b = tsToScreen(midn, 1f);
                c = tsToScreen(midn - 43199L, 0.5f);
                mCanvas.drawRoundRect(new RectF((a[0] - c[0]) * scaleGrid + c[0],
                                                 (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1],
                                                 (b[0] - c[0]) * scaleGrid + c[0],
                                                 (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1]),gridRadius,gridRadius,gridStyle);
                corner = midn + 1;
            }
            return;
        }
        long peak,bot;
        float wPeakBot, wBotPeak, wPlatBase, wBasePlat;
        if (curTask != null) {
            peak = curTask.start;
            if (peak > now)
                peak = now;
            if (peak <= midn-86400)
                peak = midn-86399;
            bot = now + curveLength;
            wPeakBot = (float) (now - peak) / (float) maxStretch;
            wPeakBot = wPeakBot > 1f ? 1f : wPeakBot;
            wBotPeak = 1 - wPeakBot;
            wPlatBase = 0.6f;
            wBasePlat = 1 - wPlatBase;
        } else {
            peak = now;
            bot = now + curveLength;
            wPeakBot = (float) (now - peak) / (float) maxStretch;
            wPeakBot = wPeakBot > 1f ? 1f : wPeakBot;
            wBotPeak = 1 - wPeakBot;
            wPlatBase = 0.99f;
            wBasePlat = 1 - wPlatBase;
        }
        midn = prevMidn(now) + 86399L;
        if (bot < midn) {
            a = tsToScreen(peak, 0);
            b = tsToScreen(bot, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            e = tsToScreen(midn, 1f);
            float xpL = (a[0] - c[0]) * width + c[0];
            float xpR = (b[0] - c[0]) * width + c[0];
            float xbR = (b[0] - c[0]) * scaleGrid + c[0];
            float xbL = (a[0] - c[0]) * scaleGrid + c[0];
            float yp = (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float yb = (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float ym = (e[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            if (yp < screenH && yb > 0) {
                float y0b = yp < -screenH ? -screenH : yp;
                float y1b = yb > 2 * screenH ? 2 * screenH : yb;
                Path tip = new Path();
                    tip.moveTo(xpL, y0b);
                    tip.lineTo(xpR, y0b);
                    tip.cubicTo(xpR, y0b * wBotPeak + y1b * wPeakBot, xbR, y0b*wPlatBase + y1b*wBasePlat, xbR, y1b);
                    tip.lineTo(xbL, y1b);
                    tip.cubicTo(xbL, y0b*wPlatBase + y1b*wBasePlat, xpL, y0b * wBotPeak + y1b * wPeakBot, xpL, y0b);
                    tip.close();
                mCanvas.drawPath(tip, gridStyle);
            }
            if (ym - gridRadius > screenH) {
                if (yb < screenH)
                    mCanvas.drawRect(new RectF((a[0] - c[0]) * scaleGrid + c[0], Math.max(0f,yb), (b[0] - c[0]) * scaleGrid + c[0], screenH),gridStyle);
            } else if (ym > 0 ) {
                float y1b = Math.max(yb,0f);
                Log.d("x","y1b: " + y1b + " y2: " + ym);
                Path base = new Path();
                    base.moveTo(xbR, y1b);
                    if (ym - y1b > gridRadius) {
                        base.lineTo(xbR, ym - gridRadius);
                        base.quadTo(xbR, ym, xbR - gridRadius, ym);
                        base.lineTo(xbL + gridRadius, ym);
                        base.quadTo(xbL, ym, xbL, ym - gridRadius);
                    } else {
                        base.lineTo(xbR, ym);
                        base.lineTo(xbL, ym);
                    }
                    base.lineTo(xbL, y1b);
                    base.close();
                    mCanvas.drawPath(base, gridStyle);
            }
        } else {
            bot = midn;
            a = tsToScreen(peak, 0);
            b = tsToScreen(bot, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            float xpL = (a[0] - c[0]) * width + c[0];
            float xpR = (b[0] - c[0]) * width + c[0];
            float xbR = (b[0] - c[0]) * scaleGrid + c[0];
            float xbL = (a[0] - c[0]) * scaleGrid + c[0];
            float yp = (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            float yb = (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1];
            if (yp < screenH && yb > 0) {
                float y0b = yp < -screenH ? -screenH : yp;
                float y1b = yb > 2 * screenH ? 2 * screenH : yb;
                Path tip = new Path();
                tip.moveTo(xpL, y0b);
                tip.lineTo(xpR, y0b);
                tip.cubicTo(xpR, y0b * wBotPeak + y1b * wPeakBot, xbR, y0b*wPlatBase + y1b*wBasePlat, xbR, y1b);
                tip.lineTo(xbL, y1b);
                tip.cubicTo(xbL, y0b*wPlatBase + y1b*wBasePlat, xpL, y0b * wBotPeak + y1b * wPeakBot, xpL, y0b);
                tip.close();
                mCanvas.drawPath(tip, gridStyle);
            }
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
    void drawInterval(Interval iv, Paint paint) {
        float width = scaleX(iv);
        if (iv.start == -1 || iv.end == -1 || iv.end <= iv.start)
            return;
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < iv.end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*width+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*width+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(iv.end, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        mCanvas.drawRect((a[0]-c[0])*width+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*width+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],paint);
    }
    private void drawOngoingInterval(Interval iv) {
        float width = scaleX(iv);
        long corner = iv.start;
        long midn = iv.start - (iv.start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        for (; midn < now; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            mCanvas.drawRect((a[0]-c[0])*width+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*width+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],iv.paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(now, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        Path pp = new Path();
        float y1 = (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1];
        float y2 = (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1];
        pp.moveTo((a[0]-c[0])*width+c[0],y1);
        pp.lineTo((b[0]-c[0])*width+c[0],y1);
        pp.lineTo((b[0]-c[0])*width+c[0],y2);
        pp.lineTo((a[0]-c[0])*width+c[0],y2);
        pp.close();
        Shader shader = new LinearGradient(0, y1, 0, y2, iv.paint.getColor(), Glob.COLOR_GRID_BACKGROUND, Shader.TileMode.CLAMP);
        ongoingStyle.setShader(shader);
        mCanvas.drawPath(pp, ongoingStyle);
    }
    Interval procTask(Interval a) {
        Interval c;
        switch (a.command) {
            case Interval.cCOMMENT:
                if (curTask != null) {
                    if (curTask.label != null)
                        curTask.label += a.label == null ? "" : a.label;
                    else
                        curTask.label = a.label;
                }
                return curTask;
            case Interval.cENDTASK:
                if (curTask != null) {
                    curTask.end = a.end;
                    c = curTask;
                    curTask = null;
                } else
                    return null;
                break;
            case Interval.cONGOING:
                if (curTask == null) {
                    curTask = a;
                    return curTask;
                } else {
                    if (curTask.start < a.start) {
                        curTask.end = a.start;
                        c = curTask;
                        curTask = a;
                    } else {
                        curTask = a;
                        return curTask;
                    }
                }
                break;
            case Interval.cCLEARLOG:
                curTask = null;
                shapeIndex.clear();
                return null;
            default:
                c = a;
        }
        if (c.end <= c.start)
            return curTask;
        List<Interval> removalList = new ArrayList<>();
        for (Interval p : shapeIndex) {
            if (c.start <= p.start) {
                if (c.end > p.start)
                    if (c.end >= p.end)
                        removalList.add(p);
                    else
                        p.start = c.end;
            } else if (c.start > p.end)
                break;
            else if (c.end < p.end) {
                Interval newLog = new Interval(p);
                newLog.end = c.start;
                shapeIndex.add(newLog);
                p.start = c.end;
                break;
            } else {
                p.end = c.start;
                break;
            }
        }
        for (Interval l : removalList)
            shapeIndex.remove(l);
        shapeIndex.add(c);
        return curTask;
    }
    List<String> getWritableShapes() {
        List<String> LogList = new ArrayList<>();
        String entry;
        for (Interval r : shapeIndex) {
            entry = r.toLogLine();
            if (entry != null)
                LogList.add(entry);
        }
        if (curTask != null) {
            entry = curTask.toLogLine();
            if (entry != null)
                LogList.add(entry);
        }
        return LogList;
    }
    private Interval selection;
        Interval getSelection() { return selection; }
        void removeSelection() {
            if (!shapeIndex.remove(selection))
                Log.d("SquareDays","Cannot remove selection: " + (selection == null ? "null" : selection.toString()));
            selection = null;
            setStatusText("");
        }
        void updateEntry(Interval selection, long start, long end) {
            if (shapeIndex.remove(selection)) {
                selection.start = start;
                selection.end = end;
                procTask(selection);
            } else
                Log.d("SquareDays","Cannot remove selection: " + selection.toString());
        }
}
