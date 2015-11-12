package com.aceft.data.primitives;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Game {
    public String mTitle, mThumbnail, mId;
    public int mViewers, mChannelCount;
    public ArrayList<Stream> mStreams;

    public Game(String title, String thumb, int viewers, int channelc, String id, ArrayList<Stream> streams) {
        mTitle = title;
        mThumbnail = thumb;
        mViewers = viewers;
        mChannelCount = channelc;
        mId = id;
        mStreams = streams;
    }

    public String toURL() {
        String url = "";
        try {
            url = URLEncoder.encode(mTitle, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Game other = (Game) obj;
        return this.mId.equals(other.mId);
    }

    @Override
    public String toString() {
        String s = "";
        if (this.mTitle != null) {
            s = "Titel: " + mTitle + ", Zuschauer: " + mViewers + ", Anzahl: " + mChannelCount + " Kanäle: \n";
            if (this.mStreams != null) {
                for (int i = 0; i < mStreams.size(); i++) {
                    s += mStreams.get(i).toString() + "\n";
                }
            }
        }
        return s;
    }
}
