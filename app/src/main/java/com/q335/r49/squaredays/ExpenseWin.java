package com.q335.r49.squaredays;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
//TODO: *** Handle "split budgeting"

class ExpenseWin extends TimeWin {
    private static final float expCornerRadius = 5;
    private static class DailyExpense {
        long midn;
        ArrayList<CalInterval> expenses;
        ArrayList<Float> alreadySpent;
        float amountSpent;
        DailyExpense(long midn) {
            expenses = new ArrayList<>();
            alreadySpent = new ArrayList<>();
            amountSpent = 0f;
            this.midn = midn;
        }
        DailyExpense(CalInterval le, long midn) { this(midn); add(le); }
        void add(CalInterval le) {
            expenses.add(le);
            alreadySpent.add(amountSpent);
            amountSpent += le.end;
        }
        List<String> getWritableShapes() {
            List<String> entries = new ArrayList<>();
            for (CalInterval le : expenses)
                entries.add(le.toLogLine());
            return entries;
        }
    }
    private float rSecondsExpense;
    public ExpenseWin(TouchView sv, long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        super(sv, tsOrigin, widthDays, heightWeeks, xMin, yMin);
        rSecondsExpense = 86400/100;
    }
    private HashMap<Long,DailyExpense> DE = new HashMap<>();
    private void drawExpenseInterval(DailyExpense de, int index, Paint paint) {
        CalInterval le;
        long start, end;
        le = de.expenses.get(index);
        start = de.midn + (long) (de.alreadySpent.get(index) * rSecondsExpense);
        end = de.midn + (long) ((de.alreadySpent.get(index) + le.end) * rSecondsExpense);

        float scaleX = end < expansionComplete ? scaleB : end > now ? scaleA : (float) (end - expansionComplete) * (scaleA - scaleB) / (float) (now - expansionComplete)  + scaleB;
        long corner = start;
        long midn = start - (start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        RectF rect;
        for (; midn < end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            rect = new RectF((a[0]-c[0])*scaleX+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*scaleX+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
            mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(end, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        rect = new RectF((a[0]-c[0])*scaleX+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*scaleX+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
        mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, paint);
    }
    private void drawDailyExpense(DailyExpense de) {
        int size = de.expenses.size();
        CalInterval le;
        long start, end;
        for (int i = 0; i < size; i++) {
            le = de.expenses.get(i);
            start = de.midn + (long) (de.alreadySpent.get(i) * rSecondsExpense);
            end = de.midn + (long) ((de.alreadySpent.get(i) + le.end) * rSecondsExpense);

            float scaleX = end < expansionComplete ? scaleB : end > now ? scaleA : (float) (end - expansionComplete) * (scaleA - scaleB) / (float) (now - expansionComplete)  + scaleB;
            long corner = start;
            long midn = start - (start - orig + 864000000000000000L) % 86400L + 86399L;
            float[] a, b, c;
            RectF rect;
            for (; midn < end; midn += 86400L) {
                a = tsToScreen(corner, 0);
                b = tsToScreen(midn, 1f);
                c = tsToScreen(midn-43199L, 0.5f);
                rect = new RectF((a[0]-c[0])*scaleX+c[0],
                        (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                        (b[0]-c[0])*scaleX+c[0],
                        (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
                mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, le.paint);
                corner = midn+1;
            }
            a = tsToScreen(corner, 0);
            b = tsToScreen(end, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            rect = new RectF((a[0]-c[0])*scaleX+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*scaleX+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
            mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, le.paint);
        }
    }

    @Override
    CalInterval procTask(CalInterval a) {  //TODO: Deal with modification commands
        MainActivity.setLogChanged();
        long midn = prevMidn(a.start);
        DailyExpense currentExpenses = DE.get(midn);
        if (currentExpenses == null)
            DE.put(prevMidn(a.start), new DailyExpense(a,midn));
        else
            currentExpenses.add(a);
        return null;
    }
    @Override
    List<String> getWritableShapes() {
        List<String> LogList = new ArrayList<>();
        for (HashMap.Entry<Long,DailyExpense> e : DE.entrySet())
            LogList.addAll(e.getValue().getWritableShapes());
        return LogList;
    }
    @Override
    void draw(Canvas canvas) {
        mCanvas = canvas;
        Log.d("SquareDays","Draw Calendar");
        screenH = canvas.getHeight();
        screenW = canvas.getWidth();
        rGridScreenW = gridW/screenW;
        rGridScreenH = gridH/screenH;
        RECT_SCALING_FACTOR_Y = 1f - LINE_WIDTH * rGridScreenH;
        expansionComplete = now - expansionTime;

        now = prevMidn(System.currentTimeMillis() / 1000L) + 86400;
        drawBackgroundGrid();

        for (HashMap.Entry<Long,DailyExpense> e : DE.entrySet())
            drawDailyExpense(e.getValue());

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
                    canvas.drawText((new SimpleDateFormat(timeFormat, Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + LINE_WIDTH * 2.6f, majorTickStyle);
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
            drawExpenseInterval(selected, selectedIndex, selectionStyle);
        drawNowLine(now);
        if (!statusText.isEmpty())
            canvas.drawText(statusText,LINE_WIDTH,screenH-LINE_WIDTH,statusBarStyle);
    }

    public static ExpenseWin newWindowClass(TouchView sv, long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        return new ExpenseWin(sv, tsOrigin,widthDays,heightWeeks,xMin,yMin);
    }

    private DailyExpense selected;
    private int selectedIndex;
    @Override
    CalInterval getSelectedShape(float sx, float sy) {
        long ts = screenToTs(sx, sy);
        long midn = prevMidn(ts);
        DailyExpense de = DE.get(midn);
        if (de != null) {
            float exp = (ts - midn) / rSecondsExpense;
            for (int i = 0; i < de.alreadySpent.size(); i++)
                if (de.alreadySpent.get(i) + de.expenses.get(i).end > exp) {
                    selected = de;
                    selectedIndex = i;
                    return de.expenses.get(i);
                }
        }
        selected = null;
        return null;
    }
}
