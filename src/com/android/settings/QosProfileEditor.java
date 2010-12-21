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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Telephony.QoSProfileColumns;
import android.provider.Telephony;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.telephony.gsm.QoSProfile;
import android.telephony.gsm.QoSProfile.DeliveryOrder;
import android.telephony.gsm.QoSProfile.ErrSDUDElivery;
import android.telephony.gsm.QoSProfile.InvalidProfileDataException;
import android.telephony.gsm.QoSProfile.SignallingIndication;
import android.telephony.gsm.QoSProfile.SourceStatisticsDescriptor;
import android.telephony.gsm.QoSProfile.TrafficClass;

import java.util.HashMap;
import java.util.Map;

public class QosProfileEditor extends PreferenceActivity {

    private final static String TAG = QosProfileEditor.class.getSimpleName(); 
    private final static String SAVED_POS = "pos";
    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;
    private static String sNotSet;
    private static final String[] sProjection;
    private static Map<String, Integer> sIndexMap;

    private static enum ParamState { NOT_SET, SET, DISABLED }
    private static ParamState[] sPStates;
    
    static {
        sProjection = new String[] {
            QoSProfileColumns._ID,
            QoSProfileColumns.NAME,
            QoSProfileColumns.TRAFFIC_CLASS,
            QoSProfileColumns.MAXIMUM_BITRATE_UL,
            QoSProfileColumns.MAXIMUM_BITRATE_DL,
            QoSProfileColumns.GUARANTEED_BITRATE_UL,
            QoSProfileColumns.GUARANTEED_BITRATE_DL,
            QoSProfileColumns.DELIVERY_ORDER,
            QoSProfileColumns.MAXIMUM_SDU_SIZE,
            QoSProfileColumns.SDU_ERROR_RATIO,
            QoSProfileColumns.RESIDUAL_BIT_ERROR_RATIO,
            QoSProfileColumns.DELIVERY_OF_ERRONEOUS_SDUS,
            QoSProfileColumns.TRANSFER_DELAY,
            QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY,
            QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR,
            QoSProfileColumns.SIGNALLING_INDICATION
        };

        sIndexMap = new HashMap<String, Integer>();
        for(int i=0; i<sProjection.length; ++i) {
            sIndexMap.put(sProjection[i], i);
        }

        sPStates = new ParamState[sProjection.length];
    }

    public QoSProfile mProfile;
    private EditTextPreference mName;
    private EditTextPreference mTrafficClass;
    private EditTextPreference mMaxBitrateUL;
    private EditTextPreference mMaxBitrateDL;
    private EditTextPreference mGuaranteedBitrateUL;
    private EditTextPreference mGuaranteedBitrateDL;
    private EditTextPreference mDeliveryOrder;
    private EditTextPreference mMaxSDUSize;
    private EditTextPreference mSduErrorRatio;
    private EditTextPreference mResidualBitErrorRatio;
    private EditTextPreference mDeliveryOfErroneousSDUs;
    private EditTextPreference mTransferDelay;
    private EditTextPreference mTrafficHandlingPriority;
    private EditTextPreference mSourceStatisticsDescriptor;
    private EditTextPreference mSignallingIndication;

    private EditTextPreference[] mPrefArray;
    
