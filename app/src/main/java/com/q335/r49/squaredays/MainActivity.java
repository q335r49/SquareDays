package com.q335.r49.squaredays;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//TODO: how to write "ongoing" to log?

//TODO: Have CalendarFrag then call to update the AB

//TODO: Need some way to mark and select instant times -- probably by modifying the messagebox

//TODO: TYPOGRAPHY: Use one letter for all
//TODO: *** Still: Startup bug: the AB isn't being updated even though there is a "current task"
//TODO: End task should be "add new task" WHEN THERE IS NO ACTIVE TASK (on long-press). When there is an active task, it should change to comment.
//TODO: There should not be a "blank" button

//TODO: Automatically add a space to comments
//TODO: "Selected" box around currently running task -- should be white. Otherwise, boxes are black
//TODO: Do not change Action Bar color -- only text

//TODO: Clean up overlap stuff *WHILE THE ACTION IS BEING WRITTEN* in the shapes thing
//TODO: Change "No active task" to an empty Stirng.

//TODO: Now time shows error color after removing a task from calendar
//TODO: Rounded rectangles in Calendar.
//TODO: Grid rectangle should be stroked boxes
//TODO: Statusbar color

//TODO: Bug: current task not being written correctly to log

class logEntry {
    private static final int REMOVE = -1;
    private static final int CMD_ADD_COMMENT = 10;
    private static final int CMD_END_TASK = 11;
    static final int MESS_CLEAR_LOG = 100;
    static final int MESS_REDRAW = 101;

    private int command;
        void markForRemoval() { command = REMOVE; }
        boolean markedForRemoval() {return (command == REMOVE); }
        boolean isCommand() {return command >= CMD_ADD_COMMENT && command < MESS_CLEAR_LOG; }
        boolean isMessage() {return command >= MESS_CLEAR_LOG;}
        void procCommand(logEntry com) {
            switch (com.command) {
                case CMD_ADD_COMMENT:
                    comment += com.comment;
                    break;
                case CMD_END_TASK:
                    if (isOngoing())
                        end = com.end;
                        onGoing = false;
                    break;
            }
        }
        int getMessage() {
            return command;
        }
        void updateTask(logEntry newTask) {
            if (isOngoing() && newTask.isOngoing()) //TODO: Error checking
                end = newTask.start;
        }

    Paint paint;
        void setColor(int color) { paint.setColor(color); }
    String comment;
    long start;
    long end;
        void setInterval(long start, long end) { this.start = start; this.end = end; }
    logEntry() { }
    private boolean onGoing = false;
        boolean isOngoing() { return onGoing; }

    static logEntry newInterval(long start_ts, long end_ts, int color) {
        logEntry le = new logEntry();
            le.paint = new Paint();
                le.paint.setColor(color);
            le.start = start_ts;
            le.end = end_ts;
        return le;
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
            le.onGoing = true;
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
            le.command = MESS_CLEAR_LOG;
        return le;
    }
    public static logEntry newFromString(String s) throws IllegalArgumentException {    //TODO: handle ONGOING in read & write
        String[] args = s.split(">",-1);
        if (args.length < 5)
            throw new IllegalArgumentException("Unparsable string, need at least 6 arguments: " + s);
        logEntry le = new logEntry();
        // le.readableTimePos = args[0];
        le.paint = new Paint();
            le.paint.setColor(MainActivity.parseColor(args[1]));
        le.start = Long.parseLong(args[2]);
        if (args[3].isEmpty())
            le.onGoing = true;
        else {
            le.end = le.start + Long.parseLong(args[3]) * 60L;
            if (le.start > le.end)
                throw new IllegalArgumentException("Starting after end time: " + s);
        }
        le.comment = args[4];
        return le;
    }
    @Override
    public String toString() {
        if (markedForRemoval() || paint == null)
            return null;
        else if (onGoing)
            return (new Date(start*1000L)).toString() + ">"
                    + String.format("#%06X", 0xFFFFFF & paint.getColor()) + ">"
                    + Long.toString(start) + ">"
                    + ">"
                    + comment;
        else
            return (new Date(start*1000L)).toString() + ">"
                    + String.format("#%06X", 0xFFFFFF & paint.getColor()) + ">"
                    + Long.toString(start) + ">"
                    + Long.toString((end-start)/60) + ">"
                    + comment;
    }
}
public class MainActivity extends AppCompatActivity implements CommandsFrag.OnFragmentInteractionListener, CalendarFrag.OnFragmentInteractionListener {
    static int COLOR_NO_TASK;
    static int COLOR_ERROR;
    static int parseColor(String s) {
        try {
            return Color.parseColor(s);
        } catch (Exception e) {
            Log.d("SquareDays","Bad color: " + s);
            return COLOR_ERROR;
        }
    }

