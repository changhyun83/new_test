/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

// LGE_WCDMA_FEATURE_MERGE  START
import android.content.ActivityNotFoundException;
// LGE_WCDMA_FEATURE_MERGE  END
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
// LGE_WCDMA_FEATURE_MERGE  START
import android.preference.PreferenceScreen;
// LGE_WCDMA_FEATURE_MERGE  END
import android.util.Config;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoSettings extends PreferenceActivity {

    private static final String TAG = "DeviceInfoSettings";
    private static final boolean LOGD = false || Config.LOGD;

    private static final String KEY_CONTAINER = "container";
    private static final String KEY_TEAM = "team";
    private static final String KEY_CONTRIBUTORS = "contributors";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.device_info_settings);
       
        setStringSummary("firmware_version", Build.VERSION.RELEASE);
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_GENERAL_AT_COMMANDS START       
//        setValueSummary("baseband_manufacturer", "gsm.baseband.manufacturer");
//        setValueSummary("baseband_model", "gsm.baseband.model");
//        setValueSummary("baseband_imei", "gsm.baseband.imei");
// LGE_GENERAL_AT_COMMANDS END 

// 20100427 taesub.kim@lge.com Add fixed baseband version for temp [START_LGE]
// 	   setValueSummary("baseband_version", "gsm.version.baseband");
	   setStringSummary("baseband_version", "XMM6160");
// 20100427 taesub.kim@lge.com Add fixed baseband version for temp [END_LGE]


// LGE_WCDMA_FEATURE_MERGE  END
        setStringSummary("device_model", Build.MODEL);
        setStringSummary("build_number", Build.DISPLAY);
// LGE_WCDMA_FEATURE_MERGE  START
       
// LGE_WCDMA_FEATURE_MERGE  END
        findPreference("kernel_version").setSummary(getFormattedKernelVersion());
// LGE_WCDMA_FEATURE_MERGE  START
         // add by hyojinan@lge.com, 2009-03-01
// LGE_MERGE_ECLAIR_WARNING
        setStringSummary("soft_version", Build.VERSION.LGE);
// LGE_WCDMA_FEATURE_MERGE  END

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), "safetylegal",
                PROPERTY_URL_SAFETYLEGAL);

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
// LGE_WCDMA_FEATURE_MERGE  START
         
         // 20100426 hs.moon@lge.com for adding Divx Vod Registration menu [START}
         findPreference("divx_reg").setOnPreferenceClickListener (
         new Preference.OnPreferenceClickListener () {
             public boolean onPreferenceClick(Preference preference) {
                 try {
                     startActivity (preference.getIntent());
                 } catch ( ActivityNotFoundException e ) {
                     Log.e ( TAG , "Unable to launch activity Divx reg" );
                 }
                 return true;
             }
         } ); 
	// 20100426 hs.moon@lge.com for adding Divx Vod Registration menu [END]
         
// LGE_WCDMA_FEATURE_MERGE  END
        
        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = (PreferenceGroup) findPreference(KEY_CONTAINER);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TEAM,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        
        // These are contained by the root preference screen
        parentPreference = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference,
                KEY_SYSTEM_UPDATE_SETTINGS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_CONTRIBUTORS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals(""))
        {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }
    
    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, 
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {

        }
    }

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
// LGE_WCDMA_FEATURE_MERGE  START
//CHANGE_KERNEL_VERSION [taesub.kim@lge.com] 2010-04-22 START
/*                return (new StringBuilder(m.group(1)).append("\n").append(
// LGE_WCDMA_FEATURE_MERGE  END
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
// LGE_WCDMA_FEATURE_MERGE  START
*/
		return (new StringBuilder(m.group(1))).toString();
//CHANGE_KERNEL_VERSION [taesub.kim@lge.com] 2010-04-22 END
// LGE_WCDMA_FEATURE_MERGE  END
            }
        } catch (IOException e) {  
            Log.e(TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

}
