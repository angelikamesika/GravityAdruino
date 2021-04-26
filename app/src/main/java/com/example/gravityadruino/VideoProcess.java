package com.example.gravityadruino;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.VideoView;

import java.io.File;


public class VideoProcess {

    private Activity mContext;

    public VideoProcess(Activity aContext) {
        mContext = aContext;
    }

    // for test 20, 40, 90
    public static String[] TEST = {"DCIM" + File.separator + "g1"};
    public static String[] NEUTRAL = {"gravity_pose1_neutral"}; //loop
    public static String[] RELOADING = {"gravity_pose1_reloading",
            "gravity_pose1_neutral"};

    public static String[] KG_20 = {"gravity_pose1_scale_20kg",
            "gravity_pose1_scale_20kg_continuation"}; // one and loop

    public static String[] KG_30 = {"gravity_pose1_scale_30kg",
            "gravity_pose1_scale_30kg_continuation"}; // one and loop

    public static String[] KG_40 = {"gravity_pose1_scale_40kg",
            "gravity_pose1_scale_40kg_continuation"}; // one and loop

    public static String[] KG_50 = {"gravity_pose1_scale_50kg",
            "gravity_pose1_scale_50kg_continuation"}; // one and loop

    public static String[] KG_60 = {"gravity_pose1_scale_60kg",
            "gravity_pose1_scale_60kg_continuation"}; // one and loop

    public static String[] KG_70 = {"gravity_pose1_scale_70kg",
            "gravity_pose1_scale_70kg_continuation"}; // one and loop

    public static String[] KG_80 = {"gravity_pose1_scale_80kg",
            "gravity_pose1_scale_80kg_continuation"}; // one and loop

    public static String[] KG_90 = {"gravity_pose1_scale_90kg",
            "gravity_pose1_scale_90kg_continuation"}; // one and loop

    public static String[] TUNING = {"gravity_scale_tuning"}; // loop

/*    GRAVITY:
            gravity_pose1_neutral.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_reloading.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_20kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_20kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_30kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_30kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_40kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_40kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_50kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_50kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_60kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_60kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_70kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_70kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_80kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_80kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_90kg.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_pose1_scale_90kg_continuation.mp4
0_HNN_ART/GRAVITY_video_for_programming/gravity_scale_tuning.mp4*/

    public Uri getFileURI(String aFilename) {
        Log.d("AAA_GRAV", "getFileURI aFilename " + aFilename);
        int id = mContext.getResources().
                getIdentifier(aFilename, "raw", mContext.getPackageName());
        Log.d("AAA_GRAV", "getFileURI id " + id);

        String filePath = "android.resource://" + mContext.getPackageName() + "/" +
                id;
        Uri uri = Uri.parse(filePath);
        return uri;
    }


    public void playOnce(final VideoView aVideoView, final Uri aUri) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (aVideoView.isPlaying())
                    aVideoView.stopPlayback();

                aVideoView.setVideoURI(aUri);
                aVideoView.start();

            }
        });

    }

    public void playLoop(final VideoView aVideoView, final Uri aUri) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (aVideoView.isPlaying())
                    aVideoView.stopPlayback();

                aVideoView.setVideoURI(aUri);
                MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playOnce(aVideoView, aUri);
                    }
                };

                aVideoView.setOnCompletionListener(onCompletionListener);
                aVideoView.start();

            }
        });

    }

    public void playOnceAndLoop(final VideoView aVideoView, final Uri aUriOnce, final Uri aUriLoop) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {


                playOnce(aVideoView, aUriOnce);
                MediaPlayer.OnCompletionListener onCompletionListenerOnce = new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playLoop(aVideoView, aUriLoop);
                    }
                };
                aVideoView.setOnCompletionListener(onCompletionListenerOnce);

            }
        });






    }

}
