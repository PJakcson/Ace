package com.aceft;

import android.animation.ObjectAnimator;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Game;
import com.aceft.data.primitives.Stream;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.ui_fragments.channel_fragments.ChatFragment;
import com.aceft.ui_fragments.channel_fragments.VideoFragment;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelPagerFragment;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVodCategoryFragment;
import com.aceft.ui_fragments.front_pages.FollowedListFragment;
import com.aceft.ui_fragments.front_pages.GamesRasterFragment;
import com.aceft.ui_fragments.front_pages.NavigationDrawerFragment;
import com.aceft.ui_fragments.front_pages.SearchFragment;
import com.aceft.ui_fragments.front_pages.SettingsFragment;
import com.aceft.ui_fragments.front_pages.StreamListFragment;
import com.aceft.ui_fragments.setup.SetupPagerFragment;
import com.gms.QuickstartPreferences;
import com.gms.RegistrationIntentService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GamesRasterFragment.OnGameSelectedListener, StreamListFragment.onStreamSelectedListener,
        FollowedListFragment.onChannelSelectedListener,
        ChannelVodCategoryFragment.onOldVideoSelectedListener, SearchFragment.OnGameSelectedListener,
        SearchFragment.onChannelSelectedListener, FragmentManager.OnBackStackChangedListener {

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private static final String ARG_ACTIONBAR_TITLE = "action_bar";
    private String mUrls[];
    private AdView mAdView;
    private String mUsername;
    private InterstitialAd interstitial;
    private boolean proVersion;
    private SharedPreferences mPreferences;
    private Toolbar mToolbar;

    // GMS Stuff
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrls = getResources().getStringArray(R.array.drawer_urls);
        setBitmapQuality();
        setContentView(R.layout.activity_main);
        mUsername = "öaoibsnwotzböslfhösudasodvasopdfoasngdüas";

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.abc_primary_text_material_dark));
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            findViewById(R.id.container).setPadding(0, getStatusBarHeight(), 0, 0);
            findViewById(R.id.placeholder).getLayoutParams().height += getStatusBarHeight();
            mToolbar.setPadding(0, getStatusBarHeight(), 0, 0);
            mToolbar.getLayoutParams().height += getStatusBarHeight();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        checkLicence();

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        String intentData = getIntent().getStringExtra("intent");
        if (intentData != null && !intentData.isEmpty()) {
            handleIntent(getIntent());
        } else {
            mNavigationDrawerFragment.selectHome();
        }

        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }


    private void checkLicence() {
        final PackageManager pacman = getPackageManager();
        final int signatureMatch = pacman.checkSignatures(getPackageName(), "com.acefortwitchkey");

        if (signatureMatch == PackageManager.SIGNATURE_MATCH) {
            proVersion = true;
            mAdView = null;
            interstitial = null;
            if (!mPreferences.getBoolean(Preferences.HAS_SEEN_PRO_MESSAGE, false)) {
                mPreferences.edit().putBoolean(Preferences.HAS_SEEN_PRO_MESSAGE, true).apply();
                mPreferences.edit().putBoolean(Preferences.IS_PRO_USER, true).apply();
                Toast.makeText(this, "Thanks for purchasing the App! Removing Ads..", Toast.LENGTH_LONG).show();
            }
        } else {
            proVersion = false;
            mPreferences.edit().putBoolean(Preferences.HAS_SEEN_PRO_MESSAGE, false).apply();
            mPreferences.edit().putBoolean(Preferences.IS_PRO_USER, false).apply();
            mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("E5E629B10D8B4A0F1BFB41DFA591AED8").build();

            if (Math.random() < 0.5)
                mAdView.loadAd(adRequest);
        }
    }

    public boolean checkLicence2() {
        final PackageManager pacman = getPackageManager();
        final int signatureMatch = pacman.checkSignatures(getPackageName(), "com.acefortwitchkey");
        return signatureMatch == PackageManager.SIGNATURE_MATCH;
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentTransaction transaction;
        String home;
        switch (position){
            case 0:
                home = String.valueOf(mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0));
                if (home.equals("0"))
                    clearBackStack();

                GamesRasterFragment mGamesRasterFragment = new GamesRasterFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, mGamesRasterFragment.newInstance(mUrls[position]), "0");
                transaction.addToBackStack("0");
                transaction.commit();
                break;
            case 1:
                home = String.valueOf(mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0));
                if (home.equals("1"))
                    clearBackStack();

                StreamListFragment mStreamListFragment = new StreamListFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, mStreamListFragment.newInstance(mUrls[position], null));
                transaction.addToBackStack("1");
                transaction.commit();
                break;
            case 2:
                home = String.valueOf(mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0));
                if (home.equals("2"))
                    clearBackStack();

                SearchFragment searchFragment = new SearchFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, searchFragment, "channel");
                transaction.addToBackStack("2");
                transaction.commit();
                break;
            case 3:
                home = String.valueOf(mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0));
                if (home.equals("3"))
                    clearBackStack();

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                if (!sp.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    gotoHome();
                    Toast.makeText(this, getString(R.string.username_not_set), Toast.LENGTH_LONG).show();
                } else {
                    String req = sp.getString(Preferences.TWITCH_USERNAME, "");
                    req = getString(R.string.twitch_user_url) + req + getString(R.string.twitch_user_following_suffix);
                    FollowedListFragment favoritesFragment = new FollowedListFragment();
                    transaction = getFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.container, favoritesFragment.newInstance(req));
                    transaction.addToBackStack("3");
                    transaction.commit();
                }
                break;
            case 4:
                //divider
                break;
