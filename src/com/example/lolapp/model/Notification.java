package com.example.lolapp.model;

public class Notification {
	public String message;
	public int type;
	public String title;
	public long time;
	public String id;
	
	public Notification(String title, String msg, long time, int t, String id) {
		message = msg;
		type = t;
		this.title = title;
		this.time = time;
		this.id = id;
	}
}
