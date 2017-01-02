package com.q335.r49.squaredays;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;

class cInterval {
    static final int ONGOING = 1;
    static final int EXPENSE = 2;
    static final int CMD_ADD_COMMENT = 10;
    static final int CMD_END_TASK = 11;
    static final int CMD_CLEAR_LOG = 100;
    static final int CMD_CLEAR_EXP = 101;
    int command;
    Paint paint;
    long start;
    long end;
    long group;
    boolean groupChild;

    String label;
    String comment;

    private cInterval() {}
    cInterval(cInterval e) {
        command = e.command;
        paint = new Paint(e.paint);
        label = e.label;
        start = e.start;
        end = e.end;
    }
    static cInterval newStartTime(long start) {
        cInterval le = new cInterval();
            le.start = start;
        return le;
    }
    static cInterval newOngoingTask(int color, long start, String comment) {
        cInterval le = new cInterval();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.label = comment;
            le.command = ONGOING;
        return le;
    }
    static cInterval newExpense(int color, long start, long amount, String comment) {
        cInterval le = new cInterval();
        le.paint = new Paint();
            le.paint.setColor(color);
        le.start = start;
        le.end = amount;
        le.label = comment;
        le.command = EXPENSE;
        return le;
    }
    static cInterval newCompletedTask(int color, long start, long duration, String comment) {
        cInterval le = new cInterval();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.end = le.start + duration;
            le.label = comment;
        return le;
    }
    static cInterval newEndCommand(long end) {
        cInterval le = new cInterval();
            le.command = CMD_END_TASK;
            le.end = end;
        return le;
    }
    static cInterval newCommentCmd(String s) {
        cInterval le = new cInterval();
        le.command = CMD_ADD_COMMENT;
        le.label = s;
        return le;
    }
    static cInterval newClearMess() {
        cInterval le = new cInterval();
            le.command = CMD_CLEAR_LOG;
        return le;
    }

    private static final String SEP = ">";
    private static final int nArgs = 7;
    private static final int pFormattedTime = 0;
    private static final int pColor = 1;
    private static final int pStamp = 2;
    private static final int pEnd = 3;
    private static final int pGroup = 4;
    private static final int pLabel = 5;
    private static final int pComment = 6;
    private static cInterval logNull(String errorLabel, String s) {
        Log.d("newFromLogLine", errorLabel + ": " + s);
        return null;
    }
    private String logNull(String errorLabel) {
        Log.d("toLogLine", errorLabel + " <" + label + ":" + comment + ">");
        return null;
    }
    static cInterval newFromLogLine(String s) throws IllegalArgumentException {
        String[] args = s.split(SEP, -1);
        if (args.length != nArgs)
            return logNull("Wrong number of args",s);

        cInterval le = new cInterval();
            le.paint = new Paint();
            le.paint.setColor(MainActivity.parseColor(args[pColor]));

        le.start = Long.parseLong(args[pStamp]);

        le.group = args[pGroup].isEmpty() ? 0 : Long.parseLong(args[pGroup]);

        if (args[pLabel].charAt(0) == '$') {
            if (args[pLabel].length() > 1) {
                le.label = args[pLabel].substring(1);
                le.command = EXPENSE;
            } else
                return logNull("Empty expense label",s);
        } else if (!args[pLabel].isEmpty()) {
            le.label = args[pLabel];
        } else
            return logNull("Empty time label",s);

        le.comment = args[pComment];

        if (args[pEnd].isEmpty()) {
            if (le.command == EXPENSE)
                return logNull("Empty expense value",s);
            else
                le.command = ONGOING;
        } else {
            le.end = Long.parseLong(args[pEnd]);
            if (le.command == EXPENSE) {
                if (le.end == 0)
                    return logNull("Zero-valued expense", s);
            } else if (le.start > le.end)
                return logNull("start > end",s);
        }
        return le;
    }
    String toLogLine() {
        if (paint == null || label == null || label.isEmpty())
            return logNull("null or empty paint or label");
        if (groupChild)
            return null;
        String[] args = new String[nArgs];
        args[pFormattedTime] = new Date(start*1000L).toString();
        args[pColor]        = String.format("#%06X", 0xFFFFFF & paint.getColor());
        args[pStamp]        = Long.toString(start);
        args[pEnd]          = command == ONGOING ? "" : Long.toString(end);
        args[pGroup]        = group == 0 ? "" : Long.toString(group);
        args[pLabel]        = command == EXPENSE ? "$" + label : label;
        args[pComment]      = comment == null ? "" : comment;
        if (command == EXPENSE) {
            if (end == 0)
                return logNull("Zero expense");
        } else {
            if (end - start < 60)
                return logNull("Interval negative or too short");
        }
        return TextUtils.join(SEP,args);
    }
}
