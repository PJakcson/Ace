package com.aceft.data.primitives;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aceft.R;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Channel {
    public HashMap<String, String> mData;
    public ArrayList<TwitchVideo> mHighlights, mBroadcasts;
    private boolean bIsOnline = false;
    private boolean bIsStarred = true;
    private int viewers = 0;

    public Channel(String name) {
        mData = new HashMap<>();
        mData.put("name", name);
    }

    public Channel(HashMap<String, String> h) {
        mData = h;
    }

    public String getLogoLink() {
        return mData.get("logo");
    }

    public String setLogoLink(String logo) {
        return mData.put("logo", logo);
    }

    public String getBannerLink() {
        return mData.get("video_banner");
    }

    public String getStatus() {
        return mData.get("status");
    }

    public String getDisplayName() {
        return mData.get("display_name");
    }

    public String getId() {
        return mData.get("_id");
    }

    public String getViews() {
        return mData.get("views");
    }

    public String getFollowers() {
        return mData.get("followers");
    }

    public String getGame() {
        return mData.get("game");
    }

    public String getName() {
        return mData.get("name");
    }

    public String getUrl() {
        return mData.get("url");
    }

    public String getMature() {
        return mData.get("mature");
    }

    public String getCreated() {
        return TwitchJSONParser.createdToDate(mData.get("created_at"));
    }

    public boolean isbIsOnline() {
        return bIsOnline;
    }

    public void setbIsOnline(boolean bIsOnline) {
        this.bIsOnline = bIsOnline;
    }

    public boolean isStarred() {
        return bIsStarred;
    }

    public void setIsStarred(boolean b) {
        this.bIsStarred = b;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Channel))
            return false;
        if (obj == this)
            return true;

        Channel rhs = (Channel) obj;
        return this.getName().equals(rhs.getName());
    }

    public int getViewers() {
        return viewers;
    }

    public void setViewers(int viewers) {
        this.viewers = viewers;
    }

    public static ArrayList<Channel> getOnlineChannels(String uname, Context context) {

        ArrayList<Channel> channels = getChannels(uname, 100, 0, context, new ArrayList<Channel>());
        ArrayList<Channel> online = checkChannelsOnlineStatus(100, 0, context, channels, new ArrayList<Channel>());
        channels.clear();

        Collections.sort(online, new CustomComparator());
        return online;
    }

    public static ArrayList<Channel> getChannels(String uname, int limit, int offset, Context c, ArrayList<Channel> ch) {
        if (c == null) return ch;
        String req = c.getString(R.string.twitch_user_url) + uname + c.getString(R.string.twitch_user_following_suffix);
        req += "limit=" + limit + "&offset=" + offset;
        String d = TwitchNetworkTasks.downloadStringData(req);

        int total = -1;
        try {
            total = new JSONObject(d).getInt("_total");
        } catch (Exception ignored) {
            return ch;
        }

        ch.addAll(TwitchJSONParser.followedChannelsToArrayList(d));
        if (offset + limit <= total)
            return getChannels(uname, limit, offset + limit, c, ch);
        else
            return ch;
    }

    public static ArrayList<Channel>  checkChannelsOnlineStatus(int limit, int offset, Context c, ArrayList<Channel> ch, ArrayList<Channel> online) {
        if (c == null || ch == null || ch.isEmpty()) return online;
        String request = c.getString(R.string.channel_stream_url);
        request += "?channel=";
        int total = ch.size();

        int last = offset + limit <= total ? offset + limit : total;
        for (int i = offset; i < last; i++) {
            request += ch.get(i).getName() + ",";
        }
        request += "&limit=" + limit;

        String j = TwitchNetworkTasks.downloadStringData(request);
        ArrayList<Stream> streams = TwitchJSONParser.streamJSONtoArrayList(j);
        updateChannelOnlineList(online, streams);
        if (offset + limit <= total)
            return checkChannelsOnlineStatus(limit, offset + limit, c, ch, online);
        else
            return online;
    }

    public static void updateChannelOnlineList(ArrayList<Channel> ch, ArrayList<Stream> streams) {
        Channel c;
        for (Stream str : streams) {
            c = str.getChannel();
            c.setbIsOnline(true);
            c.setViewers(str.getViewers());
            ch.add(c);
        }
    }

    public static class CustomComparator implements Comparator<Channel> {
        @Override
        public int compare(Channel lhs, Channel rhs) {
            if (lhs.isbIsOnline() && rhs.isbIsOnline()) {
                if (lhs.getViewers() == rhs.getViewers())
                    return lhs.getName().compareTo(rhs.getName());
                return lhs.getViewers() < rhs.getViewers() ? 1 : -1;
            }
            if (lhs.isbIsOnline()) return -1;
            if (rhs.isbIsOnline()) return 1;
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}