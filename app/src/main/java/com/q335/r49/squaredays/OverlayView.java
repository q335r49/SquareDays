package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

class OverlayView extends View {
    private int lastEvent;
    private float originX, originY, bmOrigX, bmOrigY;
    private float actionDownX, actionDownY, moveX, moveY;
    private float cancelZoneRadius = TasksFrag.cancelZone / TasksFrag.rExpDp;
    View selectedView;
    Bitmap dragBox;
    Paint bmPaint;
    @Override
    protected void onDraw(Canvas canvas) {
        if (lastEvent == MotionEvent.ACTION_MOVE) {
            canvas.drawRect(originX + actionDownX - cancelZoneRadius, originY + actionDownY - cancelZoneRadius, originX + actionDownX + cancelZoneRadius, originY + actionDownY + cancelZoneRadius, Glob.pCancelZone);
            canvas.drawBitmap(dragBox, moveX + originX - bmOrigX, moveY + originY - bmOrigY, bmPaint);
        }
    }
    public OverlayView(Context context) { super(context); }
    public OverlayView(Context context, AttributeSet attrs) { super(context, attrs); }
    public OverlayView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }
    void actionDown(View v, float clickX, float clickY) {
        originX = v.getX();
        originY = v.getY();
        actionDownX = clickX;
        actionDownY = clickY;
        lastEvent = MotionEvent.ACTION_DOWN;
        selectedView = v;
        selectedView.setDrawingCacheEnabled(true);
        selectedView.buildDrawingCache();
        dragBox = selectedView.getDrawingCache();
        bmPaint = new Paint();

        float x, y, width, height;
        if (actionDownX >= cancelZoneRadius) {
            bmOrigX = cancelZoneRadius;
            x = actionDownX - cancelZoneRadius;
        } else {
            x = 0f;
            bmOrigX = actionDownX;
        }
        if (actionDownY >= cancelZoneRadius) {
            bmOrigY = cancelZoneRadius;
            y = actionDownY - cancelZoneRadius;
        } else {
            y = 0f;
            bmOrigY = actionDownY;
        }
        width = Math.min(x + cancelZoneRadius * 2, dragBox.getWidth()) - x;
        height = Math.min(y + cancelZoneRadius * 2, dragBox.getHeight()) - y;
        dragBox = Bitmap.createBitmap(dragBox,(int) x,(int) y,(int) width,(int) height);
    }
    void actionMove(float x, float y) {
        moveX = x;
        moveY = y;
        lastEvent = MotionEvent.ACTION_MOVE;
        invalidate();
    }
    void actionUp(float x, float y) {
        lastEvent = MotionEvent.ACTION_UP;
        invalidate();
    }
}