

package com.TVCastLib.sampler.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.TVCastLib.core.MediaInfo;
import com.TVCastLib.core.SubtitleInfo;
import com.TVCastLib.device.ConnectableDevice;
import com.TVCastLib.sampler.R;
import com.TVCastLib.sampler.util.TestResponseObject;
import com.TVCastLib.service.capability.MediaControl;
import com.TVCastLib.service.capability.MediaControl.DurationListener;
import com.TVCastLib.service.capability.MediaControl.PlayStateListener;
import com.TVCastLib.service.capability.MediaControl.PlayStateStatus;
import com.TVCastLib.service.capability.MediaControl.PositionListener;
import com.TVCastLib.service.capability.MediaPlayer;
import com.TVCastLib.service.capability.MediaPlayer.MediaInfoListener;
import com.TVCastLib.service.capability.MediaPlayer.MediaLaunchObject;
import com.TVCastLib.service.capability.PlaylistControl;
import com.TVCastLib.service.capability.VolumeControl;
import com.TVCastLib.service.capability.VolumeControl.VolumeListener;
import com.TVCastLib.service.capability.listeners.ResponseListener;
import com.TVCastLib.service.command.ServiceCommandError;
import com.TVCastLib.service.sessions.LaunchSession;

import java.io.InputStream;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MediaPlayerFragment extends BaseFragment {
    public static final String URL_SUBTITLES_WEBVTT =
            "en.vtt";
    public static final String URL_SUBTITLE_SRT =
            "en.srt";
    public static final String URL_VIDEO_MP4 =
            "http://184.164.147.254/mp4/StarWars360.mp4";
    public static final String URL_IMAGE_ICON =
            "http://video-tv-cast.com/wp-content/uploads/2015/02/icon.png";

    public Button photoButton;
    public Button videoButton;
    public Button audioButton;
    public Button playButton;
    public Button pauseButton;
    public Button stopButton;
    public Button rewindButton;
    public Button fastForwardButton;
    public Button closeButton;
    public Button mediaInfoButton;
    public Button playlistButton;
    public Button previousButton;
    public Button nextButton;
    public Button jumpButton;
    public CheckBox loopingButton;
    public CheckBox subtitlesButton;

    public LaunchSession launchSession;
    private MediaControl mMediaControl = null;
    public TestResponseObject testResponse;

    public TextView positionTextView;
    public TextView durationTextView;
    public TextView mediaInfoTextView;
    public SeekBar mSeekBar;
    public boolean mIsUserSeeking;
    public SeekBar mVolumeBar;

    public EditText positionTrackView;

    public ImageView mediaInfoImageView;

    public boolean mSeeking;
    public Runnable mRefreshRunnable;
    public final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(1);
    public Handler mHandler;
    public long totalTimeDuration;
    public boolean mIsGettingPlayPosition;

    
    boolean isPlayingImage = false;
    boolean isPlaying = false;


    private PlaylistControl mPlaylistControl = null;

    private Timer refreshTimer;
    


    public MediaPlayerFragment() {};

    public MediaPlayerFragment(Context context)
    {
        super(context);

        mIsUserSeeking = false;
        mSeeking = false;
        mIsGettingPlayPosition = false;
        testResponse = new TestResponseObject();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setRetainInstance(true);
        View rootView = inflater.inflate(
                R.layout.fragment_media_player, container, false);

        photoButton = (Button) rootView.findViewById(R.id.photoButton);
        videoButton = (Button) rootView.findViewById(R.id.videoButton);
        audioButton = (Button) rootView.findViewById(R.id.audioButton);
        playButton = (Button) rootView.findViewById(R.id.playButton);
        pauseButton = (Button) rootView.findViewById(R.id.pauseButton);
        stopButton = (Button) rootView.findViewById(R.id.stopButton);
        rewindButton = (Button) rootView.findViewById(R.id.rewindButton);
        fastForwardButton = (Button) rootView.findViewById(R.id.fastForwardButton);
        closeButton = (Button) rootView.findViewById(R.id.closeButton);
        mediaInfoButton = (Button) rootView.findViewById(R.id.mediaInfo_button);
        playlistButton = (Button) rootView.findViewById(R.id.playlistButton);
        previousButton = (Button) rootView.findViewById(R.id.previousButton);
        nextButton = (Button) rootView.findViewById(R.id.nextButton);
        jumpButton = (Button) rootView.findViewById(R.id.jumpButton);
        loopingButton = (CheckBox) rootView.findViewById(R.id.loopingButton);
        subtitlesButton = (CheckBox) rootView.findViewById(R.id.subtitlesButton);

        positionTextView = (TextView) rootView.findViewById(R.id.stream_position);
        durationTextView = (TextView) rootView.findViewById(R.id.stream_duration);
        mediaInfoTextView = (TextView) rootView.findViewById(R.id.mediaInfo_textView);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.stream_seek_bar);
        mVolumeBar = (SeekBar) rootView.findViewById(R.id.volume_seek_bar);
        positionTrackView = (EditText) rootView.findViewById(R.id.positionText);
        mediaInfoImageView = (ImageView) rootView.findViewById(R.id.mediaInfo_imageView);

        buttons = new Button[] {
                photoButton, 
                videoButton, 
                audioButton, 
                playButton, 
                pauseButton, 
                stopButton, 
                rewindButton, 
                fastForwardButton, 
                closeButton,
                mediaInfoButton,
                playlistButton,
                previousButton,
                nextButton,
                jumpButton,
                loopingButton,
                subtitlesButton,
        };

        mHandler = new Handler();

        return rootView;
    }

    @Override
    public void setTv(ConnectableDevice tv) {
        super.setTv(tv);

        if (tv == null) {
            stopUpdating();
            mMediaControl = null;
            mPlaylistControl = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPlaying) {
            startUpdating();
        }
    }

    @Override
    public void onPause() {
        stopUpdating();
        super.onPause();
        testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Paused_Media);
    }

    @Override
    public void enableButtons()
    { 
        if (getTv().hasCapability(MediaPlayer.Display_Image)) {
            photoButton.setEnabled(true);
            photoButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    showImage();
                }
            });
        }
        else {
            disableButton(photoButton);
        }

        totalTimeDuration = -1;

        loopingButton.setEnabled(getTv().hasCapability(MediaPlayer.Loop));
        subtitlesButton.setEnabled(getTv().hasCapability(MediaPlayer.Subtitle_SRT)
                || getTv().hasCapability(MediaPlayer.Subtitle_WebVTT));

        if (getTv().hasCapability(MediaPlayer.Play_Video)) {
            videoButton.setEnabled(true);
            videoButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    playVideo();
                }
            });
        }
        else {
            disableButton(videoButton);
        }

        if (getTv().hasCapability(MediaPlayer.Play_Audio)) {
            audioButton.setEnabled(true);
            audioButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    playAudio();
                }
            });
        } else {
            disableButton(audioButton);
        }

        if (getTv().hasCapability(MediaPlayer.Play_Playlist)) {
            playlistButton.setEnabled(true);
            playlistButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    playM3U();
                    testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Played_Playlist);
                }
            });
        } else {
            disableButton(playlistButton);
        }

        mVolumeBar.setEnabled(getTv().hasCapability(VolumeControl.Volume_Set));
        mVolumeBar.setOnSeekBarChangeListener(volumeListener);

        if (getTv().hasCapability(VolumeControl.Volume_Get)) {
            getVolumeControl().getVolume(getVolumeListener);
        }

        if (getTv().hasCapability(VolumeControl.Volume_Subscribe)) {
            getVolumeControl().subscribeVolume(getVolumeListener);
        }

        if (getTv().hasCapability(MediaPlayer.MediaInfo_Get)) {
            mediaInfoButton.setEnabled(true);
            mediaInfoButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    getMediaPlayer().getMediaInfo(mediaInfoListener);
                }
            });
        }
        else 
            mediaInfoButton.setEnabled(false);

        if (getTv().hasCapability(MediaPlayer.MediaInfo_Subscribe)) {
            getMediaPlayer().subscribeMediaInfo(mediaInfoListener);
        }
        if (!isPlaying || !isPlayingImage)
            disableMedia();
        if (isPlaying) enableMedia();
        else if (isPlayingImage) {
            closeButton.setEnabled(true);
            closeButton.setOnClickListener(closeListener);
            stopUpdating();
        }
    }

    private void playAudio() {
        String mediaURL = "http://dl.irmp3.ir/data/song/Saeed_Asayesh-Bebar_Baroon-(WWW.IRMP3.IR).mp3";
        String iconURL = "http://images.all-free-download.com/images/graphiclarge/beautiful_natural_scenery_04_hd_pictures_166229.jpg";
        String title = "The Song that Doesn't End";
        String description = "Lamb Chop's Play Along";
        String mimeType = "audio/mp3";
        boolean shouldLoop = loopingButton.isChecked();

        MediaInfo mediaInfo = new MediaInfo.Builder(mediaURL, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconURL)
                .build();

        getMediaPlayer().playMedia(mediaInfo, shouldLoop, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.d("TVCastLib", "Error playing audio", error);
                stopMediaSession();
            }

            @Override
            public void onSuccess(MediaLaunchObject object) {
                Log.d("TVCastLib", "Started playing audio");
                launchSession = object.launchSession;
                testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Play_Audio);
                mMediaControl = object.mediaControl;
                mPlaylistControl = object.playlistControl;
                
                stopUpdating();
                enableMedia();
                isPlaying = true;
            }
        });
    }

    private void playM3U() {
        String mediaURL = "http://aljazeera-eng-apple-live.adaptive.level3.net/apple/aljazeera/english/appleman.m3u8";
        String iconURL = "http://images.all-free-download.com/images/graphiclarge/beautiful_natural_scenery_04_hd_pictures_166229.jpg";
        String title = "Playlist";
        String description = "Playlist description";
        String mimeType = "application/x-mpegurl";
        boolean shouldLoop = loopingButton.isChecked();

        MediaInfo mediaInfo = new MediaInfo.Builder(mediaURL, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconURL)
                .build();

        getMediaPlayer().playMedia(mediaInfo, shouldLoop, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.d("TVCastLib", "Error playing audio", error);
                stopMediaSession();
            }

            @Override
            public void onSuccess(MediaLaunchObject object) {
                Log.d("TVCastLib", "Started playing playlist");
                launchSession = object.launchSession;
                mMediaControl = object.mediaControl;
                mPlaylistControl = object.playlistControl;
                stopUpdating();
                enableMedia();
                isPlaying = true;
            }
        });
    }

    private void showImage() {
        disableMedia();

        String imagePath = "http://wallpaper-gallery.net/images/hd-nature-wallpapers-for-desktop-free-download/hd-nature-wallpapers-for-desktop-free-download-27.jpg";
        String mimeType = "image/jpeg";
        String title = "Test Image";
        String description = "Test Image Description";
        String icon = "http://images.all-free-download.com/images/graphiclarge/beautiful_natural_scenery_04_hd_pictures_166229.jpg";

        MediaInfo mediaInfo = new MediaInfo.Builder(imagePath, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(icon)
                .build();

        getMediaPlayer().displayImage(mediaInfo, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("Error", "Error displaying Image", error);
                stopMediaSession();
            }

            @Override
            public void onSuccess(MediaLaunchObject object) {
                launchSession = object.launchSession;
                closeButton.setEnabled(true);
                testResponse = new TestResponseObject(true, TestResponseObject.SuccessCode,
                        TestResponseObject.Display_image);
                closeButton.setOnClickListener(closeListener);
                stopUpdating();
                isPlayingImage = true;
            }
        });
    }

    private void playVideo() {
        boolean shouldLoop = loopingButton.isChecked();

        SubtitleInfo.Builder subtitleBuilder = null;
        if (subtitlesButton.isChecked()) {
            subtitleBuilder = new SubtitleInfo.Builder(
                    getTv().hasCapability(MediaPlayer.Subtitle_WebVTT) ? URL_SUBTITLES_WEBVTT :
                            URL_SUBTITLE_SRT);
            subtitleBuilder.setLabel("English").setLanguage("en");
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(URL_VIDEO_MP4, "video/mp4")
                .setTitle("Video Title")
                .setDescription("Video Description")
                .setIcon(URL_IMAGE_ICON)
                .setSubtitleInfo(subtitleBuilder == null ? null : subtitleBuilder.build())
                .build();

        getMediaPlayer().playMedia(mediaInfo, shouldLoop, new MediaPlayer.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("Error", "Error playing video", error);
                stopMediaSession();
            }

            public void onSuccess(MediaLaunchObject object) {
                launchSession = object.launchSession;
                testResponse = new TestResponseObject(true, TestResponseObject.SuccessCode,
                        TestResponseObject.Play_Video);
                mMediaControl = object.mediaControl;
                mPlaylistControl = object.playlistControl;
                stopUpdating();
                enableMedia();
                isPlaying = true;
            }
        });
    }


    private void stopMediaSession() {
        // don't call launchSession.close() here, currently it can close
        // a different web app in WebOS
        if (launchSession != null) {
            launchSession = null;
            stopUpdating();
            disableMedia();
            isPlaying = isPlayingImage = false;
        }
    }

    @Override
    public void disableButtons() {
        mSeekBar.setEnabled(false);
        mVolumeBar.setEnabled(false);
        mVolumeBar.setOnSeekBarChangeListener(null);
        positionTextView.setEnabled(false);
        durationTextView.setEnabled(false);

        mediaInfoTextView.setText("");
        mediaInfoImageView.setImageBitmap(null);
        positionTrackView.setEnabled(false);

        loopingButton.setChecked(false);
        subtitlesButton.setEnabled(false);
        super.disableButtons();
    }

    protected void onSeekBarMoved(long position) {
        if (mMediaControl != null && getTv().hasCapability(MediaControl.Seek)) {
            mSeeking = true;

            mMediaControl.seek(position, new ResponseListener<Object>() {

                @Override
                public void onSuccess(Object response) {
                    Log.d("TVCastLib", "Success on Seeking");
                    mSeeking = false;
                    startUpdating();
                }

                @Override
                public void onError(ServiceCommandError error) {
                    Log.w("TVCastLib", "Unable to seek: " + error.getCode());
                    mSeeking = false;
                    startUpdating();
                }
            });
        }
    }

    public void enableMedia() {

        playButton.setEnabled(getTv().hasCapability(MediaControl.Play));
        pauseButton.setEnabled(getTv().hasCapability(MediaControl.Pause));
        stopButton.setEnabled(getTv().hasCapability(MediaControl.Stop));
        rewindButton.setEnabled(getTv().hasCapability(MediaControl.Rewind));
        fastForwardButton.setEnabled(getTv().hasCapability(MediaControl.FastForward));
        mSeekBar.setEnabled(getTv().hasCapability(MediaControl.Seek));
        closeButton.setEnabled(getTv().hasCapability(MediaPlayer.Close));
        previousButton.setEnabled(getTv().hasCapability(PlaylistControl.Previous));
        nextButton.setEnabled(getTv().hasCapability(PlaylistControl.Next));
        jumpButton.setEnabled(getTv().hasCapability(PlaylistControl.JumpToTrack));
        positionTrackView.setEnabled(getTv().hasCapability(PlaylistControl.JumpToTrack));


        fastForwardButton.setOnClickListener(fastForwardListener);
        mSeekBar.setOnSeekBarChangeListener(seekListener);
        rewindButton.setOnClickListener(rewindListener);
        stopButton.setOnClickListener(stopListener);
        playButton.setOnClickListener(playListener);
        pauseButton.setOnClickListener(pauseListener);
        previousButton.setOnClickListener(previousListener);
        nextButton.setOnClickListener(nextListener);
        jumpButton.setOnClickListener(jumpListener);
        closeButton.setOnClickListener(closeListener);

        if (getTv().hasCapability(MediaControl.PlayState_Subscribe) && !isPlaying) {
            mMediaControl.subscribePlayState(playStateListener);
        } else {
            if (mMediaControl != null) {
                mMediaControl.getDuration(durationListener);
            }
            startUpdating();
        }
    }

    public void disableMedia() {
        closeButton.setEnabled(false);
        closeButton.setOnClickListener(null);

        stopMedia();
    }

    public void stopMedia() {
        playButton.setEnabled(false);
        playButton.setOnClickListener(null);
        pauseButton.setEnabled(false);
        pauseButton.setOnClickListener(null);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(null);
        rewindButton.setEnabled(false);
        rewindButton.setOnClickListener(null);
        fastForwardButton.setEnabled(false);
        fastForwardButton.setOnClickListener(null);
        previousButton.setEnabled(false);
        previousButton.setOnClickListener(null);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(null);
        jumpButton.setEnabled(false);
        jumpButton.setOnClickListener(null);
        positionTrackView.setEnabled(false);

        mSeekBar.setEnabled(false);
        mSeekBar.setOnSeekBarChangeListener(null);
        mSeekBar.setProgress(0);

        positionTextView.setText("--:--:--");
        durationTextView.setText("--:--:--");

        totalTimeDuration = -1;
    }

    public View.OnClickListener playListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mMediaControl != null)
                mMediaControl.play(null);
            testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Played_Media);
        }
    };

    public View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
                mMediaControl.pause(null);
        	    testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Paused_Media);
        }
    };

    public View.OnClickListener previousListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPlaylistControl != null)
                mPlaylistControl.previous(null);
            testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Previous);
        }
    };

    public View.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPlaylistControl != null)
                mPlaylistControl.next(null);
            testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Next);
        }
    };

    public View.OnClickListener jumpListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPlaylistControl != null) {
                mPlaylistControl.jumpToTrack(Integer.parseInt(positionTrackView.getText().toString()), null);
                testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Jump);
            }
        }
    };


    public View.OnClickListener closeListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (getMediaPlayer() != null) {
                if (launchSession != null)
                    launchSession.close(null);
                    launchSession = null;

                disableMedia();
                stopUpdating();
                isPlaying = isPlayingImage = false;
                testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Closed_Media);
            }
        }
    };

    public View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mMediaControl != null)
                mMediaControl.stop(new ResponseListener<Object>() {

                    @Override
                    public void onSuccess(Object response) {
                        stopMedia();
                        stopUpdating();
                        testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Stopped_Media);
                        isPlaying = false;
                        isPlayingImage = true;
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                    }
                });
        }
    };

    public View.OnClickListener rewindListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mMediaControl != null)
                mMediaControl.rewind(null);
            testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.Rewind_Media);
        }
    };

    public View.OnClickListener fastForwardListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mMediaControl != null)
                mMediaControl.fastForward(null);
            testResponse =  new TestResponseObject(true, TestResponseObject.SuccessCode, TestResponseObject.FastForward_Media);
        }
    };

    public OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsUserSeeking = false;
            mSeekBar.setSecondaryProgress(0);
            onSeekBarMoved(seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsUserSeeking = true;
            mSeekBar.setSecondaryProgress(seekBar.getProgress());
            stopUpdating();
        }

        @Override
        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {

        }
    };

    public OnSeekBarChangeListener volumeListener = new OnSeekBarChangeListener() {

        @Override public void onStopTrackingTouch(SeekBar arg0) { }
        @Override public void onStartTrackingTouch(SeekBar arg0) { }

        @Override
        public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
            if (fromUser)
                getVolumeControl().setVolume((float) mVolumeBar.getProgress() / 100.0f, null);
        }
    };

    public VolumeListener getVolumeListener = new VolumeListener() {

        @Override
        public void onError(ServiceCommandError error) {
            Log.d("TVCastLib", "Error getting Volume: " + error);
        }

        @Override
        public void onSuccess(Float object) {
            mVolumeBar.setProgress((int) (object * 100.0f));
        }
    };

    public PlayStateListener playStateListener = new PlayStateListener() {

        @Override
        public void onError(ServiceCommandError error) {
            Log.d("TVCastLib", "Playstate Listener error = " + error);
        }

        @Override
        public void onSuccess(PlayStateStatus playState) {
            Log.d("TVCastLib", "Playstate changed | playState = " + playState);

            switch (playState) {
                case Playing:
                    startUpdating();

                    if (mMediaControl != null && getTv().hasCapability(MediaControl.Duration)) {
                        mMediaControl.getDuration(durationListener);
                    }
                    break;
                case Finished:
                    positionTextView.setText("--:--");
                    durationTextView.setText("--:--");
                    mSeekBar.setProgress(0);

                default:
                    stopUpdating();
                    break;
            }
        }
    };

    private void startUpdating() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Log.d("TVCastLib", "Updating information");
                if (mMediaControl != null && getTv() != null && getTv().hasCapability(MediaControl.Position)) {
                    mMediaControl.getPosition(positionListener);
                }

                if (mMediaControl != null
                        && getTv() != null
                        && getTv().hasCapability(MediaControl.Duration)
                        && !getTv().hasCapability(MediaControl.PlayState_Subscribe)
                        && totalTimeDuration <= 0) {
                    mMediaControl.getDuration(durationListener);
                }
            }
        }, 0, REFRESH_INTERVAL_MS);
    }

    private void stopUpdating() {
        if (refreshTimer == null)
            return;

        refreshTimer.cancel();
        refreshTimer = null;
    }

    private PositionListener positionListener = new PositionListener() {

        @Override public void onError(ServiceCommandError error) { }

        @Override
        public void onSuccess(Long position) {
            positionTextView.setText(formatTime(position.intValue()));
            mSeekBar.setProgress(position.intValue());
        }
    };

    private DurationListener durationListener = new DurationListener() {

        @Override public void onError(ServiceCommandError error) { }

        @Override
        public void onSuccess(Long duration) {
            totalTimeDuration = duration;
            mSeekBar.setMax(duration.intValue());
            durationTextView.setText(formatTime(duration.intValue()));
        }
    };

    private MediaInfoListener mediaInfoListener = new MediaInfoListener() {

        @Override
        public void onSuccess(MediaInfo mediaInfo) {
            String text = mediaInfo.getTitle();
            text += "\n";
            text += mediaInfo.getDescription();
            mediaInfoTextView.setText(text);
            final String stringUrl = mediaInfo.getImages().get(0).getUrl();

            if (stringUrl!=null) new DownloadImageTask(mediaInfoImageView).execute(stringUrl);
        }

        @Override
        public void onError(ServiceCommandError error) {

        }

    };

    private String formatTime(long millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        else {
            time = String.format(Locale.US, "%d:%02d", minutes, seconds);
        }

        return time;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                Log.d("", urldisplay);
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
