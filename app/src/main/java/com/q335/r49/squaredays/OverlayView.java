package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

class OverlayView extends View {
    private int lastEvent;
    private float originX, originY, bmOrigX, bmOrigY;
    private float actionDownX, actionDownY, moveX, moveY;
    private float cancelZoneRadius = 100;
    View selectedView;
    Bitmap viewBmp;
    float viewBmpCenterX, viewBmpCenterY;
    Paint bmPaint;
    Matrix trMax = new Matrix();
    @Override
    protected void onDraw(Canvas canvas) {
        if (lastEvent == MotionEvent.ACTION_MOVE) {
            canvas.drawRect(originX + actionDownX - cancelZoneRadius, originY + actionDownY - cancelZoneRadius, originX + actionDownX + cancelZoneRadius, originY + actionDownY + cancelZoneRadius, Glob.pCancelZone);
            viewBmp = Bitmap.createBitmap(viewBmp,0,0,viewBmp.getWidth(),viewBmp.getHeight(),trMax,true);
            viewBmp = Bitmap.createScaledBitmap(viewBmp,viewBmp.getWidth(),viewBmp.getHeight(),false);
            canvas.drawBitmap(viewBmp, moveX + originX - viewBmpCenterX, moveY + originY - viewBmpCenterY, bmPaint);

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
        viewBmp = selectedView.getDrawingCache();
        viewBmpCenterX = viewBmp.getWidth()/2f;
        viewBmpCenterY = viewBmp.getHeight()/2f;
        bmPaint = new Paint();
        trMax.postRotate(5,viewBmpCenterX,viewBmpCenterY);
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