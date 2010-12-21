package com.android.settings.connectivity;

import android.os.Bundle;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.util.Log;

import com.android.settings.R;


public class ConnectivitySettings extends PreferenceActivity {
    private static String TAG = "ConnectivitySettings";
    private static String KEY_TOGGLE_HDMI = "toggle_hdmi";
    private static String KEY_RESOLUTION = "hdmi_resolution";
    private static String KEY_TOGGLE_USB = "toggle_usb";
    private static String KEY_USB_CONNECTION_MODE = "usb_connection_mode";
    private HDMIConnectivityPreference mResolutionPreference;
    private Preference mUSBAlwaysAskPreference;
    private USBConnectivityPreference mUSBconnectionPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.connectivity_settings);

        mResolutionPreference = (HDMIConnectivityPreference) findPreference(KEY_RESOLUTION);
        mResolutionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference,
                    Object objValue) {
                    HDMIUpdateSummary(objValue);

                    return false;
                }
            });

        mUSBAlwaysAskPreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_USB);

        mUSBconnectionPreference = (USBConnectivityPreference) findPreference(KEY_USB_CONNECTION_MODE);
        mUSBconnectionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference,
                    Object objValue) {
                    USBupdateSummary(objValue);

                    return false;
                }
            });
    }

    protected void HDMIUpdateSummary(Object value) {
        // TODO Auto-generated method stub
        CharSequence[] summaries = getResources()
                                       .getTextArray(R.array.hdmi_Resolution);
									   
		//LGE_CHANGE_S [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
        /*
        CharSequence[] values = mResolutionPreference.getEntryValues();
        for (int i=0; i<values.length; i++) {
        if (values[i].equals(value)) {
            mResolutionPreference.setSummary(summaries[i]);
        break;
        }
        }
        */
		//LGE_CHANGE_S [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
    }

    protected void USBupdateSummary(Object value) {
        // TODO Auto-generated method stub
        CharSequence[] summaries = getResources()
                                       .getTextArray(R.array.usb_connection_mode);
        CharSequence[] values = mUSBconnectionPreference.getEntryValues();

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                mUSBconnectionPreference.setSummary(summaries[i]);

                break;
            }
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        refresh();
    }

    private void refresh() {
        refreshUSBConnectionMode();
		//LGE_CHANGE [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
        //mResolutionPreference.setSummary(mResolutionPreference.getValue());
        mUSBconnectionPreference.setSummary(mUSBconnectionPreference.getValue());
    }

    private void refreshUSBConnectionMode() {
        Log.d(TAG, "##BS refreshUSBConnectionMode() ");
		//LGE_CHANGE [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
        //mUSBconnectionPreference.setSummary("USB connection mode value");
    }
}
