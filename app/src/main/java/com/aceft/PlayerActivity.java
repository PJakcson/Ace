package com.aceft;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.aceft.data.Preferences;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.ui_fragments.channel_fragments.ChannelCompactFragment;
import com.aceft.ui_fragments.channel_fragments.ChannelFragment;
import com.aceft.ui_fragments.channel_fragments.VideoFragment;
import com.aceft.ui_fragments.channel_fragments.video.VideoPlayerFragment;
import com.aceft.ui_fragments.front_pages.NavigationDrawerFragment;
import com.aceft.ui_fragments.setup.AuthFragment;

public class PlayerActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        VideoPlayerFragment.MediaPlayerControl, ChannelCompactFragment.OnItemSelectedListener {

    private ChannelFragment mChannelFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Window window = this.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            window.setStatusBarColor(Color.BLACK);

        String channel = getIntent().getStringExtra("channel");

        mChannelFragment = new ChannelFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mChannelFragment.newInstance(channel), "channel");
        transaction.addToBackStack("channel");
        transaction.commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        ChannelFragment cf =  ((ChannelFragment)getFragmentManager().findFragmentByTag("channel"));
        if (cf != null && cf.getFABExpanded()) {
            cf.hideExpanded();
            return;
        }
        if (cf != null && cf.getEmotisShown()) {
            cf.hideEmotis();
            return;
        }

        if(!isTaskRoot())
            super.onBackPressed();
        else {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void toggleFullScreen() {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).setFullScreen();
    }

    @Override
    public void toggleSettingsScreen() {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).openPlayerSettings();
    }

    @Override
    public void streamStateChanged(int i) {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).streamStateChanged(i);
    }

    @Override
    public void doFollow() {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).toggleFollow();
    }

    @Override
    public void doShare() {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).showShareDialog();
    }

    @Override
    public void controlsShown() {
        ((ChannelFragment)getFragmentManager().findFragmentByTag("channel")).updateViewersCount(0);
    }

    @Override
    public void onChannelPagerSelected(Channel c, int page) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("intent", Preferences.INT_CHANNEL_PAGER);
        intent.putExtra("channel", c.getName());
        intent.putExtra("page", page);
        startActivity(intent);
    }

    @Override
    public void startFullScreenChat(Channel c) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("intent", Preferences.INT_FULLSCREEN_CHAT);
        intent.putExtra("channel", c.getName());
        startActivity(intent);
    }

    @Override
    public void oldVodSelected(TwitchVod twitchVod, TwitchVideo twitchVideo) {
        VideoFragment videoFragment = new VideoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, videoFragment.newInstance(twitchVod, twitchVideo));
        transaction.addToBackStack("oldvideo");
        transaction.commit();
    }

    public void refreshToken() {
        AuthFragment a = new AuthFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, a);
        transaction.addToBackStack("auth");
        try {
            transaction.commit();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
