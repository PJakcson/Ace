package com.aceft.data;

import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.aceft.R;
import com.aceft.ui_fragments.channel_fragments.ChannelFragment;

import java.util.LinkedHashMap;

public class VideoPlayback {
    private Context mContext;
    private int mQualitySelected;

    public VideoPlayback(Context c) {
        mContext = c;
    }

    public void playStream(String s) {
        if (s == null || mContext == null) return;
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        mContext.startActivity(Intent.createChooser(stream, mContext.getResources().getText(R.string.send_to_intent)));
    }

    public void showPlayDialog(final LinkedHashMap<String, String> q, int best) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final String qualities[] = q.keySet().toArray(new String[q.size()]);
        String cleanQualities[] = getCleanQualities(qualities);

        builder.setTitle("Select Quality")
                .setSingleChoiceItems(cleanQualities, best, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mQualitySelected = which;
                    }
                })
                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        playStream(q.get(qualities[mQualitySelected]));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private String[] getCleanQualities(String[] s) {
        String q[] = new String[s.length];

        for (int i = 0; i < s.length; i++) {
            if (s[i].contains("live")) {
                q[i] = "source";
                continue;
            }
            if (s[i].contains("chunked")){
                q[i] = "source";
                continue;
            }
            if (s[i].contains("audio_only")) {
                q[i] = "audio only";
                continue;
            }
            q[i] = s[i];
        }
        return q;
    }
}
