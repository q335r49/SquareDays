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
    private static final int rSecDay = 86400;
    private static final int maxExp = 100;
    private static final float expCornerRadius = 5;
    private float rSecExp;

    private ExpenseWin(TouchView sv, long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        super(sv, tsOrigin, widthDays, heightWeeks, xMin, yMin);
        rSecExp = 86400f/maxExp;
    }
    private HashMap<Long,ExpenseDay> Days = new HashMap<>();
    private HashMap<Long,ExpenseGroup> Groups = new HashMap<>();
    private Expense selectedExp;
    @Override
    Interval getSelectedShape(float sx, float sy) {
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
                    return selectedExp.iv;
                }
        }
        selectedExp = null;
        return null;
    }
    private void drawExpense(Expense e, Paint paint) {
        Interval v;
        long start, end;
        ExpenseDay ed = e.day;
        if (ed == null) return;
        int index = ed.expenses.indexOf(e);
        if (index < 0) return;
        float scaleF = ed.total > 86400f/rSecExp ? 86399f/ed.total : rSecExp;
        v = e.iv;
        start = ed.midn + (long) (ed.cumulative.get(index) * scaleF);
        end = ed.midn + (long) ((ed.cumulative.get(index) + v.end) * scaleF);

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
    private void drawExpenseDay(ExpenseDay ed) {
        float scaleF = ed.total > 86400f/rSecExp ? 86399f/ed.total : rSecExp;
        int size = ed.expenses.size();
        Interval v;
        long start, end;
        float scaleX = 0.6f;
        for (int i = 0; i < size; i++) {
            v = ed.expenses.get(i).iv;
            start = ed.midn + (long) (ed.cumulative.get(i) * scaleF);
            end = ed.midn + (long) ((ed.cumulative.get(i) + v.end) * scaleF);
            float[] a = tsToScreen(start, 0);
            float[] b = tsToScreen(end, 1f);
            float[] c = tsToScreen(prevMidn(start) + 43200, 0.5f);
            RectF rect = new RectF((a[0]-c[0])*scaleX+c[0],
                    (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                    (b[0]-c[0])*scaleX+c[0],
                    (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
            mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, v.paint);
        }
        float[] a = tsToScreen((long) (ed.midn), 0);
        float[] b = tsToScreen((long) ((ed.midn + 86399*scaleF/rSecExp)), 1f);
        float[] c = tsToScreen((long) ((ed.midn + 43200)), 0.5f);
        RectF rect = new RectF((a[0]-c[0])*scaleX+c[0],
                (a[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1],
                (b[0]-c[0])*scaleX+c[0],
                (b[1]-c[1])*RECT_SCALING_FACTOR_Y+c[1]);
        mCanvas.drawRoundRect(rect, expCornerRadius, expCornerRadius, selectionStyle);
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
        RECT_SCALING_FACTOR_Y = 1f - LINE_WIDTH * rGridScreenH;
        expansionComplete = now - expansionTime;

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
        if (selectedExp!=null) {
            if (selectedExp.group == null)
                drawExpense(selectedExp, selectionStyle);
            else
                for (Expense s : selectedExp.group.expenses)
                    drawExpense(s, selectionStyle);
        }
        drawNowLine(now);
        if (!statusText.isEmpty())
            canvas.drawText(statusText,LINE_WIDTH,screenH-LINE_WIDTH,statusBarStyle);
    }
    public static ExpenseWin newWindowClass(TouchView sv, long tsOrigin, float widthDays, float heightWeeks, float xMin, float yMin) {
        return new ExpenseWin(sv, tsOrigin,widthDays,heightWeeks,xMin,yMin);
    }
    @Override
    Interval getSelection() { return selectedExp.iv; }

    @Override
    Interval procTask(Interval v) {  //TODO: Deal with modification commands, including modify commment
        if (v.command == Interval.cCLEARLOG) {
            Days.clear();
            Groups.clear();
        } else if (v.end > 0) {
            MainActivity.setLogChanged();
            new Expense(v);
        }
        return null;
    }

    class Expense {
        Interval iv;
        ExpenseDay day;
        ExpenseGroup group;
        Expense(Interval v) {
            iv = v;
            attach();
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
        String getLabel() { return iv.label; }
        void reattach(long time, long amt) {
            iv.start = time;
            iv.end = amt;
            attach();
        }
        long amount() { return iv.end; }
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
    }
    void updateEntry(int color, long start, long amt, long days) { //TODO: call from TouchWin
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
                newGroup.add(Interval.newExpense(color,midn,amt,selectedExp.getLabel()));
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