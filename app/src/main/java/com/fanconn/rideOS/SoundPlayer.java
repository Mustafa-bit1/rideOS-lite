package com.fanconn.rideOS;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

public class SoundPlayer {

    private static SoundPool soundPool;
    private static final Map<Integer, Integer> loadedSounds = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize once from Application or first Activity.
     */
    public static synchronized void initialize(Context context) {

        if (initialized)
            return;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build();

        initialized = true;
    }

    /**
     * Backward compatible.
     * Existing calls need NO changes.
     */
    public static void playOnce(Context context, int soundResId) {

        if (context == null)
            return;

        if (!initialized) {
            initialize(context.getApplicationContext());
        }

        Integer soundId = loadedSounds.get(soundResId);

        if (soundId != null) {

            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            float currentVolume =
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            float maxVolume =
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            float volume =
                    maxVolume == 0 ? 0f : currentVolume / maxVolume;

            soundPool.play(soundId, volume, volume, 1, 0, 1f);
            return;
        }

        int id = soundPool.load(
                context.getApplicationContext(),
                soundResId,
                1
        );

        loadedSounds.put(soundResId, id);

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {

            if (status == 0 && sampleId == id) {

                AudioManager audioManager =
                        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                float currentVolume =
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                float maxVolume =
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                float volume =
                        maxVolume == 0 ? 0f : currentVolume / maxVolume;

                pool.play(sampleId, volume, volume, 1, 0, 1f);
            }
        });
    }

    /**
     * Stop all currently playing sounds.
     */
    public static void stop() {

        if (soundPool != null) {
            soundPool.autoPause();
        }
    }

    /**
     * Release when app exits.
     */
    public static void release() {

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        loadedSounds.clear();
        initialized = false;
    }
}