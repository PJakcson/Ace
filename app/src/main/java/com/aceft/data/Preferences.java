package com.aceft.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.aceft.R;
import com.aceft.ui_fragments.channel_fragments.ChatFragment;

public final class Preferences {
    public static final String PREF_USER_COMPLETED_SETUP = "user_completed_setup";
    public static final String CHAT_AUTOSTART = "settings_chat_autostart";
    public static final String IS_PRO_USER = "licence_has_seen_pro_message";
    public static final String HAS_SEEN_PRO_MESSAGE = "licence_has_seen_pro_message";
    public static final String CHAT_SIZE = "settings_chat_size";
    public static String USER_AUTH_TOKEN = "user_auth_token";
    public static String USER_IS_AUTHENTICATED = "user_is_authenticated";
    public static String SCOPES_OF_USER = "scopes_of_user";
    public static String USER_HAS_TWITCH_USERNAME = "user_has_twitch_username";
    public static String TWITCH_USERNAME = "twitch_username";
    public static String TWITCH_DISPLAY_USERNAME = "twitch_display_username";
    public static String TWITCH_STREAM_QUALITY_TYPE = "settings_stream_quality_type";
    public static String TWITCH_PREFERRED_VIDEO_QUALITY = "settings_preferred_video_quality";
    public static String TWITCH_MOBILE_LIMIT = "settings_mobile_quality_limit";
    public static String TWITCH_AUTOPLAY_MODE = "settings_autoplay_mode";
    public static String TWITCH_BITMAP_QUALITY = "settings_bitmap_quality";
    public static String APP_DEFAULT_HOME = "app_default_home";
    public static String CHAT_THEME = "settings_chat_theme";

    public static String USERNAME_WARNING = "times_username_warned";
    public static String LOGIN_WARNING = "times_username_warned";
    public static String AUTH_WARNING = "times_auth_warned";

    public static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    public static final String PREF_USER_LEARNED_DETAILS = "details_click_learned";

    public static final String INT_FULLSCREEN_CHAT = "intent_fullscreen_chat";
    public static final String INT_CHANNEL_PAGER = "intent_channel_pager";
    public static final String INT_CHANNEL = "intent_channel";

    public static final String BOOL_NOTIFICATIONS_ACTIVE = "bool_notif_active";
    public static final String STRING_STARRED_CHANNELS = "string_starred_channels";

    public static final String BOOL_SILENT_ACTIVE = "bool_silent_active";
    public static final String STRING_SILENT_FROM = "string_silent_from";
    public static final String STRING_SILENT_UNTIL = "string_silent_until";

    public static void showUsernameToast(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        int times = sp.getInt(USERNAME_WARNING, 0);
        sp.edit().putInt(USERNAME_WARNING, times + 1).apply();
        if (times < 3)
            Toast.makeText(c, c.getString(R.string.username_not_set), Toast.LENGTH_LONG).show();
    }

    public static void showLoginToast(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        int times = sp.getInt(LOGIN_WARNING, 0);
        sp.edit().putInt(LOGIN_WARNING, times + 1).apply();
        if (times < 3)
            Toast.makeText(c, c.getString(R.string.not_logged_in_warning), Toast.LENGTH_LONG).show();
    }

    public static void checkToken(Context c, ChatFragment chatFragment) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        int times = sp.getInt(AUTH_WARNING, 0);
        if (times < 3)
            TwitchNetworkTasks.checkToken(c, chatFragment);
    }
}
