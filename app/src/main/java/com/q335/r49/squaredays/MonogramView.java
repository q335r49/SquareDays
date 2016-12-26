package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class MonogramView extends TextView {
    static float minMaxSize = 1000f;
    public String Monogram;
    Paint mPaint;
    Rect bounds;
    int cHeight;
    int cWidth;
    float originX, originY;
    public MonogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
            mPaint.setTypeface(Typeface.DEFAULT);
            mPaint.setTextSize(100f);
            mPaint.setTypeface(MainActivity.CommandFont);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        bounds = new Rect();
    }
    public void setColor(int color) {
        mPaint.setColor(color);
        mPaint.setColorFilter(new LightingColorFilter(0xFF444444,0X00222222));
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setTextSize(minMaxSize);
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        originX = cWidth / 2f - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        originY = cHeight / 2f - fm.ascent - 2*fm.descent;
        canvas.drawText(Monogram, originX, originY, mPaint);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        cHeight = h;
        cWidth = w;
        CharSequence seq = super.getText();
        Monogram = (seq == null || seq.length() < 1 ? " " : Character.toString(seq.charAt(0)));
        mPaint.getTextBounds(Monogram, 0, 1, bounds);

        float scalingFactorX = (float) w / (float) bounds.width();
        float scalingFactorY = (float) h / (float) bounds.height();
        float size = mPaint.getTextSize()*Math.min(scalingFactorX,scalingFactorY);
        if (size < minMaxSize)
            minMaxSize = size;
    }

    public void setBackgroundColor(int color) {

    }

    public int getBackgroundCOlor() {
        return 0;
    }
}
