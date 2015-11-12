package com.aceft.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aceft.R;
import com.aceft.data.LayoutTasks;
import com.aceft.data.Preferences;
import com.aceft.data.primitives.Channel;
import com.aceft.ui_fragments.front_pages.FollowedListFragment;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ChannelGridAdapter extends RecyclerView.Adapter<ChannelGridAdapter.ViewHolder> {
    private final View mHeader;
    private boolean mFollowFragment;
    private Fragment mFragment;
    private ArrayList<Channel> mChannels;
    private ArrayList<String> mSelectedChannels = new ArrayList<>();
    private RelativeLayout.LayoutParams mRelativeLayout;
    private int mSpanCount;
    private int mWidth = -1;
    private final int ITEM_VIEW_TYPE_HEADER = 0;
    private final int ITEM_VIEW_TYPE_ITEM = 1;
    OnItemClickListener mItemClickListener;
    OnStarred mStarredCallback;
    private boolean mSelectMode;
    private int mAnimDelay = 0;
    private int allChannels;

    public ChannelGridAdapter(ArrayList<Channel> c, int spanCount, View header, Fragment fragment, boolean followFragment) {
        mChannels = c;
        mSpanCount = spanCount;
        mHeader = header;
        mFragment = fragment;
        mFollowFragment = followFragment;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAnimDelay = 150;
        }

        try {
            mStarredCallback = (FollowedListFragment) fragment;
        } catch (ClassCastException ignored) {

        }
    }

    public ChannelGridAdapter(ArrayList<Channel> c, int spanCount, View header, Fragment fragment) {
        this(c, spanCount, header, fragment, true);
    }

    public void update(ArrayList<Channel> c) {
        mChannels = c;
        mSelectedChannels.clear();
        notifyDataSetChanged();
    }

    public void update(int i, Channel c) {
        mChannels.set(i, c);
        notifyDataSetChanged();
    }

    private boolean isHeader(int position) {
        return position == 0;
    }

    public ArrayList<String> getSelectedChannels() {
        return mSelectedChannels;
    }

    public ArrayList<String> getAllChannels() {
        ArrayList<String> ch = new ArrayList<>();
        for (Channel c : mChannels) {
            ch.add(c.getName());
        }
        return ch;
    }

    public void clearSelected() {
        mSelectedChannels.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnStarred {
        void onModeSwitched(boolean select);
        void onSelectionChanged(boolean allStarred);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView textLive;
        private ImageView starredIcon;
        private CardView cardView;

        public ViewHolder(final View itemView) {
            super(itemView);
            this.firstLine = (TextView) itemView.findViewById(R.id.firstLine);
            this.secondLine = (TextView) itemView.findViewById(R.id.secondLine);
            this.secondLineViewers = (TextView) itemView.findViewById(R.id.secondLineViewers);
            this.imageView = (ImageView) itemView.findViewById(R.id.icon);
            this.textLive = (TextView) itemView.findViewById(R.id.textLive);
            this.starredIcon = (ImageView) itemView.findViewById(R.id.starred_icon);
            this.cardView = (CardView) itemView.findViewById(R.id.card_view);

            if (this.starredIcon == null) return;
            if (!mFollowFragment) {
                this.starredIcon.setVisibility(View.GONE);
                return;
            }

            itemView.setOnClickListener(this);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    cardView.setCardBackgroundColor(mFragment.getResources().getColor(R.color.selected_item));
                    switchSelectMode(true);
                    mStarredCallback.onSelectionChanged(mChannels.get(getPosition()-1).isStarred());
                    return false;
                }
            });
        }

        @Override
        public void onClick(View view) {
            if(!mSelectMode)
                mItemClickListener.onItemClick(getPosition() - 1);
            else {
                if (!mSelectedChannels.contains(mChannels.get(getPosition()-1).getName()))
                    mSelectedChannels.add(mChannels.get(getPosition()-1).getName());
                else {
                    mSelectedChannels.remove(mChannels.get(getPosition() - 1).getName());
                }
                if (mSelectedChannels.isEmpty())
                    switchSelectMode(false);
                else
                    mStarredCallback.onSelectionChanged(onlyStarredSelected());

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notifyItemChanged(getPosition());
                    }
                }, mAnimDelay);
            }
        }
    }

    @Override
    public ChannelGridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {

        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            return new ViewHolder(mHeader);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout_channel, parent, false);
        if (mWidth < 0) {
            int windowWidth = LayoutTasks.getWindowWidth((Activity) parent.getContext());
            mWidth = Math.round(windowWidth / mSpanCount * 0.33f);
            mRelativeLayout = new RelativeLayout.LayoutParams(mWidth, mWidth);
        }
        v.findViewById(R.id.icon).setLayoutParams(mRelativeLayout);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (isHeader(position)) return;

        int index = position - 1;
        String logoUrl = mChannels.get(index).getLogoLink().isEmpty() ?
                "null" : mChannels.get(index).getLogoLink();

        Picasso.with(mFragment.getActivity())
                .load(logoUrl)
                .config(Bitmap.Config.RGB_565)
                .into(holder.imageView);

        holder.firstLine.setText(index+1 + ". " + mChannels.get(index).getDisplayName());
        holder.secondLine.setText(mChannels.get(index).getGame());
        String followers;

        if (mChannels.get(index).getFollowers().equals("1")) {
            followers = LayoutTasks.formatNumber(mChannels.get(index).getFollowers()) + " Follower";
        } else {
            followers = LayoutTasks.formatNumber(mChannels.get(index).getFollowers()) + " Followers";
        }
        holder.secondLineViewers.setText(followers);

        if (!mChannels.get(index).isbIsOnline()) {
            holder.textLive.setVisibility(View.INVISIBLE);
        } else {
            holder.textLive.setVisibility(View.VISIBLE);
            String v = LayoutTasks.formatNumber(mChannels.get(index).getViewers());
            holder.secondLineViewers.setText(v + " Viewers");
        }

        if (mChannels.get(index).isStarred()) {
            holder.starredIcon.setImageResource(R.drawable.ic_notifications_off_grey);
        } else {
            holder.starredIcon.setImageResource(R.drawable.ic_notifications_off_black);
        }

        if (mSelectedChannels.contains(mChannels.get(index).getName()))
            holder.cardView.setCardBackgroundColor(mFragment.getResources().getColor(R.color.selected_item));
        else
            holder.cardView.setCardBackgroundColor(mFragment.getResources().getColor(R.color.chat_white));
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ?
                ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mChannels.size() + 1;
    }

    private void switchSelectMode(boolean b) {
        mSelectMode = b;
        mStarredCallback.onModeSwitched(b);
    }

    private boolean onlyStarredSelected() {
        for (Channel c : mChannels) {
            if (mSelectedChannels.contains(c.getName()) && c.isStarred())
                return true;
        }
        return false;
    }
}