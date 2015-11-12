package com.aceft.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aceft.R;
import com.aceft.data.LayoutTasks;
import com.aceft.data.primitives.Stream;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class StreamListAdapter extends RecyclerView.Adapter<StreamListAdapter.ViewHolder> {
    private final View mHeader;
    private ArrayList<Stream> mStreams;
    private ViewGroup.LayoutParams mParams;
    private int mSpanCount;
    private Context mContext;
    private int mWidth = -1;
    private final int ITEM_VIEW_TYPE_HEADER = 0;
    private final int ITEM_VIEW_TYPE_ITEM = 1;
    OnItemClickListener mItemClickListener;
    private int mHeight = -1;


    public StreamListAdapter(ArrayList<Stream> s, int spanCount, View header, Context context) {
        mStreams = s;
        mSpanCount = spanCount;
        mHeader = header;
        mContext = context;
    }

    public void update(ArrayList<Stream> s) {
        if (s == null) return;
        if (mStreams == null)
            mStreams = new ArrayList<>();
        mStreams.addAll(s);
        notifyDataSetChanged();
    }

    public void clearData() {
        mStreams = new ArrayList<>();
    }

    private boolean isHeader(int position) {
        return position == 0;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView streamStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            this.firstLine = (TextView) itemView.findViewById(R.id.firstLine);
            this.secondLine = (TextView) itemView.findViewById(R.id.secondLine);
            this.secondLineViewers = (TextView) itemView.findViewById(R.id.secondLineViewers);
            this.streamStatus = (TextView) itemView.findViewById(R.id.text_stream_status);
            this.imageView = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onItemClick(getPosition()-1);
        }
    }

    @Override
    public StreamListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {

        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            return new ViewHolder(mHeader);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout_streams, parent, false);
        if (mWidth < 0) {
            float scale = 360f / 640f;
            mWidth = LayoutTasks.getWindowWidth((Activity) mContext) / mSpanCount;
            mHeight = (int) (mWidth * scale);
            mParams = new ViewGroup.LayoutParams(mWidth, (int) (mWidth * scale));
        }

        if (mHeight > 0)
            v.getLayoutParams().height = mHeight;

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (isHeader(position)) return;

        int pos = position - 1;

        String picUrl = mStreams.get(pos).getPreviewLink().isEmpty() ?
                "null" : mStreams.get(pos).getPreviewLink();
        Picasso.with(mContext)
                .load(picUrl)
                .config(Bitmap.Config.RGB_565)
                .into(holder.imageView);

        holder.firstLine.setText(mStreams.get(pos).getTitle());
        holder.secondLine.setText(mStreams.get(pos).printGame());
        holder.secondLineViewers.setText(LayoutTasks.formatNumber(mStreams.get(pos).getViewers()));
        holder.streamStatus.setText(String.valueOf(mStreams.get(pos).getStatus()));
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ?
                ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mStreams.size() + 1;
    }
}