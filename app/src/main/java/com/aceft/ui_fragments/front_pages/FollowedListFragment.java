package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.ChannelGridAdapter;
import com.aceft.custom_layouts.FullscreenGridLayoutManager;
import com.aceft.data.AceAnims;
import com.aceft.data.LayoutTasks;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;
import com.gms.QuickstartPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class FollowedListFragment extends Fragment implements ChannelGridAdapter.OnStarred {
    private int DOWNLOAD_CHUNK_SIZE = 200;
    private int ONLINE_CHUNK_SIZE = 100;
    private onChannelSelectedListener mCallback;
    private ProgressBar mProgressBar;
    private String mUrl;
    private String mTitle;
    private boolean adIsOnTop = false;

    private ArrayList<Channel> mChannels;
    private boolean mResumed;
    private SharedPreferences mPreferences;
    private long mLastUpdated = 0;
    private int mTotalFollowingCount = -1;
    private boolean mIsLoading;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private ChannelGridAdapter mAdapter;
    private boolean mABHidden, mABLocked;

    public FollowedListFragment newInstance(String url) {
        FollowedListFragment fragment = new FollowedListFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putString("bar_title", "Favorites");
        fragment.setArguments(args);
        return fragment;
    }

    public interface onChannelSelectedListener {
        void onChannelSelected(Channel c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_channel_list2, container, false);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        if (getActivity() == null) return rootView;
        final int spanCount = getActivity().getResources().getInteger(R.integer.following_span_count);
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
        mAdapter = new ChannelGridAdapter(new ArrayList<Channel>(), spanCount, header, this);
        mAdapter.SetOnItemClickListener(new ChannelGridAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (mChannels.isEmpty()) return;
                mCallback.onChannelSelected(mChannels.get(position));
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                int visibleItemCount = lastVisibleItem - firstVisibleItem;
                int totalItemCount = mChannels.size();

                if (dy > 0 && !mABHidden && firstVisibleItem > 0 && !mABLocked) {
                    mABHidden = true;
                    AceAnims.hideActionBar(getActivity());
                }
                if (dy < 0 && mABHidden && !mABLocked) {
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
            }
        });

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channels_list_progress);

        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString("url");
            mTitle = savedInstanceState.getString("bar_title");
        } else {
            mUrl = getArguments().getString("url");
            mTitle = getArguments().getString("bar_title");
        }

        mTitle = getActivity().getString(R.string.title_section4);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(mTitle);

        return rootView;
    }

    private void allDataComplete() {
        Collections.sort(mChannels, new CustomComparator());
        mProgressBar.setVisibility(View.INVISIBLE);
        updateLayout();
    }

    public void updateLayout() {
        if (!mResumed) {
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mRecyclerView, "alpha", 0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        }
        mAdapter.update(mChannels);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChannels != null) {
            mResumed = true;
            updateLayout();
            mProgressBar.setVisibility(View.GONE);
        }
        setUpToolbar2();
        downloadFollowedChannels();
        mABHidden = false;
        AceAnims.showActionbar(getActivity(), false);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity) getActivity()).resetAdPosition();
        ((MainActivity) getActivity()).getToolbar().getMenu().clear();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
        restoreActionBar();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", mUrl);
        outState.putString("bar_title", mTitle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onChannelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChannelSelectedListener");
        }
    }

    public void downloadFollowedChannels() {
        downloadFollowedChannels(false);
    }

    public void downloadFollowedChannels(boolean force) {
        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            Preferences.showUsernameToast(getActivity());
            return;
        }
        if (mIsLoading) return;
        if (!force && System.currentTimeMillis() < mLastUpdated + 60000) return;

        mIsLoading = true;
        mTotalFollowingCount = -1;
        mLastUpdated = System.currentTimeMillis();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                mChannels = new ArrayList<>();
                String username = mPreferences.getString(Preferences.TWITCH_USERNAME, "");

                getChannels(username, DOWNLOAD_CHUNK_SIZE, 0);
                syncWithAppServer();
                checkChannelsOnlineStatus(ONLINE_CHUNK_SIZE, 0);

                Collections.sort(mChannels, new CustomComparator());

                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mIsLoading = false;
                        allDataComplete();
                    }
                });
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void getChannels(String uname, int limit, int offset) {
        if (getActivity() == null) return;
        String req = getString(R.string.twitch_user_url) + uname + getString(R.string.twitch_user_following_suffix);
        req += "limit=" + limit + "&offset=" + offset;
        String d = TwitchNetworkTasks.downloadStringData(req);

        if (mTotalFollowingCount < 0) {
            try {
                mTotalFollowingCount = new JSONObject(d).getInt("_total");
            } catch (JSONException | NullPointerException e) {
                return;
            }
        }
        mChannels.addAll(TwitchJSONParser.followedChannelsToArrayList(d));
        if (offset + limit <= mTotalFollowingCount)
            getChannels(uname, limit, offset + limit);
    }

    private void checkChannelsOnlineStatus(int limit, int offset) {
        if (getActivity() == null) return;
        String request = getString(R.string.channel_stream_url);
        request += "?channel=";

        int last = offset + limit <= mTotalFollowingCount ? offset + limit : mTotalFollowingCount;
        for (int i = offset; i < last; i++) {
            request += mChannels.get(i).getName() + ",";
        }
        request += "&limit=" + limit;

        String j = TwitchNetworkTasks.downloadStringData(request);
        ArrayList<Stream> streams = TwitchJSONParser.streamJSONtoArrayList(j);
        updateChannelOnlineStatus(streams);
        if (offset + limit <= mTotalFollowingCount) {
            checkChannelsOnlineStatus(limit, offset + limit);
        }
    }

    private void updateChannelOnlineStatus(ArrayList<Stream> streams) {
        int index;
        for (Stream str : streams) {
            index = mChannels.indexOf(str.getChannel());
            if (index >= 0) {
//                mChannels.set(index, str.getChannel());
                mChannels.get(index).setLogoLink(str.getPreviewLink());
                mChannels.get(index).setbIsOnline(true);
                mChannels.get(index).setViewers(str.getViewers());
            }
        }
    }

    private class CustomComparator implements Comparator<Channel> {
        @Override
        public int compare(Channel lhs, Channel rhs) {
            if (lhs.isbIsOnline() && rhs.isbIsOnline()) {
                if (lhs.getViewers() == rhs.getViewers())
                    return lhs.getName().compareTo(rhs.getName());
                return lhs.getViewers() < rhs.getViewers() ? 1 : -1;
            }
            if (lhs.isbIsOnline()) return -1;
            if (rhs.isbIsOnline()) return 1;
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    private void syncWithAppServer() {
        String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null) return;

        String token = mPreferences.getString(QuickstartPreferences.GCM_REG_KEY, null);
        if (token == null) return;

        ArrayList<String> chList = new ArrayList<>();
        ArrayList<Boolean> sList = new ArrayList<>();
        for (Channel c : mChannels) {
            chList.add(c.getName());
        }
        String starred = TwitchNetworkTasks.sendDataToAppServer(123, username, token, chList);
        mPreferences.edit().putString(Preferences.STRING_STARRED_CHANNELS, starred).apply();
        setStarred();
    }

    @Override
    public void onModeSwitched(boolean select) {
        if (getActivity() == null) return;
        if (select) {
            setUpToolbar(false);
            mABLocked = true;
            if (mABHidden) AceAnims.showActionbar(getActivity());
            ((MainActivity)getActivity()).setStatusBarColor(Color.BLACK);
        } else {
            ((MainActivity) getActivity()).getToolbar().getMenu().clear();
            mABLocked = false;
            setUpToolbar2();
            //restoreActionBar();
        }
    }

    @Override
    public void onSelectionChanged(boolean allStarred) {
        setUpToolbar(allStarred);
    }

    public void setStarred() {
        String s = mPreferences.getString(Preferences.STRING_STARRED_CHANNELS, "");
        if (s.isEmpty())
            return;
        for (Channel c : mChannels) {
            c.setIsStarred(s.contains(c.getName()));
        }
    }

    private void setUpToolbar(final boolean allStarred) {
        ((MainActivity) getActivity()).getToolbar().getMenu().clear();
        final Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        toolbar.setBackgroundResource(R.color.twitch_main_dark);
        if (toolbar.getMenu().size() < 2)
            toolbar.inflateMenu(R.menu.main);

        if (allStarred)
            toolbar.getMenu().findItem(R.id.action_notification).setIcon(R.drawable.ic_notifications_off_white);
        else
            toolbar.getMenu().findItem(R.id.action_notification).setIcon(R.drawable.ic_notifications_active_white);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_notification:
                        if (allStarred)
                            muteSelected();
                        else
                            unmuteSelected();
                        break;
                    case R.id.action_delete:
                        showUnFollowDialog(mAdapter.getSelectedChannels());
                        break;
                    case R.id.action_clear:
                        mAdapter.clearSelected();
                        onModeSwitched(false);
                        break;
                }
                return false;
            }
        });
    }

    private void setUpToolbar2() {
        ((MainActivity) getActivity()).getToolbar().getMenu().clear();
        final Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        toolbar.setBackgroundResource(R.color.twitch_main);
        ((MainActivity)getActivity()).setStatusBarColor(getResources().getColor(R.color.twitch_main));
        toolbar.inflateMenu(R.menu.follow);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_dropdown:
                        showDialog();
                        break;
                    case R.id.action_mute_all:
                        showMuteDialog();
                        break;
                }
                return false;
            }
        });
    }



    private void unmuteSelected() {
        final ArrayList<String> selected =  new ArrayList<>(mAdapter.getSelectedChannels());
        final String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null || selected.isEmpty()) return;
        onModeSwitched(false);
        updateChannelList(true, selected);
        mAdapter.update(mChannels);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TwitchNetworkTasks.syncStarredWithAppServer(username, selected, true);
            }
        }).start();
    }

    private void muteSelected() {
        final ArrayList<String> selected =  new ArrayList<>(mAdapter.getSelectedChannels());
        final String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null || selected.isEmpty()) return;
        onModeSwitched(false);
        updateChannelList(false, selected);
        mAdapter.update(mChannels);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TwitchNetworkTasks.syncStarredWithAppServer(username, selected, false);
            }
        }).start();
    }

    private void muteAll() {
        final ArrayList<String> selected =  new ArrayList<>(mAdapter.getAllChannels());
        final String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null || selected.isEmpty()) return;
        onModeSwitched(false);
        updateChannelList(false, selected);
        mAdapter.update(mChannels);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TwitchNetworkTasks.syncStarredWithAppServer(username, selected, false);
            }
        }).start();
    }

    private void showUnFollowDialog(ArrayList<String> s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String message = "Do your really want to unfollow ";
        String title = "Unfollow ";
        if (s.size() <= 0) return;
        if (s.size() == 1) {
            message += s.get(0) + "?";
            title += s.get(0);
        }
        if (s.size() > 1) {
            message += "these " + s.size() + " channels?";
            title += s.size() + " Channels";
        }
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        unfollowSelected();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void unfollowSelected() {
        final ArrayList<String> selected =  new ArrayList<>(mAdapter.getSelectedChannels());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                TwitchNetworkTasks.unFollowChannels(getActivity(), selected);
                downloadFollowedChannels(true);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        restoreActionBar();
                    }
                });
            }
        }).start();
    }

    private void updateChannelList(boolean b, ArrayList<String> selected) {
        for (Channel c : mChannels) {
            if (selected.contains(c.getName()))
                c.setIsStarred(b);
        }
    }

    private void restoreActionBar() {
        if (getActivity() == null) return;
        final Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        toolbar.setBackgroundResource(R.color.twitch_main);
        ((MainActivity)getActivity()).setStatusBarColor(getResources().getColor(R.color.twitch_main));
    }

    private void showDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog alert;

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View notifDialogView = inflater.inflate(R.layout.follow_notif_dialog, null);

        final Switch notifs = (Switch) notifDialogView.findViewById(R.id.notifSwitcherText);
        notifs.setChecked(mPreferences.getBoolean(Preferences.BOOL_NOTIFICATIONS_ACTIVE, true));

        final Switch silents = (Switch) notifDialogView.findViewById(R.id.silentSwitcherText);
        if (!mPreferences.getBoolean(Preferences.IS_PRO_USER,false)) {
            silents.setText("Silent Hours (Pro Feature)");
            silents.setChecked(false);
            silents.setClickable(false);
        } else {
            silents.setChecked(mPreferences.getBoolean(Preferences.BOOL_SILENT_ACTIVE, true));
        }

        final TextView fromTime = (TextView) notifDialogView.findViewById(R.id.fromTime);
        String fTime = mPreferences.getString(Preferences.STRING_SILENT_FROM, "23:00");
        fromTime.setText(fTime);

        final TextView untilTime = (TextView) notifDialogView.findViewById(R.id.untilTime);
        String uTime = mPreferences.getString(Preferences.STRING_SILENT_UNTIL, "09:00");
        untilTime.setText(uTime);

        fromTime.setOnClickListener(test);
        untilTime.setOnClickListener(test);

        builder.setView(notifDialogView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mPreferences.edit().putBoolean(Preferences.BOOL_NOTIFICATIONS_ACTIVE, notifs.isChecked()).apply();
                        mPreferences.edit().putBoolean(Preferences.BOOL_SILENT_ACTIVE, silents.isChecked()).apply();
                        mPreferences.edit().putString(Preferences.STRING_SILENT_FROM, fromTime.getText().toString()).apply();
                        mPreferences.edit().putString(Preferences.STRING_SILENT_UNTIL, untilTime.getText().toString()).apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        alert = builder.create();
        alert.show();
    }

    private void showMuteDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog alert;
        builder.setTitle("Mute all Channels?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        muteAll();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        alert = builder.create();
        alert.show();
    }

    private View.OnClickListener test = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            int[] t = LayoutTasks.stringTimeToInt(((TextView) v).getText().toString());
            new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    String time = LayoutTasks.formatTime(hourOfDay, minute);
                    ((TextView)v).setText(time);
                }
            }, t[0], t[1], true).show();
        }
    };

}