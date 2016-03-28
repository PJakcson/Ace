package com.aceft.ui_fragments.setup;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.aceft.R;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchNetworkTasks;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class AuthFragment extends Fragment {
    private int mNumberOfAttempts = 0;
    private View mRootView;
    private ProgressBar mProgress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_auth, container, false);
        mProgress = (ProgressBar) mRootView.findViewById(R.id.authProgress);

        try {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle("Authentication");
        } catch (NullPointerException ignored) {

        }

        loadTwitchAuthentication();

        return mRootView;
    }

    private void loadTwitchAuthentication() {
        mProgress.setVisibility(View.VISIBLE);
        final WebView w = (WebView) mRootView.findViewById(R.id.webView);
        final String get_oauth_token = getActivity().getResources().getString(R.string.twitch_oauth_get_token_url);
        final String oauth_base = getActivity().getResources().getString(R.string.twitch_oauth_base_url);
//        w.getSettings().setJavaScriptEnabled(true);

        w.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgress.setVisibility(View.GONE);
                if (mNumberOfAttempts > 4) return;
                if (url.contains("access_token=")) {
                    w.setVisibility(View.GONE);

                    int index_token = url.indexOf("=")+1;
                    int index_middle = url.lastIndexOf("&");
                    int index_scope = url.lastIndexOf("=")+1;
                    String token = url.substring(index_token, index_middle);
                    String scopes = url.substring(index_scope, url.length());
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, true).apply();
                    sp.edit().putString(Preferences.USER_AUTH_TOKEN, token).apply();
                    sp.edit().putString(Preferences.SCOPES_OF_USER, scopes).apply();
                    sp.edit().putInt(Preferences.AUTH_WARNING, 0).apply();

                    new DownloadJSONTask(0).execute(oauth_base + token);
                    mProgress.setVisibility(View.VISIBLE);
                }
                if (url.contains("error=access_denied")) {
                    getActivity().getFragmentManager().popBackStack();
                }
            }
        });
        w.loadUrl(get_oauth_token);
    }

    private void usernameConfirmed(String username, String userDisplayName) {
        mProgress.setVisibility(View.GONE);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sp.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, true).apply();
        sp.edit().putString(Preferences.TWITCH_USERNAME, username).apply();
        sp.edit().putString(Preferences.TWITCH_DISPLAY_USERNAME, userDisplayName).apply();
        sp.edit().putInt(Preferences.USERNAME_WARNING, 0).apply();
        sp.edit().putInt(Preferences.AUTH_WARNING, 0).apply();
        getActivity().getFragmentManager().popBackStack();
        Toast.makeText(getActivity(), getActivity().getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
    }

    private void oauthDataReceived(JSONObject loggedIn) {
        String username;
        try {
            username = loggedIn.getJSONObject("token").getString("user_name");
            String user_url = getActivity().getResources().getString(R.string.twitch_user_url) + username;
            new DownloadJSONTask(1).execute(user_url);
        } catch (JSONException | NullPointerException e) {
            Toast.makeText(getActivity(), "Usertoken not valid", Toast.LENGTH_LONG).show();
        }
    }

    private void userSearchDataReceived(JSONObject userData) {
        String username, userDisplayName;
        try {
            username = userData.getString("name");
            userDisplayName = userData.getString("display_name");
            usernameConfirmed(username, userDisplayName);
        } catch (JSONException | NullPointerException e) {
            Toast.makeText(getActivity(), "Username not valid", Toast.LENGTH_LONG).show();
        }
    }

    private class DownloadJSONTask extends AsyncTask<String, Void, JSONObject> {

        private final int type;

        public DownloadJSONTask(int type) {
            this.type = type;
        }

        protected JSONObject doInBackground(String... urls) {
            return TwitchNetworkTasks.downloadJSONData(urls[0]);
        }

        protected void onPostExecute(JSONObject result) {
            if (type == 0) oauthDataReceived(result);
            if (type == 1) userSearchDataReceived(result);
        }
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
    public void onResume() {
//        ((PlayerActivity)getActivity()).pauseAd();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}