package com.q335.r49.squaredays;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarFrag<T extends TimeWin> extends Fragment {
    static String CODE_CAL = "cal";
    static String CODE_EXP = "exp";
    private String gClass;
    TouchView inputLayer;
    public interface OnFragmentInteractionListener {
        <T extends TimeWin> void setWin(CalendarFrag<T> frag, T disp, String code);
        void popAll();
    }
    private OnFragmentInteractionListener mListener;
    @Override
    public void onResume() {
        super.onResume();
        mListener.popAll();
    }
    void invalidate() { inputLayer.invalidate(); }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frame = inflater.inflate(R.layout.calendar,container,false);
        inputLayer = (TouchView) (frame.findViewById(R.id.drawing));
        inputLayer.setClass(gClass);
        Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.DAY_OF_WEEK,1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

        if (gClass.equals(CODE_CAL)) {
            TimeWin drawLayer = TimeWin.newWindowClass(cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            mListener.setWin(this, (T) drawLayer, gClass);
            inputLayer.setDisplay(drawLayer);
        } else {
            ExpenseWin drawLayer = ExpenseWin.newWindowClass(cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            mListener.setWin(this, (T) drawLayer, gClass);
            inputLayer.setDisplay(drawLayer);
        }
        return frame;
    }
    public CalendarFrag() { }
    private static final String CLASS_PARAM = "gClass";
    public static <T extends TimeWin> CalendarFrag<T> newInstance(String genericClass) {
        CalendarFrag<T> fragment = new CalendarFrag<>();
        Bundle args = new Bundle();
        args.putString(CLASS_PARAM, genericClass);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            gClass = getArguments().getString(CLASS_PARAM);
    }
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

