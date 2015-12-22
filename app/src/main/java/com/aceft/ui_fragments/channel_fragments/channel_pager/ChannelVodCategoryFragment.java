package com.aceft.ui_fragments.channel_fragments.channel_pager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.VideoAdapter;
import com.aceft.data.LiveMethods;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.VideoPlayback;
import com.aceft.data.async_tasks.TwitchBroadcastThread;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.async_tasks.TwitchOldBroadcastThread;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;

import java.util.LinkedHashMap;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class ChannelVodCategoryFragment extends Fragment {

    private SharedPreferences mPreferences;
    onOldVideoSelectedListener mCallback;

    private GridView mVideoGrid;
    private VideoAdapter mVideoGridAdapter;
    private ProgressBar mProgressBar;
    private TwitchVideo mPlayingVideo;
    private int mLoadedItems, INT_GRID_UPDATE_VALUE, INT_GRID_UPDATE_THRESHOLD;

    private boolean adIsOnTop = false;

    public interface onOldVideoSelectedListener {
        void onOldVideoSelected(TwitchVod t1, TwitchVideo t2);
    }

    public ChannelVodCategoryFragment newInstance(String name, int type) {
        ChannelVodCategoryFragment fragment = new ChannelVodCategoryFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", name);
        args.putInt("video_type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_category, container, false);
        mVideoGrid = (GridView) rootView.findViewById(R.id.videoGrid);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mVideoGridAdapter = new VideoAdapter(getActivity());

        final int videoType = getArguments().getInt("video_type");

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        mVideoGrid.setAdapter(mVideoGridAdapter);

        mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
        INT_GRID_UPDATE_VALUE = getResources().getInteger(R.integer.game_grid_update_items);
        INT_GRID_UPDATE_THRESHOLD = getResources().getInteger(R.integer.game_grid_update_threshold);

        if (videoType == 0) {
            downloadHighlightData(mLoadedItems,0);
        }
        if (videoType == 1) {
            downloadBroadcastData(mLoadedItems, 0);
        }

        mVideoGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSelectedVideo(mVideoGridAdapter.getItem(position));
            }
        });

        mVideoGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (lastVisibleItem >= mLoadedItems - INT_GRID_UPDATE_THRESHOLD) {
                    if (videoType == 0) {
                        downloadHighlightData(INT_GRID_UPDATE_VALUE,mLoadedItems);
                    }
                    if (videoType == 1) {
                        downloadBroadcastData(INT_GRID_UPDATE_VALUE, mLoadedItems);
                    }
                    mLoadedItems += INT_GRID_UPDATE_VALUE;
                }
            }
        });

        return rootView;
    }

    //------------------ Highlight Stuff -------------------------///////////////////////////////////////////

    public void downloadHighlightData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 0);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void highlightDataReceived(String s) {
        if (mVideoGridAdapter != null) {
            mVideoGridAdapter.updateVideos(TwitchJSONParser.dataToVideoList(s));
        }
        mVideoGrid.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    //------------------ PastBroadcast Stuff -------------------------/////////////////////////////////////////////

    public void downloadBroadcastData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += getString(R.string.channel_broadcasts_url_appendix);
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 1);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void broadcastDataReceived(String s) {
        if (mVideoGridAdapter.getCount() == 0) {
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mVideoGrid, "alpha",  0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        }
        if (mVideoGridAdapter != null) {
            mVideoGridAdapter.updateVideos(TwitchJSONParser.dataToVideoList(s));
        }
        mVideoGrid.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    //-------------------VideoPlayback------------------------------//////////////////////////////////////

    public void playSelectedVideo(TwitchVideo v) {
        mPlayingVideo = v;
        if (v == null) {
            Toast.makeText(getActivity(), "Could not load Video", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAuthenticated = mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);

        String userToken = "";
//                userScope = "";
        if (isAuthenticated) {
            userToken = mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");
//            userScope = mPreferences.getString(Preferences.SCOPES_OF_USER, "");
        }

        mProgressBar.setVisibility(View.VISIBLE);
        String prefix = v.mId.substring(0,1);
        String suffix = v.mId.substring(1, v.mId.length());
        String request;
        TwitchOldBroadcastThread to;
        switch (prefix) {
            case "v":
                TwitchBroadcastThread t = new TwitchBroadcastThread(this);
                if (isAuthenticated)
                    t = new TwitchBroadcastThread(this, userToken);
                request = getString(R.string.twitch_video_token_url) + suffix + "/access_token";
                t.downloadJSONInBackground(request, suffix, 0, Thread.NORM_PRIORITY);
                break;
            case "a":
                to = new TwitchOldBroadcastThread(this);
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (isAuthenticated)
                    request += "&oauth_token=" + userToken;
                to.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
                break;
            case "c":
                to = new TwitchOldBroadcastThread(this);
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (isAuthenticated)
                    request += "&oauth_token=" + userToken;
                to.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
                break;
        }
    }

    public void videoPlaylistReceived(LinkedHashMap<String, String> result) {
        mProgressBar.setVisibility(View.INVISIBLE);
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


    public void oldVideoPlaylistReceived(TwitchVod t) {
        mProgressBar.setVisibility(View.INVISIBLE);
        if (t.bestPossibleUrl() >= 0) {
            showOldVodFragment(t);
        } else {
            Toast.makeText(getActivity(), "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOldVodFragment(TwitchVod t) {
        mCallback.onOldVideoSelected(t, mPlayingVideo);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onOldVideoSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
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