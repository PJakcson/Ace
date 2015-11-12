package com.aceft.data.async_tasks;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

import com.aceft.data.TwitchNetworkTasks;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVodCategoryFragment;

public class TwitchBroadcastThread {
    private ChannelVodCategoryFragment mChannelCategoryFragment;
    private LinkedHashMap<String, String> mStreamUrls;
    private boolean mIsAuthenticated;
    private String mUserToken;

    public TwitchBroadcastThread(ChannelVodCategoryFragment c) {
        mChannelCategoryFragment = c;
    }

    public TwitchBroadcastThread(ChannelVodCategoryFragment c, String token) {
        mChannelCategoryFragment = c;
        mIsAuthenticated = true;
        mUserToken = token;
    }

    public void downloadJSONInBackground(final String tokenUrl, final String videoId, final int requestType, int priority) {
        Thread mThread = new Thread(new Runnable() {
            public void run() {
                if (requestType == 0) mStreamUrls = liveStreamUrls(tokenUrl, videoId);
                if (getThreadActivity() == null) return;
                getThreadActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pushResult(mStreamUrls, requestType);
                    }
                });

            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private void pushResult(LinkedHashMap<String, String> result, int req) {
         if (mChannelCategoryFragment != null && req == 0) mChannelCategoryFragment.videoPlaylistReceived(result);
    }

    private Activity getThreadActivity() {
        if (mChannelCategoryFragment != null) return mChannelCategoryFragment.getActivity();
        return null;
    }

    private LinkedHashMap<String, String> liveStreamUrls(String s, String id) {
        LinkedHashMap<String, String> videoPlaylist;
        JSONObject jToken = TwitchNetworkTasks.downloadJSONData(s);
        try {
            String token = jToken.getString("token");
            String sig = jToken.getString("sig");

            String m3u8Url = "http://usher.twitch.tv/vod/" + id + "?nauth=";
            m3u8Url += token + "&nauthsig=" + sig;
            if (mIsAuthenticated) m3u8Url += "&oauth_token=" + mUserToken;

            videoPlaylist = TwitchNetworkTasks.fetchTwitchPlaylist(m3u8Url);
            return videoPlaylist;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
