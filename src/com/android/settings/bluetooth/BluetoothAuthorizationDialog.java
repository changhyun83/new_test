/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2010 Texas Instruments Israel Ltd.
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

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * BluetoothAuthorizationDialog asks the user to enter Y/N for authorization with a remote
 * Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothAuthorizationDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "BluetoothAuthorizationDialog";

    private LocalBluetoothManager mLocalManager;
    private String mAddress;
  private BluetoothDevice mDevice;  
  private String mName;
  private String mRemoteName;
  private CheckBox checkbox = null;

  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 Log.e(TAG,"Dialog################# ");
        Intent intent = getIntent();
        if (!intent.getAction().equals(BluetoothDevice.AUTHORIZATION_REQUEST_ACTION))
        {
            Log.e(TAG,
                  "Error: this activity may be started only with intent " +
                  BluetoothDevice.AUTHORIZATION_REQUEST_ACTION);
            finish();
        }
       Log.e(TAG,"Dialog################# "+intent.getAction());  
        mLocalManager = LocalBluetoothManager.getInstance(this);
	mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	mAddress = intent.getStringExtra(BluetoothDevice.EXTRA_ADDRESS);
	mName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
//	mRemoteName = mDevice.getName();
//       mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
// 	mAddress = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
  //      mAddress = intent.getStringExtra(BluetoothDevice.ADDRESS);
        
        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_authorization);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_authorization, null);
    String name = mLocalManager.getCachedDeviceManager().getName(mDevice);     
 //       String name = mLocalManager.getLocalDeviceManager().getName(mAddress);
        TextView messageView = (TextView) view.findViewById(R.id.message);
 	checkbox = (CheckBox) view.findViewById(R.id.always_trust);
	checkbox.setText(R.string.trust_service);
        messageView.setText(getString(R.string.bluetooth_authorization_msg, name));
        
        return view;
    }
    
    public void onClick(DialogInterface dialog, int which) {
		
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
	boolean value = checkbox.isChecked();
	if(value){
                    mDevice.authorizeDeviceReply(BluetoothDevice.ALWAYS_AUTHORIZED_REPLY);
                    Log.d(TAG,"Option service Successfully set as ALWAYS AUTHORIZED");
	}else{
                    mDevice.authorizeDeviceReply(BluetoothDevice.AUTHORIZED_REPLY);
                    Log.d(TAG,"Option service Successfully set as AUTHORIZED_NOT_TRUSTED");
	}
                Log.d(TAG,"Trust service Successfully set");		
                break;
                
            case DialogInterface.BUTTON_NEGATIVE:
                mDevice.authorizeDeviceReply(BluetoothDevice.NOT_AUTHORIZED_REPLY);
                Log.d(TAG,"UnTrust service Successfully set");		
                break;
        }

    }

}
