package com.noplanbees.tbm.ui;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
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

	public FriendsAdapter(Context context, List<GridElement> arrayList) {
		this.context = context;
		this.list = arrayList;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public GridElement getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		GridElement ge = getItem(position);
		Friend f = ge.friend();
		if (f != null) 
			v = getFriendView(f);
		else 
			v = getEmptyView();
		return v;
	}

	private View getEmptyView() {
		View v = LayoutInflater.from(context).inflate(R.layout.friendview_empty_item, null);
		return v;
	}

	private View getFriendView(Friend f) {
		View v = LayoutInflater.from(context).inflate(R.layout.friendview_item, null);

		TextView tw_name = (TextView) v.findViewById(R.id.textView1);
		ImageView img_thumb = (ImageView) v.findViewById(R.id.img_thumb);

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
		return v;
	}
	
	
}