package com.shaubert.util;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Views {

    public static FrameLayout createVerticalSpacer(Context context, float dp) {
        FrameLayout frameLayout = new FrameLayout(context);
        TextView textView = new TextView(context);
        textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, Sizes.dpToPx(dp, context)));
        frameLayout.addView(textView);
        return frameLayout;
    }
	
}
