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

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import com.ti.bluetooth.BluetoothOpp;
import android.bluetooth.BluetoothUuid;
/* TI_CHANGE_E [Merged by younghyun.kwon@lge.com] 2010-02-13 */
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/* TI HID port - start */
import android.os.SystemProperties;
/* TI HID port - end */

/**
 * CachedBluetoothDevice represents a remote Bluetooth device. It contains
 * attributes of the device (such as the address, name, RSSI, etc.) and
 * functionality that can be performed on the device (connect, pair, disconnect,
 * etc.).
 */
public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private static final String TAG = "CachedBluetoothDevice";
    private static final boolean D = LocalBluetoothManager.D;
    private static final boolean V = LocalBluetoothManager.V;
    private static final boolean DEBUG = false;

    private static final int CONTEXT_ITEM_CONNECT = Menu.FIRST + 1;
    private static final int CONTEXT_ITEM_DISCONNECT = Menu.FIRST + 2;
    private static final int CONTEXT_ITEM_UNPAIR = Menu.FIRST + 3;
    /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23 START*/
//    private static final int CONTEXT_ITEM_PULL_DEFAULT_VCARD = Menu.FIRST + 4;
//    private static final int CONTEXT_ITEM_GET_MONITORING_IMAGE = Menu.FIRST + 5;    
//    private static final int CONTEXT_ITEM_GET_IMAGE_LIST = Menu.FIRST + 6;    
    /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23 END*/
    private static final int CONTEXT_ITEM_CONNECT_ADVANCED = Menu.FIRST + 4;

    //Feature - Set Nickname - BEGIN
    //    private static final int CONTEXT_ITEM_SET_AS_AUTHORIZED = Menu.FIRST + 8;//Trust Added    /*  BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23*/		
    private static final int CONTEXT_ITEM_UNAUTHORIZE = Menu.FIRST + 5;
    private static final int CONTEXT_ITEM_SET_NICKNAME = Menu.FIRST + 6;
    private static final int CONTEXT_ITEM_EDIT_NICKNAME = Menu.FIRST + 7;
    private static final int CONTEXT_ITEM_REMOVE_NICKNAME = Menu.FIRST + 8;
    //Feature Set Nickname - End
    private static final int CONTEXT_ITEM_PAIR = Menu.FIRST + 9;						/*  BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23*/

    public static final String OPP_RECEIVER_PACKAGE_NAME = "com.android.bluetooth";
    public static final String OPP_RECEIVER_CLASS_NAME = "com.android.bluetooth.opp.BluetoothOppReceiver";
