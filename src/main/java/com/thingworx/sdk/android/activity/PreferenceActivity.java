package com.thingworx.sdk.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

import com.thingworx.sdk.android.bottleflip.R;

public class PreferenceActivity extends android.preference.PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_preference);
        this.initSummaries(this.getPreferenceScreen());

        this.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        setTitle("ThingWorx Settings");

    }

    /**
     * Set the summaries of all preferences
     */
    private void initSummaries(PreferenceGroup pg) {
        for (int i = 0; i < pg.getPreferenceCount(); ++i) {
            Preference p = pg.getPreference(i);
            if (p instanceof PreferenceGroup)
                this.initSummaries((PreferenceGroup) p); // recursion
            else
                this.setSummary(p);
        }
    }

    /**
     * Set the summaries of the given preference
     */
    private void setSummary(Preference pref) {
        // react on type or key
        if (pref instanceof EditTextPreference) {
            EditTextPreference listPref = (EditTextPreference) pref;
            if(listPref!=null){
                if(listPref.getText()!=null) {
                    pref.setSummary(listPref.getText());
                } else {
                    if (listPref.getKey().equals("prefUri")) {
                        pref.setSummary("ex. wss://hostname/Thingworx/WS");
                }
                    if (listPref.getKey().equals("prefAppKey")) {
                        pref.setSummary("ex. e9274d87-58aa-4d60-b27f-e67962f3e5c4");
                    }
                }
            }
        }
    }

    /**
     * used to change the summary of a preference
     */
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        Preference pref = findPreference(key);
        this.setSummary(pref);
    }


}
