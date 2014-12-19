package com.noplanbees.tbm.ui;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.GridElement;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.ui.view.FriendView;
import com.noplanbees.tbm.ui.view.FriendView.ClickListener;

public class FriendsAdapter extends BaseAdapter {

	private static final String TAG = "FriendsAdapter";
	
	private Context context;
	private List<GridElement> list;

	private ClickListener clickListener;

	public FriendsAdapter(Context context, List<GridElement> arrayList, ClickListener clickListener) {
		this.context = context;
		this.list = arrayList;
		this.clickListener = clickListener;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public GridElement getItem(int position) {
		switch(position){
		case 0:
			return list.get(7);
		case 1:
			return list.get(6);
		case 2:
			return list.get(4);
		case 3:
			return list.get(5);
		case 4:
			return list.get(0);
		case 5:
			return list.get(3);
		case 6:
			return list.get(1);
		case 7:
			return list.get(2);
		default:
			return list.get(position);
		}
		
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
			v = getFriendView(parent, convertView, f);
		else
			v = getEmptyView();
		return v;
	}

	private View getEmptyView() {
		View v = LayoutInflater.from(context).inflate(R.layout.friendview_empty_item, null);
		return v;
	}
	
	private View getFriendView(final ViewGroup parent, View convertView, Friend f) {
		FriendView fv;
		if(convertView==null || !(convertView instanceof FriendView)){
			fv = new FriendView(context);
			fv.setOnClickListener(clickListener);
		}else{
			fv = (FriendView) convertView;
		}
		fv.setFriend(f);
		return fv ;
	}

}