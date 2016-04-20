package com.aceft.data;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;

import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Emoticon;
import com.aceft.data.primitives.Game;
import com.aceft.data.primitives.Stream;
import com.aceft.data.primitives.TwitchUser;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.data.primitives.TwitchVodFile;
import com.aceft.data.primitives.TwitchVodFileOld;

public final class TwitchJSONParser {
    private static String BITMAP_QUALITY = "large";

    private TwitchJSONParser() {
    }

    public static void setHighQuality() {
        BITMAP_QUALITY = "large";
    }

    public static void setMediumQuality() {
        BITMAP_QUALITY = "medium";
    }

    public static void setSmallQuality() {
        BITMAP_QUALITY = "small";
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Game> topGamesJSONtoArrayList(String r, HashMap<String, String> mapping) {
        String title, thumb = "", id;
        int viewers, channelc;
        JSONObject game;

        ArrayList<Game> games = new ArrayList<>();

        try {
            JSONObject jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("top");

            for (int i=0; i<jArray.length(); i++) {
                viewers = jArray.getJSONObject(i).getInt("viewers");
                channelc = jArray.getJSONObject(i).getInt("channels");

                game = jArray.getJSONObject(i).getJSONObject("game");
                title = game.getString("name");
                id = game.getString("_id");

                thumb = mapping.get(id);

                Game temp = new Game(title,thumb,viewers,channelc,id,null);
                games.add(temp);
            }
            return games;
        } catch (JSONException e) {
            e.printStackTrace();
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.v("topGamesJSONtoArrayList", "Nothing to parse. String is empty");
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Game> gameSearchJSONtoArrayList(String r) {
        String title, thumb, id;
        int popularity;
        JSONObject game;

        ArrayList<Game> games = new ArrayList<>();

        try {
            JSONObject jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("games");

            for (int i=0; i<jArray.length(); i++) {
                game = jArray.getJSONObject(i);
                title = game.getString("name");
                id = game.getString("_id");
                popularity = game.getInt("popularity");
                thumb = game.getJSONObject("box").getString(BITMAP_QUALITY);
                Game temp = new Game(title,thumb,popularity,0,id,null);
                games.add(temp);
            }
            return games;
        } catch (JSONException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        } catch (NullPointerException e) {
            Log.v("topGamesJSONtoArrayList", "Nothing to parse. String is empty");
            return null;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Channel> channelsJSONtoArrayList(String r) {
        ArrayList<Channel> channels = new ArrayList<>();
        if (r == null) return null;
        JSONObject jObject;
        try {
            jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("channels");
            JSONObject j;
            Channel channel;
            for (int i=0; i<jArray.length(); i++) {
                try {
                    j = jArray.getJSONObject(i);
                    channel = channelJSONtoChannel(j);
                    if (channel == null) continue;
                    channels.add(channel);
                } catch (JSONException | NullPointerException e) {
                    Log.v("channelsJSONtoArrayList", "no JSON Data");
                }
            }
            return channels;
        } catch (JSONException | NullPointerException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        }
    }

    public static ArrayList<Channel> followedChannelsToArrayList(String r) {
        ArrayList<Channel> channels = new ArrayList<>();
        if (r == null) return null;
        JSONObject jObject;
        try {
            jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("follows");
            JSONObject j;
            Channel channel;
            for (int i=0; i<jArray.length(); i++) {
                try {
                    j = jArray.getJSONObject(i);
                    channel = followedJSONtoChannel(j);
                    if (channel == null) {
                        continue;
                    }

                    channels.add(channel);
                } catch (JSONException | NullPointerException e) {
                    Log.v("channelsJSONtoArrayList", "no JSON Data");
                }
            }
            return channels;
        } catch (JSONException | NullPointerException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Channel channelJSONtoChannel(JSONObject j) {
        HashMap<String, String> hTemp = new HashMap<>();
        try {
            hTemp.put("_id", j.getString("_id"));
            hTemp.put("name", j.getString("name"));

            String mature = jToBool(j, "mature") ? "Yes" : "No";
            hTemp.put("mature", mature);

            hTemp.put("status", jToString(j, "status"));
            hTemp.put("display_name", jToString(j, "display_name"));
            hTemp.put("game", jToString(j, "game"));
            hTemp.put("created_at", jToString(j, "created_at"));
            hTemp.put("updated_at", jToString(j, "updated_at"));
            hTemp.put("logo", jToString(j, "logo"));
            hTemp.put("video_banner", jToString(j, "video_banner"));
            hTemp.put("views", jToString(j, "views"));
            hTemp.put("followers", jToString(j, "followers"));

            return new Channel(hTemp);
        } catch (JSONException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        } catch (NullPointerException e) {
            Log.v("channelsJSONtoArrayList", "Nothing to parse. String is empty");
            return null;
        }
    }

    public static Channel followedJSONtoChannel(JSONObject j) {
        HashMap<String, String> hTemp = new HashMap<>();

        try {
            String cName = j.getJSONObject("_links").getString("self");
            int index = cName.lastIndexOf("/")+1;
            hTemp.put("name", cName.substring(index));

            JSONObject jChannel = j.getJSONObject("channel");
            hTemp.put("_id", jChannel.getString("_id"));

            String mature = jToBool(jChannel, "mature") ? "Yes" : "No";
            hTemp.put("mature", mature);

            hTemp.put("status", jToString(jChannel, "status"));
            hTemp.put("display_name", jToString(jChannel, "display_name"));
            hTemp.put("game", jToString(jChannel, "game"));
            hTemp.put("created_at", jToString(jChannel, "created_at"));
            hTemp.put("updated_at", jToString(jChannel, "updated_at"));
            hTemp.put("logo", jToString(jChannel, "logo"));
            hTemp.put("video_banner", jToString(jChannel, "video_banner"));
            hTemp.put("views", jToString(jChannel, "views"));
            hTemp.put("followers", jToString(jChannel, "followers"));

            return new Channel(hTemp);

        } catch (JSONException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        } catch (NullPointerException e) {
            Log.v("channelsJSONtoArrayList", "Nothing to parse. String is empty");
            return null;
        }
    }


    public static Channel channelStringToChannel(String r) {
        try {
            JSONObject jObject = new JSONObject(r);
            return channelJSONtoChannel(jObject);
        } catch (JSONException e) {
            Log.v("channelsJSONtoArrayList", "no JSON Data");
            return null;
        } catch (NullPointerException e) {
            Log.v("channelsJSONtoArrayList", "Nothing to parse. String is empty");
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Stream> streamJSONtoArrayList(String r) {
        ArrayList<Stream> streams = new ArrayList<>();

        try {
            JSONObject jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("streams");
            Stream stream;
            for (int i=0; i<jArray.length(); i++) {
                try {
                    stream = streamJSONtoStream(jArray.getJSONObject(i));
                    if (stream == null) continue;
                    streams.add(stream);
                } catch (JSONException ignored) {
                }
            }
        } catch (JSONException | NullPointerException e) {
            Log.v("topGamesJSONtoArrayList", "Nothing to parse. String is empty");
            e.printStackTrace();
        }
        return streams;
    }

    public static Stream streamJSONtoStream(JSONObject j) {
        String preview, curl, game;
        int viewers, id;

        try {
            id = j.getInt("_id");
            game = jToString(j, "game");
            viewers = jToInt(j, "viewers");
            curl = jToString(j.getJSONObject("_links"), "self");
            preview = jToString(j.getJSONObject("preview"), BITMAP_QUALITY);

            JSONObject jChannel = j.getJSONObject("channel");
            Channel channel = channelJSONtoChannel(jChannel);
            return new Stream(curl, game, viewers, preview, id, channel);
        } catch (JSONException | NullPointerException e) {
            Log.v("streamJSONtoStream", "no Stream");
            return null;
        }
    }

    public static Stream streamStringToStream(String s) {
        String preview, curl, game;
        int viewers, id;

        try {
            JSONObject j = new JSONObject(s).getJSONObject("stream");
            id = j.getInt("_id");
            game = jToString(j, "game");
            viewers = jToInt(j, "viewers");
            curl = jToString(j.getJSONObject("_links"), "self");
            preview = jToString(j.getJSONObject("preview"), BITMAP_QUALITY);

            JSONObject jChannel = j.getJSONObject("channel");
            Channel channel = channelJSONtoChannel(jChannel);
            return new Stream(curl, game, viewers, preview, id, channel);
        } catch (JSONException | NullPointerException e) {
            Log.v("streamJSONtoStream", "no Stream");
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String recordedAtToDate(String s) {
        int year = Integer.valueOf(s.substring(0, 4));
        int month = Integer.valueOf(s.substring(5, 7));
        int day = Integer.valueOf(s.substring(8, 10));

        int hours = Integer.valueOf(s.substring(11, 13));
        int minutes = Integer.valueOf(s.substring(14, 16));
        int seconds = Integer.valueOf(s.substring(17, 19));


        Calendar now = Calendar.getInstance();
        Calendar recorded = new GregorianCalendar(year, month-1, day, hours, minutes, seconds);

        long minDiff = (now.getTimeInMillis() - recorded.getTimeInMillis())/60000;
        long hourDiff = minDiff/60;
        long dayDiff = hourDiff/24;
        long monthDiff = dayDiff/31;

        if (minDiff < 2) return "" + minDiff + " minute ago";
        if (minDiff < 60) return "" + minDiff + " minutes ago";
        if (hourDiff < 2) return "" + hourDiff + " hour ago";
        if (hourDiff < 24) return "" + hourDiff + " hours ago";
        if (dayDiff < 2) return "" + dayDiff + " day ago";
        if (dayDiff < 31) return "" + dayDiff + " days ago";
        if (monthDiff < 2) return "" + monthDiff + " month ago";
        if (monthDiff < 4) return "" + monthDiff + " months ago";
        return "from " + day + "." + month + "." + year;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String createdToDate(String s) {
        String year = s.substring(0, 4);
        int month = Integer.valueOf(s.substring(5, 7));
        int day = Integer.valueOf(s.substring(8, 10));

        String sMonth;
        sMonth = month < 10 ? "0" + month : "" + month;
        String sDay;
        sDay = day < 10 ? "0" + day : "" + day;

        return "" + sDay + "." + sMonth + "." + year;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("SimpleDateFormat")
    public static String secondsInHMS(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            int seconds = (int) Double.parseDouble(s);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.add(Calendar.SECOND, seconds);

            SimpleDateFormat format;
            if (seconds >= 3600) {
                format = new SimpleDateFormat("HH:mm:ss");
            } else {
                format = new SimpleDateFormat("mm:ss");
            }
            return format.format(c.getTime());
        } catch (Exception ignored) {
            return "";
        }

//        int sec = (int) Double.parseDouble(s);
//        String hms = "";
//        String sSec,sMin;
//        int hour = (int) ((1.0 * sec / 3600));
//        if (hour > 0) hms += hour + ":";
//        int min = (int) ((1.0 * sec / 60) % 60);
//        if (min < 10)
//            sMin = "0"+min;
//        else
//            sMin = "" + min;
//        hms += sMin + ":";
//        sec = sec % 60;
//        if (sec < 10)
//            sSec = "0"+sec;
//        else
//            sSec = "" + sec;
//
//        return hms+sSec;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<TwitchVideo> dataToVideoList(String s) {
        ArrayList<TwitchVideo> videos = new ArrayList<>();
        String title, description, recorded_at, preview, status, game, id, length, views;

        JSONObject jObject;
        try {
            jObject = new JSONObject(s);
            JSONArray jArray = jObject.getJSONArray("videos");
            JSONObject video;

            for (int i=0; i<jArray.length(); i++) {
                video = jArray.getJSONObject(i);

                title = video.getString("title");
                description = video.getString("description");
                status = video.getString("status");
                id = video.getString("_id");
                recorded_at = video.getString("recorded_at");
                game = video.getString("game");
                length = video.getString("length");
                preview = video.getString("preview");
                views = video.getString("views");

                if (preview.equals("null")) continue;

                TwitchVideo temp = new TwitchVideo(title, description, status, id, recorded_at, game,
                        length, preview, views);
                videos.add(temp);
            }
            return videos;
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static TwitchUser userDataToUser(String s) {
        String display_name = "", name = "", bio = "", updated_at= "", type = "", created_at = "", logo = "";
        TwitchUser user;

        JSONObject jUser;
        try {
            jUser = new JSONObject(s);
            display_name = jUser.getString("display_name");
            name = jUser.getString("name");
            bio = jToString(jUser, "bio");
            updated_at = jToString(jUser, "updated_at");
            type = jToString(jUser, "type");
            created_at = jToString(jUser, "created_at");
            logo = jToString(jUser, "logo");
            user = new TwitchUser(display_name, name, bio, updated_at, type, created_at, logo);
            return user;
        } catch (JSONException e) {
            e.printStackTrace();
           return new TwitchUser(display_name, name, bio, updated_at, type, created_at, logo);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.v("userDataToUser", "Nothing to parse. String is empty");
            return new TwitchUser(display_name, name, bio, updated_at, type, created_at, logo);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static TwitchVod oldVideoDataToPlaylist(JSONObject json) {
        TwitchVod result = new TwitchVod();
        ArrayList<TwitchVodFileOld> fo = new ArrayList<>();

        try {
            result.setDuration(json.getInt("duration"));
            result.setChannel(json.getString("channel"));
            result.setPreviewLink(json.getString("preview"));

            result.setStartOffset(json.getInt("start_offset"));
            result.setEndOffset(json.getInt("end_offset"));
            result.setPlayOffset(json.getInt("play_offset"));

            JSONObject jsonObject = json.getJSONObject("chunks");
            JSONArray jPlaylist;

            for (int i = 0; i < jsonObject.names().length(); i++) {
                TwitchVodFileOld to = new TwitchVodFileOld();
                to.setQuality(jsonObject.names().getString(i));

                ArrayList<TwitchVodFile> vf = new ArrayList<>();
                jPlaylist = jsonObject.getJSONArray(jsonObject.names().getString(i));
                for (int j = 0; j < jPlaylist.length(); j++) {
                    String url = jPlaylist.getJSONObject(j).getString("url");
                    String length = jPlaylist.getJSONObject(j).getString("length");
                    vf.add(new TwitchVodFile(url, length));
                }
                if (vf.size() > 0) {
                    to.setVideo(vf);
                    fo.add(to);
                }
            }
            result.setVideo(fo);
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Emoticon> emotesJSONtoArrayList(String r) {
        ArrayList<Emoticon> emotes = new ArrayList<>();
        JSONObject jEmote;
        String code, id;

        try {
            JSONObject jObject = new JSONObject(r);
            JSONArray jArray = jObject.getJSONArray("emoticons");

            for (int i=0; i<jArray.length(); i++) {
                try {
                    jEmote = jArray.getJSONObject(i);
                    if (jEmote == null) continue;
                    id = jEmote.getString("id");
                    code = jEmote.getString("code");
                    emotes.add(new Emoticon(id, 0, null, code));
                } catch (JSONException ignored) {
                }
            }
        } catch (JSONException | NullPointerException e) {
            Log.v("topGamesJSONtoArrayList", "Nothing to parse. String is empty");
            e.printStackTrace();
        }
        return emotes;
    }

    @Nullable
    public static String getGamesDBThumbnail(Document d) {
        try {
            String rel = d.getElementsByTag("Game").first().getElementsByTag("boxart").first().attr("thumb");
            String base = d.getElementsByTag("baseImgUrl").first().text();
            return base + rel;
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    public static String getGamesDBId(Document d) {
        try {
            return d.getElementsByTag("Game").first().getElementsByTag("id").first().text();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static String[] tokenSigToStringArray(JSONObject j) {
        String tokSig[] = new String[2];
        try {
            tokSig[0] = j.getString("token");
            tokSig[1] = j.getString("sig");
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return tokSig;
    }

    private static String jToString(JSONObject j, String s) {
        try{
            if (j.getString(s).equals("null")) return "";
            return j.getString(s);
        } catch (JSONException e) {
            return "";
        }
    }

    private static int jToInt(JSONObject j, String s) {
        try{
            return j.getInt(s);
        } catch (JSONException e) {
            return 0;
        }
    }

    private static boolean jToBool(JSONObject j, String s) {
        try{
            return !j.getString(s).equals("null") && j.getBoolean(s);
        } catch (JSONException e) {
            return false;
        }
    }
}
