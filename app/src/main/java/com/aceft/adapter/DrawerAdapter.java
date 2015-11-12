package com.aceft.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aceft.R;
import com.aceft.data.CircleTransform;
import com.aceft.data.primitives.Channel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DrawerAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private String[] titles;
    private int[] drawables;
    private String[] footer;
    private ArrayList<Channel> mChannels;
    private Context mContext;
    private boolean mLoading;


    public DrawerAdapter(Context c, String[] titles, int[] drawables, String[] footer) {
        mContext = c;
        this.titles = titles;
        this.drawables = drawables;
        this.footer = footer;
        mInflater = LayoutInflater.from(c);
        mChannels = new ArrayList<>();
    }

    public void setIsLoading() {
        mLoading = true;
        mChannels.clear();
        notifyDataSetChanged();
    }

    public void updateData(ArrayList<Channel> c) {
        if (c != null) {
            mChannels = c;
        }
        mLoading = false;
        notifyDataSetChanged();
    }

    public void clearFollowed() {
        if (!mChannels.isEmpty()) {
            mChannels.clear();
            notifyDataSetChanged();
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.drawer_row_layout, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.drawerText);
            holder.thumbImage = (ImageView) convertView.findViewById(R.id.drawerIcon);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.drawerProgress);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position < titles.length) {
            holder.title.setText(titles[position]);
            holder.thumbImage.setImageResource(drawables[position]);
        }

        if (position == titles.length){
            convertView = mInflater.inflate(R.layout.item_layout_drawer, parent, false);
            return convertView;
        }

        if (position > titles.length && position < getFixedSize()){
            convertView = mInflater.inflate(R.layout.drawer_row_footer, parent, false);
            ((TextView) convertView.findViewById(R.id.drawerFooter)).setText(footer[position - titles.length - 1]);
            return convertView;
        }

        if (position == getFixedSize()){
            convertView = mInflater.inflate(R.layout.item_layout_drawer, parent, false);
            return convertView;
        }

        if (position == getFixedSize()+1){
            convertView = mInflater.inflate(R.layout.drawer_row_footer, parent, false);
            ((TextView) convertView.findViewById(R.id.drawerFooter)).setText(mContext.getString(R.string.drawer_live_channels_header));
            ((TextView) convertView.findViewById(R.id.drawerFooter)).setTextColor(mContext.getResources().getColor(R.color.twitch_main));
            return convertView;
        }

        if (position > getFixedSize()+1 && mLoading){
            holder.title.setText("");
            holder.thumbImage.setImageBitmap(null);
            holder.progressBar.setVisibility(View.VISIBLE);
            return convertView;
        }

        if (position > getFixedSize()+1){
            holder.progressBar.setVisibility(View.GONE);
            int i = getChannelIndex(position);
            holder.title.setText(mChannels.get(i).getDisplayName());
            String logoUrl = mChannels.get(i).getLogoLink().isEmpty() ?
                    "null" : mChannels.get(i).getLogoLink();
            Picasso.with(mContext)
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_placeholder_rounded)
                    .transform(new CircleTransform())
                    .into(holder.thumbImage);
        }

        return convertView;
    }

    public int getCount() {
        if (mChannels.isEmpty() && mLoading) {
            return titles.length + 4 + footer.length;
        }
        if (mChannels.isEmpty()) {
            return titles.length + 1 + footer.length;
        }
        return getFixedSize() + mChannels.size() + 2;
    }

    public int getFixedSize() {
        return titles.length + 1 + footer.length;
    }

    public int getChannelIndex(int i) {
        return i - getFixedSize() - 2;
    }

    public Channel getChannel(int position) {
        int i = getChannelIndex(position);
        return mChannels.get(i);
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != 4;
    }

    public long getItemId(int position) {
        return position;
    }

    public class ViewHolder {
        ImageView thumbImage;
        TextView title;
        ProgressBar progressBar;
    }
}