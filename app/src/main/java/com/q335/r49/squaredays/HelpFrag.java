package com.q335.r49.squaredays;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class HelpFrag extends DialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }
    public HelpFrag() { }
    public static HelpFrag newInstance() { return new HelpFrag(); }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_help_scroller, container, false);
        ((WebView) mView.findViewById(R.id.webView)).loadUrl("file:///android_asset/help.html");
        return mView;
    }
    @Override
    public void onAttach(Context context) { super.onAttach(context); }
    @Override
    public void onDetach() { super.onDetach(); }
}
