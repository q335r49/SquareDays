package com.q335.r49.squaredays;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class HelpScroller extends DialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }
    public HelpScroller() { }
    public static HelpScroller newInstance() { return new HelpScroller(); }
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
