package com.aceft.ui_fragments.channel_fragments.video;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.aceft.R;
import com.aceft.data.LayoutTasks;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.exoplayer.metadata.TxxxMetadata;

import java.util.Map;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class VideoPlayerFragment extends Fragment implements SurfaceHolder.Callback, HlsPlayer.Listener, HlsPlayer.Id3MetadataListener, VideoControllerView.MediaPlayerControl {
    private FrameLayout mRootView;
    private VideoSurfaceView mPlayerSurface;
    private HlsPlayer player;
    private HlsRendererBuilder mHlsRenderer;
    private boolean playerNeedsPrepare;
    private VideoControllerView controller;
    private boolean enableBackgroundAudio = false;
    private MediaPlayerControl mCallback;
    private boolean isFullScreen;
    private boolean mStreamStarted;
    private String mVideoUrl;
    private Context mContext;
    private boolean mFallbackMode, mFullscreenButton = true;

    private long lastClickTime;
    static final int MAX_DURATION = 250;
    private VideoView mVideoFallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = (FrameLayout) inflater.inflate(R.layout.fragment_player, container, false);
        mPlayerSurface = (VideoSurfaceView) mRootView.findViewById(R.id.playerSurface);

        mRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < MAX_DURATION){
                        toggleFullScreen();
                    } else {
                        toggleControlsVisibility();
                    }
                    lastClickTime = clickTime;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            isFullScreen = savedInstanceState.getBoolean("isFullScreen");
            mVideoUrl = savedInstanceState.getString("mVideoUrl");
            mStreamStarted = savedInstanceState.getBoolean("mStreamStarted");
        }

        if (getActivity() == null)
            return mRootView;

        mContext = getActivity();
        SurfaceHolder mHolder = mPlayerSurface.getHolder();
        mHolder.addCallback(this);
        controller = new VideoControllerView(mContext);

        return mRootView;
    }

    public void playStream(String s) {
        if (getActivity() == null) return;
        mStreamStarted = true;
        mVideoUrl = s;
        mHlsRenderer = new HlsRendererBuilder(getUserAgent(getActivity()), mVideoUrl);
        preparePlayer();
    }

    public void restartStream(String s) {
        if (getActivity() == null) return;
        mVideoUrl = s;
        mStreamStarted = true;
        mFallbackMode = false;
        player = null;
        playerNeedsPrepare = true;
        controller = new VideoControllerView(getActivity());
        mHlsRenderer = new HlsRendererBuilder(getUserAgent(getActivity()), mVideoUrl);
        preparePlayer();
    }

    public boolean hasData() {
        return mVideoUrl != null;
    }

    public String getUrl() {
        return mVideoUrl;
    }

    public void changeQuality(String s) {
        if (getActivity() == null) return;
        if (player != null)
            player.release();
        if (controller != null)
            controller.hide();
        player = null;
        mStreamStarted = true;
        mVideoUrl = s;
        mHlsRenderer = new HlsRendererBuilder(getUserAgent(getActivity()), mVideoUrl);
        preparePlayer();
    }

    public void updateViewers(String s) {
        s = LayoutTasks.formatNumber(s);
        if (controller != null) {
            controller.updateViewers(s);
        }
    }

    public void updateFollowing(boolean b) {
        if (controller != null) {
            controller.updateFollowing(b);
        }
    }

    private void preparePlayer() {
        if (getActivity() == null) return;

        if (mFallbackMode) {
            if (controller != null) {
                controller.setMediaPlayer(this);
                controller.setAnchorView(mRootView);
                if (!mFullscreenButton) controller.hideFullscreen();
            } else {
                controller = new VideoControllerView(getActivity());
                controller.setMediaPlayer(this);
                controller.setAnchorView(mRootView);
                if (!mFullscreenButton) controller.hideFullscreen();
            }
            startFallbackPlayer();
            return;
        }

        if (mHlsRenderer == null) {
            mHlsRenderer = new HlsRendererBuilder(getUserAgent(getActivity()), mVideoUrl);
        }
        if (player == null) {
            player = new HlsPlayer(mHlsRenderer);
            player.addListener(this);
            player.setMetadataListener(this);
            playerNeedsPrepare = true;

            if (controller != null) {
                controller.setMediaPlayer(this);
                controller.setAnchorView((FrameLayout) mRootView);
                if (!mFullscreenButton) controller.hideFullscreen();
            } else {
                controller = new VideoControllerView(getActivity());
                controller.setMediaPlayer(this);
                controller.setAnchorView((FrameLayout) mRootView);
                if (!mFullscreenButton) controller.hideFullscreen();
            }
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(mPlayerSurface.getHolder().getSurface());
        if (!mFallbackMode)
            player.setPlayWhenReady(true);
    }

    private void startFallbackPlayer() {
        mVideoFallback = new VideoView(getActivity());
        mRootView.removeAllViews();
        mRootView.addView(mVideoFallback);
        controller.setAnchorView(mRootView);

        if (controller != null) {
            controller.setmFallbackMode(true);
        } else {
            controller = new VideoControllerView(getActivity());
            controller.setmFallbackMode(true);
        }

        mFallbackMode = true;
        mCallback.streamStateChanged(HlsPlayer.STATE_PREPARING);
        mPlayerSurface.setVisibility(View.GONE);
        mVideoFallback.setVisibility(View.VISIBLE);
        mVideoFallback.setVideoURI(Uri.parse(mVideoUrl));
        mVideoFallback.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mVideoFallback.requestFocus();
                mCallback.streamStateChanged(HlsPlayer.STATE_READY);
            }
        });
        mVideoFallback.start();
    }

    public static String getUserAgent(Context context) {
        if (context == null) return "?";
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            versionName = "?";
        }
        return "ExoPlayerDemo/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE +
                ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isFullScreen", isFullScreen);
        outState.putString("mVideoUrl", mVideoUrl);
        outState.putBoolean("mStreamStarted", mStreamStarted);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (MediaPlayerControl) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMediaPlayerControl");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setBackgrounded(false);
        }
    }

    @Override
    public void onPause() {
        if (!enableBackgroundAudio) {
            releasePlayer();
        } else {
            player.setBackgrounded(true);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
//        shutterView.setVisibility(View.GONE);
        mPlayerSurface.setVideoWidthHeightRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        mCallback.streamStateChanged(playbackState);
    }

    @Override
    public void onError(Exception e) {
        Log.d("playerError", e.toString());
        player.release();
        mFallbackMode = true;
        startFallbackPlayer();
    }

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (int i = 0; i < metadata.size(); i++) {
            if (metadata.containsKey(TxxxMetadata.TYPE)) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) metadata.get(TxxxMetadata.TYPE);
                Log.i("ExoPlayer", String.format("ID3 TimedMetadata: description=%s, value=%s",
                        txxxMetadata.description, txxxMetadata.value));
            }
        }
    }

    private void toggleControlsVisibility()  {
        if (controller.isShowing()) {
            controller.hide();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controller.show();
        mCallback.controlsShown();
    }

    public void releasePlayer() {
        if (getActivity() == null) return;
        if (player != null) {
            player.release();
            player = null;
        }
        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void start() {
        if (player != null)
            player.getPlayerControl().start();
    }

    @Override
    public void pause() {
        if (mFallbackMode) {
            if (mVideoFallback.isPlaying())
                mVideoFallback.pause();
            else
                mVideoFallback.start();
        } else {
            if (player != null)
                player.getPlayerControl().pause();
        }
    }

    @Override
    public boolean isPlaying() {
        if (player == null && !mFallbackMode)
            return false;
        if (mFallbackMode && mVideoFallback == null)
            return false;
        if (mFallbackMode)
            return mVideoFallback.isPlaying();
        return player.getPlayerControl().isPlaying();
    }

    @Override
    public boolean isFullScreen() {
        return isFullScreen;
    }

    @Override
    public void toggleFullScreen() {
        mCallback.toggleFullScreen();
        controller.show();
    }

    @Override
    public void toggleSettingsScreen() {
        mCallback.toggleSettingsScreen();
    }

    @Override
    public void doFollow() {
        mCallback.doFollow();
    }

    @Override
    public void doShare() {
        mCallback.doShare();
    }

    public void setFullScreen(boolean b) {
        isFullScreen = b;
    }

    public interface MediaPlayerControl {
        void    toggleFullScreen();
        void    toggleSettingsScreen();
        void    streamStateChanged(int i);
        void    doFollow();
        void    doShare();
        void    controlsShown();
    }
}