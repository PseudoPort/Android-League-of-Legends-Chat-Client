package com.example.lolapp.model;

import android.content.Intent;

public class Notification {
	public String message;
	public int type;
	public String title;
	public long time;
	public String id;
	public Intent intent;
	public int unread;
	
	public Notification(String title, String msg, long time, int t, String id, Intent intent, int unread) {
		message = msg;
		type = t;
		this.title = title;
		this.time = time;
		this.id = id;
		this.intent = intent;
		this.unread = unread;
	}
}
