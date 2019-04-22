package com.sscctv.seeeyes.video;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.sscctv.seeeyesmonitor.MainActivity;
import com.sscctv.seeeyesmonitor.R;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by trlim on 2015. 12. 17..
 * <p>
 * 카메라 입력 처리 등의 공통 작업을 맡는다
 */

// Camera API는 camera2로 대체되었으나 API 17에서는 쓸 수가 없으므로 경고를 잠재운다
@SuppressWarnings("deprecation")
public abstract class CameraInput extends VideoInput {
    //        implements Camera.PreviewCallback {
    private static final String TAG = "CameraInput";
    private static int re_width, re_height;

    private Camera _camera;
    private MediaRecorder _recorder;
    private String _outputPath;
    private String veryLongString;
    private Context mContext;
    private int width, height;
    private byte[] camData;
    private TimerTask mTask;
    private Timer mTimer;


    CameraInput(int input, SurfaceView surfaceView, Listener listener) {
        super(input, surfaceView, listener);
    }


//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        camera = getCamera();
//        Camera.Parameters parameters = camera.getParameters();
//
//        width = parameters.getPreviewSize().width;
//        height = parameters.getPreviewSize().height;
//
//
//        Log.d(TAG, "Data: " + Arrays.toString(data));
//        Log.d(TAG, "RGB: " + Arrays.toString(decodeYUV420SP(data, width, height)));
////        Log.d(TAG, "Byte? " + data.length);
//
//    }

//    public static class Log {
//
//        public static void d(String TAG, String message) {
//            int maxLogSize = 2000;
//            for (int i = 0; i <= message.length() / maxLogSize; i++) {
//                int start = i * maxLogSize;
//                int end = (i + 1) * maxLogSize;
//                end = end > message.length() ? message.length() : end;
//                android.util.Log.d(TAG, message.substring(start, end));
//            }
//        }
//
//    }

    public int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        int rgb[] = new int[width * height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                        0xff00) | ((b >> 10) & 0xff);


            }
        }
        return rgb;
    }

    public static void encodeYUV420SP(byte[] yuv420sp, int[] rgba, int width, int height) {
//        Log.d(TAG, "encodeYUV420SP: " + Arrays.toString(yuv420sp));
        final int frameSize = width * height;

        int[] U, V;
        U = new int[frameSize];
        V = new int[frameSize];

        final int uvwidth = width / 2;

        int r = 0, g = 0, b = 0, y = 0, u = 0, v = 0;
        for (int j = 0; j < height; j++) {
            int index = width * j;
            for (int i = 0; i < width; i++) {

                r = Color.red(rgba[index]);
                g = Color.green(rgba[index]);
                b = Color.blue(rgba[index]);

                // rgb to yuv
                y = (66 * r + 129 * g + 25 * b + 128) >> 8 + 16;
                u = (-38 * r - 74 * g + 112 * b + 128) >> 8 + 128;
                v = (112 * r - 94 * g - 18 * b + 128) >> 8 + 128;

                // clip y
                yuv420sp[index] = (byte) ((y < 0) ? 0 : ((y > 255) ? 255 : y));
                U[index] = u;
                V[index++] = v;
//                Log.d(TAG, "width: " + i);
                if (j == 750 && i == 1200) {
                    Log.d(TAG, "R: " + r + " G: " + g + " B: " + b + " Y: " + y + " U: " + u + " V: " + v);

                }
            }

//            int[] data = {r, g, b, y, u, v};
//            ByteBuffer buffer = ByteBuffer.allocate(1100);
//            buffer.put(j, data);
//            Log.d(TAG, "Height: " + j);
//            Log.d(TAG, "R: " + r + " G: " + g + " B: " + b + " Y: " + y + " U: " + u + " V: " + v);
        }

    }

    protected int getCameraId() {
        return getInput();
    }

    private Camera getCamera() {
        return _camera;
    }

    /**
     * 음성을 녹음해야 할지를 알려준다.
     *
     * @return true면 녹음하고 false이면 녹음하지 않음
     */
    protected boolean hasAudio() {
        return false;
    }

    @Override
    public void start(Bundle args) {
//        Log.d(TAG, "Camera Start");
//        mTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (camData != null) {
//                    encodeYUV420SP(camData, decodeYUV420SP(camData, width, height), width, height);
//                }
////                Log.d(TAG, "Count: " );
//            }
//        };
//        mTimer = new Timer();
//        mTimer.schedule(mTask, 1000, 10000);
        //startCameraInput();
    }


    @Override
    public void stop() {
        stopCameraInput();
    }

    @Override
    public void restart() {
        super.restart();

        restartPreview();
    }


    //@Override
    //public void surfaceCreated(SurfaceHolder holder) {
    //    Log.i(TAG, "surfaceCreated");
    // The Surface has been created, acquire the camera and tell it where to draw.
    //startPreview();
    //}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        Log.i(TAG, "Surface Changed  -  Format: " + format + " Width: " + width + " Height: " + height);
