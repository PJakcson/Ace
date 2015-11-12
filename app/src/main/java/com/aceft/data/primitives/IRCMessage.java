package com.aceft.data.primitives;

import android.graphics.Bitmap;
import android.text.SpannableString;
import android.text.Spanned;

import java.util.ArrayList;

public class IRCMessage {
    private String displayName, userType;
    private int color;
    private int subscriber, turbo;
    private String messageText;
    private CharSequence formattedText;
    private ArrayList<Emoticon> emotes;

    public IRCMessage(int color, String displayName, int subscriber, int turbo, ArrayList<Emoticon> emotes, String userType, String message) {
        this.color = color;
        this.displayName = displayName;
        this.subscriber = subscriber;
        this.turbo = turbo;
        this.emotes = emotes;
        this.userType = userType;
        this.messageText = message;
    }

    public IRCMessage(IRCMessage m, CharSequence formattedText) {
        this.color = m.getColor();
        this.displayName = m.getDisplayName();
        this.subscriber = m.getSubscriber();
        this.turbo = m.getTurbo();
        this.emotes = m.getEmotes();
        this.userType = m.getUserType();
        this.messageText = m.getMessageText();
        this.formattedText = formattedText;
    }

    public void setBitmap(Bitmap b, String id) {
        for (Emoticon e : emotes) {
            if (id.equals(e.getId())) {
                e.setEmoti(b);
            }
        }
    }

    public boolean emotesReady() {
        for (Emoticon e : emotes) {
            if (!e.isReady()) return false;
        }
        return true;
    }


    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUserType() {
        return userType;
    }

    public int getTurbo() {
        return turbo;
    }

    public int getSubscriber() {
        return subscriber;
    }

    public ArrayList<Emoticon> getEmotes() {
        return emotes;
    }

    public String getMessageText() {
        return messageText;
    }

    public CharSequence getFormattedText() {
        return formattedText;
    }

    public void setFormattedText(Spanned formattedText) {
        this.formattedText = formattedText;
    }
}
