package com.q335.r49.squaredays;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

class logEntry {
    static final int ONGOING = 1;
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
            le.command = logEntry.ONGOING;
        else {
            le.end = le.start + Long.parseLong(args[3]) * 60L;
            if (le.start > le.end)
                throw new IllegalArgumentException("Starting after end time: " + s);
        }
        le.comment = args[4];
        return le;
    }
    String toLogLine(boolean onGoing) {
        if (paint == null || comment == null) {
            Log.d("SquareDays", "---- Null paint or comment");
            return null;
        } else if (onGoing)
            return (new Date(start*1000L)).toString() + ">"
                    + String.format("#%06X", 0xFFFFFF & paint.getColor()) + ">"
                    + Long.toString(start) + ">"
                    + ">"
                    + comment;
        else if (end - start < 60) {
            return null;
        } else
            return (new Date(start*1000L)).toString() + ">"
                    + String.format("#%06X", 0xFFFFFF & paint.getColor()) + ">"
                    + Long.toString(start) + ">"
                    + Long.toString((end-start)/60) + ">"
                    + comment;
    }
}
public class MainActivity extends AppCompatActivity implements TasksFrag.OnFragmentInteractionListener, CalendarFrag.OnFragmentInteractionListener {
    static int COLOR_BACKGROUND;
    static int COLOR_ERROR;
    static Typeface CommandFont;
    static int parseColor(String s) {
        try {
            return Color.parseColor(s);
        } catch (Exception e) {
            Log.d("SquareDays","Bad color: " + s);
            return COLOR_ERROR;
        }
    }
    private static boolean LOG_CHANGED;
        static void setLogChanged() {LOG_CHANGED = true;}

    public void readLogsFromFile(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    pushOnly(logEntry.newFromLogLine(line));
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
        if (!LOG_CHANGED)
            return;
        List<String> entries = GF.getWritableShapes();
        File log = new File(context.getFilesDir(), MainActivity.LOG_FILE);
        try {
            if (log.exists()) {
                if (!log.delete())
                    Log.d("SquareDays", "Cannot delete log file");
            }
            FileOutputStream out = new FileOutputStream(log, true);
            for (String s : entries) {
                out.write(s.getBytes());
                out.write(System.getProperty("line.separator").getBytes());
            }
            out.close();
            LOG_CHANGED = false;
        } catch (Exception e) {
            Log.d("SquareDays", "File write error: " + e.toString());
            Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
        }
    }

    private Queue<logEntry> logQ = new LinkedList<>();
    public void pushProc(logEntry log) {
        if (logQ.isEmpty()) {
            if (GF.activityCreated)
                GF.procTask(log);
            else
                logQ.add(log);
        } else {
            logQ.add(log);
            if (GF.activityCreated)
                popAll();
        }
    }
    public void pushOnly(logEntry log) { logQ.add(log); }
    public void popAll() {
        Log.d("SquareDays","Init!");
        logEntry onGoing = null;
        if (logQ.isEmpty())
            onGoing = GF.procTask(logEntry.newCommentCmd(""));
        else for (logEntry le = logQ.poll(); le != null; le = logQ.poll())
            onGoing = GF.procTask(le);
        setSavedAB(onGoing);
        BF.setActiveTask(onGoing);
    }

    private Toolbar AB;
    String AB_curText = "";
    String AB_savedText = "";
    public void setAB(String text) {
        AB.setTitle(text);
        AB_curText = text;
    }
    public void restoreAB() {
        AB.setTitle(AB_savedText);
        AB_curText = AB_savedText;
    }
    private void setSavedAB(logEntry le) {
        String text = le != null ? le.comment + " @" + (new SimpleDateFormat(" h:mm", Locale.US).format(new Date(le.start * 1000L))) : "";
        AB.setTitle(text);
        AB_curText = text;
        AB_savedText = AB_curText;
    }

