package com.shaubert.widget;

/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * 
 * Modified by Kostya Vasilyev, 2011.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;

import com.shaubert.dirty.R;

/**
 * This class is an amalgamation of two fast scrollers.
 * 
 * One (older) was extracted from Contacts application and shared as part of Dianne Hackborn's Rings
 * Extended application here:
 * 
 * <a>http://code.google.com/p/apps-for-android/source/browse/trunk/
 * RingsExtended/src/com/example/android/rings_extended/FastScrollView.java</a>
 * 
 * ... also included in this project, as FastScrollView.
 * 
 * It is a subclass of FrameView and is intended to be used as a container for a ListView.
 * 
 * The other, newer one, was taken from ICS sources, and is a helper class that ListView knows about
 * and calls into to perform fast scrolling.
 * 
 * The result is a fast scroller that can be used as the former one, as a parent view wrapper around
 * a ListView, without having to pull a copy of AbsListView / ListView from the framework into an
 * application to integrate with.
 * 
 * There is a method, {@link #onSectionsChanged()}, for updating the section index without having to
 * toggle fast scrolling off and on, which is necessary with the built-in fast scroller (as it only
 * gets the section index once, when enabled).
 * 
 * Updating the section list preserves the user's touch interaction state. If your list view can be
 * updated dynamically, the built-in fast scroller loses the touch interaction state when toggled
 * off and back on.
 * 
 * Finally, the section overlay is sized differently, by using dimension resources, and correctly
 * draws a section name longer than one character.
 * 
 */

public class FasterScrollerView extends FrameLayout implements OnScrollListener, OnHierarchyChangeListener {

	// private static final String TAG = "FasterScrollerView";

	private static final int OVERLAY_FLOATING = 0;
	private static final int OVERLAY_AT_THUMB = 1;

	// Minimum number of pages to justify showing a fast scroll thumb
	private static int MIN_PAGES = 2;
	// Scroll thumb not showing
	private static final int STATE_NONE = 0;
	// Not implemented yet - fade-in transition
	// !!! private static final int STATE_ENTER = 1;
	// Scroll thumb visible and moving along with the scrollbar
	private static final int STATE_VISIBLE = 2;
	// Scroll thumb being dragged by user
	private static final int STATE_DRAGGING = 3;
	// Scroll thumb fading out due to inactivity timeout
	private static final int STATE_EXIT = 4;

	private static final int[] PRESSED_STATES = new int[] { android.R.attr.state_pressed };

	private static final int[] DEFAULT_STATES = new int[0];

	private Drawable mThumbDrawable;
	private Drawable mOverlayDrawable;

	private RectF mOverlayPos;
	private int mOverlayW;
	private int mOverlayH;

	private Object[] mSections;
	private String mSectionText;
	private boolean mDrawOverlay;

	private int mState;
	private int mVisibleItem;
	private int mScaledTouchSlop;
	private Paint mPaint;

	private boolean mMatchDragPosition;

	private boolean mChangedBounds;
	private boolean mAlwaysShow = true;

	private Handler mHandler = new Handler();

	private int mItemCount = -1;
	private boolean mLongList;

	private boolean mFastScrollEnabled = false;

	private int mOverlayPosition = OVERLAY_AT_THUMB;

	private final Rect mTmpRect = new Rect();

	private static final int FADE_TIMEOUT = 1500;
	private static final int PENDING_DRAG_DELAY = 180;

	AbsListView mList;
	int mListOffset;
	BaseAdapter mListAdapter;
	SectionIndexer mSectionIndexer;
	boolean mScrollCompleted;

	float mInitialTouchY;
	boolean mPendingDrag;

	int mThumbH;
	int mThumbW;
	int mThumbY;

	public FasterScrollerView(Context context) {
		super(context);

		init(context);
	}

