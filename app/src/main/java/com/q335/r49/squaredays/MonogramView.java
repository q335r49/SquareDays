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

//TODO: Rotation should not show "negative space" -- set View background color
public class MonogramView extends TextView implements View.OnTouchListener {
    private onTouch commands;
        public interface onTouch {
            void actionDown();
            void actionMove(float amount);
            void actionUp(float amount);
            void actionCancel();
        }
    private float rValDrag;
        private static float rExpDrag = 1f;
        private static float rTimeDrag = 6f;
    private static float rRotDrag = 0.6f;
    private int state;
        public static final int PRESSED = 5;
        public static final int ACTIVE = 6;
        public static final int INACTIVE = 0;
    private static float minMaxSize = 1000f;
    private static final float[] NORM_FILTER = {
            -0.25f,0,     0,     0,64, // red
            0,     -0.25f,0,     0,64, // green
            0,     0,     -0.25f,0,64, // blue
            0,     0,     0,     1,0   // alpha
    };
    private float centerX, centerY, border;
    float originX, originY;
    Paint mPaint, ActivePaint;
    Rect bounds;
    private boolean hasExited;
    private float dragDist;
    int type;
    String label;
    private String Monogram;

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
    public void init(int type, int color, String label, onTouch obj) {
        this.type = type;
        this.label = label;
        this.commands = obj;
        mPaint.setColor(color);
        super.setText(label);
        if (Color.red(color) > 200 && Color.green(color) > 200 & Color.blue(color) > 200)
            ActivePaint.setColor(0xFF888888);
        else
            ActivePaint.setColor(Glob.invert(color));
        if (this.type == Interval.tEXP)
            rValDrag = rExpDrag;
        else {
            rValDrag = rTimeDrag;
            mPaint.setColorFilter(new ColorMatrixColorFilter(NORM_FILTER));
                GradientDrawable rrect = new GradientDrawable();
                rrect.setCornerRadius(Glob.rPxDp * 10f);
                rrect.setColor(color);
            setBackground(rrect);
        }
        setOnTouchListener(this);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                state = PRESSED;
                invalidate();
                hasExited = false;
                dragDist = 0;
                v.getParent().requestDisallowInterceptTouchEvent(true);
                commands.actionDown();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (state == PRESSED) {
                    dragDist = (float) Math.sqrt ((eventX - centerX) * (eventX - centerX) + (eventY - centerY) * (eventY - centerY));
                    if (dragDist > border) {
                        hasExited = true;
                        setRotation((dragDist - border) * rRotDrag);
                        dragDist = rValDrag * (dragDist - border);
                    } else {
                        setRotation(0);
                        dragDist = 0;
                    }
                    commands.actionMove(dragDist);
                    return true;
                } else
                    return false;
            case MotionEvent.ACTION_UP:
                if (state == PRESSED) {
                    dragDist = (float) Math.sqrt ((eventX - centerX) * (eventX - centerX) + (eventY - centerY) * (eventY - centerY));
                    state = INACTIVE;
                    invalidate();
                    setRotation(0);
                    if (dragDist > border) {
                        hasExited = true;
                        dragDist =  rValDrag * (dragDist - border);
                    } else
                        dragDist = 0;
                    commands.actionUp(dragDist);
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                state = INACTIVE;
                setRotation(0);
                invalidate();
                commands.actionCancel();
                return false;
            default:
                return true;
        }
    }
    public void press() {
        state = PRESSED;
        invalidate();
    }
    public void unpress() {
        state = INACTIVE;
        invalidate();
    }
    public boolean pressed() { return (state == PRESSED); }
    public boolean hasExited() {
        return hasExited;
    }
    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setTextSize(minMaxSize);
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        originX = centerX - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        originY = centerY - fm.ascent - 2*fm.descent;
        if (state == INACTIVE)
            canvas.drawText(Monogram, originX, originY, mPaint);
        else {
            ActivePaint.setTextSize(minMaxSize);
            canvas.drawText(Monogram, originX, originY, ActivePaint);
        }
    }
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        mPaint.getTextBounds(Monogram, 0, 1, bounds);
        float size = mPaint.getTextSize()*Math.min(0.9f * (float) w / (float) bounds.width(),0.9f * (float) h / (float) bounds.height());
        if (size < minMaxSize)
            minMaxSize = size;
        centerX = w / 2f;
        centerY = h / 2f;
        border = (float) Math.sqrt(centerX * centerX + centerY * centerY);
    }
    @Override
    public void onTextChanged (CharSequence text, int start, int lengthBefore, int lengthAfter) {
        Monogram = text.length() > 0 ? Character.toString(text.charAt(0)) : " ";
    }
    public String getStatusString() {
        if (type == Interval.tEXP)
            return dragDist == 0 ? label : " $" + dragDist;
        else
            return dragDist == 0 ?  label : " already  " + Integer.toString((int) (dragDist / 60)) + ":" + String.format(Locale.US, "%02d", (int) dragDist % 60)
                    + " (" + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(1000L * (System.currentTimeMillis() / 1000L - 60 * (long) dragDist))) + ")";
    }
}
