/***
 Copyright (c) 2008-2009 CommonsWare, LLC
 Portions (c) 2009 Google, Inc.

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.endless;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import com.commonsware.cwac.adapter.AdapterWrapper;
import com.shaubert.dirty.R;

/**
 * Adapter that assists another adapter in appearing endless. For example, this could be used for an adapter being
 * filled by a set of Web service calls, where each call returns a "page" of data.
 * <p/>
 * Subclasses need to be able to return, via getPendingView() a row that can serve as both a placeholder while more data
 * is being appended. * If your situation is such that you will not know if there is more data until you do some work
 * (e.g., make another Web service call), it is up to you to do something useful with that row returned by
 * getPendingView() to let the user know you are out of data.
 */
public abstract class EndlessAdapter extends AdapterWrapper {

    private View pendingView = null;
    private boolean keepOnAppending = true;
    private boolean loading;
    private Context context;
    private int pendingResource = R.layout.endless_adapter_progress;
    private int errorResource = R.layout.endless_adapter_error_loading;
    private boolean hasError = false;
    private View errorView = null;
    private boolean showEmptyItem;

    /**
     * Constructor wrapping a supplied ListAdapter
     */
    public EndlessAdapter(Context context, ListAdapter wrapped) {
        super(wrapped);
        this.context = context;
    }

    /**
     * Constructor wrapping a supplied ListAdapter and providing a id for a pending view.
     *
     * @param context
     * @param wrapped
     * @param pendingResource
     */
    public EndlessAdapter(Context context, ListAdapter wrapped,
                          int pendingResource, int errorResource) {
        super(wrapped);
        this.context = context;
        this.pendingResource = pendingResource;
    }

    public void restartAppending() {
        keepOnAppending = true;
        notifyDataSetChanged();
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return super.getWrappedAdapter();
    }

    /**
     * Use to manually notify the adapter that it's dataset has changed. Will remove the pendingView and update the
     * display.
     */
    public void onDataReady() {
        this.pendingView = null;
        this.loading = false;
        notifyDataSetChanged();
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
        this.errorView = null;
        notifyDataSetChanged();
    }

    public void setAdapterIsFull() {
        keepOnAppending = false;
        notifyDataSetChanged();
    }

    /**
     * How many items are in the data set represented by this Adapter.
     */
    @Override
    public int getCount() {
        if (keepOnAppending || hasError) {
            return (super.getCount() + 1); // one more for
            // "pending"
        }

        return (super.getCount());
    }

    public boolean isHasError() {
        return hasError;
    }

    /**
     * Masks ViewType so the AdapterView replaces the "Pending" row when new data is loaded.
     */
    @Override
    public int getItemViewType(int position) {
        if (position == getWrappedAdapter().getCount()) {
            return (IGNORE_ITEM_VIEW_TYPE);
        }

        return (super.getItemViewType(position));
    }

    /**
     * Masks ViewType so the AdapterView replaces the "Pending" row when new data is loaded.
     *
     * @see #getItemViewType(int)
     */
    @Override
    public int getViewTypeCount() {
        return (super.getViewTypeCount() + 2);
    }

    @Override
    public Object getItem(int position) {
        if (position >= super.getCount()) {
            return (null);
        }

        return (super.getItem(position));
    }

    /**
     * Get a View that displays the data at the specified position in the data set. In this case, if we are at the end
     * of the list and we are still in append mode, we ask for a pending view and return it, plus kick off the
     * background task to append more data to the wrapped adapter.
     *
     * @param position    Position of the item whose data we want
     * @param convertView View to recycle, if not null
     * @param parent      ViewGroup containing the returned View
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int ourItemPosition = super.getCount();

        if (position == ourItemPosition) {
            if (hasError) {
                if (errorView == null) {
                    errorView = getErrorView(parent);
                }
                return errorView;
            } else if (keepOnAppending) {
                if (pendingView == null) {
                    pendingView = getPendingView(parent);
                    if (!loading) {
                        loading = true;
                        onLoadMore();
                    }
                }
                return pendingView;
            }
        }
        return (super.getView(position, convertView, parent));
    }

    protected abstract void onLoadMore();

    public void retry() {
        if (pendingView == null) {
            errorView = null;
            hasError = false;
            keepOnAppending = true;
            notifyDataSetChanged();
        }
    }

    /**
     * Inflates pending view using the pendingResource ID passed into the constructor
     *
     * @param parent
     * @return inflated pending view, or null if the context passed into the pending view constructor was null.
     */
    protected View getPendingView(ViewGroup parent) {
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(pendingResource, parent, false);
        }

        throw new RuntimeException(
                "You must either override getPendingView() or supply a pending View resource via the constructor");
    }

    /**
     * Inflates error view using the errorResource ID passed into the constructor
     *
     * @param parent
     * @return inflated pending view, or null if the context passed into the pending view constructor was null.
     */
    protected View getErrorView(ViewGroup parent) {
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(errorResource, parent, false);
            view.setClickable(true);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    retry();
                }
            });
            return view;
        }

        throw new RuntimeException(
                "You must either override getErrorView() or supply a error View resource via the constructor");
    }

    /**
     * Getter method for the Context being held by the adapter
     *
     * @return Context
     */
    protected Context getContext() {
        return (context);
    }
}
