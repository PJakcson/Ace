package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.aceft.R;
import com.aceft.adapter.GamesAdapter;
import com.aceft.custom_layouts.FullscreenGridLayoutManager;
import com.aceft.data.AceAnims;
import com.aceft.data.Storage;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Game;
import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class GamesRasterFragment extends Fragment
{
    OnGameSelectedListener  mCallback;
    private String mBaseUrl;
    private int mLoadedItems, INT_GRID_UPDATE_VALUE, INT_GRID_UPDATE_THRESHOLD;

    private ArrayList<Game> mGames;
    private ProgressBar mProgressBar;

    private boolean mABHidden;
    private Context mAppContext;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private GamesAdapter mAdapter;

    private HashMap<String, String> mMapping;

    private static final String FILENAME = "mapping_file";

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
        mAppContext = getActivity().getApplicationContext();
//        mGridView = (GridView) rootView.findViewById(R.id.gridView);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.games_grid_progress);
        mBaseUrl = getArguments().getString("url");
        mMapping = Storage.getMappingFromFile(getActivity(), FILENAME);

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
        dataParsed(TwitchJSONParser.topGamesJSONtoArrayList(s, mMapping));
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
        getGamesDBIds(l);
    }

    private void getGamesDBIds(final ArrayList<Game> l) {
        if (getActivity() == null) return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < l.size(); i++) {
                    final Game g = l.get(i);
                    if (mMapping.containsKey(g.mId)) continue;
                    String thumb = TwitchNetworkTasks.downloadAndFetchThumb(g.mTitle);
                    if (thumb != null && getActivity() != null) {
                        mMapping.put(g.mId, thumb);
                        g.mThumbnail = thumb;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.update(g);
                            }
                        });
                    }
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        Storage.saveMappingOnDisk(mAppContext, mMapping, FILENAME);
        super.onPause();
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