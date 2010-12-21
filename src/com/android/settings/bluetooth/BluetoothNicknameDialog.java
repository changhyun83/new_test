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

package com.android.settings.bluetooth;

import android.app.Activity;

import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothIntent;
//import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * BluetoothNicknameDialog asks the user to enter a new nickname for a remote device
 */
public class BluetoothNicknameDialog extends AlertActivity implements DialogInterface.OnClickListener,
        TextWatcher/*Activity implements Button.OnClickListener*/ {
    private static final String TAG = "BluetoothNicknameDialog";
	public static final String EXTRA_DEVICE = "device"; //Nick Name
	private LocalBluetoothManager mLocalManager;
	private String mAddress;
	private String mNickName;
	private final int BLUETOOTH_NICKNAME_MAX_LENGTH = 20;	//TODO: Confirm!
	private Button mOkButton;
	private BluetoothDevice mDevice; //Praveen Added

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		if(!intent.getAction().equals("android.bluetooth.intent.action.NICKNAME"))//TODO: Move it to BluetoothIntent class
		{	
			Log.e(TAG, "Error: This activity must only be started with intent - " + 
						"android.bluetooth.intent.action.NICKNAME" );

			finish();
		}
		
		mLocalManager = LocalBluetoothManager.getInstance(this);
		mAddress = intent.getStringExtra("BDA_Remote");
		mNickName = intent.getStringExtra("currNickName");
		mDevice = intent.getParcelableExtra(EXTRA_DEVICE); //Praveen added
		//Set up Dialog
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_nickname); 
		p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok); 
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
		p.mNegativeButtonListener = this;
        setupAlert();
        mOkButton = mAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        mOkButton.setEnabled(false);
		
	}

	private View createView() {
		View view = getLayoutInflater().inflate(R.layout.bluetooth_nickname_entry, null);
		
		
		TextView messageView = (TextView) view.findViewById(R.id.nickname_view); 
		messageView.setText(getString(R.string.bluetooth_nickname_text_view));
		
		EditText nickname = (EditText) view.findViewById(R.id.nickname_text);

		if(mNickName != null)
		{
			//For Edit Nickname
			nickname.setText(mNickName, BufferType.EDITABLE);
			nickname.selectAll();
		}

		nickname.addTextChangedListener(this);

		// Maximum of 20 characters in nickname
		nickname.setFilters(new InputFilter[] { new LengthFilter(BLUETOOTH_NICKNAME_MAX_LENGTH) });
		
		return view;
	}


	//@Override  //Nick
	public void onClick(DialogInterface dialog, int which)
	{
		switch(which)
		{
			case DialogInterface.BUTTON_POSITIVE:

			EditText nickname = (EditText)findViewById(R.id.nickname_text);
			mNickName = nickname.getText().toString();
			Log.d(TAG, "Setting nickname to: " + mNickName + " for address: " + mAddress );

			mDevice.setRemoteNickName(mAddress, mNickName);
			mLocalManager.getCachedDeviceManager().findDevice(mDevice).refreshName();//Praveen Modified

			break;

			case DialogInterface.BUTTON_NEGATIVE:
			//TODO: cancelNickname!
			Log.d(TAG, "Cancelling nickname set ");
			break;
		}
		
//		finish();
	}

    public void afterTextChanged(Editable s) {
    	if (s.length() > 0) {
        	mOkButton.setEnabled(true);
    	}
	}

	// Not used
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	//Not used
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

}
