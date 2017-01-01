package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class CalendarFrag<T extends TimeWin> extends Fragment {
    static String CODE_CAL = "cal";
    static String CODE_EXP = "exp";
    String code;
    public interface OnFragmentInteractionListener {
        <T extends TimeWin> void setDisplay(CalendarFrag<T> frag, T disp, String code);
        void popAll();
        PaletteRing getPalette();
    }
    private OnFragmentInteractionListener mListener;
    @Override
    public void onResume() {
        super.onResume();
        mListener.popAll();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frame = inflater.inflate(R.layout.fragment_calendar,container,false);
        TouchView inputLayer = (TouchView) (frame.findViewById(R.id.drawing));
        Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.DAY_OF_WEEK,1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

        if (code.equals(CODE_CAL)) {
            TimeWin drawLayer = TimeWin.newWindowClass(inputLayer, cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            drawLayer.setDPIScaling(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
            mListener.setDisplay(this, (T) drawLayer, code);
            inputLayer.setDisplay(mListener.getPalette(), drawLayer);
        } else {
            ExpenseWin drawLayer = ExpenseWin.newWindowClass(inputLayer, cal.getTimeInMillis() / 1000L, 8f, 1.5f, -0.8f, -0.1f);
            drawLayer.setDPIScaling(Math.round(6 * (getContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT)));
            mListener.setDisplay(this, (T) drawLayer, code);
            inputLayer.setDisplay(mListener.getPalette(), drawLayer);
        }
        return frame;
    }
    public CalendarFrag() { }

    private static final String CODE_PARAM = "code";
    private String mParam1;
    public static <T extends TimeWin> CalendarFrag<T> newInstance(String param1) {
        CalendarFrag<T> fragment = new CalendarFrag<T>();
        Bundle args = new Bundle();
        args.putString(CODE_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            code = getArguments().getString(CODE_PARAM);
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