    static final String LOG_FILE = "log.txt";
    static final String COMMANDS_FILE = "commands.json";
    static final String EXT_STORAGE_DIR = "tracker";
    Context context;
    SharedPreferences sprefs;

    PaletteRing palette;
    static final int PALETTE_LENGTH = 24;
    public PaletteRing getPalette() {
        return palette;
    }

    CalendarFrag GF;
        public void setGF(CalendarFrag GF) { this.GF = GF; }
    TasksFrag BF;
        public void setBF(TasksFrag BF) { this.BF = BF; }
    FragmentManager FM;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onPause() {
        super.onPause();
        writeLogsToFile();
        Log.d("Squaredays","File written");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalendarWin.COLOR_SCALE_TEXT = ResourcesCompat.getColor(getResources(), R.color.scale_text, null);
        CalendarWin.COLOR_GRID_BACKGROUND = ResourcesCompat.getColor(getResources(), R.color.grid_background, null);
        CalendarWin.COLOR_NOW_LINE = ResourcesCompat.getColor(getResources(), R.color.now_line, null);
        CalendarWin.COLOR_STATUS_BAR = ResourcesCompat.getColor(getResources(), R.color.status_bar, null);
        CalendarWin.COLOR_SELECTION = ResourcesCompat.getColor(getResources(), R.color.selection, null);
        TasksFrag.COLOR_END_BOX = ResourcesCompat.getColor(getResources(), R.color.end_box, null);
        COLOR_ERROR = ResourcesCompat.getColor(getResources(), R.color.error, null);
        COLOR_BACKGROUND =  ResourcesCompat.getColor(getResources(), R.color.background, null);
        CommandFont = Typeface.createFromAsset(getAssets(),  "fonts/22203___.TTF");
        palette = new PaletteRing(PALETTE_LENGTH);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        sprefs = getApplicationContext().getSharedPreferences("TrackerPrefs", MODE_PRIVATE);
        AB = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(AB);
        AB.setBackgroundColor(COLOR_BACKGROUND);
        FM = getSupportFragmentManager();
        BF = new TasksFrag();
        GF = new CalendarFrag();
        mSectionsPagerAdapter = new SectionsPagerAdapter(FM,BF,GF);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        readLogsFromFile(context, LOG_FILE);
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
                                    Files.write(sprefs.getString("commands", ""),cmdFile, Charsets.UTF_8);
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
                                    Files.copy(new File(getFilesDir(), "log.txt"),logFile);
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
                                    Files.copy(new File(getFilesDir(), "log.txt"),logFile);
                                    Files.write(sprefs.getString("commands", ""),cmdFile,Charsets.UTF_8);
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
                                        String jsonText = Files.toString(cmdFile,Charsets.UTF_8);
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
                                            Files.copy(logFile, new File(getFilesDir(), "log.txt"));
                                            pushOnly(logEntry.newClearMess());
                                            readLogsFromFile(context, LOG_FILE);
                                            if (GF.activityCreated)
                                                popAll();
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
                                        String jsonText = Files.toString(cmdFile,Charsets.UTF_8);
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
                                            Files.copy(logFile, new File(getFilesDir(), "log.txt"));
                                            pushOnly(logEntry.newClearMess());
                                            readLogsFromFile(context, LOG_FILE);
                                            if (GF.activityCreated)
                                                popAll();
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
                                if (logFile.exists()) {
                                    if (logFile.delete())
                                        pushOnly(logEntry.newClearMess());
                                    else
                                        Log.d("SquareDays", "Log clear failed!");
                                } else
                                    pushOnly(logEntry.newClearMess());
                                if (GF.activityCreated)
                                    popAll();
                            }
                        })
                        .show();
                return true;
            }
            case R.id.menuItemHelp: {
                FragmentManager fm = getSupportFragmentManager();
                HelpScroller helpV = HelpScroller.newInstance();
                helpV.show(fm, "fragment_edit_name");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        Fragment F0;
        Fragment F1;
        SectionsPagerAdapter(FragmentManager fm, Fragment F0, Fragment F1) {
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