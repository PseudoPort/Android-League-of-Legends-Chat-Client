package com.example.lolapp.model;

import com.example.lolapp.xmppservice.XMPPService;
import com.example.lolapp.xmppservice.XMPPService.GroupType;

public class ChatData {
	public String name;
	public GroupType type;
	
	public ChatData(String n, GroupType t) {
		this.name = n;
		this.type = t;
	}
	
}
