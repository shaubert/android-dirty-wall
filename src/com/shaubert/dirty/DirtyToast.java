package com.shaubert.dirty;

import com.shaubert.util.Sizes;
import com.shaubert.util.Versions;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DirtyToast {

    public static void show(Context context, int imageId, CharSequence text) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.l_dirty_toast, null);

        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageResource(imageId);
        TextView textView = (TextView) layout.findViewById(R.id.text);
        textView.setText(text);

        Toast toast = new Toast(context.getApplicationContext());
        if (Versions.isApiLevelAvailable(11)) {
            toast.setGravity(Gravity.TOP| Gravity.CENTER_HORIZONTAL, 0, Sizes.dpToPx(45, context));
        } else {
            toast.setGravity(Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL, 0, 0);
        }
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();        
    }
    
}
