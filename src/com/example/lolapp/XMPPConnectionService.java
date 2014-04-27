package com.example.lolapp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.example.lolapp.utils.DummySSLSocketFactory;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class XMPPConnectionService extends IntentService{
	
	static final String INTENT_TYPE = "INTENT_TYPE";
	static final int MESSAGE_LOGIN = 0;
	
	XMPPConnection connection = null;
	ConnectionConfiguration connConfig;
	String HOST, SERVICE, username, password;
	int PORT;
	
	XMPPThread mThread;
	Handler mHandler;

	public XMPPConnectionService() {
		super("XMPP_SERVICE");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle b = intent.getExtras();
		//username = b.getString("USERNAME");
		//password = b.getString("PASSWORD");
		//HOST = b.getString("HOST");
		//PORT = b.getInt("PORT");
		//SERVICE = b.getString("SERVICE");
		
		Log.d("SERVICE", "SERVICE CREATED! - " + PORT);
		
		// Initialize XMPP Connection
		mThread = new XMPPThread();
		mThread.start();
		
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("INTENT", "INTENT HANDLED");
		/*
		while (mThread.mHandler == null) {}
		mHandler = mThread.mHandler;
		
		Bundle mBundle = intent.getExtras();
		int type = mBundle.getInt(INTENT_TYPE);
		
		Message msg = new Message();
		Bundle b = new Bundle();		
		
		switch (type) {
		case MESSAGE_LOGIN:
			msg.what = type;
			mHandler.sendMessage(msg);
		}*/
	}
	
	private class XMPPThread extends Thread {
		public Handler mHandler;
		
		public void run() {
			Looper.prepare();
			mHandler = new Handler() {

				@Override
				public void handleMessage(Message msg) {
					processMessage(msg);
				}
				
			};
			Looper.loop();
		}
		
		private void processMessage(Message msg) {
			Bundle b = msg.getData();
			switch (msg.what) {
			case MESSAGE_LOGIN:
				login();
			}
		}
		
		private void login() {
			connConfig = new ConnectionConfiguration(HOST, PORT, SERVICE);
			connConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
			connConfig.setSocketFactory(new DummySSLSocketFactory());
			connection = new XMPPConnection(connConfig);
			
			boolean success = true;
			
			try {
				//Connect to the server
				connection.connect();
				System.out.println("CONNECTED");
			} catch (XMPPException ex) {
				connection = null;
				System.out.println("FAILED" + " - " + ex.getMessage());
				//Unable to connect to server
				success = false;
			}

		}
	}	
}
