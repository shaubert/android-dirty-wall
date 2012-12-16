package com.shaubert.dirty;

import com.shaubert.dirty.net.RequestContract;
import com.shaubert.dirty.net.RequestService;
import com.shaubert.net.core.DefaultExecutorBridge;
import com.shaubert.net.core.DefaultJournal;
import com.shaubert.net.core.DefaultRequestRecreator;
import com.shaubert.net.core.LoaderBasedRequestStateWatcher;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestRepositoryOnContentResolver;
import com.shaubert.net.core.RequestStatusListener;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class JournalBasedFragmentActivity extends FragmentActivity {

    public static class DefaultStatusListener extends RequestStatusListener {
    	private JournalBasedFragmentActivity activity;
    	
        public DefaultStatusListener(JournalBasedFragmentActivity activity) {
			this.activity = activity;
		}

		@Override
        public void onFinished(Request request) {
            activity.journal.unregisterForUpdates(request);
        }
                
        @Override
        public void onError(Request request) {
            activity.journal.unregisterForUpdates(request);
        }
    };
    
    private DefaultJournal journal;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setupJournal();
        if (savedInstanceState != null) {
            onRestoreRequestState(savedInstanceState);
        } else {
            onStartInitialRequests();
        }
    }
    
    public void onStartInitialRequests() {
    }

    public void onRestoreRequestState(Bundle savedInstanceState) {
    }

    protected void setupJournal() {
        RequestRepositoryOnContentResolver repository = new RequestRepositoryOnContentResolver(
                getBaseContext(), new DefaultRequestRecreator(getBaseContext()), RequestContract.Request.URI);
        DefaultExecutorBridge executorBridge = new DefaultExecutorBridge(getApplicationContext(), RequestService.class);
        LoaderBasedRequestStateWatcher stateWatcher = new LoaderBasedRequestStateWatcher(repository, getBaseContext(), 
                getSupportLoaderManager(), Loaders.REQUEST_LOADER_MAPPER);
        journal = new DefaultJournal(repository, executorBridge, stateWatcher);
    }
    
    public long startRequest(RequestBase request) {
        journal.register(request);
        journal.registerForUpdates(request);
        return request.getState().getId();
    }
    
    public void cancelRequest(RequestBase request) {
        journal.cancel(request.getState().getId());
        journal.unregisterForUpdates(request);
    }
    
    public void cancelRequest(long requestId) {
        journal.cancel(requestId);
    }
    
    public void registerForUpdates(RequestBase request) {
        journal.registerForUpdates(request);
    }
    
    public void unregisterForUpdates(RequestBase request) {
        journal.unregisterForUpdates(request);
    }
    
    public <T extends RequestBase> T restoreAndRegisterIfNotFinished(long requestId) {
        T request = restore(requestId);
        if (!isFinished(request)) {
            journal.registerForUpdates(request);
        }
        return request;
    }
    
    public boolean isFinished(RequestBase request) {
        return RequestStatus.isFinishedSomehow(request.getState().getStatus());
    }
    
    public <T extends RequestBase> T restoreAndRegister(long requestId) {
        T request = restore(requestId);
        journal.registerForUpdates(request);
        return request;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends RequestBase> T restore(long requestId) {
        return (T)journal.getRequest(requestId);
    }
    
}
