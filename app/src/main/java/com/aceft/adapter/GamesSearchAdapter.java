package com.aceft.adapter;

import android.app.Activity;
import android.content.Context;
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
import com.aceft.data.primitives.Game;

import java.util.ArrayList;


public class GamesSearchAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<Game> mGames;
    private Context mContext;
    private int mWidth;
    private ViewGroup.LayoutParams mParams;


    public GamesSearchAdapter(Activity c) {
        mContext = c;
        if (mGames == null) mGames = new ArrayList<>();
        mInflater = LayoutInflater.from(c);
        mGames = new ArrayList<>();
    }

    public void update(ArrayList<Game> g) {
        if (g == null) return;
        if (mGames == null)
            mGames = new ArrayList<>();
        mGames.addAll(g);
        notifyDataSetChanged();
    }

    public void cleanData() {
        mGames.clear();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.item_layout_game_search, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.game_desc);
            holder.viewers = (TextView) convertView.findViewById(R.id.game_viewers);
            holder.thumbImage = (ImageView) convertView.findViewById(R.id.game_thumbnail);

            if (mWidth == 0 && ((GridView)parent).getColumnWidth() > 0) {
                float scale = 380.f / 272.f;
                mWidth = ((GridView)parent).getColumnWidth();
                mParams = holder.thumbImage.getLayoutParams();
                mParams.width = mWidth;
                mParams.height = (int) (mWidth * scale);
            }

            if (mParams.height > 0) {
                holder.thumbImage.setLayoutParams(mParams);
            }

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        String picUrl = mGames.get(position).mThumbnail.isEmpty() ?
                "null" : mGames.get(position).mThumbnail;
        Picasso.with(mContext)
                .load(picUrl)
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(holder.thumbImage);

        holder.title.setText(mGames.get(position).mTitle);
        //holder.viewers.setText(Integer.toString(mGames.get(position).mViewers));

        return convertView;
    }

    public int getCount() {
        return mGames.size();
    }

    public Game getItem(int position) {
        return mGames.get(position);
    }

    public long getItemId(int position) {
        return Long.valueOf(mGames.get(position).mId);
    }

    public void clearAllData(){
        mGames.clear();
    }

    public class ViewHolder {
        ImageView thumbImage;
        TextView title;
        TextView viewers;
    }
}