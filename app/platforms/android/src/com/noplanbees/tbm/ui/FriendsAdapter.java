package com.noplanbees.tbm.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.ui.view.FriendView;
import com.noplanbees.tbm.utilities.Convenience;

import java.util.List;

public class FriendsAdapter extends BaseAdapter {

	private static final String TAG = "FriendsAdapter";
	
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
		return list.get(Convenience.getFriendPosByUiPos(position));		
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GridElement ge = getItem(position);
        Friend f = ge.friend();
        FriendView v = getFriendView(parent, convertView, f, position);
        return v;
    }

    private FriendView getFriendView(final ViewGroup parent, View convertView, Friend f, int position) {
        FriendView fv = new FriendView(context, position);

        if (f == null) {
            fv.showEmpty(true);
        } else {
            boolean isAlterName = false;
            for (GridElement gridElement : list) {
                Friend _f = gridElement.friend();
                if (_f != null && !(_f.equals(f)) && _f.getDisplayName().equals(f.getDisplayName())) {
                    isAlterName = true;
                    break;
                }
            }
            fv.showEmpty(false);
            fv.setName(isAlterName ? f.getDisplayNameAlternative() : f.getDisplayName());
        }
        return fv;
    }

}