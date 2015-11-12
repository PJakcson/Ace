package com.aceft.data;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.graphics.Point;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.View;

import com.aceft.MainActivity;

public class AceAnims {

    private void fadeIn(View v, int i){
        ObjectAnimator fIn = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
        fIn.setDuration(i);
        fIn.start();
    }

    public static void fadeOut(View v, int i) {
        ObjectAnimator fOut = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f);
        fOut.setDuration(i);
        fOut.start();
    }

    public static void hideActionBar(final Activity a, boolean anim) {
        if (a == null) return;
        if (!anim) {
            ((MainActivity) a).getSupportActionBar().hide();
        }
        Toolbar ab = ((MainActivity) a).getToolbar();
        ObjectAnimator fOut = ObjectAnimator.ofFloat(ab, "translationY", 0, -ab.getHeight());
        int dur = anim ? 250 : 0;
        fOut.setDuration(dur);
        fOut.setInterpolator(new android.view.animation.Interpolator() {
            @Override
            public float getInterpolation(float v) {
                return v * v;
            }
        });
        fOut.start();
    }

    public static void hideActionBar(Activity activity) {
        hideActionBar(activity, true);
    }

    public static void showActionbar(Activity a, boolean anim) {
        if (a == null) return;

        if (!anim) {
            ((MainActivity) a).getSupportActionBar().show();
        }

        Toolbar ab = ((MainActivity) a).getToolbar();
        ObjectAnimator fOut = ObjectAnimator.ofFloat(ab, "translationY", -ab.getHeight(), 0);
        int dur = anim ? 250 : 0;
        fOut.setDuration(dur);
        fOut.setInterpolator(new android.view.animation.Interpolator() {
            @Override
            public float getInterpolation(float v) {
                return v * v;
            }
        });
        fOut.start();
    }

    public static void showActionbar(Activity activity) {
        showActionbar(activity, true);
    }

    private static int getTransHeight(Activity activity, Toolbar ab) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result + ab.getHeight();
    }

    public static void showFloatingButton(View v, boolean anim) {

        ObjectAnimator sX = ObjectAnimator.ofFloat(v, "scaleX", 0, 1);
        ObjectAnimator sY = ObjectAnimator.ofFloat(v, "scaleY", 0, 1);
        AnimatorSet aS = new AnimatorSet();
        aS.playTogether(sX, sY);
        aS.setDuration(anim ? 150 : 0);
        aS.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return input*input;
            }
        });
        aS.start();
        v.setVisibility(View.VISIBLE);
    }
    public static void hideFloatingButton(final View v, boolean anim) {
        ObjectAnimator sX = ObjectAnimator.ofFloat(v, "scaleX", 0, 1);
        ObjectAnimator sY = ObjectAnimator.ofFloat(v, "scaleY", 0, 1);
        AnimatorSet aS = new AnimatorSet();
        aS.playTogether(sX, sY);
        aS.setDuration(anim ? 150 : 0);
        aS.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return -(input*input)+1;
            }
        });
        aS.start();
        aS.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (v != null)
                    v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}
