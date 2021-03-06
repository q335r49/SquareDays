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

class ExpenseWin extends TimeWin {
    private static final int maxExp = 10000;
    private float expCornerRadius = 5;
    private float rSecExp;
    private ExpenseWin(long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        super(tsOrigin, widthDays, heightWeeks, xMin, yMin);
        rSecExp = 86400f/maxExp;
        gridStyle.setColor(Glob.COLOR_EXP_GRID);
        gridRadius = Glob.rPxDp * 10f;
        expCornerRadius = Glob.rPxDp * 10f;
    }
    @Override
    void drawBackgroundGrid() {
        long start = Math.max(screenToTs(0f, 0f), now);
        long end = screenToTs(screenW, screenH);
        if (end <= start)
            return;
        float[] a, b, c;
        long corner = prevMidn(start);
        long midn = corner + 86399L;
        for (; corner < end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn - 43199L, 0.5f);
            mCanvas.drawRoundRect(new RectF((a[0] - c[0]) * scaleGrid + c[0],
                    (a[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1],
                    (b[0] - c[0]) * scaleGrid + c[0],
                    (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1]), gridRadius, gridRadius, gridStyle);
            corner = midn + 1;
        }
    }

    private HashMap<Long,ExpenseDay> Days = new HashMap<>();
    private HashMap<Long,ExpenseGroup> Groups = new HashMap<>();
    private Expense selectedExp;
        Expense getSelectedExp() { return selectedExp; }
    private static final float expScaleXmin= 0.40f;
    private static final float expScaleXmax= 0.85f;
    private static final long expMin    = 5;
    private static final long expMax    = 30;
    private static final float expScaleF= (expScaleXmax - expScaleXmin) / (expMax - expMin);
    float scaleX(ExpenseWin.Expense e) {
        long amt = e == null ? 0 : e.amount();
        return amt < expMin ? expScaleXmin : amt > expMax ? expScaleXmax : expScaleXmin + expScaleF * (amt - expMin);
    }
    Expense getSelectedExpense(float sx, float sy) {
        long ts = screenToTs(sx, sy);
        long midn = prevMidn(ts);
        ExpenseDay de = Days.get(midn);
        if (de != null) {
            float scaleF = de.total > 86400f/rSecExp ? 86399f / de.total : rSecExp;
            float exp = (ts - midn) / scaleF;
            for (int i = 0; i < de.cumulative.size(); i++)
                if (de.cumulative.get(i) + de.expenses.get(i).amount() > exp) {
                    selectedExp = de.expenses.get(i);
                    if (selectedExp.group != null)
                        selectedExp = selectedExp.group.expenses.get(0);
                    return selectedExp;
                }
        }
        selectedExp = null;
        return null;
    }
    private void drawExpense(Expense e, Paint paint) {
        float width = scaleX(e);
        long start, end;
        ExpenseDay ed = e.day;
        if (ed == null) return;
        int index = ed.expenses.indexOf(e);
        if (index < 0) return;
        float scaleF = ed.total > 86400f/rSecExp ? 86399f/ed.total : rSecExp;
        Interval v = e.iv;
        start = ed.midn + (long) (ed.cumulative.get(index) * scaleF);
        end = ed.midn + (long) ((ed.cumulative.get(index) + v.end) * scaleF);

        long corner = start;
        long midn = start - (start - orig + 864000000000000000L) % 86400L + 86399L;
        float[] a, b, c;
        RectF rect;
        for (; midn < end; midn += 86400L) {
            a = tsToScreen(corner, 0);
            b = tsToScreen(midn, 1f);
            c = tsToScreen(midn-43199L, 0.5f);
            rect = new RectF((a[0]-c[0])*width+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*width+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
            mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, paint);
            corner = midn+1;
        }
        a = tsToScreen(corner, 0);
        b = tsToScreen(end, 1f);
        c = tsToScreen(midn-43199L, 0.5f);
        rect = new RectF((a[0]-c[0])*width+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*width+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
        mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, paint);
    }
    private void drawExpenseDay(ExpenseDay ed) {
        float scaleF = ed.total > 86400f/rSecExp ? 86399f/ed.total : rSecExp;
        int size = ed.expenses.size();
        Interval v;
        long start, end;
        for (int i = 0; i < size; i++) {
            float width = scaleX(ed.expenses.get(i));
            v = ed.expenses.get(i).iv;
            start = ed.midn + (long) (ed.cumulative.get(i) * scaleF);
            end = ed.midn + (long) ((ed.cumulative.get(i) + v.end) * scaleF);
            float[] a = tsToScreen(start, 0);
            float[] b = tsToScreen(end, 1f);
            float[] c = tsToScreen(prevMidn(start) + 43200, 0.5f);
            RectF rect = new RectF((a[0]-c[0])*width+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*width+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
            mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, v.paint);
        }
        if (ed.total > 86400f/rSecExp) {
            float[] a = tsToScreen(ed.midn, 0);
            float[] b = tsToScreen((long) ((ed.midn + 86400 * scaleF / rSecExp)), 1f);
            float[] c = tsToScreen(ed.midn + 43200, 0.5f);
            mCanvas.drawLine((a[0] - c[0]) * 0.6f + c[0],
                    (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1],
                    (b[0] - c[0]) * 0.6f + c[0],
                    (b[1] - c[1]) * RECT_SCALING_FACTOR_Y + c[1], overflowLine);
        }
    }

