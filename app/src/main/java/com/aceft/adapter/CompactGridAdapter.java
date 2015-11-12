package com.aceft.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aceft.R;
import com.aceft.data.CircleTransform;
import com.aceft.data.LayoutTasks;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.TwitchUser;
import com.aceft.data.primitives.TwitchVideo;
import com.sorcix.sirc.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class CompactGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Fragment mFragment;
    private Channel mChannel;
    private TwitchUser mUser;
    private RelativeLayout.LayoutParams mHeaderLayout, mVideoLayout;
    private int mSpanCount;
    private int mWidth = -1;
    OnItemClickListener mItemClickListener;

    private ArrayList<Integer> mEntryTypes;
    private ArrayList<TwitchVideo> mHighlights;
    private ArrayList<TwitchVideo> mBroadcasts;

    private final static int IS_HEADER = 0;
    private final static int IS_HIGHLIGHT_HEADER = 1;
    private final static int IS_HIGHLIGHT = 2;
    private final static int IS_BROADCAST_HEADER = 3;
    private final static int IS_BROADCAST = 4;
    private final static int IS_CHAT = 5;

    public CompactGridAdapter(int spanCount, Fragment fragment) {
        mEntryTypes = new ArrayList<>();
        mHighlights = new ArrayList<>();
        mBroadcasts = new ArrayList<>();
        mSpanCount = spanCount;
        mFragment = fragment;
    }

    public void updateChannel(Channel channel, TwitchUser user) {
        mChannel = channel;
        mUser = user;
//        mEntryTypes.add(IS_CHAT);
        mEntryTypes.add(IS_HEADER);
        notifyDataSetChanged();
    }

    public void updateBroadcasts(ArrayList <TwitchVideo> c) {
        if (c == null || c.isEmpty()) return;
        if (mBroadcasts == null) mBroadcasts = new ArrayList<>();

        mEntryTypes.add(1, IS_BROADCAST_HEADER);
        for (int i = 0; i < c.size(); i++)
            mEntryTypes.add(2 + i, IS_BROADCAST);

        mBroadcasts.addAll(c);
        notifyDataSetChanged();
    }

    public void updateHighlights(ArrayList <TwitchVideo> c) {
        if (c == null || c.isEmpty()) return;
        if (mHighlights == null) mHighlights = new ArrayList<>();

        mEntryTypes.add(IS_HIGHLIGHT_HEADER);
        for (int i = 0; i < c.size(); i++)
            mEntryTypes.add(IS_HIGHLIGHT);

        mHighlights.addAll(c);
        notifyDataSetChanged();
    }

    public void refresh() {
        ArrayList<Integer> copy = (ArrayList<Integer>) mEntryTypes.clone();
        mEntryTypes = new ArrayList<>();
        notifyDataSetChanged();
        mEntryTypes = copy;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onMainHeaderClick();
        void onVideoItemClick(int position);
        void onChatClick();
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView videoDuration;

        public VideoViewHolder(View itemView) {
            super(itemView);
            this.firstLine = (TextView) itemView.findViewById(R.id.firstLine);
            this.secondLine = (TextView) itemView.findViewById(R.id.secondLine);
            this.secondLineViewers = (TextView) itemView.findViewById(R.id.secondLineViewers);
            this.videoDuration = (TextView) itemView.findViewById(R.id.textBroadcastDuration);
            this.imageView = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onVideoItemClick(getPosition());
        }
    }

    public class MainHeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView banner;
        public ImageView chat;
        public TextView name;
        public TextView bio;

        public MainHeaderViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.textTitleView);
            this.bio = (TextView) itemView.findViewById(R.id.textBioView);
            this.banner = (ImageView) itemView.findViewById(R.id.channelBanner);
            itemView.setOnClickListener(this);

            this.chat = (ImageView) itemView.findViewById(R.id.chatIcon);
            this.chat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemClickListener.onChatClick();
                }
            });
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onMainHeaderClick();
        }
    }

    public class CategoryHeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView name;
        public ImageView icon;

        public CategoryHeaderViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.textView);
            this.icon = (ImageView) itemView.findViewById(R.id.videoCategoryIcon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onVideoItemClick(getPosition());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {

        if (viewType == IS_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_compact_channel, parent, false);
            if (mHeaderLayout != null)
                v.findViewById(R.id.channelBanner).setLayoutParams(mHeaderLayout);
            return new MainHeaderViewHolder(v);
        }

        if (viewType == IS_BROADCAST_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout_channel_category, parent, false);
            ((TextView)v.findViewById(R.id.textView)).setText("Broadcasts");
            ((ImageView) v.findViewById(R.id.videoCategoryIcon)).setImageResource(R.drawable.ic_broadcast);
            return new CategoryHeaderViewHolder(v);
        }

        if (viewType == IS_HIGHLIGHT_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout_channel_category, parent, false);
            ((TextView)v.findViewById(R.id.textView)).setText("Highlights");
            ((ImageView) v.findViewById(R.id.videoCategoryIcon)).setImageResource(R.drawable.ic_highlight);
            return new CategoryHeaderViewHolder(v);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout_broadcast, parent, false);
        if (mVideoLayout != null)
            v.findViewById(R.id.icon).setLayoutParams(mVideoLayout);

        return new VideoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mFragment.getActivity() == null) return;
        String picUrl;
        int index;
        switch (getItemViewType(position)) {
            case IS_HEADER:
                MainHeaderViewHolder h = (MainHeaderViewHolder) holder;

                picUrl = mChannel.getLogoLink().isEmpty() ?
                        "null" : mChannel.getLogoLink();
                Picasso.with(mFragment.getActivity())
                        .load(picUrl)
                        .transform(new CircleTransform())
                        .placeholder(R.drawable.ic_placeholder_rounded)
                        .into(h.banner);

                h.name.setText(mChannel.getDisplayName());
                h.bio.setText(mUser.getBio());

                if (mHeaderLayout != null)
                    h.banner.setLayoutParams(mHeaderLayout);

                break;
            case IS_BROADCAST:
                VideoViewHolder vbh = (VideoViewHolder) holder;
                index = getArrayIndex(position);
                vbh.firstLine.setText(mBroadcasts.get(index).mTitle);
                vbh.secondLine.setText(mBroadcasts.get(index).timeAgo());
                vbh.secondLineViewers.setText(mBroadcasts.get(index).mViews);
                vbh.videoDuration.setText(TwitchJSONParser.secondsInHMS(mBroadcasts.get(index).mLength));

                picUrl = mBroadcasts.get(index).mPreviewLink.isEmpty() ?
                        "null" : mBroadcasts.get(index).mPreviewLink;
                Picasso.with(mFragment.getActivity())
                        .load(picUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .config(Bitmap.Config.RGB_565)
                        .into(vbh.imageView);

                if (mVideoLayout != null)
                    vbh.imageView.setLayoutParams(mVideoLayout);

                break;
            case IS_HIGHLIGHT:
                VideoViewHolder vhh = (VideoViewHolder) holder;
                index = getArrayIndex(position);
                vhh.firstLine.setText(mHighlights.get(index).mTitle);
                vhh.secondLine.setText(mHighlights.get(index).timeAgo());
                vhh.secondLineViewers.setText(mHighlights.get(index).mViews);
                vhh.videoDuration.setText(TwitchJSONParser.secondsInHMS(mHighlights.get(index).mLength));

                picUrl = mHighlights.get(index).mPreviewLink.isEmpty() ?
                        "null" : mHighlights.get(index).mPreviewLink;
                Picasso.with(mFragment.getActivity())
                        .load(picUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .config(Bitmap.Config.RGB_565)
                        .into(vhh.imageView);

                if (mVideoLayout != null)
                    vhh.imageView.setLayoutParams(mVideoLayout);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mEntryTypes.get(position);
    }

    @Override
    public int getItemCount() {
        return mEntryTypes.size();
    }

    public int getArrayIndex(int gPosition) {
        int type = getItemViewType(gPosition);
        int startIndex = 0;
        for (int i = 0; i < mEntryTypes.size(); i++) {
            if (mEntryTypes.get(i) == type) {
                startIndex = i;
                break;
            }
        }
        return gPosition - startIndex;
    }

    public TwitchVideo getBroadcast(int i) {
        if (i >= mBroadcasts.size()) return null;
        return mBroadcasts.get(i);
    }

    public TwitchVideo getHighlight(int i) {
        if (i >= mHighlights.size()) return null;
        return mHighlights.get(i);
    }

    public void updateWidth(int width) {
        if (mWidth != width) {
            mWidth = width;
            mHeaderLayout = new RelativeLayout.LayoutParams((int) (mWidth * 0.25), (int) (mWidth * 0.25));
            float scale = 240f / 320f;
            mVideoLayout = new RelativeLayout.LayoutParams((int) (mWidth * 0.25), (int) (mWidth * 0.25 * scale));
            notifyDataSetChanged();
        }
    }
}