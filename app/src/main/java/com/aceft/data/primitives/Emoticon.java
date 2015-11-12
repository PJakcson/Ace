package com.aceft.data.primitives;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Emoticon {
    private String id, url;
    private Bitmap emoti;
    private int regexLength;
    private ArrayList<Integer> startIndices;
    private boolean ready;

    public Emoticon(String id, int regexLength, ArrayList<Integer> startIndices) {
        this.id = id;
        this.regexLength = regexLength;
        this.startIndices = startIndices;
        this.url = "http://static-cdn.jtvnw.net/emoticons/v1/" + id + "/1.0";
    }

    public String getUrl() {
        return url;
    }

    public Bitmap getEmoti() {
        return emoti;
    }

    public void setEmoti(Bitmap emoti) {
        this.emoti = emoti;
        ready = true;
    }

    public int getRegexLength() {
        return regexLength;
    }

    public ArrayList<Integer> getStartIndices() {
        return startIndices;
    }

    public String getId() {
        return id;
    }

    public boolean isReady() {
        return ready;
    }
}