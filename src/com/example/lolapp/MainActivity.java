package com.example.lolapp;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jivesoftware.smack.util.StringUtils;

import com.example.lolapp.ChatFragment.Type;
import com.example.lolapp.listview.ChatListAdapter;
import com.example.lolapp.listview.FriendsListAdapter;
import com.example.lolapp.listview.NotificationListAdapter;
import com.example.lolapp.model.ChatData;
import com.example.lolapp.model.ChatMessage;
import com.example.lolapp.model.Summoner;
import com.example.lolapp.model.Notification;
import com.example.lolapp.xmppservice.XMPPService;
import com.example.lolapp.xmppservice.XMPPService.GroupType;

import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.R.anim;
import android.R.color;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends ActionBarActivity implements LoginFragment.OnLoginListener, FriendsListFragment.OnFriendChatClickListener, ChatFragment.OnCreateListener, ChatListFragment.ChatListListener, NotificationFragment.NotificationListListener, 
LoginFragment.OnFragmentCreatedListener, FriendsListFragment.OnFragmentCreatedListener, HomeFragment.OnFragmentCreatedListener, NotificationFragment.OnFragmentCreatedListener, ChatFragment.OnFragmentCreatedListener,
ChatListFragment.OnFragmentCreatedListener, HomeFragment.StatusClickListener {
	public static final String FILENAME = "login_file";

	public static final String HOST = "chat.na1.lol.riotgames.com";
	public static final int PORT = 5223;
	public static final String SERVICE = "pvp.net";

	public static final String FRAGMENT_LOGIN = "login";
	public static final String FRAGMENT_HOME = "home";
	public static final String FRAGMENT_FRIEND_LIST = "friendlist";
	public static final String FRAGMENT_CHAT_LIST = "chatlist";
	public static final String FRAGMENT_CHAT = "chat";
	public static final String FRAGMENT_SETTINGS = "settings";
	public static final String FRAGMENT_NOTIFICATIONS = "notifications";

	public static final String ACTION_NOTIFICATION_DELETE = "action.NOTIFICATION_DELETE";

	MainReceiver mainReceiver;

	Activity activity = this;

	ActionBar actionBar;

	Menu optionsMenu = null;
	MenuInflater inflater = null;
	LayoutInflater mLayoutInflater = null;

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
	ProgressDialog dialog2 = null;

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

	String activeChat = null;

	// Notification animation
	int position = 0, positionOld = 0;

	// Back navigation
	String previousFragment = null;

	// Activity active
	boolean foreground = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Setup Action bar
		actionBar = getSupportActionBar();
		actionBar.setDisplayShowCustomEnabled(true);

		if (savedInstanceState == null) {
			// Initialize FragmentManager
			fragmentManager = getSupportFragmentManager();

			// Initialize Fragments
			loginFragment = new LoginFragment();
			friendListFragment = new FriendsListFragment();
			chatFragment = new HashMap<String, ChatFragment>();
			chatListFragment = new ChatListFragment();
			homeFragment = new HomeFragment();
			notificationFragment = new NotificationFragment();

			// Set fragment view
			openLogin();

			// Hide split action bar
		} else {

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
		intentFilter.addAction(XMPPService.ACTION_SEND_GROUP_INVITE);
		intentFilter.addAction(XMPPService.ACTION_GROUP_LIST);
		intentFilter.addAction(XMPPService.ACTION_TEST);

		registerReceiver(mainReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		foreground = false;
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		foreground = true;
		notificationMultiple = false;
		notificationCount = 0;

		if (notificationChat != null) {
			for (Entry<String, Summoner> sum : summoners.entrySet()) {
				if (sum.getValue().name.equalsIgnoreCase(notificationChat)) {
					openChat(sum.getKey());
					break;
				}
			}
		}
		
		super.onResume();
	}

	// On action bar select	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		optionsMenu = menu;
		inflater = getMenuInflater();
		mLayoutInflater = getLayoutInflater();

		if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_LOGIN) != null) { 
			inflater.inflate(R.menu.empty, menu);
		} else if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CHAT) != null) {
			inflater.inflate(R.menu.empty, menu);
			actionBarChat();
		} else if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CHAT_LIST) != null) {
			inflater.inflate(R.menu.activity_main_actions, menu);
			actionBarChatList();
		} else {
			inflater.inflate(R.menu.activity_main_actions, menu);
			actionBar.setCustomView(null);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (groupListView != null) {
			try {
				((FrameLayout)groupListView.getParent()).removeView(groupListView);
				groupListView = null;
			} catch (Exception e) {
			}
		}

		actionBar.setHomeButtonEnabled(false);
		previousFragment = null;

		switch (item.getItemId()) {
		case android.R.id.home:
			back();
			return true;
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

	// Back navigation
	boolean exit = false;
	private void back() {
		if (groupListView != null) {
			try {
				((FrameLayout)groupListView.getParent()).removeView(groupListView);
				groupListView = null;
			} catch (Exception e) {
			}
		}
		if (exit) super.onBackPressed();
		if (previousFragment == null) {
			// Check if chat?
			if (fragmentManager.findFragmentByTag(FRAGMENT_CHAT) != null) {
				openChatList();
				return;
			}
			
			Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
			exit = true;
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					exit = false;
				}
			}, 2000);

			return;
		}
		if (previousFragment.equals(FRAGMENT_FRIEND_LIST)) {
			openFriendList();
		} else if (previousFragment.equals(FRAGMENT_HOME)) {
			openHome();
		} else if (previousFragment.equals(FRAGMENT_CHAT_LIST)) {
			openChatList();
		} else if (previousFragment.equals(FRAGMENT_NOTIFICATIONS)) {
			openNotifications();
		}
		previousFragment = null;
	}

	// Setup action bar
	public void actionBarChatList() {
		View mActionBarView = mLayoutInflater.inflate(R.layout.actionbar_overflow, null);

		mActionBarView.findViewById(R.id.action_overflow).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(activity, v);

				popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						final Intent intent = new Intent(activity, XMPPService.class);
						intent.setAction(XMPPService.ACTION_JOIN_GROUP_CHAT);

						switch (item.getItemId()) {
						case R.id.action_join_private:
							String chatName = (summonerName == null ? StringUtils.parseName(currentUser) : summonerName) + "'s Chat Room";

							String chatId = sha1(chatName.toLowerCase());

							intent.putExtra(XMPPService.CHAT_ID, "pr~" + chatId + "@conference.pvp.net");
							intent.putExtra(XMPPService.GROUP_CHAT_NAME, chatName);

							startService(intent);

							dialog2 = ProgressDialog.show(activity, "Group Chat", "Joining group chat", false);
							dialog2.show();

							return true;
						case R.id.action_join_public:


							final EditText input = new EditText(MainActivity.this);  
							RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
									RelativeLayout.LayoutParams.WRAP_CONTENT,
									RelativeLayout.LayoutParams.WRAP_CONTENT);

							input.setLayoutParams(lp);
							input.setImeActionLabel("Done", EditorInfo.IME_ACTION_NONE);
							input.setSingleLine();

							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							builder.setView(input)
							.setMessage("Join public chat")
							.setPositiveButton("Join", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									String chatName = "" + input.getText();

									if (chatName.equals("")) return;

									String user = "";
									String chatId = sha1(chatName.toLowerCase());

									intent.putExtra(XMPPService.CHAT_ID, "pu~" + chatId + "@lvl.pvp.net");
									intent.putExtra(XMPPService.GROUP_CHAT_NAME, chatName);

									startService(intent);

									dialog2 = ProgressDialog.show(activity, "Group Chat", "Joining group chat", false);

									dialog2.show();
								}
							});
							AlertDialog alert = builder.create();
							alert.show();
							return true;
						default:
							return false;
						}
					}
				});

				MenuInflater inflater = popup.getMenuInflater();
				inflater.inflate(R.menu.chatlist_actions, popup.getMenu());
				popup.show();
			}
		});
		actionBar.setCustomView(mActionBarView, new LayoutParams(Gravity.RIGHT));
	}

	boolean[] checkedItems = null;
	AlertDialog d = null;
	ArrayList<CharSequence> friends = null;
	public void actionBarChat() {
		final GroupType type = chatData.get(activeChat).type;

		View mActionBarView;
		if (type == GroupType.NORMAL) mActionBarView = mLayoutInflater.inflate(R.layout.actionbar_overflow, null);
		else mActionBarView = mLayoutInflater.inflate(R.layout.actionbar_chatlist, null);

		mActionBarView.findViewById(R.id.action_overflow).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(activity, v);
				checkedItems = null;
				popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.action_invite:
							// Open invite dialog

							// Get friends
							friends = new ArrayList<CharSequence>();
							for (Entry<String, Summoner> s : summoners.entrySet()) {
								Summoner summoner = s.getValue();
								if (summoner.isOnline) friends.add(summoner.name);
							}
							final int size = friends.size();
							d = null;
							checkedItems = null;

							AlertDialog.Builder b = new AlertDialog.Builder(context)
							.setTitle("Friends")
							.setPositiveButton("Invite", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (checkedItems == null) return;

									Intent intent = new Intent(activity, XMPPService.class);
									intent.setAction(XMPPService.ACTION_SEND_GROUP_INVITE);									

									ArrayList<String> invited = new ArrayList<String>();
									for (int i = 0; i < checkedItems.length; i++) {
										//System.out.println(i + " " + friends.get(i) + " " +checkedItems[i]);
										if (checkedItems[i]) invited.add(friends.get(i).toString());
									}

									for (Entry<String, Summoner> entry : summoners.entrySet()) {
										for (String sum : invited) {
											if (entry.getValue().name.equalsIgnoreCase(sum)) {
												if (intent.getStringExtra(XMPPService.USER) == null) {
													intent.putExtra(XMPPService.USER, entry.getValue().user);
												} else {
													intent.putExtra(XMPPService.USER, intent.getStringExtra(XMPPService.USER) + "," + entry.getValue().user);
												}
											}
										}
									}
									intent.putExtra(XMPPService.CHAT_ID, activeChat);
									intent.putExtra(XMPPService.GROUP_CHAT_NAME, chatData.get(activeChat).name);

									if (intent.getExtras().containsKey(XMPPService.USER)) startService(intent);
								}
							})
							.setMultiChoiceItems(((ArrayList<CharSequence>)friends).toArray(new CharSequence[size]), checkedItems, new OnMultiChoiceClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which, boolean isChecked) {
									if (checkedItems == null) checkedItems = new boolean[size];
									checkedItems[which] = isChecked;
								}
							});
							d = b.create();
							d.show();

							return true;
						case R.id.action_close:
							// Go to chat list, remove chat instance
							chatFragment.remove(activeChat);
							chatData.remove(activeChat);
							chatMessages.remove(activeChat);
							for (String header : chatListHeader) {
								if (chatListChildren.get(header).remove(activeChat)) break;
							}
							if (type != GroupType.NORMAL) {
								Intent intent = new Intent(activity, XMPPService.class);
								intent.setAction(XMPPService.ACTION_LEAVE_GROUP);
								intent.putExtra(XMPPService.CHAT_ID, activeChat);
								activity.startService(intent);
								System.out.println("STARTED");
							}
							activeChat = null;
							openChatList();
							return true;
						default:
							return false;
						}
					}
				});

				MenuInflater inflater = popup.getMenuInflater();
				inflater.inflate(R.menu.chat_actions, popup.getMenu());
				popup.show();
				if (type == GroupType.NORMAL) popup.getMenu().getItem(0).setVisible(false);
			}
		});

		if (type != GroupType.NORMAL) {
			mActionBarView.findViewById(R.id.action_chatlist).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// Get group list
					Intent intent = new Intent(activity, XMPPService.class);
					intent.setAction(XMPPService.ACTION_GROUP_LIST);
					intent.putExtra(XMPPService.CHAT_ID, activeChat);
					startService(intent);
				}
			});
		}

		actionBar.setCustomView(mActionBarView, new LayoutParams(Gravity.RIGHT));
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

	// On Friend Group Click
	@Override
	public void onFriendGroupClick(ExpandableListView parent, View v,
			final int groupPosition, long id) {
		PopupMenu popup = new PopupMenu(activity, v);

		popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String groupName = friendListFragment.listHeader.get(groupPosition);
				switch (item.getItemId()) {
				case R.id.action_invite:
					if (summonerName == null) return true;
					// Invite to group chat
					inviteGroupToChat(groupName, friendListFragment.listChildren.get(groupName));
					return true;
				case R.id.action_message:
					// Send message to all in group
					sendMessageToGroup(groupName, friendListFragment.listChildren.get(groupName));
					return true;
				default:
					return false;
				}
			}
		});

		MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.friend_group_actions, popup.getMenu());
		popup.show();
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
		if (notificationFragment.notificationListView != null) {

			notificationFragment.notificationListView.setAdapter(notificationListAdapter);

			// Setup onClick
			ListView listView = notificationFragment.notificationListView;
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// Do nothing
				}
			});

			// Setup Delete
			final GestureDetector gestureDetector = new GestureDetector(this, new NotificationGestureListener());
			OnTouchListener gestureListener = new OnTouchListener() {

				@Override
				public boolean onTouch(final View v, MotionEvent event) {

					if (event.getAction() == MotionEvent.ACTION_UP) {
						//int position = notificationFragment.notificationListView.pointToPosition((int)event.getX(), (int)event.getY());

						if (position == -1) {
							position = positionOld;
						} else {
							positionOld = position;
						}

						final int pos = position - notificationFragment.notificationListView.getFirstVisiblePosition();
						try {
							final float x = notificationFragment.notificationListView.getChildAt(position).getX();

							final Animation anim = new TranslateAnimation(0f, -x, 0f, 0f);
							anim.setDuration(300);

							anim.setAnimationListener(new AnimationListener() {

								@Override
								public void onAnimationStart(Animation animation) {
									notificationFragment.notificationListView.setOnTouchListener(new OnTouchListener() {

										@Override
										public boolean onTouch(View v, MotionEvent event) {
											return false;
										}
									});
								}

								@Override
								public void onAnimationRepeat(Animation animation) {

								}

								@Override
								public void onAnimationEnd(Animation animation) {
									//notificationFragment.notificationListView.getChildAt(pos).setX(0f);
									//System.out.println(pos);
									onNotificationListCreated();
								}
							});
							notificationFragment.notificationListView.getChildAt(position).startAnimation(anim);

						} catch (NullPointerException e) {

						}

					}

					return gestureDetector.onTouchEvent(event);
				}
			};
			listView.setOnTouchListener(gestureListener);
		}
	}
	
	// On Status Change
	@Override
	public void onStatusClicked(String statusText, String statusType) {
		Intent intent = new Intent(this, XMPPService.class);
		intent.setAction(XMPPService.ACTION_STATUS_CHANGE);
		intent.putExtra(XMPPService.STATUS, statusText);
		intent.putExtra(XMPPService.STATUS_TYPE, statusType);
		startService(intent);
	}
	
	@Override
	public void onBackPressed() {
		back();
		//openHome();

		//Intent intent = new Intent(this, XMPPService.class);
		//intent.setAction(XMPPService.ACTION_TEST);
		//startService(intent);

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
		//onSearchRequested();
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
				addNotification(intent, 0);
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
			} else if (intent.getAction().equals(XMPPService.ACTION_GROUP_LIST)) { // Group list
				getGroupList(intent);
			} else if (intent.getAction().equals(XMPPService.ACTION_ADD_NOTIFICATION)) {

			}
		}
	}

	private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			notificationCount = 0;
			notificationMultiple = false;
			notificationChat = null;
			unregisterReceiver(this);
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unregisterReceiver(mainReceiver);
			dialog.dismiss();
			stopService(new Intent(this, XMPPService.class));	
		} catch (Exception e) {

		}
	}
	View v = null;
	@Override
	public boolean onSearchRequested() {
		Intent intent = new Intent(this, XMPPService.class);
		intent.setAction(XMPPService.ACTION_TEST);
		startService(intent);

		openHome();
		return super.onSearchRequested();
	}

	private class CheckGameTime extends Thread implements Runnable {

		@Override
		public void run() {
			Intent intent = new Intent(activity, XMPPService.class);
			intent.setAction(XMPPService.ACTION_UPDATE_ROSTER);

			boolean update;

			while (updateInGameTime) {
				try {
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
				} catch (Exception e1) {

				}	
			}
		}
	}

	// Create Fragments
	public void openFriendList() {
		actionBar.setTitle("Friend List");
		
		friendListFragment = new FriendsListFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, friendListFragment, FRAGMENT_FRIEND_LIST);
		fragmentTransaction.commit();
		updateRoster();
	}

	public void openChatList() {
		actionBar.setTitle("Chat List");
		
		chatListFragment = new ChatListFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, chatListFragment, FRAGMENT_CHAT_LIST);
		fragmentTransaction.commit();
	}

	public void openHome() {
		actionBar.setTitle("Home");
		
		homeFragment = new HomeFragment();

		Bundle b = new Bundle();
		b.putString(HomeFragment.SUMMONER_NAME, summonerName);
		homeFragment.setArguments(b);

		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, homeFragment, FRAGMENT_HOME);
		fragmentTransaction.commit();
	}

	public void openNotifications() {
		actionBar.setTitle("Notifications");
		
		notificationFragment = new NotificationFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, notificationFragment, FRAGMENT_NOTIFICATIONS);
		fragmentTransaction.commit();
	}

	public void openSettings() {
		actionBar.setTitle("Settings");
	}

	public void openLogin() {
		actionBar.setTitle("Login");
		loginFragment = new LoginFragment();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(android.R.id.content, loginFragment, FRAGMENT_LOGIN);
		fragmentTransaction.commit();
	}

	public void openChat(String chatId) {
		if (!chatData.containsKey(chatId)) return;
		
		// Clear notifications for this chat
		activeChat = chatId;
		clearMessageNotification();

		String chatName = chatData.get(chatId).name;

		actionBar.setTitle(chatName);
		
		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(chatId)) chatFragment.put(chatId,  chatTemp);
		else chatTemp = chatFragment.get(chatId);

		Bundle b = new Bundle();
		b.putString(ChatFragment.CHAT_ID, chatId);
		b.putString(ChatFragment.NAME, chatName);
		if (summoners.containsKey(chatId)) b.putSerializable(ChatFragment.TYPE, ChatFragment.Type.NORMAL);
		else b.putSerializable(ChatFragment.TYPE, ChatFragment.Type.GROUP);

		// Switch fragment

		if (fragmentManager.findFragmentByTag(FRAGMENT_FRIEND_LIST) != null) {
			previousFragment = FRAGMENT_FRIEND_LIST;
		} else if (fragmentManager.findFragmentByTag(FRAGMENT_NOTIFICATIONS) != null) {
			previousFragment = FRAGMENT_NOTIFICATIONS;
		} else {
			previousFragment = FRAGMENT_CHAT_LIST;
		}

		try {
			chatTemp.setArguments(b);
			
			fragmentTransaction = fragmentManager.beginTransaction();
			//fragmentTransaction.replace(android.R.id.content, chatTemp, "chatTag").addToBackStack("main_chat_tag");
			fragmentTransaction.replace(android.R.id.content, chatTemp, FRAGMENT_CHAT);
			fragmentTransaction.commit();	
		} catch (Exception e) {
			
		}
		
		// Enable up
		actionBar.setHomeButtonEnabled(true);
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
			openHome();
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
		try {
			chat.chatAdapter.clear();
			for (Entry<Long, ChatMessage> entry : chatMessages.get(recipient).entrySet()) {
				//Long key = (Long) entry.getKey();
				ChatMessage value = (ChatMessage) entry.getValue();
				chat.chatAdapter.add(value.message);
			}
		} catch (Exception e) {

		}

		// Add new message
		chatMessages.get(recipient).put(System.currentTimeMillis(), new ChatMessage(message, username, true));
		try {
			chat.chatAdapter.add(message);

			chat.chatView.setAdapter(chat.chatAdapter);
		} catch (Exception e) {

		}
	}

	public void onReceiveMessage(Intent intent) {		
		Bundle b = intent.getExtras();
		String user = b.getString(XMPPService.USER);
		
		// Add new message
		// Setup chatMessages
		if (!chatMessages.containsKey(user)) {
			try {
				chatMessages.put(user, new LinkedHashMap<Long, ChatMessage>());
				chatData.put(user, new ChatData(summoners.get(user).name, GroupType.NORMAL));	
			} catch (Exception e) {

			}
		}
		
		System.out.println(summoners.get(user));
		
		chatMessages.get(user).put(System.currentTimeMillis(), new ChatMessage(summoners.get(b.getString(XMPPService.USER)).name + ": " + b.getString(XMPPService.MESSAGE), b.getString(XMPPService.USER), false));

		// Send Notification
		String sender = summoners.get(b.getString(XMPPService.USER)).name;
		createNotification("New Message", sender);
		
		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(user)) {
			chatFragment.put(user, chatTemp);

			Bundle b2 = new Bundle();
			b2.putString(ChatFragment.CHAT_ID, user);
			b2.putString(ChatFragment.NAME, summoners.get(b.getString(XMPPService.USER)).name);
			b2.putSerializable(ChatFragment.TYPE, ChatFragment.Type.NORMAL);
			chatTemp.setArguments(b2);

			// update chat list fragment
			if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CHAT_LIST) != null) onChatListCreated();

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
			if (chatId.contains("pr~")) chatData.put(chatId,  new ChatData("PRIVATE CHAT", GroupType.PRIVATE));
			else chatData.put(chatId,  new ChatData("PUBLIC CHAT", GroupType.PUBLIC));
		}
		chatMessages.get(chatId).put(System.currentTimeMillis(), new ChatMessage(sender + ": " + message, chatId, false));

		ChatFragment chatTemp = new ChatFragment();
		if (!chatFragment.containsKey(chatId)) {
			chatFragment.put(chatId, chatTemp);

			Bundle b2 = new Bundle();
			b2.putString(ChatFragment.CHAT_ID, chatId);
			b2.putString(ChatFragment.NAME, chatData.get(chatId).name);
			b2.putSerializable(ChatFragment.TYPE, chatData.get(chatId).type);
			chatTemp.setArguments(b2);
			return;
		}
		else chatTemp = chatFragment.get(chatId);

		if (chatTemp.chatAdapter != null) {
			chatTemp.chatAdapter.add(sender + ": " + message);
			chatTemp.chatAdapter.notifyDataSetChanged();
		}

		addNotification(intent, 0);
	}

	public void onSendGroupInvite(Intent intent) {
		String chatId = intent.getStringExtra(XMPPService.CHAT_ID);
		dialog2.dismiss();
		try {
			if (!activeChat.equalsIgnoreCase(chatId)) {
				openChat(chatId);	
			}
		} catch (Exception e) {

		}
	}

	public void onReceiveGroupInvite(final Intent intent) {
		Bundle b = intent.getExtras();

		final String from = b.getString(XMPPService.GROUP_FROM);
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
				
				if (chatMessages.containsKey(from)) return;
				
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					System.out.println("Y");
					i.putExtra(XMPPService.INVITE_RESPONSE, true);
					dialog2 = ProgressDialog.show(activity, "Group Chat", "Joining group chat", false);
					dialog2.show();
					startService(i);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					System.out.println("N");
					i.putExtra(XMPPService.INVITE_RESPONSE, false);
					startService(i);
					break;
				case DialogInterface.BUTTON_NEUTRAL:
					System.out.println("L");
					// Put in notification
					addNotification(intent, 1);
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

		// Create invitation notification
		createNotification("New Invitation", groupName);
	}

	public void updateGroupChat(Intent intent) {
		if (dialog2 != null) dialog2.dismiss();
		
		Bundle b = intent.getExtras();
		
		if (b == null) return;
		
		String from = b.getString(XMPPService.GROUP_FROM);
		boolean response = b.getBoolean(XMPPService.INVITE_RESPONSE);
		GroupType type = (GroupType) b.get(XMPPService.GROUP_TYPE);
		String groupName = b.getString(XMPPService.GROUP_CHAT_NAME);

		if (response) {
			if (!chatMessages.containsKey(from)) chatMessages.put(from, new LinkedHashMap<Long, ChatMessage>());
			chatData.put(from, new ChatData(groupName, type));
			//System.out.println(from);
			//openChatList();
			openChat(from);
			if (type.equals(GroupType.PRIVATE)) {

			} else {

			}
		}
	}

	View groupListView = null;
	public void getGroupList(Intent intent) {
		String chatId = intent.getStringExtra(XMPPService.CHAT_ID);
		ArrayList<String> participantList = intent.getStringArrayListExtra(XMPPService.GROUPLIST);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		adapter.addAll(participantList);

		boolean showView = true;

		if (groupListView != null) {
			if (((ListView)groupListView.findViewById(R.id.listView1)).getAdapter().getCount() == participantList.size()) {
				if (groupListView.getVisibility() == View.VISIBLE) groupListView.setVisibility(View.GONE);
				else groupListView.setVisibility(View.VISIBLE);
				return;
			} else {
				if (groupListView.getVisibility() == View.VISIBLE) showView = false; 
				((FrameLayout)groupListView.getParent()).removeView(groupListView);
			}
		}
		groupListView = mLayoutInflater.inflate(R.layout.groupchat_list, null);

		((ListView)groupListView.findViewById(R.id.listView1)).setAdapter(adapter);

		View insertPoint = findViewById(android.R.id.content);

		RelativeLayout.LayoutParams lP = new RelativeLayout.LayoutParams(dp(128), dp(96));
		groupListView.setLayoutParams(lP);

		((TextView)groupListView.findViewById(R.id.textView1)).setText(String.format("Participants (%s)", participantList.size()));
		
		LinearLayout.LayoutParams linP = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		linP.gravity = Gravity.TOP | Gravity.RIGHT;
		
		((ViewGroup)insertPoint).addView(groupListView, linP);
		if (!showView) groupListView.setVisibility(View.GONE);
	}

	@SuppressLint("SimpleDateFormat")
	public void addNotification(Intent intent, int type) {
		if (fragmentManager.findFragmentByTag(FRAGMENT_CHAT) == null) activeChat = null;

		Bundle b = intent.getExtras();


		String chatId;
		String message;
		String sender;
		String title;

		long t = System.currentTimeMillis();
		// MS to Time
		Date date = new Date(t);
		SimpleDateFormat format = new SimpleDateFormat("'['HH:mm:ss']'");
		String time = format.format(date);

		switch (type) {
		case 0:
			chatId = b.getString(XMPPService.CHAT_ID);
			message = b.getString(XMPPService.MESSAGE);
			title = "New Message - " + chatData.get(chatId).name;

			// Set message
			if (b.containsKey(XMPPService.SENDER)) { // GROUP CHAT
				sender = b.getString(XMPPService.SENDER);
				message = sender + ": " + message;
			} else { // NORMAL CHAT
				sender = summoners.get(chatId).name;
			}

			message = time + " " + message;

			// Check if chat is open
			if (activeChat != null) if (activeChat.equals(chatId)) return;

			int unread = 1;

			// Check if notification exists
			for (Notification n : notificationList) {
				if (n.id.equals(chatId)) {
					unread += n.unread;
					notificationList.remove(n);
					break;
				}
			}
			notificationList.add(0, new Notification(title, message, t, type, chatId, intent, unread));

			break;
		case 1:
			String from = b.getString(XMPPService.GROUP_FROM);
			GroupType typ = (GroupType) b.get(XMPPService.GROUP_TYPE);
			String groupName = b.getString(XMPPService.GROUP_CHAT_NAME);

			// Check if notification exists
			for (Notification n : notificationList) {
				if (n.id.equals(from)) {
					notificationList.remove(n);
					break;
				}
			}

			if (typ.equals(XMPPService.GroupType.PRIVATE)) {
				title = "Private";
			} else {
				title = "Public";
			}

			title = title + " Chat Invitation";

			message = groupName;

			notificationList.add(0, new Notification(title, message, t, type, from, intent, 0));

			break;
		case 2:
			break;
		}

		onNotificationListCreated();
	}


	// Other Methods
	public void clearMessageNotification() {
		if (activeChat == null) return;

		for (Notification n : notificationList) {
			if (n.id.equals(activeChat)) {
				notificationList.remove(n);
				onNotificationListCreated();
				break;
			}
		}
	}

	class NotificationGestureListener extends SimpleOnGestureListener {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int REL_SWIPE_MIN_DISTANCE = (int)(120.0f * dm.densityDpi / 160.0f + 0.5); 
		int REL_SWIPE_MAX_OFF_PATH = (int)(250.0f * dm.densityDpi / 160.0f + 0.5);
		int REL_SWIPE_THRESHOLD_VELOCITY = (int)(200.0f * dm.densityDpi / 160.0f + 0.5);

		boolean delete = false;

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			System.out.println("SINGLE TAP");
			int position = notificationFragment.notificationListView.pointToPosition((int)e.getX(), (int)e.getY());
			Notification n = notificationList.get(position);
			switch (n.type) {
			case 0:
				openChat(n.id);
				break;
			case 1:
				onReceiveGroupInvite(n.intent);
				break;
			case 2:
				break;
			}
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) { 

			try {
				if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH) {
					return false;
				}

				position = notificationFragment.notificationListView.pointToPosition((int)e1.getX(), (int)e1.getY());
				if (position == -1) {
					position = positionOld;
				} else {
					positionOld = position;
				}

				delete = false;
				Animation anim = null;

				if(e1.getX() - e2.getX() > REL_SWIPE_MIN_DISTANCE && 
						Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
					delete = true;
					anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);

					//notificationList.remove(position);
					//onNotificationListCreated();
				}  else if (e2.getX() - e1.getX() > REL_SWIPE_MIN_DISTANCE &&
						Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
					anim = AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right);
					delete = true;
					//notificationList.remove(position);
					//onNotificationListCreated();
				}

				if (delete) {
					System.out.println("STOP");
					anim.setDuration(300);
					notificationFragment.notificationListView.getChildAt(position).startAnimation(anim);

					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							notificationList.remove(position);
							onNotificationListCreated();
						}
					}, anim.getDuration());
				}
			} catch (Exception e) {
				//e.printStackTrace();
			}



			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {

			try {
				position = notificationFragment.notificationListView.pointToPosition((int)e1.getX(), (int)e1.getY());
				if (position == -1) {
					position = positionOld;
				} else {
					positionOld = position;
				}
				//System.out.println(position + " " + e1.getX() + " " + e1.getY());


				notificationFragment.notificationListView.getChildAt(position).setX(-(e1.getX()-e2.getX()));

				//e.printStackTrace();
			} catch (Exception e) {

			}

			return true;
		}

	}

	public void inviteGroupToChat(String groupName, final List<String> groupList) {
		final Intent intent = new Intent(this, XMPPService.class);
		intent.setAction(XMPPService.ACTION_SEND_GROUP_INVITE);

		final EditText input = new EditText(MainActivity.this);  
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		input.setLayoutParams(lp);
		input.setImeActionLabel("Done", EditorInfo.IME_ACTION_NONE);
		input.setSingleLine();

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(input)
		.setMessage("Invite " + groupName + " To Chat?")
		.setPositiveButton("Invite", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String chatName = "" + input.getText();

				if (chatName.equals("")) return;

				String user = "";
				String chatId = sha1(chatName.toLowerCase());

				for (String s : groupList) {
					Summoner summoner = summoners.get(s);
					user = summoner.user;
					intent.putExtra(XMPPService.USER, intent.getStringExtra(XMPPService.USER) + "," + user);
				}

				intent.putExtra(XMPPService.CHAT_ID, "pu~" + chatId + "@lvl.pvp.net");
				intent.putExtra(XMPPService.GROUP_CHAT_NAME, chatName);

				startService(intent);

				dialog2 = ProgressDialog.show(activity, "Group Chat", "Creating group chat", false);

				dialog2.show();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void sendMessageToGroup(String groupName, final List<String> groupList) {
		final Intent intent = new Intent(activity, XMPPService.class);
		intent.setAction(XMPPService.ACTION_SEND_MESSAGE);

		final EditText input = new EditText(MainActivity.this);  
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		input.setLayoutParams(lp);
		input.setImeActionLabel("Done", EditorInfo.IME_ACTION_NONE);
		input.setSingleLine();


		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(input)//getLayoutInflater().inflate(R.layout.dialog_message, null))
		.setMessage("Send message to " + groupName)
		.setPositiveButton("Send", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String message = "" + input.getText();
				for (String s : groupList) {
					Summoner summoner = summoners.get(s);

					// Setup chat if does not exist
					if (!chatMessages.containsKey(s)) {
						try {
							chatMessages.put(s, new LinkedHashMap<Long, ChatMessage>());
							chatData.put(s, new ChatData(summoners.get(s).name, GroupType.NORMAL));	
						} catch (Exception e) {

						}
					}

					if (!chatFragment.containsKey(s)) {
						ChatFragment chatTemp = new ChatFragment();
						chatFragment.put(s, chatTemp);

						Bundle b2 = new Bundle();
						b2.putString(ChatFragment.CHAT_ID, s);
						b2.putString(ChatFragment.NAME, summoner.name);
						b2.putSerializable(ChatFragment.TYPE, ChatFragment.Type.NORMAL);
						chatTemp.setArguments(b2);
					}

					intent.putExtra(XMPPService.CHAT_ID, summoner.user); // Username
					intent.putExtra(XMPPService.NAME, summoner.name); // Recipient name
					intent.putExtra(XMPPService.MESSAGE, message); // Message
					activity.startService(intent);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public String sha1(String s) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		digest.reset();
		byte[] data = digest.digest(s.getBytes());
		return String.format("%0" + (data.length*2) + "X", new BigInteger(1, data)).toLowerCase();
	}

	@Override
	public void onFragmentCreated() {
		ActivityCompat.invalidateOptionsMenu(this);
	}

	int notificationCount = 0;
	int notifyID = 666666;
	boolean notificationMultiple = false;
	String notificationChat = null;
	boolean notificationActive = false;
	private void createNotification(String title, String message) {
		if (foreground) return;
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
		.setContentTitle(title + (notificationCount > 0 ? "s" : ""))
		.setSmallIcon(R.drawable.ic_launcher)
		.setNumber(++notificationCount)
		.setAutoCancel(true);

		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
				Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pIntent = PendingIntent.getActivity(this, notifyID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pIntent);
		
		Intent intent2 = new Intent(ACTION_NOTIFICATION_DELETE);
		PendingIntent pIntent2 = PendingIntent.getBroadcast(this, notifyID, intent2, 0);
		registerReceiver(notificationReceiver, new IntentFilter(ACTION_NOTIFICATION_DELETE));
		builder.setDeleteIntent(pIntent2);

		if (notificationCount == 1) {
			if (title.contains("New Message")) {
				notificationChat = message;
				builder.setContentText(message);
				notificationMultiple = false;
			} else {
				builder.setContentText(message);
				notificationChat = null;
				notificationMultiple = false;
			}
		} else {
			notificationMultiple = true;
			if (notificationChat != null) {
				if (notificationChat.equals(message)) {
					notificationMultiple = false;
				}
			}
		}
		
		if (notificationMultiple) {
			builder.setContentTitle("New Notifications")
			.setContentText("Click to view");
			notificationChat = null;
		}
		
		builder.setDefaults(android.app.Notification.DEFAULT_ALL);
		nm.notify(notifyID, builder.build());
	
	}
	
	public int dp(double p) {
		float scale = getResources().getDisplayMetrics().density;
		int dpAsPixels = (int) (p*scale + 0.5f);
		return dpAsPixels;
	}

	

}
