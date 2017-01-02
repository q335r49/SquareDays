package com.q335.r49.squaredays;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
//TODO: ** Differentiate expenses and tasks

public class TasksFrag extends Fragment {
    SharedPreferences prefs;
    public interface OnFragmentInteractionListener {
        void pushProc(cInterval log);
        void setBF(TasksFrag bf);
        boolean onMenuItemClick(MenuItem item);
    }
    private OnFragmentInteractionListener mListener;
    private FlexboxLayout buttons;
    private TextView statusBar;
        private String savedStatusText = "";
        void setSavedAB(cInterval le) {
            savedStatusText = le != null ? le.label + " @" + (new SimpleDateFormat(" h:mm", Locale.US).format(new Date(le.start * 1000L))) : "";
            statusBar.setText(savedStatusText);
        }
    private List<String[]> commands = new ArrayList<>();
    private final static int iCOMMENT = 0;
    private final static int iCOLOR = 1;
    private LayoutInflater inflater;
    private MonogramView activeView;
    private MonogramView endButtonMonogram;
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.inflater = inflater;
        View view = this.inflater.inflate(R.layout.fragment_commands,container, false);
        buttons = (FlexboxLayout) view.findViewById(R.id.GV);
        statusBar = (TextView) view.findViewById(R.id.status);
        final Context context = getActivity().getApplicationContext();
        view.findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v);
                popup.inflate(R.menu.menu_settings);
                popup.setOnMenuItemClickListener((PopupMenu.OnMenuItemClickListener) mListener);
                popup.show();
            }
        });
        prefs = context.getSharedPreferences("TrackerPrefs", MODE_PRIVATE);
        String jsonText = prefs.getString("commands", "");
        loadCommands(jsonText);
        mListener.setBF(this);
        return view;
    }
    public void loadCommands(String s) {
        if (s.isEmpty()) {
            commands.add(new String[]{"1st task", "#1abc9c"});
            commands.add(new String[]{"2nd task", "#2ecc71"});
            commands.add(new String[]{"3rd task", "#3498db"});
            commands.add(new String[]{"4th task", "#9b59b6"});
            commands.add(new String[]{"5th task", "#34495e"});
            commands.add(new String[]{"6th task", "#16a085"});
            commands.add(new String[]{"$Expense", "#16a085","E"});
        } else {
            Type listType = new TypeToken<List<String[]>>() { }.getType();
            commands = new Gson().fromJson(s, listType);
        }
        for (String[] sa : commands)
            Globals.palette.add(MainActivity.parseColor(sa[iCOLOR]));
        makeView();
    }
    public void setActiveTask(cInterval le) { //TODO: Handle special case of expense name  = task name
        if (le == null)
            setActiveTask(endButtonMonogram);
        else {
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i)[iCOMMENT].equals(le.label)) {
                    View activeV = buttons.getChildAt(i);
                    setActiveTask(activeV);
                    break;
                }
            }
        }
    }
    public void setActiveTask(View v) {
        if (activeView != null) {
            activeView.active = false;
            activeView.invalidate();
        } else if (endButtonMonogram != null &&  endButtonMonogram.active) {
            endButtonMonogram.active = false;
            endButtonMonogram.invalidate();
        }
        activeView =((MonogramView) v.findViewById(R.id.text1));
        activeView.active = true;
        activeView.invalidate();
    }
    public void clearActiveTask() {
        if (endButtonMonogram != null) {
            endButtonMonogram.active = true;
            endButtonMonogram.invalidate();
        }
        if (activeView != null) {
            activeView.active = false;
            activeView.invalidate();
            activeView = null;
        }
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
        int cornerRadius = dpToPx(10);

        buttons.removeAllViews();
        for (int i = 0; i<commands.size(); i++) {
            final String[] comF = commands.get(i);
            final int ixF = i;
            View child = inflater.inflate(R.layout.gv_list_item, null);
            buttons.addView(child,lp);
            final int bg_Norm = MainActivity.parseColor(comF[iCOLOR]);
            final int bg_Press = Globals.darkenColor(bg_Norm,0.7f);
            MonogramView mv = (MonogramView) child.findViewById(R.id.text1);
                mv.setText(comF[iCOMMENT]);
                mv.setColor(bg_Norm);
            GradientDrawable rrect = new GradientDrawable();
                rrect.setCornerRadius(cornerRadius);
                rrect.setColor(bg_Norm);
            child.setBackground(rrect);
            child.setOnTouchListener(new View.OnTouchListener() {
                private float actionDownX, actionDownY;
                private boolean hasRun, hasDragged;
                private final Handler handler = new Handler();
                private Runnable mLongPressed;
                private final float ratio_dp_px = 1000f /(float) dpToPx(1000);
                boolean isExpense = comF.length > 2 && comF[2].equals("E");
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isExpense) {    //TODO: ** Allow for delay (e.g., yesterday)
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                actionDownX = event.getX();
                                actionDownY = event.getY();
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                                final View finalView = v;
                                hasRun = hasDragged = false;
                                statusBar.setText(comF[iCOMMENT]);
                                mLongPressed = new Runnable() {
                                    public void run() { //TODO: ** Distinguish expense
                                        hasRun = true;
                                        statusBar.setText(savedStatusText);
                                        ((GradientDrawable) finalView.getBackground()).setColor(bg_Norm);
                                        View promptView = inflater.inflate(R.layout.prompts, null);
                                        final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                        commentEntry.setText(comF[iCOMMENT]);
                                        final View curColorV = promptView.findViewById(R.id.CurColor);
                                        curColorV.setBackgroundColor(MainActivity.parseColor(comF[iCOLOR]));
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
                                            pv.setBackgroundColor(Globals.palette.get(i));
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
                                                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        int newColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                                        commands.set(ixF, new String[]{commentEntry.getText().toString(), String.format("#%06X", (0xFFFFFF & newColor)), "0", ""});
                                                        Globals.palette.add(newColor);
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
                                                })
                                                .create().show();
                                    }
                                };
                                handler.postDelayed(mLongPressed,1200);
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                if (hasRun)
                                    return false;
                                int delay = (int) Math.abs((event.getX() - actionDownX)*ratio_dp_px);
                                int duration = (int) Math.abs((event.getY() - actionDownY)*ratio_dp_px);
                                delay = delay > 50 ? delay - 50 : 0;
                                duration = duration > 50 ? duration - 50 : 0;
                                if (duration != 0 || delay != 0) {
                                    if (!hasDragged) {
                                        handler.removeCallbacks(mLongPressed);
                                        hasDragged = true;
                                    }
                                    String abString = " $ " + (delay + duration);
                                    statusBar.setText(abString.isEmpty()? comF[iCOMMENT] : abString);
                                } else if (hasDragged)
                                    statusBar.setText("Cancel");
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (hasRun)
                                    return false;
                                ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                                handler.removeCallbacks(mLongPressed);
                                delay = (int) Math.abs((event.getX() - actionDownX) * ratio_dp_px);
                                duration = (int) Math.abs((event.getY() - actionDownY) * ratio_dp_px);
                                delay = delay > 50 ? delay - 50 : 0;
                                duration = duration > 50 ? duration - 50 : 0;
                                if (delay != 0)
                                    mListener.pushProc(cInterval.newExpense(MainActivity.parseColor(comF[iCOLOR]),System.currentTimeMillis()/1000L, delay,comF[iCOMMENT]));
                                else
                                    statusBar.setText(savedStatusText);
                                return false;
                            case MotionEvent.ACTION_CANCEL:
                                handler.removeCallbacks(mLongPressed);
                                ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                                return false;
                            default:
                                return true;
                        }
                    }
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            actionDownX = event.getX();
                            actionDownY = event.getY();
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                            final View finalView = v;
                            hasRun = hasDragged = false;
                            statusBar.setText(comF[iCOMMENT]);
                            mLongPressed = new Runnable() {
                                public void run() {
                                    hasRun = true;
                                    statusBar.setText(savedStatusText);
                                    ((GradientDrawable) finalView.getBackground()).setColor(bg_Norm);
                                    View promptView = inflater.inflate(R.layout.prompts, null);
                                    final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                        commentEntry.setText(comF[iCOMMENT]);
                                    final View curColorV = promptView.findViewById(R.id.CurColor);
                                        curColorV.setBackgroundColor(MainActivity.parseColor(comF[iCOLOR]));
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
                                        pv.setBackgroundColor(Globals.palette.get(i));
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
                                            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    int newColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                                    commands.set(ixF, new String[]{commentEntry.getText().toString(), String.format("#%06X", (0xFFFFFF & newColor)), "0", ""});
                                                    Globals.palette.add(newColor);
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
                                            })
                                            .create().show();
                                }
                            };
                            handler.postDelayed(mLongPressed,1200);
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (hasRun)
                                return false;
                            int delay = (int) Math.abs((event.getX() - actionDownX)*ratio_dp_px);
                            int duration = (int) Math.abs((event.getY() - actionDownY)*ratio_dp_px);
                            delay = delay > 50 ? delay - 50 : 0;
                            duration = duration > 50 ? duration - 50 : 0;
                            if (duration != 0 || delay != 0) {
                                if (!hasDragged) {
                                    handler.removeCallbacks(mLongPressed);
                                    hasDragged = true;
                                }
                                String abString = "..";
                                long now = System.currentTimeMillis()/1000L;
                                if (delay != 0)
                                    abString += " already  " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
                                            + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay))) + ")";
                                if (duration != 0)
                                    abString += " for " + Integer.toString(duration / 60) + ":" + String.format(Locale.US, "%02d", duration % 60)
                                            + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay + 60 * duration))) + ")";
                                statusBar.setText(abString.isEmpty()? comF[iCOMMENT] : abString);
                            } else if (hasDragged)
                                statusBar.setText("Cancel");
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (hasRun)
                                return false;
                            ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                            handler.removeCallbacks(mLongPressed);
                            delay = (int) Math.abs((event.getX() - actionDownX) * ratio_dp_px);
                            duration = (int) Math.abs((event.getY() - actionDownY) * ratio_dp_px);
                            delay = delay > 50 ? delay - 50 : 0;
                            duration = duration > 50 ? duration - 50 : 0;
                            if (delay != 0 || duration != 0 || !hasDragged) {
                                if (duration == 0) {
                                    mListener.pushProc(cInterval.newOngoingTask(MainActivity.parseColor(comF[iCOLOR]), System.currentTimeMillis() / 1000L - delay * 60, comF[iCOMMENT]));
                                    setActiveTask(v);
                                } else
                                    mListener.pushProc(cInterval.newCompletedTask(MainActivity.parseColor(comF[iCOLOR]),System.currentTimeMillis()/1000L - delay * 60,duration * 60,comF[iCOMMENT]));
                            } else
                                statusBar.setText(savedStatusText);
                            return false;
                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(mLongPressed);
                            ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                            return false;
                        default:
                            return true;
                    }
                }
            });
        }
        final int bg_Norm = Globals.COLOR_END_BOX;
        final int bg_Press = Globals.darkenColor(bg_Norm,0.7f);
        final View endButton = inflater.inflate(R.layout.gv_list_item, null);
        buttons.addView(endButton,lp);
        GradientDrawable rrect = new GradientDrawable();
            rrect.setCornerRadius(cornerRadius);
            rrect.setColor(bg_Norm);
        endButton.setBackground(rrect);
        endButtonMonogram = (MonogramView) endButton.findViewById(R.id.text1);
            endButtonMonogram.setColor(bg_Norm);
            endButtonMonogram.setText("!");
        endButton.setOnTouchListener(new View.OnTouchListener() {
            private float actionDownX, actionDownY;
            private boolean hasRun, hasDragged;
            private final Handler handler = new Handler();
            private Runnable mLongPressed;
            private final float ratio_dp_px = 1000f /(float) dpToPx(1000);
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        actionDownX = event.getX();
                        actionDownY = event.getY();
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                        hasRun = hasDragged = false;
                        mLongPressed = new Runnable() {
                            public void run() {
                                hasRun = true;
                                statusBar.setText(savedStatusText);
                                ((GradientDrawable) endButton.getBackground()).setColor(bg_Norm);
                                final View promptView = inflater.inflate(R.layout.prompt_new_task, null);
                                final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                final View curColorV = promptView.findViewById(R.id.CurColor);
                                final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
                                final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
                                final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);
                                final CheckBox checkbox = (CheckBox) promptView.findViewById(R.id.expenseCheck);
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
                                    pv.setBackgroundColor(Globals.palette.get(i));
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
                                                commands.add(new String[]{commentEntry.getText().toString(), String.format("#%06X", (0xFFFFFF & newColor)), checkbox.isEnabled()? "E" : ""});
                                                Globals.palette.add(newColor);
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
                        if (hasRun)
                            return false;
                        int delay = (int) Math.abs((event.getX() - actionDownX)*ratio_dp_px);
                        int duration = (int) Math.abs((event.getY() - actionDownY)*ratio_dp_px);
                        delay = delay > 50 ? delay - 50 : 0;
                        duration = duration > 50 ? duration - 50 : 0;
                        if (duration != 0 || delay  != 0) {
                            if (!hasDragged) {
                                handler.removeCallbacks(mLongPressed);
                                hasDragged = true;
                            }
                            String abString = "..";
                            long now = System.currentTimeMillis()/1000L;
                            if (duration != 0)
                                abString += " + COMMENT..";
                            if (delay != 0)
                                abString += " ended already  " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
                                        + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay))) + ")";
                            statusBar.setText(abString.isEmpty()? "End Task" : abString);
                        } else if (hasDragged)
                            statusBar.setText("Cancel");
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (hasRun)
                            return false;
                        ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                        handler.removeCallbacks(mLongPressed);
                        delay = (int) Math.abs((event.getX() - actionDownX) * ratio_dp_px);
                        duration = (int) Math.abs((event.getY() - actionDownY) * ratio_dp_px);
                        delay = delay > 50 ? delay - 50 : 0;
                        duration = duration > 50 ? duration - 50 : 0;
                        statusBar.setText(savedStatusText);
                        if (duration != 0) {
                            LayoutInflater inflater = LayoutInflater.from(getContext());
                            View commentView = inflater.inflate(R.layout.comment_prompt, null);

                            final EditText commentEntry = (EditText) commentView.findViewById(R.id.edit1);
                            final int finalDelay = delay;
                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                            alertDialogBuilder.setView(commentView);
                            alertDialogBuilder
                                    .setCancelable(true)
                                    .setTitle("Comment:")
                                    .setPositiveButton("Add label", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            mListener.pushProc(cInterval.newCommentCmd(" " + commentEntry.getText().toString()));
                                            if (finalDelay != 0) {
                                                mListener.pushProc(cInterval.newEndCommand(System.currentTimeMillis() / 1000L - finalDelay * 60));
                                                clearActiveTask();
                                            }
                                        }
                                    })
                                    .setNegativeButton("(Cancel)", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                                    .create().show();
                        } else if (delay != 0 || !hasDragged) {
                            mListener.pushProc(cInterval.newEndCommand(System.currentTimeMillis()/1000L - delay * 60));
                            clearActiveTask();
                        }
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(mLongPressed);
                        ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                        return false;
                    default:
                        return true;
                }
            }
        });

        View addButton = inflater.inflate(R.layout.gv_list_item, null);
        buttons.addView(addButton,lp);

        prefs.edit().putString("commands", new Gson().toJson(commands)).apply();
    }
    public TasksFrag() { }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
            mListener = (OnFragmentInteractionListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}