/* TI_CHANGE_E [Merged by younghyun.kwon@lge.com] 2010-02-13 */
    private final BluetoothDevice mDevice;
    private String mName;
    private short mRssi;
    private BluetoothClass mBtClass;

    private List<Profile> mProfiles = new ArrayList<Profile>();

    private boolean mVisible;
    private boolean mIsAuthorized = false;  //Trust Added  /* TI_CHANGE [Merged by younghyun.kwon@lge.com] 2010-02-13 */

    private final LocalBluetoothManager mLocalManager;

    private List<Callback> mCallbacks = new ArrayList<Callback>();

    /**
     * When we connect to multiple profiles, we only want to display a single
     * error even if they all fail. This tracks that state.
     */
    private boolean mIsConnectingErrorPossible;

    /**
     * Last time a bt profile auto-connect was attempted.
     * If an ACTION_UUID intent comes in within
     * MAX_UUID_DELAY_FOR_AUTO_CONNECT milliseconds, we will try auto-connect
     * again with the new UUIDs
     */
    private long mConnectAttempted;

    // See mConnectAttempted
    private static final long MAX_UUID_DELAY_FOR_AUTO_CONNECT = 5000;

    // Max time to hold the work queue if we don't get or missed a response
    // from the bt framework.
    private static final long MAX_WAIT_TIME_FOR_FRAMEWORK = 25 * 1000;

    private enum BluetoothCommand {
        CONNECT, DISCONNECT,
    }

    static class BluetoothJob {
        final BluetoothCommand command; // CONNECT, DISCONNECT
        final CachedBluetoothDevice cachedDevice;
        final Profile profile; // HEADSET, A2DP, etc
        // 0 means this command was not been sent to the bt framework.
        long timeSent;

        public BluetoothJob(BluetoothCommand command,
                CachedBluetoothDevice cachedDevice, Profile profile) {
            this.command = command;
            this.cachedDevice = cachedDevice;
            this.profile = profile;
            this.timeSent = 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(command.name());
            sb.append(" Address:").append(cachedDevice.mDevice);
                sb.append(" Profile:").append(profile.name());
            sb.append(" TimeSent:");
            if (timeSent == 0) {
                sb.append("not yet");
            } else {
                sb.append(DateFormat.getTimeInstance().format(new Date(timeSent)));
            }
            return sb.toString();
        }
    }

    /**
     * We want to serialize connect and disconnect calls. http://b/170538
     * This are some headsets that may have L2CAP resource limitation. We want
     * to limit the bt bandwidth usage.
     *
     * A queue to keep track of asynchronous calls to the bt framework.  The
     * first item, if exist, should be in progress i.e. went to the bt framework
     * already, waiting for a notification to come back. The second item and
     * beyond have not been sent to the bt framework yet.
     */
    private static LinkedList<BluetoothJob> workQueue = new LinkedList<BluetoothJob>();

    private void queueCommand(BluetoothJob job) {
	if (D) {
	    Log.d(TAG, workQueue.toString());
	}
        synchronized (workQueue) {
            boolean processNow = pruneQueue(job);

            // Add job to queue
            if (D) {
                Log.d(TAG, "Adding: " + job.toString());
            }
            workQueue.add(job);

            // if there's nothing pending from before, send the command to bt
            // framework immediately.
            if (workQueue.size() == 1 || processNow) {
                // If the failed to process, just drop it from the queue.
                // There will be no callback to remove this from the queue.
                processCommands();
            }
        }
    }

    private boolean pruneQueue(BluetoothJob job) {
        boolean removedStaleItems = false;
        long now = System.currentTimeMillis();
        Iterator<BluetoothJob> it = workQueue.iterator();
        while (it.hasNext()) {
            BluetoothJob existingJob = it.next();

            // Remove any pending CONNECTS when we receive a DISCONNECT
            if (job != null && job.command == BluetoothCommand.DISCONNECT) {
                if (existingJob.timeSent == 0
                        && existingJob.command == BluetoothCommand.CONNECT
                        && existingJob.cachedDevice.mDevice.equals(job.cachedDevice.mDevice)
                        && existingJob.profile == job.profile) {
                    if (D) {
                        Log.d(TAG, "Removed because of a pending disconnect. " + existingJob);
                    }
                    it.remove();
                    continue;
                }
            }

            // Defensive Code: Remove any job that older than a preset time.
            // We never got a call back. It is better to have overlapping
            // calls than to get stuck.
            if (existingJob.timeSent != 0
                    && (now - existingJob.timeSent) >= MAX_WAIT_TIME_FOR_FRAMEWORK) {
                Log.w(TAG, "Timeout. Removing Job:" + existingJob.toString());
                it.remove();
                removedStaleItems = true;
                continue;
            }
        }
        return removedStaleItems;
    }

    private boolean processCommand(BluetoothJob job) {
        boolean successful = false;
        if (job.timeSent == 0) {
            job.timeSent = System.currentTimeMillis();
            switch (job.command) {
            case CONNECT:
                successful = connectInt(job.cachedDevice, job.profile);
                break;
            case DISCONNECT:
                successful = disconnectInt(job.cachedDevice, job.profile);
                break;
            }

            if (successful) {
                if (D) {
                    Log.d(TAG, "Command sent successfully:" + job.toString());
                }
            } else if (V) {
                Log.v(TAG, "Framework rejected command immediately:" + job.toString());
            }
        } else if (D) {
            Log.d(TAG, "Job already has a sent time. Skip. " + job.toString());
        }

        return successful;
    }

    public void onProfileStateChanged(Profile profile, int newProfileState) {
        if (D) {
            Log.d(TAG, "onProfileStateChanged:" + workQueue.toString());
        }

        int newState = LocalBluetoothProfileManager.getProfileManager(mLocalManager,
                profile).convertState(newProfileState);

        if (newState == SettingsBtStatus.CONNECTION_STATUS_CONNECTED) {
            if (!mProfiles.contains(profile)) {
                mProfiles.add(profile);
            Log.d(TAG, "onProfileStateChanged: added profile " + profile.toString());
            }
        }

        /* Ignore the transient states e.g. connecting, disconnecting */
        if (newState == SettingsBtStatus.CONNECTION_STATUS_CONNECTED ||
                newState == SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED) {
            BluetoothJob job = workQueue.peek();
            if (job == null) {
                return;
            } else if (!job.cachedDevice.mDevice.equals(mDevice)) {
                // This can happen in 2 cases: 1) BT device initiated pairing and
                // 2) disconnects of one headset that's triggered by connects of
                // another.
                if (D) {
                    Log.d(TAG, "mDevice:" + mDevice + " != head:" + job.toString());
                }

                // Check to see if we need to remove the stale items from the queue
                if (!pruneQueue(null)) {
                    // nothing in the queue was modify. Just ignore the notification and return.
                    return;
                }
            } else {
                // Remove the first item and process the next one
                workQueue.poll();
            }

            processCommands();
        }
    }

    /*
     * This method is called in 2 places:
     * 1) queryCommand() - when someone or something want to connect or
     *    disconnect
     * 2) onProfileStateChanged() - when the framework sends an intent
     *    notification when it finishes processing a command
     */
    private void processCommands() {
        if (D) {
            Log.d(TAG, "processCommands:" + workQueue.toString());
        }
        Iterator<BluetoothJob> it = workQueue.iterator();
        while (it.hasNext()) {
            BluetoothJob job = it.next();
            if (processCommand(job)) {
                // Sent to bt framework. Done for now. Will remove this job
                // from queue when we get an event
                return;
            } else {
                /*
                 * If the command failed immediately, there will be no event
                 * callbacks. So delete the job immediately and move on to the
                 * next one
                 */
                it.remove();
            }
        }
    }

    CachedBluetoothDevice(Context context, BluetoothDevice device) {
        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            throw new IllegalStateException(
                    "Cannot use CachedBluetoothDevice without Bluetooth hardware");
        }

        mDevice = device;

        fillData();
    }

    public void onClicked() {
        int bondState = getBondState();

        if (isConnected()) {
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            connect();
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            pair();
        }
    }

    public void disconnect() {
        for (Profile profile : mProfiles) {
            disconnect(profile);
        }
    }

    public void disconnect(Profile profile) {
        queueCommand(new BluetoothJob(BluetoothCommand.DISCONNECT, this, profile));
    }

    private boolean disconnectInt(CachedBluetoothDevice cachedDevice, Profile profile) {
        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(cachedDevice.mDevice);
        if (SettingsBtStatus.isConnectionStatusConnected(status)) {
            if (profileManager.disconnect(cachedDevice.mDevice)) {
                return true;
            }
        }
        return false;
    }

    public void askDisconnect() {
        Context context = mLocalManager.getForegroundActivity();
        if (context == null) {
            // Cannot ask, since we need an activity context
            disconnect();
            return;
        }

        Resources res = context.getResources();

        String name = getName();
        if (TextUtils.isEmpty(name)) {
            name = res.getString(R.string.bluetooth_device);
        }
        String message = res.getString(R.string.bluetooth_disconnect_blank, name);

        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                disconnect();
            }
        };

        new AlertDialog.Builder(context)
                .setTitle(getName())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, disconnectListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void connect() {
        if (!ensurePaired()) return;

        mConnectAttempted = SystemClock.elapsedRealtime();
        Log.d(TAG, "connect: mConnectAttempted = " + mConnectAttempted);

        connectWithoutResettingTimer();
    }
