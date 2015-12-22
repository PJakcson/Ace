package com.aceft.ui_fragments.channel_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.IRCAdapter;
import com.aceft.data.AceAnims;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchChatClient;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.IRCMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatFragment extends Fragment implements IRCAdapter.OnMessageClicked {
    private View mRootView;
    private TwitchChatClient mClient;
    private TwitchChatClient mClient2;
    private EditText mChatBox;
    private IRCAdapter mIRCAdapter;
    private String mChatRoom;
    private String mChannelName;
    private boolean mAutostart;
    ArrayList<String> mServers;
    private String channelDisplayName;
    private TextView mStatusText;
    public boolean hasStartedChat;
    private boolean mChatStarted;
    private int scrolling = 0;
    private LinearLayoutManager mLayoutManager;

    public ChatFragment newInstance(String c, String d) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        args.putString("channel_display_name", d);
        args.putBoolean("autostart", true);
        args.putBoolean("mini", false);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment newInstance(String c, String d, boolean autostart) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        args.putString("channel_display_name", d);
        args.putBoolean("autostart", autostart);
        args.putBoolean("mini", true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_chat2, container, false);
        mChatBox = (EditText) mRootView.findViewById(R.id.chatBox);
        RecyclerView recyclerView = (RecyclerView) mRootView.findViewById(R.id.messageList);
        recyclerView.setHasFixedSize(true);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mChannelName = getArguments().getString("channel_name");
        channelDisplayName = getArguments().getString("channel_display_name");
        mAutostart = getArguments().getBoolean("autostart");
        hasStartedChat = mAutostart;
        mChatRoom = "#"+ mChannelName;

        mStatusText = (TextView) mRootView.findViewById(R.id.statusText);

        mStatusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadChat();
            }
        });

        if (mIRCAdapter == null) {
            mIRCAdapter = new IRCAdapter(getActivity(), recyclerView);
        } else {
            mIRCAdapter.updateView(recyclerView);
        }

        mIRCAdapter.setOnNameClickedListener(this);

        setChatTheme();

        mLayoutManager = new LinearLayoutManager(getActivity(), OrientationHelper.VERTICAL, false);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mIRCAdapter);
        recyclerView.setItemAnimator(null);
//        recyclerView.getItemAnimator().setAddDuration(0);
//        recyclerView.getItemAnimator().setMoveDuration(0);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int count = recyclerView.getAdapter().getItemCount();
                int lastIndex = mLayoutManager.findLastCompletelyVisibleItemPosition();

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    mIRCAdapter.setAutoScroll(false);

                if (count - lastIndex < 6) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        mIRCAdapter.setAutoScroll(true);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        });

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
            //TODO automatically hide keyboard setting
