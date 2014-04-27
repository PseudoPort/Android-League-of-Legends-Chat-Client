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

import com.example.lolapp.utils.DummySSLSocketFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.ExpandableListView;

public class FriendsListFragment extends Fragment {

	FriendsListAdapter friendsListAdapter;
	ExpandableListView friendsListView;
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
	public interface OnFriendChatClickListener {
		public void onFriendChatClick(String name);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mCallback = (OnFriendChatClickListener) activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Initialize list view friends list
		friendsListView = (ExpandableListView) view.findViewById(R.id.friendsListView);

		friendsListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				String friendName = ((TextView)v.findViewById(R.id.friendName)).getText().toString();

				mCallback.onFriendChatClick(friendName);

				return true;
			}
		});

		// Set title
		activity.setTitle("Friend List");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_friendslist, container,
				false);
		return rootView;
	}

	
}
