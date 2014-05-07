package com.example.lolapp;

import com.example.lolapp.FriendsListFragment.OnFragmentCreatedListener;
import com.example.lolapp.FriendsListFragment.OnFriendChatClickListener;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HomeFragment extends Fragment {
	
	public final static String SUMMONER_NAME = "SUMMONER_NAME";
	
	// Variables
	String summonerName = null;
	
	// Views
	TextView summonerText;
	
	Activity activity;
	
	// Interface
	OnFragmentCreatedListener mFragmentCallback;
	public interface OnFragmentCreatedListener {
		public void onFragmentCreated();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		mFragmentCallback = (OnFragmentCreatedListener) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		summonerName = getArguments().getString(SUMMONER_NAME);
		
		summonerText = (TextView) view.findViewById(R.id.summonerName);
		setSummonerName(summonerName);
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
}
