package com.q335.r49.squaredays;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//TODO: Visual overhaul
//TODO:     Visually distinguish expense-tasks
//TODO:     Change expense background grid color
//TODO:     Squares




//TODO: is color being changed ANYWHERE??
//TODO: really should have APPEND, if possible (such as with a flag)
//TODO: Implement border auto-increment
//TODO: Rescale drag input for expenses
//TODO: Visually distinguish boxes with "extra" comments
//TODO: Rounded corners in TimeWin
//TODO: Background fading
//TODO: Eventually: draw only those intevrals that are onscreen
//TODO: Allow for delay or groups?


public class MainActivity extends AppCompatActivity implements TasksFrag.OnFragmentInteractionListener, CalendarFrag.OnFragmentInteractionListener,PopupMenu.OnMenuItemClickListener  {
    Context context;
    SharedPreferences prefs;
    static int parseColor(String s) {
        try {
            return Color.parseColor(s);
        } catch (Exception e) {
            Log.d("SquareDays","Bad color: " + s);
            return Glob.COLOR_ERROR;
        }
    }
    private static final String LOG_FILE = "log.txt";
    private static final String COMMANDS_FILE = "commands.json";
    private static final String EXT_STORAGE_DIR = "tracker";
    private static boolean logChanged;
    private void readLogFile() {
        try {
            for (String l : Files.readLines(new File(getFilesDir(), LOG_FILE), Charsets.UTF_8))
                pushOnly(Interval.newFromLogLine(l));
        } catch (Exception e) {
            Log.d("SquareDays","Log read exception: " + e.toString());
        }
    }
    private void writeLogFile() {
        if (!logChanged)
            return;
        try {
            File file = new File(getFilesDir(), LOG_FILE);
            List<String> fullLog = CW.getWritableShapes();
            fullLog.addAll(EW.getWritableShapes());
            Files.asCharSink(file, Charsets.UTF_8).writeLines(fullLog);
            logChanged = false;
        } catch (Exception e) {
            Log.d("SquareDays", "File write error: " + e.toString());
            Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
        }
    }
    private Queue<Interval> logQ;
    public void pushProc(Interval log) {
        logChanged = true;
        if (logQ.isEmpty()) {
            if (log.type == Interval.tEXP) {
                if (EW != null) {
                    EW.procTask(log);
                    EF.invalidate();
                } else
                    logQ.add(log);
            } else {
                if (CW != null) {
                    BF.setSavedAB(CW.procTask(log));
                    CF.invalidate();
                } else
                    logQ.add(log);
            }
        } else {
            logQ.add(log);
            popAll();
        }
    }
    public void pushOnly(Interval log) { logChanged = true; if (log != null) logQ.add(log); }
    public void popAll() {
        if (CW == null || BF == null || EW == null)
            return;
        Log.d("SquareDays","Init!");
        Interval onGoing = null;
        if (logQ.isEmpty())
            onGoing = CW.procTask(Interval.newCommentCmd(""));
        else for (Interval le = logQ.poll(); le != null; le = logQ.poll()) {
            if (le.type == Interval.tEXP)
                EW.procTask(le);
            else
                onGoing = CW.procTask(le);
        }
        BF.setSavedAB(onGoing);
        BF.setActiveTask(onGoing);
        EF.invalidate();
        CF.invalidate();

    }

    FragmentManager FM;
    TasksFrag BF;
        public void setBF(TasksFrag BF) { this.BF = BF; }
    CalendarFrag<TimeWin> CF;
    TimeWin CW;
    CalendarFrag<ExpenseWin> EF;
    ExpenseWin EW;
        public <T extends TimeWin> void setWin(CalendarFrag<T> frag, T disp, String code) {
        if (code.equals(CalendarFrag.CODE_CAL)) {
            CF = (CalendarFrag<TimeWin>) frag;
            CW = disp;
        } else {
            EF = (CalendarFrag<ExpenseWin>) frag;
            EW = (ExpenseWin) disp;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeLogFile();
        Log.d("Squaredays","File written");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        prefs = getApplicationContext().getSharedPreferences("TrackerPrefs", MODE_PRIVATE);
        Glob.init(context);
        logQ = new LinkedList<>();

        setContentView(R.layout.activity_main);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
            FM = getSupportFragmentManager();
            BF = new TasksFrag();
            CF = CalendarFrag.newInstance(CalendarFrag.CODE_CAL);
            EF = CalendarFrag.newInstance(CalendarFrag.CODE_EXP);
        mViewPager.setAdapter(new SectionsPagerAdapter(FM,EF,BF,CF));
        mViewPager.setOffscreenPageLimit(2);
        readLogFile();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
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
                                    Files.write(prefs.getString("commands", ""),cmdFile,Charsets.UTF_8);
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
                                    writeLogFile();
                                    Files.copy(new File(getFilesDir(), LOG_FILE),logFile);
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
                                    writeLogFile();
                                    Files.copy(new File(getFilesDir(), LOG_FILE),logFile);
                                    Files.write(prefs.getString("commands", ""),cmdFile,Charsets.UTF_8);
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
                                            pushOnly(Interval.newClearTimeMsg());
                                            pushOnly(Interval.newClearExpMess());
                                            readLogFile();
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
                                            pushOnly(Interval.newClearTimeMsg());
                                            pushOnly(Interval.newClearExpMess());
                                            readLogFile();
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
                                File logFile = new File(getFilesDir(), LOG_FILE);
                                if (logFile.exists()) {
                                    if (logFile.delete()) {
                                        pushOnly(Interval.newClearTimeMsg());
                                        pushOnly(Interval.newClearExpMess());
                                    } else
                                        Log.d("SquareDays", "Log clear failed!");
                                } else {
                                    pushOnly(Interval.newClearTimeMsg());
                                    pushOnly(Interval.newClearExpMess());
                                }
                                popAll();
                            }
                        })
                        .show();
                return true;
            }
            case R.id.menuItemHelp: {
                FragmentManager fm = getSupportFragmentManager();
                HelpFrag helpV = HelpFrag.newInstance();
                helpV.show(fm, "fragment_edit_name");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    class SectionsPagerAdapter extends FragmentPagerAdapter {
        Fragment F0;
        Fragment F1;
        Fragment F2;
        SectionsPagerAdapter(FragmentManager fm, Fragment F0, Fragment F1, Fragment F2) {
            super(fm);
            this.F0 = F0;
            this.F1 = F1;
            this.F2 = F2;
        }
        @Override
        public Fragment getItem(int pos) {
            switch (pos) {
                case 0:
                    return F0;
                case 1:
                    return F1;
                default:
                    return F2;
            }
        }
        @Override
        public int getCount() { return 3; }
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}