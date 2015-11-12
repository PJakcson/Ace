package com.aceft.data.primitives;

public class TwitchVodFile {
    private String url, length;

    public TwitchVodFile(String u, String l) {
        url = u;
        length = l;
    }

    public String getUrl() {
        return url;
    }

    public String getLength() {
        return length;
    }
}
