package com.aceft.ui_fragments.channel_fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aceft.PlayerActivity;
import com.aceft.R;
import com.aceft.adapter.ExpandFabAdapter;
import com.aceft.data.AceAnims;
import com.aceft.data.LayoutTasks;
import com.aceft.data.LiveMethods;
import com.aceft.data.Preferences;
import com.aceft.data.ScreenSlidePagerAdapter;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;
import com.aceft.ui_fragments.channel_fragments.video.VideoPlayerFragment;
import com.google.android.exoplayer.ExoPlayer;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.viewpagerindicator.CirclePageIndicator;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ChannelFragment extends Fragment {

    private ArrayList<Fragment> mFragments;
    private CirclePageIndicator mIndicator;
    private String mChannelName;
    private VideoPlayerFragment mPlayerFragment;
    private View mPlayerContainer;
    private boolean mFullScreen;
    private boolean mIsAuthenticated;
    private SharedPreferences mPreferences;
    private long timeFadeIndicator;
    private boolean isFollowingChannel;

    private Stream mStream;
    private LinkedHashMap<String, String> mAvailableQualities;
    private ImageView mPlayerOverlay;
    private View mStreamProgress;
    private boolean mIsPlaying;
    private View mPlayerBlock;
    private FloatingActionButton mCastToButton;

    private int uiVisibleFlags;

    private FragmentManager mRetainedChildFragmentManager;

    private boolean alreadyOpenedExternal;
    private int mQualitySelected;
    private int mInAppQualitySelected = 0;
    private ViewPager mPager;
    private long mCountLastUpdated;
    private RelativeLayout mRootView;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ExpandFabAdapter mFabAdapter;
    private View mExpandBackground;

    private ArrayList<Channel> mChannels = new ArrayList<>();
    private boolean mFabIsExpanded;

    public ChannelFragment newInstance(String c) {
        ChannelFragment fragment = new ChannelFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = (RelativeLayout) inflater.inflate(R.layout.fragment_channel_test, container, false);

        mPager = (ViewPager) mRootView.findViewById(R.id.pager);
        mPlayerBlock = mRootView.findViewById(R.id.videoBlock);
        mPlayerContainer = mRootView.findViewById(R.id.videoContainer);
        mPlayerOverlay = (ImageView) mRootView.findViewById(R.id.playerOverlay);
        mStreamProgress = mRootView.findViewById(R.id.streamLoadingProgress);
        mCastToButton = (FloatingActionButton) mRootView.findViewById(R.id.fab);
        mExpandBackground = mRootView.findViewById(R.id.expandedBackground);

        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.fabExpanded);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mLayoutManager = new LinearLayoutManager(getActivity(), OrientationHelper.VERTICAL, true);
        mLayoutManager.setStackFromEnd(false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mFabAdapter = new ExpandFabAdapter(this);
        mRecyclerView.setAdapter(mFabAdapter);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        View mDecorView = getActivity().getWindow().getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener(fullscreenListener);

        themeCastButton();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsAuthenticated = mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);
        mChannelName = getArguments().getString("channel_name");

        mPlayerFragment = new VideoPlayerFragment();
        childFragmentManager().beginTransaction().add(R.id.videoContainer, mPlayerFragment, "playerFragment").commit();

        ChannelCompactFragment mCompactFragment = new ChannelCompactFragment().newInstance(mChannelName);
        ChatFragment mChatFragment = new ChatFragment().newInstance(mChannelName, mChannelName, false);

        mFragments = new ArrayList<>();
        mFragments.add(mCompactFragment);
        mFragments.add(mChatFragment);

        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(childFragmentManager(), mFragments);
        mPager.setAdapter(mPagerAdapter);
        mIndicator = (CirclePageIndicator) mRootView.findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                showIndicatorFor(2000);
                if (position == 0)
                    AceAnims.showFloatingButton(mCastToButton, true);
                if (position == 1)
                    AceAnims.hideFloatingButton(mCastToButton, true);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                showIndicatorFor(2000);
            }
        });

        mExpandBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideExpanded();
            }
        });
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mExpandBackground.callOnClick();
                return false;
            }
        });

        showIndicatorFor(5000);

        mIsPlaying = false;
        downloadStreamData(mChannelName);
        fetchStreamToken(mChannelName);
        downloadFollowData();

        downloadFollowChannels();

        setPlayerSize();

        return mRootView;
    }

    private void downloadFollowChannels() {
        if (getActivity() == null) return;
        if (mPreferences == null || !mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false))
            return;
        final String uname = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (uname == null)
            playStream(false);
        Thread download = new Thread(new Runnable() {
            @Override
            public void run() {
                mChannels = Channel.getOnlineChannels(uname, getActivity());
            }
        });
        download.setPriority(Thread.MIN_PRIORITY);
        download.start();
    }

    private void downloadStreamData(String name) {
        if (getActivity() == null || name == null) return;
        String request = getActivity().getResources().getString(R.string.channel_stream_url);
        request += name;
        downloadStreamDataThread(request);
    }

    private void streamDataReceived(String s) {
        mCountLastUpdated = System.currentTimeMillis();
        Stream st = TwitchJSONParser.streamStringToStream(s);
        if (st == null) {
            mPlayerContainer.setVisibility(View.GONE);
            mPlayerBlock.setVisibility(View.GONE);
            mCastToButton.setVisibility(View.GONE);
            return;
        }
        streamParsed(st);
    }

    private void downloadFollowData() {
        if (getActivity() == null) return;
        String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null || username.isEmpty()) return;
        final String req = "https://api.twitch.tv/kraken/users/" + username + "/follows/channels/" + mChannelName;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final JSONObject j = TwitchNetworkTasks.downloadJSONData(req);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isFollowingChannel(j != null);
                    }
                });
            }
        }).start();
    }

    private void isFollowingChannel(boolean b) {
        isFollowingChannel = b;
        mPlayerFragment.updateFollowing(b);

        if (mPlayerBlock.findViewById(R.id.followOverlay) == null) return;
        if (b)
            ((ImageView) mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_unfollow);
        else
            ((ImageView) mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_follow);
    }

    private void fetchStreamToken(String s) {
        String tokenUrl = getString(R.string.stream_token_url) + s + "/access_token";
        downloadPlaylistDataThread(tokenUrl);
    }

    private void liveLinksReceived(LinkedHashMap<String, String> result) {
        if (result == null) return;
        if (result.isEmpty()) return;
        mAvailableQualities = result;
        updatePlayerView();
    }

    private void updatePlayerView() {
        mPlayerBlock.setVisibility(View.VISIBLE);
//        mPlayerOverlay.setImageResource(R.drawable.ic_play_overlay);
        mPlayerOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playStream(true);
            }
        });
        mCastToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandFAB();            }
        });

        mCastToButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                playStream(false);
                return false;
            }
        });

        if (mPlayerBlock.findViewById(R.id.followOverlay) != null)
            mPlayerBlock.findViewById(R.id.followOverlay).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow();
                }
            });

        if (mPlayerBlock.findViewById(R.id.shareOverlay) != null)
            mPlayerBlock.findViewById(R.id.shareOverlay).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showShareDialog();
                }
            });
        updateStreamOverlay();

        if (hasTabletLayout() || !isInLandscape())
            AceAnims.showFloatingButton(mCastToButton, true);

        if (!mIsPlaying) {
            showInitialPlayControls();
            startAutoStream();
//            if (mPager.getCurrentItem() == 0)
//                AceAnims.showFloatingButton(mCastToButton, true);
        }
    }

    private FragmentManager childFragmentManager() {
        if (mRetainedChildFragmentManager == null) {
            mRetainedChildFragmentManager = getChildFragmentManager();
        }
        return mRetainedChildFragmentManager;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mRetainedChildFragmentManager != null) {
            try {
                Field childFMField = Fragment.class.getDeclaredField("mChildFragmentManager");
                childFMField.setAccessible(true);
                childFMField.set(this, mRetainedChildFragmentManager);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        uiVisibleFlags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Data
//        if (mStream == null)


        if (mPlayerFragment.hasData() && !mPlayerFragment.isPlaying()) {
            String currentStream = mPlayerFragment.getUrl();
//            mPlayerFragment = new VideoPlayerFragment();
//            childFragmentManager().beginTransaction().replace(R.id.videoContainer, mPlayerFragment, "playerFragment").commit();
            mPlayerFragment.restartStream(currentStream);
        }


    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    private void setPlayerSize() {
        if (mFullScreen && hasTabletLayout()) {
            mPlayerContainer.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerContainer.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
            mPlayerBlock.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerBlock.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (mPager.getCurrentItem() == 0)
                AceAnims.hideFloatingButton(mCastToButton, true);
            hideSystemUI();
            return;
        }

        if (isInLandscape() && hasTabletLayout()) {
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            p.addRule(RelativeLayout.END_OF, R.id.videoBlock);
            mPager.setLayoutParams(p);
            mPager.setVisibility(View.VISIBLE);

            int width = (int) (getWindowWidth() * 0.6f);
            int height = (int) (getWindowWidth() * 0.6f * (360f / 640f));
            mPlayerContainer.getLayoutParams().width = width;
            mPlayerContainer.getLayoutParams().height = height;
            mPlayerOverlay.getLayoutParams().width = width;
            mPlayerOverlay.getLayoutParams().height = height;
            mPlayerBlock.getLayoutParams().width = width;
            mPlayerBlock.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (mPager.getCurrentItem() == 0 && mAvailableQualities != null)
                AceAnims.showFloatingButton(mCastToButton, true);
            return;
        }

        if (!isInLandscape() && hasTabletLayout()) {
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            p.addRule(RelativeLayout.BELOW, R.id.videoBlock);
            mPager.setLayoutParams(p);
            mPager.setVisibility(View.VISIBLE);

            mPlayerBlock.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mPlayerBlock.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerContainer.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerContainer.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
            mPlayerOverlay.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerOverlay.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
            if (mPager.getCurrentItem() == 0 && mAvailableQualities != null)
                AceAnims.showFloatingButton(mCastToButton, true);
            return;
        }

        if (isInLandscape() && !hasTabletLayout()) {
            mPlayerBlock.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mPlayerContainer.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerOverlay.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerOverlay.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (mPager.getCurrentItem() == 0)
                AceAnims.hideFloatingButton(mCastToButton, true);
            hideSystemUI();
            return;
        }

        if (!isInLandscape() && !hasTabletLayout()) {
            mPlayerBlock.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mPlayerContainer.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerContainer.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
            mPlayerOverlay.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            mPlayerOverlay.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
            if (mPager.getCurrentItem() == 0 && mAvailableQualities != null)
                AceAnims.showFloatingButton(mCastToButton, true);
            if (isImmersiveEnabled()) hideSystemUI();
            return;
        }
        mPlayerContainer.getLayoutParams().height = (int) (getWindowWidth() * (360f / 640f));
    }

    private boolean isInLandscape() {
        return getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private boolean hasTabletLayout() {
        int screenLayout = getActivity().getResources().getConfiguration().screenLayout;
        screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE || screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private int getWindowWidth() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public void setFullScreen() {
        if (getActivity() == null)
            return;
        if (!mFullScreen) {
            mFullScreen = true;
            mPlayerFragment.setFullScreen(true);

            if (hasTabletLayout())
                setPlayerSize();

            if (getActivity() != null)
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        } else {
            mFullScreen = false;
            mPlayerFragment.setFullScreen(false);
            if (!hasTabletLayout()) {
                if (getActivity() != null)
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null)
                            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                }, 1000);
            }
            if (hasTabletLayout()) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                setPlayerSize();
            }
        }
    }

    private void showIndicatorFor(final int t) {
        timeFadeIndicator = System.currentTimeMillis() + t;
        mIndicator.setAlpha(1f);

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(t);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (getActivity() == null || System.currentTimeMillis() < timeFadeIndicator) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fadeOutView(mIndicator);
                    }
                });
            }
        }).start();
    }

    private void fadeOutView(View v) {
        ObjectAnimator fOut = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f);
        fOut.setDuration(500);
        fOut.start();
    }

    private void downloadStreamDataThread(final String s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = TwitchNetworkTasks.downloadStringData(s);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        streamDataReceived(result);
                    }
                });
            }
        }).start();
    }

    private void downloadPlaylistDataThread(final String s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject result = TwitchNetworkTasks.downloadJSONData(s);
                String tokSig[] = TwitchJSONParser.tokenSigToStringArray(result);

                if (tokSig == null) return;
                if (tokSig.length < 2) return;
                if (tokSig[0] == null || tokSig[1] == null) return;

                String m3u8Url = "http://usher.twitch.tv/api/channel/hls/";
                m3u8Url += getArguments().getString("channel_name") + ".m3u8?token=";
                m3u8Url += tokSig[0] + "&sig=" + tokSig[1];
                m3u8Url += "&allow_audio_only=true&allow_source=true&type=any";
                if (mIsAuthenticated)
                    m3u8Url += "&oauth_token=" + mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");

                final LinkedHashMap<String, String> videoPlaylist = TwitchNetworkTasks.fetchTwitchPlaylist(m3u8Url);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        liveLinksReceived(videoPlaylist);
                    }
                });
            }
        }).start();
    }

    public void streamStateChanged(int i) {
        switch (i) {
            case ExoPlayer.STATE_IDLE:
                break;
            case ExoPlayer.STATE_PREPARING:
                mStreamProgress.setVisibility(View.GONE);
                mPlayerOverlay.setVisibility(View.GONE);
                mStreamProgress.setVisibility(View.VISIBLE);
                mPlayerOverlay.setImageDrawable(null);
                if (mPlayerFragment != null && mStream != null)
                    mPlayerFragment.updateViewers("" + mStream.getViewers());
                hideStreamOverlay();
                break;
            case ExoPlayer.STATE_BUFFERING:
                break;
            case ExoPlayer.STATE_READY:
                mStreamProgress.setVisibility(View.GONE);
                mPlayerOverlay.setVisibility(View.GONE);
                startChat();
                updateViewersCount(45000);
                mIsPlaying = true;
                if (!hasTabletLayout() && isInLandscape())
                    AceAnims.hideFloatingButton(mCastToButton, true);
                break;
            case ExoPlayer.STATE_ENDED:
                break;
        }
    }

    private void playStream(boolean inApp) {
        playStream(inApp, false);
    }

    private void playStream(boolean inApp, boolean share) {
        String q = LiveMethods.getQualityKey(getActivity(), mAvailableQualities);
        if (q.contains("showDialog")) {
            mQualitySelected = Integer.parseInt(q.substring(q.length() - 1));
            showPlayDialog(mAvailableQualities, mQualitySelected, inApp, false, share);
        } else {
            if (inApp) {
                mPlayerFragment.playStream(mAvailableQualities.get(q));
                mInAppQualitySelected = LiveMethods.keyIndex(mAvailableQualities, q);
            } else {
                if (!share)
                    playExternal(mAvailableQualities.get(q));
                else
                    playShare(mAvailableQualities.get(q));
            }
        }
    }

    private void playShare(String s) {
        if (s == null) return;
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        startActivity(Intent.createChooser(stream, getResources().getText(R.string.send_to_intent)));
    }

    public void playExternal(String s) {
        if (s == null) return;
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        getActivity().startActivity(stream);
    }

    private void showPlayDialog(final LinkedHashMap<String, String> q, int best, final boolean inApp, final boolean changeQuality, final boolean share) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String qualities[] = q.keySet().toArray(new String[q.size()]);

        builder.setTitle("Select Quality")
                .setSingleChoiceItems(qualities, best, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mQualitySelected = which;
                        if (inApp) mInAppQualitySelected = which;
                        if (inApp && !changeQuality)
                            mPlayerFragment.playStream(mAvailableQualities.get(qualities[mQualitySelected]));
                        if (inApp && changeQuality)
                            mPlayerFragment.changeQuality(mAvailableQualities.get(qualities[mQualitySelected]));
                        if (!inApp && !share)
                            playExternal(mAvailableQualities.get(qualities[mQualitySelected]));
                        if (!inApp && share)
                            playShare(mAvailableQualities.get(qualities[mQualitySelected]));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (isInLandscape())
                    hideStatusUI();
            }
        });
        builder.create();
        builder.show();
    }

    private void startAutoStream() {
        if (getActivity() == null) return;
        String autoplay[] = getResources().getStringArray(R.array.autoplay_settings);
        if (mPreferences.getString(Preferences.TWITCH_AUTOPLAY_MODE, getString(R.string.autoplay_default)).equals(autoplay[1]))
            playStream(true);
        if (mPreferences.getString(Preferences.TWITCH_AUTOPLAY_MODE, getString(R.string.autoplay_default)).equals(autoplay[2]) && !alreadyOpenedExternal) {
            playStream(false);
            alreadyOpenedExternal = true;
        }
        if (mPreferences.getString(Preferences.TWITCH_AUTOPLAY_MODE, getString(R.string.autoplay_default)).equals(autoplay[3]) && !alreadyOpenedExternal) {
            showShareDialog();
            alreadyOpenedExternal = true;
        }
    }

    public void openPlayerSettings() {
        showPlayDialog(mAvailableQualities, mInAppQualitySelected, true, true, false);
//        new VideoPlayback(getActivity(), mPlayerFragment).showInAppSettingsDialog(mAvailableQualities, 0);
    }

    public void toggleFollow() {
        if (isFollowingChannel) showUnFollowDialog();
        else followChannel();
    }

    public void showUnFollowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Unfollow " + mChannelName)
                .setMessage("Do your really want to unfollow " + mChannelName + "?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        unfollowChannel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void followChannel() {
        isFollowingChannel = true;
        mPlayerFragment.updateFollowing(true);
        if (mPlayerBlock.findViewById(R.id.followOverlay) != null)
            ((ImageView) mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_unfollow);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                TwitchNetworkTasks.followChannel(getActivity(), mChannelName);
            }
        }).start();
    }

    private void unfollowChannel() {
        isFollowingChannel = false;
        mPlayerFragment.updateFollowing(false);
        if (mPlayerBlock.findViewById(R.id.followOverlay) != null)
            ((ImageView) mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_follow);

        final ArrayList<String> ch = new ArrayList<>();
        ch.add(mChannelName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                TwitchNetworkTasks.unFollowChannels(getActivity(), ch);
            }
        }).start();
    }

    public void showShareDialog() {
        if (mPlayerFragment.isPlaying()) mPlayerFragment.pause();
        playStream(false, true);
    }

    private void startChat() {
        if (mFragments == null) return;
        if (mFragments.get(1) == null) return;
        if (!mPreferences.getBoolean(Preferences.CHAT_AUTOSTART, true)) return;
        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) return;
        if (!mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) return;

        mPager.setCurrentItem(1, true);
        if (!((ChatFragment) mFragments.get(1)).getChatStatus())
            ((ChatFragment) mFragments.get(1)).loadChat();
    }

    private void themeCastButton() {
        //if (mCastToButton == null) return;
        //AceAnims.hideFloatingButton(mCastToButton, false);
    }

    private void streamParsed(Stream s) {
        if (s == null) return;
        mStream = s;
        updateStreamOverlay();
        String picUrl = mStream.getPreviewLink().isEmpty() ?
                "null" : mStream.getPreviewLink();
        if (getActivity() == null) return;
        Picasso.with(getActivity())
                .load(picUrl)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        if (getActivity() == null) return;
                        try {
                            mPlayerOverlay.setBackground(new BitmapDrawable(getResources(), bitmap));
                        } catch (IllegalStateException ignored) {

                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });
        if (mPlayerFragment != null) {
            mPlayerFragment.updateViewers("" + mStream.getViewers());
        }
    }

    private void showInitialPlayControls() {
        if (mPlayerBlock.findViewById(R.id.playIconView) != null)
            mPlayerBlock.findViewById(R.id.playIconView).setVisibility(View.VISIBLE);
        if (mPlayerBlock.findViewById(R.id.followOverlay) != null) {
            mPlayerBlock.findViewById(R.id.followOverlay).setVisibility(View.VISIBLE);
            if (isFollowingChannel)
                ((ImageView)mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_unfollow);
            else
                ((ImageView)mPlayerBlock.findViewById(R.id.followOverlay)).setImageResource(R.drawable.ic_follow);
        }
        if (mPlayerBlock.findViewById(R.id.shareOverlay) != null)
            mPlayerBlock.findViewById(R.id.shareOverlay).setVisibility(View.VISIBLE);
    }

    private void updateStreamOverlay() {
        if (mStream == null || mPlayerBlock == null || mAvailableQualities == null) return;
        if (mPlayerBlock.findViewById(R.id.channelTitle) != null)
            ((TextView) mPlayerBlock.findViewById(R.id.channelTitle)).setText(mStream.getChannel().getDisplayName());
        if (mPlayerBlock.findViewById(R.id.channelGame) != null)
            ((TextView) mPlayerBlock.findViewById(R.id.channelGame)).setText(mStream.printGame());
        if (mPlayerBlock.findViewById(R.id.channelViewers) != null)
            ((TextView) mPlayerBlock.findViewById(R.id.channelViewers)).setText(LayoutTasks.formatNumber(mStream.getViewers()) + " Viewers");
        if (mPlayerBlock.findViewById(R.id.streamStatus) != null) {
            ((TextView) mPlayerBlock.findViewById(R.id.streamStatus)).setText(mStream.getStatus());
            mPlayerBlock.findViewById(R.id.streamStatus).getLayoutParams().width = (int) (mPlayerBlock.getMeasuredWidth() * 0.6f);
        }
    }

    private void hideStreamOverlay() {
        if (mPlayerBlock == null) return;
        mPlayerBlock.setVisibility(View.VISIBLE);
        if (mPlayerBlock.findViewById(R.id.channelTitle) != null)
            mPlayerBlock.findViewById(R.id.channelTitle).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.channelGame) != null)
            mPlayerBlock.findViewById(R.id.channelGame).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.channelViewers) != null)
            mPlayerBlock.findViewById(R.id.channelViewers).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.streamStatus) != null)
            mPlayerBlock.findViewById(R.id.streamStatus).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.playIconView) != null)
            mPlayerBlock.findViewById(R.id.playIconView).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.followOverlay) != null)
            mPlayerBlock.findViewById(R.id.followOverlay).setVisibility(View.GONE);
        if (mPlayerBlock.findViewById(R.id.shareOverlay) != null)
            mPlayerBlock.findViewById(R.id.shareOverlay).setVisibility(View.GONE);
    }

    public void updateViewersCount(final int t) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(t);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (getActivity() == null || System.currentTimeMillis() < mCountLastUpdated + t)
                    return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCountLastUpdated = System.currentTimeMillis();
                        downloadStreamData(mChannelName);
                        if (t > 5000) updateViewersCount(t);
                    }
                });
            }
        }).start();
    }


    private void hideSystemUI() {
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("IMMERS", "Turning immersive mode mode off. ");
        } else {
            Log.i("IMMERS", "Turning immersive mode mode on.");
        }

        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if (Build.VERSION.SDK_INT >= 19) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private void hideStatusUI() {
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPlayerFragment.isPlaying())
            hideStreamOverlay();

        setPlayerSize();
