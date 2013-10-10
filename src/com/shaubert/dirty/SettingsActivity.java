package com.shaubert.dirty;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.shaubert.util.Shlog;
import com.shaubert.util.Versions;
import de.keyboardsurfer.android.widget.crouton.Crouton;

import java.util.List;

@TargetApi(11)
public class SettingsActivity extends PreferenceActivity {

	private static Shlog SHLOG = new Shlog(SettingsActivity.class.getSimpleName());
	
	public static class DirtyMainPreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.main_prefs);
		}

	}

    public static class DirtyAccountPreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.account_prefs);
            setLoginPreferenceListener(findPreference(getString(R.string.login_preference_key)), getActivity());
        }

    }

	public static class DirtySyncPreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.sync_prefs);
			
			setSyncPreferenceListener(findPreference(getString(R.string.posts_background_sync_key)));
			setSyncIntervalPreferenceListener(findPreference(getString(R.string.background_sync_period_key)));
		}

	}
	
	public static class DirtyFavoritesPreferencesFragment extends
			PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.favorites_prefs);

			setFavoritesPreferenceListener(findPreference(getString(R.string.export_favorites_pref_key)), getActivity());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!Versions.isApiLevelAvailable(11)) {
			addPreferencesFromResource(R.xml.old_prefs);
			setFavoritesPreferenceListener(findPreference(getString(R.string.export_favorites_pref_key)), this);
			setSyncIntervalPreferenceListener(findPreference(getString(R.string.background_sync_period_key)));
			setLoginPreferenceListener(findPreference(getString(R.string.login_preference_key)), this);
		} else {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}


    @Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.prefs_headers, target);
	}

    private static void setLoginPreferenceListener(Preference preference, final Activity activity) {
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(activity, DirtyLoginActivity.class);
                activity.startActivity(intent);
                return true;
            }
        });
    }

	private static void setFavoritesPreferenceListener(Preference preference, final Activity activity) {
		preference
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					private DirtyFavoritesExporter favoritesExporter;

					@Override
					public boolean onPreferenceClick(Preference preference) {
						if (favoritesExporter == null) {
							favoritesExporter = new DirtyFavoritesExporter(activity);
						}
						favoritesExporter.startExport();
						return true;
					}
				});
	}

	private static void setSyncPreferenceListener(final Preference preference) {
		preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			private DirtyPreferences dirtyPreferences = new DirtyPreferences(
					PreferenceManager.getDefaultSharedPreferences(preference.getContext()), preference.getContext());
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue == Boolean.TRUE) {
					BackgroundPostLoaderReceiver.scheduleSync(preference.getContext(), 
							dirtyPreferences.getBackgroundSyncInterval());
				} else {
					BackgroundPostLoaderReceiver.unscheduleSync(preference.getContext());
				}
				return true;
			}
		});
	}
	
	private static void setSyncIntervalPreferenceListener(final Preference preference) {
		preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue != null && newValue instanceof String) {
					try {
						BackgroundPostLoaderReceiver.scheduleSync(preference.getContext(),
								Long.parseLong((String) newValue));
			        } catch (NumberFormatException ex) {
			        	SHLOG.w(ex);
			        	return false;
			        }
				}
				return true;
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }
}