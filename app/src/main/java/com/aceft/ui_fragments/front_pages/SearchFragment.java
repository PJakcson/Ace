package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.ChannelListAdapter;
import com.aceft.adapter.GamesSearchAdapter;
import com.aceft.data.AceAnims;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Game;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SearchFragment extends Fragment {
    private OnGameSelectedListener mGameCallback;
    private onChannelSelectedListener mChannelCallback;
    private ProgressBar mProgressBar;
    private ArrayList<Channel> mChannels;
    private ArrayList<Game> mGames;
    private GridView mGamesGridView, mChannelGridView;
    private GamesSearchAdapter mGamesAdapter;
    private ChannelListAdapter mChannelsAdapter;
    private String mQuery;

    public interface OnGameSelectedListener {
        void onGameSelected(Game g);
    }

    public interface onChannelSelectedListener {
        void onChannelSelected(Channel c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.search_progress);
        mProgressBar.setVisibility(View.GONE);

        mGamesGridView = (GridView) rootView.findViewById(R.id.gameGrid);
        mChannelGridView = (GridView) rootView.findViewById(R.id.channelGrid);

        ActionBar actionBar = ((MainActivity)getActivity()).getSupportActionBar();

        View v = inflater.inflate(R.layout.actionbar_search_layout, null);
        SearchView searchView = (SearchView) v.findViewById(R.id.searchfield);
        styleSearchView(searchView);
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(v);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        mGamesAdapter = new GamesSearchAdapter(getActivity());
        mGamesGridView.setAdapter(mGamesAdapter);
        if (mGames != null) updateGamesGrid();
        mChannelsAdapter = new ChannelListAdapter(getActivity());
        mChannelGridView.setAdapter(mChannelsAdapter);
        if (mChannels != null) updateChannelsGrid();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                answerQuery(query);
                mGamesAdapter.clearAllData();
                mChannelsAdapter.clearAllData();
                mQuery = query;
                mProgressBar.setVisibility(View.VISIBLE);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setQuery(mQuery, false);

        mGamesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mGameCallback.onGameSelected(mGamesAdapter.getItem(position));
            }
        });

        mChannelGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mChannelCallback.onChannelSelected(mChannelsAdapter.getItem(position));
            }
        });

        return rootView;
    }

    private void answerQuery(String s) {
        String channel_request = getActivity().getString(R.string.twitch_search_channels);
        String game_request = getActivity().getString(R.string.twitch_search_games);

        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        String queryLimit = "&limit=20";

        TwitchJSONDataThread game_thread = new TwitchJSONDataThread(this, 1);
        game_thread.downloadJSONInBackground(game_request + "q=" + s + "&type=suggest&live=true", Thread.NORM_PRIORITY);
        TwitchJSONDataThread channel_thread = new TwitchJSONDataThread(this, 0);
        channel_thread.downloadJSONInBackground(channel_request + "q=" + s + queryLimit, Thread.NORM_PRIORITY);
    }

    public void gameDataReceived(String s) {
        if (s == null) return;
        mGames = TwitchJSONParser.gameSearchJSONtoArrayList(s);
        if (mGames == null) return;
        updateGamesGrid();
    }

    private void updateGamesGrid() {
        if (mGames.size() == 0) return;
        int columns = getActivity().getResources().getInteger(R.integer.number_of_columns);
        ArrayList<Game> g;
        if (!isInLandscape() && mGames.size() > columns * 2) {
            g = new ArrayList<>(mGames.subList(0, columns * 2));
        } else if (isInLandscape() && mGames.size() > columns) {
            g = new ArrayList<>(mGames.subList(0, columns));
        } else {
            g = mGames;
        }

        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mGamesGridView, "alpha",  0f, 1f);
        fadeInStream.setDuration(500);
        fadeInStream.start();
        mProgressBar.setVisibility(View.GONE);
        mGamesAdapter.update(g);
    }

    public void channelDataReceived(String s) {
        mChannels = TwitchJSONParser.channelsJSONtoArrayList(s);
        if (mChannels == null) return;
        updateChannelsGrid();
    }

    private void updateChannelsGrid() {
        if (mChannels.size() == 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mChannelGridView, "alpha",  0f, 1f);
        fadeInStream.setDuration(500);
        fadeInStream.start();
        mProgressBar.setVisibility(View.GONE);
        mChannelsAdapter.update(mChannels);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mGameCallback = (OnGameSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
        try {
            mChannelCallback = (onChannelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChannelSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mGameCallback = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).pauseAd();
        AceAnims.showActionbar(getActivity(), false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        ActionBar actionBar =  ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
        }
        super.onPause();
    }

    private void styleSearchView(SearchView searchView) {
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        int searchTextId = searchPlate.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
        searchText.setTextColor(Color.WHITE);
        searchText.setHintTextColor(Color.LTGRAY);
        searchPlate.setBackgroundResource(R.drawable.abc_textfield_search_activated_mtrl_alpha);

        int searchIconId = searchView.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchIcon = (ImageView) searchView.findViewById(searchIconId);
        searchIcon.setImageResource(R.drawable.ic_search_white);
    }

    private boolean isInLandscape() {
        return getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
}