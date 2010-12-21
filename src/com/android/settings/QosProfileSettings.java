// LGE_WCDMA_FEATURE_MERGE  START
/*
 * Copyright (C) 2009 The Android Open Source Project
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
 
// LGE_MPDP START
package com.android.settings;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.Telephony;
import android.provider.Telephony.QoSProfileColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

public class QosProfileSettings extends PreferenceActivity {

    static final String TAG = "QosProfileSettings";
    public static final String RESTORE_QOSPROFILECOLUMNS_URI = 
	"content://telephony/qos_profiles/restore";
    private static final Uri mUri = Telephony.QoSProfileColumns.CONTENT_URI;
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;
    private static final int EVENT_RESTORE_DEFAULTQOS_START = 1;
    private static final int EVENT_RESTORE_DEFAULTQOS_COMPLETE = 2;
    private static final int DIALOG_RESTORE_DEFAULTQOS = 1001;
    private static final Uri DEFAULTQOS_URI = Uri.parse(RESTORE_QOSPROFILECOLUMNS_URI);
    private static boolean mRestoreDefaultQosMode;
    private RestoreQosUiHandler mRestoreQosUiHandler;
    private RestoreQosProcessHandler mRestoreQosProcessHandler;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.qos_settings);
        getListView().setItemsCanFocus(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mRestoreDefaultQosMode) {
            fillList();
        } else {
            showDialog(DIALOG_RESTORE_DEFAULTQOS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void fillList() {
        Cursor cursor = managedQuery(mUri,new String[] {
                "_id", "name"}, null, null,null);
        Log.d(TAG, "fillList():managedQuery()");
        PreferenceGroup qosList = (PreferenceGroup) findPreference("qos_list");
        qosList.removeAll();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(NAME_INDEX);
            String key = cursor.getString(ID_INDEX);
            QosPreference pref = new QosPreference(this);
            pref.setKey(key);
            pref.setTitle(name);
            pref.setPersistent(false);
            qosList.addPreference(pref);
            cursor.moveToNext();
        }
        cursor.close();
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new_qos_profile))
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewQosProfile();
            return true;
        case MENU_RESTORE:
            restoreDefaultQos();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewQosProfile() {
        startActivity(new Intent(Intent.ACTION_INSERT, Telephony.QoSProfileColumns.CONTENT_URI));
    }
    
    private boolean restoreDefaultQos() {
        showDialog(DIALOG_RESTORE_DEFAULTQOS);
        mRestoreDefaultQosMode = true;

        if (mRestoreQosUiHandler == null) {
            mRestoreQosUiHandler = new RestoreQosUiHandler();
        }

        if (mRestoreQosProcessHandler == null) {
            HandlerThread restoreDefaultQosThread = new HandlerThread(
                    "Restore default QoS Handler: Process Thread");
            restoreDefaultQosThread.start();
            mRestoreQosProcessHandler = new RestoreQosProcessHandler(
                    restoreDefaultQosThread.getLooper(), mRestoreQosUiHandler);
        }

        mRestoreQosProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTQOS_START);
        return true;
    }

    private class RestoreQosUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTQOS_COMPLETE:
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultQosMode = false;
                    dismissDialog(DIALOG_RESTORE_DEFAULTQOS);
                    Toast.makeText(
                        QosProfileSettings.this,
                        getResources().getString(
                                R.string.restore_default_qos_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreQosProcessHandler extends Handler {
        private Handler mRestoreQosUiHandler;

        public RestoreQosProcessHandler(Looper looper, Handler restoreQosUiHandler) {
            super(looper);
            this.mRestoreQosUiHandler = restoreQosUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTQOS_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(DEFAULTQOS_URI, null, null);                    
                    mRestoreQosUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTQOS_COMPLETE);
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTQOS) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.restore_default_qos));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_RESTORE_DEFAULTQOS) {
            getPreferenceScreen().setEnabled(false);
        }
    }
}
// LGE_MPDP END
// LGE_WCDMA_FEATURE_MERGE  END
