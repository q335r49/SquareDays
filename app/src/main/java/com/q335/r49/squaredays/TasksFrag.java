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
import android.view.LayoutInflater;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import static android.content.Context.MODE_PRIVATE;
//TODO: Prettify statusbar display (eliminate altogether)
//TODO: Tasks toString and fromString; don't use GSON
public class TasksFrag extends Fragment {
    static final String prefsTasksKey = "tasks_1.0";
    SharedPreferences prefs;
    public interface OnFragmentInteractionListener {
        void pushProc(Interval log);
        void setBF(TasksFrag bf);
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
    private MonogramView endM;
    public OverlayView overlay;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.inflater = inflater;
        View view = this.inflater.inflate(R.layout.tasks,container, false);
        buttons = (FlexboxLayout) view.findViewById(R.id.GV);
        overlay = (OverlayView) view.findViewById(R.id.ovl);
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
    public void setActiveTask(Interval le) {
        if (le == null)
            setActiveTask(endM,-1L);
        else for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).label.equals(le.label) && tasks.get(i).type != Interval.tEXP) {
            setActiveTask((MonogramView) buttons.getChildAt(i).findViewById(R.id.monogram), le.command != Interval.cONGOING ? 0L : le.start);
            break;
        }
    }
    public static final float rRotSec = 360f/86400f;
    private long activeSince;
    public void setActiveTask(MonogramView v, long since) {
        if (activeView != null)
            activeView.deactivate();
        else if (endM != null && endM.isActive())
            endM.deactivate();
        activeView = v;
        activeSince = since;
        activeView.activate(activeSince < 0 ? 0f : (System.currentTimeMillis()/1000L - activeSince) * rRotSec);
    }
    public void clearActiveTask() {
        if (endM != null)
            endM.activate(0);
        if (activeView != null) {
            activeView.deactivate();
            activeView = null;
        }
    }
    public void refreshActiveTask() {
        if (activeView != null)
            activeView.activate(activeSince < 0 ? 0f : (System.currentTimeMillis()/1000L - activeSince) * rRotSec);
        else endM.activate(0);
    }
    private static GradientDrawable getRRect(int color) {
        GradientDrawable rrect = new GradientDrawable();
        rrect.setCornerRadius(Glob.rPxDp * 10f);
        rrect.setColor(color);
        return rrect;
    }
    private void makeView() {
        Collections.sort(tasks, new Comparator<Task>() {
            public int compare(Task t1, Task t2) {
                return t1.label.compareToIgnoreCase(t2.label);
            }
        });
        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams((int) (Glob.rPxDp * 30), (int) (Glob.rPxDp * 30));
            lp.minHeight    = (int) (Glob.rPxDp * 50f );
            lp.minWidth     = (int) (Glob.rPxDp * 100f );
            lp.maxHeight    = (int) (Glob.rPxDp * 200f);
            lp.maxWidth     = (int) (Glob.rPxDp * 200f);
            lp.flexGrow     = FlexboxLayout.LayoutParams.ALIGN_SELF_STRETCH;
            lp.flexShrink   = 0.2f;
        buttons.removeAllViews();
        for (int i = 0; i< tasks.size(); i++) {
            final Task task = tasks.get(i);
            final int ixF = i;
            final View child = inflater.inflate(R.layout.monogram, null);
            if (task.type == Interval.tCAL)
                child.setBackground(getRRect(task.color));
            buttons.addView(child,lp);
            final MonogramView mv = (MonogramView) child.findViewById(R.id.monogram);
            final Handler handler = new Handler();
            final Runnable mLongPressed = new Runnable() {
                public void run() {
                    mv.deactivate();
                    refreshActiveTask();
                    statusBar.setText(savedStatusText);
                    View promptView = inflater.inflate(R.layout.edit_task, null);
                    final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                    commentEntry.setText(task.label);
                    final View curColorV = promptView.findViewById(R.id.CurColor);
                    curColorV.setBackgroundColor(task.color);
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
            if (task.type == Interval.tEXP) {
                mv.init(task.color, task.label, new MonogramView.onTouch() {
                    @Override
                    public void actionDown() {
                        statusBar.setText(task.label);
                        handler.postDelayed(mLongPressed, 1200);
                    }
                    @Override
                    public void actionMove(float d) {
                        if (d > 0) {
                            handler.removeCallbacks(mLongPressed);
                            statusBar.setText(" $" + d);
                        } else if (mv.hasExited())
                            statusBar.setText("Cancel");
                    }

                    @Override
                    public void actionUp(float d) {
                        handler.removeCallbacks(mLongPressed);
                        if (d > 0)
                            mListener.pushProc(Interval.newExpense(task.color, System.currentTimeMillis() / 1000L, (long) d, 0, task.label));
                        else
                            statusBar.setText(savedStatusText);
                    }
                    @Override
                    public void actionCancel() { handler.removeCallbacks(mLongPressed); }
                });
            } else {
                mv.init(Glob.invert(task.color, 0.4f), task.label, new MonogramView.onTouch() {
                    @Override
                    public void actionDown() {
                        statusBar.setText(task.label);
                        handler.postDelayed(mLongPressed, 1200);
                    }
                    @Override
                    public void actionMove(float d) {
                        if (d > 0) {
                            handler.removeCallbacks(mLongPressed);
                            statusBar.setText(" already  " + Integer.toString((int) (d / 60)) + ":" + String.format(Locale.US, "%02d", (int) d % 60)
                                    + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (System.currentTimeMillis() / 1000L - 60 * (long) d))) + ")");
                        } else if (mv.hasExited())
                            statusBar.setText(task.label);
                    }
                    @Override
                    public void actionUp(float d) {
                        handler.removeCallbacks(mLongPressed);
                        if (d > 0 || !mv.hasExited()) {
                            long time = System.currentTimeMillis() / 1000L - (long) d * 60L;
                            mListener.pushProc(Interval.newOngoingTask(task.color, time, task.label));
                            setActiveTask(mv, time);
                        }
                        statusBar.setText(savedStatusText);
                    }
                    @Override
                    public void actionCancel() { handler.removeCallbacks(mLongPressed); }
                });
            }
        }
        final View endButton = inflater.inflate(R.layout.monogram, null);
            endButton.setBackground(getRRect(Glob.COLOR_END_BOX));
        buttons.addView(endButton,lp);
        endM = (MonogramView) endButton.findViewById(R.id.monogram);
        final Runnable mLongPressed = new Runnable() {
            public void run() {
                endM.deactivate();
                refreshActiveTask();
                statusBar.setText(savedStatusText);
                final View promptView = inflater.inflate(R.layout.edit_new_task, null);
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
                                tasks.add(new Task(commentEntry.getText().toString(), newColor, checkbox.isChecked()? Interval.tEXP : Interval.tCAL));
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
        final Handler handler = new Handler();
        endM.init(Glob.invert(Glob.COLOR_END_BOX,0.2f), "!", new MonogramView.onTouch() {
            @Override
            public void actionDown() { handler.postDelayed(mLongPressed, 1200); } //TODO: Make long-press delay static int;
            @Override
            public void actionMove(float d) {
                if (d > 0) {
                    handler.removeCallbacks(mLongPressed);
                    statusBar.setText(" already  " + Integer.toString((int) (d / 60)) + ":" + String.format(Locale.US, "%02d", (int) d % 60)
                            + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (System.currentTimeMillis() / 1000L - 60 * (long) d))) + ")");
                } else if (endM.hasExited())
                    statusBar.setText("Cancel");
            }
            @Override
            public void actionUp(float amount) {
                handler.removeCallbacks(mLongPressed);
                mListener.pushProc(Interval.newEndCommand(System.currentTimeMillis() / 1000L - (long) amount * 60));
                clearActiveTask();
                statusBar.setText(savedStatusText);
            }
            @Override
            public void actionCancel() { handler.removeCallbacks(mLongPressed); }
        });
        View addButton = inflater.inflate(R.layout.monogram, null);
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