//            case 5:
//                gopro
//                break;
            case 5:
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, new SettingsFragment(), "settings");
                transaction.addToBackStack("6");
                transaction.commit();
                break;
            case 6:
                if (proVersion)
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:acefortwitch@gmail.com?subject=Feedback " + Build.MANUFACTURER + " " + Build.MODEL)));
                else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.acefortwitchkey")));
                }
                break;
            case 7:
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:acefortwitch@gmail.com?subject=Support " + Build.MANUFACTURER + " " + Build.MODEL)));
                break;
            case -1:
                setDefaultSettings();
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, new SetupPagerFragment(), "setup");
                transaction.commit();
                break;
        }
    }

    private void clearBackStack() {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        while (getParent() != null)
            getParent().finish();

    }

    public void setBitmapQuality() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        String qArray[] = getResources().getStringArray(R.array.settings_bitmap_qualities);
        String q = sp.getString(Preferences.TWITCH_BITMAP_QUALITY, "");

        if (q.contains(qArray[0])) TwitchJSONParser.setHighQuality();
        if (q.contains(qArray[1])) TwitchJSONParser.setMediumQuality();
        if (q.contains(qArray[2])) TwitchJSONParser.setSmallQuality();
    }

    private void setDefaultSettings() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putInt(Preferences.APP_DEFAULT_HOME, 0);
        sp.edit().putString(Preferences.TWITCH_STREAM_QUALITY_TYPE, getString(R.string.default_stream_quality_type)).apply();
        sp.edit().putString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, getString(R.string.default_preferred_video_quality)).apply();
        sp.edit().putString(Preferences.TWITCH_AUTOPLAY_MODE, getResources().getStringArray(R.array.autoplay_settings)[0]).apply();
        String defaultBitmap = getResources().getStringArray(R.array.settings_bitmap_qualities)[0];
        sp.edit().putString(Preferences.TWITCH_BITMAP_QUALITY, defaultBitmap).apply();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLicence();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int itemCount = fm.getBackStackEntryCount();

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
            return;
        }

        if (fm.findFragmentByTag("setup") != null) {
            fm.beginTransaction().remove(fm.findFragmentByTag("setup")).commit();
            mNavigationDrawerFragment.enableDrawer();
            gotoHome();
        }else if (itemCount > 1) {
            fm.popBackStack();
        } else {
//            if (interstitial != null && !proVersion && interstitial.isLoaded()) {
//                interstitial.show();
//            }
            super.onBackPressed();
        }
    }

    public void exitSetup(SetupPagerFragment s) {
        mNavigationDrawerFragment.enableDrawer();
        getFragmentManager().beginTransaction().remove(s).commit();
        gotoHome();
    }

    private void synchronizeDrawer(String i) {
        if(i == null) return;
        switch (i) {
            case "0": mNavigationDrawerFragment.selectListItem(0); return;
            case "1": mNavigationDrawerFragment.selectListItem(1); return;
            case "2": mNavigationDrawerFragment.selectListItem(2); return;
            case "3": mNavigationDrawerFragment.selectListItem(3); return;
            case "6": mNavigationDrawerFragment.selectListItem(5); return;
            case "7": mNavigationDrawerFragment.selectListItem(6); return;
            case "8": mNavigationDrawerFragment.selectListItem(7); return;
        }
        mNavigationDrawerFragment.deselectList();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            savedInstanceState.putString(ARG_ACTIONBAR_TITLE, (String) actionBar.getTitle());
    }


    @Override
    public void onGameSelected(Game g) {
        mNavigationDrawerFragment.deselectList();
        String url = getString(R.string.game_streams_url);
        url += g.toURL() + "&";
        StreamListFragment mStreamListFragment = new StreamListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mStreamListFragment.newInstance(url, g.mTitle));
        transaction.addToBackStack(g.mId);
        transaction.commit();
    }

    @Override
    public void onStreamSelected(Stream g) {
//        getSupportActionBar().hide();
        mNavigationDrawerFragment.deselectList();
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel", g.getName());
        startActivity(intent);
//        ChannelFragment f = new ChannelFragment();
//        FragmentTransaction transaction = getFragmentManager().beginTransaction();
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        transaction.replace(R.id.container, f.newInstance(g.getName()), "channel");
//        transaction.addToBackStack("channel");
//        transaction.commit();
    }

    @Override
    public void onChannelSelected(Channel c) {
//        getWindow().setEnterTransition(new Fade());
//        getWindow().setExitTransition(new Fade());
        mNavigationDrawerFragment.deselectList();
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel", c.getName());
        startActivity(intent);
//        mNavigationDrawerFragment.deselectList();
//        ChannelFragment f = new ChannelFragment();
//        FragmentTransaction transaction = getFragmentManager().beginTransaction();
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        transaction.replace(R.id.container, f.newInstance(c.getName()), "channel");
//        transaction.addToBackStack("channel");
//        transaction.commit();
    }

    @Override
    public void onOldVideoSelected(TwitchVod t1, TwitchVideo t2) {
        VideoFragment videoFragment = new VideoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, videoFragment.newInstance(t1, t2));
        transaction.addToBackStack("video");
        transaction.commit();
    }

    public void gotoHome() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        int home = sp.getInt(Preferences.APP_DEFAULT_HOME, 0);
        mNavigationDrawerFragment.selectItem(home);
    }

    public void pauseAd() {
        if (mAdView == null || proVersion) return;
        mAdView.setVisibility(View.GONE);
    }

    public void resumeAd() {
        if (mAdView == null || proVersion) return;
        mAdView.setVisibility(View.VISIBLE);
    }

    public void setUpAd() {
        if (proVersion) return;
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("E5E629B10D8B4A0F1BFB41DFA591AED8").build();
        interstitial.loadAd(adRequest);
    }

    public void resetAdPosition() {
        if (mAdView == null || proVersion) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", mAdView.getMeasuredHeight(), 0f);
        fadeInStream.setDuration(0);
        fadeInStream.start();
    }

    public void pushUpAd() {
        if (mAdView == null || proVersion) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", mAdView.getMeasuredHeight(), 0f);
        fadeInStream.setDuration(300);
        fadeInStream.start();
    }

    public void pushDownAd() {
        if (mAdView == null || proVersion) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", 0f, mAdView.getMeasuredHeight());
        fadeInStream.setDuration(300);
        fadeInStream.start();
    }

    public void setAdPosition(int pos) {
        if (mAdView == null || proVersion) return;
        if (mAdView.getLayoutParams() == null) return;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mAdView.getLayoutParams();
        if (pos == RelativeLayout.ALIGN_PARENT_TOP) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        }
        if (pos == RelativeLayout.ALIGN_PARENT_BOTTOM) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        mAdView.setLayoutParams(params);
    }

    public void disableDrawer() {
        mNavigationDrawerFragment.disableDrawer();
    }

    public void enableDrawer() {
        mNavigationDrawerFragment.enableDrawer();
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() <= 0) return;
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (!sp.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            mUsername = "";
        } else if (!sp.getString(Preferences.TWITCH_USERNAME, "").equals(mUsername)) {
            mUsername = sp.getString(Preferences.TWITCH_USERNAME, "");
        }
        synchronizeDrawer(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName());
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        switch (intent.getStringExtra("intent")) {
            case Preferences.INT_FULLSCREEN_CHAT:
                startFullScreenChat(intent.getStringExtra("channel"));
                break;
            case Preferences.INT_CHANNEL_PAGER:
                onChannelPagerSelected(intent.getStringExtra("channel"), getIntent().getIntExtra("page", 0));
                break;
            case Preferences.INT_CHANNEL:
                onChannelSelected(new Channel(intent.getStringExtra("channel")));
                break;
            default:
                mNavigationDrawerFragment.selectHome();
        }
        intent.removeExtra("intent");
    }

    public void onChannelPagerSelected(String c, int page) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container,
                new ChannelPagerFragment().newInstance(c, c, page));
        transaction.addToBackStack("pager");
        transaction.commitAllowingStateLoss();
    }

    public void startFullScreenChat(String c) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container,
                new ChatFragment().newInstance(c, c));
        transaction.addToBackStack("chat");
        transaction.commit();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void refreshNavDrawer() {
        mNavigationDrawerFragment.refreshDrawer(true);
    }

