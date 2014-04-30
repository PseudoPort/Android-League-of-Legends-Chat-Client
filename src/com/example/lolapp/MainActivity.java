package com.example.lolapp;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.example.lolapp.adapters.ChatListAdapter;
import com.example.lolapp.adapters.FriendsListAdapter;
import com.example.lolapp.adapters.NotificationListAdapter;
import com.example.lolapp.model.ChatData;
import com.example.lolapp.model.ChatMessage;
import com.example.lolapp.model.Summoner;
import com.example.lolapp.model.Notification;
import com.example.lolapp.xmppservice.XMPPService;
import com.example.lolapp.xmppservice.XMPPService.GroupType;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class MainActivity extends ActionBarActivity implements LoginFragment.OnLoginListener, FriendsListFragment.OnFriendChatClickListener, ChatFragment.OnCreateListener, ChatListFragment.ChatListListener, NotificationFragment.NotificationListListener{
	public static final String FILENAME = "login_file";

	public static final String HOST = "chat.na1.lol.riotgames.com";
	public static final int PORT = 5223;
	public static final String SERVICE = "pvp.net";

	MainReceiver mainReceiver;

	Activity activity = this;

	ActionBar actionBar;

	// Update in game time
	boolean updateInGameTime;
	Thread updateFriends = null;

	// Fragment Manager
	FragmentManager fragmentManager = null;
	FragmentTransaction fragmentTransaction = null;

	// Fragments
	LoginFragment loginFragment;
	FriendsListFragment friendListFragment;
	HashMap<String, ChatFragment> chatFragment;
	ChatListFragment chatListFragment;
	HomeFragment homeFragment;
	NotificationFragment notificationFragment;

	// other
	boolean remember, automatic;
	String username, password;
	Context context = this;
	ProgressDialog dialog = null;

	// Friends List
	HashMap<String, Summoner> summoners = new HashMap<String, Summoner>();

	// Current User
	String currentUser;
	String summonerName = null;

	// Active Chats
	HashMap<String, LinkedHashMap<Long, ChatMessage>> chatMessages = new HashMap<String, LinkedHashMap<Long, ChatMessage>>(); // KEY-userFrom (user/private/public), LinkedHashMap<TIME, CHATMESSAGE>
	HashMap<String, ChatData> chatData = new HashMap<String, ChatData>(); // Chat data (key-chatname/type)

	// Chat List Adapter
	List<String> chatListHeader = new ArrayList<String>();
	HashMap<String, List<String>> chatListChildren = new HashMap<String, List<String>>();
	ChatListAdapter chatListAdapter;
	
	// Notifications
	NotificationListAdapter notificationListAdapter;
	ArrayList<Notification> notificationList = new ArrayList<Notification>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		actionBar = getSupportActionBar();

		if (savedInstanceState == null) {
			// Initialize FragmentManager
			fragmentManager = getSupportFragmentManager();
			fragmentTransaction = fragmentManager.beginTransaction();

			// Initialize Fragments
			loginFragment = new LoginFragment();
			friendListFragment = new FriendsListFragment();
			chatFragment = new HashMap<String, ChatFragment>();
			chatListFragment = new ChatListFragment();
			homeFragment = new HomeFragment();
			notificationFragment = new NotificationFragment();

			// Set fragment view 			
			fragmentTransaction.add(android.R.id.content, loginFragment);
			fragmentTransaction.commit();

			// Hide split action bar
		}


		// Start update task
		updateInGameTime = true;
		if (updateFriends == null) {
			System.out.println("STARTING UPDATE FRIENDS");
			updateFriends = new CheckGameTime();
			updateFriends.start();
		}

		// Setup receiver
		mainReceiver = new MainReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(XMPPService.ACTION_CONNECT);
		intentFilter.addAction(XMPPService.ACTION_DISCONNECT);
		intentFilter.addAction(XMPPService.ACTION_UPDATE_ROSTER);
		intentFilter.addAction(XMPPService.ACTION_SEND_MESSAGE);
		intentFilter.addAction(XMPPService.ACTION_RECEIVE_MESSAGE);
		intentFilter.addAction(XMPPService.ACTION_SEND_GROUP_MESSAGE);
		intentFilter.addAction(XMPPService.ACTION_RECEIVE_GROUP_MESSAGE);
		intentFilter.addAction(XMPPService.ACTION_SEND_GROUP_INVITE);
		intentFilter.addAction(XMPPService.ACTION_RECEIVE_GROUP_INVITE);
		intentFilter.addAction(XMPPService.ACTION_UPDATE_GROUP_CHAT);
		intentFilter.addAction(XMPPService.ACTION_SET_SUMMONER_NAME);
		registerReceiver(mainReceiver, intentFilter);

	}

	// On action bar select	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_actions, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			openSettings();
			return true;
		case R.id.action_friendlist:
			openFriendList();
			return true;
		case R.id.action_home:
			openHome();
			return true;
		case R.id.action_chatlist:
			openChatList();
			return true;
		case R.id.action_notifications:
			openNotifications();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Fragment Callback

	@Override
	public void onLogin(String username, String password, boolean remember, boolean automatic) {
		// Show load
		dialog = ProgressDialog.show(this, "Connecting...", "Please wait...", false);
		dialog.show();

		// OVERRIDE LOGIN
		//username = "mohammedsirhan";

		this.username = username;
		this.password = password;
		this.remember = remember;
		this.automatic = automatic;

		// Start Connection Service
		// Initialize Service
		Intent serviceIntent = new Intent(this, XMPPService.class);
		serviceIntent.putExtra("HOST", HOST);
		serviceIntent.putExtra("PORT", ""+PORT);
		serviceIntent.putExtra("SERVICE", SERVICE);
		serviceIntent.putExtra("USERNAME", username);
		serviceIntent.putExtra("PASSWORD", password);
		serviceIntent.putExtra("REMEMBER", remember);
		serviceIntent.putExtra("AUTOMATIC", automatic);
		serviceIntent.setAction(XMPPService.ACTION_CONNECT);
		startService(serviceIntent);

	}

	// On Friend List Click / Chat Click
	@Override
	public void onFriendChatClick(String name) {
		String chatId = "";
		for (Entry<String, Summoner> entry : summoners.entrySet()) {
			Summoner s = summoners.get(entry.getKey());
			if (s.name.equalsIgnoreCase(name)) {
				if (!s.isOnline) return;
				chatId = s.user;
				if (!chatData.containsKey(chatId)) chatData.put(chatId, new ChatData(name, GroupType.NORMAL));

				break;
			}
		}

		System.out.println(name + " " + chatId);
		onChatListClick(chatId);
	}

	// On Chat List Click
	@Override
	public void onChatListClick(String chatId) {
		openChat(chatId);
	}

	// On Chat Fragment Created
	@Override
	public void onCreate(String user) {
		// Setup chatMessages
		if (!chatMessages.containsKey(user)) {
			chatMessages.put(user, new LinkedHashMap<Long, ChatMessage>());
			chatData.put(user, new ChatData(summoners.get(user).name, GroupType.NORMAL));
		}

		ChatFragment chatTemp = chatFragment.get(user);

		chatTemp.chatAdapter.clear();
		for (Entry<Long, ChatMessage> entry : chatMessages.get(user).entrySet()) {
			ChatMessage value = (ChatMessage) entry.getValue();
			chatTemp.chatAdapter.add(value.message);
		}
		chatTemp.chatView.setAdapter(chatTemp.chatAdapter);
	}

	// On ChatList Fragment Created
	@Override
	public void onChatListCreated() {
		// Chat List
		// Create groups (Normal, Private, Public)
		if (chatListHeader.isEmpty()) {
			chatListHeader.add("Normal");
			chatListHeader.add("Private");
			chatListHeader.add("Public");
		}

		// Add Chats to groups
		List<String> normalChat, privateChat, publicChat;
		normalChat = new ArrayList<String>();
		privateChat = new ArrayList<String>();
		publicChat = new ArrayList<String>();

		// Add keys of each chat into the groups
		for (Entry<String, LinkedHashMap<Long, ChatMessage>> entry : chatMessages.entrySet()) {
			System.out.println("ADDING: " + entry.getKey());
			if (entry.getKey().contains("@conference.pvp.net")) {
				privateChat.add(entry.getKey());
			} else if (entry.getKey().contains("@lvl.pvp.net")) {
				publicChat.add(entry.getKey());
			} else {
				normalChat.add(entry.getKey());
			}
		}

		chatListChildren.put(chatListHeader.get(0), normalChat);
		chatListChildren.put(chatListHeader.get(1), privateChat);
		chatListChildren.put(chatListHeader.get(2), publicChat);

		chatListAdapter = new ChatListAdapter(context, chatListHeader, chatListChildren, chatData, chatMessages);
		chatListFragment.chatListView.setAdapter(chatListAdapter);

		// Expand view
		for (int i = 0; i < chatListHeader.size(); i++) {
			chatListFragment.chatListView.expandGroup(i);
		}
	}
	
	// On NotificationList Fragment Created
	@Override
	public void onNotificationListCreated() {
		// Populate notification list		
		notificationListAdapter = new NotificationListAdapter(notificationList);
		if (notificationFragment.notificationListView != null) notificationFragment.notificationListView.setAdapter(notificationListAdapter);
	}

	@Override
	public void onBackPressed() {
		notificationList.add(0, new Notification("New Notif - invit", NotificationListAdapter.TYPE_INVITE));
		onNotificationListCreated();
		
		return;
		/*
		if (getSupportFragmentManager().findFragmentByTag("chatTag") != null) {
			getSupportFragmentManager().popBackStack("main_chat_tag",
					FragmentManager.POP_BACK_STACK_INCLUSIVE);
			updateRoster();
		} else if (getSupportFragmentManager().findFragmentByTag("chatListTag") != null) {
			getSupportFragmentManager().popBackStack("list_chat_tag",
					FragmentManager.POP_BACK_STACK_INCLUSIVE);
			updateRoster();
		} else {
			super.onBackPressed();
		}*/
	}

	// OTHER METHODS
	public void updateRoster() {
		Intent refreshRoster = new Intent(this, XMPPService.class);
		refreshRoster.setAction(XMPPService.ACTION_UPDATE_ROSTER);
		startService(refreshRoster);
	}


	private class MainReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(XMPPService.ACTION_CONNECT)) {
				onConnect(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_SET_SUMMONER_NAME)) {
				setSummonerName(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_UPDATE_ROSTER)) {
				updateRoster(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_SEND_MESSAGE)) {
				onSendMessage(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_RECEIVE_MESSAGE)) {
				onReceiveMessage(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_SEND_GROUP_MESSAGE)) {
				onSendGroupMessage(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_RECEIVE_GROUP_MESSAGE)) {
				onReceiveGroupMessage(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_SEND_GROUP_INVITE)) {
				onSendGroupInvite(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_RECEIVE_GROUP_INVITE)) { // On receive group chat invite
				onReceiveGroupInvite(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_UPDATE_GROUP_CHAT)) { // Update group chat invite
				updateGroupChat(intent);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mainReceiver);
		dialog.dismiss();
		stopService(new Intent(this, XMPPService.class));
	}

	@Override
	public boolean onSearchRequested() {
		//Intent intent = new Intent(this, XMPPService.class);
		//intent.setAction(XMPPService.ACTION_SET_AWAY_STATUS);
		//startService(intent);

		//Intent intent = new Intent(this, XMPPService.class);
		//intent.setAction(XMPPService.ACTION_SEND_GROUP_INVITE);
		//intent.putExtra("NAME", "sum54559857@pvp.net");
		
		//startService(intent);
		
		notificationList.add(0, new Notification("New Notif", NotificationListAdapter.TYPE_MESSAGE));
		onNotificationListCreated();
		
		return super.onSearchRequested();
	}

	private class CheckGameTime extends Thread implements Runnable {

		@Override
		public void run() {
			Intent intent = new Intent(activity, XMPPService.class);
			intent.setAction(XMPPService.ACTION_UPDATE_ROSTER);

			boolean update;

			while (updateInGameTime) {
				update = false;
				if (summoners != null) {
					for (Entry<String, Summoner> summoner : summoners.entrySet()) {
						Summoner s = summoner.getValue();
						if (!s.isOnline || !s.inGame) {
							s.inGameTime = 0;
							continue;
						}
						long time = Long.parseLong(s.getTimeStamp());
						if (s.inGameTime != time) {
							update = true;
							System.out.println(s.name + ": " + s.inGameTime + " " + time);
							s.inGameTime = time;
						}

					}
					if (update) {
						System.out.println("UPDATE ROSTER - " + update);
						startService(intent);
					}
				}
				// Refresh rate
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

				}
			}
		}
	}
	
	// Create Fragments
	public void openFriendList() {
		friendListFragment = new FriendsListFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, friendListFragment);
		fragmentTransaction.commit();
		updateRoster();
	}
	
	public void openChatList() {
		chatListFragment = new ChatListFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, chatListFragment);
		fragmentTransaction.commit();
	}
	
	public void openHome() {
		homeFragment = new HomeFragment();
		
		Bundle b = new Bundle();
		b.putString(HomeFragment.SUMMONER_NAME, summonerName);
		homeFragment.setArguments(b);
		
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, homeFragment);
		fragmentTransaction.commit();
	}
	
	public void openNotifications() {
		notificationFragment = new NotificationFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, notificationFragment);
		fragmentTransaction.commit();
	}
	
	public void openSettings() {
		
	}
	
	public void openLogin() {
		
	}
	
	public void openChat(String chatId) {
		if (!chatData.containsKey(chatId)) return;

		String chatName = chatData.get(chatId).name;

		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(chatId)) chatFragment.put(chatId,  chatTemp);
		else chatTemp = chatFragment.get(chatId);

		Bundle b = new Bundle();
		b.putString(ChatFragment.CHAT_ID, chatId);
		b.putString(ChatFragment.NAME, chatName);
		if (summoners.containsKey(chatId)) b.putSerializable(ChatFragment.TYPE, ChatFragment.Type.NORMAL);
		else b.putSerializable(ChatFragment.TYPE, ChatFragment.Type.GROUP);

		chatTemp.setArguments(b);

		// Switch fragment
		fragmentTransaction = fragmentManager.beginTransaction();
		//fragmentTransaction.replace(android.R.id.content, chatTemp, "chatTag").addToBackStack("main_chat_tag");
		fragmentTransaction.replace(android.R.id.content, chatTemp);
		fragmentTransaction.commit();
	}
	
	// Functions
	
	public void setSummonerName(Intent intent) {
		summonerName = intent.getStringExtra(XMPPService.NAME);
		if (homeFragment != null) homeFragment.setSummonerName(summonerName);
	}
	
	public void onConnect(Intent intent) {
		dialog.dismiss();

		boolean success = intent.getBooleanExtra(XMPPService.CONNECTION_STATUS, false);
		// Write to file
		FileOutputStream fos;
		if (!success) {
			try {
				fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				osw.write(remember + "\n" + "false" + "\n" + username + "\n" + password);
				osw.close();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setCancelable(false)
			.setPositiveButton("OK", null)
			.setMessage("Unable to login!");
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			try {
				fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				osw.write(remember + "\n" + automatic + "\n" + username + "\n" + password);
				osw.close();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Switch fragment
			openFriendList();
		}

		currentUser = intent.getExtras().getString(XMPPService.USER);

	}
	
	public void updateRoster(Intent intent) {
		Bundle b = intent.getExtras();
		ArrayList<String> users = b.getStringArrayList(XMPPService.USER);
		ArrayList<String> names = b.getStringArrayList(XMPPService.NAME);
		ArrayList<String> statuses = b.getStringArrayList(XMPPService.STATUS);
		ArrayList<String> modes = b.getStringArrayList(XMPPService.MODE);
		ArrayList<String> groups = b.getStringArrayList(XMPPService.GROUP);
		ArrayList<String> grouplist = b.getStringArrayList(XMPPService.GROUPLIST);
		ArrayList<String> isOnline = b.getStringArrayList(XMPPService.IS_ONLINE);

		summoners.clear();

		for (int i = 0; i < users.size(); i++) {
			summoners.put(users.get(i), new Summoner(users.get(i), names.get(i), statuses.get(i), modes.get(i), groups.get(i), Boolean.parseBoolean(isOnline.get(i))));
		}

		ArrayList<ArrayList<String>> friendGroups = new ArrayList<ArrayList<String>>();
		friendListFragment.listHeader = new ArrayList<String>();
		friendListFragment.listChildren = new HashMap<String, List<String>>();

		for (String g : grouplist) {
			friendListFragment.listHeader.add(g);
			friendGroups.add(new ArrayList<String>());
		}

		for (Entry<String, Summoner> entry : summoners.entrySet()) {
			Summoner s = summoners.get(entry.getKey());
			friendGroups.get(friendListFragment.listHeader.indexOf(s.group)).add(s.user);
		}

		for (int i = 0; i < friendListFragment.listHeader.size(); i++) {
			friendListFragment.listChildren.put(friendListFragment.listHeader.get(i), friendGroups.get(i));
		}

		friendListFragment.summoners = summoners;

		friendListFragment.friendsListAdapter = new FriendsListAdapter(context, friendListFragment.listHeader, friendListFragment.listChildren, friendListFragment.summoners);
		friendListFragment.friendsListView.setAdapter(friendListFragment.friendsListAdapter);

		for (int i = 0; i < friendListFragment.listHeader.size() && !friendListFragment.listHeader.get(i).equals("Offline"); i++) {
			friendListFragment.friendsListView.expandGroup(i);
		}
	}
	
	public void onSendMessage(Intent intent) {
		Bundle b = intent.getExtras();

		String recipient = b.getString(XMPPService.USER);
		String message = b.getString(XMPPService.MESSAGE);

		ChatFragment chat = chatFragment.get(recipient);

		if (!chatMessages.containsKey(recipient)) {
			chatMessages.put(recipient, new LinkedHashMap<Long, ChatMessage>());
		}

		// Retrieve old messages
		chat.chatAdapter.clear();
		for (Entry<Long, ChatMessage> entry : chatMessages.get(recipient).entrySet()) {
			//Long key = (Long) entry.getKey();
			ChatMessage value = (ChatMessage) entry.getValue();
			chat.chatAdapter.add(value.message);
		}

		// Add new message
		chatMessages.get(recipient).put(System.currentTimeMillis(), new ChatMessage(message, username, true));
		chat.chatAdapter.add(message);

		chat.chatView.setAdapter(chat.chatAdapter);
	}
	
	public void onReceiveMessage(Intent intent) {
		Bundle b = intent.getExtras();
		String user = b.getString(XMPPService.USER);

		// Add new message
		// Setup chatMessages
		if (!chatMessages.containsKey(user)) {
			chatMessages.put(user, new LinkedHashMap<Long, ChatMessage>());
			chatData.put(user, new ChatData(summoners.get(user).name, GroupType.NORMAL));
		}
		chatMessages.get(user).put(System.currentTimeMillis(), new ChatMessage(summoners.get(b.getString(XMPPService.USER)).name + ": " + b.getString(XMPPService.MESSAGE), b.getString(XMPPService.USER), false));

		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(user)) {
			chatFragment.put(user, chatTemp);

			Bundle b2 = new Bundle();
			b2.putString(ChatFragment.CHAT_ID, user);
			b2.putString(ChatFragment.NAME, summoners.get(b.getString(XMPPService.USER)).name);
			b2.putSerializable(ChatFragment.TYPE, ChatFragment.Type.NORMAL);
			chatTemp.setArguments(b2);
			return;
		}
		else chatTemp = chatFragment.get(user);

		if (chatTemp.chatAdapter != null) {
			chatTemp.chatAdapter.add(summoners.get(b.getString(XMPPService.USER)).name + ": " + b.getString(XMPPService.MESSAGE));
			chatTemp.chatAdapter.notifyDataSetChanged();
		}
	}
	
	public void onSendGroupMessage(Intent intent) {
		Bundle b = intent.getExtras();

		String recipient = b.getString(XMPPService.CHAT_ID);
		String message = b.getString(XMPPService.MESSAGE);

		ChatFragment chat = chatFragment.get(recipient);

		if (!chatMessages.containsKey(recipient)) {
			chatMessages.put(recipient, new LinkedHashMap<Long, ChatMessage>());
		}

		// Retrieve old messages
		chat.chatAdapter.clear();
		for (Entry<Long, ChatMessage> entry : chatMessages.get(recipient).entrySet()) {
			//Long key = (Long) entry.getKey();
			ChatMessage value = (ChatMessage) entry.getValue();
			chat.chatAdapter.add(value.message);
		}

		// Add new message
		chatMessages.get(recipient).put(System.currentTimeMillis(), new ChatMessage(message, username, true));
		chat.chatAdapter.add(message);

		chat.chatView.setAdapter(chat.chatAdapter);
	}
	
	public void onReceiveGroupMessage(Intent intent) {
		Bundle b = intent.getExtras();
		String chatId = b.getString(XMPPService.CHAT_ID);
		String message = b.getString(XMPPService.MESSAGE);
		String sender = b.getString(XMPPService.SENDER);

		// Add new group message
		// Setup chatMessages
		if (!chatMessages.containsKey(chatId)) {
			chatMessages.put(chatId, new LinkedHashMap<Long, ChatMessage>());
			if (chatId.contains("pr~")) chatData.put(chatId,  new ChatData("NAME OF CHAT", GroupType.PRIVATE));
			else chatData.put(chatId,  new ChatData("NAME OF CHAT", GroupType.PUBLIC));
		}
		chatMessages.get(chatId).put(System.currentTimeMillis(), new ChatMessage(sender + ": " + message, chatId, false));

		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(chatId)) {
			chatFragment.put(chatId, chatTemp);

			Bundle b2 = new Bundle();
			b2.putString(ChatFragment.CHAT_ID, chatId);
			b2.putString(ChatFragment.NAME, "Chat Name");
			b2.putSerializable(ChatFragment.TYPE, chatData.get(chatId).type);
			chatTemp.setArguments(b2);
			return;
		}
		else chatTemp = chatFragment.get(chatId);

		if (chatTemp.chatAdapter != null) {
			chatTemp.chatAdapter.add(sender + ": " + message);
			chatTemp.chatAdapter.notifyDataSetChanged();
		}
	}
	
	public void onSendGroupInvite(Intent intent) {
		
	}
	
	public void onReceiveGroupInvite(Intent intent) {
		Bundle b = intent.getExtras();

		String from = b.getString(XMPPService.GROUP_FROM);
		GroupType type = (GroupType) b.get(XMPPService.GROUP_TYPE);
		String groupName = b.getString(XMPPService.GROUP_CHAT_NAME);

		//long time = b.getLong(XMPPService.TIMESTAMP);

		if (type.equals(GroupType.PRIVATE)) {

		} else {

		}

		final Intent i = new Intent(context, XMPPService.class);
		i.setAction(XMPPService.ACTION_RECEIVE_GROUP_INVITE);
		i.putExtra(XMPPService.GROUP_FROM, from);
		i.putExtra(XMPPService.GROUP_TYPE, type);
		i.putExtra(ChatFragment.TYPE, ChatFragment.Type.GROUP);
		i.putExtra(XMPPService.GROUP_CHAT_NAME, groupName);
		
		// Chat invite receive
		DialogInterface.OnClickListener onInviteClick = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					System.out.println("Y");
					i.putExtra(XMPPService.INVITE_RESPONSE, true);
					startService(i);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					System.out.println("N");
					i.putExtra(XMPPService.INVITE_RESPONSE, false);
					startService(i);
					break;
				case DialogInterface.BUTTON_NEUTRAL:
					System.out.println("L");
					break;
				}
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Do you want to join " + groupName + "?")
			.setPositiveButton("Yes", onInviteClick)
			.setNegativeButton("No", onInviteClick)
			.setNeutralButton("Later", onInviteClick);
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public void updateGroupChat(Intent intent) {
		System.out.println("UPDATING GROUP CHAT (Adding messages)");

		Bundle b = intent.getExtras();

		String from = b.getString(XMPPService.GROUP_FROM);
		boolean response = b.getBoolean(XMPPService.INVITE_RESPONSE);
		GroupType type = (GroupType) b.get(XMPPService.GROUP_TYPE);
		String groupName = b.getString(XMPPService.GROUP_CHAT_NAME);

		if (response) {
			if (!chatMessages.containsKey(from)) chatMessages.put(from, new LinkedHashMap<Long, ChatMessage>());
			chatData.put(from, new ChatData(groupName, type));
			if (type.equals(GroupType.PRIVATE)) {

			} else {

			}
		}
	}
}
