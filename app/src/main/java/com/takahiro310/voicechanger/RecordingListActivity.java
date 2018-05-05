package com.takahiro310.voicechanger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.takahiro310.voicechanger.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordingListActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private static final String TAG = RecordingListActivity.class.getName();
    private RecordAdapter adapter = null;
    private static String mFileName = null;
    private boolean mStartPlaying = false;
    private MediaPlayer mPlayer = null;
    private SeekBar mSeekBar = null;
    private FloatingActionButton mFloatingActionButton = null;
    private AtomicInteger mStatusPlayer = new AtomicInteger();
    public static final int STATUS_PALYER_NO_START = 0;
    public static final int STATUS_PALYER_PREPARED = 1;
    public static final int STATUS_PALYER_PLAYED = 2;
    private static final int STATUS_PALYER_STOP = 3;
    private ScheduledExecutorService scheduledEx;
    private static final int THREAD_RUNNING_INTERVAL = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // 録音リストのリストビューを生成
        List<Map<String, String>> items = getRecordItems();
        adapter = new RecordAdapter(this.getApplicationContext(),
                R.layout.content_recording_list, items);
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view,
                                    int position, long id) {
                RecordAdapter.ViewHolder viewHolder = (RecordAdapter.ViewHolder) view.getTag();
                String filename = viewHolder.filename.getText().toString();
                mFileName = filename.toString();
                mStatusPlayer.set(STATUS_PALYER_PREPARED);

                TextView textView = findViewById(R.id.textView);
                textView.setText(filename);
            }
        });
        registerForContextMenu(listView);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFileName == null) {
                    // 再生ファイルが選択されていない
                    return;
                }
                if (mStartPlaying) {
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });

        mStatusPlayer.set(STATUS_PALYER_NO_START);
        scheduledEx =  Executors.newSingleThreadScheduledExecutor();
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (null != mPlayer) {
                    if (!mPlayer.isPlaying()) {
                        mPlayer.seekTo(i);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (null != mPlayer) {
                    mPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (null != mPlayer) {
                    mPlayer.start();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        boolean result = true;

        switch (id) {
            case android.R.id.home:
                finish();
                break;
            default:
                result = super.onOptionsItemSelected(item);
        }

        return result;
    }

    private List<Map<String, String>> getRecordItems() {

        // FIXME 再生時間も欲しい
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        String sdPath = Environment.getExternalStorageDirectory().getPath();

        File[] files = new File(sdPath).listFiles();
        if (files == null) {
            return items;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile() && files[i].getName().endsWith(".wav")) {
                Map<String, String> e = new HashMap<String, String>();
                e.put("filename", files[i].getName());
                long size = files[i].length();
                e.put("filesize", getSizeStr(size));
                CharSequence lastModified = DateFormat.format("yyyy/MM/dd, E, kk:mm:ss", files[i].lastModified());
                e.put("created_at", lastModified.toString());
                items.add(e);
            }
        }

        return items;
    }

    private String getSizeStr(long size) {
        if (1024 > size) {
            return size + " Byte";
        } else if (1024 * 1024 > size) {
            double dsize = size;
            dsize = dsize / 1024;
            long value = Math.round(dsize);
            return value + " KByte";
        } else {
            double dsize = size;
            dsize = dsize / 1024 / 1024;
            long value = Math.round(dsize);
            return value + " MB";
        }
    }

    private void startPlaying() {

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/" + mFileName);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(this);
            mSeekBar.setMax(mPlayer.getDuration());
            mFloatingActionButton.setImageResource(android.R.drawable.ic_media_pause);
            mStartPlaying = true;

            if(mStatusPlayer.get() == STATUS_PALYER_PREPARED){
                scheduledEx.scheduleAtFixedRate(new SeekRunner("seek task"), 0, THREAD_RUNNING_INTERVAL, TimeUnit.MILLISECONDS);
                mStatusPlayer.set(STATUS_PALYER_PLAYED);
            }

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void stopPlaying() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mFloatingActionButton.setImageResource(android.R.drawable.ic_media_play);
        mStartPlaying = false;
        mSeekBar.setProgress(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlaying();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_context_menu_list, menu);

        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        Map<String, String> map = (Map<String, String>) adapter.getItem(info.position);
        menu.setHeaderTitle(map.get("filename"));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //長押しされたビューに関する情報が格納されたオブジェクトを取得。
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //長押しされたリストのポジションを取得。
        int listPosition = info.position;

        Map<String, String> map = (Map<String, String>) adapter.getItem(listPosition);
        String filename = map.get("filename");

        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.context_menu_rename:
                FileRenameDialogFragment renameDialog = new FileRenameDialogFragment();
                renameDialog.setFilename(filename);
                renameDialog.setAdapter(adapter);
                renameDialog.setPosition(listPosition);
                renameDialog.show(getFragmentManager(), "rename");
                break;
            case R.id.context_menu_playback:
                mFileName = filename.toString();
                mStatusPlayer.set(STATUS_PALYER_PREPARED);
                TextView textView = findViewById(R.id.textView);
                textView.setText(filename);
                mFloatingActionButton.callOnClick();
                break;
            case R.id.context_menu_delete:
                FileDeleteDialogFragment deleteDialog = new FileDeleteDialogFragment();
                deleteDialog.setFilename(filename);
                deleteDialog.setAdapter(adapter);
                deleteDialog.setPosition(listPosition);
                deleteDialog.show(getFragmentManager(), "delete");
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // 再生終了となった場合の処理
        stopPlaying();
    }

    public static class FileRenameDialogFragment extends DialogFragment {

        private String filename;
        private int position;
        private RecordAdapter adapter;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final EditText editView = new EditText(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(editView);
            builder.setTitle("ファイル名の変更");
            builder.setMessage(filename + "の名前を変更");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {

                     if (editView.getText() == null || "".equals(editView.getText().toString())) {
                         return;
                     }
                     String sdPath = Environment.getExternalStorageDirectory().getPath();
                     File file = new File(sdPath + "/" + filename);
                     File newFile = new File(sdPath + "/" + editView.getText().toString());
                     file.renameTo(newFile);
                     adapter.changeName(position, editView.getText().toString());
                 }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // NOP
                }
            });
            return builder.create();
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
        public void setAdapter(RecordAdapter adapter) {
            this.adapter = adapter;
        }
        public void setPosition(int position) {
            this.position = position;
        }
    }

    public static class FileDeleteDialogFragment extends DialogFragment {

        private String filename;
        private int position;
        private RecordAdapter adapter;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("ファイルの削除");
            builder.setMessage(filename + "を削除します。よろしいですか？");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String sdPath = Environment.getExternalStorageDirectory().getPath();
                    File file = new File(sdPath + "/" + filename);
                    file.delete();
                    adapter.remove(position);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // NOP
                }
            });
            return builder.create();
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
        public void setAdapter(RecordAdapter adapter) {
            this.adapter = adapter;
        }
        public void setPosition(int position) {
            this.position = position;
        }
    }

    private class SeekRunner implements Runnable {

        private String name;

        public SeekRunner(String name) {
            this.name = name;
        }


        @Override
        public void run() {
            int currentPosition = mPlayer.getCurrentPosition();
            mSeekBar.setProgress(currentPosition);
        }
    }
}
