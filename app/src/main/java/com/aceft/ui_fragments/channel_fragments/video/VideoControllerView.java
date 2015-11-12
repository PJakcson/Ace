/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aceft.ui_fragments.channel_fragments.video;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import com.aceft.R;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 * 
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 *   has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 *   setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 *   otherwise by using the MediaController(Context, boolean) constructor
 *   with the boolean set to false
 * </ul>
 */
public class VideoControllerView extends FrameLayout {
    private static final String TAG = "VideoControllerView";
    
    private MediaPlayerControl  mPlayer;
    private Context             mContext;
    private ViewGroup           mAnchor;
    private View                mRoot;
    private boolean             mShowing;
    private static final int    sDefaultTimeout = 3000;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;
    private ImageView           mPauseButton;
    private ImageView           mSettingsButton;
    private ImageView           mShareButton;
    private ImageView           mFollowButton;
    private TextView            mViewersCount;
    private ImageView           mFullscreenButton;
    private ImageView           mViewersIcon;
    private Handler             mHandler = new MessageHandler(this);
    private boolean             mFallbackMode;
    private boolean             mIsFollowing;


    public VideoControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        mContext = context;
        
        Log.i(TAG, TAG);
    }

    public VideoControllerView(Context context) {
        super(context);
        mContext = context;
        
        Log.i(TAG, TAG);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
        super.onFinishInflate();
    }
    
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
        updateFullScreen();
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;

        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller2, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageView) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }
        
        mFullscreenButton = (ImageView) v.findViewById(R.id.fullscreen);
        if (mFullscreenButton != null) {
            mFullscreenButton.requestFocus();
            mFullscreenButton.setOnClickListener(mFullscreenListener);
        }

        mSettingsButton = (ImageView) v.findViewById(R.id.quality);
        if (mSettingsButton != null) {
            mSettingsButton.requestFocus();
            mSettingsButton.setOnClickListener(mSettingsListener);
        }

        mFollowButton = (ImageView) v.findViewById(R.id.follow);
        if (mFollowButton != null) {
            mFollowButton.requestFocus();
            mFollowButton.setOnClickListener(mFollowListener);
            if(mIsFollowing) mFollowButton.setImageResource(R.drawable.ic_unfollow);
            else mFollowButton.setImageResource(R.drawable.ic_follow);
        }

        mShareButton = (ImageView) v.findViewById(R.id.share);
        if (mShareButton != null) {
            mShareButton.requestFocus();
            mShareButton.setOnClickListener(mShareListener);
        }

        mViewersCount = (TextView) v.findViewById(R.id.viewersText);
        mViewersIcon = (ImageView) v.findViewById(R.id.viewersIcon);

        if (mAnchor != null) {
            mAnchor.removeViews(1, mAnchor.getChildCount() - 1);
            mAnchor.addView(this);
        }

        hide();
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0.f, 1.f);

            if (mPauseButton != null) {
                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(mPauseButton, "scaleX", 1.1f);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(mPauseButton, "scaleY", 1.1f);
                scaleDownX.setInterpolator(new TimeInterpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        return -(2 * v - 1f) * (2 * v - 1f) + 1f;
                    }
                });
                scaleDownY.setInterpolator(new TimeInterpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        v = 2*v - 1f;
                        return - v*v + 1f;
                    }
                });
                AnimatorSet scaleDown = new AnimatorSet();
                scaleDown.play(scaleDownX).with(scaleDownY).with(fadeIn);
                scaleDown.start();
            }

            mShowing = true;
        }
        updatePausePlay();
        updateFullScreen();
        
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }
    
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null) {
            return;
        }

//        try {
//            mAnchor.removeView(this);
//            mHandler.removeMessages(SHOW_PROGRESS);
//        } catch (IllegalArgumentException ex) {
//            Log.w("MediaController", "already removed");
//        }
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
        fadeIn.start();
        mShowing = false;
    }

    public void updateViewers(String s) {
        if (s == null || s.isEmpty()) return;
        if (mViewersCount != null && mViewersIcon != null) {
            mViewersCount.setText(s);
            mViewersIcon.setVisibility(VISIBLE);
        }
    }


    public void updateFollowing(boolean b) {
        mIsFollowing = b;
        if (mFollowButton != null) {
            if (b) mFollowButton.setImageResource(R.drawable.ic_unfollow);
            else mFollowButton.setImageResource(R.drawable.ic_follow);
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }
        
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private OnClickListener mFullscreenListener = new OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            show(sDefaultTimeout);
        }
    };

    private OnClickListener mSettingsListener = new OnClickListener() {
        public void onClick(View v) {
            doToggleSettings();
            show(sDefaultTimeout);
        }
    };

    private OnClickListener mFollowListener = new OnClickListener() {
        public void onClick(View v) {
            doFollow();
            show(sDefaultTimeout);
        }
    };

    private OnClickListener mShareListener = new OnClickListener() {
        public void onClick(View v) {
            doShare();
            show(sDefaultTimeout);
        }
    };

    public void updatePausePlay() {
        if (mRoot == null || mPauseButton == null || mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_pause_circle);
        } else {
            mPauseButton.setImageResource(R.drawable.ic_play_circle);
        }
    }

    public void updateFullScreen() {
        if (mRoot == null || mFullscreenButton == null || mPlayer == null) {
            return;
        }
        if (mPlayer.isFullScreen()) {
            mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit_white);
        }
        else {
            mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_white);
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mFallbackMode) {
            mPlayer.pause();
            return;
        }
        
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    private void doToggleFullscreen() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.toggleFullScreen();
    }

    private void doToggleSettings() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.toggleSettingsScreen();
    }

    private void doFollow() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.doFollow();
    }

    private void doShare() {
        if (mPlayer == null) {
            return;
        }
        mPlayer.doShare();
    }

    public void hideFullscreen() {
        mFullscreenButton.setVisibility(View.GONE);
    }


    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(VideoControllerView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(VideoControllerView.class.getName());
    }

    public void setmFallbackMode(boolean mFallbackMode) {
        this.mFallbackMode = mFallbackMode;
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        boolean isPlaying();
        boolean isFullScreen();
        void    toggleFullScreen();
        void toggleSettingsScreen();
        void doFollow();
        void doShare();
    }
    
    private static class MessageHandler extends Handler {
        private final WeakReference<VideoControllerView> mView; 

        MessageHandler(VideoControllerView view) {
            mView = new WeakReference<>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
            }
        }
    }
}