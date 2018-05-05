package com.takahiro310.voicechanger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class RecordAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private int layoutID;
    private List<Map<String, String>> items;

    static class ViewHolder {
        TextView filename;
        TextView filesize;
        TextView createdAt;
//        TextView playbackTime;
    }

    RecordAdapter(Context context, int itemLayoutId, List<Map<String, String>> items) {
        inflater = LayoutInflater.from(context);
        layoutID = itemLayoutId;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(layoutID, null);
            holder = new ViewHolder();
            holder.filename = convertView.findViewById(R.id.text_filename);
            holder.filesize = convertView.findViewById(R.id.text_filesize);
            holder.createdAt = convertView.findViewById(R.id.text_created_at);
//            holder.playbackTime = convertView.findViewById(R.id.text_playback_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, String> e = items.get(position);
        holder.filename.setText(e.get("filename"));
        holder.filesize.setText(e.get("filesize"));
        holder.createdAt.setText(e.get("created_at"));

        return convertView;
    }

    public void remove(int i) {
        if (i > getCount() - 1) {
            return;
        }
        items.remove(i);
        notifyDataSetChanged();
    }

    public void changeName(int i, String filename) {
        if (i > getCount() - 1) {
            return;
        }
        Map<String, String> e = items.get(i);
        e.put("filename", filename);
        notifyDataSetChanged();
    }
}
