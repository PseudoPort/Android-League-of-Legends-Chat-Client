package com.example.lolapp.listview;

import com.example.lolapp.R;
import com.example.lolapp.R.id;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FriendListView extends ExpandableListView {

	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;

	private int mDragPointX;    // at what x offset inside the item did the user grab it
	private int mDragPointY;    // at what y offset inside the item did the user grab it
	private int mXOffset;  // the difference between screen coordinates and coordinates in this view
	private int mYOffset;  // the difference between screen coordinates and coordinates in this view

	private int mDragPos;
	private int mSrcDragPos;

	GestureDetector mGestureDetector;
	ImageView imageView = null;

	private Bitmap mDragBitmap;

	boolean longPressed = false;
	
	private int mParPos;
	
	private ChangeGroupListener mChangeGroupListener = null;
	
	public FriendListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {

			@Override
			public void onLongPress(MotionEvent ev) {
				longPressed = true;

				if (imageView == null) {
					stopDragging();

					int x = (int) ev.getX();
					int y = (int) ev.getY();

					int position = pointToPosition(x, y);
					if (position == AdapterView.INVALID_POSITION) {
						return;
					}
					
					// Check if view is header or off line
					if (getChildAt(position - getFirstVisiblePosition()).findViewById(R.id.friendsListHeader) != null) {
						longPressed = false;
						// Do long press on group
						return;
					} else {
						if (((TextView)getChildAt(position - getFirstVisiblePosition()).findViewById(R.id.status)).getText().equals("Offline")) {
							longPressed = false;
							return;
						}
					}
					
					ViewGroup item = (ViewGroup) getChildAt(position - getFirstVisiblePosition());
					mDragPointX = x - item.getLeft();
					mDragPointY = y - item.getTop();
					mXOffset = ((int)ev.getRawX()) - x;
					mYOffset = ((int)ev.getRawY()) - y;

					item.setDrawingCacheEnabled(true);

					Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

					startDragging(bitmap, x, y);

					mDragPos = position;
					mSrcDragPos = mDragPos;

					super.onLongPress(ev);
				}
			}
		});

		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		if (ev.getAction() == MotionEvent.ACTION_UP) {
			longPressed = false;
			// On change group listener
			if (mChangeGroupListener != null) {
				try {
					String userId = ((TextView)getChildAt(mSrcDragPos).findViewById(R.id.friendName)).getText().toString();
					String groupName = ((TextView)getChildAt(mParPos).findViewById(R.id.friendsListHeader)).getText().toString();
					mChangeGroupListener.changeGroup(userId, groupName);
				} catch (Exception e) {
					
				}
			}
			stopDragging();
		}

		if (mGestureDetector != null) {
			mGestureDetector.onTouchEvent(ev);
		}

		if (mDragView != null) {
			if (ev.getAction() == MotionEvent.ACTION_MOVE) {
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				
				
				//System.out.println(mSrcDragPos + " " + pointToPosition(x, y));
				
				int mCurPos = pointToPosition(x, y) - getFirstVisiblePosition();
				
				if (pointToPosition(x, y) != ListView.INVALID_POSITION) {
					
					int tempParPos = mCurPos;
					while (getChildAt(tempParPos).findViewById(R.id.friendName) != null) {
						tempParPos--;
					}
					
					mParPos = tempParPos;
					
					//System.out.println(mParPos);
					
					View v = getChildAt(mCurPos);
					if (v.findViewById(R.id.friendName) == null) {
						//System.out.println("HEADER");
					} else {
						//System.out.println("CHILD");
					}
				}
				
				dragView(x, y);
			}
		}

		return super.onTouchEvent(ev);
	}

	private void startDragging(Bitmap bm, int x, int y) {
		stopDragging();

		mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
		mWindowParams.x = x - mDragPointX + mXOffset;
		mWindowParams.y = y - mDragPointY + mYOffset;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		Context context = getContext();
		ImageView v = new ImageView(context);

		v.setBackgroundColor(Color.GRAY);
		v.setPadding(0, 0, 0, 0);
		v.setImageBitmap(bm);
		mDragBitmap = bm;

		mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	private void dragView(int x, int y) {

		mWindowParams.x = x - mDragPointX + mXOffset;

		mWindowParams.y = y - mDragPointY + mYOffset;
		mWindowManager.updateViewLayout(mDragView, mWindowParams);
	}

	private void stopDragging() {
		if (mDragView != null) {
			mDragView.setVisibility(GONE);
			WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
		if (mDragBitmap != null) {
			mDragBitmap.recycle();
			mDragBitmap = null;
		}
	}
	
	public void setChangeGroupListener(ChangeGroupListener l) {
		mChangeGroupListener = l;
	}
	
	public interface ChangeGroupListener {
		void changeGroup(String fromName, String toGroup);
	}
}
