package jp.glassmoon.mbga.imascg.presentlister;

import java.util.ArrayList;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener {
	public static final String KEY_PREF_WAITTIME = "waittime";
	public static final String KEY_PREF_CLEAR_SUGGESTIONS = "clearsuggestions";
	public static final String KEY_PREF_DROPBOX ="dropbox";

	public static final String DROPBOX_TOKEN_KEY = "dropbox_key";
	public static final String DROPBOX_TOKEN_SECRET = "dropbox_secret";
	
	public static final String APP_KEY = "ur4tg0f3fomdl1j";
	public static final String APP_SECRET = "pi5f4rtkq6t2o9h";
	public static final AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	private boolean mDropboxAuthenticated;
	private DropboxAPI<AndroidAuthSession> mDBApi;

	private SharedPreferences.OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			// TODO Auto-generated method stub
			if (KEY_PREF_WAITTIME.equals(key)) {
				ListPreference listPref = (ListPreference) findPreference(key);
				listPref.setSummary(
						String.format(
								getString(R.string.pref_waittime_summary),
								sharedPreferences.getString(
										key,
										getString(R.string.pref_waittime_default))));
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		ArrayList<String> entries = new ArrayList<String>(); 
		for (int i=5; i<=30; i++ ) {
			entries.add(String.valueOf(i));
		}
		for (int i=35; i<=120; i+=5) {
			entries.add(String.valueOf(i));
		}
		
		String[] strEntries = entries.toArray(new String[0]);
		
		ListPreference listPref = (ListPreference) findPreference(KEY_PREF_WAITTIME);
		listPref.setEntries(strEntries);
		listPref.setEntryValues(strEntries);
		listPref.setDefaultValue("5");
		listPref.setSummary(String.format(
				getString(R.string.pref_waittime_summary),
				listPref.getValue()));
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		mDropboxAuthenticated = sp.getString(DROPBOX_TOKEN_KEY, null) == null ? false : true;
		
		Preference pref = (Preference) findPreference(KEY_PREF_DROPBOX);
		if (mDropboxAuthenticated) {
			pref.setSummary(R.string.pref_dropbox_enabled_summary);
		}
		else {
			pref.setSummary(R.string.pref_dropbox_disabled_summary);
		}
		pref.setOnPreferenceClickListener(this);
		
        // Dropbox
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getPreferenceScreen()
		.getSharedPreferences()
		.registerOnSharedPreferenceChangeListener(listener);
		
		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
			    // MANDATORY call to complete auth.
	            // Sets the access token on the session
	            mDBApi.getSession().finishAuthentication();

	            AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();

	            // Provide your own storeKeys to persist the access token pair
	            // A typical way to store tokens is using SharedPreferences
	            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
	            editor.putString(SettingsActivity.DROPBOX_TOKEN_KEY, tokens.key);
	            editor.putString(SettingsActivity.DROPBOX_TOKEN_SECRET, tokens.secret);
	            editor.commit();
	            
	            Preference pref = (Preference) findPreference(KEY_PREF_DROPBOX);
	            pref.setSummary(R.string.pref_dropbox_enabled_summary);
	            mDropboxAuthenticated = true;
			} catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		getPreferenceScreen()
		.getSharedPreferences()
		.unregisterOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		if (KEY_PREF_DROPBOX.equals(preference.getKey())) {
			if (mDropboxAuthenticated) {
				// Authenticated -> clear token
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
				editor.remove(DROPBOX_TOKEN_KEY);
				editor.remove(DROPBOX_TOKEN_SECRET);
				editor.commit();
				preference.setSummary(R.string.pref_dropbox_disabled_summary);
				mDropboxAuthenticated = false;
			}
			else {
				// Unauthenticated -> go authentication
				mDBApi.getSession().startAuthentication(this);
			}
		}
		
		return false;
	}

}
