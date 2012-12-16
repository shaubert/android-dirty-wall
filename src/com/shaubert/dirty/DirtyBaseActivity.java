package com.shaubert.dirty;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.shaubert.util.Versions;

@TargetApi(11)
public class DirtyBaseActivity extends JournalBasedFragmentActivity {

    private View petr;
    private AnimationDrawable petrBackgroung;
    private Animation petrMovement;
    
    private Gertruda gertruda;
    
    protected View dirtyTv;
    private AnimationDrawable dirtyTvAnimation;
    
    protected DirtyPreferences dirtyPreferences;
    protected DirtyMessagesProvider dirtyMessagesProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!Versions.isApiLevelAvailable(11)) {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }
        super.onCreate(savedInstanceState);
        dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(this), this);
        dirtyMessagesProvider = DirtyMessagesProvider.getInstance(getApplicationContext());
        
        setContentView(R.layout.l_dirty_base_activity);
    }
    
    @Override
    public void onContentChanged() {
    	super.onContentChanged();
        petr = findViewById(R.id.petr);
        petrBackgroung = (AnimationDrawable)petr.getBackground();
        
        dirtyTv = findViewById(R.id.empty_view_tv);
        dirtyTvAnimation = (AnimationDrawable) dirtyTv.getBackground();
    }

    public Gertruda getGertruda() {
    	if (gertruda == null) {
    		gertruda = new Gertruda(this);
    	}
		return gertruda;
	}
    
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (dirtyPreferences.isPetrEnabled()) {
                petrBackgroung.start();
                petrBackgroung.stop();
                petrBackgroung.start();
            }
            dirtyTvAnimation.start();
        }
    }

    @Override
    public void onStartInitialRequests() {
        super.onStartInitialRequests();
        getGertruda().onStartInitialRequests();
    }

    @Override
    public void onRestoreRequestState(Bundle savedInstanceState) {
        super.onRestoreRequestState(savedInstanceState);
        getGertruda().onRestoreRequestState(savedInstanceState);
    }

	protected Intent attachRequestsIds(Intent intent) {
		intent.putExtra(Gertruda.GERTRUDA_LOAD_REQUEST_ID, getGertruda().getGertrudaLoadRequestId());
		return intent;
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getGertruda().onSaveInstanceState(outState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (dirtyPreferences.isPetrEnabled()) {
            if (petr.getAnimation() == null) {
                petrMovement = AnimationUtils.loadAnimation(this, R.anim.petr_movement);
                petr.startAnimation(petrMovement);
            }
            petr.setVisibility(View.VISIBLE);
        } else {
            petr.setVisibility(View.GONE);
            petr.clearAnimation();
        }
    }

	protected void startGertrudaRefresh(String gertrudaUrl) {
		getGertruda().startGertrudaRefresh(gertrudaUrl);
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        
        petrBackgroung.stop();
        dirtyTvAnimation.stop();
        
        getGertruda().onPause();
    }
        
}