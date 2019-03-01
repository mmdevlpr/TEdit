package com.atr.tedit.utilitybar.state;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.atr.tedit.util.Callback;
import com.atr.tedit.utilitybar.UtilityBar;

public abstract class UtilityState {
    public static final int ANIMLENGTH = 200;
    public static final float SCALE = 0.5f;

    protected int animDelay = 30;

    public final int STATE;
    public final UtilityBar BAR;

    protected View[][] LAYERS;

    private int layer = 0;

    protected boolean animating = false;

    protected UtilityState(UtilityBar bar, int state) {
        BAR = bar;
        STATE = state;
    }

    public boolean setLayer(int num) {
        if (num < 0 || num >= LAYERS.length)
            return false;

        layer = num;
        return true;
    }

    public int getLayer() {
        return layer;
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setEnabled(boolean enable) {
        for (View v : LAYERS[layer]) {
            v.setEnabled(enable);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void animateGL(View view, float alpha, float scale, Interpolator interpolator, int delay) {
        view.animate().alpha(alpha).scaleX(scale).scaleY(scale).setDuration(ANIMLENGTH)
                .setInterpolator(interpolator).setStartDelay(delay).withLayer();
    }

    protected void animateSW(View view, float alpha, float scale, Interpolator interpolator, int delay) {
        view.animate().alpha(alpha).scaleX(scale).scaleY(scale).setDuration(ANIMLENGTH)
                .setInterpolator(interpolator).setStartDelay(delay);
    }

    protected void animateBarHeight(int fromHeight, int toHeight, Interpolator interpolator, int delay) {
        ValueAnimator anim = ValueAnimator.ofInt(fromHeight, toHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams lp = BAR.bar.getLayoutParams();
                lp.height = (int)valueAnimator.getAnimatedValue();
                BAR.bar.setLayoutParams(lp);
            }
        });

        anim.setDuration(ANIMLENGTH).setInterpolator(interpolator);
        anim.setStartDelay(delay);
        anim.start();
    }

    public void setToState() {
        if (BAR.getState().STATE == STATE && BAR.getState().getLayer() == layer)
            return;

        BAR.bar.removeAllViews();
        View[] l = LAYERS[layer];
        for (View v : l) {
            v.setEnabled(true);
            BAR.bar.addView(v);
        }
    }

    protected void transOut() {
        animating = true;
        View[] l = LAYERS[layer];
        int count = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (View v : l) {
                v.setEnabled(false);
                animateGL(v, 0, SCALE, new AnticipateInterpolator(), animDelay * count);
                count++;
            }
        } else {
            for (View v : l) {
                v.setEnabled(false);
                animateSW(v, 0, SCALE, new AnticipateInterpolator(), animDelay * count);
                count++;
            }
        }
    }

    protected void transIn() {
        animating = true;
        View[] l = LAYERS[layer];
        int count = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (View v : l) {
                v.setScaleX(SCALE);
                v.setScaleY(SCALE);
                v.setAlpha(0);
                v.setEnabled(false);
                BAR.bar.addView(v);
                animateGL(v, 1, 1, new OvershootInterpolator(), animDelay * count);
                count++;
            }
        } else {
            for (View v : l) {
                v.setScaleX(SCALE);
                v.setScaleY(SCALE);
                v.setAlpha(0);
                v.setEnabled(false);
                BAR.bar.addView(v);
                animateSW(v, 1, 1, new OvershootInterpolator(), animDelay * count);
                count++;
            }
        }

        BAR.handler.postDelayed(new Runnable() {
            public void run() {
                animating = false;
                View[] l = LAYERS[layer];
                for (View v : l) {
                    v.setEnabled(true);
                }
            }
        }, ANIMLENGTH + (animDelay * (LAYERS[layer].length - 1)));
    }

    public void transToState(final UtilityState toState) {
        transToState(toState,null);
    }

    public void transToState(final UtilityState toState, final Callback<UtilityState> callback) {
        transTo(toState, toState.getLayer(), callback);
    }

    public void transToLayer(final int stateLayer) {
        transToLayer(stateLayer, null);
    }

    public void transToLayer(final int stateLayer, final Callback<UtilityState> callback) {
        transTo(this, stateLayer, callback);
    }

    public void transTo(final UtilityState toState, final int stateLayer, final Callback<UtilityState> callback) {
        if (toState.STATE == STATE && toState.getLayer() == layer)
            return;

        transOut();
        BAR.handler.postDelayed(new Runnable() {
            public void run() {
                BAR.bar.removeAllViews();
                animating = false;
                toState.setLayer(stateLayer);
                toState.transIn();
                if (callback != null)
                    callback.call(toState);
            }
        }, ANIMLENGTH + (animDelay * (LAYERS[layer].length - 1)));
    }
}
