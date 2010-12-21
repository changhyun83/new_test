package com.android.settings.connectivity;

import android.app.AlertDialog.Builder;

import android.content.Context;
import android.content.DialogInterface;

import android.content.res.TypedArray;

import android.os.Parcel;
import android.os.Parcelable;

import android.preference.DialogPreference;

import android.util.AttributeSet;
import android.util.Log;


//import android.lge.lghdmi.*;
public class HDMIConnectivityPreference extends DialogPreference {
    private final static String TAG = "HDMIConnectivityPreference";
    private static final int _AUTO = 0;
    private static final int _480I = 1;
    private static final int _480P = 2;
    private static final int _720P = 3;
    private static final int _1080I = 4;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private String mValue;
    private int mClickedDialogEntryIndex;
	//LGE_CHANGE_S [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
    //    private static final int _1080P = 5;
    //    LgHdmi hdmiConnector;
	//LGE_CHANGE_E [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
    public HDMIConnectivityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.ListPreference, 0, 0);
        mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
        mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
        a.recycle();
    }
	//LGE_CHANGE_S [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
    /*
    public HDMIConnectivityPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            hdmiConnector = new LgHdmi(context);
    
            TypedArray a = context.obtainStyledAttributes(attrs,
                    com.android.internal.R.styleable.ListPreference, 0, 0);
            mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
            mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
            a.recycle();
    
                    Log.d(TAG, "[HDMIConnectivityPreference] hdmiConnector.HdmiCheckCurRes() : "+ hdmiConnector.HdmiCheckCurRes());
            if(getValue() == null){
                    switch (hdmiConnector.HdmiCheckCurRes()) {
                            case 0:
                                    setValue(mEntryValues[_1080I].toString());
                                    break;
                            case 1:
                                    setValue(mEntryValues[_720P].toString());
                                    break;
                            case 2:
                                    setValue(mEntryValues[_480P].toString());
                                    break;
                            case 3:
                                    setValue(mEntryValues[_480I].toString());
                                    break;
                            case 4:
                                    setValue(mEntryValues[_AUTO].toString());
                                    break;
                            default:
                                    setValue(mEntryValues[_AUTO].toString());
                            }
            }
        }
    
        public HDMIConnectivityPreference(Context context) {
            this(context, null);
        }
    
    
        public void setEntries(CharSequence[] entries) {
            mEntries = entries;
        }
    
    
        public void setEntries(int entriesResId) {
            setEntries(getContext().getResources().getTextArray(entriesResId));
        }
    
        public CharSequence[] getEntries() {
            return mEntries;
        }
    
    
        public void setEntryValues(CharSequence[] entryValues) {
            mEntryValues = entryValues;
        }
    
    
        public void setEntryValues(int entryValuesResId) {
            setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
        }
    
    
        public CharSequence[] getEntryValues() {
            return mEntryValues;
        }
    
    
        public void setValue(String value) {
            mValue = value;
    
            persistString(value);
        }
    
    
        public void setValueIndex(int index) {
            if (mEntryValues != null) {
                setValue(mEntryValues[index].toString());
            }
        }
    
    
        public String getValue() {
            return mValue;
        }
    
    
        public CharSequence getEntry() {
            int index = getValueIndex();
            return index >= 0 && mEntries != null ? mEntries[index] : null;
        }
    
    
        public int findIndexOfValue(String value) {
            if (value != null && mEntryValues != null) {
                for (int i = mEntryValues.length - 1; i >= 0; i--) {
                    if (mEntryValues[i].equals(value)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    
        private int getValueIndex() {
            return findIndexOfValue(mValue);
        }
    
        @Override
        protected void onPrepareDialogBuilder(Builder builder) {
            super.onPrepareDialogBuilder(builder);
    
            if (mEntries == null || mEntryValues == null) {
                throw new IllegalStateException(
                        "ConnectivityPreference requires an entries array and an entryValues array.");
            }
    
            mClickedDialogEntryIndex = getValueIndex();
            builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mClickedDialogEntryIndex = which;
                                        switch(which){
                                    case 0:
                                            hdmiConnector.HdmiChangeAVFormat(_1080I);
                                            Log.d(TAG, "[HDMIConnectivityPreference_onPrepareDialogBuilder] hdmiConnector.HdmiCheckCurRes() : "
                                                            + hdmiConnector.HdmiCheckCurRes());
                                            break;
                                    case 1:
                                            hdmiConnector.HdmiChangeAVFormat(_720P);
                                            Log.d(TAG, "[HDMIConnectivityPreference_onPrepareDialogBuilder] hdmiConnector.HdmiCheckCurRes() : "
                                                            + hdmiConnector.HdmiCheckCurRes());
                                            break;
                                    case 2:
                                            hdmiConnector.HdmiChangeAVFormat(_480P);
                                            Log.d(TAG, "[HDMIConnectivityPreference_onPrepareDialogBuilder] hdmiConnector.HdmiCheckCurRes() : "
                                                            + hdmiConnector.HdmiCheckCurRes());
                                            break;
                                    case 3:
                                            hdmiConnector.HdmiChangeAVFormat(_480I);
                                            Log.d(TAG, "[HDMIConnectivityPreference_onPrepareDialogBuilder] hdmiConnector.HdmiCheckCurRes() : "
                                                            + hdmiConnector.HdmiCheckCurRes());
                                            break;
                                    case 4:
                                            hdmiConnector.HdmiChangeAVFormat(_AUTO);
                                            Log.d(TAG, "[HDMIConnectivityPreference_onPrepareDialogBuilder] hdmiConnector.HdmiCheckCurRes() : "
                                                            + hdmiConnector.HdmiCheckCurRes());
                                            break;
    //                                case 5:
    //                                        hdmiConnector.HdmiChangeAVFormat(_1080P);
    //                                        break;
                            }
    
                            setValue(mEntryValues[which].toString());
                            HDMIConnectivityPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                            dialog.dismiss();
                        }
            }).setPositiveButton(null, null);
    
    
        }
    
        @Override
        protected void onDialogClosed(boolean positiveResult) {
            super.onDialogClosed(positiveResult);
    
            if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntryValues != null) {
                String value = mEntryValues[mClickedDialogEntryIndex].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                }
            }
        }
    
        @Override
        protected Object onGetDefaultValue(TypedArray a, int index) {
            return a.getString(index);
        }
    
        @Override
        protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
            setValue(restoreValue ? getPersistedString(mValue) : (String) defaultValue);
        }
    
        @Override
        protected Parcelable onSaveInstanceState() {
            final Parcelable superState = super.onSaveInstanceState();
            if (isPersistent()) {
                // No need to save instance state since it's persistent
                return superState;
            }
    
            final SavedState myState = new SavedState(superState);
            myState.value = getValue();
            return myState;
        }
    
        @Override
        protected void onRestoreInstanceState(Parcelable state) {
            if (state == null || !state.getClass().equals(SavedState.class)) {
                // Didn't save state for us in onSaveInstanceState
                super.onRestoreInstanceState(state);
                return;
            }
    
            SavedState myState = (SavedState) state;
            super.onRestoreInstanceState(myState.getSuperState());
            setValue(myState.value);
        }
    
        private static class SavedState extends BaseSavedState {
            String value;
    
            public SavedState(Parcel source) {
                super(source);
                value = source.readString();
            }
    
            @Override
            public void writeToParcel(Parcel dest, int flags) {
                super.writeToParcel(dest, flags);
                dest.writeString(value);
            }
    
            public SavedState(Parcelable superState) {
                super(superState);
            }
    
            public static final Parcelable.Creator<SavedState> CREATOR =
                    new Parcelable.Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }
    
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
        */
		//LGE_CHANGE_E [yongjoo.jung@lge.com], 2010-10-25, Merge Connectivity menu from Hub, temporary comment for no-error
}
