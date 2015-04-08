package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;

/**
 * Layouts its children with fixed sizes and fixed spacing between each child in
 * the horizontal dimension.
 */
public class SequenceViewGroupPortraitAndLandscape extends AdapterView<SequenceAdapter> {

    private final int DEFAULT_ITEM_WIDTH = 250;
    private final int DEFAULT_ITEM_HEIGHT = 250;
    private final int DEFAULT_SPACING = 100;

    private final int ANIMATION_TIME = 350;
    private final int DRAG_DISTANCE = 8;

    //Layout
    private int horizontalSpacing;
    private int itemWidth;
    private int itemHeight;

    private int offsetY = 0;

    //Dragging data
    private View draggingView = null;
    private boolean isDragging = false;
    private int startDragIndex = -1;
    private int curDragIndexPos = -1;
    private int dragStartX;
    private int centerOffset;
    private int touchX = -1;
    private int touchDeltaX = 0;
    private boolean animatingDragReposition = false;
    private int[] newPositions;

    private AutoScroller autoScroller;
    private boolean doAutoScroll = false;

    //Mode handling
    private boolean isInEditMode = false;
    private View addNewPictoGramView;

    //Data and Event handling
    private OnRearrangeListener rearrangeListener;
    private OnNewButtonClickedListener newButtonClickedListener;

    private SequenceAdapter adapter;
    private AdapterDataSetObserver observer = new AdapterDataSetObserver();

    public SequenceViewGroupPortraitAndLandscape(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SequenceViewGroup);
        try {
            horizontalSpacing = a.getDimensionPixelSize(
                    R.styleable.SequenceViewGroup_horizontalSpacing,
                    DEFAULT_SPACING);
            itemWidth = a.getDimensionPixelSize(
                    R.styleable.SequenceViewGroup_itemWidth, DEFAULT_ITEM_WIDTH);
            itemHeight = a.getDimensionPixelSize(
                    R.styleable.SequenceViewGroup_itemHeight, DEFAULT_ITEM_HEIGHT);
        } finally {
            a.recycle();
        }

