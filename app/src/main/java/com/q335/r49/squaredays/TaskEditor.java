package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import com.google.android.flexbox.FlexboxLayout;

public class TaskEditor extends DialogFragment {


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    EditText commentField;
    View colorField;
    FlexboxLayout paletteField;
    SeekBar seekRed, seekGreen, seekBlue;
    Button removeButton, updateButton, cancelButton;
    String comment;
    String color;
    PaletteRing palette;
    void setFields(String comment, String color, PaletteRing palette) {
        this.comment = comment;
        this.color = color;
        this.palette = palette;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View thisView = inflater.inflate(R.layout.fragment_task_editor, container, false);
        commentField = (EditText) thisView.findViewById(R.id.commentInput);
        colorField = thisView.findViewById(R.id.CurColor);
        paletteField = (FlexboxLayout) thisView.findViewById(R.id.paletteBox);
        commentField.setText(comment);
        int curColor;
        try { curColor = Color.parseColor(color); }
        catch (Exception e) { curColor = CalendarRect.COLOR_ERROR; }
        colorField.setBackgroundColor(curColor);

        final int childCount = paletteField.getChildCount();
        for (int i = 0; i < childCount ; i++) {
            View v = paletteField.getChildAt(i);
            v.setBackgroundColor(palette.get(i));
            final int fbg = ((ColorDrawable) v.getBackground()).getColor();
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekRed.setProgress(Color.red(fbg));
                    seekGreen.setProgress(Color.green(fbg));
                    seekBlue.setProgress(Color.blue(fbg));
                    colorField.setBackgroundColor(fbg);
                }
            });
        }
        seekRed = (SeekBar) thisView.findViewById(R.id.seekRed);
        seekGreen = (SeekBar) thisView.findViewById(R.id.seekGreen);
        seekBlue = (SeekBar) thisView.findViewById(R.id.seekBlue);
        seekRed.setProgress(Color.red(curColor));
        seekGreen.setProgress(Color.green(curColor));
        seekBlue.setProgress(Color.blue(curColor));
            seekRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        colorField.setBackgroundColor(Color.rgb(progress,seekGreen.getProgress(),seekBlue.getProgress()));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            seekGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        colorField.setBackgroundColor(Color.rgb(seekRed.getProgress(),progress,seekBlue.getProgress()));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            seekBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        colorField.setBackgroundColor(Color.rgb(seekRed.getProgress(),seekGreen.getProgress(),progress));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        removeButton = (Button) thisView.findViewById(R.id.removeButton);
        updateButton = (Button) thisView.findViewById(R.id.updateButton);
        cancelButton = (Button) thisView.findViewById(R.id.cancelButton);
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { mListener.onResult(CODE_REMOVE,"",0); dismiss();}
            });
            updateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { mListener.onResult(CODE_UPDATE,commentField.getText().toString(),((ColorDrawable) colorField.getBackground()).getColor());  dismiss();}
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { mListener.onResult(CODE_CANCEL,"",0);  dismiss();}
            });
        return thisView;
    }


    private static final String ARG_PARAM1 = "comment";
    private static final String ARG_PARAM2 = "color";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    public TaskEditor() { }
    public static TaskEditor newInstance(String param1, String param2) {
        TaskEditor fragment = new TaskEditor();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    final static int CODE_REMOVE = 12;
    final static int CODE_UPDATE = 13;
    final static int CODE_CANCEL = 14;
    public interface OnFragmentInteractionListener {
        void onResult(int code, String comment, int color);
    }
}
