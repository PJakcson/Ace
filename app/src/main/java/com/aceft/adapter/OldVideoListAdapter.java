package com.aceft.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import com.aceft.R;
import com.aceft.data.TwitchJSONParser;
import com.aceft.ui_fragments.channel_fragments.VideoFragment;

public class OldVideoListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<String> mLengths;

    public OldVideoListAdapter(VideoFragment v, ArrayList<String> l) {
        mLengths = l;
        mInflater = LayoutInflater.from(v.getActivity());
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.old_video_row, parent, false);
            holder = new ViewHolder();
            holder.part = (TextView) convertView.findViewById(R.id.part);
            holder.length = (TextView) convertView.findViewById(R.id.length);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.part.setText("Part " + (position+1) + " of " + mLengths.size());
        holder.length.setText(secondsInMinutes(mLengths.get(position)));

        return convertView;
    }

    public int getCount() {
        return mLengths.size();
    }

    public Integer getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder {
        public TextView part;
        public TextView length;
    }

    private String secondsInMinutes(String s) {
        return "Duration: " + TwitchJSONParser.secondsInHMS(s);
    }
}