//                InputMethodManager imm = (InputMethodManager) view.getContext()
//                        .getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                if (mChatBox != null)
                    mChatBox.setCursorVisible(false);
                return false;
            }
        });

        final View activityRootView = getActivity().findViewById(R.id.container);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff < 100) {
                    if (mChatBox != null)
                        mChatBox.setCursorVisible(false);
                }
            }
        });

        return mRootView;
    }

    private void setChatTheme() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIRCAdapter.setTextSize(sp.getInt(Preferences.CHAT_SIZE, 18));
        if (sp.getString(Preferences.CHAT_THEME, getString(R.string.settings_chat_theme_default)).contains("Light")) {
            mIRCAdapter.updateTheme(0);
        }
        if (sp.getString(Preferences.CHAT_THEME, getString(R.string.settings_chat_theme_default)).contains("Dark")) {
            mChatBox.getBackground().setColorFilter(getResources().getColor(R.color.chat_white), PorterDuff.Mode.SRC_ATOP);
            mChatBox.setTextColor(getResources().getColor(R.color.chat_white));
            mRootView.setBackground(getResources().getDrawable(R.color.chat_black));
            mStatusText.setTextColor(getResources().getColor(R.color.chat_white));
            mIRCAdapter.updateTheme(1);
        }
    }

    public void loadChat() {
        mChatStarted = true;
        if (mRootView == null) return;
        if (mRootView.findViewById(R.id.statusText) != null)
            mRootView.findViewById(R.id.statusText).setVisibility(View.GONE);
        if (mRootView.findViewById(R.id.chatProgress) != null)
            mRootView.findViewById(R.id.chatProgress).setVisibility(View.VISIBLE);
        startFreshIRCClient();
    }

    private void startFreshIRCClient() {
        getSubIcon();
        if (mServers == null) {
            getChatProperties();
            return;
        }
        if (mRootView == null) return;
        if (mRootView.findViewById(R.id.chatProgress) != null)
            mRootView.findViewById(R.id.chatProgress).setVisibility(View.VISIBLE);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        String token = "oauth:" + sp.getString(Preferences.USER_AUTH_TOKEN, "");
        String nick = sp.getString(Preferences.TWITCH_USERNAME, "");
        mClient2 = new TwitchChatClient(this, nick, token, mServers, true);
        mClient = new TwitchChatClient(this, nick, token, mServers);
        mClient2.connect(mChatRoom, 0);
        mClient.connect(mChatRoom, 0);
    }

    public void newMessage(IRCMessage ircMessage) {
        mIRCAdapter.update(ircMessage);
    }

    public void onJoined() {
        if (mRootView == null) return;
        if (mRootView.findViewById(R.id.chatProgress) != null)
            mRootView.findViewById(R.id.chatProgress).setVisibility(View.GONE);
        if (mRootView.findViewById(R.id.statusText) != null)
            mRootView.findViewById(R.id.statusText).setVisibility(View.GONE);
        mChatBox.setVisibility(View.VISIBLE);

        mChatBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mChatBox.setCursorVisible(true);
                return false;
            }
        });

        mChatBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String message = String.valueOf(mChatBox.getText());
                    mClient2.sendMessage(message);
                    mChatBox.setText("");
                    return true;
                }
                return false;
            }
        });
    }

    private void getSubIcon() {
        String req = getActivity().getString(R.string.twich_chat_emoticons) + mChannelName + "/badges";
        new DownloadBadgeTask().execute(req);
    }

    public void subBitmapReceived(Bitmap b) {
        mIRCAdapter.updateSubBadge(b);
    }

    public void emotisDataReceived(String s) {
        try {
            JSONObject badges = new JSONObject(s);
            String subUrl = badges.getJSONObject("subscriber").getString("image");
            new DownloadEmotisTask().execute(subUrl);
        } catch (JSONException ignored) {
        }
    }

    public boolean getChatStatus() {
        return mChatStarted;
    }


    private class DownloadBadgeTask extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... urls) {
            JSONObject badges = TwitchNetworkTasks.downloadJSONData(urls[0]);
            try {
                String subUrl = badges.getJSONObject("subscriber").getString("image");
                return TwitchNetworkTasks.downloadBitmap(subUrl);
            } catch (JSONException | NullPointerException ignored) {
            }
            return null;
        }

        protected void onPostExecute(Bitmap result) {
            subBitmapReceived(result);
        }
    }

    private class DownloadEmotisTask extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... urls) {
            return TwitchNetworkTasks.downloadBitmap(urls[0]);
        }
        protected void onPostExecute(Bitmap result) {
            subBitmapReceived(result);
        }
    }

    private void getChatProperties() {
        final String req = getActivity().getString(R.string.twich_chat_properties) + mChannelName + "/chat_properties";
        mServers = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                JSONObject j = TwitchNetworkTasks.downloadJSONData(req);
                try {
                    JSONArray jsonArray = j.getJSONArray("chat_servers");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        mServers.add(jsonArray.getString(i));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (mServers.isEmpty()) {
                            Toast.makeText(getActivity(), "Could not find any servers online", Toast.LENGTH_LONG).show();
                            return;
                        }
                        startFreshIRCClient();
                    }
                });
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void chatIsOffline() {
        Toast.makeText(getActivity(), "Chat seems to be offline", Toast.LENGTH_LONG).show();
        mRootView.findViewById(R.id.chatProgress).setVisibility(View.GONE);
    }

    public void cantLoadChat() {
        if (mStatusText == null || getActivity() == null) return;
        if (mRootView != null)
            mRootView.findViewById(R.id.chatProgress).setVisibility(View.GONE);
        if (mStatusText != null) {
            mStatusText.setText(getActivity().getString(R.string.authorize_twitch_account));
            mStatusText.setOnClickListener(null);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getArguments().getBoolean("mini")) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle("#" + channelDisplayName);
            AceAnims.showActionbar(getActivity(), false);
//            ((MainActivity)getActivity()).setAdPosition(RelativeLayout.ALIGN_PARENT_TOP);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!preferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
            Preferences.showLoginToast(getActivity());
            cantLoadChat();
            return;
        }
        if (preferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
            TwitchNetworkTasks.checkToken(getActivity(), this);
        }

//        ((MainActivity)getActivity()).resetAdPosition();
//        ((MainActivity)getActivity()).setAdPosition(RelativeLayout.ALIGN_PARENT_TOP);
        if (!getArguments().getBoolean("autostart") && !mChatStarted) {
            mRootView.findViewById(R.id.chatProgress).setVisibility(View.GONE);
            mStatusText.setText("Click To Load Chat");
            mStatusText.setTextColor(getResources().getColor(R.color.accent_material_light));
            return;
        }

        if (mClient == null) {
            startFreshIRCClient();
        } else {
            onJoined();
            mClient.joinChannel(this, mChatRoom);
            mClient2.joinChannel(this, mChatRoom);
        }
    }

    @Override
    public void onPause() {
        if (!getArguments().getBoolean("mini"))
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mClient == null) return;
        mClient.partChannel();
        if (mClient2 == null) return;
        mClient2.partChannel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() == null)
            setRetainInstance(true);
    }

    @Override
    public void nameClicked(String s) {
        if (mChatBox != null) {
            mChatBox.setText(mChatBox.getText() + "@" + s + " ");
            mChatBox.setSelection(mChatBox.getText().length());
        }
    }
}