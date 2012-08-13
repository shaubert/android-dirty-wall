package com.shaubert.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.MetricAffectingSpan;

public class Spannables {

	//http://code.google.com/p/android/issues/detail?id=35412
	//http://code.google.com/p/android/issues/detail?id=35259
	public static void clearMetrictAffectingSpansIfJB(Spanned text) {
        if (Versions.isApiLevelAvailable(16) && text instanceof SpannableStringBuilder) {
        	SpannableStringBuilder builder = (SpannableStringBuilder) text;
        	MetricAffectingSpan[] spans = builder.getSpans(0, builder.length(), MetricAffectingSpan.class);
			for (Object span : spans) {
        		builder.removeSpan(span);
        	}
        }
	}
	
}
