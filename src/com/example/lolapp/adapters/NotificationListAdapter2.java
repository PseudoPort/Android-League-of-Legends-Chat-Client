package com.example.lolapp.adapters;

import java.util.HashMap;
import java.util.List;

import com.example.lolapp.R;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class NotificationListAdapter2 extends BaseExpandableListAdapter{
	
	Context context;
	List<String> listHeader;
	HashMap<String, List<String>> listChildren;
	
	public NotificationListAdapter2 (Context context, List<String> listHeader, HashMap<String, List<String>> listChildren) {
		this.context = context;
		this.listHeader = listHeader;
		this.listChildren = listChildren;
	}

	@Override
	public int getGroupCount() {
		return listHeader.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return listChildren.get(listHeader.get(groupPosition)).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return listHeader.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return listChildren.get(listHeader.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		String headerTitle = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.friendslist_group, null);
		}

		TextView lblListHeader = (TextView) convertView
				.findViewById(R.id.friendsListHeader);
		lblListHeader.setTypeface(null, Typeface.BOLD);
		lblListHeader.setText(headerTitle);
		
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final String childName = (String) getChild(groupPosition, childPosition);
		 
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.friendslist_item, null);
        }
        
        // Set Properties
        TextView notifTitle = (TextView) convertView.findViewById(R.id.friendName);
        TextView notifDescription = (TextView) convertView.findViewById(R.id.status);
        
        return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
