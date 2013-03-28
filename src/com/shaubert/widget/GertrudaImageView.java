package com.shaubert.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GertrudaImageView extends ImageView {

    private boolean colorFilterEnabled;

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
        setColorFilterEnabled(true);
    }

    public void setColorFilterEnabled(boolean colorFilterEnabled) {
        if (this.colorFilterEnabled != colorFilterEnabled) {
            this.colorFilterEnabled = colorFilterEnabled;
            if (colorFilterEnabled) {
                setColorFilter(Color.argb(110, 255, 255, 255), PorterDuff.Mode.LIGHTEN);
            } else {
                setColorFilter(null);
            }
        }

    }

}
