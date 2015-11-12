package com.aceft.ui_fragments.channel_fragments.video;

import android.media.MediaCodec;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;

import java.io.IOException;
import java.util.Map;

public class HlsRendererBuilder implements HlsPlayer.RendererBuilder, ManifestFetcher.ManifestCallback<HlsPlaylist> {

    private final String userAgent;
    private final String url;

    private HlsPlayer player;
    private HlsPlayer.RendererBuilderCallback callback;

    public HlsRendererBuilder(String userAgent, String url) {
        this.userAgent = userAgent;
        this.url = url;
    }

    @Override
    public void buildRenderers(HlsPlayer player, HlsPlayer.RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;
        HlsPlaylistParser parser = new HlsPlaylistParser();
        ManifestFetcher<HlsPlaylist> playlistFetcher =
                new ManifestFetcher<>(url, new DefaultHttpDataSource(userAgent, null), parser);

        try {
            playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSingleManifest(HlsPlaylist manifest) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        DataSource dataSource = new UriDataSource(userAgent, bandwidthMeter);
        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, url, manifest, bandwidthMeter, null,
                HlsChunkSource.ADAPTIVE_MODE_NONE);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, true, 3);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, player.getMainHandler(), player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        MetadataTrackRenderer<Map<String, Object>> id3Renderer =
                new MetadataTrackRenderer<>(sampleSource, new Id3Parser(),
                        player.getId3MetadataRenderer(), player.getMainHandler().getLooper());

        Eia608TrackRenderer closedCaptionRenderer = new Eia608TrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());

        TrackRenderer[] renderers = new TrackRenderer[HlsPlayer.RENDERER_COUNT];
        renderers[HlsPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[HlsPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[HlsPlayer.TYPE_TIMED_METADATA] = id3Renderer;
        renderers[HlsPlayer.TYPE_TEXT] = closedCaptionRenderer;
        callback.onRenderers(null, null, renderers);
    }

    @Override
    public void onSingleManifestError(IOException e) {
        callback.onRenderersError(e);
    }
}
