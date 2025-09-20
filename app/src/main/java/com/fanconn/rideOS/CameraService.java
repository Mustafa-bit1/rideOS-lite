package com.fanconn.rideOS;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraService extends Service {
    private static final String TAG = "CameraService";
    private static final String CHANNEL_ID = "CameraServiceChannel";
    private static final int TARGET_FPS = 30;
    private static final int SEGMENT_DURATION_MS = 60000;

    private String frontId, rearId;
    private Cam frontCam, rearCam;
    private File outDir;
    private boolean isRecording = false;
    private boolean dualCamSupported = true;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        outDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "DualRecordings");
        outDir.mkdirs();
        findCameraIds();
        checkDualCamSupport();
        createNotificationChannel();
        startForeground(1, buildNotification("Idle"));
        Log.d(TAG, "Service created, cameras found front=" + frontId + " rear=" + rearId +
                " dualCamSupported=" + dualCamSupported);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand action=" + action);

        if ("START_REC".equals(action)) startRecording();
        else if ("STOP_REC".equals(action)) stopRecording();

        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) return;
        isRecording = true;

        executor.execute(() -> {
            if (frontId != null && frontCam == null) frontCam = new Cam(true, frontId);
            if (rearId != null && rearCam == null) rearCam = new Cam(false, rearId);

            if (dualCamSupported) {
                if (frontCam != null) frontCam.startLoopRecording(outDir);
                if (rearCam != null) rearCam.startLoopRecording(outDir);
            } else {
                // Sequential recording loop
                executor.execute(() -> sequentialLoopRecording());
            }
            updateNotification("Recording...");
        });
    }

    private void sequentialLoopRecording() {
        while (isRecording) {
            if (frontCam != null) frontCam.recordSingleSegment(outDir);
            if (!isRecording) break;
            if (rearCam != null) rearCam.recordSingleSegment(outDir);
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;

        executor.execute(() -> {
            if (frontCam != null) {
                frontCam.stop();
                frontCam = null;
            }
            if (rearCam != null) {
                rearCam.stop();
                rearCam = null;
            }
            updateNotification("Idle");
        });
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ===== Notification Helpers =====
    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dual Camera Recording")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Camera Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    // ===== Camera Helpers =====
    private void findCameraIds() {
        try {
            CameraManager mgr = (CameraManager) getSystemService(CAMERA_SERVICE);
            for (String id : mgr.getCameraIdList()) {
                CameraCharacteristics c = mgr.getCameraCharacteristics(id);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) continue;
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) frontId = id;
                if (facing == CameraCharacteristics.LENS_FACING_BACK) rearId = id;
            }
        } catch (Exception e) {
            Log.e(TAG, "findCameraIds error", e);
        }
    }

    private void checkDualCamSupport() {
        try {
            CameraManager mgr = (CameraManager) getSystemService(CAMERA_SERVICE);
            CameraCharacteristics front = mgr.getCameraCharacteristics(frontId);
            CameraCharacteristics rear = mgr.getCameraCharacteristics(rearId);
            StreamConfigurationMap fMap = front.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            StreamConfigurationMap rMap = rear.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size fSize = chooseVideoSize(fMap.getOutputSizes(MediaRecorder.class));
            Size rSize = chooseVideoSize(rMap.getOutputSizes(MediaRecorder.class));

            dualCamSupported = fSize.getWidth() <= 1280 && rSize.getWidth() <= 1280;
        } catch (Exception e) {
            dualCamSupported = false;
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        if (choices == null || choices.length == 0) return new Size(1280, 720);
        return Arrays.stream(choices)
                .filter(s -> s.getWidth() <= 1280)
                .findFirst()
                .orElse(choices[0]);
    }

    private static String ts() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
    }

    // ===== Cam Class =====
    private class Cam {
        private final boolean isFront;
        private final String cameraId;
        private HandlerThread thread;
        private Handler handler;
        private CameraDevice device;
        private CameraCaptureSession session;
        private Size videoSize;
        private MediaRecorder recorder;
        private File baseDir;
        private boolean loopRecording = false;
        private Handler loopHandler;

        Cam(boolean isFront, String cameraId) {
            this.isFront = isFront;
            this.cameraId = cameraId;
            startThread();
        }

        private void startThread() {
            thread = new HandlerThread(isFront ? "FrontCamThread" : "RearCamThread");
            thread.start();
            handler = new Handler(thread.getLooper());
        }

        void startLoopRecording(File base) {
            baseDir = base;
            loopRecording = true;
            loopHandler = new Handler(thread.getLooper());
            recordNextChunk();
        }

        void recordSingleSegment(File base) {
            baseDir = base;
            loopRecording = true;
            loopHandler = new Handler(thread.getLooper());
            recordNextChunk(true);
        }

        private void recordNextChunk() { recordNextChunk(false); }

        private void recordNextChunk(boolean singleSegment) {
            if (!loopRecording) return;
            File outFile = new File(baseDir, (isFront ? "front_" : "rear_") + ts() + ".mp4");
            try {
                setupSizes();
                setupRecorder(outFile);
                reconfigureSession();

                if (singleSegment) {
                    // stop after this chunk
                    loopHandler.postDelayed(this::stopChunk, SEGMENT_DURATION_MS);
                } else {
                    loopHandler.postDelayed(() -> {
                        stopChunk();
                        recordNextChunk();
                    }, SEGMENT_DURATION_MS);
                }
            } catch (Exception e) {
                Log.e(TAG, "recordNextChunk error", e);
            }
        }

        void stop() {
            loopRecording = false;
            if (loopHandler != null) loopHandler.removeCallbacksAndMessages(null);
            stopChunk();
            if (device != null) {
                device.close();
                device = null;
            }
            if (thread != null) {
                thread.quitSafely();
                thread = null;
            }
        }

        private void stopChunk() {
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.reset();
                    recorder.release();
                    recorder = null;
                }
                if (session != null) {
                    session.stopRepeating();
                    session.abortCaptures();
                    session.close();
                    session = null;
                }
            } catch (Exception ignored) {}
        }

        private void setupSizes() {
            if (videoSize != null) return;
            try {
                CameraManager mgr = (CameraManager) getSystemService(CAMERA_SERVICE);
                CameraCharacteristics cc = mgr.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            } catch (Exception e) {
                videoSize = new Size(1280, 720);
            }
        }

        private void setupRecorder(File outFile) throws IOException {
            if (recorder != null) {
                recorder.reset();
                recorder.release();
            }
            recorder = new MediaRecorder();
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(outFile.getAbsolutePath());
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            recorder.setVideoFrameRate(TARGET_FPS);
            recorder.setVideoEncodingBitRate(2_500_000);
            recorder.prepare();
        }

        private void reconfigureSession() throws CameraAccessException {
            CameraManager mgr = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (ActivityCompat.checkSelfPermission(CameraService.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) return;

            mgr.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cam) {
                    device = cam;
                    Surface recSurface = recorder.getSurface();
                    try {
                        device.createCaptureSession(Arrays.asList(recSurface),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession s) {
                                        session = s;
                                        try {
                                            CaptureRequest.Builder b = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                            b.addTarget(recSurface);
                                            b.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                            b.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(TARGET_FPS, TARGET_FPS));
                                            session.setRepeatingRequest(b.build(), null, handler);
                                            recorder.start();
                                        } catch (Exception e) {
                                            Log.e(TAG, "sessionConfig error", e);
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession s) {
                                        Log.e(TAG, "session configure failed");
                                    }
                                }, handler);
                    } catch (Exception e) {
                        Log.e(TAG, "reconfigureSession error", e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cam) {
                    cam.close();
                    device = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cam, int error) {
                    cam.close();
                    device = null;
                }
            }, handler);
        }
    }

    // ===== Static helpers for Activity =====
    public static void startRecording(Context ctx) {
        Intent i = new Intent(ctx, CameraService.class);
        i.setAction("START_REC");
        ctx.startService(i);
    }

    public static void stopRecording(Context ctx) {
        Intent i = new Intent(ctx, CameraService.class);
        i.setAction("STOP_REC");
        ctx.startService(i);
    }
}