    public void loadLogsFromFile(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    pushTask(logEntry.newFromString(line));
                } catch (Exception E) {
                    Log.d("SquareDays", E.toString());
                }
            }
        } catch (FileNotFoundException e) {
            Log.d("SquareDays","Log file not found!");
        } catch (UnsupportedEncodingException e) {
            Log.d("SquareDays","Log file bad encoding!");
        } catch (IOException e) {
            Log.d("SquareDays","Log file IO exception!");
        }
    }
    public void writeLogsToFile() {
        //TODO: ## figure out when to call this
        //TODO: ## mark boolean changed in commands
        List<String> entries = GF.getWritableShapes();
        File internalFile = new File(context.getFilesDir(), MainActivity.LOG_FILE);
        try {
            internalFile.delete();
            FileOutputStream out = new FileOutputStream(internalFile, true);
            for (String s : entries) {
                out.write(s.getBytes());
                out.write(System.getProperty("line.separator").getBytes());
            }
            out.close();
        } catch (Exception e) {
            Log.d("SquareDays", "File write error: " + e.toString());
            Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
        }
    }

    private Queue<logEntry> logQ = new LinkedList<>();
    public void pushTask(logEntry log) {
        if (log != null)
            logQ.add(log);
    }
    public void popTasks() {
        Log.d("SquareDays","Pop!");
        for(logEntry l = logQ.poll(); l != null; l = logQ.poll())
            GF.procTask(l);
    }
    private List<logEntry> writeBuffer = new ArrayList<>();

    private Toolbar AB;
    int AB_curColor = 0;
    String AB_curText = "";
    int AB_savedColor = 0;
    String AB_savedText = "";
    public void setABState(int color, String text) {
        AB.setBackgroundColor(color);
        AB_curColor = color;
        AB.setTitle(text);
        AB_curText = text;
    }
    public void setPermABState(int color, String text) {
        AB.setBackgroundColor(color);
        AB_curColor = color;
        AB.setTitle(text);
        AB_curText = text;
        AB_savedColor = AB_curColor;
        AB_savedText = AB_curText;

    }
    public void restoreABState() {
        AB.setBackgroundColor(AB_savedColor);
        AB_curColor = AB_savedColor;
        AB.setTitle(AB_savedText);
        AB_curText = AB_savedText;
    }

    static final String LOG_FILE = "log.txt";
    static final String COMMANDS_FILE = "commands.json";
    static final String EXT_STORAGE_DIR = "tracker";
    Context context;
    SharedPreferences sprefs;
    //TODO: selection of shape where there are overlapping tasks

    PaletteRing palette;
    static final int PALETTE_LENGTH = 24;
    public PaletteRing getPalette() {
        return palette;
    }

    CalendarFrag GF;
        public void setGF(CalendarFrag GF) { this.GF = GF; }
    CommandsFrag BF;
        public void setBF(CommandsFrag BF) { this.BF = BF; }
    FragmentManager FM;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onPause() {
        super.onPause();
        writeLogsToFile();  //TODO: find better time to call this
        Log.d("Squaredays","File written");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CalendarFrag.COLOR_NO_TASK =  ResourcesCompat.getColor(getResources(), R.color.no_task, null);
        CalendarWin.COLOR_SCALE_TEXT = ResourcesCompat.getColor(getResources(), R.color.scale_text, null);
        CalendarWin.COLOR_GRID_BACKGROUND = ResourcesCompat.getColor(getResources(), R.color.grid_background, null);
        CalendarWin.COLOR_NOW_LINE = ResourcesCompat.getColor(getResources(), R.color.now_line, null);
        CalendarWin.COLOR_STATUS_BAR = ResourcesCompat.getColor(getResources(), R.color.status_bar, null);
        CommandsFrag.COLOR_ERROR = ResourcesCompat.getColor(getResources(), R.color.error, null);
        COLOR_ERROR = ResourcesCompat.getColor(getResources(), R.color.error, null);
        CommandsFrag.COLOR_END_BOX = ResourcesCompat.getColor(getResources(), R.color.end_box, null);
        COLOR_NO_TASK =  ResourcesCompat.getColor(getResources(), R.color.no_task, null);   //TODO: make this a main static field, etc. (+othrers too)
        CalendarWin.COLOR_SELECTION = ResourcesCompat.getColor(getResources(), R.color.selection, null);

        palette = new PaletteRing(PALETTE_LENGTH);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        sprefs = getApplicationContext().getSharedPreferences("TrackerPrefs", MODE_PRIVATE);

        AB = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(AB);

        BF = new CommandsFrag();
        GF = new CalendarFrag();
        FM = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(FM,BF,GF);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        loadLogsFromFile(context, LOG_FILE);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String extStorPath = Environment.getExternalStorageDirectory() + File.separator + EXT_STORAGE_DIR + File.separator;
        final File cmdFile = new File(extStorPath, COMMANDS_FILE);
        final File logFile = new File(extStorPath, LOG_FILE);
        switch (item.getItemId()) {
            case R.id.menuItemExport: {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                final File directory = new File(extStorPath);
                directory.mkdirs();
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder
                        .setCancelable(true)
                        .setMessage("Selected files will be exported to " + extStorPath)
                        .setNeutralButton("Commands", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    writeString(cmdFile, sprefs.getString("commands", ""));
                                    Toast.makeText(context, "Commands exported to " + extStorPath + COMMANDS_FILE, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.d("SquareDays",e.toString());
                                    Toast.makeText(context, "Export failed. Does this app have storage permission? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("Log entries", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    writeLogsToFile();
                                    copyFile(new File(getFilesDir(), "log.txt"), logFile);
                                    Toast.makeText(context, "Log entries exported to " + extStorPath + LOG_FILE, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.d("SquareDays",e.toString());
                                    Toast.makeText(context, "Export failed. Does this app have storage permission? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setPositiveButton("BOTH", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    writeLogsToFile();
                                    copyFile(new File(getFilesDir(), "log.txt"), logFile);
                                    writeString(cmdFile, sprefs.getString("commands", ""));
                                    Toast.makeText(context, "Commands exported to " + extStorPath + COMMANDS_FILE + System.getProperty("line.separator")
                                            + "Log entries exported to " + extStorPath + LOG_FILE, Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Log.d("SquareDays",e.toString());
                                    Toast.makeText(context, "Export failed. Does this app have storage permission? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .show();
                return true;
            }
            case R.id.menuItemImport: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                final File directory = new File(extStorPath);
                if (!directory.isDirectory())
                    Toast.makeText(context, "Import failed. " + extStorPath + "not found.", Toast.LENGTH_LONG).show();
                else {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertBuilder
                            .setCancelable(true)
                            .setMessage("Importing from " + extStorPath)
                            .setNeutralButton("commands.json", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        String jsonText = readString(cmdFile);
                                        if (jsonText == null)
                                            Toast.makeText(context, "Import failed: empty file", Toast.LENGTH_SHORT).show();
                                        else {
                                            BF.loadCommands(jsonText);
                                            Toast.makeText(context, COMMANDS_FILE + " import successful", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Log.d("SquareDays",e.toString());
                                        Toast.makeText(context, "Import failed. Does this app have storage access? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .setNegativeButton("log.txt", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!logFile.exists())
                                        Toast.makeText(context, "Import failed: log file not found", Toast.LENGTH_LONG).show();
                                    else {
                                        try {
                                            copyFile(logFile, new File(getFilesDir(), "log.txt"));
                                            pushTask(logEntry.newClearMess());
                                            loadLogsFromFile(context, LOG_FILE);
                                            if (GF.isVisible())
                                                popTasks();
                                            Toast.makeText(context, LOG_FILE + " import successful", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Log.d("SquareDays",e.toString());
                                            Toast.makeText(context, "Import failed. Does this app have storage access? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            })
                            .setPositiveButton("Both", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        String jsonText = readString(cmdFile);
                                        if (jsonText == null) {
                                            Toast.makeText(context, "Import failed: empty file", Toast.LENGTH_SHORT).show();
                                        } else {
                                            BF.loadCommands(jsonText);
                                            Toast.makeText(context, COMMANDS_FILE + " import successful", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Log.d("SquareDays",e.toString());
                                        Toast.makeText(context, "Import failed. Does this app have storage access? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                    }
                                    if (!logFile.exists())
                                        Toast.makeText(context, LOG_FILE + " failed: no file", Toast.LENGTH_SHORT).show();
                                    else {
                                        try {
                                            copyFile(logFile, new File(getFilesDir(), "log.txt"));
                                            pushTask(logEntry.newClearMess());
                                            loadLogsFromFile(context, LOG_FILE);
                                            if (GF.isVisible())
                                                popTasks();
                                            Toast.makeText(context, LOG_FILE + " import successful", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Log.d("SquareDays",e.toString());
                                            Toast.makeText(context, "Import failed. Does this app have storage access? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            })
                            .show();
                }
                return true;
            }
            case R.id.menuItemClear: {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder
                        .setCancelable(true)
                        .setMessage("Really clear log?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File logFile = new File(context.getFilesDir(), LOG_FILE);
                                if (logFile.delete()) {
                                    pushTask(logEntry.newClearMess());
                                    setPermABState(COLOR_NO_TASK,"No active task");
                                    if (GF.isVisible())
                                        popTasks();
                                } else
                                    Log.d("SquareDays","Log clear failed!");
                            }
                        })
                        .show();
                return true;
            }
            case R.id.menuItemHelp: {
                FragmentManager fm = getSupportFragmentManager();
                HelpScroller helpV = HelpScroller.newInstance("","");
                helpV.show(fm, "fragment_edit_name");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void writeString(File file, String data) throws Exception {
        FileOutputStream stream = new FileOutputStream(file);
        try {
            stream.write(data.getBytes());
        } finally {
            stream.close();
        }
    }
    public static String readString(File file) throws Exception {
        int length = (int) file.length();
        byte[] bytes = new byte[length];

        FileInputStream in = new FileInputStream(file);
        try {
            in.read(bytes);
        } finally {
            in.close();
        }
        return new String(bytes);
    }
    public static void copyFile(File src, File dst) throws Exception {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dst).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        Fragment F0;
        Fragment F1;
        public SectionsPagerAdapter(FragmentManager fm, Fragment F0, Fragment F1) {
            super(fm);
            this.F0 = F0;
            this.F1 = F1;
        }
        @Override
        public Fragment getItem(int pos) { return pos == 0 ? F0 : F1; }
        @Override
        public int getCount() { return 2; }
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
            }
            return null;
        }
    }
}