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
import com.aceft.adapter.GamesAdapter;
import com.aceft.custom_layouts.FullscreenGridLayoutManager;
import com.aceft.data.AceAnims;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Game;

import java.util.ArrayList;

public class GamesRasterFragment extends Fragment
{
    OnGameSelectedListener  mCallback;
    private String mBaseUrl;
    private int mLoadedItems, INT_GRID_UPDATE_VALUE, INT_GRID_UPDATE_THRESHOLD;

    private ArrayList<Game> mGames;
    private ProgressBar mProgressBar;

    private boolean mABHidden;

    private boolean adIsOnTop = false;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private GamesAdapter mAdapter;

    public GamesRasterFragment newInstance(String url) {
        GamesRasterFragment fragment = new GamesRasterFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnGameSelectedListener {
        void onGameSelected(Game g);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_games2, container, false);
//        mGridView = (GridView) rootView.findViewById(R.id.gridView);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.games_grid_progress);
        mBaseUrl = getArguments().getString("url");

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(getActivity().getString(R.string.title_section1));

        mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
        INT_GRID_UPDATE_VALUE = getResources().getInteger(R.integer.game_grid_update_items);
        INT_GRID_UPDATE_THRESHOLD = getResources().getInteger(R.integer.game_grid_update_threshold);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
//        mRecyclerView.setItemAnimator(null);

        if (getActivity() == null) return rootView;
        final int spanCount = getActivity().getResources().getInteger(R.integer.games_span_count);
        mLayoutManager = new FullscreenGridLayoutManager(getActivity(), spanCount);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? spanCount : 1;
            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);
        mLayoutManager.setMeasuredDimension(400, 400);

        View header = LayoutInflater.from(getActivity()).inflate(
                R.layout.item_top_placeholder, mRecyclerView, false);
        mAdapter = new GamesAdapter(new ArrayList<Game>(), spanCount, header, this);
        mAdapter.SetOnItemClickListener(new GamesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mCallback.onGameSelected(mGames.get(position));
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
                int totalItemCount = mGames.size();

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

                if (lastVisibleItem >= mLoadedItems - INT_GRID_UPDATE_THRESHOLD) {
                    loadGameData(INT_GRID_UPDATE_VALUE, mLoadedItems);
                    mLoadedItems += INT_GRID_UPDATE_VALUE;
                }

            }
        });

        return rootView;
    }

    public void loadGameData(int limit, int offset) {
        String request = mBaseUrl;
        request += "limit=" + limit + "&offset=" + offset;

        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(request, Thread.MAX_PRIORITY);
    }

    public void dataReceived(String s) {
        dataParsed(TwitchJSONParser.topGamesJSONtoArrayList(s));
    }

    public void dataParsed(ArrayList<Game> l) {
        if (l == null) return;
        if (mGames == null) {
            mGames = l;
            mProgressBar.setVisibility(View.INVISIBLE);
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mRecyclerView, "alpha",  0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        } else {
            mGames.addAll(l);
        }

        mAdapter.update(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).resumeAd();
        if (mGames != null) {
            mLoadedItems = mGames.size();
            mAdapter.cleanData();
            mAdapter.update(mGames);
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
            loadGameData(mLoadedItems, 0);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnGameSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}