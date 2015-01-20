package com.noplanbees.tbm.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.noplanbees.tbm.utilities.Convenience;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.ui.view.FriendView;
import com.noplanbees.tbm.ui.view.FriendView.ClickListener;

import java.util.List;

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
		return list.get(Convenience.getFriendPosByUiPos(position));		
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
            boolean isAlterName = false;
            for (GridElement gridElement : list) {
                Friend _f = gridElement.friend();
                if(_f!=null && !(_f.equals(f)) && _f.getDisplayName().equals(f.getDisplayName())){
                   isAlterName = true;
                    break;
                }
            }
            fv = new FriendView(context, isAlterName);
			fv.setOnClickListener(clickListener);
		}else{
			fv = (FriendView) convertView;
		}
		fv.setFriend(f);
		return fv ;
	}

}