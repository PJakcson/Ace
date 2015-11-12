package com.aceft.data.async_tasks;

import android.app.Activity;

import org.json.JSONObject;

import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVodCategoryFragment;

public class TwitchOldBroadcastThread {
    private ChannelVodCategoryFragment mChannelCategoryFragment;
    private TwitchVod mVod;

    public TwitchOldBroadcastThread(ChannelVodCategoryFragment c) {
        mChannelCategoryFragment = c;
    }

    public void downloadJSONInBackground(final String url, int priority) {
        Thread mThread = new Thread(new Runnable() {
            public void run() {
                mVod = oldVods(url);
                if (getThreadActivity() == null) return;
                getThreadActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pushResult(mVod);
                    }
                });
            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private TwitchVod oldVods(String s) {
        JSONObject jData = TwitchNetworkTasks.downloadJSONData(s);
        return TwitchJSONParser.oldVideoDataToPlaylist(jData);
    }

    private void pushResult(TwitchVod result) {
        if (mChannelCategoryFragment != null) mChannelCategoryFragment.oldVideoPlaylistReceived(result);
    }

    private Activity getThreadActivity() {
        if (mChannelCategoryFragment != null) return mChannelCategoryFragment.getActivity();
        return null;
    }

}
