package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonogramView extends TextView {
    public interface onProc {
        void onPress();
        void onDragMessage(float amount);
        void onRelease(float amount);
        void onCancel();
    }

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
    int type;
    String label;
    float rValDrag;
    onProc commands;
    private static float rExpDrag = 1f;
    private static float rTimeDrag = 6f;
    public void init(int color, String label, onProc obj) {
        this.label = label;
        this.commands = obj;
        mPaint.setColor(color);
        super.setText(label);
        if (this.type == Interval.tEXP) {
            rValDrag = rExpDrag;
            ActivePaint.setColor(Glob.invert(color));
            if (Color.red(color) > 200 && Color.green(color) > 200 & Color.blue(color) > 200)
                ActivePaint.setColor(0xFF888888);
            else
                ActivePaint.setColor(Glob.invert(color));
        } else {
            rValDrag = rTimeDrag;
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
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float eventX = event.getX();
                float eventY = event.getY();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        press();
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!pressed())
                            return false;
                        onDrag(eventX, eventY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!pressed())
                            return false;
                        onRelease(eventX, eventY);
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        unpress();
                        commands.onCancel();
                        return false;
                    default:
                        return true;
                }
            }
        });
    }
    private boolean hasExited;
    public void press() {
        state = PRESSED;
        invalidate();
        hasExited = false;
        escapeDistance = 0;
        commands.onPress();
    }
    public void unpress() {
        state = INACTIVE;
        setRotation(0);
        invalidate();
    }
    public boolean pressed() { return (state == PRESSED); }
    private float escapeDistance;

    public void onDrag(float x, float y) {
        escapeDistance = (float) Math.sqrt ((x - cx) * (x - cx) + (y - cy) * (y - cy));
        if (escapeDistance > border) {
            hasExited = true;
            setRotation((escapeDistance - border) * rRotDrag);
            escapeDistance = rValDrag * (escapeDistance - border);
        } else {
            setRotation(0);
            escapeDistance = 0;
        }
        commands.onDragMessage(escapeDistance);
    }
    public void onRelease(float x, float y) {
        escapeDistance = (float) Math.sqrt ((x - cx) * (x - cx) + (y - cy) * (y - cy));
        if (escapeDistance > border) {
            hasExited = true;
            unpress();
            escapeDistance =  rValDrag * (escapeDistance - border);
        } else {
            unpress();
            escapeDistance = 0;
        }
        commands.onRelease(escapeDistance);
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

    public String getStatusString() {
        if (type == Interval.tEXP)
            return escapeDistance == 0 ? label : " $" + escapeDistance;
        else
            return escapeDistance == 0 ?  label : " already  " + Integer.toString((int) (escapeDistance / 60)) + ":" + String.format(Locale.US, "%02d", (int) escapeDistance % 60)
                    + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (System.currentTimeMillis() / 1000L - 60 * (long) escapeDistance))) + ")";
    }
}
