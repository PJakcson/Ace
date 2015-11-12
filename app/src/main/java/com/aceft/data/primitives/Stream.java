package com.aceft.data.primitives;

public class Stream {
    private String mPreviewLink, mUrl, mGame;
    private int mViewers, mId;
    private Channel mChannel;

    public Stream(String name) {
        mChannel = new Channel(name);
    }

    public Stream(String curl, String game, int viewers, String preview, int id, Channel channel) {
        mUrl = curl;
        mViewers = viewers;
        mGame = game;
        mId = id;
        mPreviewLink = preview;
        mChannel = channel;
    }

    public String printGame() {
        return "playing " + mGame;
    }

    public Channel getChannel() {
        return mChannel;
    }

    public String getPreviewLink() {
        return mPreviewLink;
    }

    public String getStatus() {
        if (mChannel == null) return "";
        return mChannel.getStatus();
    }

    public String getTitle() {
        if (mChannel == null) return "";
        return mChannel.getDisplayName();
    }

    public String getName() {
        if (mChannel == null) return "";
        return mChannel.getName();
    }

    public String getUrl() {
        return mUrl;
    }

    public int getId() {
        return mId;
    }

    public int getViewers() {
        return mViewers;
    }

    @Override
    public String toString() {
        return getTitle()+ " spielt " + mGame + " mit " + mViewers + " Zuschauern";
    }
}