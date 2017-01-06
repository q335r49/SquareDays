package com.q335.r49.squaredays;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;
import java.util.Date;

class Interval {
    int command;
        static final int cNULL = 0;
        static final int cONGOING = 1;
        static final int cCOMMENT = 10;
        static final int cENDTASK = 11;
        static final int cCLEARLOG = 100;
    int type;
        static final int tCAL = 0;
        static final int tEXP = 1;

    Paint paint;
    long start;
    long end;
    long group;
    String label;
    String comment;

    private Interval() {}
    private Interval(int command, int type, int color, long start, long end, long group, String label, String comment) {
        this.command = command;
        this.type    = type;
        this.paint   = new Paint();
            this.paint.setColor(color);
        this.start   = start;
        this.end     = end;
        this.group   = group;
        this.label   = label;
        this.comment = comment;
    }
    Interval(Interval i) {                                                                               this    (i.command,i.type,i.paint.getColor(),i.start,i.end,    i.group,i.label,i.comment);}
    static Interval newStartTime(long start) { return new                                                Interval(cNULL,    tCAL,  Glob.COLOR_ERROR,  start,  0,        0,      null,   null     );}
    static Interval newOngoingTask(int color, long start, String label) { return new                     Interval(cONGOING, tCAL,  color,             start,  0,        0,      label,  null     );}
    static Interval newExpense(int color, long start, long amount, long group, String label) {return new Interval(cNULL,    tEXP,  color,             start,  amount,   group,  label,  null     );}
    static Interval newCompleted(int color, long start, long dur, String label) { return new             Interval(cNULL,    tCAL,  color,             start,  start+dur,0,      label,  null     );}
    static Interval newEndCommand(long end) { return new                                                 Interval(cENDTASK, tCAL,  Glob.COLOR_ERROR,  0,      end,      0,      null,   null     );}
    static Interval newCommentCmd(String label) { return new                                             Interval(cCOMMENT, tCAL,  Glob.COLOR_ERROR,  0,      0,        0,      label,  null     );}
    static Interval newClearTimeMsg() { return new                                                       Interval(cCLEARLOG,tCAL,  Glob.COLOR_ERROR,  0,      0,        0,      null,   null     );}
    static Interval newClearExpMess() { return new                                                       Interval(cCLEARLOG,tEXP,  Glob.COLOR_ERROR,  0,      0,        0,      null,   null     );}

    private static final String SEP      = ">";
    private static final int    nArgs    = 7;
    private static final int    pStamp   = 0;
    private static final int    pColor   = 1;
    private static final int    pUTS     = 2;
    private static final int    pEnd     = 3;
    private static final int    pGroup   = 4;
    private static final int    pLabel   = 5;
    private static final int    pComment = 6;
    private static Interval logNull(String errorLabel, String s) { Log.d("newFromLogLine", errorLabel + ": " + s);   return null;}
    private String logNull(String errorLabel) { Log.d("toLogLine", errorLabel + " <" + label + ":" + comment + ">"); return null;}
    static Interval newFromLogLine(String s) throws IllegalArgumentException {
        String[] args = s.split(SEP, -1);
        if (args.length != nArgs)
            return logNull("Wrong number of args",s);

        Interval le = new Interval();
            le.paint = new Paint();
            le.paint.setColor(Glob.parseColor(args[pColor]));

        le.start = Long.parseLong(args[pUTS]);

        le.group = args[pGroup].isEmpty() ? 0 : Long.parseLong(args[pGroup]);

        if (args[pLabel].charAt(0) == '$') {
            if (args[pLabel].length() > 1) {
                le.label = args[pLabel].substring(1);
                le.type = tEXP;
            } else
                return logNull("Empty expense label",s);
        } else if (!args[pLabel].isEmpty()) {
            le.label = args[pLabel];
        } else
            return logNull("Empty time label",s);

        le.comment = args[pComment];

        if (args[pEnd].isEmpty()) {
            if (le.type == tEXP)
                return logNull("Empty expense value",s);
            else
                le.command = cONGOING;
        } else {
            le.end = Long.parseLong(args[pEnd]);
            if (le.type == tEXP) {
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
        String[] args = new String[nArgs];
        args[pStamp]     = new Date(start*1000L).toString();
        args[pColor]        = String.format("#%06X", 0xFFFFFF & paint.getColor());
        args[pUTS]        = Long.toString(start);
        args[pEnd]          = command == cONGOING ? "" : Long.toString(end);
        args[pGroup]        = group == 0 ? "" : Long.toString(group);
        args[pLabel]        = type == tEXP ? "$" + label : label;
        args[pComment]      = comment == null ? "" : comment;
        if (type == tEXP) {
            if (end == 0)
                return logNull("Zero expense");
        } else {
            if (end - start < 60 && command != cONGOING)
                return logNull("Interval negative or too short");
        }
        return TextUtils.join(SEP,args);
    }
}