	public FasterScrollerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context);
	}

	public FasterScrollerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context);
	}

	private void init(Context context) {
		// Get both the scrollbar states drawables
		final Resources res = context.getResources();
		setThumbDrawable(res.getDrawable(R.drawable.fastscroll_thumb));

		mOverlayDrawable = res.getDrawable(R.drawable.fastscroll_label_right_holo_light);

		mScrollCompleted = true;
		setWillNotDraw(false);

		// Need to know when the ListView is added
		setOnHierarchyChangeListener(this);

		mScrollCompleted = true;

		mOverlayW = res.getDimensionPixelSize(R.dimen.fastscroll_overlay_width);
		mOverlayH = res.getDimensionPixelSize(R.dimen.fastscroll_overlay_height);
		mOverlayPos = new RectF();

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTextAlign(Paint.Align.CENTER);
		mPaint.setTextSize(res.getDimension(R.dimen.fastscroll_overlay_font_size));

		mPaint.setColor(0xFFFFFFFF);
		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

		mState = STATE_NONE;
		refreshDrawableState();

		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		mMatchDragPosition = false; // Don't like it Build.VERSION.SDK_INT >=
									// Build.VERSION_CODES.HONEYCOMB;
	}

	private void initListView(AbsListView listView) {
		mList = listView;

		// to show mOverlayDrawable properly
		if (mList.getWidth() > 0 && mList.getHeight() > 0) {
			onSizeChanged(mList.getWidth(), mList.getHeight(), 0, 0);
		}

		mList.setOnScrollListener(this);
		getSectionsFromIndexer();
	}

	public void setFastScrollEnabled(boolean enabled) {
		mFastScrollEnabled = enabled;
	}

	public void setState(int state) {
		switch (state) {
		case STATE_NONE:
			mHandler.removeCallbacks(mScrollFade);
			doInvalidate();
			break;
		case STATE_VISIBLE:
			if (mState != STATE_VISIBLE) { // Optimization
				resetThumbPos();
			}
			// Fall through
		case STATE_DRAGGING:
			mHandler.removeCallbacks(mScrollFade);
			break;
		case STATE_EXIT:
			int viewWidth = mList.getWidth();
			doInvalidate(viewWidth - mThumbW, mThumbY, viewWidth, mThumbY + mThumbH);
			break;
		}
		mState = state;
		refreshDrawableState();
	}

	public int getState() {
		return mState;
	}

	/**
	 * Updates the section list. Should be called from the adapter when the data is invalidated or
	 * changed.
	 * 
	 * @param resetDragging
	 *            True to reset current dragging state, false to keep dragging. Depending on how
	 *            much the underlying data changed, preserving the drag state may or may not be
	 *            confusing to the user. It's up to the application to make that decision and call
	 *            with the correct resetDraggging value.
	 */
	public void updateSections(boolean resetDragging) {
		mListAdapter = null;
		if (mFastScrollEnabled) {
			if (mList != null) {
				getSectionsFromIndexer();

				if (resetDragging) {
					if (mState == STATE_DRAGGING) {
						mList.requestDisallowInterceptTouchEvent(false);
						setState(STATE_VISIBLE);
						final Handler handler = mHandler;
						handler.removeCallbacks(mScrollFade);
						if (!mAlwaysShow) {
							handler.postDelayed(mScrollFade, FADE_TIMEOUT);
						}
						doInvalidate();
					}
				}
			}
		}
	}

	@Override
	public void refreshDrawableState() {
		int[] state = mState == STATE_DRAGGING ? PRESSED_STATES : DEFAULT_STATES;

		if (mThumbDrawable != null && mThumbDrawable.isStateful()) {
			mThumbDrawable.setState(state);
		}
	}

	@Override
	public void onChildViewAdded(View parent, View child) {
		if (child instanceof AbsListView) {
			initListView((AbsListView) child);
		}
	}

	@Override
	public void onChildViewRemoved(View parent, View child) {
		if (child == mList) {
			mList = null;
			mListAdapter = null;
			mSections = null;
		}
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (mState == STATE_NONE || !mFastScrollEnabled) {
			// No need to draw anything
			return;
		}

		final int y = mThumbY;
		final int viewWidth = mList.getWidth();
		final int viewHeight = mList.getHeight();
		final ScrollFade scrollFade = mScrollFade;

		int alpha = -1;
		if (mState == STATE_EXIT) {
			alpha = scrollFade.getAlpha();
			if (alpha < ScrollFade.ALPHA_MAX / 2) {
				mThumbDrawable.setAlpha(alpha * 2);
			}
			int left = viewWidth - (mThumbW * alpha) / ScrollFade.ALPHA_MAX;
			mThumbDrawable.setBounds(left, 0, left + mThumbW, mThumbH);
			mChangedBounds = true;
		}

		canvas.translate(0, y);
		mThumbDrawable.draw(canvas);
		canvas.translate(0, -y);

		// If user is dragging the scroll bar, draw the alphabet overlay
		if (mState == STATE_DRAGGING && mDrawOverlay) {
			if (mOverlayPosition == OVERLAY_AT_THUMB) {
				int left = Math.max(0, viewWidth - mThumbW - mOverlayW);
				int top = Math.max(0, Math.min(y + (mThumbH - mOverlayH) / 2, viewHeight - mOverlayH));

				final RectF pos = mOverlayPos;
				pos.left = left;
				pos.right = pos.left + mOverlayW;
				pos.top = top;
				pos.bottom = pos.top + mOverlayH;
				if (mOverlayDrawable != null) {
					mOverlayDrawable.setBounds((int) pos.left, (int) pos.top, (int) pos.right, (int) pos.bottom);
				}
			}
			mOverlayDrawable.draw(canvas);
			final Paint paint = mPaint;
			float ascent = paint.ascent();
			float descent = paint.descent();
			final RectF rectF = mOverlayPos;
			final Rect tmpRect = mTmpRect;
			mOverlayDrawable.getPadding(tmpRect);
			final int hOff = (tmpRect.right - tmpRect.left) / 2;
			final int vOff = (tmpRect.bottom - tmpRect.top) / 2;
			canvas.drawText(mSectionText, (int) (rectF.left + rectF.right) / 2 - hOff, (int) (rectF.bottom + rectF.top)
					/ 2 - (ascent + descent) / 2 - vOff, paint);
		} else if (mState == STATE_EXIT) {
			if (alpha == 0) { // Done with exit
				setState(STATE_NONE);
			} else {
				doInvalidate(viewWidth - mThumbW, y, viewWidth, y + mThumbH);
			}
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		if (mThumbDrawable != null) {
			mThumbDrawable.setBounds(w - mThumbW, 0, w, mThumbH);
		}
		if (mOverlayPosition == OVERLAY_FLOATING) {
			final RectF pos = mOverlayPos;
			pos.left = (w - mOverlayW) / 2;
			pos.right = pos.left + mOverlayW;
			pos.top = h / 10; // 10% from top
			pos.bottom = pos.top + mOverlayH;
			if (mOverlayDrawable != null) {
				mOverlayDrawable.setBounds((int) pos.left, (int) pos.top, (int) pos.right, (int) pos.bottom);
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		updateScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
	}

	private void updateScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Are there enough pages to require fast scroll? Recompute only if
		// total count changes
		if (mItemCount != totalItemCount && visibleItemCount > 0) {
			mItemCount = totalItemCount;
			mLongList = mItemCount / visibleItemCount >= MIN_PAGES;
		}
		if (mAlwaysShow) {
			mLongList = true;
		}
		if (!mLongList) {
			if (mState != STATE_NONE) {
				setState(STATE_NONE);
			}
			return;
		}

		if (totalItemCount - visibleItemCount > 0 && mState != STATE_DRAGGING) {
			final int viewWidth = mList.getWidth();
			doInvalidate(viewWidth - mThumbW, mThumbY, viewWidth, mThumbY + mThumbH);

			mThumbY = getThumbPositionForListPosition(firstVisibleItem, visibleItemCount, totalItemCount);

			doInvalidate(viewWidth - mThumbW, mThumbY, viewWidth, mThumbY + mThumbH);

			if (mChangedBounds) {
				resetThumbPos();
				mChangedBounds = false;
			}
		}
		mScrollCompleted = true;
		if (firstVisibleItem == mVisibleItem) {
			return;
		}
		mVisibleItem = firstVisibleItem;
		if (mState != STATE_DRAGGING) {
			setState(STATE_VISIBLE);
			final Handler handler = mHandler;
			handler.removeCallbacks(mScrollFade);
			if (!mAlwaysShow) {
				handler.postDelayed(mScrollFade, FADE_TIMEOUT);
			}
		}
	}

	private void setThumbDrawable(Drawable drawable) {
		mThumbDrawable = drawable;
		if (drawable instanceof NinePatchDrawable) {
			Resources res = getResources();
			mThumbW = res.getDimensionPixelSize(R.dimen.fastscroll_thumb_width);
			mThumbH = res.getDimensionPixelSize(R.dimen.fastscroll_thumb_height);
		} else {
			mThumbW = drawable.getIntrinsicWidth();
			mThumbH = drawable.getIntrinsicHeight();
		}
		mChangedBounds = true;
	}

	private void resetThumbPos() {
		final int viewWidth = mList.getWidth();
		mThumbDrawable.setBounds(viewWidth - mThumbW, 0, viewWidth, mThumbH);
		mThumbDrawable.setAlpha(ScrollFade.ALPHA_MAX);
	}

	private void getSectionsFromIndexer() {
		Adapter adapter = mList.getAdapter();
		mSectionIndexer = null;

		if (!mFastScrollEnabled) {
			mListAdapter = (BaseAdapter) adapter;
			mSections = new String[] { " " };
			return;
		}

		if (adapter instanceof HeaderViewListAdapter) {
			mListOffset = ((HeaderViewListAdapter) adapter).getHeadersCount();
			adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
		}

		if (adapter instanceof SectionIndexer) {
			mListAdapter = (BaseAdapter) adapter;
			mSectionIndexer = (SectionIndexer) adapter;
			mSections = mSectionIndexer.getSections();
			if (mSections == null) {
				mSections = new String[] { " " };
			}
		} else {
			mListAdapter = (BaseAdapter) adapter;
			mSections = new String[] { " " };
		}
	}

	private void scrollTo(float position) {
		int count = mList.getCount();
		mScrollCompleted = false;
		float fThreshold = (1.0f / count) / 8;
		final Object[] sections = mSections;
		int sectionIndex;
		if (sections != null && sections.length > 1) {
			final int nSections = sections.length;
			int section = (int) (position * nSections);
			if (section >= nSections) {
				section = nSections - 1;
			}
			int exactSection = section;
			sectionIndex = section;
			int index = mSectionIndexer.getPositionForSection(section);
			// Given the expected section and index, the following code will
			// try to account for missing sections (no names starting with..)
			// It will compute the scroll space of surrounding empty sections
			// and interpolate the currently visible letter's range across the
			// available space, so that there is always some list movement while
			// the user moves the thumb.
			int nextIndex = count;
			int prevIndex = index;
			int prevSection = section;
			int nextSection = section + 1;
			// Assume the next section is unique
			if (section < nSections - 1) {
				nextIndex = mSectionIndexer.getPositionForSection(section + 1);
			}

			// Find the previous index if we're slicing the previous section
			if (nextIndex == index) {
				// Non-existent letter
				while (section > 0) {
					section--;
					prevIndex = mSectionIndexer.getPositionForSection(section);
					if (prevIndex != index) {
						prevSection = section;
						sectionIndex = section;
						break;
					} else if (section == 0) {
						// When section reaches 0 here, sectionIndex must follow
						// it.
						// Assuming mSectionIndexer.getPositionForSection(0) ==
						// 0.
						sectionIndex = 0;
						break;
					}
				}
			}
			// Find the next index, in case the assumed next index is not
			// unique. For instance, if there is no P, then request for P's
			// position actually returns Q's. So we need to look ahead to make
			// sure that there is really a Q at Q's position. If not, move
			// further down...
			int nextNextSection = nextSection + 1;
			while (nextNextSection < nSections && mSectionIndexer.getPositionForSection(nextNextSection) == nextIndex) {
				nextNextSection++;
				nextSection++;
			}
			// Compute the beginning and ending scroll range percentage of the
			// currently visible letter. This could be equal to or greater than
			// (1 / nSections).
			float fPrev = (float) prevSection / nSections;
			float fNext = (float) nextSection / nSections;
			if (prevSection == exactSection && position - fPrev < fThreshold) {
				index = prevIndex;
			} else {
				index = prevIndex + (int) ((nextIndex - prevIndex) * (position - fPrev) / (fNext - fPrev));
			}
			// Don't overflow
			if (index > count - 1) index = count - 1;

			if (mList instanceof ListView) {
				((ListView) mList).setSelectionFromTop(index + mListOffset, 0);
			} else {
				mList.setSelection(index + mListOffset);
			}
		} else {
			int index = (int) (position * count);
			// Don't overflow
			if (index > count - 1) index = count - 1;

			if (mList instanceof ExpandableListView) {
				ExpandableListView expList = (ExpandableListView) mList;
				expList.setSelectionFromTop(
						expList.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(index + mListOffset)),
						0);
			} else if (mList instanceof ListView) {
				((ListView) mList).setSelectionFromTop(index + mListOffset, 0);
			} else {
				mList.setSelection(index + mListOffset);
			}
			sectionIndex = -1;
		}

		if (sectionIndex >= 0) {
			String text = mSectionText = sections[sectionIndex].toString();
			mDrawOverlay = (text.length() != 1 || text.charAt(0) != ' ') && sectionIndex < sections.length;
		} else {
			mDrawOverlay = false;
		}
	}

	private int getThumbPositionForListPosition(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mSectionIndexer == null || mListAdapter == null) {
			getSectionsFromIndexer();
		}
		if (mSectionIndexer == null || !mMatchDragPosition) {
			int position = ((mList.getHeight() - mThumbH) * firstVisibleItem) / (totalItemCount - visibleItemCount);
			return position;
		}

		firstVisibleItem -= mListOffset;
		if (firstVisibleItem < 0) {
			return 0;
		}
		totalItemCount -= mListOffset;

		final int trackHeight = mList.getHeight() - mThumbH;

		final int section = mSectionIndexer.getSectionForPosition(firstVisibleItem);
		final int sectionPos = mSectionIndexer.getPositionForSection(section);
		final int nextSectionPos = mSectionIndexer.getPositionForSection(section + 1);
		final int sectionCount = mSections.length;
		final int positionsInSection = nextSectionPos - sectionPos;

		final View child = mList.getChildAt(0);
		final float incrementalPos = child == null ? 0 : firstVisibleItem
				+ (float) (mList.getPaddingTop() - child.getTop()) / child.getHeight();
		final float posWithinSection = (incrementalPos - sectionPos) / positionsInSection;
		int result = (int) ((section + posWithinSection) / sectionCount * trackHeight);

		// Fake out the scrollbar for the last item. Since the section indexer
		// won't
		// ever actually move the list in this end space, make scrolling across
		// the last item
		// account for whatever space is remaining.
		if (firstVisibleItem > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
			final View lastChild = mList.getChildAt(visibleItemCount - 1);
			final float lastItemVisible = (float) (mList.getHeight() - mList.getPaddingBottom() - lastChild.getTop())
					/ lastChild.getHeight();
			result += (trackHeight - result) * lastItemVisible;
		}

		return result;
	}

	private boolean isPointInside(float x, float y) {
		boolean inTrack = x > mList.getWidth() - mThumbW;

		// Allow taps in the track to start moving.
		return inTrack && y >= mThumbY && y <= mThumbY + mThumbH;
	}

	private void cancelFling() {
		// Cancel the list fling
		MotionEvent cancelFling = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
		mList.onTouchEvent(cancelFling);
		cancelFling.recycle();
	}

	private void cancelPendingDrag() {
		mList.removeCallbacks(mDeferStartDrag);
		mPendingDrag = false;
	}

	@SuppressWarnings("unused")
	private void startPendingDrag() {
		mPendingDrag = true;
		mList.postDelayed(mDeferStartDrag, PENDING_DRAG_DELAY);
	}

	private void beginDrag() {
		setState(STATE_DRAGGING);
		if (mListAdapter == null && mList != null) {
			getSectionsFromIndexer();
		}
		if (mList != null) {
			mList.requestDisallowInterceptTouchEvent(true);
			// !!!
			// mList.reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
		}

		cancelFling();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!mFastScrollEnabled) {
			return false;
		}

		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (mState > STATE_NONE && isPointInside(ev.getX(), ev.getY())) {
				if (true) { // !!! !mList.isInScrollingContainer()) {
					beginDrag();
					return true;
				}
				// !!!
				// mInitialTouchY = ev.getY();
				// startPendingDrag();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			cancelPendingDrag();
			break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if (mState == STATE_NONE || !mFastScrollEnabled) {
			return false;
		}

		final int action = me.getAction();

		if (action == MotionEvent.ACTION_DOWN) {
			if (isPointInside(me.getX(), me.getY())) {
				if (true) { // !!! !mList.isInScrollingContainer()) {
					beginDrag();
					return true;
				}
				// !!!
				// mInitialTouchY = me.getY();
				// startPendingDrag();
			}
		} else if (action == MotionEvent.ACTION_UP) { // don't add ACTION_CANCEL
														// here
			if (mPendingDrag) {
				// Allow a tap to scroll.
				beginDrag();

				final int viewHeight = mList.getHeight();
				// Jitter
				int newThumbY = (int) me.getY() - mThumbH + 10;
				if (newThumbY < 0) {
					newThumbY = 0;
				} else if (newThumbY + mThumbH > viewHeight) {
					newThumbY = viewHeight - mThumbH;
				}
				mThumbY = newThumbY;
				scrollTo((float) mThumbY / (viewHeight - mThumbH));

				cancelPendingDrag();
				// Will hit the STATE_DRAGGING check below
			}
			if (mState == STATE_DRAGGING) {
				if (mList != null) {
					// ViewGroup does the right thing already, but there might
					// be other classes that don't properly reset on touch-up,
					// so do this explicitly just in case.
					mList.requestDisallowInterceptTouchEvent(false);
					// !!!
					// mList.reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
				}
				setState(STATE_VISIBLE);
				final Handler handler = mHandler;
				handler.removeCallbacks(mScrollFade);
				if (!mAlwaysShow) {
					handler.postDelayed(mScrollFade, FADE_TIMEOUT);
				}
				doInvalidate();
				return true;
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (mPendingDrag) {
				final float y = me.getY();
				if (Math.abs(y - mInitialTouchY) > mScaledTouchSlop) {
					setState(STATE_DRAGGING);
					if (mListAdapter == null && mList != null) {
						getSectionsFromIndexer();
					}
					if (mList != null) {
						mList.requestDisallowInterceptTouchEvent(true);
						// !!!
						// mList.reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
					}

					cancelFling();
					cancelPendingDrag();
					// Will hit the STATE_DRAGGING check below
				}
			}
			if (mState == STATE_DRAGGING) {
				final int viewHeight = mList.getHeight();
				// Jitter
				int newThumbY = (int) me.getY() - mThumbH + 10;
				if (newThumbY < 0) {
					newThumbY = 0;
				} else if (newThumbY + mThumbH > viewHeight) {
					newThumbY = viewHeight - mThumbH;
				}
				if (Math.abs(mThumbY - newThumbY) < 2) {
					return true;
				}
				mThumbY = newThumbY;
				// If the previous scrollTo is still pending
				if (mScrollCompleted) {
					scrollTo((float) mThumbY / (viewHeight - mThumbH));
				}
				return true;
			}
		} else if (action == MotionEvent.ACTION_CANCEL) {
			cancelPendingDrag();
		}
		return false;
	}

	private void doInvalidate() {
		invalidate();
	}

	private void doInvalidate(int l, int t, int r, int b) {
		invalidate(l, t, r, b);
	}

	private final Runnable mDeferStartDrag = new Runnable() {
		@Override
		public void run() {
			if (true) { // !!! mList.mIsAttached) {
				beginDrag();

				final int viewHeight = mList.getHeight();
				// Jitter
				int newThumbY = (int) mInitialTouchY - mThumbH + 10;
				if (newThumbY < 0) {
					newThumbY = 0;
				} else if (newThumbY + mThumbH > viewHeight) {
					newThumbY = viewHeight - mThumbH;
				}
				mThumbY = newThumbY;
				scrollTo((float) mThumbY / (viewHeight - mThumbH));
			}

			mPendingDrag = false;
		}
	};

	private ScrollFade mScrollFade = new ScrollFade();

	private class ScrollFade implements Runnable {

		long mStartTime;
		long mFadeDuration;
		static final int ALPHA_MAX = 255;
		static final long FADE_DURATION = 200;

		void startFade() {
			mFadeDuration = FADE_DURATION;
			mStartTime = SystemClock.uptimeMillis();
			setState(STATE_EXIT);
		}

		int getAlpha() {
			if (getState() != STATE_EXIT) {
				return ALPHA_MAX;
			}
			int alpha;
			long now = SystemClock.uptimeMillis();
			if (now > mStartTime + mFadeDuration) {
				alpha = 0;
			} else {
				alpha = (int) (ALPHA_MAX - ((now - mStartTime) * ALPHA_MAX) / mFadeDuration);
			}
			return alpha;
		}

		@Override
		public void run() {
            if (getState() != STATE_EXIT) {
                startFade();
                return;
            }

            if (getAlpha() > 0) {
                doInvalidate();
            } else {
                setState(STATE_NONE);
            }
		}
	}
}
