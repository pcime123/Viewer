package com.sscctv.seeeyesmonitor;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.logging.Handler;

import static android.support.constraint.Constraints.TAG;

public class ZoomableSurfaceView extends SurfaceView {

    private ScaleGestureDetector SGD;
    private Context context;
    private boolean isSingleTouch;
    private float width, height = 0;
    private float scale = 0.5f;
    private float minScale = 1f;
    private float maxScale = 4f;
    int left, top, right, bottom;

    private TextView mSignalInfoView;
    private Runnable mHideSignalInfoTask;


    public ZoomableSurfaceView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public ZoomableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public ZoomableSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        setOnTouchListener(new MyTouchListeners());
        SGD = new ScaleGestureDetector(context, new ScaleListener());
        this.getViewTreeObserver().addOnGlobalLayoutListener(() -> {

        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (width == 0 && height == 0) {
            width = ZoomableSurfaceView.this.getWidth();
            height = ZoomableSurfaceView.this.getHeight();
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            SharedPreferences pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putFloat("scale", 1f);
            editor.apply();

//            Log.d(TAG, "Width: " + width + " Height: " + height);
        }

    }

    public class MyTouchListeners implements View.OnTouchListener {

        float dX, dY;

        MyTouchListeners() {
            super();
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            SGD.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                isSingleTouch = false;
            } else {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    isSingleTouch = true;
                }
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = ZoomableSurfaceView.this.getX() - event.getRawX();
                    dY = ZoomableSurfaceView.this.getY() - event.getRawY();
//                    Log.d(TAG, "Zoom Touch");
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isSingleTouch) {
                        ZoomableSurfaceView.this.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        checkDimension(ZoomableSurfaceView.this);
                    }
                    break;
                default:
                    return true;
            }
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            Log.e("onGlobalLayout: ", scale + " " + width + " " + height);
            scale *= detector.getScaleFactor();
            scale = Math.max(minScale, Math.min(scale, maxScale));

            SharedPreferences pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putFloat("scale", scale);
            editor.apply();


            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
            layoutParams.gravity = Gravity.CENTER;
            layoutParams.width = (int) (scale * width);
            layoutParams.height = (int) (scale * height);
//            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
//                    (int) (width * scale), (int) (height * scale));
            ZoomableSurfaceView.this.setLayoutParams(layoutParams);
            checkDimension(ZoomableSurfaceView.this);
            return true;
        }
    }

    private void checkDimension(View vi) {
        if (vi.getX() > left) {
            vi.animate()
                    .x(left)
                    .y(vi.getY())
                    .setDuration(0)
                    .start();
        }

        if ((vi.getWidth() + vi.getX()) < right) {
            vi.animate()
                    .x(right - vi.getWidth())
                    .y(vi.getY())
                    .setDuration(0)
                    .start();
        }

        if (vi.getY() > top) {
            vi.animate()
                    .x(vi.getX())
                    .y(top)
                    .setDuration(0)
                    .start();
        }

        if ((vi.getHeight() + vi.getY()) < bottom) {
            vi.animate()
                    .x(vi.getX())
                    .y(bottom - vi.getHeight())
                    .setDuration(0)
                    .start();
        }
    }
}
