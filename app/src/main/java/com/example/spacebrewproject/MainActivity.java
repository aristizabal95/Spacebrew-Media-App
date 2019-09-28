package com.example.spacebrewproject;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.daasuu.epf.EPlayerView;
import com.daasuu.epf.filter.GlBilateralFilter;
import com.daasuu.epf.filter.GlBulgeDistortionFilter;
import com.daasuu.epf.filter.GlCGAColorspaceFilter;
import com.daasuu.epf.filter.GlCrosshatchFilter;
import com.daasuu.epf.filter.GlExposureFilter;
import com.daasuu.epf.filter.GlHalftoneFilter;
import com.daasuu.epf.filter.GlHazeFilter;
import com.daasuu.epf.filter.GlHighlightShadowFilter;
import com.daasuu.epf.filter.GlInvertFilter;
import com.daasuu.epf.filter.GlLookUpTableFilter;
import com.daasuu.epf.filter.GlLuminanceFilter;
import com.daasuu.epf.filter.GlLuminanceThresholdFilter;
import com.daasuu.epf.filter.GlPixelationFilter;
import com.daasuu.epf.filter.GlRGBFilter;
import com.daasuu.epf.filter.GlSepiaFilter;
import com.daasuu.epf.filter.GlSolarizeFilter;
import com.daasuu.epf.filter.GlSphereRefractionFilter;
import com.daasuu.epf.filter.GlSwirlFilter;
import com.daasuu.epf.filter.GlThreex3TextureSamplingFilter;
import com.daasuu.epf.filter.GlToneCurveFilter;
import com.daasuu.epf.filter.GlToneFilter;
import com.daasuu.epf.filter.GlVignetteFilter;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.jraf.android.alibglitch.GlitchEffect;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import spacebrew.Spacebrew;

public class MainActivity extends AppCompatActivity {

    private String server; //properties for spacebrew
    private String name; //properties for spacebrew
    private String description; //properties for spacebrew

    private Random rnd; // random number generator

    private SimpleExoPlayer player;
    private EPlayerView mainPlayerView;
    private Button sbInteractBtn;
    private List<String> sourceList = Arrays.asList("file:///android_asset/video.mp4", "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
    private int sourceIdx = 0;
    private int id;
    private Activity act = this;

    /** Run this on application start*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // Force fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // force keep-awake
        setContentView(R.layout.activity_main);

        rnd = new Random();

        server = "ws://192.168.0.10:9000";
        name = "P5 Button Example " + id;
        description = "Client that sends and receives boolean messages. Background turns yellow when message received.";

        SpacebrewCallbacks sketch = new SpacebrewCallbacks(this); // Spacebrew requires a processing sketch to run
        final Spacebrew sb = new Spacebrew(sketch);

        sbInteractBtn = (Button)findViewById(R.id.sbInteractBtn);

        player = ExoPlayerFactory.newSimpleInstance(this.getApplicationContext());
        mainPlayerView = (EPlayerView)findViewById(R.id.mainPlayerView);

        // Add sub-pub interactions to spacebrew
        sb.addPublish( "device_publisher", "string", "" );
        sb.addSubscribe( "device_subscribe", "string", "" );

        // connect to the server specified previously
        sb.connect(server, name, description);

        // Attach the ExoPlayer to the PlayerView
        mainPlayerView.setSimpleExoPlayer(player);

        final DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        final MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(sourceList.get(sourceIdx)));

        player.prepare(videoSource);
        player.setPlayWhenReady(true);
        mainPlayerView.onResume();

        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == 4) {
                    sb.send( "device_publisher", "1:video_finished");
                    goToNextVideo();
                }
                //Log.d("PLAYERSTATE", playWhenReady + "" + playbackState);

            }
        });


        // add a listener to the layout button that interacts with spacebrew.
        /** This runs every time the user clicks the button */
        sbInteractBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send a message to the spacebrew server indication the button has been pressed
                sb.send( "device_publisher", "1:skip_button_pressed");
                goToNextVideo();
            }
        });
    }

    /** This function runs when a boolean message is received from spacebrew. */
    public void onBooleanMessage( String name, boolean value ){

        final int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        goToNextVideo();
        Log.d("COLOR", color + "");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rnd.nextInt(2) == 1) {
                    mainPlayerView.setGlFilter(new GlSwirlFilter());
                }
                else {
                    mainPlayerView.setGlFilter(new GlVignetteFilter());
                }
                GlitchEffect.showGlitch(act);
            }
        });


        //runOnUiThread(new Runnable() {
        //    @Override
        //    public void run() {
        //        mainPlayerView.setBackgroundColor(color);
        //    }
        //});
    }

    private void goToNextVideo() {
        sourceIdx = (sourceIdx + 1) % sourceList.size();
        final DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        Log.d("SOURCEIDX", sourceIdx + "");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediaSource nextVideoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(sourceList.get(sourceIdx)));
                player.prepare(nextVideoSource);
            }
        });
    }
}
