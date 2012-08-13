package com.shaubert.dirty;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class PostAndCommentActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.post_and_comment_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copy_text_menu_item:
                copyMessage();
                mode.finish();
                return true;
            case R.id.copy_url_menu_item:
                copyLink();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    protected void copyMessage() {
        
    }
    
    protected void copyLink() {
        
    }

};
