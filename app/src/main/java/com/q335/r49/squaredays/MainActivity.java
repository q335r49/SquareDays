package com.q335.r49.squaredays;
import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity implements CommandsFrag.OnFragmentInteractionListener, CalendarFrag.OnFragmentInteractionListener, TaskEditor.OnFragmentInteractionListener {
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

    private Toolbar AB;
    int AB_curColor = 0;
    String AB_curText = "";
    int AB_savedColor = 0;
    String AB_savedText = "";

    final static int PROC_ENTRY = 9;
    final static int AB_SETCOLOR = 10;
    final static int AB_SETTEXT = 11;
    final static int AB_SAVESTATE = 13;
    final static int AB_RESTORESTATE = 14;
    public void procMess(int code, int arg) {
        switch (code) {
            case AB_SETCOLOR:
                AB.setBackgroundColor(arg);
                AB_curColor = arg;
                break;
            case AB_SAVESTATE:
                AB_savedColor = AB_curColor;
                AB_savedText = AB_curText;
                break;
            case AB_RESTORESTATE:
                AB.setBackgroundColor(AB_savedColor);
                AB_curColor = AB_savedColor;
                AB.setTitle(AB_savedText);
                AB_curText = AB_savedText;
                break;
            default:
                Log.d("SquareDays", "Bad Message: CODE-" + code + " ARG-" + arg);
        }
    }
    public void procMess(int code, String arg) {
        switch (code) {
            case PROC_ENTRY:
                GF.procMess(arg);
                break;
            case AB_SETTEXT:
                AB.setTitle(arg);
                AB_curText = arg;
                break;
            default:
                Log.d("SquareDays", "Bad Message: CODE " + code + " ARG " + arg);
        }
    }

    CalendarFrag GF;
        public void setGF(CalendarFrag GF) { this.GF = GF; }
    CommandsFrag BF;
        public void setBF(CommandsFrag BF) { this.BF = BF; }
    FragmentManager FM;
    TaskEditor TE;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CalendarFrag.COLOR_NO_TASK =  ResourcesCompat.getColor(getResources(), R.color.no_task, null);
        CalendarWin.COLOR_SCALE_TEXT = ResourcesCompat.getColor(getResources(), R.color.scale_text, null);
        CalendarWin.COLOR_GRID_BACKGROUND = ResourcesCompat.getColor(getResources(), R.color.grid_background, null);
        CalendarWin.COLOR_NOW_LINE = ResourcesCompat.getColor(getResources(), R.color.now_line, null);
        CalendarWin.COLOR_STATUS_BAR = ResourcesCompat.getColor(getResources(), R.color.status_bar, null);
        CalendarRect.COLOR_ERROR = ResourcesCompat.getColor(getResources(), R.color.error, null);
        CommandsFrag.COLOR_ERROR = ResourcesCompat.getColor(getResources(), R.color.error, null);
        CommandsFrag.COLOR_END_BOX = ResourcesCompat.getColor(getResources(), R.color.end_box, null);
        CommandsFrag.COLOR_NO_TASK =  ResourcesCompat.getColor(getResources(), R.color.no_task, null);
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
        TE = new TaskEditor();
        FM = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(FM,BF,GF);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
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
                                            GF.procMess(ScaleView.MESS_RELOAD_LOG);
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
                                            GF.procMess(ScaleView.MESS_RELOAD_LOG);
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
                                    GF.procMess(ScaleView.MESS_RELOAD_LOG);
                                    procMess(AB_SETCOLOR,0xFF192125);
                                    procMess(AB_SETTEXT,"Empty Log");
                                } else
                                    Log.d("SquareDays","Log clear failed!");
                            }
                        })
                        .show();
                return true;
            }
            case R.id.menuItemHelp: {
//                FragmentManager fm = getSupportFragmentManager();
//                TaskEditor newFragment = new TaskEditor();
//                newFragment.setFields("blah","red",palette);
//                newFragment.show(fm, "dialog");
                FragmentManager fm = getSupportFragmentManager();
                HelpScroller helpV = HelpScroller.newInstance("","");
                helpV.show(fm, "fragment_edit_name");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResult(int code, String comment, int color) {
        //XTODO: Respond to results
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