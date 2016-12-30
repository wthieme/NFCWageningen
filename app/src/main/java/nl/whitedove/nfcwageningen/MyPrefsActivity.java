package nl.whitedove.nfcwageningen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class MyPrefsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.instellingen);
		openPreference("Gebruikersnaam");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		// Registers a callback to be invoked whenever a user changes a
		// preference.
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();

		// Unregisters the listener set in onResume().
		// It's best practice to unregister listeners when your app isn't using
		// them to cut down on
		// unnecessary system overhead. You do this in onPause().
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	private PreferenceScreen findPreferenceScreenForPreference(String key,
			PreferenceScreen screen) {
		if (screen == null) {
			screen = getPreferenceScreen();
		}

		PreferenceScreen result;

		android.widget.Adapter ada = screen.getRootAdapter();
		for (int i = 0; i < ada.getCount(); i++) {
			String prefKey = ((Preference) ada.getItem(i)).getKey();
			if (prefKey != null && prefKey.equals(key)) {
				return screen;
			}
			if (ada.getItem(i).getClass()
					.equals(android.preference.PreferenceScreen.class)) {
				result = findPreferenceScreenForPreference(key,
						(PreferenceScreen) ada.getItem(i));
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private void openPreference(String key) {
		PreferenceScreen screen = findPreferenceScreenForPreference(key, null);
		if (screen != null) {
			screen.onItemClick(null, null, findPreference(key).getOrder(), 0);
		}
	}
}
