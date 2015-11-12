package com.aceft.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.aceft.R;

import java.util.LinkedHashMap;

public class LiveMethods {

    public static String getQualityKey(Context c, LinkedHashMap<String, String> q) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String qSetting = sp.getString(Preferences.TWITCH_STREAM_QUALITY_TYPE, c.getString(R.string.default_stream_quality_type));
        String qPref = sp.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, c.getString(R.string.default_preferred_video_quality));
        String mQPref = sp.getString(Preferences.TWITCH_MOBILE_LIMIT, c.getString(R.string.mobile_limit_default));

        if (mQPref != null)
            if (mQPref.equals("no limit")) mQPref = "source";

        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        String pref = qPref;
        if (cm != null)
            pref = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE ? mQPref : qPref;

        final String qKeys[] = q.keySet().toArray(new String[q.size()]);

        if (bestQIndex(q) >= 0) {
            switch (qSetting) {
                case "always ask": return "showDialog" + prefOrBestQIndex(q, pref);
                case "auto select best":
                    int best = bestQIndex(q, pref);
                    if (best < 0) {
                        Toast.makeText(c, "Could not find stream below limit", Toast.LENGTH_LONG).show();
                        return "showDialog" + closeToPrefQIndex(q, pref);
                    } else {
                        return qKeys[best];
                    }
                case "set maximum":
                    if (prefOrWorseQIndex(q, pref) < 0) {
                        Toast.makeText(c, "Could not find stream below limit", Toast.LENGTH_LONG).show();
                        return "showDialog" + closeToPrefQIndex(q, pref);
                    }
                    else
                        return qKeys[prefOrWorseQIndex(q, pref)];
            }
        }
        return "";
    }

    private static int bestQIndex(LinkedHashMap<String, String> q) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int bestQ = -1;
        int bestI = -1;

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) > bestQ) {
                bestQ = qualityValue(qa[i]);
                bestI = i;
            }
        }
        return bestI;
    }

    private static int bestQIndex(LinkedHashMap<String, String> q, String limit) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int bestQ = -1;
        int bestI = -1;

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) > bestQ && qualityValue(qa[i]) <= qualityValue(limit)) {
                bestQ = qualityValue(qa[i]);
                bestI = i;
            }
        }
        return bestI;
    }

    public static int prefOrBestQIndex(LinkedHashMap<String, String> q, String pref) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int iPref = qualityValue(pref);

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) == iPref) {
                return i;
            }
        }
        return bestQIndex(q);
    }

    public static int prefOrWorseQIndex(LinkedHashMap<String, String> q, String pref) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int iPref = qualityValue(pref);
        int bestQ = -1;
        int bestI = -1;

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) == iPref) return i;
            if (qualityValue(qa[i]) <= iPref && qualityValue(qa[i]) > bestQ) {
                bestQ = qualityValue(qa[i]);
                bestI = i;
            }
        }
        if (bestI < 0) return -1;
        return bestI;
    }

    public static int closeToPrefQIndex(LinkedHashMap<String, String> q, String pref) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int iPref = qualityValue(pref);
        int bestI = 0;
        int distance = 100;

        for (int i = 0; i < qa.length; i++) {
            if (Math.abs(qualityValue(qa[i]) - iPref) < distance) {
                distance = qualityValue(qa[i]) - iPref;
                bestI = i;
            }
        }
        return bestI;
    }

    private static int qualityValue(String s) {
        if (s.contains("audio_only")) return 0;
        if (s.contains("240")) return 1;
        if (s.contains("mobile")) return 1;
        if (s.contains("360")) return 2;
        if (s.contains("low")) return 2;
        if (s.contains("480")) return 3;
        if (s.contains("medium")) return 3;
        if (s.contains("720")) return 4;
        if (s.contains("high")) return 4;
        if (s.contains("live")) return 5;
        if (s.contains("source")) return 5;
        if (s.contains("chunked")) return 5;
        return -1;
    }

    public static int keyIndex(LinkedHashMap<String, String> q, String key) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        for (int i = 0; i < qa.length; i++) {
            if (qa[i].equals(key)) {
                return i;
            }
        }
        return 0;
    }
    
    // ------------------------VideoPlayer-----------------/////////////////////////////////
