package com.example.lolapp;

public class Summoner {

	public String user = null;
	public String name = null;
	public String status = null;
	public String mode = null;
	public String group = null;
	public boolean isOnline;
	public boolean inGame = false;
	
	public long inGameTime = 0;

	public Summoner(String user, String name, String status, String mode, String group, boolean isOnline) {
		this.user = user;
		this.name = name;
		this.status = status;
		this.mode = mode;
		this.group = group;
		this.isOnline = isOnline;
		
		this.inGameTime = Long.parseLong(getTimeStamp());
	}

	public String getChatStatus() {
		return getTag("statusMsg");
	}

	public String getGameStatus() {
		return getTag("gameStatus");
	}

	public String getChampion() {
		return getTag("skinname");
	}

	public String getTimeStamp() {
		String t = getTag("timeStamp");
		if (t.equals("")) return "0";
		long startTime = Long.parseLong(t);
		long currentTime = System.currentTimeMillis();

		long time = (currentTime - startTime) / 1000 / 60;
		return "" + time;
	}

	public String getTag(String tag) {
		int start = status.indexOf("<"+tag+">") + tag.length() + 2;
		int end = status.indexOf("</" + tag + ">");

		String s = "";
		try {
			s = status.substring(start, end);
		} catch (Exception e) {

		}

		return s;
	}

	public String getStatus() {
		String s = "";
		if (mode.equals("chat")) {
			s = getChatStatus();
		} else if (mode.equals("dnd")) {
			if (getGameStatus().equals("championSelect")) {
				s = "In Champion Select";
			} else if (getGameStatus().equals("inQueue")) {
				s = "In Queue";
			} else if (getGameStatus().equals("hostingNormalGame")) {
				s = "Creating Normal Game";
			} else if (getGameStatus().equals("hostingRankedGame")) {
				s = "Creating Ranked Game";
			} else if (getGameStatus().equals("hostingCoopVsAIGame")) {
				s = "Creating Coop vs. AI";
			} else if (getGameStatus().equals("hostingPracticeGame")) {
				s = "Creating Custom Game";
			} else if (getGameStatus().equals("inGame")) {
				s = "In Game: " + getChampion() + " (" + getTimeStamp() + " min)";
			}
		}
		if (!isOnline) s = "Offline";
		if (isOnline && s.equals("")) s = "Online";
		
		if (getGameStatus().equals("inGame")) {
			inGame = true;
		} else {
			inGame = false;
		}
		return s;
	}
}
