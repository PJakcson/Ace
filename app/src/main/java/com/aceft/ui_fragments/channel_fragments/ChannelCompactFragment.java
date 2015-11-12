package com.aceft.ui_fragments.channel_fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.PlayerActivity;
import com.aceft.R;
import com.aceft.adapter.CompactGridAdapter;
import com.aceft.custom_layouts.FullscreenGridLayoutManager;
import com.aceft.data.LiveMethods;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.VideoPlayback;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;
import com.aceft.data.primitives.TwitchUser;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class ChannelCompactFragment extends Fragment {
    private final static int IS_HEADER = 0;
    private final static int IS_HIGHLIGHT_HEADER = 1;
    private final static int IS_HIGHLIGHT = 2;
    private final static int IS_BROADCAST_HEADER = 3;
    private final static int IS_BROADCAST = 4;
    private final static int IS_CHAT = 5;

    LinkedHashMap<String, String> mAvailableQualities;
    HashMap<String, String> mData;
    private int mLoadedItems;
    private ImageView mPlayOverlay;
    private ProgressBar mProgressBar;
    private AdapterView.OnItemClickListener mVideoClicked;

    private RelativeLayout mStreamView;
    private Channel mChannel;
    private Stream mStream;
    private TwitchUser mUser;

    private String mUserToken;
    private boolean mIsAuthenticated;
    private int mRootWidth = 0;

    private ImageView mThumbnail, mChannelBanner;
    private RecyclerView mVideoList;
    private CompactGridAdapter mAdapter;
    private View mStreamHeader;
    private View mChannelHeader;
    private TwitchVideo mPlayingVideo;
    private SharedPreferences mPreferences;
    private View mChatHeader;
    private boolean highlightsLoaded, broadcastLoaded, channelLoaded, userLoaded;

    private boolean adIsOnTop = false;
    private View.OnClickListener mStreamClicked;
    private String mChannelName;
    private View rootView;
    private OnItemSelectedListener mCallback;
    private int mQualitySelected;
    private RecyclerView mRecyclerView;
    private FullscreenGridLayoutManager mLayoutManager;

    public interface OnItemSelectedListener {
        void onChannelPagerSelected(Channel c, int page);

        void startFullScreenChat(Channel c);

        void oldVodSelected(TwitchVod twitchVod, TwitchVideo twitchVideo);
    }

    public ChannelCompactFragment newInstance(String c) {
        ChannelCompactFragment fragment = new ChannelCompactFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_channel_compact, container, false);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mIsAuthenticated = sp.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);

        if (mIsAuthenticated) {
            mUserToken = sp.getString(Preferences.USER_AUTH_TOKEN, "");
        }

        mChannelName = getArguments().getString("channel_name");
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channel_detail_progress);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        if (getActivity() == null) return rootView;
        mLayoutManager = new FullscreenGridLayoutManager(getActivity(), 1);
