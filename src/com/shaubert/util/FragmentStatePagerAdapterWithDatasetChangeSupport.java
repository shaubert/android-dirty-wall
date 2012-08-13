package com.shaubert.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class FragmentStatePagerAdapterWithDatasetChangeSupport extends PagerAdapter {
    
        private static final String TAG = "FragmentAdapterDebug";
        private static final boolean DEBUG = true;

        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        private Map<Long, Fragment.SavedState> mSavedState = new HashMap<Long, Fragment.SavedState>();
        private Map<Long, Fragment> mFragments = new HashMap<Long, Fragment>();
        private HashMap<Integer, Long> stableIds = new HashMap<Integer, Long>();
        
        private Fragment mCurrentPrimaryItem = null;

        public FragmentStatePagerAdapterWithDatasetChangeSupport(FragmentManager fm) {
            mFragmentManager = fm;
        }

        /**
         * Return the Fragment associated with a specified position.
         */
        public abstract Fragment getItem(int position);

        public abstract long getStableId(int position);
        
        @Override
        public void startUpdate(ViewGroup container) {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // If we already have this item instantiated, there is nothing
            // to do.  This can happen when we are restoring the entire pager
            // from its saved state, where the fragment manager has already
            // taken care of restoring the fragments we previously had instantiated.
            Fragment f = getFragmentByPos(position);
            if (f != null) {
                return f;
            }

            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }

            Fragment fragment = getItem(position);
            long id = getStableId(position);
            if (DEBUG) Log.v(TAG, "Adding item " + position + "/" + id +": f=" + fragment);
            if (!mSavedState.isEmpty()) {
                Fragment.SavedState fss = mSavedState.get(id);
                if (fss != null) {
                    fragment.setInitialSavedState(fss);
                }
            }
            fragment.setMenuVisibility(false);
            stableIds.put(position, id);
            mFragments.put(id, fragment);
            Fragment curFr = mFragmentManager.findFragmentByTag("fr" + id);
            if (curFr != null) {
                mCurTransaction.remove(curFr);
            }
            mCurTransaction.add(container.getId(), fragment, "fr" + id);

            return fragment;
        }

        private Fragment getFragmentByPos(int pos) {
            Long oldId = stableIds.get(pos);
            long newId = getStableId(pos);
            if (oldId == null || oldId != newId) {
                return null;
            } else {
                return mFragments.get(newId);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment)object;

            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Long id = stableIds.get(position);
            if (DEBUG) Log.v(TAG, "Removing item " + position + "/" + id +": f=" + object
                    + " v=" + ((Fragment)object).getView());
            
            if (id != null) {
                if (fragment.isAdded() && !fragment.isDetached()) {
                    mSavedState.put(id, mFragmentManager.saveFragmentInstanceState(fragment));
                }
                mFragments.remove(id);
            }

            mCurTransaction.remove(fragment);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment)object;
            if (fragment != mCurrentPrimaryItem) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setMenuVisibility(false);
                }
                if (fragment != null) {
                    fragment.setMenuVisibility(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        public Fragment getCurrentPrimaryItem() {
            return mCurrentPrimaryItem;
        }
        
        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment)object).getView() == view;
        }

        @Override
        public Parcelable saveState() {
            Bundle state = null;
            for (Entry<Long, Fragment.SavedState> entry : mSavedState.entrySet()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "fs" + entry.getKey();
                state.putParcelable(key, entry.getValue());
            }
            for (Entry<Long, Fragment> entry : mFragments.entrySet()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "fr" + entry.getKey();
                Fragment fragment = entry.getValue();
                if (fragment.isAdded() && !fragment.isDetached()) {
                    mFragmentManager.putFragment(state, key, fragment);
                }
            }
            if (!stableIds.isEmpty()) {
                if (state == null) {
                    state = new Bundle();
                }
                state.putSerializable("st", stableIds);
            }
            return state;
        }
                
        @SuppressWarnings("unchecked")
        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            if (state != null) {
                Bundle bundle = (Bundle)state;
                bundle.setClassLoader(loader);
                mSavedState.clear();
                mFragments.clear();
                Iterable<String> keys = bundle.keySet();
                for (String key: keys) {
                    if (key.startsWith("fr")) {
                        long id = Long.parseLong(key.substring(2));
                        Fragment f = mFragmentManager.getFragment(bundle, key);
                        if (f != null) {
                            f.setMenuVisibility(false);
                            mFragments.put(id, f);
                        } else {
                            Log.w(TAG, "Bad fragment at key " + key);
                        }
                    } else if (key.startsWith("fs")) {
                        long id = Long.parseLong(key.substring(2));
                        Fragment.SavedState frState = bundle.getParcelable(key);
                        mSavedState.put(id, frState);
                    } else if (key.equals("st")) {
                        stableIds = (HashMap<Integer, Long>)bundle.getSerializable("st");
                    }
                }
            }
        }
    }