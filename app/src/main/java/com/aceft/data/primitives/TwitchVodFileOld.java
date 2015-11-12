package com.aceft.data.primitives;

import java.util.ArrayList;

public class TwitchVodFileOld {
    private String quality;
    private ArrayList<TwitchVodFile> video;

    public TwitchVodFileOld() {
        video = new ArrayList<>();
        quality = "";
    }

     public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public ArrayList<TwitchVodFile> getVideo() {
        return video;
    }

    public void setVideo(ArrayList<TwitchVodFile> video) {
        this.video = video;
    }
}
