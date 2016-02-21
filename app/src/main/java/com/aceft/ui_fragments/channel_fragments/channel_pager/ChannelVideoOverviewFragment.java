package com.aceft.ui_fragments.channel_fragments.channel_pager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.data.LayoutTasks;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.TwitchUser;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class ChannelVideoOverviewFragment extends Fragment {

    private Channel mChannel;
    private boolean mIsLive;
    private String mChannelName;

    private SharedPreferences mPreferences;

    private boolean mCComplete, mUComplete, mOComplete, mFComplete;

    private ProgressBar mProgressBar;
    private RelativeLayout mVideoHeader;
    private boolean mIsAuthenticated;
    private String mUserToken;
    private TwitchUser mUser;
    private boolean mIsFollowing = false;

    public ChannelVideoOverviewFragment newInstance(String name) {
        ChannelVideoOverviewFragment fragment = new ChannelVideoOverviewFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_overview, container, false);
        mVideoHeader = (RelativeLayout) rootView.findViewById(R.id.channelData);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsAuthenticated = mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);
        if (mIsAuthenticated) {
            mUserToken = mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mChannelName = getArguments().getString("channel_name");

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);

        downloadChannelData();
        downloadUserData();
        checkIfChannelOnline();
        downloadFollowData();
        return rootView;
    }

    //------------------ Header Stuff -------------------------///////////////////
    private void downloadChannelData() {
        String request = getActivity().getResources().getString(R.string.channel_url);
        request += getArguments().getString("channel_name");
        if (mIsAuthenticated) request += "?oauth_token=" + mUserToken;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 0);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void channelDataReceived(String s) {
        mChannel = TwitchJSONParser.channelStringToChannel(s);
        mCComplete = true;
        dataComplete();
    }

    private void downloadUserData() {
        String request = getActivity().getResources().getString(R.string.twitch_user_url);
        request += getArguments().getString("channel_name");
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 1);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void userDataReceived(String s) {
        mUser = TwitchJSONParser.userDataToUser(s);
        mUComplete = true;
        dataComplete();
    }

    private void checkIfChannelOnline(){
        final String request = getActivity().getResources().getString(R.string.channel_stream_url)
                + getArguments().getString("channel_name");
        new Thread(new Runnable() {
            public void run() {
                final JSONObject j = TwitchNetworkTasks.downloadJSONData(request);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (j.getString("stream").equals("null"))
                                onlineDataReceived(false);
                            else
                                onlineDataReceived(true);
                        } catch (JSONException | NullPointerException e) {
                            onlineDataReceived(false);
                        }
                    }
                });

            }
        }).start();
    }

    private void onlineDataReceived(boolean b){
        mIsLive = b;
        mOComplete = true;
        dataComplete();
    }

    private void downloadFollowData() {
        String req = mPreferences.getString(Preferences.TWITCH_USERNAME, "");
        req = getString(R.string.twitch_user_url) + req + getString(R.string.twitch_user_following_suffix);
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 2);
        t.downloadJSONInBackground(req, Thread.NORM_PRIORITY);
    }

    public void followDataReceived(String s) {
        if (s != null) {
            ArrayList<Channel> channels = TwitchJSONParser.followedChannelsToArrayList(s);
            for (Channel c: channels) {
                if (c.getName().equals(mChannelName)) {
                    mIsFollowing = true;
                    break;
                }
            }
        }
        mFComplete = true;
        dataComplete();
    }

    private void dataComplete() {
        if (mCComplete && mUComplete && mFComplete && mOComplete) {
            updateLayout();
        }
    }

    public void updateLayout() {
        ImageView mChannelBanner = (ImageView) mVideoHeader.findViewById(R.id.channelBanner);

        if (isInLandscape()) {
            mChannelBanner.getLayoutParams().height = (int) (0.3f * getWindowHeight());
            mChannelBanner.getLayoutParams().width = (int) (0.3f * getWindowHeight());
        } else {
            mChannelBanner.getLayoutParams().height = (int) (0.4f * getWindowWidth());
            mChannelBanner.getLayoutParams().width = (int) (0.4f * getWindowWidth());
        }

        String picUrl = mChannel.getLogoLink().isEmpty() ?
                "null" : mChannel.getLogoLink();
        Picasso.with(getActivity())
                .load(picUrl)
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(mChannelBanner);

        if (mIsLive) {
            mVideoHeader.findViewById(R.id.textLive).setVisibility(View.VISIBLE);
            mChannelBanner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getFragmentManager().popBackStack();
                }
            });
        }
        else
            mVideoHeader.findViewById(R.id.textLive).setVisibility(View.GONE);

        ((TextView) mVideoHeader.findViewById(R.id.textTitleView)).setText(mChannel.getDisplayName());
        ((TextView) mVideoHeader.findViewById(R.id.textPlayingGame))
                .setText("Playing " + mChannel.getGame());
        String bio = mUser.getBio();
        if (bio.equals("null")) bio = "";
        ((TextView) mVideoHeader.findViewById(R.id.textBioView)).setText(bio);
        ((TextView) mVideoHeader.findViewById(R.id.textFollowers))
                .setText(LayoutTasks.formatNumber(mChannel.getFollowers()) + " Followers and "
                        + LayoutTasks.formatNumber(mChannel.getViews()) + " Views");

        TextView textUrl = (TextView) mVideoHeader.findViewById(R.id.textUrl);
        textUrl.setText(mChannel.getUrl());

        ((TextView) mVideoHeader.findViewById(R.id.textMature))
                .setText("Mature Content: " + mChannel.getMature());
        ((TextView) mVideoHeader.findViewById(R.id.textCreated))
                .setText("On Twitch since: " + mChannel.getCreated());

        if (mIsFollowing) {
            ((TextView)mVideoHeader.findViewById(R.id.followingText)).setText("Following");
            mVideoHeader.findViewById(R.id.followingLayout).setBackgroundResource(R.color.twitch_following_channel);
        } else {
            ((TextView)mVideoHeader.findViewById(R.id.followingText)).setText("Follow");
            mVideoHeader.findViewById(R.id.followingLayout).setBackgroundResource(R.color.twitch_main);
            mVideoHeader.findViewById(R.id.followingText).setVisibility(View.VISIBLE);
        }

        mVideoHeader.findViewById(R.id.followingLayoutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsFollowing) {
                    if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                        Preferences.showUsernameToast(getActivity());
                    } else if (!mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
                        Preferences.showUsernameToast(getActivity());
                    } else {
                        showFollowDialog();
                    }
                } else {
                    showUnFollowDialog();
                }
            }
        });

        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mVideoHeader, "alpha",  0f, 1f);
        fadeInStream.setDuration(500);
        fadeInStream.start();
        mVideoHeader.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    private void showFollowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Channel Follow")
                .setMessage("Do you want to follow this channel?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showNotificationDialog();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showNotificationDialog() {
        final String req = getActivity().getResources().getString(R.string.twitch_user_url)
                + mPreferences.getString(Preferences.TWITCH_USERNAME, "")
                + "/follows/channels/" + mChannelName + "?oauth_token="
                + mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Channel Follow")
                .setMessage("Do you want to receive twitch-notifications if the channel goes live?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        followThisChannel(req + "&notifications=true");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        followThisChannel(req + "&notifications=false");
                    }
                });
        builder.create();
        builder.show();
    }

    private void followThisChannel(String s) {
        final String req = s;
        new Thread(new Runnable() {
            public void run() {
                final String is = TwitchNetworkTasks.followChannel(req);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        followResponseReceived(is);
                    }
                });
            }
        }).start();
    }

    private void followResponseReceived(String s) {
        if (s == null) s = "";
        if (s.contains("channel")) {
            Toast.makeText(getActivity(), "You are now following " + mChannel.getDisplayName(),
                    Toast.LENGTH_LONG).show();
            mIsFollowing = true;
            ((MainActivity) getActivity()).refreshNavDrawer();
            updateLayout();
        } else {
            Toast.makeText(getActivity(), "Something went wrong. Try to refresh your token under settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showUnFollowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Channel Unfollow")
                .setMessage("Do your really want to unfollow this channel?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        unFollowThisChannel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void unFollowThisChannel() {
        final String req = getActivity().getResources().getString(R.string.twitch_user_url)
                + mPreferences.getString(Preferences.TWITCH_USERNAME, "")
                + "/follows/channels/" + mChannelName + "?oauth_token="
                + mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");
        new Thread(new Runnable() {
            public void run() {
                final String is = TwitchNetworkTasks.unFollowChannel(req);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unFollowResponseReceived(is);
                    }
                });
            }
        }).start();
    }

    private void unFollowResponseReceived(String is) {
        if (is == null) {
            Toast.makeText(getActivity(), "Something went wrong. Try to refresh your token under settings.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (is.isEmpty()) {
            Toast.makeText(getActivity(), "You have successfully unfollowed " + mChannel.getDisplayName(),
                    Toast.LENGTH_SHORT).show();
            ((MainActivity) getActivity()).refreshNavDrawer();
            mIsFollowing = false;
            updateLayout();
        } else {
            Toast.makeText(getActivity(), "Something went wrong. Try to refresh your token under settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private int getWindowHeight() {
        if (getActivity() == null)
            return 0;
        int height;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        height = size.y;
        return height;
    }

    private int getWindowWidth() {
        if (getActivity() == null)
            return 0;
        int width;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        return width;
    }

    private boolean isInLandscape() {
        if (getActivity() == null) return false;
        return getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

}