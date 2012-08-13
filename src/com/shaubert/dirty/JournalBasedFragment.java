package com.shaubert.dirty;

import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStatusListener;
import com.shaubert.net.nutshell.Request;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class JournalBasedFragment extends Fragment {

    public class DefaultStatusListener extends RequestStatusListener {
        @Override
        public void onFinished(Request request) {
            unregisterForUpdates((RequestBase)request);
        }
                
        @Override
        public void onError(Request request) {
            unregisterForUpdates((RequestBase)request);
        }
    };
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
    
    public JournalBasedFragmentActivity getJournalBasedActivity() {
        return (JournalBasedFragmentActivity)getActivity();
    }
    
    public long startRequest(RequestBase request) {
        return getJournalBasedActivity().startRequest(request);
    }
    
    public void cancelRequest(RequestBase request) {
        getJournalBasedActivity().cancelRequest(request);
    }
    
    public void cancelRequest(long requestId) {
        getJournalBasedActivity().cancelRequest(requestId);
    }
    
    public void registerForUpdates(RequestBase request) {
        getJournalBasedActivity().registerForUpdates(request);
    }
    
    public void unregisterForUpdates(RequestBase request) {
        if (getJournalBasedActivity() != null) {
            getJournalBasedActivity().unregisterForUpdates(request);
        }
    }
    
    public <T extends RequestBase> T restoreAndRegisterIfNotFinished(long requestId) {
        return getJournalBasedActivity().restoreAndRegisterIfNotFinished(requestId);
    }
    
    public boolean isFinished(RequestBase request) {
        return getJournalBasedActivity().isFinished(request);
    }
    
    public <T extends RequestBase> T restoreAndRegister(long requestId) {
        return getJournalBasedActivity().restoreAndRegister(requestId);
    }
    
    public <T extends RequestBase> T restore(long requestId) {
        return getJournalBasedActivity().restore(requestId);
    }
}
