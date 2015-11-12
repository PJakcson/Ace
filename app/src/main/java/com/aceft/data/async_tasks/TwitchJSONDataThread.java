package com.aceft.data.async_tasks;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.aceft.ui_fragments.channel_fragments.ChatFragment;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVodCategoryFragment;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVideoOverviewFragment;
import com.aceft.ui_fragments.front_pages.FollowedListFragment;
import com.aceft.ui_fragments.front_pages.GamesRasterFragment;
import com.aceft.ui_fragments.front_pages.SearchFragment;
import com.aceft.ui_fragments.front_pages.StreamListFragment;

public class TwitchJSONDataThread {
    private ChatFragment mChatFragment;
    private SearchFragment mSearchFragment;
    private ChannelVideoOverviewFragment mVideoOverviewFragment;
    private ChannelVodCategoryFragment mVideoCategoryFragment;
    private StreamListFragment mStreamListFragment;
    private GamesRasterFragment mGamesRasterFragment;
    private FollowedListFragment mFavoritesListFragment;
    private int mRequestType, mSearchRequestType;

    public TwitchJSONDataThread(FollowedListFragment c) {
        mFavoritesListFragment = c;
    }

    public TwitchJSONDataThread(GamesRasterFragment g) {
        mGamesRasterFragment = g;
    }

    public TwitchJSONDataThread(StreamListFragment s) {
        mStreamListFragment = s;
    }
    public TwitchJSONDataThread(ChatFragment s) {
        mChatFragment = s;
    }

    public TwitchJSONDataThread(ChannelVideoOverviewFragment s, int request_type) {
        mVideoOverviewFragment = s;
        mRequestType = request_type;
    }

    public TwitchJSONDataThread(ChannelVodCategoryFragment s, int request_type) {
        mVideoCategoryFragment = s;
        mRequestType = request_type;
    }

    public TwitchJSONDataThread(SearchFragment s, int request_type) {
        mSearchFragment = s;
        mSearchRequestType = request_type;
    }

    public void downloadJSONInBackground(String s, int priority) {
        final String fUrl = s;
        Thread mThread = new Thread(new Runnable() {
            public void run() {
                final String is = downloadJSONData(fUrl);
                if (getThreadActivity() == null) return;
                getThreadActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pushResult(is);
                    }
                });
            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private Activity getThreadActivity() {
        if (mFavoritesListFragment != null) return mFavoritesListFragment.getActivity();
        if (mGamesRasterFragment != null) return mGamesRasterFragment.getActivity();
        if (mStreamListFragment != null) return mStreamListFragment.getActivity();
        if (mSearchFragment != null) return mSearchFragment.getActivity();
        if (mVideoOverviewFragment != null) return mVideoOverviewFragment.getActivity();
        if (mVideoCategoryFragment != null) return mVideoCategoryFragment.getActivity();
        if (mChatFragment != null) return mChatFragment.getActivity();
        return null;
    }

    private void pushResult(String s) {
//        if (mFavoritesListFragment != null) mFavoritesListFragment.dataReceived(s);
        if (mGamesRasterFragment != null) mGamesRasterFragment.dataReceived(s);
        if (mStreamListFragment != null) mStreamListFragment.dataReceived(s);

        if (mVideoOverviewFragment != null && mRequestType == 0) mVideoOverviewFragment.channelDataReceived(s);
        if (mVideoOverviewFragment != null && mRequestType == 1) mVideoOverviewFragment.userDataReceived(s);
        if (mVideoOverviewFragment != null && mRequestType == 2) mVideoOverviewFragment.followDataReceived(s);

        if (mVideoCategoryFragment != null && mRequestType == 0) mVideoCategoryFragment.highlightDataReceived(s);
        if (mVideoCategoryFragment != null && mRequestType == 1) mVideoCategoryFragment.broadcastDataReceived(s);

        if (mSearchFragment != null && mSearchRequestType == 0) mSearchFragment.channelDataReceived(s);
        if (mSearchFragment != null && mSearchRequestType == 1) mSearchFragment.gameDataReceived(s);

        if (mChatFragment != null) mChatFragment.emotisDataReceived(s);
    }

    private String downloadJSONData(String myurl) {
        InputStream is = null;
        String result;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
            result = sb.toString();
            return result;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