//        Log.d(TAG, "surfaceChanged Start");

        if (getSurfaceHolder().getSurface() == null) {
            // preview surface does not exist
            return;
        }
//        stopPreview();
//        _camera.setParameters(parameters);

//        stop preview before making changes
//        stopPreview();

//        set preview size and make any resize, rotate or
//        reformatting changes here
//        start preview with new settings
        startPreview();
    }

    public void startCameraInput() {
//        Log.d(TAG, "_Camera: " + _camera);
        if (_camera != null) {
            //throw new IllegalStateException("Camera is already started");
//            Log.e(TAG, "Camera is already started!!! - " + _camera);
            //return;
            stopPreview();
            stopCameraInput();
        }

        int numCameras = Camera.getNumberOfCameras();
//        Log.d(TAG, "numCameras: " + numCameras);

        if (numCameras > 0) {
            if (getCameraId() < numCameras) {
//                Log.d(TAG, "startCameraInput() " + _camera + " , " + getCameraId());
                try {
                    _camera = Camera.open(getCameraId());

                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    _camera.release();
                    return;
                }
//                Log.d(TAG, "getParamsWidth: " + _camera.getParameters().getPreviewSize().width + " getParamsHeight: " + _camera.getParameters().getPreviewSize().height);
//                Log.d(TAG, "Test: " + _camera.getParameters().getPreviewFormat());
                // get Camera parameters
//                Camera.Parameters params = _camera.getParameters();
//                for (int[] range : params.getSupportedPreviewFpsRange()) {
//                    Log.i(TAG, "Camera Fps Range: " + range[0] + ", " + range[1]);
//                }

//                params.setPreviewFpsRange(5000, 5000);
//                params.setRecordingHint(false);

//                _camera.setParameters(params);
//                params.setPreviewFormat(ImageFormat.YUV_420_888);

//                params.setPreviewFormat(ImageFormat.RGB_565);
//                _camera.setPreviewCallback(this);

            } else {
                Log.d(TAG, "No camera for input - " + getCameraId());
            }
        } else {
            Log.d(TAG, "No camera!");
        }

        _camera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                android.util.Log.e(TAG, "onError: " + error + "  " + camera);
                if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                    ((MainActivity) MainActivity.mContext).showRecordErrorDialog();
                } else if (error == Camera.CAMERA_ERROR_UNKNOWN) {
                    Log.d(TAG, "Camera setErrorCallback : Camera_ERROR_UNKNOWN");
                }

            }
        });

    }

    public void stopCameraInput() {
        if (_camera != null) {
//            Log.i(TAG, "stopCameraInput()");
            // 9. Call stopPreview() to stop updating the preview surface.
            //_camera.stopPreview();

            // 10. Important: Call release() to release the camera for use by other applications.
            // Applications should release the camera immediately in onPause() (and re-open() it in onResume()).
            _camera.release();
            _camera = null;
        }
    }

    public void startPreview() {
//        Log.i(TAG, "StartPreview");
        try {
            //Camera camera = getCamera();

            if (_camera != null) {
                // 5. Important: Pass a fully initialized SurfaceHolder to setPreviewDisplay(SurfaceHolder).
                // Without a surface, the camera will be unable to start the preview.

                _camera.setPreviewDisplay(getSurfaceHolder());

                // 6. Important: Call startPreview() to start updating the preview surface.
                // Preview must be started before you can take a picture.
                _camera.startPreview();


            }
        } catch (IOException | RuntimeException exception) {
//            Log.e(TAG, "Error setting camera preview", exception);
        }
    }

    public void stopPreview() {
        try {
            //Camera camera = getCamera();

            if (_camera != null) {
                _camera.stopPreview();
            }
        } catch (Exception e) {
//            Log.e(TAG, "Error stopping camera preview", e);
        }
    }

    // HDMI 입력의 경우 비디오 입력이 바뀌면 restartPreview()만으로는 대응이 안되므로 중지했다 다시 시작할 수 있게한다.
    //protected void stopAndStartPreview() {
    //    stopPreview();
    //    startPreview();
    //}

    private void restartPreview() {
        getCamera().startPreview();
    }

    @Override
    public void takeSnapshot(SnapshotCallback callback) {
        Camera camera = getCamera();

        final SnapshotCallback cb = callback;
        final VideoInput self = this;

        camera.takePicture(cb::onShutter, (data, camera1)
                -> cb.onSnapshotTaken(SnapshotCallback.SNAPSHOT_RAW, data, self), (data, camera13)
                -> cb.onSnapshotTaken(SnapshotCallback.SNAPSHOT_POSTVIEW, data, self), (data, camera12)
                -> {
            cb.onSnapshotTaken(SnapshotCallback.SNAPSHOT_JPEG, data, self);
            restartPreview();
        });
    }

    @Override
    public boolean startRecording(String path) {
//        Log.d(TAG, "CameraInput startRecording");

        if (!prepareVideoRecorder(path, hasAudio())) {

            _outputPath = null;
            return false;
        }
//        Log.d(TAG, "CameraInput startRecording1");

        _outputPath = path;

        _recorder.start();
//        Log.d(TAG, "CameraInput startRecording2");

        return true;
    }

    @Override
    public String stopRecording(boolean stat) {
        String path = null;

        if (_recorder != null) {
            try {
                _recorder.stop();
            } catch (RuntimeException stopException) {
                ((MainActivity) MainActivity.mContext).showRecordErrorDialog();
//                ((MainActivity) MainActivity.mContext).showToast(R.string.stop_error);
                ((MainActivity) MainActivity.mContext).deleteMediaToLibrary(_outputPath);
            }
            Log.d(TAG, "stopRecording stat: " + stat);
            releaseMediaRecorder();
            if (stat) stopCameraInput();

            path = _outputPath;
            _outputPath = null;
        }

        return path;
    }

    public boolean isRecording() {
        return _recorder != null;
    }

    private boolean prepareVideoRecorder(String path, boolean hasAudio) {
        Camera camera = getCamera();
        Camera.Parameters parameters = camera.getParameters();
        CamcorderProfile profile = getCamcorderProfile();
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

//        Log.w(TAG, "getMdinWidth: " + getMdinWidth() + " getMdinHeight: " + getMdinHeight() + " getMdinRate() : " + getMdinRate());
        profile.videoFrameWidth = getMdinWidth();
        profile.videoFrameHeight = getMdinHeight();
//        profile.videoFrameRate = getMdinRate();
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        parameters.setPreviewFrameRate(profile.videoFrameRate);
        parameters.setPreviewFpsRange(15000, 30000);
        camera.setParameters(parameters);

//        Log.d(TAG, "CameraInput prepareVideoRecorder");

//        camera.setErrorCallback(new Camera.ErrorCallback() {
//            @Override
//            public void onError(int error, Camera camera) {
////                android.util.Log.e(TAG, "Recording setErrorCallback: " + error + "  " + camera);
//                ((MainActivity) MainActivity.mContext).stopRecording();
//                ((MainActivity) MainActivity.mContext).showRecordErrorDialog();
//
//            }
//        });


        MediaRecorder recorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        try {
            camera.unlock();
//            Log.d(TAG, "CameraInput prepareVideoRecorder1");

        } catch (final Exception ex) {
            ex.printStackTrace();
//            Log.e(TAG, "CameraInput Preparing: " + ex.getMessage());
//            recorder.release();
            return false;
        }
        recorder.setCamera(camera);

        // Step 2: Set sources
        if (hasAudio) {
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        }
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        recorder.setOutputFormat(profile.fileFormat);
//        recorder.setVideoFrameRate(profile.videoFrameRate);
        recorder.setVideoFrameRate(getMdinRate());
        recorder.setVideoEncodingBitRate(profile.videoBitRate);
        recorder.setVideoEncoder(profile.videoCodec);
        recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        Log.d(TAG, "------------Video: " + profile.videoCodec);
//        Log.d(TAG, "------------fileFormat: " + profile.fileFormat);

//        Log.d(TAG, "getVideoFrameRate: " + profile.videoFrameRate);
//        if ((getInput() == VIDEO_INPUT_CVI) || (getInput() == VIDEO_INPUT_CVBS))
//        if((getInput() == VIDEO_INPUT_CVI) || (getInput() == VIDEO_INPUT_CVBS))
//            recorder.setVideoSize(profile.videoFrameWidth, getHeight());
//        else
//        recorder.setVideoSize(1920, 1080);
//        Log.d(TAG, "getVideoFrameRate: " + profile.videoBitRate);

//        noinspection StatementWithEmptyBody
        if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
                profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
//             Nothing needs to be done. Call to setCaptureRate() enables
//             time lapse video recording.
        } else if (hasAudio) {
//            Log.d(TAG, "recorder Audio: " + profile.audioChannels);
            recorder.setAudioEncodingBitRate(profile.audioBitRate);
            recorder.setAudioChannels(profile.audioChannels);
            recorder.setAudioSamplingRate(profile.audioSampleRate);
            recorder.setAudioEncoder(profile.audioCodec);
//            Log.d(TAG, "------------Auido: " + profile.audioCodec);
        }