    @Override
    List<String> getWritableShapes() {
        List<String> LogList = new ArrayList<>();
        for (HashMap.Entry<Long,ExpenseDay> e : Days.entrySet())
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
        RECT_SCALING_FACTOR_Y = 1f - Glob.rPxDp * 10 * rGridScreenH;

        now = prevMidn(System.currentTimeMillis() / 1000L) + 86400;
        drawBackgroundGrid();

        for (HashMap.Entry<Long,ExpenseDay> e : Days.entrySet())
            drawExpenseDay(e.getValue());

        float gridSize;
        String timeFormat;
        if (gridH > 3f) {
            gridSize = 1f;
            timeFormat = "M.d";
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/100000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 50f, scaledMark, majorTickStyle);
                    canvas.drawText((new SimpleDateFormat(timeFormat, Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 26f, majorTickStyle);
                }
            }
        } else if (gridH > 1f) {
            gridSize = 1f/5f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/100000f; scaledMark < screenH; startGrid += gridSize) {
                if (startGrid - Math.floor(startGrid) < 0.001f) {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, Glob.rPxDp * 50f, scaledMark, majorTickStyle);
                        canvas.drawText((new SimpleDateFormat("M.d",Locale.US).format(new Date(gridToTs(-1, startGrid) * 1000))), 0, scaledMark + Glob.rPxDp * 26f, majorTickStyle);
                    }
                } else {
                    scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                    if (scaledMark > 0f) {
                        canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                        canvas.drawText(gridToExpString(startGrid), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
                    }
                }
            }
        } else if (gridH > 1f/6f) {
            gridSize = 1f/25f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/10000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText(gridToExpString(startGrid), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
                }
            }
        } else if (gridH > 1f/24f) {
            gridSize = 1f/100f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/10000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText(gridToExpString(startGrid), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
                }
            }
        } else {
            gridSize = 1f/1000f;
            float scaledMark = 0;
            float startGrid = g0y + (1f - RECT_SCALING_FACTOR_Y) * (g0y - (float) Math.floor(g0y) - 0.5f);
            for (startGrid = (float) Math.floor(startGrid / gridSize) * gridSize + gridSize/10000f; scaledMark < screenH; startGrid += gridSize) {
                scaledMark = ((startGrid - (float) Math.floor(startGrid) - 0.5f) * RECT_SCALING_FACTOR_Y + (float) Math.floor(startGrid) + 0.5f - g0y) / rGridScreenH;
                if (scaledMark > 0f) {
                    canvas.drawLine(0f, scaledMark, Glob.rPxDp * 30f, scaledMark, minorTickStyle);
                    canvas.drawText(gridToExpString(startGrid), 0, scaledMark + Glob.rPxDp * 21f, minorTickStyle);
                }
            }
        }
        if (selectedExp!=null) {
            if (selectedExp.group == null)
                drawExpense(selectedExp, selectionStyle);
            else
                for (Expense s : selectedExp.group.expenses)
                    drawExpense(s, selectionStyle);
        }
        if (!statusText.isEmpty())
            canvas.drawText(statusText,Glob.rPxDp*10,screenH-Glob.rPxDp*10,statusBarStyle);
    }
    private String gridToExpString(float grid) {
        int cents = (int) ((grid - Math.floor(grid)) * 10000f);
        return String.format(Locale.US, "%.2f   ", cents / 100f);
    }
    public static ExpenseWin newWindowClass(long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        return new ExpenseWin(tsOrigin,widthDays,heightWeeks,xMin,yMin);
    }
    @Override
    Interval getSelection() { return selectedExp.iv; }

    @Override
    Interval procTask(Interval v) {
        if (v.command == Interval.cCLEARLOG) {
            Days.clear();
            Groups.clear();
        } else if (v.end > 0)
            new Expense(v);
        return null;
    }

    class Expense {
        Interval iv;
        ExpenseDay day;
        ExpenseGroup group;
        Expense(Interval v) {
            iv = v;
            attach();
            if (v.group > nextGroupCode)
                nextGroupCode = v.group;
        }
        void detach() {
            if (day != null) {
                day.remove(this);
                day = null;
            }
        }
        void attach() {
            long midn = prevMidn(iv.start);
            if (day == null || midn != day.midn) {
                detach();
                day = Days.get(midn);
                if (day == null) {
                    day = new ExpenseDay(midn);
                    Days.put(day.midn, day);
                }
                day.add(this);
            }
        }
        int color() { return iv.paint.getColor(); }
        String label() { return iv.label; }
        void reattach(long time, long amt) {
            iv.start = time;
            iv.end = amt;
            attach();
        }
        long dailyTotal() { return day == null ? 0 : (long) day.total; }
        long amount() { return iv.end; }
        long start() { return iv.start; }
        long days() { return group == null ? 1 : group.days(); }
        String toLogLine() { return iv.toLogLine(); }
    }
    private class ExpenseDay {
        private long midn;
        ArrayList<Expense> expenses;
        private ArrayList<Float> cumulative;
        private float total;
        ExpenseDay(long uts) {
            expenses = new ArrayList<>();
            cumulative = new ArrayList<>();
            total = 0f;
            this.midn = prevMidn(uts);
        }
        private void add(Expense e) {
            expenses.add(e);
            cumulative.add(total);
            total += e.amount();
        }
        private boolean remove(Expense e) {
            int index = expenses.indexOf(e);
            if (index < 0) return false;
            expenses.remove(index);
            cumulative.remove(index);
            total -= e.amount();
            int S = cumulative.size();
            for (; index < S; index++)
                cumulative.set(index, cumulative.get(index) - e.amount());
            return true;
        }
        List<String> getWritableShapes() {
            List<String> entries = new ArrayList<>(expenses.size());
            for (Expense e : expenses)
                entries.add(e.toLogLine());
            return entries;
        }
    }
    private static long nextGroupCode = 0;
    private class ExpenseGroup {
        long code;
        ArrayList<Expense> expenses;
        void detachAll() {
            for (Expense e : expenses)
                e.detach();
        }
        void add(Interval v) {
            Expense e = new Expense(v);
            expenses.add(e);
            e.group = this;
        }
        ExpenseGroup() {
            nextGroupCode++;
            code = nextGroupCode;
            expenses = new ArrayList<>();
        }
        long days() { return expenses.size(); }
    }
    void updateEntry(int color, long start, long amt, long days) {
        if (selectedExp == null)
            return;
        if (selectedExp.group != null) {
            selectedExp.group.detachAll();
            Groups.remove(selectedExp.group.code);
        }
        selectedExp.detach();
        setStatusText("");
        if (days > 1) {
            ExpenseGroup newGroup = new ExpenseGroup();
            Groups.put(newGroup.code,newGroup);
            int day; long midn;
            for (day = 0, midn = prevMidn(start); day < days; day++, midn+=86400)
                newGroup.add(Interval.newExpense(color,midn,amt,newGroup.code, selectedExp.label()));
        } else if (days == 1){
            if (selectedExp == null)
                return;
            selectedExp.detach();
            selectedExp.reattach(start, amt);
        }
        selectedExp = null;
    }
    @Override
    void removeSelection() {
        if (selectedExp == null)
            return;
        else if (selectedExp.group != null) {
            selectedExp.group.detachAll();
            selectedExp.group = null;
        } else {
            selectedExp.detach();
            selectedExp = null;
        }
        setStatusText("");
    }
}