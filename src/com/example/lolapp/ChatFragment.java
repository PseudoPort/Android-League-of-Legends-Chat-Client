package com.example.lolapp;

import com.example.lolapp.xmppservice.XMPPService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ChatFragment extends Fragment {
	public static final String CHAT_ID = "CHAT_ID";
	public static final String NAME = "NAME";
	public static final String MESSAGE = "MESSAGE";
	public static final String TYPE = "TYPE";
	
	public enum Type {
		NORMAL, GROUP
	}
	
	ListView chatView = null;
	EditText messageText;
	Button sendButton;
	
	ArrayAdapter<String> chatAdapter = null;
	
	String chatId, name;
	
	// Type
	Type type = null;
	
	Activity activity;
	OnCreateListener mCallback;
	
	// Interface
	public interface OnCreateListener {
		public void onCreate(String user);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mCallback = (OnCreateListener) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		chatId = getArguments().getString(CHAT_ID);
		name = getArguments().getString(NAME);
		type = (Type) getArguments().getSerializable(TYPE);
		
		
		chatView = (ListView) view.findViewById(R.id.chatView);
		chatAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1);
		
		messageText = (EditText) view.findViewById(R.id.messageText);
		sendButton = (Button) view.findViewById(R.id.sendButton);
		
		sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String message = messageText.getText().toString();
				if (message.length() <= 0) return;
				messageText.setText("");
				
				Intent intent = new Intent(activity, XMPPService.class);
				if (type == Type.NORMAL) intent.setAction(XMPPService.ACTION_SEND_MESSAGE);
				else if (type == Type.GROUP) intent.setAction(XMPPService.ACTION_SEND_GROUP_MESSAGE);
				else return;
				
				intent.putExtra(CHAT_ID, chatId);
				intent.putExtra(NAME, name);
				intent.putExtra(MESSAGE, message);
				activity.startService(intent);
				
			}
		});
		
		messageText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String message = messageText.getText().toString();
				if (message.length() <= 0) return false;
				messageText.setText("");
				
				Intent intent = new Intent(activity, XMPPService.class);
				intent.setAction(XMPPService.ACTION_SEND_MESSAGE);
				intent.putExtra(CHAT_ID, chatId);
				intent.putExtra(NAME, name);
				intent.putExtra(MESSAGE, message);
				activity.startService(intent);
				return true;
			}
		});
		
		mCallback.onCreate(chatId);
		
		activity.setTitle(name);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_chat, container,
				false);
		return rootView;
	}
}
