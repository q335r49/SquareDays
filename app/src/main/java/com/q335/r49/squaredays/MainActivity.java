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
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity implements CommandsFrag.OnFragmentInteractionListener, CalendarFrag.OnFragmentInteractionListener {
    private static final String LOG_FILE = "log.txt";
    private static final String COMMANDS_FILE = "commands.json";
    private static final String EXT_STORAGE_DIR = "tracker";
    Context context;
    SharedPreferences sprefs;
    //TODO: Think of a better label for the "swipe" area
    //TODO: Bring back "labels" and sliders, such as a "status bar" for on-click?


    public void processNewLogEntry(String E) {
        GF.procMess(E);
    }
    private int bgColor = Color.parseColor("darkgrey");
        public void receiveCurBG(int c) { bgColor = c; }
        public int getCurBG() { return bgColor; }

    CalendarFrag GF;
    CommandsFrag BF;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        sprefs = getApplicationContext().getSharedPreferences("TrackerPrefs", MODE_PRIVATE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BF = new CommandsFrag();
        GF = new CalendarFrag();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),BF,GF);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
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
                                    Log.e("tracker:",e.toString());
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
                                    Log.e("tracker:",e.toString());
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
                                    Log.e("tracker:",e.toString());
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
                                        Log.e("tracker:",e.toString());
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
                                            Log.e("tracker:",e.toString());
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
                                        Log.e("tracker:",e.toString());
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
                                            Log.e("tracker:",e.toString());
                                            Toast.makeText(context, "Import failed. Does this app have storage access? (Settings > Apps > tracker > Permissions)", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            })
                            .show();
                }
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