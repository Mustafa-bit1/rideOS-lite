//package com.fanconn.rideOS;
//
//import android.content.Context;
//import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
//import android.media.CamcorderProfile;
//import android.media.MediaRecorder;
//import android.os.Environment;
//import android.util.Log;
//import android.view.TextureView;
//import android.widget.FrameLayout;
//
//import java.io.File;
//import java.util.List;
//
//@SuppressWarnings("deprecation")
//public class CameraHandler {
//    private static final String TAG = "CameraHandler";
//
//    private Camera frontCamera;
//    private Camera rearCamera;
//
//    private MediaRecorder frontRecorder;
//    private MediaRecorder rearRecorder;
//
//    private final TextureView frontTextureView;
//    private final TextureView rearTextureView;
//    private final FrameLayout frontContainer;
//    private final FrameLayout rearContainer;
//
//    private final Context context;
//
//    public CameraHandler(TextureView frontView,
//                         TextureView rearView,
//                         FrameLayout frontContainer,
//                         FrameLayout rearContainer,
//                         Context context) {
//        this.frontTextureView = frontView;
//        this.rearTextureView = rearView;
//        this.frontContainer = frontContainer;
//        this.rearContainer = rearContainer;
//        this.context = context;
//
//        initSurfaceListener(frontTextureView, true);
//        initSurfaceListener(rearTextureView, false);
//    }
//
//    private void initSurfaceListener(TextureView textureView, boolean isFront) {
//        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
//                Log.d(TAG, (isFront ? "Front" : "Rear") + " surface ready");
//                if (isFront) startFrontPreview(); else startRearPreview();
//            }
//            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {}
//            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                if (isFront) stopFrontPreview(); else stopRearPreview();
//                return true;
//            }
//            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
//        });
//    }
//
//    // ---------- PREVIEW ----------
//    public void startFrontPreview() {
//        stopFrontPreview();
//        int id = findCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
//        if (id == -1) {
//            Log.w(TAG, "No FRONT camera on this device.");
//            frontContainer.setVisibility(FrameLayout.GONE);
//            return;
//        }
//        if (!frontTextureView.isAvailable()) {
//            Log.w(TAG, "Front TextureView not available yet.");
//            return;
//        }
//        try {
//            frontCamera = Camera.open(id);
//            applySafePreviewParams(frontCamera);
//            frontCamera.setPreviewTexture(frontTextureView.getSurfaceTexture());
//            frontCamera.startPreview();
//            frontContainer.setVisibility(FrameLayout.VISIBLE);
//            Log.d(TAG, "✅ Front preview started");
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error starting front preview", e);
//            safeRelease(frontCamera);
//            frontCamera = null;
//        }
//    }
//
//    public void startRearPreview() {
//        stopRearPreview();
//        int id = findCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
//        if (id == -1) {
//            Log.w(TAG, "No REAR camera on this device.");
//            rearContainer.setVisibility(FrameLayout.GONE);
//            return;
//        }
//        if (!rearTextureView.isAvailable()) {
//            Log.w(TAG, "Rear TextureView not available yet.");
//            return;
//        }
//        try {
//            rearCamera = Camera.open(id);
//            applySafePreviewParams(rearCamera);
//            rearCamera.setPreviewTexture(rearTextureView.getSurfaceTexture());
//            rearCamera.startPreview();
//            rearContainer.setVisibility(FrameLayout.VISIBLE);
//            Log.d(TAG, "✅ Rear preview started");
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error starting rear preview", e);
//            safeRelease(rearCamera);
//            rearCamera = null;
//        }
//    }
//
//    public void stopFrontPreview() {
//        if (frontCamera != null) {
//            try { frontCamera.stopPreview(); } catch (Exception ignored) {}
//            safeRelease(frontCamera);
//            frontCamera = null;
//            Log.d(TAG, "Front preview stopped & camera released");
//        }
//        frontContainer.setVisibility(FrameLayout.GONE);
//    }
//
//    public void stopRearPreview() {
//        if (rearCamera != null) {
//            try { rearCamera.stopPreview(); } catch (Exception ignored) {}
//            safeRelease(rearCamera);
//            rearCamera = null;
//            Log.d(TAG, "Rear preview stopped & camera released");
//        }
//        rearContainer.setVisibility(FrameLayout.GONE);
//    }
//
//    private void safeRelease(Camera c) {
//        try { c.release(); } catch (Exception ignored) {}
//    }
//
//    private int findCameraId(int facing) {
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
//            Camera.getCameraInfo(i, info);
//            if (info.facing == facing) return i;
//        }
//        return -1;
//    }
//
//    private void applySafePreviewParams(Camera cam) {
//        try {
//            Camera.Parameters p = cam.getParameters();
//            // Hint the HAL we will record soon (improves start behavior on many devices)
//            p.setRecordingHint(true);
//
//            List<String> focus = p.getSupportedFocusModes();
//            if (focus != null) {
//                if (focus.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
//                    p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//                } else if (focus.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
//                    p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                }
//            }
//
//            // Choose a conservative preview size (doesn't have to match video, but keep it sane)
//            Camera.Size best = choosePreviewSize(p);
//            if (best != null) p.setPreviewSize(best.width, best.height);
//
//            cam.setParameters(p);
//        } catch (Exception e) {
//            Log.w(TAG, "Could not apply preview parameters", e);
//        }
//    }
//
//    private Camera.Size choosePreviewSize(Camera.Parameters p) {
//        List<Camera.Size> sizes = p.getSupportedPreviewSizes();
//        if (sizes == null || sizes.isEmpty()) return null;
//        // Prefer 1280x720, else 640x480, else first
//        Camera.Size pick720 = null, pickVga = null;
//        for (Camera.Size s : sizes) {
//            if (s.width == 1280 && s.height == 720) pick720 = s;
//            if (s.width == 640 && s.height == 480) pickVga = s;
//        }
//        if (pick720 != null) return pick720;
//        if (pickVga != null) return pickVga;
//        return sizes.get(0);
//    }
//
//    // ---------- RECORDING ----------
//    private String getOutputFile(boolean isFront) {
//        File dir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_MOVIES), "ridecams");
//        if (!dir.exists()) dir.mkdirs();
//        return new File(dir, (isFront ? "front_" : "rear_") + System.currentTimeMillis() + ".mp4")
//                .getAbsolutePath();
//    }
//
//    private MediaRecorder buildAndPrepareRecorder(Camera camera, boolean isFront, String path) {
//        if (camera == null) {
//            Log.e(TAG, (isFront ? "Front" : "Rear") + " camera is null.");
//            return null;
//        }
//
//        try {
//            // Stop preview before giving camera to recorder
//            try { camera.stopPreview(); } catch (Exception ignored) {}
//
//            // Get cameraId
//            int cameraId = findCameraId(isFront ? Camera.CameraInfo.CAMERA_FACING_FRONT
//                    : Camera.CameraInfo.CAMERA_FACING_BACK);
//
//            // Pick a profile for the correct camera
//            CamcorderProfile profile;
//            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
//                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
//            } else {
//                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
//            }
//
//            // Align camera params BEFORE unlock
//            alignCameraToProfile(camera, profile);
//
//            // Now unlock
//            camera.unlock();
//            Log.d(TAG, (isFront ? "Front" : "Rear") + " camera unlocked");
//
//            // Setup MediaRecorder
//            MediaRecorder r = new MediaRecorder();
//            r.setCamera(camera);
//            r.setAudioSource(MediaRecorder.AudioSource.MIC);
//            r.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//            r.setProfile(profile);
//            r.setOutputFile(path);
//            r.setOrientationHint(isFront ? 270 : 90);
//
//            r.prepare();
//            Log.d(TAG, (isFront ? "Front" : "Rear") + " MediaRecorder prepared ✅");
//            return r;
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ prepare recorder failed: " + e.getMessage(), e);
//            try { camera.lock(); } catch (Exception ignored) {}
//            return null;
//        }
//    }
//
//
//    private CamcorderProfile selectProfile() {
//        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
//            return CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
//            return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
//        } else {
//            return CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
//        }
//    }
//
//    private void alignCameraToProfile(Camera camera, CamcorderProfile profile) {
//        try {
//            Camera.Parameters p = camera.getParameters();
//            // Choose a supported video size matching the profile if possible
//            List<Camera.Size> videoSizes = p.getSupportedVideoSizes(); // may be null on some devices
//            Camera.Size chosen = null;
//            if (videoSizes != null && !videoSizes.isEmpty()) {
//                for (Camera.Size s : videoSizes) {
//                    if (s.width == profile.videoFrameWidth && s.height == profile.videoFrameHeight) {
//                        chosen = s;
//                        break;
//                    }
//                }
//                if (chosen == null) {
//                    // fallback: pick the first size
//                    chosen = videoSizes.get(0);
//                }
//            }
//            p.setRecordingHint(true);
//            camera.setParameters(p);
//        } catch (Exception e) {
//            Log.w(TAG, "alignCameraToProfile: could not set video size/params", e);
//        }
//    }
//
//    public void startRecording(boolean isFront) {
//        Camera cam = isFront ? frontCamera : rearCamera;
//        String path = getOutputFile(isFront);
//        Log.d(TAG, "Recording output path: " + path);
//
//        MediaRecorder r = buildAndPrepareRecorder(cam, isFront, path);
//        if (r == null) {
//            Log.e(TAG, "Recorder not prepared; abort start.");
//            // Try to restart preview so UI stays alive
//            restartPreviewAfter(isFront);
//            return;
//        }
//
//        try {
//            r.start(); // <- previously failing
//            if (isFront) frontRecorder = r; else rearRecorder = r;
//            Log.i(TAG, (isFront ? "Front" : "Rear") + " recording started ✅ Path: " + path);
//        } catch (RuntimeException startEx) {
//            // Typical “-38” lands here. Clean up gracefully and restart preview.
//            Log.e(TAG, "❌ MediaRecorder.start() failed", startEx);
//            try { r.reset(); } catch (Exception ignored) {}
//            try { r.release(); } catch (Exception ignored) {}
//            if (isFront) frontRecorder = null; else rearRecorder = null;
//            try { cam.lock(); } catch (Exception ignored) {}
//            restartPreviewAfter(isFront);
//        }
//    }
//
//    public void stopRecording(boolean isFront) {
//        MediaRecorder r = isFront ? frontRecorder : rearRecorder;
//        Camera cam = isFront ? frontCamera : rearCamera;
//
//        if (r == null) {
//            Log.w(TAG, "stopRecording: recorder is null (" + (isFront ? "front" : "rear") + ")");
//            restartPreviewAfter(isFront);
//            return;
//        }
//
//        try {
//            r.stop();
//            Log.i(TAG, (isFront ? "Front" : "Rear") + " recording stopped ✅");
//        } catch (RuntimeException stopEx) {
//            Log.e(TAG, "❌ recorder.stop() failed (file may be corrupted)", stopEx);
//        } finally {
//            try { r.reset(); } catch (Exception ignored) {}
//            try { r.release(); } catch (Exception ignored) {}
//            if (isFront) frontRecorder = null; else rearRecorder = null;
//            try { cam.lock(); } catch (Exception ignored) {}
//            restartPreviewAfter(isFront);
//        }
//    }
//
//    private void restartPreviewAfter(boolean isFront) {
//        Camera cam = isFront ? frontCamera : rearCamera;
//        TextureView tv = isFront ? frontTextureView : rearTextureView;
//        if (cam == null || !tv.isAvailable()) return;
//        try {
//            cam.setPreviewTexture(tv.getSurfaceTexture());
//            cam.startPreview();
//            Log.d(TAG, (isFront ? "Front" : "Rear") + " preview restarted");
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to restart preview", e);
//        }
//    }
//}
