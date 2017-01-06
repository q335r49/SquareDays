package com.q335.r49.squaredays;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

class OverlayView extends View {
    private boolean active;
    private float originX, originY;
    private float cancelZoneRadius;
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("overlay","drawn");
        if (!active) return;

        canvas.drawRect(originX-cancelZoneRadius,originY-cancelZoneRadius,originX+cancelZoneRadius,originY+cancelZoneRadius,Glob.pCancelZone);
    }
    OverlayView(Context context) { super(context); }
    OverlayView(Context context, AttributeSet attrs) { super(context, attrs); }
    OverlayView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }
    void actionDown(float originX, float originY) {
        active = true;
        this.originX = originX;
        this.originY = originY;
        cancelZoneRadius = TasksFrag.cancelZone / TasksFrag.rExpDp;
    }
    void actionUp() { active = false; }
    void activate() {

    }
}