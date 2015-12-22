package com.aceft.ui_fragments.setup;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.custom_layouts.NonSwipeViewPager;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchNetworkTasks;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SetupPagerFragment extends Fragment{
    private int current = 0;
    private NonSwipeViewPager mPager;
    private RadioGroup loginType;
    private EditText mEditUsername;
    private View mFinished;
    private View mTwitchLogin;
    private ImageView mIndicator;
    private TextView mNextButton;
    private TextView mSkipButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_setup_pager, container, false);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sp.edit().putBoolean(Preferences.PREF_USER_COMPLETED_SETUP, true).apply();

        mPager = (NonSwipeViewPager) rootView.findViewById(R.id.pager);
        mPager.setPagingEnabled(false);

        mNextButton = (TextView) rootView.findViewById(R.id.nextButton);
        mSkipButton = (TextView) rootView.findViewById(R.id.skipButton);

        View welcome = inflater.inflate(R.layout.setup_welcome, null);
        View chooseLogin = inflater.inflate(R.layout.setup_choose_login, null);
        mTwitchLogin = inflater.inflate(R.layout.setup_twitch_login, null);
        View usernameLogin = inflater.inflate(R.layout.setup_username_login, null);
        mFinished = inflater.inflate(R.layout.setup_finished, null);

        loginType = (RadioGroup)chooseLogin.findViewById(R.id.radioLoginMethod);
        loginType.getCheckedRadioButtonId();

        mIndicator = (ImageView) rootView.findViewById(R.id.borderIndicator);
        mIndicator.getLayoutParams().width = getWindowWidth()/4;

        mEditUsername = (EditText) usernameLogin.findViewById(R.id.editText);
        mEditUsername.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    String user_url = getActivity().getResources().getString(R.string.twitch_user_url) + mEditUsername.getText();
                    new DownloadJSONTask(1).execute(user_url);
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEditUsername.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        ArrayList<View> views = new ArrayList<>();
        views.add(welcome);
        views.add(chooseLogin);
        views.add(mTwitchLogin);
        views.add(usernameLogin);
        views.add(mFinished);

        SimplePagerAdapter s = new SimplePagerAdapter(views);
        mPager.setAdapter(s);

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current == 3) {
                    String user_url = getActivity().getResources().getString(R.string.twitch_user_url) + mEditUsername.getText();
                    new DownloadJSONTask(1).execute(user_url);
                } else if (current <= 3) {
                    nextStep();
                } else {
                    exitSetup();
                }
            }
        });

        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipStep();
            }
        });

        return rootView;
    }

    private void nextStep() {
        switch (current) {
            case 0: makeSteps(1); break;
            case 1:
                if (loginType.getCheckedRadioButtonId() == R.id.radioTwitchLogin) {
                    makeSteps(1);
                    loadTwitchAuthenication();
                }
                else {
                    makeSteps(2);
                }
                break;
            case 2: makeSteps(2); break;
            default: makeSteps(1); break;
        }
    }

    private void makeSteps(int steps) {
        mNextButton.setVisibility(View.VISIBLE);
        mSkipButton.setVisibility(View.VISIBLE);

        if (current+steps == 4){
            mNextButton.setText("Finish");
            mSkipButton.setVisibility(View.INVISIBLE);
        }
        if (current+steps == 2){
            mNextButton.setVisibility(View.GONE);
        }

        ObjectAnimator progress = ObjectAnimator.ofFloat(mIndicator, "translationX", current *(getWindowWidth() /4), (current+1)*(getWindowWidth() /4));
        progress.start();
        current += steps;
        mPager.setCurrentItem(current);
    }

    private void skipStep() {
        switch (current) {
            case 0: exitSetup(); break;
            case 1: makeSteps(3); break;
            case 2: makeSteps(2); break;
            default: makeSteps(1);break;
        }
    }

    private void exitSetup() {
        ((MainActivity)getActivity()).exitSetup(this);
    }


    private void usernameConfirmed(String username, String userDisplayName) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sp.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, true).apply();
        sp.edit().putString(Preferences.TWITCH_USERNAME, username).apply();
        sp.edit().putString(Preferences.TWITCH_DISPLAY_USERNAME, userDisplayName).apply();
        sp.edit().putInt(Preferences.USERNAME_WARNING, 0).apply();
        sp.edit().putInt(Preferences.AUTH_WARNING, 0).apply();
        ((TextView)mFinished.findViewById(R.id.finishedText)).setText("Congratulations " + userDisplayName +"! \n Ace is now ready");
        nextStep();
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

            if (!mEditUsername.getText().toString().isEmpty()) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditUsername, InputMethodManager.SHOW_IMPLICIT);
                mEditUsername.setError("Username not found");
            }

        }
    }

    private void loadTwitchAuthenication() {
        final WebView w = (WebView) mTwitchLogin.findViewById(R.id.webView);
        final ProgressBar p = (ProgressBar) mTwitchLogin.findViewById(R.id.twitchProgress);
        final String get_oauth_token = getActivity().getResources().getString(R.string.twitch_oauth_get_token_url);
        final String oauth_base = getActivity().getResources().getString(R.string.twitch_oauth_base_url);

        CookieManager cm = CookieManager.getInstance();
        if(android.os.Build.VERSION.SDK_INT >= 21){
            cm.removeAllCookies(null);
        }else{
            cm.removeAllCookie();
        }
//        w.getSettings().setJavaScriptEnabled(true);

        w.setWebViewClient(new WebViewClient() {
            public int mNumberOfAttempts = 0;

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                p.setVisibility(View.GONE);
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

                    p.setVisibility(View.VISIBLE);
                    new DownloadJSONTask(0).execute(oauth_base + token);
                } else if (url.contains("error=access_denied")) {
                    nextStep();
                }
            }
        });
        w.loadUrl(get_oauth_token);
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

    private class SimplePagerAdapter extends PagerAdapter {
        private ArrayList<View> views = new ArrayList<>();

        public SimplePagerAdapter(ArrayList<View> v){
            views = v;
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public Object instantiateItem (ViewGroup container, int position)
        {
            View v = views.get(position);
            container.addView(v);
            return v;
        }

        //-----------------------------------------------------------------------------
        // Used by ViewPager.  Called when ViewPager no longer needs a page to display; it
        // is our job to remove the page from the container, which is normally the
        // ViewPager itself.  Since all our pages are persistent, we do nothing to the
        // contents of our "views" ArrayList.
        @Override
        public void destroyItem (ViewGroup container, int position, Object object)
        {
            container.removeView(views.get(position));
        }


        @Override
        public int getItemPosition (Object object)
        {
            int index = views.indexOf(object);
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
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
        ((MainActivity)getActivity()).disableDrawer();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
        super.onResume();
    }

    @Override
    public void onPause() {
        ((MainActivity)getActivity()).enableDrawer();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
        super.onPause();
    }

    private int getWindowWidth() {
        int width;
        if (getActivity() == null) return 500;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        return width;
    }
}