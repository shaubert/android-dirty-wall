package com.shaubert.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedSizeImageView extends ImageView {

    public FixedSizeImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSizeImageView(Context context) {
        super(context);
    }

    @Override
    public void requestLayout() {
    }
    
    public void callRequestLayout() {
        super.requestLayout();
    }

}
