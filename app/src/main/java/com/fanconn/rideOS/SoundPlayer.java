package com.fanconn.rideOS;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundPlayer {

    private static MediaPlayer mediaPlayer;

    public static void playOnce(Context context, int soundResId) {
        stop(); // Stop any currently playing sound

        if (context == null) {
            Log.e("SoundPlayer", "❌ Context is null, cannot play sound.");
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), soundResId);

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                    Log.d("SoundPlayer", "✅ Sound completed and released.");
                });

                mediaPlayer.start();
                //Log.d("SoundPlayer", "🔊 Sound started.");
            } else {
                //Log.e("SoundPlayer", "❌ MediaPlayer creation failed.");
            }

        } catch (Exception e) {
            //Log.e("SoundPlayer", "❌ Exception in playOnce: " + e.getMessage(), e);
        }
    }

    public static void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.d("SoundPlayer", "⏹️ Sound stopped.");
                }
            } catch (IllegalStateException e) {
                Log.e("SoundPlayer", "❌ IllegalStateException while stopping: " + e.getMessage());
            } finally {
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d("SoundPlayer", "🧹 MediaPlayer released.");
            }
        }
    }
}
