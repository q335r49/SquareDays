package com.q335.r49.squaredays;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarFrag<T extends TimeWin> extends Fragment {
    static String CODE_CAL = "cal";
    static String CODE_EXP = "exp";
    private String gClass;
    public interface OnFragmentInteractionListener {
        <T extends TimeWin> void setWin(CalendarFrag<T> frag, T disp, String code);
        void popAll();
    }
    private OnFragmentInteractionListener mListener;
    @Override
    public void onResume() {
        super.onResume();
        //TODO: invalidate?
        mListener.popAll();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frame = inflater.inflate(R.layout.fragment_calendar,container,false);
        TouchView inputLayer = (TouchView) (frame.findViewById(R.id.drawing));
        inputLayer.setClass(gClass);
        Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.DAY_OF_WEEK,1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

        if (gClass.equals(CODE_CAL)) {
            TimeWin drawLayer = TimeWin.newWindowClass(inputLayer, cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            drawLayer.setDPIScaling(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
            mListener.setWin(this, (T) drawLayer, gClass);
            inputLayer.setDisplay(drawLayer);
        } else {
            ExpenseWin drawLayer = ExpenseWin.newWindowClass(inputLayer, cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            drawLayer.setDPIScaling(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
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

