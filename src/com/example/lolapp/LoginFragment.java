package com.example.lolapp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.example.lolapp.FriendsListFragment.OnFragmentCreatedListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class LoginFragment extends Fragment {

	public static final String FILENAME = "login_file";

	public static final String HOST = "chat.na1.lol.riotgames.com";
	public static final int PORT = 5223;
	public static final String SERVICE = "pvp.net";

	// Views
	TextView loginTitle, usernameLabel, passwordLabel;
	EditText usernameText, passwordText;
	CheckBox rememberLogin, loginAutomatically;
	Button loginButton;
	
	Context context;

	// Variables for activity communication
	OnLoginListener mCallback;

	// VARIABLES

	String error;
	Boolean success;

	Thread loginThread;
	
	Intent serviceIntent;
	
	// Interface
	OnFragmentCreatedListener mFragmentCallback;
	public interface OnFragmentCreatedListener {
		public void onFragmentCreated();
	}
	
	public interface OnLoginListener {
		public void onLogin(String username, String password, boolean remember, boolean automatic);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mCallback = (OnLoginListener) activity;
		mFragmentCallback = (OnFragmentCreatedListener) activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Get context
		context = view.getContext();
		
		// Setup Views
		loginTitle = (TextView) view.findViewById(R.id.loginTitle);
		usernameLabel = (TextView) view.findViewById(R.id.usernameLabel);
		passwordLabel = (TextView) view.findViewById(R.id.passwordLabel);

		usernameText = (EditText) view.findViewById(R.id.usernameText);
		passwordText = (EditText) view.findViewById(R.id.passwordText);

		rememberLogin = (CheckBox) view.findViewById(R.id.rememberLogin);
		loginAutomatically = (CheckBox) view.findViewById(R.id.loginAutomatically);

		loginButton = (Button) view.findViewById(R.id.loginButton);

		// add listeners
		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String username = usernameText.getText().toString();
				String password = passwordText.getText().toString();
				login(username, password);
			}
		});
		
		// Read login data
		FileInputStream fis;
		try {
			fis = view.getContext().openFileInput(FILENAME);
			InputStreamReader in = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(in);

			boolean saveLogin = Boolean.parseBoolean(br.readLine());
			boolean autoLogin = Boolean.parseBoolean(br.readLine());
			String username = br.readLine();
			String password = br.readLine();

			br.close();
			in.close();
			fis.close();

			rememberLogin.setChecked(saveLogin);
			loginAutomatically.setChecked(autoLogin);

			if (saveLogin) {
				usernameText.setText(username);
				passwordText.requestFocus();
			}
			if (autoLogin) {
				passwordText.setText(password);
				loginButton.requestFocus();
				login(username, password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mFragmentCallback.onFragmentCreated();
	}

	public void login(final String un, final String pw) {
		boolean remember = rememberLogin.isChecked();
		boolean automatic = loginAutomatically.isChecked();
		mCallback.onLogin(un, pw, remember, automatic);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_login, container,
				false);
		return rootView;
	}
}
