package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

public class MonogramView extends TextView {
    static float minMaxSize = 1000f;
    private static final float[] FILTER = {
            -1, 0, 0, 0,255, // red
            0, -1, 0, 0,255, // green
            0,  0,-1, 0,255, // blue
            0,  0, 0, 1,  0  // alpha
    };
    private static final float[] NORM_FILTER = {
            -0.25f,0,     0,     0,64, // red
            0,     -0.25f,0,     0,64, // green
            0,     0,     -0.25f,0,64, // blue
            0,     0,     0,     1,0   // alpha
    };
    public String Monogram;
    int cWidth, cHeight;
    float originX, originY;
    Paint mPaint, ActivePaint;
    Rect bounds;
    public boolean active;
    public MonogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(10f);
            mPaint.setTextSize(100f);
            mPaint.setTypeface(Glob.CommandFont);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        ActivePaint = new Paint();
            ActivePaint.setStyle(Paint.Style.FILL);
            ActivePaint.setTypeface(Glob.CommandFont);
            ActivePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        bounds = new Rect();
    }
    public void setColor(int color) {
        ActivePaint.setColor(color);
        if (Color.red(color) > 200 && Color.green(color) > 200 & Color.blue(color) > 200)
            ActivePaint.setColor(0xFF888888);
        else
            ActivePaint.setColorFilter(new ColorMatrixColorFilter(FILTER));
        mPaint.setColor(color);
        mPaint.setColorFilter(new ColorMatrixColorFilter(NORM_FILTER));
    }
    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setTextSize(minMaxSize);
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        originX = cWidth / 2f - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        originY = cHeight / 2f - fm.ascent - 2*fm.descent;
        if (!active)
            canvas.drawText(Monogram, originX, originY, mPaint);
        else {
            ActivePaint.setTextSize(minMaxSize);
            ActivePaint.setStrokeWidth(minMaxSize/10f);
            canvas.drawText(Monogram, originX, originY, ActivePaint);
        }
    }
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        cWidth = w;
        cHeight = h;
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        float size = mPaint.getTextSize()*Math.min(0.9f * (float) w / (float) bounds.width(),0.9f * (float) h / (float) bounds.height());
        if (size < minMaxSize)
            minMaxSize = size;
    }
    @Override
    public void onTextChanged (CharSequence text, int start, int lengthBefore, int lengthAfter) {
        Monogram = text.length() > 0 ? Character.toString(text.charAt(0)) : " ";
    }
}
