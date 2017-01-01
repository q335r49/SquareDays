package com.q335.r49.squaredays;
import android.graphics.Paint;
import android.util.Log;
import java.util.Date;

class LogEntry {
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

    private LogEntry() {}
    LogEntry(LogEntry e) {
        command = e.command;
        paint = new Paint(e.paint);
        comment = e.comment;
        start = e.start;
        end = e.end;
    }
    static LogEntry newStartTime(long start) {
        LogEntry le = new LogEntry();
            le.start = start;
        return le;
    }
    static LogEntry newOngoingTask(int color, long start, String comment) {
        LogEntry le = new LogEntry();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.comment = comment;
            le.command = ONGOING;
        return le;
    }
    static LogEntry newExpense(int color, long start, long amount, String comment) {
        LogEntry le = new LogEntry();
        le.paint = new Paint();
            le.paint.setColor(color);
        le.start = start;
        le.end = amount;
        le.comment = comment;
        le.command = EXPENSE;
        return le;
    }
    static LogEntry newCompletedTask(int color, long start, long duration, String comment) {
        LogEntry le = new LogEntry();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start;
            le.end = le.start + duration;
            le.comment = comment;
        return le;
    }
    static LogEntry newEndCommand(long end) {
        LogEntry le = new LogEntry();
            le.command = CMD_END_TASK;
            le.end = end;
        return le;
    }
    static LogEntry newCommentCmd(String s) {
        LogEntry le = new LogEntry();
        le.command = CMD_ADD_COMMENT;
        le.comment = s;
        return le;
    }
    static LogEntry newClearMess() {
        LogEntry le = new LogEntry();
            le.command = CMD_CLEAR_LOG;
        return le;
    }
    static LogEntry newFromLogLine(String s) throws IllegalArgumentException {
        String[] args = s.split(">",-1);
        if (args.length < 5)
            throw new IllegalArgumentException("Unparsable string, need at least 6 arguments: " + s);
        LogEntry le = new LogEntry();
        le.paint = new Paint();
            le.paint.setColor(MainActivity.parseColor(args[1]));
        le.start = Long.parseLong(args[2]);
        if (args[3].isEmpty())
            le.command = ONGOING;
        else if (args[3].charAt(0) == 'E') {
            le.command = EXPENSE;
            if (args[3].length() > 1)
                le.end = Long.parseLong(args[3].substring(1));
            else
                throw new IllegalArgumentException("Bad expenses format: " + s);
        } else {
            le.end = le.start + Long.parseLong(args[3]) * 60L;
            if (le.start > le.end)
                throw new IllegalArgumentException("Starting after end time: " + s);
        }
        le.comment = args[4];
        return le;
    }
    String toLogLine() {    //TODO: remove zero expenses
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
