package com.aceft.custom_layouts;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;

public class FloatingButtonSmall extends FloatingActionButton{

    public FloatingButtonSmall(Context context) {
        super(context);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int preferredSize = (int)this.getResources().getDimension(android.support.design.R.dimen.fab_content_size);
        int w = resolveAdjustedSize(preferredSize, widthMeasureSpec);
        int h = resolveAdjustedSize(preferredSize, heightMeasureSpec);
        int d = Math.min(w, h);
        this.setMeasuredDimension(d + 20, d + 20);
    }

    private static int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch(specMode) {
            case -2147483648:
                result = Math.min(desiredSize, specSize);
                break;
            case 0:
                result = desiredSize;
                break;
            case 1073741824:
                result = specSize;
        }

        return result;
    }

}
