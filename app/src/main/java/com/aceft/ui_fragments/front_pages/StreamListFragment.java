package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.StreamListAdapter;
import com.aceft.custom_layouts.FullscreenGridLayoutManager;
import com.aceft.data.AceAnims;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Stream;

import java.util.ArrayList;


public class StreamListFragment extends Fragment{
    private int mLoadedItems, INT_LIST_UPDATE_VALUE, INT_LIST_UPDATE_THRESHOLD;
    private onStreamSelectedListener mCallback;
    private ProgressBar mProgressBar;
    private String mUrl, mTitle;

    private ArrayList<Stream> mStreams;

    private boolean mABHidden;
    private boolean adIsOnTop = false;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private StreamListAdapter mAdapter;

    public Fragment newInstance(String url, String mTitle) {
        StreamListFragment fragment = new StreamListFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        if (mTitle == null)
            mTitle = "Popular Streams";
        args.putString("bar_title", mTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public Fragment newInstance(String s) {
        StreamListFragment fragment = new StreamListFragment();
        Bundle args = new Bundle();
        String url = getString(R.string.game_streams_url) + s;
        args.putString("url", url);
        if (mTitle == null)
            mTitle = "Popular Streams";
        args.putString("bar_title", s);
        fragment.setArguments(args);
        return fragment;
    }

    public interface onStreamSelectedListener {
        void onStreamSelected(Stream c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_top_streams2, container, false);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channels_list_progress);

        mLoadedItems = getResources().getInteger(R.integer.channel_list_start_items);
        INT_LIST_UPDATE_VALUE = getResources().getInteger(R.integer.channel_list_update_items);
        INT_LIST_UPDATE_THRESHOLD = getResources().getInteger(R.integer.channel_list_update_threshold);

        if (mStreams != null) {
            mLoadedItems = mStreams.size();
        }

        mUrl = getArguments().getString("url");
        mTitle = getArguments().getString("bar_title");

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
               actionBar.setTitle(mTitle);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        if (getActivity() == null) return rootView;
        final int spanCount = getActivity().getResources().getInteger(R.integer.stream_span_count);
        mLayoutManager = new FullscreenGridLayoutManager(getActivity(), spanCount);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? spanCount : 1;
            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);

        View header = LayoutInflater.from(getActivity()).inflate(
                R.layout.item_top_placeholder, mRecyclerView, false);
        mAdapter = new StreamListAdapter(new ArrayList<Stream>(), spanCount, header, getActivity());
        mAdapter.SetOnItemClickListener(new StreamListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mCallback.onStreamSelected(mStreams.get(position));
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled (RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                int visibleItemCount = lastVisibleItem - firstVisibleItem;
                int totalItemCount = mStreams.size();

                if (dy > 5 && !mABHidden && firstVisibleItem > 0) {
                    mABHidden = true;
                    AceAnims.hideActionBar(getActivity());
                }
                if (dy < -5 && mABHidden) {
                    mABHidden = false;
                    AceAnims.showActionbar(getActivity());
                }

                if (totalItemCount > 0 && totalItemCount >= visibleItemCount) {
                    if (lastVisibleItem >= totalItemCount - 1 && !adIsOnTop) {
                        ((MainActivity) getActivity()).pushDownAd();
                        adIsOnTop = true;
                    }
                    if (lastVisibleItem < totalItemCount - 1 && adIsOnTop) {
                        ((MainActivity) getActivity()).pushUpAd();
                        adIsOnTop = false;
                    }
                }

                if (lastVisibleItem >= mLoadedItems - INT_LIST_UPDATE_THRESHOLD) {
                    downloadStreamData(INT_LIST_UPDATE_VALUE, mLoadedItems);
                    mLoadedItems += INT_LIST_UPDATE_VALUE;
                }
            }
        });

        return rootView;
    }

    public void downloadStreamData(int limit, int offset) {
        String request = mUrl;
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(request, Thread.MAX_PRIORITY);
    }

    public void dataReceived(String s) {
//        TwitchJSONParserThread t = new TwitchJSONParserThread(this);
//        t.parseJSONInBackground(s, Thread.MAX_PRIORITY);
        dataParsed(TwitchJSONParser.streamJSONtoArrayList(s));
    }

    public void dataParsed(ArrayList<Stream> l) {
        if (l == null) return;
        if (mStreams == null) {
            mStreams = l;
            mProgressBar.setVisibility(View.INVISIBLE);
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mRecyclerView, "alpha",  0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        }
        else
            mStreams.addAll(l);
        mAdapter.update(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", mUrl);
        outState.putString("bar_title", mTitle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onStreamSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStreams != null) {
            mAdapter.clearData();
            mLoadedItems = mStreams.size();
            mAdapter.update(mStreams);
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            downloadStreamData(mLoadedItems, 0);
        }
        mABHidden = false;
        AceAnims.showActionbar(getActivity(), false);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).resetAdPosition();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}