package com.example.lolapp;

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
			stopDragging();
		}

		if (mGestureDetector != null) {
			mGestureDetector.onTouchEvent(ev);
		}

		if (mDragView != null) {
			if (ev.getAction() == MotionEvent.ACTION_MOVE) {
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				
				System.out.println(mSrcDragPos + " " + pointToPosition(x, y));
				
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




}
