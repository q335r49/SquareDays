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
import android.text.TextUtils;
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

//Prettify statusbar display
//TODO: Draw overlay circle / bar
//TODO: BUG Vertical delay still updating status bar
//TODO: Tasks toString and fromString; don't use GSON
public class TasksFrag extends Fragment {
    private static final String prefsTasksKey = "tasks_1.0";
    SharedPreferences prefs;
    public interface OnFragmentInteractionListener {
        void pushProc(Interval log);
        void setBF(TasksFrag bf);
        boolean onMenuItemClick(MenuItem item);
    }
    private class Task {
        String label;
        int color;
        int type;
        Task(String label, int color, int type) {
            this.label = label;
            this.color = color;
            this.type = type;
        }
    }
    private OnFragmentInteractionListener mListener;
    private FlexboxLayout buttons;
    private TextView statusBar;
        private String savedStatusText = "";
        void setSavedAB(Interval le) {
            savedStatusText = le != null ? le.label + " @" + (new SimpleDateFormat(" h:mm", Locale.US).format(new Date(le.start * 1000L))) : "";
            statusBar.setText(savedStatusText);
        }
    private List<Task> tasks = new ArrayList<>();
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
            statusBar.setEllipsize(TextUtils.TruncateAt.START);
            statusBar.setHorizontallyScrolling(false);
            statusBar.setSingleLine();
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
        prefs = context.getSharedPreferences(MainActivity.prefsName, MODE_PRIVATE);
        String jsonText = prefs.getString(prefsTasksKey, "");
        loadCommands(jsonText);
        mListener.setBF(this);
        return view;
    }
    public void loadCommands(String s) {
        if (s.isEmpty()) {
            tasks.add(new Task("1st task", Glob.parseColor("#2ecc71"),Interval.tCAL));
            tasks.add(new Task("2nd task", Glob.parseColor("#3498db"),Interval.tCAL));
            tasks.add(new Task("3rd task", Glob.parseColor("#9b59b6"),Interval.tCAL));
            tasks.add(new Task("4th task", Glob.parseColor("#34495e"),Interval.tCAL));
            tasks.add(new Task("5th task", Glob.parseColor("#16a085"),Interval.tCAL));
            tasks.add(new Task("6th task", Glob.parseColor("#16a085"),Interval.tCAL));
            tasks.add(new Task("Expense",  Glob.parseColor("#1abc9c"),Interval.tEXP));
        } else //TODO: try here
            tasks = new Gson().fromJson(s, new TypeToken<List<Task>>() { }.getType());
        for (Task t : tasks)
            Glob.palette.add(t.color);
        makeView();
    }
    public void setActiveTask(Interval le) { //TODO: Handle special case of expense name = task name
        if (le == null)
            setActiveTask(endButtonMonogram);
        else {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).label.equals(le.label)) {
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

    private static final float rExpDp = 1f / 6f;
    private static final float rMinsDp = 1f / 2f;
    private void makeView() {
        Collections.sort(tasks, new Comparator<Task>() {
            public int compare(Task t1, Task t2) {
                return t1.label.compareToIgnoreCase(t2.label);
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
        final float rDpPx = 1000f / (float) dpToPx(1000);
        final float rMinsPx = rDpPx * rMinsDp;
        final float rExpPx = rDpPx * rExpDp;
        buttons.removeAllViews();
        for (int i = 0; i< tasks.size(); i++) {
            final Task comF = tasks.get(i);
            final boolean isExpense = comF.type == Interval.tEXP;
            final int ixF = i;
            View child = inflater.inflate(R.layout.gv_list_item, null);
            buttons.addView(child,lp);
            final int bg_Norm = comF.color;
            final int bg_Press = Glob.darkenColor(bg_Norm,0.7f);
            MonogramView mv = (MonogramView) child.findViewById(R.id.text1);
                mv.setText(comF.label);
                if (isExpense)
                    mv.setStaticColor(bg_Norm);
                else
                    mv.setColor(bg_Norm);
            GradientDrawable rrect = new GradientDrawable();
                rrect.setCornerRadius(cornerRadius);
                rrect.setColor(isExpense? Glob.COLOR_PRIMARY_DARK : bg_Norm);
            child.setBackground(rrect);
            child.setOnTouchListener(new View.OnTouchListener() {
                private float actionDownX, actionDownY;
                private boolean hasRun, hasDragged;
                private final Handler handler = new Handler();
                private Runnable mLongPressed;
                private final int cancelZone = (int) (50f * rExpDp);
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isExpense) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                actionDownX = event.getX();
                                actionDownY = event.getY();
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                                final View finalView = v;
                                hasRun = hasDragged = false;
                                statusBar.setText(comF.label);
                                mLongPressed = new Runnable() {
                                    public void run() {
                                        hasRun = true;
                                        statusBar.setText(savedStatusText);
                                        ((GradientDrawable) finalView.getBackground()).setColor(Glob.COLOR_PRIMARY_DARK);
                                        View promptView = inflater.inflate(R.layout.prompts, null);
                                        final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                        commentEntry.setText(comF.label);
                                        final View curColorV = promptView.findViewById(R.id.CurColor);
                                        curColorV.setBackgroundColor(comF.color);
                                        final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                        final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
                                        final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
                                        final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);
                                        seekRed.setProgress(Color.red(curColor));
                                        seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                            @Override
                                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                                curColorV.setBackgroundColor(Color.rgb(progress, seekGreen.getProgress(), seekBlue.getProgress()));
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
                                                curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(), progress, seekBlue.getProgress()));
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
                                                curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), progress));
                                            }
                                            @Override
                                            public void onStartTrackingTouch(SeekBar seekBar) { }
                                            @Override
                                            public void onStopTrackingTouch(SeekBar seekBar) { }
                                        });

                                        final FlexboxLayout paletteView = (FlexboxLayout) promptView.findViewById(R.id.paletteBox);
                                        final int childCount = paletteView.getChildCount();
                                        for (int i = 0; i < childCount; i++) {
                                            View pv = paletteView.getChildAt(i);
                                            pv.setBackgroundColor(Glob.palette.get(i));
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
                                                        tasks.set(ixF, new Task(commentEntry.getText().toString(), newColor, Interval.tEXP));
                                                        Glob.palette.add(newColor);
                                                        makeView();
                                                    }
                                                })
                                                .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        tasks.remove(ixF);
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
                                handler.postDelayed(mLongPressed, 1200);
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                if (hasRun)
                                    return false;
                                int delay = (int) Math.abs((event.getX() - actionDownX) * rExpDp);
                                int duration = (int) Math.abs((event.getY() - actionDownY) * rExpDp);
                                delay = delay > cancelZone ? delay - cancelZone : 0;
                                duration = duration > cancelZone ? duration - cancelZone : 0;
                                if (duration != 0 || delay != 0) {
                                    if (!hasDragged) {
                                        handler.removeCallbacks(mLongPressed);
                                        hasDragged = true;
                                    }
                                    String abString = " $ " + (delay + duration);
                                    statusBar.setText(abString.isEmpty() ? comF.label : abString);
                                } else if (hasDragged)
                                    statusBar.setText("Cancel");
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (hasRun)
                                    return false;
                                ((GradientDrawable) v.getBackground()).setColor(Glob.COLOR_PRIMARY_DARK);
                                handler.removeCallbacks(mLongPressed);
                                delay = (int) Math.abs((event.getX() - actionDownX) * rExpDp);
                                duration = (int) Math.abs((event.getY() - actionDownY) * rExpDp);
                                delay = delay > cancelZone ? delay - cancelZone : 0;
                                duration = duration > cancelZone ? duration - cancelZone : 0;
                                if (delay != 0)
                                    mListener.pushProc(Interval.newExpense(comF.color, System.currentTimeMillis() / 1000L, delay, 0, comF.label));
                                else
                                    statusBar.setText(savedStatusText);
                                return false;
                            case MotionEvent.ACTION_CANCEL:
                                handler.removeCallbacks(mLongPressed);
                                ((GradientDrawable) v.getBackground()).setColor(Glob.COLOR_PRIMARY_DARK);
                                return false;
                            default:
                                return true;
                        }
                    } else {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                actionDownX = event.getX();
                                actionDownY = event.getY();
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                                final View finalView = v;
                                hasRun = hasDragged = false;
                                statusBar.setText(comF.label);
                                mLongPressed = new Runnable() {
                                    public void run() {
                                        hasRun = true;
                                        statusBar.setText(savedStatusText);
                                        ((GradientDrawable) finalView.getBackground()).setColor(bg_Norm);
                                        View promptView = inflater.inflate(R.layout.prompts, null);
                                        final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                        commentEntry.setText(comF.label);
                                        final View curColorV = promptView.findViewById(R.id.CurColor);
                                        curColorV.setBackgroundColor(comF.color);
                                        final int curColor = ((ColorDrawable) curColorV.getBackground()).getColor();
                                        final SeekBar seekRed = (SeekBar) promptView.findViewById(R.id.seekRed);
                                        final SeekBar seekGreen = (SeekBar) promptView.findViewById(R.id.seekGreen);
                                        final SeekBar seekBlue = (SeekBar) promptView.findViewById(R.id.seekBlue);
                                        seekRed.setProgress(Color.red(curColor));
                                        seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                            @Override
                                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                                curColorV.setBackgroundColor(Color.rgb(progress, seekGreen.getProgress(), seekBlue.getProgress()));
                                            }

                                            @Override
                                            public void onStartTrackingTouch(SeekBar seekBar) {
                                            }

                                            @Override
                                            public void onStopTrackingTouch(SeekBar seekBar) {
                                            }
                                        });
                                        seekGreen.setProgress(Color.green(curColor));
                                        seekGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                            @Override
                                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                                curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(), progress, seekBlue.getProgress()));
                                            }

                                            @Override
                                            public void onStartTrackingTouch(SeekBar seekBar) {
                                            }

                                            @Override
                                            public void onStopTrackingTouch(SeekBar seekBar) {
                                            }
                                        });
                                        seekBlue.setProgress(Color.blue(curColor));
                                        seekBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                            @Override
                                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                                curColorV.setBackgroundColor(Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), progress));
                                            }

                                            @Override
                                            public void onStartTrackingTouch(SeekBar seekBar) {
                                            }

                                            @Override
                                            public void onStopTrackingTouch(SeekBar seekBar) {
                                            }
                                        });

                                        final FlexboxLayout paletteView = (FlexboxLayout) promptView.findViewById(R.id.paletteBox);
                                        final int childCount = paletteView.getChildCount();
                                        for (int i = 0; i < childCount; i++) {
                                            View pv = paletteView.getChildAt(i);
                                            pv.setBackgroundColor(Glob.palette.get(i));
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
                                                        tasks.set(ixF, new Task(commentEntry.getText().toString(), newColor, Interval.tCAL));
                                                        Glob.palette.add(newColor);
                                                        makeView();
                                                    }
                                                })
                                                .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        tasks.remove(ixF);
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
                                handler.postDelayed(mLongPressed, 1200);
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                if (hasRun)
                                    return false;
                                int delay = (int) Math.abs((event.getX() - actionDownX) * rMinsDp);
                                int duration = (int) Math.abs((event.getY() - actionDownY) * rMinsDp);
                                delay = delay > cancelZone ? delay - cancelZone : 0;
                                duration = duration > cancelZone ? duration - cancelZone : 0;
                                if (duration != 0 || delay != 0) {
                                    if (!hasDragged) {
                                        handler.removeCallbacks(mLongPressed);
                                        hasDragged = true;
                                    }
                                    String abString = "";
                                    long now = System.currentTimeMillis() / 1000L;
                                    if (delay != 0)
                                        abString += " already  " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
                                                + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (now - 60 * delay))) + ")";
                                    if (duration != 0)
                                        abString += " for " + Integer.toString(duration / 60) + ":" + String.format(Locale.US, "%02d", duration % 60)
                                                + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (now - 60 * delay + 60 * duration))) + ")";
                                    statusBar.setText(abString.isEmpty() ? comF.label : abString);
                                } else if (hasDragged)
                                    statusBar.setText("Cancel");
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (hasRun)
                                    return false;
                                ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                                handler.removeCallbacks(mLongPressed);
                                delay = (int) Math.abs((event.getX() - actionDownX) * rMinsDp);
                                duration = (int) Math.abs((event.getY() - actionDownY) * rMinsDp);
                                delay = delay > cancelZone ? delay - cancelZone : 0;
                                duration = duration > cancelZone ? duration - cancelZone : 0;
                                if (delay != 0 || duration != 0 || !hasDragged) {
                                    if (duration == 0) {
                                        mListener.pushProc(Interval.newOngoingTask(comF.color, System.currentTimeMillis() / 1000L - delay * 60, comF.label));
                                        setActiveTask(v);
                                    } else
                                        mListener.pushProc(Interval.newCompleted(comF.color, System.currentTimeMillis() / 1000L - delay * 60, duration * 60, comF.label));
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
                }
            });
        }
        final int bg_Norm = Glob.COLOR_END_BOX;
        final int bg_Press = Glob.darkenColor(bg_Norm,0.7f);
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
            private final int cancelZone = (int) (50f * ratio_dp_px);
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
                                    pv.setBackgroundColor(Glob.palette.get(i));
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
                                                tasks.add(new Task(commentEntry.getText().toString(), newColor, checkbox.isEnabled()? Interval.tEXP : Interval.tCAL));
                                                Glob.palette.add(newColor);
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
                        delay = delay > cancelZone ? delay - cancelZone : 0;
                        duration = duration > cancelZone ? duration - cancelZone : 0;
                        if (duration != 0 || delay  != 0) {
                            if (!hasDragged) {
                                handler.removeCallbacks(mLongPressed);
                                hasDragged = true;
                            }
                            String abString = "";
                            long now = System.currentTimeMillis()/1000L;
                            if (duration != 0)
                                abString += " + COMMENT..";
                            if (delay != 0)
                                abString += " ended already " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
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
                        delay = delay > cancelZone ? delay - cancelZone : 0;
                        duration = duration > cancelZone ? duration - cancelZone : 0;
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
                                            mListener.pushProc(Interval.newCommentCmd(" " + commentEntry.getText().toString()));
                                            if (finalDelay != 0) {
                                                mListener.pushProc(Interval.newEndCommand(System.currentTimeMillis() / 1000L - finalDelay * 60));
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
                            mListener.pushProc(Interval.newEndCommand(System.currentTimeMillis()/1000L - delay * 60));
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

        prefs.edit().putString(prefsTasksKey, new Gson().toJson(tasks)).apply();
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