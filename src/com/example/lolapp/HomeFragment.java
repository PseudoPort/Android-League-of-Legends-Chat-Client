package com.example.lolapp;

import com.example.lolapp.FriendsListFragment.OnFragmentCreatedListener;
import com.example.lolapp.FriendsListFragment.OnFriendChatClickListener;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class HomeFragment extends Fragment {
	
	public final static String SUMMONER_NAME = "SUMMONER_NAME";
	
	public static final int LEFT = 0;
	public static final int MIDDLE = 1;
	public static final int RIGHT = 2;
	
	public static final String[] ONLINE_STATUSES = {
		"None",
		"Invite to game!",
		"Just Online",
		"Can play for ____",
		"__________"
	};
	
	public static final String[] AWAY_STATUSES = {
		"None",
		"Can't play right now",
		"AFK",
		"Gone for ____",
		"__________"
	};
	
	public static final String[] GAME_STATUSES = {
		"None",
		"Just started",
		"Almost done",
		"Done in ____",
		"__________"
	};
	
	// Variables
	String summonerName = null;
	
	// Views
	TextView summonerText;
	TextView summonerStatus;
	
	ImageView imageLeft, imageMiddle, imageRight;
	ImageView[] imageStatus = new ImageView[3];
	
	RadioButton[] radioButton = new RadioButton[5];
	
	// Status
	int selectedStatus = 1;
	
	Activity activity;
	
	View view;
	
	// Interface
	OnFragmentCreatedListener mFragmentCallback;
	public interface OnFragmentCreatedListener {
		public void onFragmentCreated();
	}
	
	StatusClickListener mStatusCallback;
	public interface StatusClickListener {
		public void onStatusClicked(String statusText, String statusType);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mFragmentCallback = (OnFragmentCreatedListener) activity;
		mStatusCallback = (StatusClickListener) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		this.view = view;
		
		// Setup Summoner Name
		summonerName = getArguments().getString(SUMMONER_NAME);
		
		summonerText = (TextView) view.findViewById(R.id.summonerName);
		setSummonerName(summonerName);
		
		summonerStatus = (TextView) view.findViewById(R.id.summonerStatus);
		
		// Setup Image Views for status
		setupStatusView();
		
		// Setup RadioButton Views
		setupRadioButtons();
		
		mFragmentCallback.onFragmentCreated();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_home, container,
				false);
		return rootView;
	}
	
	public void setSummonerName(String name) {		
		if (summonerText == null) return;
		if (name == null) { 
			summonerText.setText("Summoner");
		} else {
			summonerText.setText(name);
			summonerName = name;
		}
	}
	
	public void setupStatusView() {
		// Initialize Views
		imageLeft = (ImageView) view.findViewById(R.id.imageLeft);
		imageMiddle = (ImageView) view.findViewById(R.id.imageMiddle);
		imageRight = (ImageView) view.findViewById(R.id.imageRight);
		
		imageStatus[0] = imageLeft;
		imageStatus[1] = imageMiddle;
		imageStatus[2] = imageRight;
		
		// Setup color
		imageLeft.getDrawable().setColorFilter(Color.rgb(255, 0, 0), Mode.MULTIPLY);
		imageMiddle.getDrawable().setColorFilter(Color.rgb(0, 255, 100), Mode.MULTIPLY);
		imageRight.getDrawable().setColorFilter(Color.rgb(255, 255, 0), Mode.MULTIPLY);
		
		// Setup scale
		scaleView(imageMiddle, 1f);
		scaleView(imageLeft, 0.65f);
		scaleView(imageRight, 0.65f);
		
		// Setup click listeners
		imageLeft.setOnClickListener(new StatusClickedListener());
		imageMiddle.setOnClickListener(new StatusClickedListener());
		imageRight.setOnClickListener(new StatusClickedListener());
	}
	
	private class StatusClickedListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			int previousStatus = selectedStatus;
			if (v == imageLeft) {
				selectedStatus = 0;
				updateRadioButtons(AWAY_STATUSES);
			} else if (v == imageMiddle) {
				selectedStatus = 1;
				updateRadioButtons(ONLINE_STATUSES);
			} else if (v == imageRight) {
				selectedStatus = 2;
				updateRadioButtons(GAME_STATUSES);
			}
			
			if (previousStatus == selectedStatus) return;
			
			radioButton[0].setChecked(true);
			radioButton[0].callOnClick();
			
			scaleView(imageStatus[selectedStatus], 1f);
			
			for (int i = 0; i < imageStatus.length; i++) {
				if (selectedStatus == i) continue;
				scaleView(imageStatus[i], 0.65f);
			}
			
		}
		
	}
	
	public void setupRadioButtons() {
		for (int i = 1; i <= 5; i++) {
			radioButton[i-1] = (RadioButton) view.findViewById(getResources().getIdentifier("radioButton" + i, "id", this.getActivity().getPackageName()));
		}
		for (int i = 0; i < 5; i++) {
			radioButton[i].setOnClickListener(new StatusButtonClickListener());
		}
		
		updateRadioButtons(ONLINE_STATUSES);
	}
	
	public void updateRadioButtons(String[] text) {
		// On click options
		for (int i = 0; i < 5; i++) {
			radioButton[i].setText(text[i]);
		}
	}
	
	private class StatusButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// Determine which option was clicked
			int selected = 0;
			if (v == radioButton[0]) {
				selected = 0;
			} else if (v == radioButton[1]) {
				selected = 1;
			} else if (v == radioButton[2]) {
				selected = 2;
			} else if (v == radioButton[3]) {
				selected = 3;
			} else if (v == radioButton[4]) {
				selected = 4;
			}
			
			String statusText = "";
			String statusType = "";
			switch (selectedStatus) {
			case 0:
				statusText = AWAY_STATUSES[selected];
				statusType = "away";
				summonerStatus.setText("Away");
				break;
			case 1:
				statusText = ONLINE_STATUSES[selected];
				statusType = "chat";
				summonerStatus.setText("Online");
				break;
			case 2:
				statusText = GAME_STATUSES[selected];
				statusType = "dnd";
				summonerStatus.setText("In Game");
				break;
			}
			if (selected != 0) {
				summonerStatus.setText(statusText);
			} else {
				statusText = "";
			}
			
			mStatusCallback.onStatusClicked(statusText, statusType);
		}
		
	}
	
	
	
	
	private void scaleView(View v, float scale) {
		v.setScaleX(scale);
		v.setScaleY(scale);
	}
}



























