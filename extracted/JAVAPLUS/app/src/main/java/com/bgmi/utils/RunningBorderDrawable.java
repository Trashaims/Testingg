package com.bgmi.utils;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

public class RunningBorderDrawable extends Drawable implements Animatable {
    private Paint paint;
    private Paint bgPaint;
    private float cornerRadius;
    private float borderWidth;
    private ValueAnimator animator;
    private float rotationAngle = 0;
    private SweepGradient shader;
    private Matrix matrix;
    private int[] borderColors;

    public RunningBorderDrawable(int bgColor, int[] borderColors, float borderWidth, float cornerRadius) {
        this.borderWidth = borderWidth;
        this.cornerRadius = cornerRadius;
        this.borderColors = borderColors;

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);

        matrix = new Matrix();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.width() > 0 && bounds.height() > 0) {
            // SweepGradient maps colors angularly from 0 to 360 degrees.
            // Using multiple color stops creates distinct "running light" segments.
            shader = new SweepGradient(bounds.centerX(), bounds.centerY(), borderColors, null);
            paint.setShader(shader);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        RectF rectF = new RectF(
            bounds.left + borderWidth / 2f,
            bounds.top + borderWidth / 2f,
            bounds.right - borderWidth / 2f,
            bounds.bottom - borderWidth / 2f
        );

        // 1. Draw solid/translucent background card
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint);

        // 2. Rotate the sweep gradient local matrix
        if (shader != null) {
            matrix.setRotate(rotationAngle, bounds.centerX(), bounds.centerY());
            shader.setLocalMatrix(matrix);
        }

        // 3. Draw the animated border stroke
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        bgPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        bgPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void start() {
        if (isRunning()) return;

        // Loop rotationAngle from 0 to 360 degrees infinitely
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(4000); // Duration in milliseconds for one full cycle
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            rotationAngle = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
        animator.start();
    }

    @Override
    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    public boolean isRunning() {
        return animator != null && animator.isRunning();
    }
}
