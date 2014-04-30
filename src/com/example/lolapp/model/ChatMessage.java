package com.example.lolapp.model;

public class ChatMessage {
	public String message;
	public String fromUser;
	public boolean isUser;
	public boolean isRead = false;
	
	public ChatMessage(String message, String fromUser, boolean isUser) {
		this.message = message;
		this.fromUser = fromUser;
		this.isUser = isUser;
	}
	
	public void markRead() {
		isRead = true;
	}
}