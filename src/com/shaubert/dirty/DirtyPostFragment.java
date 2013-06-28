package com.shaubert.dirty;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.text.TextUtils;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import com.shaubert.dirty.DirtyPostView.OnCommentLoadClickListener;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.CommentsCursor;
import com.shaubert.dirty.db.DirtyPostLoaderCallbacks;
import com.shaubert.dirty.net.DirtyCommentsLoadRequest;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.Shlog;
import com.shaubert.util.Versions;
import com.shaubert.util.Views;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class DirtyPostFragment extends JournalBasedFragment {

    private static final Shlog SHLOG = new Shlog(DirtyPostFragment.class.getSimpleName());
    
    private static final String POST_ID = "post-id";
    private static final String COMMENTS_LOAD_REQUEST_ID = "comments-load-request-id"; 
    
    private ListView contentListView;
    private View progress;
    
    private DirtyPost dirtyPost;
    private DirtyPostLoaderCallbacks postLoaderCallbacks;
    private long postId;
    private DirtyPostView postView;
    
    private DirtyCommentsLoadRequest commentsLoadRequest;
    private long commentsLoadRequestId;
    private DirtyCommentsAdapter commentsAdapter;
    private ShareActionProvider shareActionProvider;
    private int contextMenuInfoItemPosition;

    private DirtyPreferences dirtyPreferences;

    private ActionMode postAndCommentActionMode;

    private class DirtyCommentsLoadRequestListener extends DefaultStatusListener {
        @Override
        public void onFinished(Request request) {
            super.onFinished(request);
            if (getActivity() != null) {
                hideCommentsLoadingProgress();
            }
        }

        @Override
        public void onError(Request request) {
            super.onError(request);
            if (getActivity() != null) {
                DirtyMessagesProvider prov = DirtyMessagesProvider.getInstance(getActivity().getApplicationContext());
                if (dirtyPreferences.isUseCrouton()) {
                    Crouton.clearCroutonsForActivity(getActivity());
                    Crouton.makeText(getActivity(), prov.getSimpleErrorMessage(), Style.ALERT).show();
                } else {
                    DirtyToast.show(getActivity(), prov.getRandomFaceImageId(), prov.getErrorMessage());
                }
                hideCommentsLoadingProgress();
            }
        }
        
    }

    public static DirtyPostFragment newInstance(long postId) {
        Bundle bundle = new Bundle();
        bundle.putLong(POST_ID, postId);
        DirtyPostFragment result = new DirtyPostFragment();
        result.setArguments(bundle);
        return result;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dirtyPreferences = new DirtyPreferences(PreferenceManager.getDefaultSharedPreferences(getActivity()), getActivity());
        postId = getArguments().getLong(POST_ID);
    }
    
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        setHasOptionsMenu(menuVisible);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.l_post_and_comments_list, container, false);
        contentListView = (ListView)result.findViewById(R.id.post_listview);
        progress = result.findViewById(R.id.post_loading_progress);
                
        postView = new DirtyPostView(getJournalBasedActivity(), postId);
        contentListView.addHeaderView(postView, null, true);
        contentListView.addFooterView(Views.createVerticalSpacer(getActivity(), 30), null, false);
        postView.setCommentLoadClickListener(new OnCommentLoadClickListener() {
            @Override
            public void onCommentLoadClicked(DirtyPostView dirtyPostPresenter) {
                loadComments(true);
            }
        });
        
        commentsAdapter = new DirtyCommentsAdapter(getActivity(), postId);
        contentListView.setAdapter(commentsAdapter);        
        contentListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                boolean result = createPostAndCommentActionMode();
                if (result) {
                    contentListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    contentListView.setItemChecked(position, true);
                }
                return result;
            }
        });
        
        if (!Versions.isApiLevelAvailable(11)) {
            registerForContextMenu(contentListView);
        }

        initDirtyPostLoader();
        commentsAdapter.initLoader();

        return result;
    }

    protected boolean createPostAndCommentActionMode() {
        if (Versions.isApiLevelAvailable(11)) {
            if (postAndCommentActionMode == null) {
                PostAndCommentActionModeCallback commentActionModeCallback = new PostAndCommentActionModeCallback() {
                    @Override
                    protected void copyMessage() {
                        copyPostOrCommentAtPos(contentListView.getCheckedItemPosition());
                    }
                    
                    @Override
                    protected void copyLink() {
                        copyPostOrCommentLinkAtPos(contentListView.getCheckedItemPosition());
                    }
                    
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        postAndCommentActionMode = null;
                        clearListViewSelection();
                    }

                };
                postAndCommentActionMode = getActivity().startActionMode(commentActionModeCallback);
            }
            return true;
        } else {
            return false;
        }
    }
    
    private void copyPostOrCommentAtPos(int pos) {
        if (pos > 0) {
            if (pos < contentListView.getAdapter().getCount()) {
                CommentsCursor commentsCursor = (CommentsCursor)contentListView.getAdapter().getItem(pos);
                String message = commentsCursor.getMessage();
                copyText(message);
            }
        } else {
            copyText(dirtyPost.getMessage());
        }
    }
    
    private void copyPostOrCommentLinkAtPos(int pos) {
        DirtyBlog blog = DirtyBlog.getInstance();
        if (pos > 0) {
            if (pos < contentListView.getAdapter().getCount()) {
                copyText(blog.getCommentLink(dirtyPost, 
                        ((CommentsCursor) contentListView.getAdapter().getItem(pos)).getServerId()));
            }
        } else {
            copyText(blog.getPostLink(dirtyPost));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void copyText(String text) {
        if (!TextUtils.isEmpty(text)) {
            if (Versions.isApiLevelAvailable(11)) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText(text.substring(0, Math.min(text.length(), 15)), text);
                clipboard.setPrimaryClip(clipData);
            } else {
                android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setText(text);
            }
        }
    }
    
    private void clearListViewSelection() {
        for (int i = 0; i <= contentListView.getAdapter().getCount(); i++) {
            contentListView.setItemChecked(i, false);
        }
        contentListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshContent();
    }
    
    @Override
    public void onRestoreRequestState(Bundle savedInstanceState) {
        super.onRestoreRequestState(savedInstanceState);
        postView.restoreRequestState(savedInstanceState);
        
        commentsLoadRequestId = savedInstanceState.getLong(COMMENTS_LOAD_REQUEST_ID, -1);
        if (commentsLoadRequestId > 0) {
            AsyncTasks.executeInBackground(new Task<JournalBasedFragmentActivity>(getJournalBasedActivity()) {
                public void run(JournalBasedFragmentActivity activity) {
                    commentsLoadRequest = activity.restore(commentsLoadRequestId);
                    if (postId != commentsLoadRequest.getState().getLong("post-id", -1)) {
                        commentsLoadRequestId = -1;
                        commentsLoadRequest = null;
                    }
                }
                
                @Override
                public void onUiThread(JournalBasedFragmentActivity context) {
                    if (getActivity() != null && commentsLoadRequest != null) {
                        if (!RequestStatus.isFinishedSomehow(commentsLoadRequest.getState().getStatus())) {
                            commentsLoadRequest.setFullStateChangeListener(new DirtyCommentsLoadRequestListener());
                            registerForUpdates(commentsLoadRequest);
                            showCommentsLoadingProgress();
                        } else {
                            hideCommentsLoadingProgress();
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        postView.saveInstanceState(outState);
        outState.putLong(COMMENTS_LOAD_REQUEST_ID, commentsLoadRequestId);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        postView.resume();
        commentsAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onPause() {
        if (postAndCommentActionMode != null) {
            postAndCommentActionMode.finish();
        }
        super.onPause();
        postView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        postView.release();
        postLoaderCallbacks.destroyLoader();
        postLoaderCallbacks.setDirtyPostLoadedCallback(null);
        commentsAdapter.destroyLoader();
    }
    
    private void setContentVisible(boolean visible) {
        if (!visible) {
            progress.setVisibility(View.VISIBLE);
            progress.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.dirty_progress));
            contentListView.setVisibility(View.GONE);
        } else {
            if (contentListView.getVisibility() != View.VISIBLE) {
                contentListView.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                progress.clearAnimation();
                contentListView.startAnimation(AnimationUtils.makeInChildBottomAnimation(getActivity()));
            }
        }
    }

    public long getPostId() {
        return postId;
    }
    
    private void initDirtyPostLoader() {
        postLoaderCallbacks = new DirtyPostLoaderCallbacks(getActivity(), postId);
        postLoaderCallbacks.setDirtyPostLoadedCallback(new DirtyPostLoaderCallbacks.DirtyPostLoadedCallback() {
            @Override
            public void onDirtyPostLoaded(DirtyPost dirtyPost) {
                setDirtyPost(dirtyPost);
            }
        });
        postLoaderCallbacks.initLoader();
    }
    
    public void setDirtyPost(DirtyPost dirtyPost) {
        this.dirtyPost = dirtyPost;
        postView.setDirtyPost(dirtyPost);
        refreshContent();
        
        if (Versions.isApiLevelAvailable(14) && shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        }
    }

    private void refreshContent() {
        if (getActivity() != null) {
            if (dirtyPost != null) {
                setContentVisible(true);
            } else {
                setContentVisible(false);
            }
        }
    }    
    
    private void loadComments(boolean force) {
        if (commentsLoadRequest == null || RequestStatus.isFinishedSomehow(commentsLoadRequest.getState().getStatus())) {
            commentsLoadRequest = new DirtyCommentsLoadRequest();
            commentsLoadRequest.getState().put("post-id", postId);
            commentsLoadRequest.getState().put("force", force);
            commentsLoadRequest.setFullStateChangeListener(new DirtyCommentsLoadRequestListener());
            commentsLoadRequestId = startRequest(commentsLoadRequest);
            showCommentsLoadingProgress();
        }
    }

    private void showCommentsLoadingProgress() {
        if (postView != null && !postView.isReleased()) {
            postView.showCommentsLoadingProgress();
        }
    }

    private void hideCommentsLoadingProgress() {
        if (postView != null && !postView.isReleased()) {
            postView.hideCommentsLoadingProgress();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.post_options_menu, menu);
        
        MenuItem shareMenuItem = menu.findItem(R.id.share_menu_item);
        if (Versions.isApiLevelAvailable(14)) {
            shareActionProvider = ((ShareActionProvider)shareMenuItem.getActionProvider());
            if (dirtyPost != null) {
                shareActionProvider.setShareIntent(createShareIntent());
            }
        }
    }
        
    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, DirtyBlog.getInstance().getPostLink(dirtyPost));
        intent.setType("text/plain");
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_menu_item:
                if (sharePost(false)) {
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
                
            case R.id.open_in_browser_menu_item:
                openPostInBrowser();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        MenuInflater inflater = new MenuInflater(getActivity());
        inflater.inflate(R.menu.post_and_comment_context_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.copy_text_menu_item:
                copyPostOrCommentAtPos(info == null ? contextMenuInfoItemPosition : info.position);
                return true;
            case R.id.copy_url_menu_item:
                copyPostOrCommentLinkAtPos(info == null ? contextMenuInfoItemPosition : info.position);
                return true;
                
            default:
                if (info != null) {
                    contextMenuInfoItemPosition = info.position;
                }
                return super.onContextItemSelected(item);
        }
    }

    private void openPostInBrowser() {
        if (dirtyPost != null && getActivity() != null) {
            Uri uri = Uri.parse(DirtyBlog.getInstance().getPostLink(dirtyPost));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
            getActivity().startActivity(intent);
        }
    }

    private boolean sharePost(boolean fromContextMenu) {
        if (!Versions.isApiLevelAvailable(14) || fromContextMenu) {
            Intent shareIntent = createShareIntent();            
            Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share));
            try {
                startActivity(chooserIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                SHLOG.w(ex);
            }            
            return true;
        } else {
            return false;
        }
    }
    
}