//    public void playVideo(LinkedHashMap<String, String> q) {
//
//        if (bestQIndex(q) >= 0) {
//            switch (mPreferences.getString("settings_stream_quality_type", "")) {
//                case "always ask": showPlayDialog(q, prefOrBestQIndex(q)); break;
//                case "auto select best":
//                    if (!bMobileData) playStream(q.get(bestQString(q)));
//                    else playPreferredQualityOrWorse(q);
//                    break;
//                case "set maximum":
//                    playPreferredQualityOrWorse(q);
//                    break;
//            }
//        } else {
//            Toast.makeText(mContext, "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void playPreferredQualityOrWorse(LinkedHashMap<String, String> q) {
//        String pref = "";
//        if (bMobileData) {
//            pref = mPreferences.getString(Preferences.TWITCH_MOBILE_LIMIT,"");
//        } else {
//            pref = mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY,"");
//        }
//
//        if(prefOrWorseQIndex(q, pref) == null) {
//            showPlayDialog(q, prefOrBestQIndex(q));
//            Toast.makeText(mContext, "Sorry. No video below the maximum quality.", Toast.LENGTH_SHORT).show();
//        } else {
//            playStream(q.get(prefOrWorseQIndex(q, pref)));
//        }
//    }
//
//    // ------------------------VideoPlayer-----------------/////////////////////////////////
//    public void playVideoInApp(LinkedHashMap<String, String> q) {
//        if (bestQIndex(q) >= 0) {
//            switch (mPreferences.getString("settings_stream_quality_type", "")) {
//                case "always ask": showInAppPlayDialog(q, prefOrBestQIndex(q)); break;
//                case "auto select best":
//                    if (!bMobileData) mVideoPlayer.playStream(q.get(bestQString(q)));
//                    else playPreferredQualityOrWorseInApp(q);
//                    break;
//                case "set maximum":
//                    playPreferredQualityOrWorseInApp(q);
//                    break;
//            }
//        } else {
//            Toast.makeText(mContext, "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void playPreferredQualityOrWorseInApp(LinkedHashMap<String, String> q) {
//        String pref = "";
//        if (bMobileData) {
//            pref = mMobileMode;
//        } else {
//            pref = mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY,"");
//        }
//
//        if(prefOrWorseQIndex(q, pref) == null) {
//            showInAppPlayDialog(q, prefOrBestQIndex(q));
//            Toast.makeText(mContext, "Sorry. No video below the maximum quality.", Toast.LENGTH_SHORT).show();
//        } else {
//            mVideoPlayer.playStream(q.get(prefOrWorseQIndex(q, pref)));
//        }
//    }
//
    ////////////////////////////////////////////////////////////////////////////////////
//
//    public static String bestQUrl(LinkedHashMap<String, String> qualities) {
//        return qualities.get(VideoPlayback2.bestQString(qualities));
//    }



//    private void showPlayDialog(final LinkedHashMap<String, String> q, int best) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        final String qualities[] = q.keySet().toArray(new String[q.size()]);
//        String cleanQualities[] = getCleanQualities(qualities);
//
//        builder.setTitle("Select Quality")
//                .setSingleChoiceItems(cleanQualities, best, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mQualitySelected = which;
//                    }
//                })
//                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        playStream(q.get(qualities[mQualitySelected]));
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                });
//        builder.create();
//        builder.show();
//    }
//
//    private void showInAppPlayDialog(final LinkedHashMap<String, String> q, int best) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        final String qualities[] = q.keySet().toArray(new String[q.size()]);
//        String cleanQualities[] = getCleanQualities(qualities);
//
//        builder.setTitle("Select Quality")
//                .setSingleChoiceItems(cleanQualities, best, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mQualitySelected = which;
//                    }
//                })
//                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        mVideoPlayer.playStream(q.get(qualities[mQualitySelected]));
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                });
//        builder.create();
//        builder.show();
//    }

//    public void showInAppSettingsDialog(final LinkedHashMap<String, String> q, int best) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        final String qualities[] = q.keySet().toArray(new String[q.size()]);
//        String cleanQualities[] = getCleanQualities(qualities);
//
//        builder.setTitle("Select Quality")
//                .setSingleChoiceItems(cleanQualities, best, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mQualitySelected = which;
//                    }
//                })
//                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        mVideoPlayer.changeQuality(q.get(qualities[mQualitySelected]));
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                });
//        builder.create();
//        builder.show();
//    }


//
//    private String[] getCleanQualities(String[] s) {
//        String q[] = new String[s.length];
//
//        for (int i = 0; i < s.length; i++) {
//            if (s[i].contains("live")) {
//                q[i] = "source";
//                continue;
//            }
//            if (s[i].contains("chunked")){
//                q[i] = "source";
//                continue;
//            }
//            if (s[i].contains("audio_only")) {
//                q[i] = "audio only";
//                continue;
//            }
//            q[i] = s[i];
//        }
//        return q;
//    }
}
