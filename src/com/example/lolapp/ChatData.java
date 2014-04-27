package com.example.lolapp;

import com.example.lolapp.XMPPService.GroupType;

public class ChatData {
	public String name;
	public GroupType type;
	
	public ChatData(String n, GroupType t) {
		this.name = n;
		this.type = t;
	}
	
}
