package com.bgmi.floating;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.bgmi.R;

public class ToggleBullet extends Service {
    
    private WindowManager windowManager;
    private View mainView;
    private LayoutParams paramsView;
    private boolean isBulletOn = true;

    public native void ToggleBullet(boolean value);

    static {
        System.loadLibrary("THUNDER");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        showFloatingButton();
        ToggleBullet(true);
    }

    private void showFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainView = LayoutInflater.from(this).inflate(R.layout.toggle_aimbullet, null);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = LayoutParams.TYPE_PHONE;
        }

        paramsView = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        paramsView.gravity = Gravity.TOP | Gravity.START;
        paramsView.x = 100;
        paramsView.y = 350; // Offset from Aimbot toggle

        windowManager.addView(mainView, paramsView);

        final ImageView bulletIcon = mainView.findViewById(R.id.imageview_aim);
        bulletIcon.setImageResource(R.drawable.b2); // b2 for ON

        mainView.findViewById(R.id.layout_icon_control_aim).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = paramsView.x;
                        initialY = paramsView.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);
                        if (Xdiff < 10 && Ydiff < 10) {
                            isBulletOn = !isBulletOn;
                            ToggleBullet(isBulletOn);
                            if (isBulletOn) {
                                bulletIcon.setImageResource(R.drawable.b2);
                            } else {
                                bulletIcon.setImageResource(R.drawable.b1);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        paramsView.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsView.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(mainView, paramsView);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToggleBullet(false);
        if (mainView != null) {
            windowManager.removeView(mainView);
        }
    }
}
