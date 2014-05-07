package com.example.lolapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;

import com.example.lolapp.MainActivity.NotificationGestureListener;
import com.example.lolapp.NotificationFragment.OnFragmentCreatedListener;
import com.example.lolapp.listview.FriendListView;
import com.example.lolapp.listview.FriendsListAdapter;
import com.example.lolapp.listview.FriendListView.ChangeGroupListener;
import com.example.lolapp.model.Summoner;
import com.example.lolapp.utils.DummySSLSocketFactory;
import com.example.lolapp.xmppservice.XMPPService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ExpandableListView;

public class FriendsListFragment extends Fragment {

	FriendsListAdapter friendsListAdapter;
	FriendListView friendsListView;
	List<String> listHeader;
	HashMap<String, List<String>> listChildren;
	HashMap<String, Summoner> summoners;

	ArrayList<String> users;
	ArrayList<String> names;
	ArrayList<String> statuses;
	ArrayList<String> modes;
	ArrayList<String> groups;
	ArrayList<String> grouplist;

	OnFriendChatClickListener mCallback;
	
	Activity activity;

	// Interface
	OnFragmentCreatedListener mFragmentCallback;
	public interface OnFragmentCreatedListener {
		public void onFragmentCreated();
	}
	
	public interface OnFriendChatClickListener {
		public void onFriendChatClick(String name);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mCallback = (OnFriendChatClickListener) activity;
		mFragmentCallback = (OnFragmentCreatedListener) activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Initialize list view friends list
		friendsListView = (FriendListView) view.findViewById(R.id.friendsListView);

		friendsListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				String friendName = ((TextView)v.findViewById(R.id.friendName)).getText().toString();

				mCallback.onFriendChatClick(friendName);

				return true;
			}
		});

		// Enable friend list expansion when off line
		friendsListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				// Expand/Collapse Off line
				if (groupPosition == listHeader.size() - 1 && listHeader.get(groupPosition).equals("Offline")) {
					return false;
				}


				return true;
			}
		});

		// Child change group
		friendsListView.setChangeGroupListener(new ChangeGroupListener() {
			
			@Override
			public void changeGroup(String fromName, String toGroup) {
				
				Intent intent = new Intent(activity, XMPPService.class);
				intent.setAction(XMPPService.ACTION_CHANGE_FRIEND_GROUP);
				intent.putExtra(XMPPService.GROUP, toGroup);
				
				Summoner summoner = null;
				
				for (Entry s : summoners.entrySet()) {
					if (summoners.get(s.getKey()).name.equalsIgnoreCase(fromName)) {
						summoner = summoners.get(s.getKey());
						break;
					}
				}
				//System.out.println(summoner.name);
				if (summoner != null) {
					intent.putExtra(XMPPService.USER, summoner.user);
					
					if (summoner.group.equals(toGroup) || toGroup.equals("Offline")) {
						return;
					}
					activity.startService(intent);
				}
			}
		});
		
		// Set title
		activity.setTitle("Friend List");
		mFragmentCallback.onFragmentCreated();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_friendslist, container,
				false);
		return rootView;
	}

}
