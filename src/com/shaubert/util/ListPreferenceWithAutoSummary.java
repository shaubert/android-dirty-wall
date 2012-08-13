package com.shaubert.util;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListPreferenceWithAutoSummary extends ListPreference {

    public ListPreferenceWithAutoSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListPreferenceWithAutoSummary(Context context) {
        super(context);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(getEntry());
    }
        
}