package com.aceft.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.PlayerActivity;
import com.aceft.R;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.ui_fragments.channel_fragments.ChatFragment;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.net.ssl.HttpsURLConnection;

public final class TwitchNetworkTasks {
    public static int NEW_VOD = 0;
    public static int OLD_VOD = 1;

    private TwitchNetworkTasks() {
    }

    public static Bitmap downloadBitmap(String myurl) {
        InputStream is;
        Bitmap bitmap = null;
        URL url;
        try {
            url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            conn.connect();
            is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static JSONObject downloadJSONData(String myurl) {
        InputStream is = null;
        String result;
        JSONObject jObject = null;
        try {
            URL url = new URL(myurl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
            jObject = new JSONObject(result);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return jObject;
    }

    public static String downloadStringData(String myurl) {
        InputStream is = null;
        String result;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
            return result;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static LinkedHashMap<String, String> fetchTwitchPlaylist(String myurl) {
        InputStream is = null;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            try {
                conn.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);

            String q, u;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("http")) {
                    u = line;
                    q = getQuality(line);
                    hmap.put(q, u);
                }
            }
            return hmap;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return hmap;
    }

    public static void followChannel(Activity a, String channel) {
        if (channel == null || channel.isEmpty()) return;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(a);

        String base = a.getResources().getString(R.string.twitch_user_url);
        String username = pref.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null) return;
        String token = pref.getString(Preferences.USER_AUTH_TOKEN, null);
        if (token == null) return;

        String req = base + username + "/follows/channels/" + channel + "?oauth_token=" + token + "&notifications=false";
        String result = followChannel(req);
    }

    public static String followChannel(String myurl) {
        InputStream is;
        URL url;
        try {
            url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String unFollowChannel(String myurl) {
        InputStream is;

        URL url;
        try {
            url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int whichVodType(TwitchVideo v) {
        String prefix = v.mId.substring(0, 1);
        if (prefix.equals("v")) return NEW_VOD;
        else return OLD_VOD;
    }

    private static String getQuality(String s) {
        String q = "none";
        if (s.contains("chunked")) return "source";
        if (s.contains("high")) return "high";
        if (s.contains("medium")) return "medium";
        if (s.contains("low")) return "low";
        if (s.contains("mobile")) return "mobile";
        if (s.contains("audio_only")) return "audio_only";
        return q;
    }

    public static void checkToken(final Context c, final ChatFragment chatFragment) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String token = sp.getString(Preferences.USER_AUTH_TOKEN, "");
        if (token.isEmpty()) {
            Toast.makeText(c, c.getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show();
            return;
        }
        final String req = "https://api.twitch.tv/kraken?oauth_token=" + token;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String answer = TwitchNetworkTasks.downloadStringData(req);
                try {
                    JSONObject j = new JSONObject(answer);
                    if (!j.getJSONObject("token").getBoolean("valid")) {
                        showRefreshDialog((Activity) c, chatFragment);
                    }
                } catch (JSONException | NullPointerException e) {
                    showRefreshDialog((Activity) c, chatFragment);
                }
            }
        }).start();
    }

    private static void showRefreshDialog(final Activity a, final ChatFragment chatFragment) {
        if (a == null) return;
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(a);
        final int times = sp.getInt(Preferences.AUTH_WARNING, 0);

        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chatFragment != null) chatFragment.cantLoadChat();
                if (times >= 3) return;
                sp.edit().putInt(Preferences.AUTH_WARNING, times + 1).apply();
                final AlertDialog.Builder builder = new AlertDialog.Builder(a);
                builder.setTitle("Token No Longer Valid")
                        .setMessage("Do you want to quickly refresh your Twitch Token?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    ((PlayerActivity) a).refreshToken();
                                } catch (ClassCastException ignored) {
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create();
                builder.show();
            }
        });
    }

    public static String sendDataToAppServer(int id, String username, String token, ArrayList<String> channels) {
        URL url;
        try {
            url = new URL("http://85.214.48.232:4444/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();

            JSONObject json = new JSONObject();
            JSONArray jsArray = new JSONArray(channels);
            json.put("task", "add_user");
            json.put("id", id);
            json.put("username", username);
            json.put("token", token);
            json.put("channels", jsArray);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(json.toString());
            wr.flush();
            wr.close();

            int HttpResult = conn.getResponseCode();
            InputStream is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void removeTokenOnAppServer(String token, String newToken) {
        URL url;
        try {
            url = new URL("http://85.214.48.232:4444/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();

            JSONObject json = new JSONObject();
            json.put("task", "remove_token");
            json.put("old_token", token);
            json.put("new_token", newToken);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(json.toString());
            wr.flush();
            wr.close();

            int HttpResult = conn.getResponseCode();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void syncStarredWithAppServer(String name, ArrayList<String> ch, boolean st) {
        URL url;
        try {
            url = new URL("http://85.214.48.232:4444/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();

            JSONObject json = new JSONObject();
            JSONArray jsArray = new JSONArray(ch);
            if (st)
                json.put("task", "set_starred");
            else
                json.put("task", "remove_starred");

            json.put("username", name);
            json.put("channels", jsArray);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(json.toString());
            wr.flush();
            wr.close();

            int HttpResult = conn.getResponseCode();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void unFollowChannels(Activity a, ArrayList<String> channels) {
        if (a == null || channels == null || channels.isEmpty()) return;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(a);

        String base = a.getResources().getString(R.string.twitch_user_url);
        String username = pref.getString(Preferences.TWITCH_USERNAME, null);
        if (username == null) return;
        String token = pref.getString(Preferences.USER_AUTH_TOKEN, null);
        if (token == null) return;

        String req = "";
        for (String ch : channels) {
            req = base + username + "/follows/channels/" + ch + "?oauth_token=" + token;
            String result = unFollowChannel(req);
        }
    }
}