//        mCompactFragment.refreshLayout();
    }

    private boolean isImmersiveEnabled() {
        if (getActivity() == null) return false;
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        return ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
    }

    private boolean sysUiVisible() {
        if (getActivity() == null) return false;
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        return uiOptions == uiVisibleFlags;
    }

    private View.OnSystemUiVisibilityChangeListener fullscreenListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            if (Build.VERSION.SDK_INT >= 18) return;
            if (sysUiVisible() && !hasTabletLayout() && isInLandscape()) {
                toggleFullscreen();
            }
            if (sysUiVisible() && hasTabletLayout() && mFullScreen) {
                toggleFullscreen();
            }
        }
    };

    private void toggleFullscreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sysUiVisible() && mFullScreen)
                    hideSystemUI();
            }
        }, 3500);
    }

    private void expandFAB() {
        if (getActivity() == null) return;
        if (mPreferences == null || !mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false))
            playStream(false);

        mFabIsExpanded = true;
        ObjectAnimator rot = ObjectAnimator.ofFloat(mCastToButton, "rotation", 0, 360);
        ObjectAnimator a = ObjectAnimator.ofFloat(mExpandBackground, "alpha", 0, 1);
        a.start();
        rot.start();
        mCastToButton.setImageResource(R.drawable.ic_play_arrow);
        mExpandBackground.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mFabAdapter.updateChannel(mChannels);

        mCastToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimatorSet afab1 = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.fab_pressed);
                afab1.setTarget(mCastToButton);
                afab1.setInterpolator(new TimeInterpolator() {
                    @Override
                    public float getInterpolation(float input) {
                        return -(2*input-1f)*(2*input-1f)+1;
                    }
                });
                afab1.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        playStream(false);
                        hideExpanded();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                afab1.start();
            }
        });
    }

    public void hideExpanded() {
        mFabIsExpanded = false;
        mExpandBackground.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        ObjectAnimator rot = ObjectAnimator.ofFloat(mCastToButton, "rotation", 0, 360);
        rot.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mCastToButton.setImageResource(R.drawable.ic_face_white);
            }
        }, 120);
        mFabAdapter.clear();
        mCastToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandFAB();
            }
        });
    }

    public void goToChannel(String ch) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("channel", ch);
        startActivity(intent);
        hideExpanded();
    }

    public boolean getFABExpanded() {
        return mFabIsExpanded;
    }
}