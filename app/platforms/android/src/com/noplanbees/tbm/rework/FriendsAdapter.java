package com.noplanbees.tbm.rework;

import com.noplanbees.tbm.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class FriendsAdapter extends BaseAdapter {

	private Context context;

	public FriendsAdapter(Context context) {
		this.context = context;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if(position == 4){
			v = getUserView(position, convertView, parent);
		}else
			v = getFriendView(position, convertView, parent);
		return v;
	}

	private View getFriendView(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(context).inflate(R.layout.user_grid_item, parent ,false);
		return v;
	}

	private View getUserView(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(context).inflate(R.layout.friend_grid_item, parent ,false);
		return v;
	}

}
