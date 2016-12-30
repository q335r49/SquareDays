package com.q335.r49.squaredays;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ExpenseFrag extends Fragment {
    PaletteRing palette;
    private ScaleView inputLayer;
    public interface OnFragmentInteractionListener {
        void setEF(ExpenseFrag cf);
        void popAll();
        PaletteRing getPalette();
    }
    private OnFragmentInteractionListener mListener;
    logEntry procTask(logEntry le) { return inputLayer.procTask(le); }

    List<String> getWritableShapes() {return inputLayer.getWritableShapes(); }
    boolean activityCreated;
    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        activityCreated = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        mListener.popAll();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frame = inflater.inflate(R.layout.fragment_calendar,container,false);
        inputLayer = (ScaleView) (frame.findViewById(R.id.drawing));
        mListener.setEF(this);
        palette = mListener.getPalette();
        inputLayer.loadCalendarView(palette);
        return frame;
    }
    public ExpenseFrag() { }
    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }
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