//    private void handleIntent(String intentData) {
//        String data[] = intentData.split("/");
//        int length = data.length;
//
//        if (data[length-1].equals("mobile")) length--;
//        if (length < 4) return;
//
//        String d3 = data[3];
//        if (d3.isEmpty()) return;
//
//        if (d3.equals("messages")) return;
//        if (d3.equals("directory")) {
//            if (length == 4) return;
//            String d4 = data[4];
//            if (d4.equals("following")) {
//                //GOTO Following
//                return;
//            }
//            if (d4.equals("all")) {
//                //GOTO Top-Streams
//                return;
//            }
//        }
//
//        if (length == 4) {
//            return;
//        }
//
//        String d5 = data[4];
//        if (d5.equals("profile")) {
//            if (length == 5) {
//                //GOTO Profile
//                return;
//            }
//            if (data[5].equals("past_broadcasts")) {
//                //GOTO PastBroadcasts
//                return;
//            }
//            if (data[5].length() == 1) {
//                //GOTO VOD
//            }
//        }
//    }
//
//    private void errorIntent() {
//        Toast.makeText(this, "Could not handle Url", Toast.LENGTH_LONG).show();
//    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void setStatusBarColor(int color) {
        findViewById(R.id.placeholder).setBackgroundColor(color);
    }
}
