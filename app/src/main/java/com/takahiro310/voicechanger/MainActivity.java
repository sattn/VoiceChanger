package com.takahiro310.voicechanger;

import android.Manifest;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.tyorikan.voicerecordingvisualizer.MyRecordingSampler;
import com.tyorikan.voicerecordingvisualizer.VisualizerView;

// @see
// http://sky.geocities.jp/kmaedam/directx9/waveform.html#data

public class MainActivity extends AppCompatActivity implements
        MyRecordingSampler.CalculateVolumeListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_PERMISSION = 100;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {
            Manifest.permission.RECORD_AUDIO
            ,Manifest.permission.MODIFY_AUDIO_SETTINGS
            ,Manifest.permission.READ_EXTERNAL_STORAGE
            ,Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Recording Info
    private MyRecordingSampler mRecordingSampler;

    // View
    private VisualizerView mVisualizerView;
    private FloatingActionButton mFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);

        MediaRecorder mr = new MediaRecorder();
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizer);

        mRecordingSampler = new MyRecordingSampler();
        mRecordingSampler.setVolumeListener(this);
        mRecordingSampler.setSamplingInterval(100);
        mRecordingSampler.link(mVisualizerView);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordingSampler.isRecording()) {
                    mRecordingSampler.stopRecording();
                    mFloatingActionButton.setImageResource(android.R.drawable.ic_btn_speak_now);
                    Toast.makeText(MainActivity.this, R.string.success, Toast.LENGTH_LONG).show();
                } else {
                    try {
                        mRecordingSampler.startRecording();
                        mFloatingActionButton.setImageResource(android.R.drawable.ic_media_pause);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mRecordingSampler.release();
        super.onDestroy();
    }

    @Override
    public void onCalculateVolume(int volume) {
        // NOP
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options_menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.option_menu_record_list:
                // 録音リスト画面へ遷移
                Intent intent = new Intent(MainActivity.this, RecordingListActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
