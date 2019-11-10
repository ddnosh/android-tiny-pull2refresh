package com.androidwind.tinypull2refresh;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

/**
 * @author ddnosh
 * @website http://blog.csdn.net/ddnosh
 */
public class Pull2RefreshLayout extends ViewGroup {

    private static final String TAG = "Pull2RefreshLayout";

    private static final int TYPE_TARGET_NORMAL = 1;
    private static final int TYPE_TARGET_LIST = 2;
    private static final int TYPE_TARGET_RECYCLER = 3;

    private static final float DRAGGING_RATE = 0.5f;

    private View mHeadView;
    private View mTargetView;
    private int mHeadViewHeight;
    private int mMovement = 0;
    private int mTargetViewType;
    private boolean mIsRefreshing;
    private ProgressBar mProgressBar;

    private Pull2RefreshListener mPullToRefreshListener;

    public Pull2RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(generalHeadLoadingView());
    }

    private View generalHeadLoadingView() {
        mHeadView = LayoutInflater.from(getContext()).inflate(R.layout.head_pull2refresh, this, false);
        mProgressBar = mHeadView.findViewById(R.id.pb_progress);
        mProgressBar.setMax(100);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        return mHeadView;
    }

    public void setPullToRefreshListener(Pull2RefreshListener pullToRefreshListener) {
        this.mPullToRefreshListener = pullToRefreshListener;
    }

    public void stopRefreshing() {
        if (mIsRefreshing) {
            final ValueAnimator valueAnimator = ValueAnimator.ofInt(mMovement, 0);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mMovement = (int) animation.getAnimatedValue();
                    requestLayout();
                }
            });
            valueAnimator.start();
            mIsRefreshing = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i(TAG, "[onMeasure] widthMeasureSpec = " + widthMeasureSpec + ", heightMeasureSpec = " + heightMeasureSpec);
        int count = getChildCount();
        if (count < 2) {
            Log.e(TAG, "there should be one view wrapped in Pull2RefreshLayout at least.");
            return;
        }

        mHeadViewHeight = ScreenUtil.dip2px(getContext(), 80);
        if (mHeadView == null) {
            mHeadView = getChildAt(0);
        }
        mHeadView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mHeadViewHeight, MeasureSpec.EXACTLY));

        if (mTargetView == null) {
            mTargetView = getChildAt(1);
        }
        if (mTargetView instanceof ListView) {
            mTargetViewType = TYPE_TARGET_LIST;
        } else if (mTargetView instanceof RecyclerView) {
            mTargetViewType = TYPE_TARGET_RECYCLER;
        } else {
            mTargetViewType = TYPE_TARGET_NORMAL;
        }
        mTargetView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.i(TAG, "[onLayout] changed:" + changed + ", l = " + ", t = " + t + ", r = " + r + ", b = " + b);
        mHeadView.layout(0, -mHeadViewHeight + mMovement, r, mMovement);
        mTargetView.layout(0, mMovement, r, b);
    }

    private float mDownY;
    private float mDragY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.i(TAG, "onInterceptTouchEvent");
        if (mIsRefreshing) {
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                if (mTargetViewType == TYPE_TARGET_NORMAL) {
                    mDragY = ev.getY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float direction = ev.getY() - mDownY;
                if (direction > 0) {
                    if (mTargetViewType == TYPE_TARGET_LIST) {
                        ListView listView = (ListView) mTargetView;
                        if (!listView.canScrollVertically(-1)) {
                            mDragY = ev.getY();
                            return true;
                        }
                    } else if (mTargetViewType == TYPE_TARGET_RECYCLER) {
                        RecyclerView recyclerView = (RecyclerView) mTargetView;
                        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                        if (layoutManager instanceof LinearLayoutManager) {
                            if (((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
                                mDragY = ev.getY();
                                return true;
                            }
                        } else if (layoutManager instanceof GridLayoutManager) {
                            if (((GridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
                                mDragY = ev.getY();
                                return true;
                            }
                        }
                    } else {
                        mDragY = ev.getY();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "onTouchEvent");
        mMovement = (int) ((event.getY() - mDragY) * DRAGGING_RATE);
        requestLayout();
        int progress = mMovement * 100 / mHeadViewHeight;
        if (progress > 100) {
            progress = 100;
        }
        mProgressBar.setProgress(progress);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            triggerLoading();
        }
        return true;
    }

    private void triggerLoading() {
        int targetAnimationValue = 0;
        if (mMovement > mHeadViewHeight) {
            if (mPullToRefreshListener != null) {
                mPullToRefreshListener.onRefresh();
            }
            targetAnimationValue = mHeadViewHeight;
            mIsRefreshing = true;
        }


        final ValueAnimator valueAnimator = ValueAnimator.ofInt(mMovement, targetAnimationValue);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMovement = (int) animation.getAnimatedValue();
                requestLayout();
            }
        });
        valueAnimator.start();
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public interface Pull2RefreshListener {
        void onRefresh();
    }
}
