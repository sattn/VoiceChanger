package com.tyorikan.voicerecordingvisualizer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.smp.soundtouchandroid.SoundTouch;
import com.takahiro310.voicechanger.WavFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MyRecordingSampler {

    private static final String TAG = MyRecordingSampler.class.getName();
    private static final int RECORDING_SAMPLE_RATE = 44100;

    private AudioRecord mAudioRecord;
    private boolean mIsRecording;
    private int mBufSize;

    private CalculateVolumeListener mVolumeListener;
    private int mSamplingInterval = 100;
    private Timer mTimer;

    private List<VisualizerView> mVisualizerViews = new ArrayList<>();

    private String mFileName = null;
    private FileOutputStream mFileOutputStream = null;
    private SoundTouch mSoundTouch = null;

    public MyRecordingSampler() {
        initAudioRecord();
    }

    /**
     * link to VisualizerView
     *
     * @param visualizerView {@link VisualizerView}
     */
    public void link(VisualizerView visualizerView) {
        mVisualizerViews.add(visualizerView);
    }

    /**
     * setter of CalculateVolumeListener
     *
     * @param volumeListener CalculateVolumeListener
     */
    public void setVolumeListener(CalculateVolumeListener volumeListener) {
        mVolumeListener = volumeListener;
    }

    /**
     * setter of samplingInterval
     *
     * @param samplingInterval interval volume sampling
     */
    public void setSamplingInterval(int samplingInterval) {
        mSamplingInterval = samplingInterval;
    }

    /**
     * getter isRecording
     *
     * @return true:recording, false:not recording
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    private void initAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mBufSize = bufferSize;
        }

        mSoundTouch = new SoundTouch(
                0
                ,1
                ,RECORDING_SAMPLE_RATE
                ,2
                ,1.5f
                ,3);
    }

    /**
     * start AudioRecord.read
     */
    public void startRecording() throws Exception {

        mFileName = getTempFilename();
        try {
            mFileOutputStream = new FileOutputStream(mFileName);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            throw e;
        }

        mTimer = new Timer();
        mAudioRecord.startRecording();
        mIsRecording = true;
        runRecording();
    }

    /**
     * stop AudioRecord.read
     */
    public void stopRecording() {

        mIsRecording = false;
        mTimer.cancel();

        if (mVisualizerViews != null && !mVisualizerViews.isEmpty()) {
            for (int i = 0; i < mVisualizerViews.size(); i++) {
                mVisualizerViews.get(i).receive(0);
            }
        }

        if (mFileOutputStream != null) {
            try {
                WavFile wavFile = new WavFile();
                wavFile.writeHeader(mFileName);
                mFileOutputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "stopRecording: " + e.getMessage());
            } finally {
                mFileOutputStream = null;
            }
        }
        Log.d(TAG, mFileName);

    }

    private void runRecording() {
        final byte buf[] = new byte[mBufSize];

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // stop recording
                if (!mIsRecording) {
                    mAudioRecord.stop();
                    return;
                }

                mAudioRecord.read(buf, 0, mBufSize);

                mSoundTouch.putBytes(buf);
                int bufferSize = 0;
                do {
                    bufferSize = mSoundTouch.getBytes(buf);
                    if (bufferSize > 0) {
                        try {
                           mFileOutputStream.write(buf, 0, buf.length);
                        } catch (IOException e) {
                            Log.d(TAG, e.getMessage());
                        }
                    }
                } while (bufferSize != 0);
                mSoundTouch.finish();

                int decibel = calculateDecibel(buf);
                if (mVisualizerViews != null && !mVisualizerViews.isEmpty()) {
                    for (int i = 0; i < mVisualizerViews.size(); i++) {
                        mVisualizerViews.get(i).receive(decibel);
                    }
                }

                // callback for return input value
                if (mVolumeListener != null) {
                    mVolumeListener.onCalculateVolume(decibel);
                }
            }
        }, 0, mSamplingInterval);
    }

    private int calculateDecibel(byte[] buf) {
        int sum = 0;
        for (int i = 0; i < mBufSize; i++) {
            sum += Math.abs(buf[i]);
        }
        // avg 10-50
        return sum / mBufSize;
    }

    /**
     * release member object
     */
    public void release() {
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                // NOP
            } finally {
                mFileOutputStream = null;
            }
        }
        mSoundTouch.clearBuffer(0);
        mSoundTouch = null;

        stopRecording();
        mAudioRecord.release();
        mAudioRecord = null;
        mTimer = null;
    }

    public interface CalculateVolumeListener {

        /**
         * calculate input volume
         *
         * @param volume mic-input volume
         */
        void onCalculateVolume(int volume);
    }

    private String getTempFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();
        CharSequence timeCharSequence = DateFormat.format("yyyyMMdd_kkmmss", Calendar.getInstance().getTime());
        String filename = timeCharSequence.toString() + ".wav";
        Log.d(TAG, filename);
        File tempFile = new File(filepath, filename);

        if (tempFile.exists()) {
            // FIXME 消す・・・？？
            tempFile.delete();
        }

        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return tempFile.getAbsolutePath();
    }

}
