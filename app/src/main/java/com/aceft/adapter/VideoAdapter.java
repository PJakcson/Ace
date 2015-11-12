package com.aceft.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.aceft.R;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.primitives.TwitchVideo;

import java.util.ArrayList;


public class VideoAdapter extends BaseAdapter {
    private Activity mActivity;
    private ArrayList<TwitchVideo> mVideos;
    private LayoutInflater mInflater;
    private int mWidth;
    private ViewGroup.LayoutParams mParams;

    public VideoAdapter(Activity c) {
        mActivity = c;
        mVideos = new ArrayList<>();
        mInflater = LayoutInflater.from(c);
    }

    public void updateVideos(ArrayList <TwitchVideo> c) {
        if (c == null) return;
        if (mVideos == null) mVideos = new ArrayList<>();
        mVideos.addAll(c);
        notifyDataSetChanged();
    }

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

            if (mWidth == 0 && ((GridView)parent).getColumnWidth() > 0) {
                mWidth = ((GridView)parent).getColumnWidth();
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

        holder.firstLine.setText(mVideos.get(position).mTitle);
        holder.secondLine.setText(mVideos.get(position).timeAgo());
        holder.secondLineViewers.setText(mVideos.get(position).mViews);
        holder.videoDuration.setText(TwitchJSONParser.secondsInHMS(mVideos.get(position).mLength));

        String picUrl = mVideos.get(position).mPreviewLink.isEmpty() ?
                "null" : mVideos.get(position).mPreviewLink;
        Picasso.with(mActivity)
                .load(picUrl)
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(holder.imageView);

        return convertView;
    }

    public int getCount() {
        return mVideos.size();
    }

    public TwitchVideo getItem(int position) {
        return mVideos.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView videoDuration;
    }
}