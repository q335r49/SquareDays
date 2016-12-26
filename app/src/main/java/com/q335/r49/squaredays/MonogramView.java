package com.q335.r49.squaredays;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class MonogramView extends TextView {
    static float minMaxSize = 1000f;
    private static final float[] NEGATIVE = {
            1,     0,     0,    0, 0, // red
            0,      -0.7f,     0,    0,  255, // green
            0,     0,       1,    0,  0, // blue
            0,     0,     0,      1,   0  // alpha
    };

    public boolean active = false;
    static Paint ActivePaint;
    static {
        ActivePaint = new Paint();
        ActivePaint.setStyle(Paint.Style.STROKE);
        ActivePaint.setTypeface(MainActivity.CommandFont);
        ActivePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        ActivePaint.setColor(0xFFFFFFFF);
        ActivePaint.setStyle(Paint.Style.STROKE);
    }
    public String Monogram;
    Paint mPaint;
    Rect bounds;
    int cHeight;
    int cWidth;
    float originX, originY;
    public MonogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(10f);
            mPaint.setTextSize(100f);
            mPaint.setTypeface(MainActivity.CommandFont);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        bounds = new Rect();
    }
    public void setColor(int color) {
        mPaint.setColor(color);
        mPaint.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setTextSize(minMaxSize);
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        originX = cWidth / 2f - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        originY = cHeight / 2f - fm.ascent - 2*fm.descent;
        canvas.drawText(Monogram, originX, originY, mPaint);
        if (active) {
            ActivePaint.setTextSize(minMaxSize);
            ActivePaint.setStrokeWidth(minMaxSize/40f);
            canvas.drawText(Monogram, originX, originY, ActivePaint);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        cHeight = h;
        cWidth = w;
        CharSequence seq = super.getText();
        Monogram = (seq == null || seq.length() < 1 ? " " : Character.toString(seq.charAt(0)));
        mPaint.getTextBounds(Monogram, 0, 1, bounds);

        float scalingFactorX =0.9f * (float) w / (float) bounds.width();
        float scalingFactorY =0.9f * (float) h / (float) bounds.height();
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
