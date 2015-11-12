package com.aceft.custom_layouts;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.aceft.adapter.IRCAdapter;
import com.aceft.data.primitives.IRCMessage;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class EmoteTarget implements Target {
    private String id;
    private IRCMessage ircMessage;
    private IRCAdapter adapter;

    public interface OnEmoteLoaded{
        void emoteLoaded(Bitmap b, String id, IRCMessage ircMessage);
    }

    public EmoteTarget(IRCAdapter adapter, String id, IRCMessage ircMessage) {
        this.id = id;
        this.ircMessage = ircMessage;
        this.adapter = adapter;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        adapter.emoteLoaded(bitmap, id, ircMessage);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }

}
