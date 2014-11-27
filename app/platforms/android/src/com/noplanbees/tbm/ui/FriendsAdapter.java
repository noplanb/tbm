package com.noplanbees.tbm.ui;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.GridElement;
import com.noplanbees.tbm.R;

public class FriendsAdapter extends BaseAdapter {

	private Context context;
	private List<GridElement> list;
	private Preview preview;
	private Camera camera;
	private boolean isRecording;

	public FriendsAdapter(Context context, List<GridElement> arrayList) {
		this.context = context;
		this.list = arrayList;

	}

	public Surface getPreviewSurface() {
		return preview.getHolder().getSurface();
	}

	public int getPreviewWidth() {
		return preview.getHeight();
	}

	public int getPreviewHeight() {
		return preview.getWidth();
	}

	public void setRecording(boolean b) {
		isRecording = b;
		if (preview != null)
			preview.setRecording(b);
	}

	public void setCamera(Camera camera) {
		if (preview != null)
			preview.setCamera(camera);
		this.camera = camera;
	}

	@Override
	public int getCount() {
		return list.size() + 1;
	}

	@Override
	public GridElement getItem(int position) {
		if (position < (int) (getCount() / 2))
			return list.get(position);
		else
			return list.get(position - 1);
	}

	@Override
	public long getItemId(int position) {
		if (position == getCount() / 2)
			return -1;
		if (position < (int) (getCount() / 2))
			return position;
		else
			return position - 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;

		if (position == getCount() / 2) {
			v = getUserView(position, convertView, parent);
		} else {
			v = getFriendView(position, convertView, parent);
		}

		return v;
	}

	private View getUserView(int position, View convertView, ViewGroup parent) {
		if(preview == null)
			preview = new Preview(context);
		if (camera != null)
			preview.setCamera(camera);

		preview.setRecording(isRecording);

		return preview;
	}

	private View getFriendView(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(context).inflate(R.layout.friendview_item, null);

		TextView tw_name = (TextView) v.findViewById(R.id.textView1);
		ImageView img_thumb = (ImageView) v.findViewById(R.id.img_thumb);

		GridElement ge = getItem(position);
		Friend f = ge.friend();
		if (f != null) {
			if (f.incomingVideoNotViewed()) {
				img_thumb.setBackgroundResource(R.drawable.blue_border_shape);
			} else {
				img_thumb.setBackgroundResource(0);
			}

			if (f.thumbExists())
				img_thumb.setImageBitmap(f.lastThumbBitmap());
			else
				img_thumb.setImageResource(R.drawable.head);

			tw_name.setText(f.getStatusString());
		}else{
			tw_name.setText("");
		}
		return v;
	}
}