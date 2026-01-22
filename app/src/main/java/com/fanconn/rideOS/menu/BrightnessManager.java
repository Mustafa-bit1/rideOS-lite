package com.fanconn.rideOS.menu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import android.media.RingtoneManager;

import com.fanconn.rideOS.R;

public class BrightnessManager {

    private final Context context;
    private final SharedPreferences prefs;
    private final SharedPreferences volumePrefs;

    private AudioManager audioManager;
    private boolean isMuted = false;

    public BrightnessManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("menu", Context.MODE_PRIVATE);
        this.volumePrefs = context.getSharedPreferences("volume_prefs", Context.MODE_PRIVATE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    // ------------------- BRIGHTNESS ------------------------

    public boolean isAutoBrightnessEnabled() {
        return prefs.getBoolean("auto_brightness_enabled", false);
    }

    public void setAutoBrightnessEnabled(boolean enabled) {
        prefs.edit().putBoolean("auto_brightness_enabled", enabled).apply();
    }

    public int getCurrentBrightness() {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return 128;
        }
    }

    public void applyToSeekBar(SeekBar seekBar, TextView brightnessText) {
        boolean isAuto = isAutoBrightnessEnabled();
        int brightness = getCurrentBrightness();

        seekBar.setProgress(brightness);
        seekBar.setEnabled(!isAuto);

        if (brightnessText != null) {
            if (isAuto) {
                brightnessText.setText("Auto Brightness: ON");
            } else {
                int percent = (int) ((brightness / 255f) * 100);
                if (context.getClass().getSimpleName().equals("QuickSetting")) {
                    brightnessText.setText(percent + "%");
                } else {
                    brightnessText.setText("Adjust overall screen brightness : " + percent + "%");
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (!isAuto && Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);

                    if (brightnessText != null) {
                        int percent = (progress * 100) / seekBar.getMax();
                        if (context.getClass().getSimpleName().equals("QuickSetting")) {
                            brightnessText.setText(percent + "%");
                        } else {
                            brightnessText.setText("Adjust overall screen brightness : " + percent + "%");
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    // ------------------- VOLUME ------------------------

    public void applyToVolumeSlider(SeekBar volumeSlider, ImageButton muteButton) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int savedVolume = volumePrefs.getInt("volume_level", maxVolume / 2);
        isMuted = volumePrefs.getBoolean("is_muted", false);

        volumeSlider.setMax(maxVolume);
        volumeSlider.setProgress(isMuted ? 0 : savedVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, isMuted ? 0 : savedVolume, 0);

        updateMuteIcon(muteButton, isMuted);

        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boolean muted = progress == 0;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                volumePrefs.edit()
                        .putInt("volume_level", progress)
                        .putBoolean("is_muted", muted)
                        .apply();

                updateMuteIcon(muteButton, muted);
                isMuted = muted;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!isMuted) {
                    playFeedbackSound();
                }
            }
        });

        muteButton.setOnClickListener(v -> {
            if (isMuted) {
                int restoreVolume = volumePrefs.getInt("volume_level", maxVolume / 2);
                if (restoreVolume == 0) restoreVolume = maxVolume / 2;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVolume, 0);
                volumeSlider.setProgress(restoreVolume);
                volumePrefs.edit().putBoolean("is_muted", false).apply();
                isMuted = false;
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                volumeSlider.setProgress(0);
                volumePrefs.edit().putBoolean("is_muted", true).apply();
                isMuted = true;
            }
            updateMuteIcon(muteButton, isMuted);
        });
    }

    private void updateMuteIcon(ImageButton muteButton, boolean muted) {
        if (muteButton != null) {
            if (muted) {
                muteButton.setImageResource(R.drawable.mutted); // your muted icon
            } else {
                muteButton.setImageResource(R.drawable.unmutted); // your unmuted icon
            }
        }
    }

    private void playFeedbackSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, soundUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
