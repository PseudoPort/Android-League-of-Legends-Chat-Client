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
import com.example.lolapp.listview.FriendsListAdapter;
import com.example.lolapp.model.Summoner;
import com.example.lolapp.utils.DummySSLSocketFactory;

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

		// Child drag
		final GestureDetector gestureDetector = new GestureDetector(activity, new FriendGestureListener());
		OnTouchListener gestureListener = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		//friendsListView.setOnTouchListener(gestureListener);

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

	class FriendGestureListener extends SimpleOnGestureListener {

		@Override
		public void onLongPress(MotionEvent e) {

		}

		int position, positionOld;

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {

			try {
				position = friendsListView.pointToPosition((int) e1.getX(), (int) e1.getY());
				if (position == -1) {
					position = positionOld;
				} else {
					positionOld = position;
				}

				try {
					View v = friendsListView.getChildAt(position);
					//v.setX(-(e1.getX()-e2.getX()));
					//v.setY(-(e1.getY()-e2.getY()));
					v.setDrawingCacheEnabled(true);
					Bitmap bm = Bitmap.createBitmap(v.getDrawingCache());
					
					ImageView iv = new ImageView(activity);
					iv.setImageBitmap(bm);
					iv.setPadding(0, 0, 0, 0);
					iv.setBackgroundColor(Color.GRAY);
					
					LayoutParams mWindowParams = new WindowManager.LayoutParams();
			        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
			        //mWindowParams.x = x - mDragPointX + mXOffset;
			        //mWindowParams.y = y - mDragPointY + mYOffset;
			        
			        mWindowParams.x = (int) e2.getX();
			        mWindowParams.y = (int) e2.getY();
			        
			        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
			        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
			                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
			                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
			                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
			                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
			        mWindowParams.format = PixelFormat.TRANSLUCENT;
			        mWindowParams.windowAnimations = 0;
					
					WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
					wm.addView(iv, mWindowParams);
					
					//iv.setX(-(e1.getX()-e2.getX()));
					//iv.setY(-(e1.getY()-e2.getY()));
					
				} catch (Exception e) {

				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
			return false;
		}

	}

}
