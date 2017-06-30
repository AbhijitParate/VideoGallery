package com.centralway.abhijit.videogallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoActivity extends AppCompatActivity
        implements
        ExoPlayer.EventListener,
        SeekBar.OnSeekBarChangeListener {

//    private static final String TAG = VideoActivity.class.getSimpleName();

    private static final String PLAY_MODE = "PLAY_MODE";
    private static final String PLAY_SINGLE = "PLAY_SINGLE";
    private static final String PLAY_MULTIPLE = "PLAY_MULTIPLE";

    private static final String KEY_SINGLE = "KEY_SINGLE";
    private static final String KEY_MULTIPLE = "KEY_MULTIPLE";

    private static final int UI_AUTO_HIDE_DELAY = 2000;
    private static final int PLAYER_UPDATE_DELAY = 1000;
    private static final int MIN_REWIND_DURATION = 5000;
    private static final int SEEK_DURATION = 1000;

    @BindView(R.id.videoPlayer)
    SimpleExoPlayerView videoPlayer;

    @BindView(R.id.playerControlPlayPause)
    ImageButton playerControlPlayPause;

    @BindView(R.id.playerControlNext)
    ImageButton playerControlNext;

    @BindView(R.id.playerControlPrevious)
    ImageButton playerControlPrevious;

    @BindView(R.id.playerControlFastForward)
    ImageButton playerControlFastForward;

    @BindView(R.id.playerControlRewind)
    ImageButton playerControlRewind;

    @BindView(R.id.playerControlVolume)
    ImageButton playerControlVolume;

//    @BindView(R.id.playerControlPlaylist)
//    ImageButton playerControlPlaylist;

    @BindView(R.id.playerControlSeekBar)
    SeekBar playerControlSeekBar;

    @BindView(R.id.playerControlVolumeSeekBar)
    SeekBar playerControlVolumeSeekBar;

//    @BindView(R.id.playerVideoTitle)
//    TextView playerVideoTitle;

    @BindView(R.id.playerElapsedTime)
    TextView playerElapsedTime;

    @BindView(R.id.playerTotalTime)
    TextView playerTotalTime;

    @BindView(R.id.tvVolume)
    TextView tvVolume;

    private SimpleExoPlayer player;
    private Uri videoUri;
    private ArrayList<String> videoUris;
    private String playBackMode;

    private long previousPosition;
    private boolean previousPlaybackState;

    private AudioManager audioManager;

    private final Handler mHideHandler = new Handler();

    private final Runnable mHideVolumeControlsRunnable = new Runnable() {
        @Override
        public void run() {
            tvVolume.setVisibility(View.GONE);
            playerControlVolumeSeekBar.setVisibility(View.GONE);
        }
    };

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            mHideHandler.postDelayed(updateProgressRunnable, PLAYER_UPDATE_DELAY);
        }
    };

    public static Intent playSingle(Activity activity, Uri videoUri) {
        Intent intent = new Intent(activity, VideoActivity.class);
        intent.putExtra(PLAY_MODE, PLAY_SINGLE);
        intent.putExtra(KEY_SINGLE, videoUri.toString());
        return intent;
    }

    public static Intent playList(Activity activity, ArrayList<String> videoUris) {
        Intent intent = new Intent(activity, VideoActivity.class);
        intent.putExtra(PLAY_MODE, PLAY_MULTIPLE);
        intent.putExtra(KEY_MULTIPLE, videoUris);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        ButterKnife.bind(this);
        // Hide system ui like
        hideSystemUi();
        // Process Uris received in the intent
        setupActivityMode();
        //Setup ExoPlayer
        setupVideoPlayer();
        // Setup ExoPlayer controls
        setupControls();
    }

    private void setupActivityMode() {
        Intent intent = getIntent();
        playBackMode = intent.getStringExtra(PLAY_MODE);

        if (playBackMode.equals(PLAY_SINGLE)) {
            videoUri = Uri.parse(intent.getStringExtra(KEY_SINGLE));
        } else if (playBackMode.equals(PLAY_MULTIPLE)) {
            videoUris = intent.getStringArrayListExtra(KEY_MULTIPLE);
            Toast.makeText(this, "items received " + videoUris.size(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupControls() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        playerControlVolumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        playerControlVolumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        playerControlSeekBar.setOnSeekBarChangeListener(this);
        playerControlVolumeSeekBar.setOnSeekBarChangeListener(this);
    }

    private void setupVideoPlayer(){
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory
                = new DefaultDataSourceFactory(
                        this.getApplicationContext(), Util.getUserAgent(this, "VideoGallery"), null);

        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the media to be played.
        MediaSource[] mediaSources = new MediaSource[1];
        if (playBackMode.equals(PLAY_SINGLE)) {
            mediaSources[0] = new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null);
        } else if (playBackMode.equals(PLAY_MULTIPLE)){
            mediaSources = new MediaSource[videoUris.size()];
            for (int i = 0; i < videoUris.size(); i++) {
                Uri uri = Uri.parse(videoUris.get(i));
                mediaSources[i] = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
            }
        }

        // Set MediaSource
        MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
                : new ConcatenatingMediaSource(mediaSources);

        // Set looping sources
        LoopingMediaSource compositeSource = new LoopingMediaSource(mediaSource);

        videoPlayer.setPlayer(player);
        videoPlayer.requestFocus();

        player.prepare(compositeSource);
        player.addListener(this);
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume playback if was playing previously
        resumePreviousPlaybackState();
    }

    private void resumePreviousPlaybackState() {
        if (player.getPlaybackState() == ExoPlayer.STATE_READY){
            player.seekTo(previousPosition);
            player.setPlayWhenReady(previousPlaybackState);
        }
    }

    private void hideSystemUi() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        videoPlayer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlaybackState();
    }

    private void savePlaybackState() {
        // Save player state
        if (player != null) {
            previousPosition = player.getCurrentPosition();
            previousPlaybackState = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
        }
    }

    @OnClick(R.id.playerControlVolume)
    void onPlayerVolumeClick(){
        toggleVolumeControl();
    }

    private void toggleVolumeControl() {
        if (playerControlVolumeSeekBar.getVisibility() == View.VISIBLE){
            playerControlVolumeSeekBar.setVisibility(View.GONE);
        } else {
            playerControlVolumeSeekBar.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.playerControlPlayPause)
    void onPlayPauseClick(){
        togglePlayback();
    }

    @OnClick(R.id.playerControlNext)
    void onNextClick(){
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }
        Timeline.Window currentWindow = new Timeline.Window();
        int currentWindowIndex = player.getCurrentWindowIndex();
        if (currentWindowIndex < currentTimeline.getWindowCount() - 1) {
            player.seekTo(currentWindowIndex + 1, C.TIME_UNSET);
        } else if (currentTimeline.getWindow(currentWindowIndex, currentWindow, false).isDynamic) {
            player.seekTo(currentWindowIndex, C.TIME_UNSET);
            player.setPlayWhenReady(true);
        }
    }

    @OnClick(R.id.playerControlPrevious)
    void onPreviousClick(){
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }
        int currentWindowIndex = player.getCurrentWindowIndex();
        Timeline.Window currentWindow = new Timeline.Window();
        currentTimeline.getWindow(currentWindowIndex, currentWindow);
        if (currentWindowIndex > 0 && player.getCurrentPosition() < MIN_REWIND_DURATION
                || (currentWindow.isDynamic && !currentWindow.isSeekable)) {
            player.seekTo(currentWindowIndex - 1, C.TIME_UNSET);
        } else {
            player.seekTo(0);
        }
    }

    @OnClick(R.id.playerControlFastForward)
    void onFastForwardClick(){
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }

        int currentWindowIndex = player.getCurrentWindowIndex();
        player.seekTo(currentWindowIndex, Math.min(player.getCurrentPosition() + SEEK_DURATION, player.getDuration()));
        updateProgress();
    }

    @OnClick(R.id.playerControlRewind)
    void onRewindClick(){
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }
        int currentWindowIndex = player.getCurrentWindowIndex();
        player.seekTo(currentWindowIndex, Math.max(player.getCurrentPosition() - SEEK_DURATION, 0));
        updateProgress();
    }

    private void togglePlayback() {
        player.setPlayWhenReady(!player.getPlayWhenReady());
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playWhenReady) {
            playerControlPlayPause.setImageDrawable(ResourcesCompat.getDrawable(getResources() , R.drawable.ic_player_control_pause, null));
            mHideHandler.postDelayed(updateProgressRunnable, PLAYER_UPDATE_DELAY);
        } else {
            playerControlPlayPause.setImageDrawable(ResourcesCompat.getDrawable(getResources() , R.drawable.ic_player_control_play, null));
            mHideHandler.removeCallbacks(updateProgressRunnable);
        }

        if (playbackState == ExoPlayer.STATE_READY){
            String totalTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(player.getDuration()),
                    TimeUnit.MILLISECONDS.toSeconds(player.getDuration()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(player.getDuration()))
            );
            playerTotalTime.setText(totalTime);
        } else if (playbackState == ExoPlayer.STATE_ENDED){
            videoPlayer.showController();
        }
    }

    private void updateProgress() {
        int progress = (int) (player.getCurrentPosition() * 100 / player.getDuration());
        playerControlSeekBar.setProgress(progress);

        String elapsedTime = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(player.getCurrentPosition()),
                TimeUnit.MILLISECONDS.toSeconds(player.getCurrentPosition()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(player.getCurrentPosition()))
        );
        playerElapsedTime.setText(elapsedTime);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.playerControlVolumeSeekBar){
            updateVolume(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.playerControlSeekBar)
            if (player.getPlayWhenReady()) togglePlayback();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.playerControlVolumeSeekBar){
            mHideHandler.postDelayed(mHideVolumeControlsRunnable, UI_AUTO_HIDE_DELAY);
        } else if (seekBar.getId() == R.id.playerControlSeekBar) {
            seekVideo();
            togglePlayback();
        }
    }

    private void seekVideo() {
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }

        int currentWindowIndex = player.getCurrentWindowIndex();
        long currentTime = player.getDuration() * playerControlSeekBar.getProgress() / 100;
        player.seekTo(currentWindowIndex, currentTime);
    }

    public void updateVolume(int volume) {
        tvVolume.setVisibility(View.VISIBLE);
        tvVolume.setText(String.valueOf(volume));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }
}