//BugFix Eclair 2.1 Modification
    /*package*/ void onBondingDockConnect() {
        // Don't connect just set the timer.
        // TODO(): Fix the actual problem
        mConnectAttempted = SystemClock.elapsedRealtime();
    }
//BugFix Eclair 2.1 Modification
    private void connectWithoutResettingTimer() {
        // Try to initialize the profiles if there were not.
        if (mProfiles.size() == 0) {
            if (!updateProfiles()) {
                // If UUIDs are not available yet, connect will be happen
                // upon arrival of the ACTION_UUID intent.
                if (DEBUG) Log.d(TAG, "No profiles. Maybe we will connect later");
                return;
            }
        }

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (Profile profile : mProfiles) {
            Log.d(TAG, "connectWithoutResettingTimer: " + profile.toString());
            if (isConnectableProfile(profile)) {
                LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                        .getProfileManager(mLocalManager, profile);
                if (profileManager.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    disconnectConnected(profile);
                    queueCommand(new BluetoothJob(BluetoothCommand.CONNECT, this, profile));
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Preferred profiles = " + preferredProfiles);

        if (preferredProfiles == 0) {
            connectAllProfiles();
        }
    }

    private void connectAllProfiles() {
        if (!ensurePaired()) return;

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        for (Profile profile : mProfiles) {
            Log.d(TAG, "connectAllProfiles: " + profile.toString());
            if (isConnectableProfile(profile)) {
                LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                        .getProfileManager(mLocalManager, profile);
                profileManager.setPreferred(mDevice, false);
                disconnectConnected(profile);
                queueCommand(new BluetoothJob(BluetoothCommand.CONNECT, this, profile));
            }
        }
    }

    public void connect(Profile profile) {
        Log.d(TAG, "connect: " + profile.toString());
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        disconnectConnected(profile);
        queueCommand(new BluetoothJob(BluetoothCommand.CONNECT, this, profile));
    }

    private void disconnectConnected(Profile profile) {
        LocalBluetoothProfileManager profileManager =
            LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        CachedBluetoothDeviceManager cachedDeviceManager = mLocalManager.getCachedDeviceManager();
        Set<BluetoothDevice> devices = profileManager.getConnectedDevices();
        if (devices == null) return;
        for (BluetoothDevice device : devices) {
            CachedBluetoothDevice cachedDevice = cachedDeviceManager.findDevice(device);
            if (cachedDevice != null) {
                queueCommand(new BluetoothJob(BluetoothCommand.DISCONNECT, cachedDevice, profile));
            }
        }
    }

    private boolean connectInt(CachedBluetoothDevice cachedDevice, Profile profile) {
        if (!cachedDevice.ensurePaired()) return false;

        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(cachedDevice.mDevice);
        if (!SettingsBtStatus.isConnectionStatusConnected(status)) {
            if (profileManager.connect(cachedDevice.mDevice)) {
                return true;
            }
            Log.i(TAG, "Failed to connect " + profile.toString() + " to " + cachedDevice.mName);
        } else {
            Log.i(TAG, "Already connected");
        }
        return false;
    }

    public void showConnectingError() {
        if (!mIsConnectingErrorPossible) return;
        mIsConnectingErrorPossible = false;

        mLocalManager.showError(mDevice, R.string.bluetooth_error_title,
                R.string.bluetooth_connecting_error_message);
    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            pair();
            return false;
        } else {
            return true;
        }
    }

    public void pair() {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();

        // Pairing is unreliable while scanning, so cancel discovery
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        if (!mDevice.createBond()) {
            mLocalManager.showError(mDevice, R.string.bluetooth_error_title,
                    R.string.bluetooth_pairing_error_message);
        }
    }

    public void unpair() {
        synchronized (workQueue) {
            // Remove any pending commands for this device
            boolean processNow = false;
            Iterator<BluetoothJob> it = workQueue.iterator();
            while (it.hasNext()) {
                BluetoothJob job = it.next();
                if (job.cachedDevice.mDevice.equals(this.mDevice)) {
                    it.remove();
                    if (job.timeSent != 0) {
                        processNow = true;
                    }
                }
            }
            if (processNow) {
                processCommands();
            }
        }

        switch (getBondState()) {
        case BluetoothDevice.BOND_BONDED:
            mDevice.removeBond();
            break;

        case BluetoothDevice.BOND_BONDING:
            mDevice.cancelBondProcess();
            break;
        }
    }

    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();

        mVisible = false;

        mIsAuthorized = mDevice.getTrustState();//TI_Feature - Set Trusted /* TI_CHANGE [Merged by younghyun.kwon@lge.com] 2010-02-13 */
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if (!mName.equals(name)) {
            if (TextUtils.isEmpty(name)) {
                mName = mDevice.getAddress();
            } else {
                mName = name;
            }
            dispatchAttributesChanged();
        }
    }

    public void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        mName = mDevice.getName();

        if (TextUtils.isEmpty(mName)) {
            mName = mDevice.getAddress();
            if (DEBUG) Log.d(TAG, "Default to address. Device has no name (yet) " + mName);
        }
    }

    public void refresh() {
        dispatchAttributesChanged();
    }

    public boolean isVisible() {
        return mVisible;
    }

    void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        return mDevice.getBondState();
    }

    void setRssi(short rssi) {
        if (mRssi != rssi) {
            mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    /**
     * Checks whether we are connected to this device (any profile counts).
     *
     * @return Whether it is connected.
     */
    public boolean isConnected() {
        for (Profile profile : mProfiles) {
            int status = LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile)
                    .getConnectionStatus(mDevice);
            if (SettingsBtStatus.isConnectionStatusConnected(status)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBusy() {
        for (Profile profile : mProfiles) {
            int status = LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile)
                    .getConnectionStatus(mDevice);
            if (SettingsBtStatus.isConnectionStatusBusy(status)) {
                return true;
            }
        }

        if (getBondState() == BluetoothDevice.BOND_BONDING) {
            return true;
        }

        return false;
    }

    /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23 START*/
    public boolean isBond() {
        if (getBondState() == BluetoothDevice.BOND_BONDED) {
            return true;
        }
        return false;
    }
    /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-02-23 END*/
    
    public int getBtClassDrawable() {
        if (mBtClass != null) {    /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-04-15 START*/

	    int devClass = mBtClass.getDeviceClass();

            switch (mBtClass.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.COMPUTER:
	            	if(isConnected()){
                return R.drawable.ic_bt_laptop;
	            	}else{
	            		return R.drawable.ic_bt_laptop_dis;				
	            	}
            case BluetoothClass.Device.Major.PHONE:
	            	if(isConnected()){
                return R.drawable.ic_bt_cellphone;
            }
	            	else{
	            		return R.drawable.ic_bt_cellphone_dis;
	            	}
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
            	if(isConnected()){  
            		if(	devClass==BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
                            devClass==BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO )
                            return R.drawable.ic_bt_headset_hfp;
                        else
                           return R.drawable.ic_bt_headphones_a2dp;
            	}
            	else{
            		if(	devClass==BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
                            devClass==BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO )
            		    return R.drawable.ic_bt_headset_hfp_dis;
                    else
                    	return R.drawable.ic_bt_headphones_a2dp_dis;
            	}
            case BluetoothClass.Device.Major.PERIPHERAL:
	            	if(isConnected()){
	            		return R.drawable.ic_bt_device;
	            	}
	            	else{
	            		return R.drawable.ic_bt_device_dis;
	            	}
	            case BluetoothClass.Device.Major.IMAGING:
	            	if(isConnected()){
	            		return R.drawable.ic_bt_printer;
	            	}
	            	else{
	            		return R.drawable.ic_bt_printer_dis;
	            	}
				default:
	            	if(isConnected()){
	            		return R.drawable.ic_bt_device;
	            	}
	            	else{
	            		return R.drawable.ic_bt_device_dis;
	            	}
            }
        } else {
            Log.w(TAG, "mBtClass is null");
        if (mProfiles.size() > 0) {
            if (mProfiles.contains(Profile.A2DP)) {
	            	if(isConnected()){
                return R.drawable.ic_bt_headphones_a2dp;
	            	}else{
	            		return R.drawable.ic_bt_headphones_a2dp_dis;	
	            	}
            } else if (mProfiles.contains(Profile.HEADSET)) {
	            	if(isConnected()){
                return R.drawable.ic_bt_headset_hfp;
            }
	            	else{
	            		return R.drawable.ic_bt_headset_hfp_dis;		
	            	}
	            }
        } else if (mBtClass != null) {
            if (mBtClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
	            	if(isConnected()){
                return R.drawable.ic_bt_headphones_a2dp;
	            	}else{
	            		return R.drawable.ic_bt_headphones_a2dp_dis;	
	            	}
            }
            if (mBtClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
	            	if(isConnected()){
                return R.drawable.ic_bt_headset_hfp;
            }
	            	else{
	            		return R.drawable.ic_bt_headset_hfp_dis;		
	            	}
            }
	        }else{
            	if(isConnected()){
            		return R.drawable.ic_bt_device;
            	}
            	else{
            		return R.drawable.ic_bt_device_dis;
            }
        }
        }   /* BT_SettingsUI_CHANGE [gu.bosung@lge.com] 2010-04-15 END*/
        return 0;
    }

    /**
     * Fetches a new value for the cached BT class.
     */
    private void fetchBtClass() {
        mBtClass = mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = mDevice.getUuids();
        if (uuids == null) return false;

        LocalBluetoothProfileManager.updateProfiles(uuids, mProfiles);

        /* TI_CHANGE_S [Merged by younghyun.kwon@lge.com] 2010-02-13 */
        for (Profile profile : mProfiles) {
            Log.d(TAG, "updateProfiles: setPreferred " + profile.toString());
            if (isConnectableProfile(profile)) {
                LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                        .getProfileManager(mLocalManager, profile);
                profileManager.setPreferred(mDevice, true);
            }
        }
        /* TI_CHANGE_E [Merged by younghyun.kwon@lge.com] 2010-02-13 */

        if (DEBUG) {
            Log.e(TAG, "updating profiles for " + mDevice.getName());

            boolean printUuids = true;
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();

            if (bluetoothClass != null) {
                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET) !=
                    mProfiles.contains(Profile.HEADSET)) {
                    Log.v(TAG, "headset classbits != uuid");
                    printUuids = true;
                }

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP) !=
                    mProfiles.contains(Profile.A2DP)) {
                    Log.v(TAG, "a2dp classbits != uuid");
                    printUuids = true;
                }

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_OPP) !=
                    mProfiles.contains(Profile.OPP)) {
                    Log.v(TAG, "opp classbits != uuid");
                    printUuids = true;
                }
                /* TI HID port - start */
                if (SystemProperties.OMAP_ENHANCEMENT &&
                   ((bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HID) !=
                   mProfiles.contains(Profile.HID)))) {
                   Log.v(TAG, "HID classbits != uuid");
                   printUuids = true;
                }
                /* TI HID port - end */
            }

            if (printUuids) {
                if (bluetoothClass != null) Log.v(TAG, "Class: " + bluetoothClass.toString());
                Log.v(TAG, "UUID:");
                for (int i = 0; i < uuids.length; i++) {
                    Log.v(TAG, "  " + uuids[i]);
                }
            }
        }
        return true;
    }

    /**
     * Refreshes the UI for the BT class, including fetching the latest value
     * for the class.
     */
    public void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    /**
     * Refreshes the UI when framework alerts us of a UUID change.
     */
    public void onUuidChanged() {
        updateProfiles();

        if (DEBUG) {
            Log.e(TAG, "onUuidChanged: Time since last connect"
                    + (SystemClock.elapsedRealtime() - mConnectAttempted));
        }

        /*
         * If a connect was attempted earlier without any UUID, we will do the
         * connect now.
         */
        if (mProfiles.size() > 0
                && (mConnectAttempted + MAX_UUID_DELAY_FOR_AUTO_CONNECT) > SystemClock
                        .elapsedRealtime()) {
            connectWithoutResettingTimer();
        }
        dispatchAttributesChanged();
    }

    public void onBondingStateChanged(int bondState) {
        if (bondState == BluetoothDevice.BOND_NONE) {
            mProfiles.clear();
        }
        refresh();
    }

    public void setBtClass(BluetoothClass btClass) {
        if (btClass != null && mBtClass != btClass) {
            mBtClass = btClass;
            dispatchAttributesChanged();
        }
    }

    public int getSummary() {
        // TODO: clean up
        int oneOffSummary = getOneOffSummary();
        if (oneOffSummary != 0) {
            return oneOffSummary;
        }

        for (Profile profile : mProfiles) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, profile);
            int connectionStatus = profileManager.getConnectionStatus(mDevice);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus) ||
                    connectionStatus == SettingsBtStatus.CONNECTION_STATUS_CONNECTING ||
                    connectionStatus == SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING) {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        return SettingsBtStatus.getPairingStatusSummary(getBondState());
    }

    /**
     * We have special summaries when particular profiles are connected. This
     * checks for those states and returns an applicable summary.
     *
     * @return A one-off summary that is applicable for the current state, or 0.
     */
    private int getOneOffSummary() {
        boolean isA2dpConnected = false, isHeadsetConnected = false, isConnecting = false,isHidConnected = false;

        if (mProfiles.contains(Profile.A2DP)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.A2DP);
            isConnecting = profileManager.getConnectionStatus(mDevice) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isA2dpConnected = profileManager.isConnected(mDevice);
        }

        if (mProfiles.contains(Profile.HEADSET)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.HEADSET);
            isConnecting |= profileManager.getConnectionStatus(mDevice) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isHeadsetConnected = profileManager.isConnected(mDevice);
        }
        /* TI HID port - start */
        if (SystemProperties.OMAP_ENHANCEMENT && mProfiles.contains(Profile.HID)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.HID);
            isConnecting |= profileManager.getConnectionStatus(mDevice) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isHidConnected = profileManager.isConnected(mDevice);
        }
    /* TI HID port - end */
        if (isConnecting) {
            // If any of these important profiles is connecting, prefer that
            return SettingsBtStatus.getConnectionStatusSummary(
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING);
        } else if (isA2dpConnected && isHeadsetConnected) {
            return R.string.bluetooth_summary_connected_to_a2dp_headset;
        } else if (isA2dpConnected) {
            return R.string.bluetooth_summary_connected_to_a2dp;
        } else if (isHeadsetConnected) {
            return R.string.bluetooth_summary_connected_to_headset;
    }
        /* TI HID port - start */
          else if(SystemProperties.OMAP_ENHANCEMENT && isHidConnected) {
            return R.string.bluetooth_summary_connected_to_hid;
    }
        /* TI HID port - end */
        else {
            return 0;
        }
    }

    public List<Profile> getConnectableProfiles() {
        ArrayList<Profile> connectableProfiles = new ArrayList<Profile>();
        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                connectableProfiles.add(profile);
            }
        }
        return connectableProfiles;
    }

    /* TI HID port - added HID support here */
    private boolean isConnectableProfile(Profile profile) {
        return profile.equals(Profile.HEADSET) || profile.equals(Profile.A2DP)|| profile.equals(Profile.HID);
    }
    public void onCreateContextMenu(ContextMenu menu) {
        // No context menu if it is busy (none of these items are applicable if busy)
        if (mLocalManager.getBluetoothState() != BluetoothAdapter.STATE_ON || isBusy()) {
            return;
        }

        int bondState = getBondState();
        boolean isConnected = isConnected();
        boolean hasConnectableProfiles = false;

        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                hasConnectableProfiles = true;
                break;
            }
        }

        menu.setHeaderTitle(getName());

        if (bondState == BluetoothDevice.BOND_NONE) { // Not paired and not connected
            menu.add(0, CONTEXT_ITEM_CONNECT, 0, R.string.bluetooth_device_context_pair_connect);
        } else { // Paired
            if (isConnected) { // Paired and connected
                menu.add(0, CONTEXT_ITEM_DISCONNECT, 0,
                        R.string.bluetooth_device_context_disconnect);
                menu.add(0, CONTEXT_ITEM_UNPAIR, 0,
                        R.string.bluetooth_device_context_disconnect_unpair);
            } else { // Paired but not connected
                if (hasConnectableProfiles) {
                    menu.add(0, CONTEXT_ITEM_CONNECT, 0, R.string.bluetooth_device_context_connect);
                }
                menu.add(0, CONTEXT_ITEM_UNPAIR, 0, R.string.bluetooth_device_context_unpair);
            }

            // Show the connection options item
            if (hasConnectableProfiles) {
                menu.add(0, CONTEXT_ITEM_CONNECT_ADVANCED, 0,
                        R.string.bluetooth_device_context_connect_advanced);
            }
        }
    }

    /**
     * Called when a context menu item is clicked.
     *
     * @param item The item that was clicked.
     */
    public void onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CONTEXT_ITEM_DISCONNECT:
                disconnect();
                break;

            case CONTEXT_ITEM_CONNECT:
                connect();
                break;

            case CONTEXT_ITEM_UNPAIR:
                unpair();
                break;

            case CONTEXT_ITEM_CONNECT_ADVANCED:
                Intent intent = new Intent();
                // Need an activity context to open this in our task
                Context context = mLocalManager.getForegroundActivity();
                if (context == null) {
                    // Fallback on application context, and open in a new task
                    context = mLocalManager.getContext();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                intent.setClass(context, ConnectSpecificProfilesActivity.class);
                intent.putExtra(ConnectSpecificProfilesActivity.EXTRA_DEVICE, mDevice);
                context.startActivity(intent);
                break;
        }
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAttributesChanged(this);
            }
        }
    }

    @Override
    public String toString() {
        return mDevice.toString();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof CachedBluetoothDevice)) {
            throw new ClassCastException();
        }

        return mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }

    public int compareTo(CachedBluetoothDevice another) {
        int comparison;

        // Connected above not connected
        comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) return comparison;

        // Paired above not paired
        comparison = (another.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0) -
            (getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
        if (comparison != 0) return comparison;

        // Visible above not visible
        comparison = (another.mVisible ? 1 : 0) - (mVisible ? 1 : 0);
        if (comparison != 0) return comparison;

        // Stronger signal above weaker signal
        comparison = another.mRssi - mRssi;
        if (comparison != 0) return comparison;

        // Fallback on name
        return getName().compareTo(another.getName());
    }

    public interface Callback {
        void onDeviceAttributesChanged(CachedBluetoothDevice cachedDevice);
    }
}
