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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

//TODO: Change background color deeper into the past
public class CommandsFrag extends Fragment {
    static int COLOR_ERROR;
    static int COLOR_END_BOX;

    SharedPreferences sprefs;
    private OnFragmentInteractionListener mListener;
    private FlexboxLayout gridV;
    private List<String[]> commands = new ArrayList<>();
    private final static int COMMENT_IX = 0;
    private final static int COLOR_IX = 1;
    private LayoutInflater inflater;
    Context context;

    private PaletteRing palette;

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private MonogramView activeView;

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
        palette = mListener.getPalette();
        if (s.isEmpty()) {
            commands.add(new String[]{"1st task", "#1abc9c", "0", ""});
            commands.add(new String[]{"2nd task", "#2ecc71", "0", ""});
            commands.add(new String[]{"3rd task", "#3498db", "0", ""});
            commands.add(new String[]{"4th task", "#9b59b6", "0", ""});
            commands.add(new String[]{"5th task", "#34495e", "0", ""});
            commands.add(new String[]{"6th task", "#16a085", "0", ""});
        } else {
            Type listType = new TypeToken<List<String[]>>() { }.getType();
            commands = new Gson().fromJson(s, listType);
        }
        palette.add(new String[] {"#1abc9c", "#2ecc71", "#3498db", "#9b59b6", "#34495e", "#16a085",
                "#27ae60", "#2980b9", "#8e44ad", "#2c3e50", "#f1c40f",
                "#e67e22", "#e74c3c", "#ecf0f1", "#95a5a6", "#f39c12",
                "#d35400", "#c0392b", "#bdc3c7", "#7f8c8d", "#3b5999",
                "#21759b", "#dd4b39", "#bd081c"});
        for (String[] sa : commands)
            palette.add(MainActivity.parseColor(sa[COLOR_IX]));
        makeView();
    }

    public static int darkenColor(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.min(Math.round(Color.red(color) * factor),255),
                Math.min(Math.round(Color.green(color) * factor),255),
                Math.min(Math.round(Color.blue(color) * factor),255));
    }

    public void setActiveTask(logEntry le) {
        if (le == null)
            setActiveTask(endButtonMonogram);
        else {
            Log.d("SquareDays", "Setting active task [logEntry]");
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i)[COMMENT_IX].equals(le.comment)) {
                    View activeV = gridV.getChildAt(i);
                    setActiveTask(activeV);
                    break;
                }
            }
        }
    }
    public void setActiveTask(View v) {
        Log.d("SquareDays","Setting active task [View]");
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

    MonogramView endButtonMonogram;
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

        gridV.removeAllViews();
        final LayoutInflater inflaterF = inflater;
        for (int ix = 0; ix<commands.size(); ix++) {
            final String[] comF = commands.get(ix);
            final int ixF = ix;
            View child = inflaterF.inflate(R.layout.gv_list_item, null);
                gridV.addView(child,lp);
            TextView label = (TextView) (child.findViewById(R.id.text1));
                label.setText(comF[COMMENT_IX]);
            final int bg_Norm = MainActivity.parseColor(comF[COLOR_IX]);
            final int bg_Press = CommandsFrag.darkenColor(bg_Norm,0.7f);

            GradientDrawable rrect = new GradientDrawable();
            rrect.setCornerRadius(cornerRadius);
            rrect.setColor(bg_Norm);
            child.setBackground(rrect);
            MonogramView mv = (MonogramView) child.findViewById(R.id.text1);
            mv.setColor(bg_Norm);

            child.setOnTouchListener(new View.OnTouchListener() {
                private float actionDownX, actionDownY;
                private boolean has_run, has_dragged;
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
                            final View finalView = v;
                            has_run = has_dragged = false;
                            mListener.setABState(comF[COMMENT_IX]);
                            mLongPressed = new Runnable() {
                                public void run() {
                                    has_run = true;
                                    mListener.restoreABState();
                                    ((GradientDrawable) finalView.getBackground()).setColor(bg_Norm);

                                    Context context = getContext();
                                    View promptView = inflater.inflate(R.layout.prompts, null);
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                                    alertDialogBuilder.setView(promptView);

                                    final EditText commentEntry = (EditText) promptView.findViewById(R.id.commentInput);
                                        commentEntry.setText(comF[COMMENT_IX]);
                                    final View curColorV = promptView.findViewById(R.id.CurColor);
                                    curColorV.setBackgroundColor(MainActivity.parseColor(comF[COLOR_IX]));

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
                            int delay = (int) Math.abs((event.getX() - actionDownX)*ratio_dp_px);
                            int duration = (int) Math.abs((event.getY() - actionDownY)*ratio_dp_px);
                            delay = delay > 50 ? delay - 50 : 0;
                            duration = duration > 50 ? duration - 50 : 0;
                            if (duration != 0 || delay != 0) {
                                if (!has_dragged) {
                                    handler.removeCallbacks(mLongPressed);
                                    has_dragged = true;
                                }
                                String abString = "..";
                                long now = System.currentTimeMillis()/1000L;
                                if (delay != 0)
                                    abString += " already  " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
                                            + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay))) + ")";
                                if (duration != 0)
                                    abString += " for " + Integer.toString(duration / 60) + ":" + String.format(Locale.US, "%02d", duration % 60)
                                            + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay + 60 * duration))) + ")";
                                mListener.setABState(abString.isEmpty()? comF[COMMENT_IX] : abString);
                            } else if (has_dragged)
                                mListener.setABState("Cancel");
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (has_run)
                                return false;
                            ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                            handler.removeCallbacks(mLongPressed);
                            delay = (int) Math.abs((event.getX() - actionDownX) * ratio_dp_px);
                            duration = (int) Math.abs((event.getY() - actionDownY) * ratio_dp_px);
                            delay = delay > 50 ? delay - 50 : 0;
                            duration = duration > 50 ? duration - 50 : 0;
                            if (delay != 0 || duration != 0 || !has_dragged) {
                                if (duration == 0) {
                                    mListener.pushTask(logEntry.newOngoingTask(MainActivity.parseColor(comF[COLOR_IX]), System.currentTimeMillis() / 1000L - delay * 60, comF[COMMENT_IX]));
                                    setActiveTask(v);
                                } else
                                    mListener.pushTask(logEntry.newCompletedTask(MainActivity.parseColor(comF[COLOR_IX]),System.currentTimeMillis()/1000L - delay * 60,duration * 60,comF[COMMENT_IX]));
                            } else
                                mListener.restoreABState();
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

        final int bg_Norm = COLOR_END_BOX;
        final int bg_Press = darkenColor(bg_Norm,0.7f);
        final View endButton = inflaterF.inflate(R.layout.gv_list_item, null);

        GradientDrawable rrect = new GradientDrawable();
        rrect.setCornerRadius(cornerRadius);
        rrect.setColor(bg_Norm);
        endButton.setBackground(rrect);
        endButtonMonogram = (MonogramView) endButton.findViewById(R.id.text1);
        endButtonMonogram.setColor(bg_Norm);

        TextView label = (TextView) (endButton.findViewById(R.id.text1));
        label.setText("!");
        gridV.addView(endButton,lp);
        endButton.setOnTouchListener(new View.OnTouchListener() {
            private float offset_0x;
            private float offset_0y;
            private final Handler handler = new Handler();
            private Runnable mLongPressed;
            boolean has_run = false;
            boolean has_dragged;
            private final float ratio_dp_px = 1000f /(float) dpToPx(1000);

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        offset_0x = event.getX();
                        offset_0y = event.getY();
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        ((GradientDrawable) v.getBackground()).setColor(bg_Press);
                        has_run = false;
                        has_dragged = false;
                        mLongPressed = new Runnable() {
                            public void run() {
                                has_run = true;
                                mListener.restoreABState();
                                ((GradientDrawable) endButton.getBackground()).setColor(bg_Norm);
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
                        int delay = (int) Math.abs((event.getX() - offset_0x)*ratio_dp_px);
                        int duration = (int) Math.abs((event.getY() - offset_0y)*ratio_dp_px);
                        delay = delay > 50 ? delay - 50 : 0;
                        duration = duration > 50 ? duration - 50 : 0;
                        if (duration != 0 || delay  != 0) {
                            if (!has_dragged) {
                                handler.removeCallbacks(mLongPressed);
                                has_dragged = true;
                            }
                            String abString = "..";
                            long now = System.currentTimeMillis()/1000L;
                            if (duration != 0)
                                abString += " + COMMENT..";
                            if (delay != 0)
                                abString += " ended already  " + Integer.toString(delay / 60) + ":" + String.format(Locale.US, "%02d", delay % 60)
                                        + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L*(now - 60 * delay))) + ")";
                            mListener.setABState(abString.isEmpty()? "End Task" : abString);
                        } else if (has_dragged)
                            mListener.setABState("Cancel");
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (has_run)
                            return false;
                        ((GradientDrawable) v.getBackground()).setColor(bg_Norm);
                        handler.removeCallbacks(mLongPressed);
                        delay = (int) Math.abs((event.getX() - offset_0x) * ratio_dp_px);
                        duration = (int) Math.abs((event.getY() - offset_0y) * ratio_dp_px);
                        delay = delay > 50 ? delay - 50 : 0;
                        duration = duration > 50 ? duration - 50 : 0;
                        mListener.restoreABState();
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
                                    .setPositiveButton("Add comment", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            mListener.pushTask(logEntry.newCommentCmd(" " + commentEntry.getText().toString()));
                                            if (finalDelay != 0) {
                                                mListener.pushTask(logEntry.newEndCommand(System.currentTimeMillis() / 1000L - finalDelay * 60));
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
                        } else if (delay != 0 || !has_dragged) {
                            mListener.pushTask(logEntry.newEndCommand(System.currentTimeMillis()/1000L - delay * 60));
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

        View addButton = inflaterF.inflate(R.layout.gv_list_item, null);
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
        void pushTask(logEntry log);
        void restoreABState();
        void setABState(String comment);
        void setBF(CommandsFrag bf);
        PaletteRing getPalette();
    }
}