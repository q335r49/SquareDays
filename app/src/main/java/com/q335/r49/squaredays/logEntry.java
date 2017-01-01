package com.q335.r49.squaredays;

import android.graphics.Paint;
import android.util.Log;

import java.util.Date;

/**
 * Created by q335r on 1/1/2017.
 */
class logEntry {
    static final int ONGOING = 1;
    static final int EXPENSE = 2;
    static final int CMD_ADD_COMMENT = 10;
    static final int CMD_END_TASK = 11;
    static final int CMD_CLEAR_LOG = 100;
    int command;
    Paint paint;
    long start;
    long end;
    String comment;

    private logEntry() {}
    logEntry(logEntry e) {
        command = e.command;
        paint = new Paint(e.paint);
        comment = e.comment;
        start = e.start;
        end = e.end;
    }
    static logEntry newStartTime(long start) {
        logEntry le = new logEntry();
            le.start = start;
        return le;
    }
    static logEntry newOngoingTask(int color, long start, String comment) {
        logEntry le = new logEntry();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.comment = comment;
            le.command = ONGOING;
        return le;
    }
    static logEntry newExpense(int color, long start, String comment) {
        logEntry le = new logEntry();
        le.paint = new Paint();
            le.paint.setColor(color);
        le.start = start;
        le.comment = comment;
        le.command = EXPENSE;
        return le;
    }
    static logEntry newCompletedTask(int color, long start, long duration, String comment) {
        logEntry le = new logEntry();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.end = le.start + duration;
            le.comment = comment;
        return le;
    }
    static logEntry newEndCommand(long end) {
        logEntry le = new logEntry();
            le.command = CMD_END_TASK;
            le.end = end;
        return le;
    }
    static logEntry newCommentCmd(String s) {
        logEntry le = new logEntry();
        le.command = CMD_ADD_COMMENT;
        le.comment = s;
        return le;
    }
    static logEntry newClearMess() {
        logEntry le = new logEntry();
            le.command = CMD_CLEAR_LOG;
        return le;
    }
    static logEntry newFromLogLine(String s) throws IllegalArgumentException {
        String[] args = s.split(">",-1);
        if (args.length < 5)
            throw new IllegalArgumentException("Unparsable string, need at least 6 arguments: " + s);
        logEntry le = new logEntry();
        // le.readableTimePos = args[0];
        le.paint = new Paint();
            le.paint.setColor(MainActivity.parseColor(args[1]));
        le.start = Long.parseLong(args[2]);
        if (args[3].isEmpty())
            le.command = ONGOING;
        else if (args[3].charAt(0) == 'E')
            le.command = EXPENSE;
        else {
            le.end = le.start + Long.parseLong(args[3]) * 60L;
            if (le.start > le.end)
                throw new IllegalArgumentException("Starting after end time: " + s);
        }
        le.comment = args[4];
        return le;
    }
    String toLogLine() {
        if (paint == null || comment == null) {
            Log.d("SquareDays", "---- Null paint or comment");
            return null;
        } else if (command == ONGOING)
            return (new Date(start*1000L)).toString()
                    + ">" + String.format("#%06X", 0xFFFFFF & paint.getColor())
                    + ">" + Long.toString(start)
                    + ">>" + comment;
        else if (command == EXPENSE)
            return (new Date(start*1000L)).toString()
                    + ">" + String.format("#%06X", 0xFFFFFF & paint.getColor())
                    + ">" + Long.toString(start)
                    + ">E"+ Long.toString(end)
                    + ">" + comment;
        else if (end - start < 60)
            return null;
        else
            return (new Date(start*1000L)).toString()
                    + ">" + String.format("#%06X", 0xFFFFFF & paint.getColor())
                    + ">" + Long.toString(start)
                    + ">" + Long.toString((end-start)/60)
                    + ">" + comment;
    }
}
