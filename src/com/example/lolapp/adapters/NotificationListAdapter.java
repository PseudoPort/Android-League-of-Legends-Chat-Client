package com.example.lolapp.adapters;

import java.util.ArrayList;

import com.example.lolapp.R;
import com.example.lolapp.model.Notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NotificationListAdapter extends BaseAdapter {
	
	public static final int TYPE_MESSAGE = 0;
	public static final int TYPE_INVITE = 1;
	public static final int TYPE_EVENT = 2;
	
	LayoutInflater mInflater;
	
	ArrayList<Notification> notificationList;
	
	public NotificationListAdapter(ArrayList<Notification> notificationList) {
		this.notificationList = notificationList;
	}
	
	@Override
	public int getCount() {
		return notificationList.size();
	}
	
	@Override
	public Object getItem(int position) {
		return notificationList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressWarnings("unused")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int type = getItemViewType(position);
        //System.out.println("getView " + position + " " + convertView + " type = " + type);
        
        //System.out.println(notificationList.get(position).type);
        
        Notification notification = notificationList.get(position);
        
        type = notification.type;
        
        if (convertView == null) {
            holder = new ViewHolder();
        	switch (type) {
                case TYPE_MESSAGE:
                    convertView = mInflater.inflate(R.layout.friendslist_item, null);
                    holder.textView1 = (TextView) convertView.findViewById(R.id.friendName);
                    holder.textView2 = (TextView) convertView.findViewById(R.id.status);

                    holder.textView1.setText(notification.title);
                    holder.textView2.setText(notification.message);
                    break;
                case TYPE_INVITE:
                    convertView = mInflater.inflate(R.layout.friendslist_item, null);
                    holder.textView1 = (TextView) convertView.findViewById(R.id.friendName);
                    holder.textView2 = (TextView) convertView.findViewById(R.id.status);

                    holder.textView1.setText("Text1 - I");
                    holder.textView2.setText("Text2 - I");
                    break;
                case TYPE_EVENT:
                	convertView = mInflater.inflate(R.layout.friendslist_item, null);
                    holder.textView1 = (TextView) convertView.findViewById(R.id.friendName);
                    holder.textView2 = (TextView) convertView.findViewById(R.id.status);
                	break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        return convertView;
	}

	@Override
	public int getItemViewType(int position) {
		return notificationList.get(position).type;
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}
	
	public static class ViewHolder {
		public TextView textView1;
		public TextView textView2;
	}
}
