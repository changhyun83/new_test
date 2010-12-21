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

import com.android.settings.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

/**
 * BluetoothAuthorizationRequest is a receiver for any Bluetooth authorization request. It
 * checks if the Bluetooth Settings is currently visible and brings up the authorization
 * dialog. Otherwise it puts a Notification in the status bar, which can
 * be clicked to bring up the authorization dialog.
 */
public class BluetoothAuthorizationRequest extends BroadcastReceiver {
	 private static final String TAG = "BluetoothAuthorizationDialog";
    public static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;
    private BluetoothDevice mDevice;
	private String mAddress;
	private String mName;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothDevice.AUTHORIZATION_REQUEST_ACTION)) {
	
            LocalBluetoothManager mLocalManager = LocalBluetoothManager.getInstance(context);        
        
	    mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
/*		if(mDevice.getTrustState())
		{
			Log.e(TAG,"+++++++++++++Bluetooth Device already Authorized+++++++++++");
			mDevice.setTrust(true);
			return;
		}*/
	   mAddress = intent.getStringExtra(BluetoothDevice.EXTRA_ADDRESS);
	   mName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            Intent authIntent = new Intent();
            authIntent.setClass(context, BluetoothAuthorizationDialog.class);
            authIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            authIntent.putExtra(BluetoothDevice.EXTRA_ADDRESS, mAddress);
	  authIntent.putExtra(BluetoothDevice.EXTRA_NAME, mName);
            authIntent.setAction(BluetoothDevice.AUTHORIZATION_REQUEST_ACTION);
            authIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            
            if (mLocalManager.getForegroundActivity() != null) {
                // Since the BT-related activity is in the foreground, just open the dialog
                context.startActivity(authIntent);
                
            } else {
                
                // Put up a notification that leads to the dialog
                Resources res = context.getResources();
                Notification notification = new Notification(
                        android.R.drawable.stat_sys_data_bluetooth,
                        res.getString(R.string.bluetooth_notif_auth_ticker),
                        System.currentTimeMillis());

                PendingIntent pending = PendingIntent.getActivity(context, 0, 
                		authIntent, PendingIntent.FLAG_ONE_SHOT);
                 
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (TextUtils.isEmpty(name)) {
                       name = mLocalManager.getCachedDeviceManager().getName(mDevice); 
                }
                
                notification.setLatestEventInfo(context, 
                        res.getString(R.string.bluetooth_notif_auth_title), 
                        res.getString(R.string.bluetooth_notif_auth_message) + name, 
                        pending);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.defaults |= Notification.DEFAULT_SOUND;
                
                NotificationManager manager = (NotificationManager) 
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, notification);
            }
        } else if (action.equals(BluetoothDevice.AUTHORIZATION_CANCEL_ACTION)) {
            
            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
