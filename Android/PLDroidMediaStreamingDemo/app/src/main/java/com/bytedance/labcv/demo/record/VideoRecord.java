package com.bytedance.labcv.demo.record;

import android.hardware.Camera;
import android.media.MediaRecorder;

import com.bytedance.labcv.demo.CameraDevice;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VideoRecord implements IVideoRecord {
    public static final int FRAME_RATE = 30;
    public static final int BIT_RATE = 64 * 1024 * 1024;
    public static final int ORIENTATION_FRONT = 270;
    public static final int ORIENTATION_BACK = 90;
    public static final int ORIENTATION_TV = 0;
    public static final int MAX_DURATION = 60 * 60 * 1000;

    private MediaRecorder mMediaRecorder;
    private Executor mExecutor;

    public VideoRecord() {
        mMediaRecorder = new MediaRecorder();
        mExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void start(final String videoPath, final boolean isTV) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final MediaRecorder recorder = mMediaRecorder;
                Camera camera = CameraDevice.get().getCamera();
                if (camera == null || recorder == null) {
                    return;
                }
                int[] size = getSize(camera);
                camera.unlock();

                recorder.setCamera(camera);
                recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

                recorder.setVideoSize(size[0], size[1]);
                recorder.setVideoFrameRate(FRAME_RATE);
                recorder.setVideoEncodingBitRate(BIT_RATE);
                recorder.setOrientationHint(getOrientation(isTV));
                recorder.setMaxDuration(MAX_DURATION);
                recorder.setPreviewDisplay(null);
                recorder.setOutputFile(videoPath);
                try {
                    recorder.prepare();
                    recorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                MediaRecorder recorder = mMediaRecorder;
                if (recorder != null) {
                    recorder.stop();
                    recorder.reset();
                }
            }
        });
    }

    @Override
    public void release() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private int getOrientation(boolean isTV) {
        if (isTV) {
            return ORIENTATION_TV;
        } else if (CameraDevice.get().isFront()) {
            return ORIENTATION_FRONT;
        } else {
            return ORIENTATION_BACK;
        }
    }

    private int[] getSize(Camera camera) {
        List<Camera.Size> sizes = camera.getParameters().getSupportedVideoSizes();
        if (sizes.size() > 0) {
            Camera.Size s = sizes.get(0);
            return new int[] {s.width, s.height};
        } else {
            throw new RuntimeException("no available video size");
        }
    }
}
