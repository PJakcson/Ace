package com.aceft.adapter;

import android.content.Context;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aceft.custom_layouts.EmoteTarget;
import com.aceft.data.primitives.Emoticon;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class EmotisAdapter extends RecyclerView.Adapter<EmotisAdapter.ViewHolder> {
    private ArrayList<Emoticon> mEmotis;

    public EmotisAdapter() {
        mEmotis = new ArrayList<>();
    }

    public void update(ArrayList<Emoticon> e) {
        if (e == null) return;
        mEmotis = e;
        notifyDataSetChanged();
    }

    @Override
    public EmotisAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        layoutParams.setMargins(20,20,20,20);
        imageView.setLayoutParams(layoutParams);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(EmotisAdapter.ViewHolder holder, int position) {
        Picasso.with(holder.context)
                .load("http://static-cdn.jtvnw.net/emoticons/v1/" + mEmotis.get(position).getId() + "/1.0")
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mEmotis.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public Context context;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
            context = itemView.getContext();
        }
    }
}
