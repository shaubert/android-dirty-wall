package com.shaubert.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GertrudaImageView extends ImageView {

    public GertrudaImageView(Context context) {
        super(context);
        init();
    }

    public GertrudaImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GertrudaImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setColorFilter(Color.WHITE, PorterDuff.Mode.OVERLAY);
    }

}
