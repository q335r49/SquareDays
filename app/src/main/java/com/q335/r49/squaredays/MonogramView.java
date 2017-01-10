package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

//TODO: Active tasks should show "rotation"
public class MonogramView extends TextView implements View.OnTouchListener {
    private static float rRotDrag = 0.6f;
    public interface onTouch {
        void actionDown();
        void actionMove(float dist);
        void actionUp(float dist);
        void actionCancel();
    }
    private onTouch listener;
    private int state;
        private static final int INACTIVE = 0;
        private static final int PRESSED = 5;
        private static final int ACTIVE = 6;
    private static float textSizeMinMax = 1000f;
    private float rx0, ry0;
    private double rEdge;
    private Paint pNorm, pActive;
    private boolean hasExited;
    private String monogram;

    public MonogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        pNorm = new Paint();
            pNorm.setStyle(Paint.Style.FILL);
            pNorm.setTextSize(100f);
            pNorm.setTypeface(Glob.CommandFont);
            pNorm.setFlags(Paint.ANTI_ALIAS_FLAG);
        pActive = new Paint(pNorm);
        bounds = new Rect();
    }
    public void init(int color, String label, onTouch listener) {
        pNorm.setColor(color);
        super.setText(label);
        this.listener = listener;
        if (Color.red(color) > 200 && Color.green(color) > 200 && Color.blue(color) > 200)
             pActive.setColor(0xFF888888);
        else pActive.setColor(Glob.invert(color));
        setOnTouchListener(this);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();
        float dragDist;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                state = PRESSED;
                hasExited = false;
                v.getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                listener.actionDown();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (state == PRESSED) {
                    dragDist = (float) Math.max(0, Math.sqrt((X - rx0) * (X - rx0) + (Y - ry0) * (Y - ry0)) - rEdge);
                    if (dragDist > 0)
                        hasExited = true;
                    setRotation(dragDist * rRotDrag);
                    listener.actionMove(dragDist);
                    return true;
                } else
                    return false;
            case MotionEvent.ACTION_UP:
                if (state == PRESSED) {
                    dragDist = (float) Math.max(0, Math.sqrt((X - rx0) * (X - rx0) + (Y - ry0) * (Y - ry0)) - rEdge);
                    setRotation(0);
                    invalidate();
                    listener.actionUp(dragDist);
                }
                state = INACTIVE;
                return false;
            case MotionEvent.ACTION_CANCEL:
                state = INACTIVE;
                setRotation(0);
                invalidate();
                listener.actionCancel();
                return false;
            default:
                return true;
        }
    }
    public void activate(float rot) {
        state = ACTIVE;
        setRotation(rot);
        invalidate();
    }
    public void deactivate() {
        state = INACTIVE;
        invalidate();
    }
    public boolean pressed() { return state == PRESSED; }
    public boolean hasExited() { return hasExited; }
    private Rect bounds;
    @Override
    public void onDraw(Canvas canvas) {
        pNorm.setTextSize(textSizeMinMax);
        pNorm.getTextBounds(monogram, 0, 1, bounds);
        float originX = rx0 - bounds.width() / 2f - bounds.left;
        Paint.FontMetrics fm = pNorm.getFontMetrics();
        float originY = ry0 - fm.ascent - 2*fm.descent;
        if (state == INACTIVE)
            canvas.drawText(monogram, originX, originY, pNorm);
        else {
            pActive.setTextSize(textSizeMinMax);
            canvas.drawText(monogram, originX, originY, pActive);
        }
    }
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        pNorm.getTextBounds(monogram, 0, 1, bounds);
        float size = pNorm.getTextSize()*Math.min(0.9f * (float) w / (float) bounds.width(),0.9f * (float) h / (float) bounds.height());
        if (size < textSizeMinMax)
            textSizeMinMax = size;
        rx0 = w / 2f;
        ry0 = h / 2f;
        rEdge = Math.sqrt(rx0 * rx0 + ry0 * ry0);
    }
    @Override
    public void onTextChanged (CharSequence text, int start, int lengthBefore, int lengthAfter) {
        monogram = text.length() > 0 ? Character.toString(text.charAt(0)) : " ";
    }
}
