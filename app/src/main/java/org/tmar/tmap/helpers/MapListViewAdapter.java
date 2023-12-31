package org.tmar.tmap.helpers;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.tmar.tmap.MapDescriptor;
import org.tmar.tmap.R;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MapListViewAdapter extends BaseAdapter {

    public interface OnClickListener {
        void onClick(int position);
    }

    private final List<MapDescriptor> mRows;
    private final Activity mContext;
    private final int mResourceId;
    private final OnClickListener mListener;

    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    public MapListViewAdapter(Activity c, List<MapDescriptor> rows, int resourceId, OnClickListener listener) {
        mContext = c;
        mRows = rows;
        mResourceId = resourceId;
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mRows.size();
    }

    @Override
    public Object getItem(int position) {
        return mRows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MapDescriptor map = (MapDescriptor) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mResourceId, parent, false);
            mContext.registerForContextMenu(convertView);
        }

        // Populate data
        convertView.setTag(position);

        CheckBox btVisibility = convertView.findViewById(R.id.buttonVisibility);
        TextView tvPrimaryText = convertView.findViewById(R.id.primaryText);
        TextView tvSecondaryText = convertView.findViewById(R.id.secondaryText);

        btVisibility.setChecked(map.isVisible());
        btVisibility.setOnClickListener(v -> {
            mListener.onClick(position);
        });

        tvPrimaryText.setText(map.getName());

        File file = map.getFile();
        Date lastModified = new Date(file.lastModified());
        tvSecondaryText.setText(mContext.getString(R.string.mapSecondaryText, map.isShared() ? mContext.getString(R.string.mapShared) : mContext.getString(R.string.mapNotShared), mDateFormat.format(lastModified), Math.round(file.length() / (1024F * 1024F))));

        return convertView;
    }
}