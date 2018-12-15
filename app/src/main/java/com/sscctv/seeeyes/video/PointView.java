package com.sscctv.seeeyes.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class PointView extends View {
    private Paint paint;
    private float x, y, r = 5;
    public PointView(Context context) {
        super(context);

        paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(128, 128, r, paint);
        canvas.drawCircle(16, 138, r, paint);
        canvas.drawCircle(154, 16, r, paint);
        canvas.drawCircle(42, 26, r, paint);
        canvas.drawCircle(214, 230, r, paint);
        canvas.drawCircle(102, 240, r, paint);
        canvas.drawCircle(240, 118, r, paint);
        canvas.drawCircle(128, 128, r, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = event.getX();
        y = event.getY();

        invalidate();

        return true;
    }
}
