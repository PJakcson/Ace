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
import com.aceft.data.primitives.Game;
import com.aceft.ui_fragments.front_pages.GamesRasterFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
    private final View mHeader;
    private final GamesRasterFragment mFragment;
    private ArrayList<Game> mGames;
    private ViewGroup.LayoutParams mParams;
    private int mSpanCount;
    private Context mContext;
    private int mWidth = -1;
    private int mHeight = -1;
    private final int ITEM_VIEW_TYPE_HEADER = 0;
    private final int ITEM_VIEW_TYPE_ITEM = 1;

    private int mArtType = 0;
    OnItemClickListener mItemClickListener;


    public GamesAdapter(ArrayList<Game> g, int spanCount, View header, GamesRasterFragment f) {
        mGames = g;
        mSpanCount = spanCount;
        mHeader = header;
        mContext = f.getActivity();
        mFragment = f;
    }

    public GamesAdapter(ArrayList<Game> g, int spanCount, View header, Context context) {
        mGames = g;
        mSpanCount = spanCount;
        mHeader = header;
        mContext = context;
        mFragment = null;
    }

    public void update(ArrayList<Game> g) {
        if (g == null) return;
        if (mGames == null)
            mGames = new ArrayList<>();

        mGames.addAll(g);
        notifyDataSetChanged();
    }

    private boolean isHeader(int position) {
        return position == 0;
    }

    public void cleanData() {
        mGames.clear();
    }

    public void update(Game g) {
        int index = mGames.indexOf(g);
        if (index > -1) {
            mGames.set(index, g);
            notifyDataSetChanged();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView thumbImage;
        public TextView title;
        public TextView viewers;
        public Game item;

        public ViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.game_desc);
            this.viewers = (TextView) itemView.findViewById(R.id.game_viewers);
            this.thumbImage = (ImageView) itemView.findViewById(R.id.game_thumbnail);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onItemClick(getPosition()-1);
        }
    }

    @Override
    public GamesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {

        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            return new ViewHolder(mHeader);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout_game, parent, false);

        if (mWidth < 0) {
            float scale = 380.f / 272.f;
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
        holder.item = mGames.get(pos);

        if (holder.item.getGameArt(mArtType) != null) {
            Picasso.with(mContext)
                    .load(holder.item.mThumbnail)
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.thumbImage);
        } else {
            holder.thumbImage.setImageDrawable(null);
        }

        holder.title.setText(mGames.get(pos).mTitle);
        holder.viewers.setText(LayoutTasks.formatNumber(Integer.toString(mGames.get(pos).mViewers)));
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ?
                ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mGames.size() + 1;
    }
}