    private Uri mUri;
    private Cursor mCursor;
    private boolean mNewProfile;
    private boolean mFirstTime;
    private Resources mRes;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.qos_profile_settings);
        sNotSet = getResources().getString(R.string.qos_not_set);

        Log.d(TAG, "onCreate(): icicle is " + (icicle==null?"null":"not null"));

        mName = (EditTextPreference) 
            findPreference(QoSProfileColumns.NAME);
        mTrafficClass = (EditTextPreference) 
            findPreference(QoSProfileColumns.TRAFFIC_CLASS);
        mMaxBitrateUL = (EditTextPreference) 
            findPreference(QoSProfileColumns.MAXIMUM_BITRATE_UL);
        mMaxBitrateDL = (EditTextPreference) 
            findPreference(QoSProfileColumns.MAXIMUM_BITRATE_DL);
        mGuaranteedBitrateUL = (EditTextPreference) 
            findPreference(QoSProfileColumns.GUARANTEED_BITRATE_UL);
        mGuaranteedBitrateDL = (EditTextPreference) 
            findPreference(QoSProfileColumns.GUARANTEED_BITRATE_DL);
        mDeliveryOrder = (EditTextPreference) 
            findPreference(QoSProfileColumns.DELIVERY_ORDER);
        mMaxSDUSize = (EditTextPreference) 
            findPreference(QoSProfileColumns.MAXIMUM_SDU_SIZE);
        mSduErrorRatio = (EditTextPreference) 
            findPreference(QoSProfileColumns.SDU_ERROR_RATIO);
        mResidualBitErrorRatio = (EditTextPreference) 
            findPreference(QoSProfileColumns.RESIDUAL_BIT_ERROR_RATIO);
        mDeliveryOfErroneousSDUs = (EditTextPreference) 
            findPreference(QoSProfileColumns.DELIVERY_OF_ERRONEOUS_SDUS);
        mTransferDelay = (EditTextPreference) 
            findPreference(QoSProfileColumns.TRANSFER_DELAY);
        mTrafficHandlingPriority = (EditTextPreference) 
            findPreference(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY);
        mSourceStatisticsDescriptor = (EditTextPreference) 
            findPreference(QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR);
        mSignallingIndication = (EditTextPreference) 
            findPreference(QoSProfileColumns.SIGNALLING_INDICATION);

        mPrefArray = new EditTextPreference[sPStates.length];
        mPrefArray[indexOf(QoSProfileColumns.NAME)] = mName;
        mPrefArray[indexOf(QoSProfileColumns.TRAFFIC_CLASS)] = mTrafficClass;
        mPrefArray[indexOf(QoSProfileColumns.MAXIMUM_BITRATE_UL)] = mMaxBitrateUL;
        mPrefArray[indexOf(QoSProfileColumns.MAXIMUM_BITRATE_DL)] = mMaxBitrateDL;
        mPrefArray[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_UL)] = mGuaranteedBitrateUL;
        mPrefArray[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_DL)] = mGuaranteedBitrateDL;
        mPrefArray[indexOf(QoSProfileColumns.DELIVERY_ORDER)] = mDeliveryOrder;
        mPrefArray[indexOf(QoSProfileColumns.MAXIMUM_SDU_SIZE)] = mMaxSDUSize;
        mPrefArray[indexOf(QoSProfileColumns.SDU_ERROR_RATIO)] = mSduErrorRatio;
        mPrefArray[indexOf(QoSProfileColumns.RESIDUAL_BIT_ERROR_RATIO)] = mResidualBitErrorRatio;
        mPrefArray[indexOf(QoSProfileColumns.DELIVERY_OF_ERRONEOUS_SDUS)] = mDeliveryOfErroneousSDUs;
        mPrefArray[indexOf(QoSProfileColumns.TRANSFER_DELAY)] = mTransferDelay;
        mPrefArray[indexOf(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY)] = mTrafficHandlingPriority;
        mPrefArray[indexOf(QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR)] = mSourceStatisticsDescriptor;
        mPrefArray[indexOf(QoSProfileColumns.SIGNALLING_INDICATION)] = mSignallingIndication;
        mRes = getResources();
        setOnChangeListeners();

        final Intent intent = getIntent();
        final String action = intent.getAction();      
        mFirstTime = icicle == null;
        if (action.equals(Intent.ACTION_EDIT)) {
            mUri = intent.getData();
        } else if (action.equals(Intent.ACTION_INSERT)) {
            if (mFirstTime || icicle.getInt(SAVED_POS) == 0) {
                Uri uri = intent.getData();
                mUri = getContentResolver().insert(intent.getData(), new ContentValues());
            } else {
                mUri = ContentUris.withAppendedId(Telephony.QoSProfileColumns.CONTENT_URI, 
                        icicle.getInt(SAVED_POS));
            }
            mNewProfile = true;
            // If we were unable to create a new profile, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if(mUri == null) {
                Log.w(TAG, "Failed to insert new QoS profile into "
                        + getIntent().getData());
                finish();
                return;
            }
            
            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            finish();
            return;
        }

        mCursor = managedQuery(mUri,sProjection, null, null,null);
        if(mCursor.getCount() < 1) {
            Log.e(TAG, "query returned zero records. exiting...");
            finish();
            return;
        }

        mCursor.moveToFirst();
        fillUi();
    }

    private void fillUi() {
        QoSProfile p = null;
        if (mFirstTime) {
            mFirstTime = false;
            // Fill in all the values from the db in both text editor and summary

            try {
                mProfile = QoSProfile.createFromDB(mCursor);
            } catch (InvalidProfileDataException ex) {
                showErrorMessage(mRes.getString(R.string.qos_err_invalid_db_data));
                Log.e(TAG, "unable to create QoSProfile from DB: " + ex);
                mProfile = new QoSProfile();
            }

            p = mProfile;
            mName.setText("" + p.getName());
            mTrafficClass.setText("" + p.trafficClass().id());
            mMaxBitrateUL.setText("" + p.maxBitrateUL());
            mMaxBitrateDL.setText("" + p.maxBitrateDL());
            mGuaranteedBitrateUL.setText("" + p.guaranteedBitrateUL());
            mGuaranteedBitrateDL.setText("" + p.guaranteedBitrateDL());
            mDeliveryOrder.setText("" + p.deliveryOrder().id());
            mMaxSDUSize.setText("" + p.maxSDUSize());
            mSduErrorRatio.setText(p.sduErrorRatio());
            mResidualBitErrorRatio.setText(p.residualBitErrorRatio());
            mDeliveryOfErroneousSDUs.setText("" + p.deliveryOfErroneousSDUs().id());
            mTransferDelay.setText("" + p.transferDelay());
            mTrafficHandlingPriority.setText("" + p.trafficHandlingPriority());
            mSourceStatisticsDescriptor.setText("" + p.sourceStatisticsDescriptor().id());
            mSignallingIndication.setText("" + p.signallingIndication().id());

            dimUnusedFields();
        }

        p = mProfile;

        Log.v(TAG, "QoSProfile = { " + p + " }");
        
        mName.setSummary(checkNull(mName.getText()));
        mTrafficClass.setSummary(checkNull(p.trafficClass().toString()));
        mMaxBitrateUL.setSummary(checkNull(mMaxBitrateUL.getText()));
        mMaxBitrateDL.setSummary(checkNull(mMaxBitrateDL.getText()));
        mGuaranteedBitrateUL.setSummary(checkNull(mGuaranteedBitrateUL.getText()));
        mGuaranteedBitrateDL.setSummary(checkNull(mGuaranteedBitrateDL.getText()));
        mDeliveryOrder.setSummary(checkNull(p.deliveryOrder().toString()));
        mMaxSDUSize.setSummary(checkNull(mMaxSDUSize.getText()));
        mSduErrorRatio.setSummary(checkNull(mSduErrorRatio.getText()));
        mResidualBitErrorRatio.setSummary(checkNull(mResidualBitErrorRatio.getText()));
        mDeliveryOfErroneousSDUs.setSummary(checkNull(p.deliveryOfErroneousSDUs().toString()));
        mTransferDelay.setSummary(checkNull(mTransferDelay.getText()));
        mTrafficHandlingPriority.setSummary(checkNull(mTrafficHandlingPriority.getText()));
        mSourceStatisticsDescriptor.setSummary(checkNull(p.sourceStatisticsDescriptor().toString()));
        mSignallingIndication.setSummary(checkNull(p.signallingIndication().toString()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu); // TODO: we'll need to handle this for full QoS         
        if (!mNewProfile) {
            menu.add(0, MENU_DELETE, 0, R.string.delete_qos_profile)
                .setIcon(android.R.drawable.ic_menu_delete);
        }
        menu.add(0, MENU_SAVE, 0, R.string.menu_save)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
        
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DELETE:
             deleteQosProfile();
            return true;
        case MENU_SAVE:
            if (validateAndSave(false)) {
                finish();
            }
            return true;
        case MENU_CANCEL:
            if (mNewProfile) {
                getContentResolver().delete(mUri, null, null);
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

  private void deleteQosProfile() {
        getContentResolver().delete(mUri, null, null);
        finish();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (validateAndSave(false)) {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        validateAndSave(true);
        icicle.putInt(SAVED_POS, mCursor.getInt(indexOf(QoSProfileColumns._ID)));
    }
    
    /**
     * Check the key fields' validity and save if valid.
     * @return true if the data was saved
     */
    private boolean validateAndSave(boolean force) {
        String name = checkNotSet(mName.getText());
        String errorMsg = null;

        if (name.length() < 1) {
            errorMsg = mRes.getString(R.string.error_name_empty);
        }
        
        if (errorMsg != null && !force) {
            showErrorMessage(errorMsg);
            return false;
        }

        if (!mCursor.moveToFirst()) {
            Log.w(TAG,
                    "Could not go to the first row in the Cursor when saving data.");
            return false;
        }
        
        ContentValues values = new ContentValues();
        QoSProfile p = mProfile;

        Log.d(TAG, "trying to save profile: { "+ p +" }");
        
        values.put(QoSProfileColumns.NAME, p.getName());
        values.put(QoSProfileColumns.TRAFFIC_CLASS, p.trafficClass().id());
        values.put(QoSProfileColumns.MAXIMUM_BITRATE_UL, p.maxBitrateUL());
        values.put(QoSProfileColumns.MAXIMUM_BITRATE_DL, p.maxBitrateDL());
        values.put(QoSProfileColumns.GUARANTEED_BITRATE_UL, p.guaranteedBitrateUL());
        values.put(QoSProfileColumns.GUARANTEED_BITRATE_DL, p.guaranteedBitrateDL());
        values.put(QoSProfileColumns.DELIVERY_ORDER, p.deliveryOrder().id());
        values.put(QoSProfileColumns.MAXIMUM_SDU_SIZE, p.maxSDUSize());
        values.put(QoSProfileColumns.SDU_ERROR_RATIO, p.sduErrorRatio());
        values.put(QoSProfileColumns.RESIDUAL_BIT_ERROR_RATIO, p.residualBitErrorRatio());
        values.put(QoSProfileColumns.DELIVERY_OF_ERRONEOUS_SDUS, p.deliveryOfErroneousSDUs().id());
        values.put(QoSProfileColumns.TRANSFER_DELAY, p.transferDelay());
        values.put(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY, p.trafficHandlingPriority());
        values.put(QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR, p.sourceStatisticsDescriptor().id());
        values.put(QoSProfileColumns.SIGNALLING_INDICATION, p.signallingIndication().id());

        getContentResolver().update(mUri, values, null, null);
        return true;
    }

    private void showErrorMessage(String message) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
    
    private String checkNull(String value) {
        if (value == null || 
            value.length() == 0 || 
            value.equals("-1") || 
            value.equals("NOT_SET")) {

            return sNotSet;
        } else {
            return value;
        }
    }
    
    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    private int indexOf(String field) {
        return sIndexMap.get(field).intValue();
    }

    private String makeInvalidDataMsg(int resId) {
        return mRes.getString(R.string.qos_err_invalid_data) + ": " +
            mRes.getString(resId);
    }

    private void setOnChangeListeners() {
        mName.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.setName((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.qosp_name_title));
                    return false;
                }              
                mName.setSummary(checkNull(p.getName()));
                dimUnusedFields();
                return true;
            }
        });
        mTrafficClass.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.trafficClass((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.traffic_class_title));
                    return false;
                }
                mTrafficClass.setSummary(checkNull(p.trafficClass().toString()));
                dimUnusedFields();
                return true;
            }
        });
        mMaxBitrateUL.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.maxBitrateUL((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.maximum_bitrate_ul_title));
                    return false;
                }
                mMaxBitrateUL.setSummary(checkNull(String.valueOf(p.maxBitrateUL())));
                dimUnusedFields();
                return true;
            }
        });
        mMaxBitrateDL.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.maxBitrateDL((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.maximum_bitrate_dl_title));
                    return false;
                }
                mMaxBitrateDL.setSummary(checkNull(String.valueOf(p.maxBitrateDL())));
                dimUnusedFields();
                return true;
            }
        });
        mGuaranteedBitrateUL.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.guaranteedBitrateUL((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.guaranteed_bitrate_ul_title));
                    return false;
                }
                mGuaranteedBitrateUL.setSummary(checkNull(String.valueOf(p.guaranteedBitrateUL())));
                dimUnusedFields();
                return true;
            }
        });
        mGuaranteedBitrateDL.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.guaranteedBitrateDL((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.guaranteed_bitrate_dl_title));
                    return false;
                }
                mGuaranteedBitrateDL.setSummary(checkNull(String.valueOf(p.guaranteedBitrateDL())));
                dimUnusedFields();
                return true;
            }
        });
        mDeliveryOrder.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.deliveryOrder((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.delivery_order_title));
                    return false;
                }
                mDeliveryOrder.setSummary(checkNull(p.deliveryOrder().toString()));
                dimUnusedFields();
                return true;
            }
        });
        mMaxSDUSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.maxSDUSize((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.maximum_sdu_size_title));
                    return false;
                }
                mMaxSDUSize.setSummary(checkNull(String.valueOf(p.maxSDUSize())));
                dimUnusedFields();
                return true;
            }
        });
        mSduErrorRatio.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.sduErrorRatio((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.sdu_error_ratio_title));
                    return false;
                }
                mSduErrorRatio.setSummary(checkNull(p.sduErrorRatio()));
                dimUnusedFields();
                return true;
            }
        });
        mResidualBitErrorRatio.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.residualBitErrorRatio((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.residual_bit_error_ratio_title));
                    return false;
                }
                mResidualBitErrorRatio.setSummary(checkNull(p.residualBitErrorRatio()));
                dimUnusedFields();
                return true;
            }
        });
        mDeliveryOfErroneousSDUs.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.deliveryOfErroneousSDUs((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.delivery_of_erroneous_sdus_title));
                    return false;
                }
                mDeliveryOfErroneousSDUs.setSummary(checkNull(p.deliveryOfErroneousSDUs().toString()));
                dimUnusedFields();
                return true;
            }
        });
        mTransferDelay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.transferDelay((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.transfer_delay_title));
                    return false;
                }
                mTransferDelay.setSummary(checkNull(String.valueOf(p.transferDelay())));
                dimUnusedFields();
                return true;
            }
        });
        mTrafficHandlingPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.trafficHandlingPriority((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.traffic_handling_priority_title));
                    return false;
                }
                mTrafficHandlingPriority.setSummary(checkNull(String.valueOf(p.trafficHandlingPriority())));
                dimUnusedFields();
                return true;
            }
        });
        mSourceStatisticsDescriptor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.sourceStatisticsDescriptor((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.source_statistics_descriptor_title));
                    return false;
                }
                mSourceStatisticsDescriptor.setSummary(checkNull(p.sourceStatisticsDescriptor().toString()));
                dimUnusedFields();
                return true;
            }
        });
        mSignallingIndication.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                QoSProfile p = mProfile;
                try {
                    p.signallingIndication((String) newValue);
                } catch (InvalidProfileDataException exc) {
                    showErrorMessage(makeInvalidDataMsg(R.string.signalling_indication_title));
                    return false;
                }
                mSignallingIndication.setSummary(checkNull(p.signallingIndication().toString()));
                dimUnusedFields();
                return true;
            }
        });
    }

    private void dimUnusedFields() {
        QoSProfile p = mProfile;

        sPStates[indexOf(QoSProfileColumns.TRAFFIC_CLASS)] = 
            p.trafficClass() == TrafficClass.NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.MAXIMUM_BITRATE_UL)] = 
            p.maxBitrateUL() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.MAXIMUM_BITRATE_DL)] = 
            p.maxBitrateDL() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_UL)] = 
            p.guaranteedBitrateUL() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_DL)] = 
            p.guaranteedBitrateDL() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.DELIVERY_ORDER)] = 
            p.deliveryOrder() == DeliveryOrder.NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.MAXIMUM_SDU_SIZE)] = 
            p.maxSDUSize() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.SDU_ERROR_RATIO)] = 
            p.sduErrorRatio().equals(QoSProfile.STRING_NOT_SET) ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.RESIDUAL_BIT_ERROR_RATIO)] = 
            p.residualBitErrorRatio().equals(QoSProfile.STRING_NOT_SET) ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.DELIVERY_OF_ERRONEOUS_SDUS)] = 
            p.deliveryOfErroneousSDUs() == ErrSDUDElivery.NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.TRANSFER_DELAY)] = 
            p.transferDelay() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY)] = 
            p.trafficHandlingPriority() == QoSProfile.INT_NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR)] = 
            p.sourceStatisticsDescriptor() == SourceStatisticsDescriptor.NOT_SET ? ParamState.NOT_SET : ParamState.SET;
        sPStates[indexOf(QoSProfileColumns.SIGNALLING_INDICATION)] = 
            p.signallingIndication() == SignallingIndication.NOT_SET ? ParamState.NOT_SET : ParamState.SET;

        // disable parameters according to 3GPP 23.107
        switch(p.trafficClass()) {
            case CONVERSATIONAL:
            case STREAMING:
                sPStates[indexOf(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY)] = ParamState.DISABLED;
                sPStates[indexOf(QoSProfileColumns.SIGNALLING_INDICATION)] = ParamState.DISABLED;
                break;
            case BACKGROUND:
                sPStates[indexOf(QoSProfileColumns.TRAFFIC_HANDLING_PRIORITY)] = ParamState.DISABLED;
                sPStates[indexOf(QoSProfileColumns.SIGNALLING_INDICATION)] = ParamState.DISABLED;
                //no break
            case INTERACTIVE:
                sPStates[indexOf(QoSProfileColumns.TRANSFER_DELAY)] = ParamState.DISABLED;
                sPStates[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_UL)] = ParamState.DISABLED;
                sPStates[indexOf(QoSProfileColumns.GUARANTEED_BITRATE_DL)] = ParamState.DISABLED;
                sPStates[indexOf(QoSProfileColumns.SOURCE_STATISTICS_DESCRIPTOR)] = ParamState.DISABLED;
                break;
            default:
                break;
        }

        boolean isEnabled = true;
        for(int i=indexOf(QoSProfileColumns.TRAFFIC_CLASS); i<sPStates.length; ++i) {
            mPrefArray[i].setEnabled(isEnabled && sPStates[i] != ParamState.DISABLED);
            if(sPStates[i] == ParamState.NOT_SET) {
                isEnabled = false;
            }
        }

    }
}
// LGE_MPDP END
// LGE_WCDMA_FEATURE_MERGE  END