//        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                return position == 0 ? spanCount : 1;
//            }
//        });
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CompactGridAdapter(1, this);
        mAdapter.SetOnItemClickListener(new CompactGridAdapter.OnItemClickListener() {
            @Override
            public void onMainHeaderClick() {
                if (getActivity() == null) return;
                mCallback.onChannelPagerSelected(mChannel, 0);
            }

            @Override
            public void onVideoItemClick(int position) {
                int group = mAdapter.getItemViewType(position);
                int childPos = mAdapter.getArrayIndex(position);
                switch (group) {
                    case IS_CHAT:
                        if (getActivity() == null) return;
                        mCallback.startFullScreenChat(mChannel);
                        break;
                    case IS_BROADCAST_HEADER:
                        if (getActivity() == null) return;
                        mCallback.onChannelPagerSelected(mChannel, 1);
                        break;
                    case IS_BROADCAST:
                        playSelectedVideo(mAdapter.getBroadcast(childPos));
                        break;
                    case IS_HIGHLIGHT_HEADER:
                        if (getActivity() == null) return;
                        mCallback.onChannelPagerSelected(mChannel, 2);
                        break;
                    case IS_HIGHLIGHT:
                        playSelectedVideo(mAdapter.getHighlight(childPos));
                        break;
                }
            }

            @Override
            public void onChatClick() {
                mCallback.startFullScreenChat(mChannel);
            }
        });

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (rootView.getMeasuredWidth() != 0) {
                    mRootWidth = rootView.getMeasuredWidth();
                    mAdapter.updateWidth(mRootWidth);
                }
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        mChannelHeader = getActivity().getLayoutInflater().inflate(R.layout.header_compact_channel, null);
        mChatHeader = getActivity().getLayoutInflater().inflate(R.layout.header_compact_chat, null);

        mLoadedItems = 8;
        downloadUserData(getArguments().getString("channel_name"));

        return rootView;
    }

    public void refreshLayout() {
//        if (rootView.getMeasuredWidth() != 0) {
//            mRootWidth = rootView.getMeasuredWidth();
//            mAdapter.updateWidth(mRootWidth);
//        }
        mAdapter.refresh();
    }

    //------------------ Channel Stuff -------------------------///////////////////
    private void downloadChannelData() {
        if (getActivity() == null) return;
        String request = getActivity().getResources().getString(R.string.channel_url);
        request += getArguments().getString("channel_name");
        if (mIsAuthenticated) request += "?oauth_token=" + mUserToken;
        downloadStringDataThread(request, 0);
    }

    public void channelDataReceived(String s) {
        mChannel = TwitchJSONParser.channelStringToChannel(s);
        if (mChannel == null) return;

        mAdapter.updateChannel(mChannel, mUser);
        makeVisible();
        downloadBroadcastData(mLoadedItems, 0);
        downloadHighlightData(mLoadedItems, 0);
    }

    private void updateChannelLayout() {
//        String picUrl = mChannel.getLogoLink().isEmpty() ?
//                "null" : mChannel.getLogoLink();
//        Picasso.with(getActivity())
//                .load(picUrl)
//                .placeholder(R.drawable.ic_placeholder)
//                .config(Bitmap.Config.RGB_565)
//                .into(mChannelBanner);
//
//        ((TextView) mChannelHeader.findViewById(R.id.textTitleView)).setText(mChannel.getDisplayName());
//        ((TextView) mChannelHeader.findViewById(R.id.textBioView)).setText(mUser.getBio());
//        ((TextView) mChannelHeader.findViewById(R.id.textViewsView)).setText(mChannel.getFollowers() + " Followers");

    }

    private void makeVisible() {
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        fadeInView(mRecyclerView);
        if (highlightsLoaded && broadcastLoaded) {

        }
    }

    //------------------ User Stuff -------------------------///////////////////
    private void downloadUserData(String name) {
        if (getActivity() == null) return;
        String request = getActivity().getResources().getString(R.string.twitch_user_url);
        request += name;
        downloadStringDataThread(request, 1);
    }

    public void userDataReceived(String s) {
        mUser = TwitchJSONParser.userDataToUser(s);
        if (mUser == null) return;
        downloadChannelData();
    }

    //------------------ Video Stuff -------------------------///////////////////////////////////////////
    public void playSelectedVideo(TwitchVideo v) {
        mPlayingVideo = v;
        if (v == null) {
            Toast.makeText(getActivity(), "Could not load Video", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        String suffix = v.mId.substring(1, v.mId.length());
        String request;

        if (TwitchNetworkTasks.whichVodType(v) == 0) {
            request = getString(R.string.twitch_video_token_url) + suffix + "/access_token";
            downloadVODDataThread(request, suffix);
        } else {
            request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
            if (mIsAuthenticated)
                request += "&oauth_token=" + mUserToken;
            downloadOldVODDataThread(request);
        }
    }

    private void downloadVODDataThread(final String s, final String id) {
        if (getActivity() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject result = TwitchNetworkTasks.downloadJSONData(s);
                String tokSig[] = TwitchJSONParser.tokenSigToStringArray(result);

                if (tokSig == null) return;
                if (tokSig.length < 2) return;
                if (tokSig[0] == null || tokSig[1] == null) return;

                String m3u8Url = "http://usher.twitch.tv/vod/" + id + "?nauth=";
                m3u8Url += tokSig[0] + "&nauthsig=" + tokSig[1];
                if (mIsAuthenticated) m3u8Url += "&oauth_token=" + mUserToken;

                final LinkedHashMap<String, String> videoPlaylist = TwitchNetworkTasks.fetchTwitchPlaylist(m3u8Url);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        videoPlaylistReceived(videoPlaylist);
                    }
                });
            }
        }).start();
    }

    public void videoPlaylistReceived(LinkedHashMap<String, String> result) {
        if (getActivity() == null) return;
        if (result.isEmpty())
            Toast.makeText(getActivity(), "Could not load the vod. Maybe you need to subscribe to the channel.", Toast.LENGTH_LONG).show();
        mProgressBar.setVisibility(View.INVISIBLE);
//        new VideoPlayback(getActivity()).playVideo(result);
        playStream(result);
    }

    private void playStream(LinkedHashMap<String, String> result) {
        String q = LiveMethods.getQualityKey(getActivity(), result);
//        final String qualities[] = result.keySet().toArray(new String[result.size()]);
        VideoPlayback p = new VideoPlayback(getActivity());

        if (q.contains("showDialog")) {
            int selQualtity = Integer.parseInt(q.substring(q.length() - 1));
            p.showPlayDialog(result, selQualtity);
        } else {
            new VideoPlayback(getActivity()).playStream(result.get(q));
        }
    }

    private void downloadOldVODDataThread(final String s) {
        if (getActivity() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jData = TwitchNetworkTasks.downloadJSONData(s);
                final TwitchVod result = TwitchJSONParser.oldVideoDataToPlaylist(jData);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        oldVideoPlaylistReceived(result);
                    }
                });
            }
        }).start();
    }

    public void oldVideoPlaylistReceived(TwitchVod t) {
        mProgressBar.setVisibility(View.INVISIBLE);
        if (t.bestPossibleUrl() >= 0) {
            showOldVodFragment(t);
        } else {
            Toast.makeText(getActivity(), "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOldVodFragment(TwitchVod t) {
        ((PlayerActivity) getActivity()).oldVodSelected(t, mPlayingVideo);
    }

    //------------------ Highlight Stuff -------------------------///////////////////////////////////////////

    public void downloadHighlightData(int limit, int offset) {
        if (getActivity() == null) return;
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += "limit=" + limit + "&offset=" + offset;
        downloadStringDataThread(request, 2);
    }

    public void highlightDataReceived(String s) {
//        downloadBroadcastData(mLoadedItems, 0);
        mChannel.mHighlights = TwitchJSONParser.dataToVideoList(s);
        if (mAdapter != null && mChannel.mHighlights != null) {
            mAdapter.updateHighlights(mChannel.mHighlights);
        }
        highlightsLoaded = true;
//        makeVisible();
    }

    //------------------ PastBroadcast Stuff -------------------------/////////////////////////////////////////////

    public void downloadBroadcastData(int limit, int offset) {
        if (getActivity() == null) return;
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += getString(R.string.channel_broadcasts_url_appendix);
        request += "limit=" + limit + "&offset=" + offset;
        downloadStringDataThread(request, 3);
    }

    public void broadcastDataReceived(String s) {
        mChannel.mBroadcasts = TwitchJSONParser.dataToVideoList(s);
        if (mAdapter != null && mChannel.mBroadcasts != null) {
            mAdapter.updateBroadcasts(mChannel.mBroadcasts);
        }
        broadcastLoaded = true;
    }


    //------------------------------- Threads ---------------------------//////////////////////////////
    private void downloadStringDataThread(final String s, final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = TwitchNetworkTasks.downloadStringData(s);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (type) {
                            case 0:
                                channelDataReceived(result);
                                break;
                            case 1:
                                userDataReceived(result);
                                break;
                            case 2:
                                highlightDataReceived(result);
                                break;
                            case 3:
                                broadcastDataReceived(result);
                                break;
                        }

                    }
                });
            }
        }).start();
    }

    //------------------------------- Stuff ---------------------------//////////////////////////////

    @Override
    public void onResume() {
        super.onResume();
    }

    private void fadeInView(View v) {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("data", mData);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnItemSelectedListener) activity;
        } catch (ClassCastException ignored) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}