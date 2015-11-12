package com.aceft.ui_fragments.front_pages;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.DrawerAdapter;
import com.aceft.data.CircleTransform;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NavigationDrawerFragment extends Fragment {

    private NavigationDrawerCallbacks mCallbacks;
    private int DOWNLOAD_CHUNK_SIZE = 200;
    private int ONLINE_CHUNK_SIZE = 100;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;

    private View mFragmentContainerView;
    private String mOldUsername;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer, mUserHasCompletedSetup;

    private ArrayList<Channel> mChannels;
    private ArrayList<Channel> mOnline;
    private DrawerAdapter mDrawerAdapter;

    private SharedPreferences mPreferences;
    private long mLastUpdated = 0;
    private int mTotalFollowingCount = -1;
    private RelativeLayout mDrawerHeader;
    private boolean mIsLoading;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = mPreferences.getBoolean(Preferences.PREF_USER_LEARNED_DRAWER, false);
        mUserHasCompletedSetup = mPreferences.getBoolean(Preferences.PREF_USER_COMPLETED_SETUP, false);
        mCurrentSelectedPosition = mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(Preferences.STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }
        mOldUsername = mPreferences.getString(Preferences.TWITCH_USERNAME, "");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_navigation_drawer2, container, false);

        mDrawerHeader = (RelativeLayout) rootView.findViewById(R.id.drawerHeader);
        mDrawerListView = (ListView) rootView.findViewById(R.id.list_items);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        String sections[] = new String[]{
                getString(R.string.title_section1),
                getString(R.string.title_section2),
                getString(R.string.title_section3),
                getString(R.string.title_section4)
        };

        int drawables[] = new int[]{
                R.drawable.drawer_games,
                R.drawable.drawer_channel,
                R.drawable.drawer_search,
                R.drawable.drawer_favorites
        };

        String footer[] = new String[]{"Settings", "Go Pro", "Contact"};
        if (((MainActivity) getActivity()).checkLicence2())
            footer = new String[]{"Settings", "Feedback", "Support"};

        mDrawerAdapter = new DrawerAdapter(getActivity(), sections, drawables, footer);
        mDrawerListView.setAdapter(mDrawerAdapter);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.userLogo);
        String username = mPreferences.getString(Preferences.TWITCH_USERNAME, null);
        if (imageView != null && username != null)
            updateLogo(imageView, username);

        downloadFollowedChannels();

        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return rootView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            ((TextView) mDrawerHeader.findViewById(R.id.drawerHeaderUsername)).setText("Ace for Twitch");
            mDrawerHeader.setVisibility(View.VISIBLE);
        } else {
            String displayUsername = mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, "");
            ((TextView) mDrawerHeader.findViewById(R.id.drawerHeaderUsername)).setText(displayUsername);
            mDrawerHeader.setVisibility(View.VISIBLE);
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                null,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                refreshDrawer(true);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(Preferences.PREF_USER_LEARNED_DRAWER, true).apply();
                }

                //getActivity().invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
    }

    public void refreshDrawer(boolean forced) {
        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            //mDrawerHeader.setVisibility(View.GONE);
            mChannels = null;
            return;
        }
        String username = mPreferences.getString(Preferences.TWITCH_USERNAME, "");
        String displayUsername = mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, "");
        ((TextView) mDrawerHeader.findViewById(R.id.drawerHeaderUsername)).setText(displayUsername);
        mDrawerHeader.setVisibility(View.VISIBLE);
        mOldUsername = username;
        downloadFollowedChannels();
    }

    public void selectHome() {
        if (!mFromSavedInstanceState)
            selectItem(mCurrentSelectedPosition);
    }

    public void selectItem(int position) {
        if (getActivity() == null) return;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserHasCompletedSetup = sp.getBoolean(Preferences.PREF_USER_COMPLETED_SETUP, false);

        if (position == 4) return;
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            if (!mUserHasCompletedSetup) {
                mCallbacks.onNavigationDrawerItemSelected(-1);
                mCurrentSelectedPosition = 0;
                return;
            }
            if (position < 8)
                mCallbacks.onNavigationDrawerItemSelected(position);
            if (position == 9) {
                deselectList();
            }
            if (position > 9)
                channelClicked(position);
        }
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mFragmentContainerView);
    }

    public void selectListItem(int position) {
        mDrawerListView.setItemChecked(position, true);
    }

    public void deselectList() {
        mDrawerListView.setItemChecked(mDrawerListView.getCheckedItemPosition(), false);
    }

    public void disableDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void enableDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Preferences.STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void updateLogo(final ImageView v, final String u) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject j = TwitchNetworkTasks.downloadJSONData(getString(R.string.channel_url) + u);
                try {
                    final String lUrl = j.getString("logo");
                    if (lUrl == null || lUrl.equals("null") || getActivity() == null) return;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.with(getActivity())
                                    .load(lUrl)
                                    .transform(new CircleTransform())
                                    .into(v);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public void downloadFollowedChannels() {
        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false) || mIsLoading) {
            return;
        }
        mIsLoading = true;
        mLastUpdated = System.currentTimeMillis();
        mTotalFollowingCount = -1;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                mChannels = new ArrayList<>();
                mOnline = new ArrayList<>();
                String username = mPreferences.getString(Preferences.TWITCH_USERNAME, "");
                mOnline = Channel.getOnlineChannels(username, getActivity());

                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mIsLoading = false;
                        mDrawerAdapter.clearFollowed();
                        mDrawerAdapter.updateData(new ArrayList<>(mOnline));
                    }
                });
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void channelClicked(int p) {
        ((MainActivity) getActivity()).onChannelSelected(mDrawerAdapter.getChannel(p));
        closeDrawer();
    }

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
