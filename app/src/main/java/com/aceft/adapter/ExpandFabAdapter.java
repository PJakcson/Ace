package com.aceft.adapter;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.R;
import com.aceft.data.CircleTransform;
import com.aceft.data.primitives.Channel;
import com.aceft.ui_fragments.channel_fragments.ChannelFragment;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;


public class ExpandFabAdapter extends RecyclerView.Adapter<ExpandFabAdapter.ViewHolder> {

    private ChannelFragment mFragment;
    private ArrayList<Channel> mChannels = new ArrayList<>();

    public ExpandFabAdapter(ChannelFragment fragment) {
        mFragment = fragment;
    }

    public void updateChannel(Channel channel) {
        if (mChannels.size() < 8)
            mChannels.add(channel);
        notifyDataSetChanged();
    }

    public void updateChannel(ArrayList<Channel> ch) {
        mChannels.addAll(ch);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener{
        public View item;
        public FloatingActionButton fButton;
        public TextView channel;

        public ViewHolder(View itemView) {
            super(itemView);
            this.item = itemView;
            this.channel = (TextView) itemView.findViewById(R.id.liveChannel);
            this.fButton = (FloatingActionButton) itemView.findViewById(R.id.fab);
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragment.hideExpanded();
                }
            });
            channel.setOnClickListener(this);
            fButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(mFragment.getActivity(), mChannels.get(getPosition()).getDisplayName(), Toast.LENGTH_SHORT).show();
            mFragment.goToChannel(mChannels.get(getPosition()).getName());
        }
    }

    @Override
    public ExpandFabAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_fab_expanded, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (mFragment.getActivity() == null) return;
        holder.item.setAlpha(0);
        holder.channel.setText(mChannels.get(position).getName());

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                holder.fButton.setImageBitmap(bitmap);
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette p) {
                        holder.fButton.setBackgroundTintList(ColorStateList.valueOf(p.getLightVibrantColor(R.color.default_circle_indicator_fill_color)));
                    }
                });
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        Picasso.with(mFragment.getActivity())
                .load(mChannels.get(position).getLogoLink())
                .transform(new CircleTransform())
                .into(target);

        AnimatorSet afab1 = (AnimatorSet) AnimatorInflater.loadAnimator(mFragment.getActivity(), R.animator.fab_in);
        afab1.setTarget(holder.fButton);

        ObjectAnimator obj = ObjectAnimator.ofFloat(holder.item, "alpha", 0, 1);
        ObjectAnimator obj2 = ObjectAnimator.ofFloat(holder.item, "translationY", 50, 0);
        afab1.playTogether(obj, obj2);
        afab1.setDuration(150);
        afab1.setStartDelay(position * 30);
        afab1.start();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mChannels.size() < 900 ? mChannels.size() : 9;
    }

    public void clear() {
        mChannels.clear();
        notifyDataSetChanged();
    }
}