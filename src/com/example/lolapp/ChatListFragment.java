package com.example.lolapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.lolapp.listview.ChatListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;

public class ChatListFragment extends Fragment {

	ExpandableListView chatListView;
	TextView textView1;

	Activity activity;
	ChatListListener mCallback;

	// Interface
	public interface ChatListListener {
		public void onChatListCreated();
		public void onChatListClick(String chatId);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mCallback = (ChatListListener) activity;
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		chatListView = (ExpandableListView) view.findViewById(R.id.chatListView);
		textView1 = (TextView) view.findViewById(R.id.textView1);
		textView1.setText("");

		chatListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				
				return true;
			}
		});

		chatListView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {

				ChatListAdapter chatListAdapter = (ChatListAdapter) chatListView.getExpandableListAdapter();				
				//System.out.println(chatListAdapter.getChild(groupPosition, childPosition));

				String chatId = chatListAdapter.getChild(groupPosition, childPosition).toString();
				mCallback.onChatListClick(chatId);

				return true;
			}
		});

		mCallback.onChatListCreated();
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_chatlist, container,
				false);
		return rootView;
	}
}
