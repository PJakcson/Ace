package com.aceft.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import com.aceft.R;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.primitives.TwitchVideo;

public class PastBroadcastsListAdapter extends BaseAdapter {
    private final static int IS_HEADER = 0;
    private final static int IS_HIGHLIGHT_HEADER = 1;
    private final static int IS_HIGHLIGHT = 2;
    private final static int IS_BROADCAST_HEADER = 3;
    private final static int IS_BROADCAST = 4;
    private final static int IS_CHAT = 5;

    private Activity mActivity;
    private ArrayList<TwitchVideo> mHighlights;
    private ArrayList<TwitchVideo> mBroadcasts;
    private LayoutInflater mInflater;
    private int mWidth;
    private ViewGroup.LayoutParams mParams;

    public PastBroadcastsListAdapter(Activity c) {
        mActivity = c;
        mHighlights = new ArrayList<>();
        mBroadcasts = new ArrayList<>();
        mInflater = LayoutInflater.from(c);
    }

    public void updateHighlights(ArrayList <TwitchVideo> c) {
        if (c == null) return;
        if (mHighlights == null) mHighlights = new ArrayList<>();
        mHighlights.addAll(c);
        notifyDataSetChanged();
    }

    public void updateBroadcasts(ArrayList <TwitchVideo> c) {
        if (c == null) return;
        if (mHighlights == null) mBroadcasts = new ArrayList<>();
        mBroadcasts.addAll(c);
        notifyDataSetChanged();
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.item_layout_broadcast, parent, false);
            holder = new ViewHolder();

            holder.firstLine = (TextView) convertView.findViewById(R.id.firstLine);
            holder.secondLine = (TextView) convertView.findViewById(R.id.secondLine);
            holder.secondLineViewers = (TextView) convertView.findViewById(R.id.secondLineViewers);
            holder.videoDuration = (TextView) convertView.findViewById(R.id.textBroadcastDuration);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);

            if (mWidth == 0 && parent.getMeasuredWidth() > 0) {
                mWidth = parent.getMeasuredWidth();
                float scale = 240f / 320f;
                mParams = holder.imageView.getLayoutParams();
                mParams.width = (int) (mWidth*0.35);
                mParams.height = (int) (mWidth*0.35*scale);
            }

            if (mParams != null) {
                holder.imageView.setLayoutParams(mParams);
            }
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        int j = getGroup(position+2);
        if (j == IS_HIGHLIGHT_HEADER) {
            View highlights = mInflater.inflate(R.layout.item_layout_channel_category, null);
            String mHighlightHeader = "Highlights";
            ((TextView)highlights.findViewById(R.id.textView)).setText(mHighlightHeader);
            ((ImageView)highlights.findViewById(R.id.videoCategoryIcon)).setImageResource(R.drawable.ic_highlight);
            if (mHighlights.isEmpty()) highlights.setVisibility(View.INVISIBLE);
            return highlights;
        } else if (j == IS_HIGHLIGHT) {
            int index = getChildPosition(position, j)+1;
            holder.firstLine.setText(mHighlights.get(index).mTitle);
            holder.secondLine.setText(mHighlights.get(index).timeAgo());
            holder.secondLineViewers.setText(mHighlights.get(index).mViews);
            holder.videoDuration.setText(TwitchJSONParser.secondsInHMS(mHighlights.get(index).mLength));

            String picUrl = mHighlights.get(index).mPreviewLink.isEmpty() ?
                    "null" : mHighlights.get(index).mPreviewLink;
            Picasso.with(mActivity)
                    .load(picUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.imageView);

        } else if (j == IS_BROADCAST_HEADER) {
            View broadcasts = mInflater.inflate(R.layout.item_layout_channel_category, null);
            String mBroadcastHeader = "Broadcasts";
            ((TextView)broadcasts.findViewById(R.id.textView)).setText(mBroadcastHeader);
            ((ImageView)broadcasts.findViewById(R.id.videoCategoryIcon)).setImageResource(R.drawable.ic_broadcast);
            if (mBroadcasts.isEmpty()) broadcasts.setVisibility(View.INVISIBLE);
            return broadcasts;

        } else if (j == IS_BROADCAST) {
            int index = getChildPosition(position+1, j);
            holder.firstLine.setText(mBroadcasts.get(index).mTitle);
            holder.secondLine.setText(mBroadcasts.get(index).timeAgo());
            holder.secondLineViewers.setText(mBroadcasts.get(index).mViews);
            holder.videoDuration.setText(TwitchJSONParser.secondsInHMS(mBroadcasts.get(index).mLength));

            String picUrl = mBroadcasts.get(index).mPreviewLink.isEmpty() ?
                    "null" : mBroadcasts.get(index).mPreviewLink;
            Picasso.with(mActivity)
                    .load(picUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.imageView);
        }
        return convertView;
    }

    public int getCount() {
        int count = 0;
        if (!mHighlights.isEmpty()) count += mHighlights.size() + 1;
        if (!mBroadcasts.isEmpty()) count += mBroadcasts.size() + 1;
        return count;
    }

    public TwitchVideo getHighlight(int position) {
        return mHighlights.get(position);
    }

    public TwitchVideo getBroadcast(int position) {
        return mBroadcasts.get(position);
    }

    public TwitchVideo getItem(int position) {
        if (position < mHighlights.size() && position != 0) return mHighlights.get(position - 1);
        if (position > mHighlights.size() && position != 0) return mBroadcasts.get(position - mHighlights.size());
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public int getGroup(int position) {
        if (position == 1) return IS_HEADER;
        if (position == 0) return IS_CHAT;
        if (!mHighlights.isEmpty() && position == 2) return IS_HIGHLIGHT_HEADER;
        if (!mHighlights.isEmpty() && position <= mHighlights.size() + 2) return IS_HIGHLIGHT;
        if (!mHighlights.isEmpty() && position == mHighlights.size() + 3) return IS_BROADCAST_HEADER;
        if (!mHighlights.isEmpty() && position <= mHighlights.size() + mBroadcasts.size() + 3) return IS_BROADCAST;
        if (mHighlights.isEmpty() && position == 2) return IS_BROADCAST_HEADER;
        if (mHighlights.isEmpty() && position <= mBroadcasts.size()+3) return IS_BROADCAST;
        return -1;
    }

    public int getChildPosition(int position, int group) {
        if (group == IS_HEADER) return 0;
        if (group == IS_CHAT) return 0;
        if (!mHighlights.isEmpty() && group == IS_HIGHLIGHT_HEADER) return 0;
        if (!mHighlights.isEmpty() && group == IS_HIGHLIGHT) return position - 2;
        if (!mHighlights.isEmpty() && group == IS_BROADCAST_HEADER) return 0;
        if (!mHighlights.isEmpty() && group == IS_BROADCAST) return position - mHighlights.size() - 3;
        if (mHighlights.isEmpty() && group == IS_BROADCAST_HEADER) return 0;
        if (mHighlights.isEmpty() && group == IS_BROADCAST) return position - 2;
        return -1;
    }

    public ArrayList<TwitchVideo> getHighlights() {
        return mHighlights;
    }

    public ArrayList<TwitchVideo> getBroadcasts() {
        return mBroadcasts;
    }

    public void clearAllData() {
        mHighlights.clear();
        mBroadcasts.clear();
    }

    public void resetDimensions() {
        mWidth = 0;
        notifyDataSetChanged();
    }

    public class ViewHolder {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView videoDuration;
    }
}