        setupNewButton();
    }

    private void setupNewButton() {
        addNewPictoGramView = ResourceViewFactory.getAddPictogramButton(getContext());
        addNewPictoGramView.setScaleX(PictogramView.NORMAL_SCALE);
        addNewPictoGramView.setScaleY(PictogramView.NORMAL_SCALE);
        addNewPictoGramView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInEditMode && newButtonClickedListener != null)
                    newButtonClickedListener.onNewButtonClicked();
            }
        });
    }

    private int calcChildLeftPosition(int childIndex) {
        int prevAccumulatedSpacing = childIndex * horizontalSpacing;
        int prevAccumulatedWidth = childIndex * itemWidth;
        return prevAccumulatedSpacing + prevAccumulatedWidth + getPaddingLeft();
    }

    private int calcChildTopPosition() {
        return getPaddingTop() + offsetY;
    }

    private void checkForSwap() {

        int dragCenterX = touchX - centerOffset;

        boolean isDraggingRight = dragCenterX - getCenterX(curDragIndexPos) > 0;

        if (isDraggingRight) {
            int checkIndex = curDragIndexPos + 1;
            //Don't swap with  new sequence diagram view.
            while (checkIndex < getCurrentSequenceViewCount() && dragCenterX > ((getCenterX(curDragIndexPos) + getCenterX(checkIndex)) / 2)) {
                //Animate before swapping indices
                doAnimateTranslation(checkIndex, curDragIndexPos);
                swapIndexPositions(checkIndex, curDragIndexPos);
                curDragIndexPos++;
                checkIndex++;
            }
        } else {
            int checkIndex = curDragIndexPos - 1;
            while (checkIndex >= 0 && dragCenterX < ((getCenterX(curDragIndexPos) + (getCenterX(checkIndex))) / 2)) {
                //Animate before swapping indices
                doAnimateTranslation(checkIndex, curDragIndexPos);
                swapIndexPositions(checkIndex, curDragIndexPos);
                curDragIndexPos--;
                checkIndex--;
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }

    private View childAtPoint(int x, int y) {
        final int numChildren = getChildCount();
        for (int i = 0; i < numChildren; i++) {
            View child = getChildAt(i);
            Rect hitRect = new Rect();
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y))
                return child;
        }
        return null;
    }

    private void doAnimateTranslation(int indexFrom, int indexTo) {

        //This is where the current view occupying indexFrom originally came from.
        int realFrom = newPositions[indexFrom];

        int destOffset = calcChildLeftPosition(indexTo) - calcChildLeftPosition(realFrom);

        //This is the view currently occuping indexFrom
        View view = getChildAt(realFrom);
        //Before deleting current animation, get old translation
        int currentTranslatedX = 0;
        Animation animation = view.getAnimation();
        if (animation != null) {
            currentTranslatedX = getAnimationTranslatedX(animation);
        }
        view.clearAnimation();

        //Move from current offset to new offset based on it original location before dragging.
        TranslateAnimation anim = new TranslateAnimation(
                Animation.ABSOLUTE, currentTranslatedX,
                Animation.ABSOLUTE, destOffset,
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, 0);

        anim.setDuration(ANIMATION_TIME);
        anim.setFillEnabled(true);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    private int getAnimationTranslatedX(Animation animation) {
        if (animation == null)
            throw new NullPointerException("No translation x for null animation");
        Transformation transformation = new Transformation();
        animation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), transformation);
        float[] matrix = new float[9];
        transformation.getMatrix().getValues(matrix);
        return (int) matrix[Matrix.MTRANS_X];
    }

    private int getCenterX(int index) {
        return calcChildLeftPosition(index) + itemWidth / 2;
    }

    public int getHorizontalSpacing() {
        return horizontalSpacing;
    }

    private int getIndexAtPoint(int x, int y) {
        View child = childAtPoint(x, y);
        if (child == null)
            return -1;
        else
            return indexOfChild(child);
    }

    @SuppressWarnings("unused")
    private int getIndexAtX(int x) {
        return getIndexAtPoint(x, calcChildTopPosition() + itemHeight / 2);
    }

    public int getItemHeight() {
        return itemHeight;
    }

    public int getItemWidth() {
        return itemWidth;
    }

    @SuppressWarnings("unused")
    private int getLeftX(int index) {
        return calcChildLeftPosition(index);
    }

    /**
     * Returns the current OnRearrangeListener or null if not set.
     *
     * @param rearrangeListener
     * @return
     */
    public OnRearrangeListener getOnRearrangeListener(OnRearrangeListener rearrangeListener) {
        return this.rearrangeListener;
    }

    @SuppressWarnings("unused")
    private int getRightX(int index) {
        return calcChildLeftPosition(index) + itemWidth;
    }

    private void layoutChild(int i) {
        View child = getChildAt(i);

        layoutChild(child, i);
    }

    private void layoutChild(View child, int i) {
        int x = calcChildLeftPosition(i);
        int y = calcChildTopPosition();

        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (adapter == null) return;
        final int adapterCount = adapter.getCount();
        final int prevViewCount = getCurrentSequenceViewCount();

        int currentViewCount = prevViewCount;

        //Remove Views if too many.
        if (prevViewCount > adapterCount) {
            removeViewsInLayout(adapterCount, prevViewCount - adapterCount);
        }

        int currentIndex = 0;
        for (; currentIndex < adapterCount; currentIndex++) {
            if (currentIndex == startDragIndex) continue;
            View oldView = null;

            if (currentIndex < prevViewCount) {
                oldView = getChildAt(currentIndex);
            }

            View newView = adapter.getView(currentIndex, oldView, this);
            if (newView instanceof PictogramView) {
                ((PictogramView) newView).setEditModeEnabled(isInEditMode);
            }

            if (oldView == null) {
                newView.measure(getChildWidthMeasureSpec(), getChildHeightMeasureSpec());
                addViewInLayout(newView, currentViewCount, generateDefaultLayoutParams());
                currentViewCount++;
            }

            layoutChild(newView, currentIndex);
        }

        if (isInEditMode)
            layoutChild(addNewPictoGramView, currentIndex);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /*
		 * This method dictates what size all children of the given parent must have. The method is a general method, meaning we ignore measure mode.
		 * The ViewGroup requires all children to have the same size, itemWidth and itemHeight as the parent.
		 */

        int minHeight = itemHeight + getPaddingTop() + getPaddingBottom();
        int height = getHeight();

        if (height < minHeight)
            height = minHeight;

        int surplusHeight = height - minHeight;

        offsetY = surplusHeight / 2;
        if (offsetY < 0)
            offsetY = 0;

        int adapterCount = 0;
        if (adapter != null)
            adapterCount = adapter.getCount();

        int width = getPaddingLeft() + getPaddingRight();

        width += adapterCount * itemWidth;
        if (adapterCount > 1) {
            width += (adapterCount - 1) * horizontalSpacing;
        }

        // If we are in edit sequence, we add room for the add pictogram button

        if (isInEditMode) {
            width += itemWidth;
            if (adapterCount > 0) {
                width += horizontalSpacing;
            }
        }

        int childWidthMeasureSpec = getChildWidthMeasureSpec();
        int childHeightMeasureSpec = getChildHeightMeasureSpec();

        int numViewChildren = getChildCount();
        for (int i = 0; i < numViewChildren; i++) {
            View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }

        //We want to take as much width as possible.
        if (getParent().getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) getParent().getParent();
            MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();

            int parentSizeNoPadding = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - params.rightMargin - params.leftMargin;
            if (parentSizeNoPadding > width)
                width = parentSizeNoPadding;
        }

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    private int getChildWidthMeasureSpec() {
        return MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY);
    }

    private int getChildHeightMeasureSpec() {
        return MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //If performing drag animation then consume event to not disrupt
        if (animatingDragReposition) return true;

        boolean handled = false;
        float x = event.getX();
        float y = event.getY();

        //End drag if UP, Cancel or multiple pointers or pointer is gone
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getPointerCount() != 1 || x >= getWidth() || x <= 0) {
            //Be careful with coordinates from the event if getPointerCount != 1
            if (draggingView != null) {
                if (isDragging) {
                    handled = true;
                    stopAutoScroll();

                    // Remove the highlight of the pictogram
                    ((PictogramView) draggingView).placeDown();

                    //Disallow movement when repositioning dragged view.
                    animatingDragReposition = true;

                    TranslateAnimation move = new TranslateAnimation(0, calcChildLeftPosition(curDragIndexPos) - draggingView.getLeft(), 0, 0);
                    move.setDuration(ANIMATION_TIME);

                    move.setAnimationListener(new AnimationListener() {

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (startDragIndex != curDragIndexPos) {

                                if (AddEditSequencesActivity.choiceMode == true) {
                                    AddEditSequencesActivity.choice.rearrangeFrame(startDragIndex, curDragIndexPos);
                                } else {
                                    AddEditSequencesActivity.sequence.rearrangeFrame(startDragIndex, curDragIndexPos);

                                }

                                final int childViews = getChildCount();
                                for (int i = 0; i < childViews; i++) {
                                    getChildAt(i).clearAnimation();
                                }

                                rearrangeListener.onRearrange(startDragIndex, curDragIndexPos);
                                //layout(getLeft(), getTop(), getRight(), getBottom());
                                //This prevents lots of flicker

                                onLayout(true, getLeft(), getTop(), getRight(), getBottom());
                            } else {
                                //Must clear animation to prevent flicker - even though it just ended.
                                getChildAt(startDragIndex).clearAnimation();
                                layoutChild(startDragIndex);
                            }
                            startDragIndex = -1;
                            isDragging = false;
                            curDragIndexPos = -1;
                            animatingDragReposition = false;
                            draggingView = null;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }
                    });

                    draggingView.startAnimation(move);

                } else { //Not dragging

                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        handled = true;

                        performItemClick(draggingView, startDragIndex, startDragIndex);

                        // Remove the highlight of the pictogram
                        ((PictogramView) draggingView).placeDown();

                        // Reset the dragging parameters
                        startDragIndex = -1;
                        draggingView = null;
                    }
                }
            }

            return handled;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                draggingView = childAtPoint((int) x, (int) y);
                if (draggingView != null && draggingView != addNewPictoGramView) {

                    //Grap original drag position
                    startDragIndex = indexOfChild(draggingView);

                    if (isInEditMode) {
                        handled = true;

                        ((PictogramView) draggingView).liftUp();

                        for (int i = 0; i < this.getChildCount(); i++) {
                            this.getChildAt(i).invalidate();
                        }

                        requestDisallowInterceptTouchEvent(true);

                        curDragIndexPos = startDragIndex;

                        //Everything is in the right place at the start.
                        resetViewPositions();

                        touchX = (int) x;
                        dragStartX = touchX;
                        centerOffset = touchX - getCenterX(startDragIndex);

                        draggingView.invalidate();
                    } else {
                        // When a pictogram is clicked when not in editmode it is selected
                        setLowHighLighting(startDragIndex);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (draggingView == null) {  // The user is not touching a pictogram
                    handled = true;
                } else {  // The user is touching a pictogram

                    if (!isDragging && Math.abs(dragStartX - x) >= DRAG_DISTANCE) {  // The user is starting to move a pictogram
                        isDragging = true;
                        startAutoScroll();
                    }

                    handled = true;
                    handleTouchMove(x);
                }
                break;
        }
        return handled;
    }

    private void resetViewPositions() {
        newPositions = new int[getChildCount()];
        for (int i = 0; i < newPositions.length; i++) {
            newPositions[i] = i;
        }
    }

    private void setLowHighLighting(int selectedIndex) {
        for (int i = 0; i < getCurrentSequenceViewCount(); i++) {
            PictogramView pictogram = (PictogramView) this.getChildAt(i);

            if (i < selectedIndex) {
                pictogram.setLowlighted();
            } else if (i > selectedIndex) {
                pictogram.setLowlighted();
            } else {
                pictogram.setSelected();
            }
        }
    }

    private void startAutoScroll() {
        if (autoScroller == null)
            autoScroller = new AutoScroller();

        doAutoScroll = true;
        autoScroller.reset();
        post(autoScroller);
    }

    private void stopAutoScroll() {
        doAutoScroll = false;
        removeCallbacks(autoScroller);
    }

    private void handleTouchMove(float newTouchX) {
        if (!isDragging) return;

        touchX = (int) newTouchX;
        touchDeltaX = (int) (newTouchX - dragStartX);
        //Layout the dragging element. Is excluded from normal layout
        int newLeft = calcChildLeftPosition(startDragIndex) + touchDeltaX;
        draggingView.layout(newLeft, draggingView.getTop(), newLeft + draggingView.getMeasuredWidth(), draggingView.getTop() + draggingView.getMeasuredHeight());

        checkForSwap();
    }

    public void setHorizontalSpacing(int spacing) {
        if (spacing >= 0 && spacing != horizontalSpacing) {
            horizontalSpacing = spacing;
            requestLayout();
        }
    }

    public void setItemHeight(int height) {
        if (height > 0 && height != itemHeight) {
            itemHeight = height;
            requestLayout();
        }
    }

    public void setItemWidth(int width) {
        if (width > 0 && width != itemWidth) {
            itemWidth = width;
            requestLayout();
        }
    }

    /**
     * Sets the OnRearrangeListener that is called when Views are rearranged.
     * Can be null
     *
     * @param rearrangeListener
     */
    public void setOnRearrangeListener(OnRearrangeListener rearrangeListener) {
        this.rearrangeListener = rearrangeListener;
    }

    private void swapIndexPositions(int indexA, int indexB) {
        int temp = newPositions[indexA];
        newPositions[indexA] = newPositions[indexB];
        newPositions[indexB] = temp;
    }

    public interface OnRearrangeListener {
        public void onRearrange(int indexFrom, int indexTo);
    }

    private int getCurrentSequenceViewCount() {
        int numChildren = getChildCount();
        if (numChildren > 0)
            return isInEditMode ? numChildren - 1 : numChildren;
        return 0;
    }

    public void setEditModeEnabled(boolean editEnabled) {
        if (isInEditMode != editEnabled) {
            isInEditMode = editEnabled;

            if (editEnabled)
                addViewInLayout(addNewPictoGramView, -1, generateDefaultLayoutParams(), true);
            else
                removeViewInLayout(addNewPictoGramView);

            requestLayout();
        }
    }

    @Override
    public SequenceAdapter getAdapter() {
        return adapter;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setAdapter(SequenceAdapter adapter) {
        SequenceAdapter oldAdapter = this.adapter;

        if (oldAdapter == adapter) return;

        this.adapter = adapter;

        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(this.observer);
        }

        if (adapter != null) {
            adapter.registerDataSetObserver(this.observer);
        }

        requestLayout();
    }

    @Override
    public void setSelection(int position) {
        return;
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            setAdapter(null);
        }
    }

    /**
     * AutoScrollers scrolls the SequenceViewGroup during a draw if
     * the dragged point is near the border.
     */
    private class AutoScroller implements Runnable {

        private final float MAX_SCROLL_SPEED_PER_MS = 0.52f;
        private final int BORDER_DIST_TO_SCROLL = 340;

        private int currentScrollX = 0;
        private int currentScrollY = 0;

        View scroller;
        View parent;
        long timeBefore = -1;

        private View getScroller(View viewGroup) {
            return (View) viewGroup.getParent().getParent();
        }

        public AutoScroller() {
            this.scroller = getScroller(SequenceViewGroupPortraitAndLandscape.this);

            //if (!(this.scroller instanceof HorizontalScrollView))
            //    throw new IllegalStateException("Parent of SequenceViewGroup must be HorizontalScrollView");
            timeBefore = AnimationUtils.currentAnimationTimeMillis();
            reset();
        }

        public void reset() {
            timeBefore = -1;
            parent = SequenceViewGroupPortraitAndLandscape.this;
            scroller = getScroller(parent);
            currentScrollX = scroller.getScrollX();
            currentScrollY = scroller.getScrollY();
        }

        @Override
        public void run() {

            final long timeNow = AnimationUtils.currentAnimationTimeMillis();

            if (timeBefore == -1)
                timeBefore = timeNow;

            final long timeDifference = timeNow - timeBefore;
            timeBefore = timeNow;

            if (isDoneScrolling())
                return;

            //Find the closest border
            int borderRightDist = scroller.getWidth() - getRelativePosInSeqViewGrp(touchX);
            int borderLeftDist = getRelativePosInSeqViewGrp(touchX);

            int borderDist = Math.min(borderRightDist, borderLeftDist);

            //Not year border. Maybe next time
            if (borderDist > BORDER_DIST_TO_SCROLL) {
                post(this);
                return;
            }

            int scrollAmount = (int) calculateMovement(timeDifference, borderDist);

            //Scrolling left?
            if (borderLeftDist < borderRightDist)
                scrollAmount = -scrollAmount;

            final int prevScrollX = currentScrollX;

            //scrollTo, unlike scrollBy, clamps within the child bounds.
            scroller.scrollTo(currentScrollX + scrollAmount, currentScrollY);
            currentScrollX = scroller.getScrollX();

            SequenceViewGroupPortraitAndLandscape.this.handleTouchMove(touchX + (currentScrollX - prevScrollX));

            post(this);

        }

        private float calculateMovement(final long timeDifference, int borderDist) {
            //Interpolation is: 1 - (1 - x)^2
            final float x = (float) (BORDER_DIST_TO_SCROLL - borderDist) / BORDER_DIST_TO_SCROLL;
            final float inv = 1 - x;
            return (1 - inv * inv) * MAX_SCROLL_SPEED_PER_MS * timeDifference;
        }

        private int getRelativePosInSeqViewGrp(int touchX) {
            return touchX - scroller.getScrollX();
        }

        private boolean isDoneScrolling() {
            return !doAutoScroll;
        }
    }

    public void liftUpAddNewButton() {
        addNewPictoGramView.setScaleX(PictogramView.HIGHLIGHT_SCALE);
        addNewPictoGramView.setScaleY(PictogramView.HIGHLIGHT_SCALE);
        addNewPictoGramView.setAlpha(0.7f);
    }

    public void placeDownAddNewButton() {
        addNewPictoGramView.setScaleX(PictogramView.NORMAL_SCALE);
        addNewPictoGramView.setScaleY(PictogramView.NORMAL_SCALE);
        addNewPictoGramView.setAlpha(1f);
    }


    public void setOnNewButtonClickedListener(OnNewButtonClickedListener listener) {
        newButtonClickedListener = listener;
    }

    public OnNewButtonClickedListener getOnNewButtonClickedListener() {
        return newButtonClickedListener;
    }

    public interface OnNewButtonClickedListener {
        public void onNewButtonClicked();
    }
}
