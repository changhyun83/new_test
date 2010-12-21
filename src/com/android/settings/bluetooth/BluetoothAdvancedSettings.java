package com.android.settings.bluetooth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.provider.Settings.System;
import com.android.settings.R;

public class BluetoothAdvancedSettings extends PreferenceActivity {
	public static final String TAG = "BluetoothAdvancedSettings";
	public static final String PREF_KEY_FTP = "bt_ftp_svc";
	public static final String PREF_KEY_PBAP = "bt_pbap_svc";
	public static final String PREF_KEY_OPP = "bt_opp_svc";
	public static final String PREF_KEY_BIP = "bt_bip_svc";
	public static final String PREF_KEY_HID = "bt_hid_svc";
	public static final String PREF_KEY_PAN = "bt_pan_svc";

	public static final String SYSTEM_SETTINGS_FTP_STATE = "bluetooth_ftp_state";
	public static final String SYSTEM_SETTINGS_PBAP_STATE = "bluetooth_pbap_state";
	public static final String SYSTEM_SETTINGS_OPP_STATE = "bluetooth_opp_state";
	public static final String SYSTEM_SETTINGS_BIP_STATE = "bluetooth_bip_state";
	public static final String SYSTEM_SETTINGS_HID_STATE = "bluetooth_hid_state";
	public static final String SYSTEM_SETTINGS_PAN_STATE = "bluetooth_pan_state";

	private static final String FTP_ACTION_ENABLED_CHANGED = "android.bluetooth.ftp.action.ENABLED_CHANGED";
    private static final String PBAP_ACTION_ENABLED_CHANGED = "android.bluetooth.pbap.action.ENABLED_CHANGED";
    private static final String OPP_ACTION_ENABLED_CHANGED = "android.bluetooth.opp.action.ENABLED_CHANGED";
    private static final String BIP_ACTION_ENABLED_CHANGED = "android.bluetooth.bip.action.ENABLED_CHANGED";
    private static final String HID_ACTION_ENABLED_CHANGED = "android.bluetooth.hid.action.ENABLED_CHANGED";
    private static final String PAN_ACTION_ENABLED_CHANGED = "android.bluetooth.pan.action.ENABLED_CHANGED";

    private static final String EXTRA_STATE = "android.bluetooth.service.extra.STATE";
    
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.bluetooth_advanced_settings);

		((CheckBoxPreference)findPreference(PREF_KEY_FTP))
				.setChecked(getSetting(SYSTEM_SETTINGS_FTP_STATE));

		((CheckBoxPreference)findPreference(PREF_KEY_PBAP))
				.setChecked(getSetting(SYSTEM_SETTINGS_PBAP_STATE));

		((CheckBoxPreference)findPreference(PREF_KEY_OPP))
				.setChecked(getSetting(SYSTEM_SETTINGS_OPP_STATE));			

		((CheckBoxPreference)findPreference(PREF_KEY_BIP))
				.setChecked(getSetting(SYSTEM_SETTINGS_BIP_STATE));

		((CheckBoxPreference)findPreference(PREF_KEY_HID))
				.setChecked(getSetting(SYSTEM_SETTINGS_HID_STATE));

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    										   Preference preference) {

        String key = preference.getKey();
        boolean isEnabled = ((CheckBoxPreference)preference).isChecked();

    	if (PREF_KEY_FTP.equals(key)) {
    		Log.v(TAG,"FTP Server Toggled to " + isEnabled);
    		setSetting(SYSTEM_SETTINGS_FTP_STATE, isEnabled);
    		broadcastServiceToggled(FTP_ACTION_ENABLED_CHANGED,isEnabled);
    		return true;
        } else if (PREF_KEY_PBAP.equals(key)) {
	        Log.v(TAG,"PBAP Server Toggled to " + ((CheckBoxPreference)preference).isChecked());
	        setSetting(SYSTEM_SETTINGS_PBAP_STATE,isEnabled);
	        broadcastServiceToggled(PBAP_ACTION_ENABLED_CHANGED,isEnabled);
        	return true;
        } else if (PREF_KEY_OPP.equals(key)) {
	        Log.v(TAG,"OPP Server Toggled to " + ((CheckBoxPreference)preference).isChecked());
	        setSetting(SYSTEM_SETTINGS_OPP_STATE,isEnabled);
	        broadcastServiceToggled(OPP_ACTION_ENABLED_CHANGED,isEnabled);
        	return true;
        } else if (PREF_KEY_BIP.equals(key)) {
	        Log.v(TAG,"BIP Server Toggled to " + ((CheckBoxPreference)preference).isChecked());
	        setSetting(SYSTEM_SETTINGS_BIP_STATE,isEnabled);
	        broadcastServiceToggled(BIP_ACTION_ENABLED_CHANGED,isEnabled);
        	return true;	
    	} else if (PREF_KEY_HID.equals(key)) {
	        Log.v(TAG,"HID Server Toggled to " + ((CheckBoxPreference)preference).isChecked());
	        setSetting(SYSTEM_SETTINGS_HID_STATE,isEnabled);
	        broadcastServiceToggled(HID_ACTION_ENABLED_CHANGED,isEnabled);
        	return true;
        }
        	
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

    private void broadcastServiceToggled(String action, boolean isEnabled) {
        int state = isEnabled ? STATE_ENABLED : STATE_DISABLED;
		Intent intent = new Intent(action);
		intent.putExtra(EXTRA_STATE, state);
		sendBroadcast(intent);
    }


	// the default setting is enabled for all services
    private boolean getSetting(String systemSettingsKey) {
    	return getSetting(systemSettingsKey, STATE_ENABLED);
    }

    private boolean getSetting(String systemSettingsKey, int defaultValue) {
    	return Settings.System.getInt(getContentResolver(), systemSettingsKey, defaultValue) == STATE_ENABLED;
    }
    
    private void setSetting(String systemSettingsKey, boolean isEnabled) {
    	int state = isEnabled ? STATE_ENABLED : STATE_DISABLED;
    	if (!Settings.System.putInt(getContentResolver(), systemSettingsKey, state))
    		Log.w(TAG,"could not set setting " + systemSettingsKey + " to " + state);
    }

    
}
