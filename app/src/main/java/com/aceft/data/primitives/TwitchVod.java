package com.aceft.data.primitives;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TwitchVod {
    private String channel, previewLink;
    private int duration, startOffset, endOffset, playOffset;
    private ArrayList<TwitchVodFileOld> video;

    public TwitchVod() {
        video = new ArrayList<>();
    }

    public Integer bestPossibleUrl() {
        int bestQual = -1;
        int bestIndex = -1;

        for (int i = 0; i < video.size(); i++) {
            if (quality(video.get(i).getQuality()) > bestQual) {
                bestQual = quality(video.get(i).getQuality());
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int quality(String s) {
        if (s.contains("240")) return 0;
        if (s.contains("360")) return 1;
        if (s.contains("480")) return 2;
        if (s.contains("720")) return 3;
        if (s.contains("live")) return 4;
        if (s.contains("source")) return 4;
        if (s.contains("chunked")) return 4;
        return -1;
    }

    public ArrayList<String> getAvailableQualities() {
        ArrayList <String> q = new ArrayList<>();
        for (TwitchVodFileOld t: video) {
           q.add(t.getQuality());
        }
        return q;
    }

    public LinkedHashMap<String, String> toHashmap() {
        LinkedHashMap<String, String> q = new LinkedHashMap<>();
        int combinedLength;
        for (TwitchVodFileOld t: video) {
            combinedLength = 0;
            for (int i = 0; i < t.getVideo().size(); i++) {
                if(combinedLength > endOffset) continue;
                combinedLength += Integer.valueOf(t.getVideo().get(i).getLength());
                if (startOffset > combinedLength) continue;
                q.put(t.getQuality() + i, t.getVideo().get(i).getUrl());
            }
        }
        return q;
    }

    public ArrayList<String> getLengths() {
        ArrayList<String> q = new ArrayList<>();
        TwitchVodFileOld  t = video.get(0);
        TwitchVodFile tf;
        int combinedLength = 0;
        for (int i = 0; i < t.getVideo().size(); i++) {
            tf = t.getVideo().get(i);
            if(combinedLength > endOffset) continue;
            combinedLength += Integer.valueOf(tf.getLength());
            if (startOffset > combinedLength) continue;
            q.add(tf.getLength());
        }
        return q;
    }

    public ArrayList<String> getQualities() {
        ArrayList<String> q = new ArrayList<>();
        TwitchVodFileOld  t = video.get(0);
        TwitchVodFile tf;
        for (int i = 0; i < t.getVideo().size(); i++) {
            tf = t.getVideo().get(i);
            q.add(tf.getLength());
        }
        return q;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setPreviewLink(String previewLink) {
        this.previewLink = previewLink;
    }

    public ArrayList<TwitchVodFileOld> getVideo() {
        return video;
    }

    public void setVideo(ArrayList<TwitchVodFileOld> video) {
        this.video = video;
    }

    public void setStartOffset(int start_offset) {
        this.startOffset = start_offset;
    }

    public void setEndOffset(int end_offset) {
        this.endOffset = end_offset;
    }

    public void setPlayOffset(int play_offset) {
        this.playOffset = play_offset;
    }

    public int getStartOffsetIndex() {
        TwitchVodFileOld t = video.get(0);
        TwitchVodFile tf;
        int combinedLength = 0;
        int startOffsetIndex = 0;
        for (int i = 0; i < t.getVideo().size(); i++) {
            tf = t.getVideo().get(i);
            combinedLength += Integer.valueOf(tf.getLength());
            if (startOffset <= combinedLength)
                return i;

        }
        return startOffsetIndex;
    }
}
