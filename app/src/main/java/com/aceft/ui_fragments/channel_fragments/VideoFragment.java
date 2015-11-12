package com.aceft.ui_fragments.channel_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.OldVideoListAdapter;
import com.aceft.data.LiveMethods;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.VideoPlayback;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;

public class VideoFragment extends Fragment {

    private ArrayList<String> qualities;
    private LinkedHashMap <String,String> mData;

    private int mStartIndex;

    private boolean adIsOnTop = false;

    public VideoFragment newInstance(TwitchVod h, TwitchVideo twitchVideo) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putInt("start_index", h.getStartOffsetIndex());
        args.putStringArrayList("lengths", h.getLengths());
        args.putStringArrayList("qualities", h.getAvailableQualities());
        args.putSerializable("data", h.toHashmap());
        args.putString("title", twitchVideo.mTitle);
        args.putString("description", twitchVideo.mDesc);
        args.putString("views", twitchVideo.mViews);
        args.putString("previewLink", twitchVideo.mPreviewLink);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        RelativeLayout header = (RelativeLayout) rootView.findViewById(R.id.channelData);
        ListView videos = (ListView) rootView.findViewById(R.id.videoGrid);

        ArrayList<String> lengths = getArguments().getStringArrayList("lengths");
        mStartIndex = getArguments().getInt("start_index");
        qualities = getArguments().getStringArrayList("qualities");

        String title = getArguments().getString("title");
        String desc = getArguments().getString("description");
        String views = getArguments().getString("views");
        String pLink = getArguments().getString("previewLink");

        try {
            if(getArguments().getSerializable("data") instanceof LinkedHashMap) {
                mData = (LinkedHashMap<String, String>) getArguments().getSerializable("data");
            } else {
                Toast.makeText(getActivity(), "Ups, something went wrong.", Toast.LENGTH_SHORT).show();
            }
        } catch (ClassCastException e) {
            Toast.makeText(getActivity(), "Ups, something went wrong.", Toast.LENGTH_SHORT).show();
        }

        OldVideoListAdapter adapter = new OldVideoListAdapter(this, lengths);

        ImageView thumb = (ImageView) rootView.findViewById(R.id.videoThumb);
        loadLogo(pLink, thumb);

        setHeaderHeight(thumb);

        ((TextView)header.findViewById(R.id.videoTitle)).setText(title);
        ((TextView)header.findViewById(R.id.viewsAndRecorded)).setText(desc);
        ((TextView)header.findViewById(R.id.videoViews)).setText(views);

        videos.setAdapter(adapter);

        videos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playStream(getHash(position + mStartIndex));
            }
        });

        videos.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (totalItemCount > 1 && totalItemCount >= visibleItemCount) {
                    if (lastVisibleItem >= totalItemCount-1 && !adIsOnTop) {
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity)getActivity()).pushDownAd();
                        adIsOnTop = true;
                    }
                    if (lastVisibleItem < totalItemCount-1 && adIsOnTop) {
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity)getActivity()).pushUpAd();
                        adIsOnTop = false;
                    }
                }
            }
        });

        return rootView;
    }

    private void playStream(LinkedHashMap<String, String> result) {
        if (result == null) return;
        if (result.isEmpty()) {
            Toast.makeText(getActivity(), "Could not load the vod. Maybe you need to subscribe to the channel.", Toast.LENGTH_LONG).show();
            return;
        }

        String q = LiveMethods.getQualityKey(getActivity(), result);
//        final String qualities[] = result.keySet().toArray(new String[result.size()]);
        VideoPlayback p = new VideoPlayback(getActivity());

        if (q.contains("showDialog")) {
            int selQualtity = Integer.parseInt(q.substring(q.length() - 1));
            p.showPlayDialog(result, selQualtity);
        } else {
            new VideoPlayback(getActivity()).playStream(result.get(q));
        }
    }

    private void setHeaderHeight(ImageView header) {
        int width;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        if (isInLandscape())
            header.getLayoutParams().width = width/4;
        else
            header.getLayoutParams().width = width/3;
    }

    private LinkedHashMap<String, String> getHash(int p) {
        if (mData == null) return null;
        LinkedHashMap<String, String> qurls = new LinkedHashMap<>();
        for (String q: qualities) {
            qurls.put(q, mData.get(q+p));
        }
        return qurls;
    }

    private void loadLogo(final String url, final ImageView imageView) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                final Bitmap bitmap = TwitchNetworkTasks.downloadBitmap(url);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (imageView != null)
                            imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private boolean isInLandscape() {
        return getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity)
            ((MainActivity)getActivity()).resetAdPosition();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.show();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

}