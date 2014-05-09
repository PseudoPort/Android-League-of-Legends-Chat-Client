package com.example.lolapp.xmppservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import com.example.lolapp.ChatFragment;
import com.example.lolapp.MainActivity;
import com.example.lolapp.ChatFragment.Type;
import com.example.lolapp.utils.DummySSLSocketFactory;

import android.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class XMPPService extends Service {

	public final static String ACTION_CONNECT = "action.CONNECT";
	public final static String ACTION_DISCONNECT = "action.DISCONNECT";
	public final static String ACTION_UPDATE_ROSTER = "action.UPDATE_ROSTER";
	public final static String ACTION_SEND_MESSAGE = "action.SEND_MESSAGE";
	public final static String ACTION_RECEIVE_MESSAGE = "action.RECEIVE_MESSAGE";
	public final static String ACTION_SET_AWAY_STATUS = "action.SET_AWAY_STATUS";
	public final static String ACTION_SEND_GROUP_MESSAGE = "action.SEND_GROUP_MESSAGE";
	public final static String ACTION_RECEIVE_GROUP_MESSAGE = "action.RECEIVE_GROUP_MESSAGE";
	public final static String ACTION_SEND_GROUP_INVITE = "action.SEND_GROUP_INVITE";
	public final static String ACTION_RECEIVE_GROUP_INVITE = "action.RECEIVE_GROUP_INVITE";

	public final static String ACTION_UPDATE_GROUP_CHAT = "action.UPDATE_GROUP_CHAT";

	public final static String ACTION_SET_SUMMONER_NAME = "action.SET_SUMMONER_NAME";

	public final static String ACTION_ADD_NOTIFICATION = "action.ADD_NOTIFICATION";
	
	public final static String ACTION_CHANGE_FRIEND_GROUP = "action.CHANGE_FRIEND_GROUP";
	
	public final static String ACTION_TEST = "action.TEST";

	public final static String CONNECTION_STATUS = "CONNECTION_STATUS";

	public final static String SERVICE_THREAD_NAME = "XMPP_THREAD";

	public final static String USER = "USER";
	public final static String NAME = "NAME";
	public final static String STATUS = "STATUS";
	public final static String MODE = "MODES";
	public final static String GROUP = "GROUPS";
	public final static String GROUPLIST = "GROUPLIST";
	public final static String MESSAGE = "MESSAGE";
	public final static String TIMESTAMP = "TIMESTAMP";
	public final static String IS_ONLINE = "IS_ONLINE";

	public final static String CHAT_ID = "CHAT_ID";

	public final static String GROUP_FROM = "GROUP_FROM";
	public final static String GROUP_TYPE = "GROUP_TYPE";
	public final static String GROUP_CHAT_NAME = "GROUP_CHAT_NAME";

	public final static String SENDER = "SENDER";

	public final static String INVITE_RESPONSE = "INVITE_RESPONSE";
	
	public enum GroupType {
		NORMAL, PRIVATE, PUBLIC
	}

	private static volatile Looper sServiceLooper;
	private static volatile ServiceHandler sServiceHandler;
	private long mHandlerThreadId;

	// VARIABLES
	XMPPConnection connection = null;

	Roster roster = null;
	Collection<RosterEntry> entries = null;
	Collection<RosterGroup> groups = null;

	String summonerName;


	// Multi-user Chat
	HashMap<String, MultiUserChat> muc = new HashMap<String, MultiUserChat>();

	// Make sure no duplicates from self
	ArrayList<String> sentChatIds = new ArrayList<String>();


	// Get Name Function
	boolean hasName = false;
	String packetId = null;
	String getNameChatName = null;
	String getNameBody = null;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			onHandleIntent((Intent) msg.obj, msg.what);
		}
	}

	void onHandleIntent(final Intent intent, int id) {
		//System.out.println("ON HANDLE INTENT");
		// ensure XMPP manager is setup (but not yet connected)
		if (Thread.currentThread().getId() != mHandlerThreadId) {
			throw new IllegalThreadStateException();
		}

		String action;
		try {
			action = intent.getAction();
		} catch (Exception e) {
			Log.e("ERROR", "No Action on Intent - STOPPING SERVICE");
			if (connection == null) {
				stopSelf();
				Log.e("ERROR", "STOPPED");
			}
			return;
		}

		if (action.equals(ACTION_CONNECT)) {
			new Connect().execute(intent.getStringExtra("HOST"), intent.getStringExtra("PORT"), intent.getStringExtra("SERVICE"),  intent.getStringExtra("USERNAME"), intent.getStringExtra("PASSWORD"));
		} else if (action.equals(ACTION_DISCONNECT)) {
			new Disconnect().execute();
		} else if (action.equals(ACTION_UPDATE_ROSTER)) {
			if (connection == null) System.out.println("CONNECTION NULL");
			else updateRoster();
		} else if (action.equals(ACTION_SEND_MESSAGE)) {
			sendMessage(intent);
		} else if (action.equals(ACTION_SET_AWAY_STATUS)) {
			new SetAwayStatus().execute(System.currentTimeMillis());
		} else if (action.equals(ACTION_RECEIVE_GROUP_INVITE)) { // On receive invite
			inviteResponse(intent);
		} else if (action.equals(ACTION_SEND_GROUP_MESSAGE)) {
			sendGroupMessage(intent);
		} else if (action.equals(ACTION_SEND_GROUP_INVITE)) {
			sendGroupInvite(intent);
		} else if (action.equals(ACTION_CHANGE_FRIEND_GROUP)) {
			changeFriendGroup(intent);
		} else if (action.equals(ACTION_TEST)) {
			test(intent);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//System.out.println("ONSTARTCOMMAND");
		// Start a new thread for the service
		HandlerThread thread = new HandlerThread(SERVICE_THREAD_NAME);
		thread.start();
		mHandlerThreadId = thread.getId();
		sServiceLooper = thread.getLooper();
		sServiceHandler = new ServiceHandler(sServiceLooper);

		// Intent message
		android.os.Message msg = new android.os.Message();
		msg.obj = intent;
		sServiceHandler.sendMessage(msg);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	private class Connect extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			//System.out.println("STARTING CONNECT");
			boolean success;
			ConnectionConfiguration connConfig = new ConnectionConfiguration(params[0], Integer.parseInt(params[1]), params[2]);
			connConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
			connConfig.setSocketFactory(new DummySSLSocketFactory());
			connection = new XMPPConnection(connConfig);

			success = true;

			// Connect to server
			try {
				//Connect to the server
				connection.connect();
				configure(ProviderManager.getInstance());
				System.out.println("CONNECTED");
			} catch (XMPPException ex) {
				connection = null;
				System.out.println("FAILED" + " - " + ex.getMessage());
				//Unable to connect to server
				success = false;
			}

			// Try Login
			try {
				connection.login(params[3], "AIR_" + params[4], "wotomolon");
				Log.i("XMPPStatus",  "Logged in as " + connection.getUser());
				setMessageListener();
				Presence presence = new Presence(Presence.Type.available);
				connection.sendPacket(presence);
				Thread getSumName = new GetSummonerName();
				getSumName.start();
				//new ConnectionTest().start();
			} catch (Exception ex) {
				Log.e("XMPPStatus", "Failed to log in as "+  "USER");
				Log.e("XMPPStatus", ex.toString());
				success = false;
			}

			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			//System.out.println("RESULT = " + result);
			Intent intent = new Intent();
			intent.setAction(ACTION_CONNECT);
			intent.putExtra(CONNECTION_STATUS, result);
			if (result) {
				intent.putExtra(USER, connection.getUser());
			}
			sendBroadcast(intent);
		}

	}

	private class GetSummonerName extends Thread implements Runnable {
		long timeout = 1000;

		@Override
		public void run() {
			String user = connection.getUser();
			String sendTo = user.split("@")[0] + "@conference.pvp.net";
			getNameChatName = sendTo;
			String r = String.valueOf(System.currentTimeMillis());
			getNameBody = r;
			MultiUserChat tempChat = null;
			boolean firstRun = true;
			while (!hasName) {
				tempChat = new MultiUserChat(connection, sendTo);
				if (firstRun) {
					firstRun = false;
				} else {
					try {
						sleep(timeout);
					} catch (InterruptedException e) {
					}
				}
				try {
					tempChat.join(user);
				} catch (XMPPException e) {
				}
				Message msg = new Message(sendTo, Message.Type.groupchat);
				msg.setBody(r);
				packetId = msg.getPacketID();
				if (hasName) break;
				connection.sendPacket(msg);
			}
			tempChat.leave();
		}


	}

	private class Disconnect extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			connection.disconnect();
			return null;
		}

	}

	private class SetAwayStatus extends AsyncTask<Long, Void, Void> {

		Presence p = new Presence(Presence.Type.available);

		@Override
		protected Void doInBackground(Long... params) {
			long start = 0;
			while (System.currentTimeMillis() - params[0] < 60000) {
				System.out.println("LOOP TIME" + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
				p.setStatus("<body>"
						+ "<profileIcon>22</profileIcon>"
						+ "<level>0</level>"
						+ "<wins>0</wins>"
						+ "<leaves>0</leaves>"
						+ "<odinWins>0</odinWins>"
						+ "<odinLeaves>0</odinLeaves>"
						+ "<queueType />"
						+ "<rankedLosses>0</rankedLosses>"
						+ "<rankedRating>0</rankedRating>"
						+ "<tier></tier>"
						+ "<rankedLeagueName></rankedLeagueName>"
						+ "<rankedLeagueDivision></rankedLeagueDivision>"
						+ "<rankedLeagueTier></rankedLeagueTier>"
						+ "<rankedLeagueQueue></rankedLeagueQueue>"
						+ "<rankedWins>0</rankedWins>"
						+ "<statusMsg>" + (int)((System.currentTimeMillis() - params[0])/1000) + "</statusMsg>"
						+ "<gameStatus>outOfGame</gameStatus>"
						+ "</body>");
				System.out.println("SETUP TIME" + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
				connection.sendPacket(p);
				System.out.println(System.currentTimeMillis() - start);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				start = System.currentTimeMillis();
			}
			return null;
		}

	}

	public void setMessageListener() {
		if (connection != null) {
			// Add a packet listener to get messages sent to us (NORMAL MESSAGES)
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			connection.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					receiveChatMessage(packet);
				}
			}, filter);


			// GROUP MESSAGE/INVITES
			PacketFilter filter2 = new MessageTypeFilter(Message.Type.normal); // Chat Invites?
			connection.addPacketListener(new PacketListener() {

				@Override
				public void processPacket(Packet packet) { // Invite Received 
					System.out.println("NORMAL - " + packet.toXML());

					if (!packet.toXML().contains("jabber:x:conference")) return;

					//System.out.println("FROM - " + packet.getFrom());
					muc.put(packet.getFrom(), new MultiUserChat(connection, packet.getFrom()));

					Intent intent = new Intent();
					intent.setAction(ACTION_RECEIVE_GROUP_INVITE);
					intent.putExtra(GROUP_FROM, packet.getFrom());
					intent.putExtra(TIMESTAMP, System.currentTimeMillis());
					if (packet.getFrom().contains("pr~")) intent.putExtra(GROUP_TYPE, GroupType.PRIVATE);
					else if (packet.getFrom().contains("pu~")) intent.putExtra(GROUP_TYPE, GroupType.PUBLIC);

					// Parse chat name
					String chatName = "";

					String packetXML = packet.toXML();

					if (packet.getFrom().contains("pr~")) {
						int start = packetXML.indexOf("subject\":\"") + "subject\":\"".length();
						int end = packetXML.indexOf("\"}");
						chatName = packetXML.substring(start, end);
					} else if (packet.getFrom().contains("pu~")) { // FIX HERE
						//chatName = "PUBLIC CHAT";
						int start = packetXML.indexOf("subject\":\"") + "subject\":\"".length();
						int end = packetXML.indexOf("\"}");
						chatName = packetXML.substring(start, end);
					}

					intent.putExtra(GROUP_CHAT_NAME, chatName);

					intent.putExtra(ChatFragment.TYPE, ChatFragment.Type.GROUP);

					sendBroadcast(intent);

					muc.get(packet.getFrom()).addParticipantListener(new PacketListener() { 

						@Override
						public void processPacket(Packet packet) { // UPDATE CHAT
							System.out.println("PACKETLISTENER - " + packet.toXML());
						}
					});
					muc.get(packet.getFrom()).addPresenceInterceptor(new PacketInterceptor() { // Group chat presence updated

						@Override
						public void interceptPacket(Packet packet) {
							System.out.println("PRESENCEINTERCEPTOR - " + packet.toXML());
						}
					});
				}

			}, filter2);			
			PacketFilter filter3 = new MessageTypeFilter(Message.Type.groupchat);
			connection.addPacketListener(new PacketListener() {

				@Override
				public void processPacket(Packet packet) { // Group Messages
					// Get Summoner Name
					if (packet.getPacketID() != null) {
						if (packet.getPacketID().equals(packetId) && !hasName) {
							if (packet.getFrom().contains(getNameChatName) && ((Message)packet).getBody().equals(getNameBody)) {
								hasName = true;
								System.out.println(packet.getFrom().split("/")[1]);
								summonerName = packet.getFrom().split("/")[1];
								Intent intent = new Intent();
								intent.setAction(ACTION_SET_SUMMONER_NAME);
								intent.putExtra(NAME, summonerName);
								sendBroadcast(intent);

								return;
							}
						}
					}
					if (packet.getFrom().equals(getNameChatName) || packet.getPacketID() == null) return;

					////////////////////////////////////////////////////////////////////

					// Filter out sent messages
					if (sentChatIds.contains(packet.getPacketID())) {
						sentChatIds.remove(packet.getPacketID());
						return;
					}


					// Receive group messages
					//System.out.println("GROUPCHAT - " + packet.toXML());

					Message message = (Message) packet;
					if (message.getBody() != null) {
						String fromName = StringUtils.parseBareAddress(message.getFrom());

						Log.i("XMPPChat ", "Group Text Recieved " + message.getBody() + " from " +  StringUtils.parseResource(message.getFrom()));
						//System.out.println(message.getFrom()); // Chat Id
						//System.out.println(fromName); // Chat Id
						//System.out.println(StringUtils.parseResource(message.getFrom())); // SUMMONER NAME

						Intent intent = new Intent();
						intent.setAction(ACTION_RECEIVE_GROUP_MESSAGE);
						intent.putExtra(CHAT_ID, fromName); // CHAT ID
						intent.putExtra(MESSAGE, message.getBody()); // MESSAGE
						intent.putExtra(SENDER, StringUtils.parseResource(message.getFrom())); // SUMMONER NAME
						sendBroadcast(intent);
					}




					//Iterator<String> i = muc.get(packet.getFrom().split("/")[0]).getOccupants();
					//while (i.hasNext()) {
					//System.out.println(i.next()); pr~5cfb1678515568c85f8cdea1a8938329f4b4a4e8@conference.pvp.net/YOSOYSATANAS666
					//}
					//muc.get(packet.getFrom().split("/")[0]);
					//System.out.println(muc.get(packet.getFrom().split("/")[0]).getOccupantsCount());
					//connection.sendPacket(m);
				}

			}, filter3);

			PacketFilter filter4 = new MessageTypeFilter(Message.Type.headline);
			connection.addPacketListener(new PacketListener() {

				@Override
				public void processPacket(Packet packet) { // Headline
					System.out.println("HEADLINE - " + packet.toXML());
				}
			}, filter4);
		}
	}

	public void addRosterListener(Roster roster) {
		roster.addRosterListener(new RosterListener() {

			@Override
			public void entriesAdded(Collection<String> arg0) {
				updateRoster();
				System.out.println("Entries Added");
			}

			@Override
			public void entriesDeleted(Collection<String> arg0) {
				updateRoster();
				System.out.println("Entries Deleted");
			}

			@Override
			public void entriesUpdated(Collection<String> arg0) {
				updateRoster();
				System.out.println("Entries Updated");
			}

			@Override
			public void presenceChanged(Presence arg0) {
				//System.out.println("UPDATING ROSTER");
				//System.out.println(arg0.getFrom());
				//System.out.println(arg0.toXML());
				updateRoster();
			}

		});
	}

	public void updateRoster() {
		ArrayList<String> users = new ArrayList<String>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> statuses = new ArrayList<String>();
		ArrayList<String> modes = new ArrayList<String>();
		ArrayList<String> friendgroups = new ArrayList<String>();
		ArrayList<String> grouplist = new ArrayList<String>();
		ArrayList<String> isOnline = new ArrayList<String>();

		if (roster == null) {
			roster = connection.getRoster();
			addRosterListener(roster);
		}
		entries = roster.getEntries();

		String groupName;

		// Get groups
		groups = roster.getGroups();
		for (RosterGroup group : groups) {
			//Log.d("XMPPChat - GROUP", group.getName());
			groupName = group.getName();
			if (groupName.equals("**Default")) groupName = "General";
			grouplist.add(groupName);
		}
		grouplist.add("Offline");

		for (RosterEntry entry : entries) {
			Presence entryPresence = roster.getPresence(entry.getUser());

			users.add(entry.getUser()); //sumID@pvp.net
			names.add(entry.getName()); //NAME

			try {
				if (entryPresence.toString().equals("unavailable")) {
					statuses.add("Offline");
				} else {
					statuses.add(entryPresence.getStatus().toString()); // COMPLETE STATUS
				}
			} catch (Exception e) {
				if (entryPresence.toString().equals("available")) statuses.add("Online");
				else statuses.add("");
			}
			try {
				modes.add(entryPresence.getMode().toString()); // chat/away/dnd
			} catch (Exception e) {
				modes.add("null");
			}

			try {
				groupName = entry.getGroups().iterator().next().getName();
				if (groupName.equals("**Default")) groupName = "General";
			} catch (Exception e) {
				System.out.println("ERROR: " + entry);
				groupName = "null";
				if (!grouplist.contains("null")) grouplist.add("null");
			}

			if (entryPresence.toString().equals("unavailable")) {
				groupName = "Offline";
				isOnline.add("false");
			} else {
				isOnline.add("true");
			}

			friendgroups.add(groupName); // group
		}

		/*
    	Presence p = new Presence(Presence.Type.available);
    	p.setStatus("<body>"
    			+ "<profileIcon>22</profileIcon>"
    			+ "<level>0</level>"
    			+ "<wins>0</wins>"
    			+ "<leaves>0</leaves>"
    			+ "<odinWins>0</odinWins>"
    			+ "<odinLeaves>0</odinLeaves>"
    			+ "<queueType />"
    			+ "<rankedLosses>0</rankedLosses>"
    			+ "<rankedRating>0</rankedRating>"
    			+ "<tier></tier>"
    			+ "<rankedLeagueName></rankedLeagueName>"
    			+ "<rankedLeagueDivision></rankedLeagueDivision>"
    			+ "<rankedLeagueTier></rankedLeagueTier>"
    			+ "<rankedLeagueQueue></rankedLeagueQueue>"
    			+ "<rankedWins>0</rankedWins>"
    			+ "<statusMsg>abc</statusMsg>"
    			+ "<gameStatus>outOfGame</gameStatus>"
    			+ "</body>");
    	connection.sendPacket(p);
		 */

		// Send broadcast
		Intent intent = new Intent();
		intent.setAction(ACTION_UPDATE_ROSTER);
		intent.putExtra(USER, users);
		intent.putExtra(NAME, names);
		intent.putExtra(STATUS, statuses);
		intent.putExtra(MODE, modes);
		intent.putExtra(GROUP, friendgroups);
		intent.putExtra(GROUPLIST, grouplist);
		intent.putExtra(IS_ONLINE, isOnline);
		sendBroadcast(intent);
	}

	public void sendMessage2(RosterEntry entry, String message) {
		Message msg = new Message(entry.getUser(), Message.Type.chat);  
		msg.setBody(message);
		if (connection != null) {
			connection.sendPacket(msg);

			Intent intent = new Intent();
			intent.setAction(ACTION_SEND_MESSAGE);
			intent.putExtra(USER, entry.getUser());
			intent.putExtra(NAME, entry.getName());
			intent.putExtra(MESSAGE, message);
			intent.putExtra(TIMESTAMP, System.currentTimeMillis());
			sendBroadcast(intent);
		}
	}

	public void sendMessage(Intent intent) {
		String user = intent.getExtras().getString(ChatFragment.CHAT_ID);
		//String name = intent.getExtras().getString(ChatFragment.NAME);
		String message = intent.getExtras().getString(ChatFragment.MESSAGE);

		sendMessage2(roster.getEntry(user), message);
	}

	public void sendGroupMessage(Intent intent) {
		String chatId = intent.getExtras().getString(ChatFragment.CHAT_ID); // xxx@xxx.pvp.net
		String message = intent.getExtras().getString(ChatFragment.MESSAGE); // xxx
		String name = intent.getExtras().getString(ChatFragment.NAME); // xxx's chat room
		long time = System.currentTimeMillis();
		Intent i = new Intent();
		i.setAction(ACTION_SEND_GROUP_MESSAGE);
		i.putExtra(TIMESTAMP, time);
		i.putExtra(NAME, name);
		i.putExtra(MESSAGE, message);
		i.putExtra(CHAT_ID, chatId);

		Message msg = new Message(chatId, Message.Type.groupchat);
		msg.setBody(message);

		sentChatIds.add(msg.getPacketID());

		connection.sendPacket(msg);

		sendBroadcast(i);
	}
	
	// Send group chat invite
	// TODO: Check if chat exists, then invite users (may need to create chat in main activity?)
	public void sendGroupInvite(Intent intent) {
		// Invite to chat - PU/PR
		String user = intent.getStringExtra(USER);
		String chatId = intent.getStringExtra(CHAT_ID); // pu/pr~asddsadasdasddsas@lvl.pvp.net/conference.pvp.net
		String chatName = intent.getStringExtra(GROUP_CHAT_NAME); // MY CHAT ROOM
		
		MultiUserChat chat = null;
		if (muc.containsKey(chatId)) { // Chat exists!
			//System.out.println("EXISTS");
			chat = muc.get(chatId);
			
			if (chat.getOccupantsCount() == 0) {
				try {
					chat.join(connection.getUser());
				} catch (XMPPException e) {
					//e.printStackTrace();
				}
			}
		} else { // Create chat, then invite
			//System.out.println("NEW!");
			chat = new MultiUserChat(connection, chatId);
			muc.put(chatId, chat);
			chat = muc.get(chatId);
			
			try {
				chat.join(connection.getUser());
			} catch (XMPPException e) {
				//e.printStackTrace();
			}
			
			// Join room
			if (muc.get(chatId).getOccupantsCount() > 0) {
				// Send broadcast to add group chat
				Intent i = new Intent();
				i.setAction(ACTION_UPDATE_GROUP_CHAT);
				i.putExtra(GROUP_FROM, chatId); // Id
				i.putExtra(INVITE_RESPONSE, true);
				i.putExtra(GROUP_TYPE, GroupType.PUBLIC);
				i.putExtra(GROUP_CHAT_NAME, chatName);
				i.putExtra(ChatFragment.TYPE, ChatFragment.Type.GROUP);
				sendBroadcast(i);
			}
		}
		
		// Invite user
		chat.invite(user, "{\"message\":\"Please join my group chat!\",\"type\":\"pu\",\"subject\":\""+chatName+"\"}");
		
		/*
		
		System.out.println(muc2.getOccupantsCount());
		*/
		
		//muc2.invite("sum54559857@pvp.net", "{\"message\":\"Please join my group chat!\",\"type\":\"pu\",\"subject\":\"YOSOYSATANAS666's Chat Room\"}");
		//muc2.invite("sum20459570@pvp.net", "{\"message\":\"Please join my group chat!\",\"type\":\"pu\",\"subject\":\"YOSOYSATANAS666's Chat Room\"}");
	}
	
	// Join/Decline Chat
	public void inviteResponse(Intent intent) {
		Bundle b = intent.getExtras();
		boolean inviteResponse = b.getBoolean(INVITE_RESPONSE);
		String from = b.getString(GROUP_FROM);
		GroupType type = (GroupType) b.get(GROUP_TYPE);
		String groupName = b.getString(GROUP_CHAT_NAME);
		Type t = (Type) b.getSerializable(ChatFragment.TYPE);

		if (inviteResponse) {
			// Join room
			try {
				muc.get(from).join(connection.getUser());
			} catch (XMPPException e) {

			}

			if (muc.get(from).getOccupantsCount() > 0) {
				// Send broadcast to add group chat
				Intent i = new Intent();
				i.setAction(ACTION_UPDATE_GROUP_CHAT);
				i.putExtra(GROUP_FROM, from);
				i.putExtra(INVITE_RESPONSE, inviteResponse);
				i.putExtra(GROUP_TYPE, type);
				i.putExtra(GROUP_CHAT_NAME, groupName);
				i.putExtra(ChatFragment.TYPE, t);
				sendBroadcast(i);
			}
		} else {
			// Leave room
			muc.get(from).leave();
			muc.remove(from);
		}
	}

	// Change friend group
	public void changeFriendGroup(Intent intent) {
		String summonerId = intent.getStringExtra(USER);
		String groupName = intent.getStringExtra(GROUP);
		
		if (groupName.equals("General")) {
			groupName = "**Default";
		}
		
		RosterEntry re = roster.getEntry(summonerId); // cannot add to empty group
		try {
			roster.getGroup(groupName).addEntry(re);
			updateRoster();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}
	
	
	// TEST METHOD
	public void test(Intent intent) {
		String user = intent.getStringExtra("NAME");

		/* Invite to chat - PU/PR
		MultiUserChat muc2 = new MultiUserChat(connection, "pu~5cfb1678515568c85f8cdea1a8938329f4b4a4e8@lvl.pvp.net");
		try {
			muc2.join(user);
		} catch (XMPPException e1) {
			e1.printStackTrace();
		}
		muc2.invite("sum54559857@pvp.net", "{\"message\":\"Please join my group chat!\",\"type\":\"pu\",\"subject\":\"YOSOYSATANAS666's Chat Room\"}");
		muc2.invite("sum20459570@pvp.net", "{\"message\":\"Please join my group chat!\",\"type\":\"pu\",\"subject\":\"YOSOYSATANAS666's Chat Room\"}");
		 */

		/*
		Roster r = connection.getRoster();
		RosterEntry re = r.getEntry("sum54559857@pvp.net");
		/*try {
			roster.getGroup("Buddies").addEntry(re);
			System.out.println("ADDING TO BUDDIES GROUP");

		} catch (Exception e2) {
			e2.printStackTrace();
		}*/
		/*
		try {
			r.createGroup("TEST GROUP");
		} catch (IllegalArgumentException e1) {
			System.out.println("GROUP ALREADY EXISTS!");
		}

		for (RosterGroup g : r.getGroups()) {
			System.out.println(g.getName());
		}
		 */

		//Presence p = new Presence(Presence.Type.available);
		//connection.sendPacket(p);

		if (connection == null) System.out.println("NULL");
		else System.out.println("NOT NULL");

		if (connection.isConnected()) System.out.println("CON");
		else System.out.println("NOT CON");
		System.out.println(connection.isSocketClosed());
		System.out.println(connection.getConnectionID());
		System.out.println(connection.toString());

		

	}

	// Check connection status
	private class ConnectionTest extends Thread implements Runnable {

		@Override
		public void run() {
			while (true) {
				if (connection == null) {
					System.out.println("CONNECTION NULL!!");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	public void receiveChatMessage(Packet packet) {
		Message message = (Message) packet;
		if (message.getBody() != null) {
			String fromName = StringUtils.parseBareAddress(message.getFrom());
			Log.i("XMPPChat ", "Text Recieved " + message.getBody() + " from " +  fromName);
			//System.out.println(message.getFrom());
			//System.out.println(fromName);
			//System.out.println(StringUtils.parseBareAddress(connection.getUser()));
			//System.out.println(message.toXML());

			Intent intent = new Intent();
			intent.setAction(ACTION_RECEIVE_MESSAGE);
			intent.putExtra(USER, fromName);
			intent.putExtra(CHAT_ID, fromName);
			intent.putExtra(MESSAGE, message.getBody());
			sendBroadcast(intent);
		}
	}

	public void addMessageNotification(String fromName, String message) {
		Intent intent = new Intent();
		intent.setAction(ACTION_ADD_NOTIFICATION);
		intent.putExtra(USER, fromName);
		intent.putExtra(MESSAGE, message);
		sendBroadcast(intent);
	}





























	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void configure(ProviderManager pm) {

		//  Private Data Storage
		pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

		//  Time
		try {
			pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
			Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		//  Roster Exchange
		pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

		//  Message Events
		pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());

		//  Chat State
		pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 
		pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

		//  XHTML
		pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

		//  Group Chat Invitations
		pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

		//  Service Discovery # Items    
		pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

		//  Service Discovery # Info
		pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

		//  Data Forms
		pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

		//  MUC User
		pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

		//  MUC Admin    
		pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

		//  MUC Owner    
		pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

		//  Delayed Delivery
		pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());

		//  Version
		try {
			pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			//  Not sure what's happening here.
		}

		//  VCard
		pm.addIQProvider("vCard","vcard-temp", new VCardProvider());

		//  Offline Message Requests
		pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

		//  Offline Message Indicator
		pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

		//  Last Activity
		pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());

		//  User Search
		pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());

		//  SharedGroupsInfo
		pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

		//  JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());

		//   FileTransfer
		pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());

		pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

		//  Privacy
		pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
		pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
	}
}
