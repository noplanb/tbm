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
		if(convertView==null)
			fv = new FriendView(context);
		else{
			fv = (FriendView) convertView;
		}
		fv.setFriend(f);
		return fv ;
	}

//	private View getFriendView(final ViewGroup parent, Friend f) {
//		final View itemView = LayoutInflater.from(context).inflate(R.layout.friendview_item, null);
//
//		int incomingStatus = f.getIncomingVideoStatus();
//		int outgoingStatus = f.getOutgoingVideoStatus();
//		int unreadMsgCount = f.incomingVideos().size();
//		
//		Log.d(TAG, "" + incomingStatus + ", " + outgoingStatus + ", " + unreadMsgCount);
//		
//		FrameLayout body = (FrameLayout) itemView.findViewById(R.id.body);
//		TextView twName = (TextView) itemView.findViewById(R.id.tw_name);
//		TextView twUnreadCount = (TextView) itemView.findViewById(R.id.tw_unread_count);
//		ImageView imgThumb = (ImageView) itemView.findViewById(R.id.img_thumb);
//		ImageView imgViewed = (ImageView) itemView.findViewById(R.id.img_viewed);
//		ImageView imgDownloading = (ImageView) itemView.findViewById(R.id.img_downloading);
//		final ImageView imgUploading = (ImageView) itemView.findViewById(R.id.img_uploading);
//
//		if (f.incomingVideoNotViewed()) {
//			body.setBackgroundResource(R.drawable.blue_border_shape);
//			twName.setBackgroundColor(context.getResources().getColor(R.color.bg_unread_msg));
//			imgViewed.setVisibility(View.GONE);
//			twUnreadCount.setVisibility(View.VISIBLE);
//			twUnreadCount.setText(""+unreadMsgCount);
//		} else {
//			body.setBackgroundResource(0);
//			twName.setBackgroundColor(context.getResources().getColor(R.color.bg_name));
//			imgViewed.setVisibility(View.VISIBLE);
//			twUnreadCount.setVisibility(View.GONE);
//			if (f.thumbExists()){
//				imgViewed.setVisibility(View.VISIBLE);
//			}else{
//				imgViewed.setVisibility(View.GONE);
//			}
//		}
//
//		if (f.thumbExists())
//			imgThumb.setImageBitmap(f.lastThumbBitmap());
//		else
//			imgThumb.setImageResource(R.drawable.head);
//
//		twName.setText(f.getStatusString());
//		
//		switch (incomingStatus){
//		case Video.IncomingVideoStatus.NEW:
//			break;
//		case Video.IncomingVideoStatus.QUEUED:
//			break;
//		case Video.IncomingVideoStatus.DOWNLOADING:
//			break;
//		case Video.IncomingVideoStatus.DOWNLOADED:
//			break;
//		case Video.IncomingVideoStatus.VIEWED:
//			break;
//		case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
//			break;
//		}
//		switch (outgoingStatus){
//		case OutgoingVideoStatus.NEW: 
//			break;
//		case OutgoingVideoStatus.QUEUED:
//			break;
//		case OutgoingVideoStatus.UPLOADING:
////			imgUploading.setVisibility(View.VISIBLE);
////			Log.d(TAG, "onLayoutChange " + imgUploading.getWidth() + "," + imgUploading.getHeight());
////			Log.d(TAG, "onLayoutChange " + itemView.getWidth() + "," + itemView.getHeight());
////			float fromXDelta = imgUploading.getX();
////			float toXDelta = itemView.getWidth()/3*2;
////			float fromYDelta = imgUploading.getY()+100;
////			float toYDelta = imgUploading.getY()+100;
////			Log.d(TAG, "onLayoutChange " + fromXDelta + "," + toXDelta + "| " + fromYDelta + ", " + toYDelta);
////			TranslateAnimation trAn = new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
////			trAn.setDuration(1000);
////			trAn.setFillAfter(true);
////			imgUploading.startAnimation(trAn);
//			break;
//		case OutgoingVideoStatus.UPLOADED:
//			break;
//		case OutgoingVideoStatus.DOWNLOADED:
//			break;
//		case OutgoingVideoStatus.VIEWED:
//			break;
//		case OutgoingVideoStatus.FAILED_PERMANENTLY:
//			break;
//		} 
//		
//		
//		return itemView;
//	}

}