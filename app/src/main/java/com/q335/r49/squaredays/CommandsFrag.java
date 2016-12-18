package com.q335.r49.squaredays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class CommandsFrag extends Fragment {
    public static final int PROC_ENTRY = 9;
    public static final int AB_SETCOLOR = 10;
    public static final int AB_SETTEXT = 11;
    public static final int AB_SAVESTATE = 13;
    public static final int AB_RESTORESTATE = 14;

    SharedPreferences sprefs;
    private OnFragmentInteractionListener mListener;
    private FlexboxLayout gridV;
    private List<String[]> commands = new ArrayList<>();
    private static final String LOG_FILE = "log.txt";
    private final static int COMMENT_IX = 0;
    private final static int COLOR_IX = 1;
    private LayoutInflater inflater;
    Context context;

    private final static int PALETTE_LEN = 24;
    class PaletteRing {
        private int length;
        private int size;
        private int[] ring;
        private int pos;
        PaletteRing(int length) {
            this.length = length;
            ring = new int[length];
            pos = 0;
            size = 0;
        }
        public void add(int c) {
            for (int i = 0; i < length; i++) {
                if (ring[(pos + i) % length] ==  c) {
                    if (i != 0) {
                        for (int j = pos + i; j > pos; j--)
                            ring[j % length] = ring[(j - 1) % length];
                        size = size > 0 ? size - 1 : 0;
                    }
                    break;
                }
            }
            ring[pos] = c;
            pos = (pos + 1) % length;
            size++;
        }
        int get(int i) {
            return ring[(pos - 1 - i + 10*length) % length];
        }
    }
    private PaletteRing palette = new PaletteRing(PALETTE_LEN);

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        context = getActivity().getApplicationContext();
    }
    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = inf;
        View view = inflater.inflate(R.layout.fragment_commands,container, false);
        gridV = (FlexboxLayout) view.findViewById(R.id.GV);
        sprefs = context.getSharedPreferences("TrackerPrefs", MODE_PRIVATE);
        String jsonText = sprefs.getString("commands", "");
        loadCommands(jsonText);
        mListener.setBF(this);
        return view;
    }
    public void loadCommands(String s) {
        if (s.isEmpty()) {
            commands.add(new String[]{"01 This is a task...", "red", "0", ""});
            commands.add(new String[]{"02 Long press a task to edit...", "blue", "0", ""});
        } else {
            Type listType = new TypeToken<List<String[]>>() { }.getType();
            commands = new Gson().fromJson(s, listType);
        }
        palette.add(Color.parseColor("#B21F35"));
        palette.add(Color.parseColor("#D82735"));
        palette.add(Color.parseColor("#FF7435"));
        palette.add(Color.parseColor("#FFA135"));
        palette.add(Color.parseColor("#FFCB35"));
        palette.add(Color.parseColor("#FFF735"));
        palette.add(Color.parseColor("#00753A"));
        palette.add(Color.parseColor("#009E47"));
        palette.add(Color.parseColor("#16DD36"));
        palette.add(Color.parseColor("#0052A5"));
        palette.add(Color.parseColor("#0079E7"));
        palette.add(Color.parseColor("#06A9FC"));
        palette.add(Color.parseColor("#681E7E"));
        palette.add(Color.parseColor("#7D3CB5"));
        palette.add(Color.parseColor("#BD7AF6"));
        palette.add(Color.parseColor("#F44336"));
        palette.add(Color.parseColor("#E91E63"));
        palette.add(Color.parseColor("#9C27B0"));
        palette.add(Color.parseColor("#673AB7"));
        palette.add(Color.parseColor("#3F51B5"));
        palette.add(Color.parseColor("#2196F3"));
        palette.add(Color.parseColor("#FF9800"));
        palette.add(Color.parseColor("#FFEB3B"));
        palette.add(Color.parseColor("#CDDC39"));
        for (String[] sa : commands) {
            int color;
            try {
                color = Color.parseColor(sa[COLOR_IX]);
                palette.add(color);
            } catch (Exception e) {
                Log.e("SquareDays","Bad color " + sa[COLOR_IX]);
            }
        }
        makeView();
    }

    public static int darkenColor(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.min(Math.round(Color.red(color) * factor),255),
                Math.min(Math.round(Color.green(color) * factor),255),
                Math.min(Math.round(Color.blue(color) * factor),255));
    }
    private static float[] RR_radii = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
    public static ShapeDrawable getRoundRect(int color) {
        ShapeDrawable bgshape = new ShapeDrawable();
        bgshape.setShape(new RoundRectShape(RR_radii,null,null));
        bgshape.getPaint().setColor(color);
        return bgshape;
    }

    private void makeView() {
        Collections.sort(commands, new Comparator<String[]>() {
            public int compare(String[] s1, String[] s2) {
                return s1[0].compareToIgnoreCase(s2[0]);
            }
        });
        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(dpToPx(30),dpToPx(30));
        lp.minHeight = dpToPx(50);
        lp.minWidth = dpToPx(80);
        lp.maxHeight = dpToPx(200);
        lp.maxWidth = dpToPx(200);
        lp.flexGrow=FlexboxLayout.LayoutParams.ALIGN_SELF_STRETCH;
        lp.flexShrink=0.2f;

        gridV.removeAllViews();
        final LayoutInflater inflaterF = inflater;
        for (int ix = 0; ix<commands.size(); ix++) {
            final String[] comF = commands.get(ix);
            final int ixF = ix;
            View child = inflaterF.inflate(R.layout.gv_list_item, null);
            TextView label = (TextView) (child.findViewById(R.id.text1));
            label.setText(comF[COMMENT_IX]);

            gridV.addView(child,lp);

            int testColor=0xFFDDDDDD;
            try {
                testColor = Color.parseColor(comF[COLOR_IX]);
            } catch (IllegalArgumentException e) {
                Log.e("SquareDays",e.toString());
            }
            final int bg_Norm = testColor;
            final int bg_Press = CommandsFrag.darkenColor(bg_Norm,0.7f);
            child.setBackground(getRoundRect(bg_Norm));
            //TODO: Bring mLongPressed "outside"; Simplify setBackground; use single instance of color chooser
            child.setOnTouchListener(new View.OnTouchListener() {
                private Rect viewBounds;
                private float offset_0x, offset_0y;
                private boolean has_run, action_cancelled, offset_mode;
                private final Handler handler = new Handler();
                private Runnable mLongPressed;
                private final float ratio_dp_px = 1000f /(float) dpToPx(1000);
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            mListener.procMess(AB_SAVESTATE,0);
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            v.setBackground(getRoundRect(bg_Press));
                            viewBounds = new Rect(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());
                            final View finalView = v;
                            has_run = action_cancelled = offset_mode = false;
                            mLongPressed = new Runnable() {
                                public void run() {
                                    has_run = true;
                                    action_cancelled = false;
                                    finalView.setBackground(getRoundRect(bg_Norm));
                                    Context context = getContext();
                                    LayoutInflater layoutInflater = LayoutInflater.from(context);
                                    View promptView = layoutInflater.inflate(R.layout.prompts, null);
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                                    alertDialogBuilder.setView(promptView);

                                    final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                    commentEntry.setText(comF[COMMENT_IX]);
                                    final View curColorV = promptView.findViewById(R.id.CurColor);
                                    try {
                                        curColorV.setBackgroundColor(Color.parseColor(comF[COLOR_IX]));
                                    } catch (Exception e) {
                                        curColorV.setBackgroundColor(Color.parseColor("darkgrey"));
                                    }

                                    final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                    final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
                                    final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
                                    final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);

                                    seekRed.setProgress(Color.red(curColor));
                                    seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                        @Override
                                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                            curColorV.setBackgroundColor(Color.rgb(progress,seekGreen.getProgress(),seekBlue.getProgress()));
                                        }
                                        @Override
                                        public void onStartTrackingTouch(SeekBar seekBar) { }
                                        @Override
                                        public void onStopTrackingTouch(SeekBar seekBar) { }
                                    });
                                    seekGreen.setProgress(Color.green(curColor));
                                    seekGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                        @Override
                                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                            curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),progress,seekBlue.getProgress()));
                                        }
                                        @Override
                                        public void onStartTrackingTouch(SeekBar seekBar) { }
                                        @Override
                                        public void onStopTrackingTouch(SeekBar seekBar) { }
                                    });
                                    seekBlue.setProgress(Color.blue(curColor));
                                    seekBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                        @Override
                                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                            curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),seekGreen.getProgress(),progress));
                                        }
                                        @Override
                                        public void onStartTrackingTouch(SeekBar seekBar) { }
                                        @Override
                                        public void onStopTrackingTouch(SeekBar seekBar) { }
                                    });

                                    final FlexboxLayout paletteView = (FlexboxLayout) promptView.findViewById(R.id.paletteBox);
                                    final int childCount = paletteView.getChildCount();
                                    for (int i = 0; i < childCount ; i++) {
                                        View v = paletteView.getChildAt(i);
                                        v.setBackgroundColor(palette.get(i));
                                        final int bg = ((ColorDrawable) v.getBackground()).getColor();
                                        v.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                seekRed.setProgress(Color.red(bg));
                                                seekGreen.setProgress(Color.green(bg));
                                                seekBlue.setProgress(Color.blue(bg));
                                                curColorV.setBackgroundColor(bg);
                                            }
                                        });
                                    }
                                    alertDialogBuilder
                                            .setCancelable(true)
                                            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    int newColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                                    commands.set(ixF, new String[]{commentEntry.getText().toString(), String.format("#%06X", (0xFFFFFF & newColor)), "0", ""});
                                                    palette.add(newColor);
                                                    makeView();
                                                }
                                            })
                                            .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    commands.remove(ixF);
                                                    makeView();
                                                }
                                            })
                                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });
                                    alertDialogBuilder.create().show();
                                }
                            };
                            handler.postDelayed(mLongPressed,1200);
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (has_run)
                                return false;
                            if(offset_mode || !viewBounds.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())){
                                handler.removeCallbacks(mLongPressed);
                                if (!offset_mode) {
                                    offset_mode = true;
                                    offset_0x = event.getX();
                                    offset_0y = event.getY();
                                } else {
                                    int delay = (int) Math.abs((event.getX() - offset_0x)*ratio_dp_px);
                                    int duration = (int) Math.abs((event.getY() - offset_0y)*ratio_dp_px);
                                    delay = delay > 50 ? delay - 50 : 0;
                                    duration = duration > 50 ? duration - 50 : 0;
                                    String abString = "";
                                    if (duration == 0 && delay  == 0) { //Canceled
                                        action_cancelled = true;
                                        mListener.procMess(AB_RESTORESTATE,0);
                                    } else {
                                        action_cancelled = false;
                                        mListener.procMess(AB_SETCOLOR,bg_Norm);
                                        abString = "..";
                                        long now = System.currentTimeMillis()/1000L;
                                        if (delay != 0)
                                            abString += " already  " + Integer.toString(delay / 60) + ":" + String.format("%02d", delay % 60)
                                                    + " (" + new SimpleDateFormat("h:mm a").format(new Date(1000L*(now - 60 * delay))) + ")";
                                        if (duration != 0)
                                            abString += " for " + Integer.toString(duration / 60) + ":" + String.format("%02d", duration % 60)
                                                    + " (" + new SimpleDateFormat("h:mm a").format(new Date(1000L*(now - 60 * delay + 60 * duration))) + ")";
                                        mListener.procMess(AB_SETTEXT, abString.isEmpty()? comF[COMMENT_IX] : abString);
                                    }
                                }
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (has_run)
                                return false;
                            v.setBackground(getRoundRect(bg_Norm));
                            if (action_cancelled)
                                return false;
                            handler.removeCallbacks(mLongPressed);
                            int delay = 0;
                            int duration = 0;
                            if (offset_mode) {
                                delay = (int) Math.abs((event.getX() - offset_0x) * ratio_dp_px);
                                duration = (int) Math.abs((event.getY() - offset_0y) * ratio_dp_px);
                                delay = delay > 50 ? delay - 50 : 0;
                                duration = duration > 50 ? duration - 50 : 0;
                            }
                            long now = System.currentTimeMillis()/1000L;
                            if (duration == 0) {
                                mListener.procMess(AB_SETCOLOR, bg_Norm);
                                mListener.procMess(AB_SETTEXT, comF[COMMENT_IX] + " @" + new SimpleDateFormat("h:mm a").format(new Date(1000L * (now - 60 * delay))));
                            } else {
                                Toast.makeText(context, comF[COMMENT_IX]
                                        + "\n" + new SimpleDateFormat("h:mm a").format(new Date(1000L*(now - 60 * delay))) + " > " + new SimpleDateFormat("h:mm a").format(new Date(1000L*(now - 60 * delay + 60 * duration)))
                                        + "\n" + Integer.toString(duration / 60) + ":" + String.format("%02d", duration % 60) + " min", Toast.LENGTH_LONG).show();
                                mListener.procMess(AB_RESTORESTATE,0);
                            }
                            String entry = Long.toString(System.currentTimeMillis() / 1000) + ">" + (new Date()).toString() + ">" + comF[COLOR_IX] + ">" + (-delay * 60) + ">" + (duration == 0 ? "" : Integer.toString((-delay + duration) * 60)) + ">" + comF[COMMENT_IX];

                            File internalFile = new File(context.getFilesDir(), LOG_FILE);
                            try {
                                FileOutputStream out = new FileOutputStream(internalFile, true);
                                out.write(entry.getBytes());
                                out.write(System.getProperty("line.separator").getBytes());
                                out.close();
                            } catch (Exception e) {
                                Log.e("SquareDays",e.toString());
                                Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
                            }
                            mListener.procMess(PROC_ENTRY, entry);
                            return false;
                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(mLongPressed);
                            v.setBackground(getRoundRect(bg_Norm));
                            return false;
                        default:
                            return true;
                    }
                }
            });
        }

        final int bg_Press = 0xFFDDDDDD;
        final int bg_Norm = 0xFFAAAAAA;
        View endButton = inflaterF.inflate(R.layout.gv_list_item, null);
        endButton.setBackgroundColor(bg_Norm);
        TextView label = (TextView) (endButton.findViewById(R.id.text1));
        label.setText("End Task");
        gridV.addView(endButton,lp);
        endButton.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBounds;
            private boolean offset_mode = false;
            private float offset_0x;
            private float offset_0y;
            private final Handler handler = new Handler();
            private Runnable mLongPressed;
            boolean has_run = false;
            boolean action_cancelled = false;
            int prevBGColor;
            CharSequence prevBarString;
            float ratio_dp_px;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        ratio_dp_px = 1000f /(float) dpToPx(1000);
                        mListener.procMess(AB_SAVESTATE,0);
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        v.setBackground(getRoundRect(bg_Press));
                        viewBounds = new Rect(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());
                        offset_mode = false;
                        final View finalView = v;
                        has_run = false;
                        action_cancelled = false;
                        mLongPressed = new Runnable() {
                            public void run() {
                                has_run = true;
                                action_cancelled = false;

                                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                                View promptView = layoutInflater.inflate(R.layout.prompts, null);
                                final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                final View curColorV = promptView.findViewById(R.id.CurColor);

                                final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
                                final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
                                final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);

                                seekRed.setProgress(Color.red(curColor));
                                seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        curColorV.setBackgroundColor(Color.rgb(progress,seekGreen.getProgress(),seekBlue.getProgress()));
                                    }
                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) { }
                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) { }
                                });
                                seekGreen.setProgress(Color.green(curColor));
                                seekGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),progress,seekBlue.getProgress()));
                                    }
                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) { }
                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) { }
                                });
                                seekBlue.setProgress(Color.blue(curColor));
                                seekBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(),seekGreen.getProgress(),progress));
                                    }
                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) { }
                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) { }
                                });

                                final FlexboxLayout paletteView = (FlexboxLayout) promptView.findViewById(R.id.paletteBox);
                                final int childCount = paletteView.getChildCount();
                                for (int i = 0; i < childCount ; i++) {
                                    View pv = paletteView.getChildAt(i);
                                    pv.setBackgroundColor(palette.get(i));
                                    final int bg = ((ColorDrawable) pv.getBackground()).getColor();
                                    pv.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            seekRed.setProgress(Color.red(bg));
                                            seekGreen.setProgress(Color.green(bg));
                                            seekBlue.setProgress(Color.blue(bg));
                                            curColorV.setBackgroundColor(bg);
                                        }
                                    });
                                }
                                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                                alertDialogBuilder.setView(promptView);
                                alertDialogBuilder
                                        .setCancelable(true)
                                        .setPositiveButton("Add Entry", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                int newColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                                commands.add(new String[]{commentEntry.getText().toString(), String.format("#%06X", (0xFFFFFF & newColor)), "0", ""});
                                                palette.add(newColor);
                                                makeView();
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        })
                                        .create().show();

                            }
                        };
                        handler.postDelayed(mLongPressed,1200);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (has_run)
                            return false;
                        if(offset_mode || !viewBounds.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            handler.removeCallbacks(mLongPressed);
                            if (!offset_mode) {
                                offset_mode = true;
                                offset_0x = event.getX();
                                offset_0y = event.getY();
                            } else {
                                int delay = (int) Math.abs((event.getX() - offset_0x)*ratio_dp_px);
                                int duration = (int) Math.abs((event.getY() - offset_0y)*ratio_dp_px);
                                delay = delay > 50 ? delay - 50 : 0;
                                duration = duration > 50 ? duration - 50 : 0;
                                String abString = "";
                                if (duration == 0 && delay  == 0) {
                                    action_cancelled = true;
                                    mListener.procMess(AB_RESTORESTATE,0);
                                } else {
                                    action_cancelled = false;
                                    mListener.procMess(AB_SETCOLOR,bg_Norm);
                                    abString = "..";
                                    long now = System.currentTimeMillis()/1000L;
                                    if (duration != 0)
                                        abString += " + COMMENT..";
                                    if (delay != 0)
                                        abString += " ended already  " + Integer.toString(delay / 60) + ":" + String.format("%02d", delay % 60)
                                                + " (" + new SimpleDateFormat("h:mm a").format(new Date(1000L*(now - 60 * delay))) + ")";
                                    mListener.procMess(AB_SETTEXT,abString.isEmpty()? "End Task" : abString);
                                }
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (has_run)
                            return false;
                        v.setBackground(getRoundRect(bg_Norm));
                        if (action_cancelled)
                            return false;
                        handler.removeCallbacks(mLongPressed);
                        int delay = 0;
                        int duration = 0;
                        if (offset_mode) {
                            delay = (int) Math.abs((event.getX() - offset_0x) * ratio_dp_px);
                            duration = (int) Math.abs((event.getY() - offset_0y) * ratio_dp_px);
                            delay = delay > 50 ? delay - 50 : 0;
                            duration = duration > 50 ? duration - 50 : 0;
                        }
                        long now = System.currentTimeMillis()/1000L;
                        mListener.procMess(AB_RESTORESTATE,0);
                        if (duration != 0) {


                            LayoutInflater inflater = LayoutInflater.from(getContext());
                            View commentView = inflater.inflate(R.layout.comment_prompt, null);
                            final EditText commentEntry = (EditText) commentView.findViewById(R.id.edit1);
                            final int finalDelay = delay;

                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                            alertDialogBuilder.setView(commentView);
                            alertDialogBuilder
                                    .setCancelable(true)
                                    .setPositiveButton("Add comment", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            String entry = Long.toString(System.currentTimeMillis() / 1000) + ">" + (new Date()).toString() + ">>>" + (finalDelay == 0 ? "0" : Integer.toString(-finalDelay * 60)) + ">" + commentEntry.getText().toString();
                                            File internalFile = new File(context.getFilesDir(), LOG_FILE);
                                            //TODO: Don't keep on opening the file? Ie, a text buffer?
                                            try {
                                                FileOutputStream out = new FileOutputStream(internalFile, true);
                                                out.write(entry.getBytes());
                                                out.write(System.getProperty("line.separator").getBytes());
                                                out.close();
                                            } catch (Exception e) {
                                                Log.e("SquareDays", e.toString());
                                                Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
                                            }
                                            mListener.procMess(AB_SETCOLOR,0xFF444444);
                                            mListener.procMess(AB_SETTEXT, "No task");
                                            mListener.procMess(PROC_ENTRY, entry);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                                    .create().show();
                        } else {
                            String entry = Long.toString(System.currentTimeMillis() / 1000) + ">" + (new Date()).toString() + ">>>" + (delay == 0 ? "0" : Integer.toString(-delay * 60)) + ">";
                            File internalFile = new File(context.getFilesDir(), LOG_FILE);
                            //TODO: Don't keep on opening the file? Ie, a text buffer?
                            try {
                                FileOutputStream out = new FileOutputStream(internalFile, true);
                                out.write(entry.getBytes());
                                out.write(System.getProperty("line.separator").getBytes());
                                out.close();
                            } catch (Exception e) {
                                Log.e("SquareDays", e.toString());
                                Toast.makeText(context, "Cannot write to internal storage", Toast.LENGTH_LONG).show();
                            }
                            mListener.procMess(AB_SETCOLOR,0x444444);
                            mListener.procMess(AB_SETTEXT, "No task");
                            mListener.procMess(PROC_ENTRY, entry);
                        }
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(mLongPressed);
                        v.setBackground(getRoundRect(bg_Norm));
                        return false;
                    default:
                        return true;
                }
            }
        });

        View addButton = inflaterF.inflate(R.layout.gv_list_item, null);
        label = (TextView) (addButton.findViewById(R.id.text1));
        label.setText("Swipe right here for calendar");
        gridV.addView(addButton,lp);

        sprefs.edit().putString("commands", new Gson().toJson(commands)).apply();
    }

    public CommandsFrag() { } // Required empty public constructor
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    public static CommandsFrag newInstance(String param1, String param2) {
        CommandsFrag fragment = new CommandsFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void procMess(int code, int arg);
        void procMess(int code, String arg);
        void setBF(CommandsFrag bf);
    }
}