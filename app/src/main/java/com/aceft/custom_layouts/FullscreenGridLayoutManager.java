package com.aceft.custom_layouts;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class FullscreenGridLayoutManager extends GridLayoutManager {
    private int offset = -1;

    public FullscreenGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
        final int widthMode = View.MeasureSpec.getMode(widthSpec);
        final int heightMode = View.MeasureSpec.getMode(heightSpec);
        final int widthSize = View.MeasureSpec.getSize(widthSpec);
        final int heightSize = View.MeasureSpec.getSize(heightSpec);

        if (offset < 0) {
            offset = getSpanCount() - widthSize % getSpanCount();
        }

        int width;
        int height;

        switch (widthMode) {
            case View.MeasureSpec.EXACTLY:
            case View.MeasureSpec.AT_MOST:
                width = widthSize;
                break;
            case View.MeasureSpec.UNSPECIFIED:
            default:
                width = getMinimumWidth();
                break;
        }

        switch (heightMode) {
            case View.MeasureSpec.EXACTLY:
            case View.MeasureSpec.AT_MOST:
                height = heightSize;
                break;
            case View.MeasureSpec.UNSPECIFIED:
            default:
                height = getMinimumHeight();
                break;
        }

        setMeasuredDimension(width+offset, height);
    }
}
