package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.TextView;

public class MonogramView extends TextView {
    private static float rRotDrag = 0.6f;
    public static final int PRESSED = 5;
    public static final int ACTIVE = 6;
    public static final int INACTIVE = 0;
    int state;
    static float minMaxSize = 1000f;
    private static final float[] INV_FILTER = {
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
    public MonogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(10f);
            mPaint.setTextSize(100f);
            mPaint.setTypeface(Glob.CommandFont);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        ActivePaint = new Paint(mPaint);
        bounds = new Rect();
        state = INACTIVE;
    }
    public void init(int type, int color, String label) {
        mPaint.setColor(color);
        super.setText(label);
        if (type == Interval.tEXP) {
            ActivePaint.setColor(Glob.invert(color));
            if (Color.red(color) > 200 && Color.green(color) > 200 & Color.blue(color) > 200)
                ActivePaint.setColor(0xFF888888);
            else
                ActivePaint.setColor(Glob.invert(color));
        } else {
            ActivePaint.setColor(color);
            if (Color.red(color) > 200 && Color.green(color) > 200 & Color.blue(color) > 200)
                ActivePaint.setColor(0xFF888888);
            else {
                ActivePaint.setColorFilter(new ColorMatrixColorFilter(INV_FILTER));
            }
            mPaint.setColorFilter(new ColorMatrixColorFilter(NORM_FILTER));
            GradientDrawable rrect = new GradientDrawable();
            rrect.setCornerRadius(Glob.rPxDp * 10f);
            rrect.setColor(color);
            setBackground(rrect);
        }
    }
    private boolean hasExited;
    public void press() {
        state = PRESSED;
        invalidate();
        hasExited = false;
        escapeDistance = 0;
    }
    public void unpress() {
        state = INACTIVE;
        setRotation(0);
        invalidate();
    }
    public boolean pressed() { return (state == PRESSED); }
    private float escapeDistance;
    public float setDrag(float x, float y) {
        escapeDistance = (float) Math.sqrt ((x - cx) * (x - cx) + (y - cy) * (y - cy));
        if (escapeDistance > border) {
            hasExited = true;
            setRotation((escapeDistance - border) * rRotDrag);
            return escapeDistance - border;
        } else {
            setRotation(0);
            return 0;
        }
    }
    public float setRelease(float x, float y) {
        escapeDistance = (float) Math.sqrt ((x - cx) * (x - cx) + (y - cy) * (y - cy));
        if (escapeDistance > border) {
            hasExited = true;
            unpress();
            return escapeDistance - border;
        } else {
            unpress();
            return 0;
        }
    }
    public boolean hasExited() {
        return hasExited;
    }
    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setTextSize(minMaxSize);
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        originX = cWidth / 2f - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        originY = cHeight / 2f - fm.ascent - 2*fm.descent;
        if (state == INACTIVE)
            canvas.drawText(Monogram, originX, originY, mPaint);
        else {
            ActivePaint.setTextSize(minMaxSize);
            canvas.drawText(Monogram, originX, originY, ActivePaint);
        }
    }
    private float cx, cy, border;
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        cWidth = w;
        cHeight = h;
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        float size = mPaint.getTextSize()*Math.min(0.9f * (float) w / (float) bounds.width(),0.9f * (float) h / (float) bounds.height());
        if (size < minMaxSize)
            minMaxSize = size;
        cx = w / 2f;
        cy = h / 2f;
        border = (float) Math.sqrt(cx * cx + cy * cy);
    }
    @Override
    public void onTextChanged (CharSequence text, int start, int lengthBefore, int lengthAfter) {
        Monogram = text.length() > 0 ? Character.toString(text.charAt(0)) : " ";
    }
}