//        recorder.setProfile(profile);
//        recorder.setMaxFileSize(3500000000L);
//        recorder.setMaxDuration(10000);

        // Step 4: Set output file
        recorder.setOutputFile(path);
        // Step 5: Set the preview output
        recorder.setPreviewDisplay(getSurfaceHolder().getSurface());

        _recorder = recorder;

        _recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                Log.e(TAG, "recording error: " + i + " , " + i1);
            }
        });
//        _recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
//            @Override
//            public void onInfo(MediaRecorder mr, int what, int extra) {
//                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
//                    ((MainActivity) MainActivity.mContext).stopRecording();
//                    ((MainActivity) MainActivity.mContext).showToast(R.string.maxSize);
//
//                }
////                android.util.Log.e(TAG, "onInfo: " + what + "  " + extra);
//            }
//        });
        Log.i(TAG, "Recorder Configure. " +"FileFormat: "+ getCamcorderProfile().fileFormat + " FrameRate: " + getCamcorderProfile().videoFrameRate
        + " EncodingBitRate: " + getCamcorderProfile().videoBitRate + " VideoEncoder: " + getCamcorderProfile().videoCodec + " Quality: " + getCamcorderProfile().quality);
//        Log.d(TAG, "Recording Width: " + profile.videoFrameWidth + " Height: " + profile.videoFrameHeight + " Rate: " + profile.videoFrameRate);
//        Log.d(TAG, "CameraInput prepareVideoRecorder2");
        // Step 6: Prepare configured MediaRecorder
        try {
            recorder.prepare();
//            recorder.setMaxFileSize(50);
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
//            stopRecording(false);
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


//    private boolean prepareVideoRecorder(String path, boolean hasAudio) {
//        Camera camera = getCamera();
//        MediaRecorder recorder = new MediaRecorder();
//
//        // Step 1: Unlock and set camera to MediaRecorder
//        try {
//            camera.unlock();
//        } catch (RuntimeException e) {
//            android.util.Log.e(TAG, "RuntimeException unlocking camera: " + e.getMessage());
//            return false;
//        }
//        recorder.setCamera(camera);
//
//        // Step 2: Set sources
//        if (hasAudio) {
//            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//        }
//        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        // 음성 녹음을 선택할 수 있게 하기 위해 MediaRecorder.setProfile()의 코드를 가져와 수정함.
//        CamcorderProfile profile = getCamcorderProfile();
//        recorder.setOutputFormat(profile.fileFormat);
//        recorder.setVideoFrameRate(profile.videoFrameRate);
////    if((getInput() == VIDEO_INPUT_CVI) || (getInput() == VIDEO_INPUT_CVBS))
//        recorder.setVideoSize(1920, 1080);
////        //if((getInput() == VIDEO_INPUT_CVI) || (getInput() == VIDEO_INPUT_CVBS))
////        recorder.setVideoSize(profile.videoFrameWidth, getHeight());
////    else
//        recorder.setVideoSize(getWidth(), getHeight());
//        recorder.setVideoEncodingBitRate(profile.videoBitRate);
//        recorder.setVideoEncoder(profile.videoCodec);
//        //noinspection StatementWithEmptyBody
//        if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
//                profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
//            // Nothing needs to be done. Call to setCaptureRate() enables
//            // time lapse video recording.
//        } else if (hasAudio) {
//            recorder.setAudioEncodingBitRate(profile.audioBitRate);
//            recorder.setAudioChannels(profile.audioChannels);
//            recorder.setAudioSamplingRate(profile.audioSampleRate);
//            recorder.setAudioEncoder(profile.audioCodec);
//        }
//
//        // Step 4: Set output file
//        recorder.setOutputFile(path);
//
//        // Step 5: Set the preview output
//        recorder.setPreviewDisplay(getSurfaceHolder().getSurface());
//
//        _recorder = recorder;
//
//        // Step 6: Prepare configured MediaRecorder
//        try {
//            recorder.prepare();
//        } catch (IllegalStateException e) {
//            android.util.Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        } catch (IOException e) {
//            android.util.Log.e(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        }
//        return true;
//    }

    private void releaseMediaRecorder() {
        _recorder.reset();   // clear recorder configuration
        _recorder.release(); // release the recorder object
        _recorder = null;
        try {
            getCamera().reconnect();  // lock camera for later use
        } catch (IOException e) {
            Log.e(TAG, "IOException reconnecting camera: " + e.getMessage());
        }
    }

//    public static void getMode(boolean mode){
//
//        if(mode) {
//            re_width = 1280;
//            re_height = 720;
//            Log.d(TAG, "720P MODE");
//        } else {
//            re_width = 1920;
//            re_height = 1080;
//            Log.d(TAG, "1080P MODE");
//        }
//    }

    private int getMdinWidth() {
        int width = 0;
        try {
            FileInputStream file = new FileInputStream("/sys/class/vdp/mdin400/width");
            byte[] value = new byte[16];
            int length = file.read(value);
            if (length > 0) {
                width = Integer.parseInt(new String(value).substring(0, length));
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return width;
    }

    private int getMdinHeight() {
        int height = 0;
        try {
            FileInputStream file = new FileInputStream("/sys/class/vdp/mdin400/height");
            byte[] value = new byte[16];
            int length = file.read(value);
            if (length > 0) {
                height = Integer.parseInt(new String(value).substring(0, length));
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return height;
    }

    private int getMdinRate() {
        int rate = 0;
        try {
            FileInputStream file = new FileInputStream("/sys/class/vdp/mdin400/rate");
            byte[] value = new byte[16];
            int length = file.read(value);
            if (length > 0) {
                rate = Integer.parseInt(new String(value).substring(0, length));
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rate;
    }


    public abstract int getWidth();

    public abstract int getHeight();

    public abstract float getRate();

    protected CamcorderProfile getCamcorderProfile() {
        int width = getMdinWidth();
        int height = getMdinHeight();
//        Log.d(TAG, "Width: " + width + " Height: " + height);
        int quality = CamcorderProfile.QUALITY_480P;
        if ((width > 1280) && (height > 720)) {
            quality = CamcorderProfile.QUALITY_1080P;
        } else if ((width > 720) && (height > 480)) {
            quality = CamcorderProfile.QUALITY_720P;
        }
        return CamcorderProfile.get(getCameraId(), quality);
    }


}
