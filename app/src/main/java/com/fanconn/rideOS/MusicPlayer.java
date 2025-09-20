//package com.fanconn.rideOS;
//
//import android.content.Context;
//import android.content.res.Resources;
//import android.media.MediaMetadataRetriever;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.Handler;
//import android.util.Log;
//import android.widget.SeekBar;
//import com.fanconn.rideOS.databinding.ActivityMainBinding;
//import java.io.File;
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class MusicPlayer {
//    private MediaPlayer mediaPlayer;
//    private int currentSongIndex = 0;  // 当前播放的歌曲索引
//    public List<String> songResources = new ArrayList<>(); // 歌曲资源列表
//    public List<String> songTitles = new ArrayList<>(); // 歌曲标题列表
//    private float volume = 0.2f; // 设置音量为 20%
//    private int loopState = 0; // 0: 全部循环, 1: 单曲循环, 2: 随机播放
//    private boolean muteSeekbar = false; // 静音状态
//    private Handler mMusicHandler;  // 用于更新进度条的处理器
//    private ActivityMainBinding binding;  // 视图绑定
//    private Context context;  // 上下文对象
//
//    public MusicPlayer(ActivityMainBinding binding, Context context) {
//        this.binding = binding;
//        this.context = context;
//        mMusicHandler = new Handler();
//
//        // 创建音乐目录
//        createMusicDirectory();
//
//        // 加载内置和内部目录的歌曲
//        loadSongsFromRaw();
//        loadSongsFromInternal();
//
//        // 创建并准备 MediaPlayer
//        if (!songResources.isEmpty()) {
//            mediaPlayer = MediaPlayer.create(context, Uri.parse(songResources.get(currentSongIndex)));
//            binding.songTitle.setText(songTitles.get(currentSongIndex));
//
//            // 设置MediaPlayer的音量值
//            mediaPlayer.setVolume(volume, volume);
//            binding.volumeSeekbar.setProgress((int) (volume * 100));
//
//            // 开始播放音乐
//            mediaPlayer.start();
//            binding.playPauseIV.setImageResource(R.drawable.pause3); // 更新按钮图标
//            updateSeekBar();
//        } else {
//            // 处理没有音乐文件的情况
//            Log.e("MusicPlayer", "没有找到音乐文件！");
//        }
//
//        // 音量调节控制条事件监听器
//        binding.volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                volume = (float) (1 - (Math.log(100 - progress) / Math.log(100)));
//                mediaPlayer.setVolume(volume, volume);
//                muteSeekbar = false;
//                binding.volumeIV.setImageResource(R.drawable.volume); // 切换音量图标
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//
//        // 歌曲播放进度控制条事件监听器
//        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    mediaPlayer.seekTo(progress);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//    }
//
//    // 创建音乐目录
//    private void createMusicDirectory() {
//        File musicDir = new File(context.getFilesDir(), "music");
//        if (!musicDir.exists()) {
//            musicDir.mkdirs();
//        }
//    }
//
//    // 加载内置和内部目录的歌曲
//    public void loadSongsFromRaw() {
//        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        Resources resources = context.getResources();
//
//        try {
//            Field[] fields = R.raw.class.getFields();
//            for (Field field : fields) {
//                int resId = field.getInt(null);
//                songResources.add("android.resource://" + context.getPackageName() + "/" + resId);
//                mmr.setDataSource(resources.openRawResourceFd(resId).getFileDescriptor(),
//                        resources.openRawResourceFd(resId).getStartOffset(),
//                        resources.openRawResourceFd(resId).getLength());
//                String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//                songTitles.add((title != null ? title : field.getName()) + " - " + (artist != null ? artist : "未知艺术家"));
//            }
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        Log.d("MusicPlayer", "Raw 目录歌曲: " + songTitles);
//        try {
//            mmr.release();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    // 加载内部目录的歌曲
//    private void loadSongsFromInternal() {
//        File musicDir = new File(context.getFilesDir(), "music");
//        if (musicDir.exists()) {
//            File[] musicFiles = musicDir.listFiles();
//            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//            if (musicFiles != null) {
//                for (File file : musicFiles) {
//                    if (file.isFile()) {
//                        songResources.add(file.getAbsolutePath());
//                        mmr.setDataSource(file.getAbsolutePath());
//                        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//                        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//                        songTitles.add((title != null ? title : file.getName()) + " - " + (artist != null ? artist : "未知艺术家"));
//                    }
//                }
//            }
//            try {
//                mmr.release();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        Log.d("MusicPlayer", "内部音乐目录歌曲: " + songTitles);
//    }
//
//    // 播放或暂停音乐
//    public void playPauseMusic() {
//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.pause();
//                binding.playPauseIV.setImageResource(R.drawable.play); // 切换按钮图标
//            } else {
//                mediaPlayer.start();
//                binding.playPauseIV.setImageResource(R.drawable.pause3); // 切换按钮图标
//            }
//        }
//    }
//
//    // 播放下一首或上一首歌曲
//    public void playSong(int direction, boolean isPrevious) {
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
//
//            if (isPrevious) {
//                // 不考虑 loopState，总是回到上一首
//                currentSongIndex = (currentSongIndex - direction + songResources.size()) % songResources.size();
//            } else {
//                switch (loopState) {
//                    case 0: // 全部循环
//                    case 1: // 单曲循环
//                        currentSongIndex = (currentSongIndex + direction) % songResources.size();
//                        break;
//                    case 2: // 随机播放
//                        Random random = new Random();
//                        int randomIndex;
//                        do {
//                            randomIndex = random.nextInt(songResources.size());
//                        } while (randomIndex == currentSongIndex);
//                        currentSongIndex = randomIndex;
//                        break;
//                }
//            }
//
//            String songResource = songResources.get(currentSongIndex);
//            mediaPlayer = MediaPlayer.create(context, Uri.parse(songResource));
//            binding.songTitle.setText(songTitles.get(currentSongIndex));
//            mediaPlayer.setVolume(volume, volume);
//            mediaPlayer.start();
//            binding.playPauseIV.setImageResource(R.drawable.pause3);
//            updateSeekBar();
//        }
//    }
//
//    // 播放下一首歌曲
//    public void playNextSong() {
//        playSong(1, false);
//    }
//
//    // 播放上一首歌曲
//    public void playPreviousSong() {
//        playSong(-1, true);
//    }
//
//    // 更新进度条
//    private void updateSeekBar() {
//        binding.seekbar.setMax(mediaPlayer.getDuration());
//        mMusicHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//                    int mCurrentPosition = mediaPlayer.getCurrentPosition();
//                    binding.seekbar.setProgress(mCurrentPosition);
//                    updateTimer(mCurrentPosition);
//                    updateTotalTime(mediaPlayer.getDuration());
//                    if (mCurrentPosition > mediaPlayer.getDuration() - 1000) {
//                        switchSongOnCompletion();
//                    }
//                }
//                mMusicHandler.postDelayed(this, 1000);
//            }
//        }, 1000);
//    }
//
//    private void updateTimer(int currentPosition) {
//        int minutes = (currentPosition / 1000) / 60;
//        int seconds = (currentPosition / 1000) % 60;
//        String time = String.format("%02d:%02d", minutes, seconds);
//        binding.currentTime.setText(time);
//    }
//
//    private void updateTotalTime(int duration) {
//        int minutes = (duration / 1000) / 60;
//        int seconds = (duration / 1000) % 60;
//        String time = String.format("%02d:%02d", minutes, seconds);
//        binding.totalTime.setText(time);
//    }
//
//    public void changeSong() {
//        Log.d("MediaPlayer", "Changing to next song: " + songTitles.get(currentSongIndex));
//        mediaPlayer.reset();
//        mediaPlayer = MediaPlayer.create(context, Uri.parse(songResources.get(currentSongIndex)));
//        binding.songTitle.setText(songTitles.get(currentSongIndex));
//        setVolume();
//        binding.playPauseIV.setImageResource(R.drawable.pause3); // 切换图标
//        mediaPlayer.start();
//        updateSeekBar();
//    }
//
//    private void setVolume() {
//        if (muteSeekbar) {
//            mediaPlayer.setVolume(0.0f, 0.0f);
//        } else {
//            mediaPlayer.setVolume(volume, volume);
//        }
//    }
//
//    // 切换静音状态
//    public void toggleMute() {
//        muteSeekbar = !muteSeekbar;
//        setVolume();
//        binding.volumeIV.setImageResource(muteSeekbar ? R.drawable.mute : R.drawable.volume); // 切换音量图标
//    }
//
//    public void updateLoopButtonImage(int state) {
//        loopState = state;
//        switch (state) {
//            case 0:
//                binding.loopIV.setImageResource(R.drawable.sequential);
//                mediaPlayer.setLooping(true);
//                break;
//            case 1:
//                binding.loopIV.setImageResource(R.drawable.single);
//                mediaPlayer.setLooping(true);
//                break;
//            case 2:
//                binding.loopIV.setImageResource(R.drawable.random);
//                mediaPlayer.setLooping(false);
//                break;
//        }
//    }
//
//    private void switchSongOnCompletion() {
//        Log.d("MusicPlayer", "Song completed, switching to next song");
//        switch (loopState) {
//            case 0:
//                currentSongIndex = (currentSongIndex + 1) % songResources.size();
//                break;
//            case 1:
//                currentSongIndex = currentSongIndex % songResources.size();
//                break;
//            case 2:
//                Random random = new Random();
//                int randomIndex;
//                do {
//                    randomIndex = random.nextInt(songResources.size());
//                } while (randomIndex == currentSongIndex);
//                currentSongIndex = randomIndex;
//                break;
//        }
//        changeSong();
//    }
//
//    public void onDestroy() {
//        // 释放音频播放资源
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//    }
//}
