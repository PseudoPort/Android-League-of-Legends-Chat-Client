package com.example.lolapp;

import com.example.lolapp.listview.NotificationListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class NotificationFragment extends Fragment {
	
	Activity activity;
	NotificationListListener mCallback;
	
	ListView notificationListView;
	
	// Interface
	public interface NotificationListListener {
		public void onNotificationListCreated();
		//public void onChatListClick(String chatId);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mCallback = (NotificationListListener) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		activity.setTitle("Notifications");
		
		notificationListView = (ListView) view.findViewById(R.id.notificationListView);
		mCallback.onNotificationListCreated();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_notifications, container,
				false);
		return rootView;
	}
}
