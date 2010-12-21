/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetStatService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
// LGE_WCDMA_FEATURE_MERGE  START
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
// LGE_WCDMA_FEATURE_MERGE  END
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;
// LGE_WCDMA_FEATURE_MERGE  START
import android.widget.CheckBox;
import android.widget.Toast;
// LGE_GMM_ATTACH_MODE START
import android.widget.CompoundButton;
// LGE_GMM_ATTACH_MODE END
// LGE_WCDMA_FEATURE_MERGE  END

import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MPDP START
import com.android.internal.telephony.gsm.PdpConnection;
// LGE_MPDP EBD
// LGE_WCDMA_FEATURE_MERGE  END

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
// LGE_WCDMA_FEATURE_MERGE  START
import java.util.Map;
// LGE_WCDMA_FEATURE_MERGE  END
import android.util.Log;

public class RadioInfo extends Activity {
    private final String TAG = "phone";

    private static final int EVENT_PHONE_STATE_CHANGED = 100;
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;
    private static final int EVENT_CFI_CHANGED = 302;

    private static final int EVENT_QUERY_PREFERRED_TYPE_DONE = 1000;
    private static final int EVENT_SET_PREFERRED_TYPE_DONE = 1001;
    private static final int EVENT_QUERY_NEIGHBORING_CIDS_DONE = 1002;
    private static final int EVENT_QUERY_SMSC_DONE = 1005;
    private static final int EVENT_UPDATE_SMSC_DONE = 1006;

    private static final int MENU_ITEM_SELECT_BAND  = 0;
    private static final int MENU_ITEM_VIEW_ADN     = 1;
    private static final int MENU_ITEM_VIEW_FDN     = 2;
    private static final int MENU_ITEM_VIEW_SDN     = 3;
    private static final int MENU_ITEM_GET_PDP_LIST = 4;
    private static final int MENU_ITEM_TOGGLE_DATA  = 5;

    static final String ENABLE_DATA_STR = "Enable data connection";
    static final String DISABLE_DATA_STR = "Disable data connection";

    private TextView mDeviceId; //DeviceId is the IMEI in GSM and the MEID in CDMA
    private TextView number;
    private TextView callState;
    private TextView operatorName;
    private TextView roamingState;
    private TextView gsmState;
    private TextView gprsState;
    private TextView network;
    private TextView dBm;
    private TextView mMwi;
    private TextView mCfi;
    private TextView mLocation;
    private TextView mNeighboringCids;
    private TextView resets;
    private TextView attempts;
    private TextView successes;
    private TextView disconnects;
    private TextView sentSinceReceived;
    private TextView sent;
    private TextView received;
    private TextView mPingIpAddr;
    private TextView mPingHostname;
    private TextView mHttpClientTest;
    private TextView dnsCheckState;
    private EditText smsc;
    private Button radioPowerButton;
    private Button dnsCheckToggleButton;
    private Button pingTestButton;
    private Button updateSmscButton;
    private Button refreshSmscButton;
    private Button oemInfoButton;
    private Spinner preferredNetworkType;
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_GMM_ATTACH_MODE START
    private CheckBox gmmModeCheckbox;
// LGE_GMM_ATTACH_MODE END
// LGE_WCDMA_FEATURE_MERGE  END

    private TelephonyManager mTelephonyManager;
    private Phone phone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private INetStatService netstat;

    private String mPingIpAddrResult;
    private String mPingHostnameResult;
    private String mHttpClientTestResult;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;

// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MPDP_CHECK START
    private static final int MPDP_DISABLED = 0;
    private static final int MPDP_ENABLED_PRIMARY = 1;
    private static final int MPDP_ENABLED_FULL = 2;
    private Button mpdpButton;
    private TextView mpdpStateLabel;
// LGE_MPDP_CHECK END
// LGE_WCDMA_FEATURE_MERGE  END
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateDataStats();
            updatePdpList();
            updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            updateDataStats2();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            updateLocation(location);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            mMwiValue = mwi;
            updateMessageWaiting();
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            mCfiValue = cfi;
            updateCallRedirect();
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_PHONE_STATE_CHANGED:
                    updatePhoneState();
                    break;

                case EVENT_SIGNAL_STRENGTH_CHANGED:
                    updateSignalStrength();
                    break;

                case EVENT_SERVICE_STATE_CHANGED:
                    updateServiceState();
                    updatePowerState();
                    break;

                case EVENT_QUERY_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        int type = ((int[])ar.result)[0];
                        preferredNetworkType.setSelection(type, true);
                    } else {
                        preferredNetworkType.setSelection(8, true);
                    }
                    break;
                case EVENT_SET_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        phone.getPreferredNetworkType(
                                obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
                    }
                    break;
                case EVENT_QUERY_NEIGHBORING_CIDS_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        updateNeighboringCids((ArrayList<NeighboringCellInfo>)ar.result);
                    } else {
                        mNeighboringCids.setText("unknown");
                    }
                    break;
                case EVENT_QUERY_SMSC_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        smsc.setText("refresh error");
                    } else {
                        smsc.setText((String)ar.result);
                    }
                    break;
                case EVENT_UPDATE_SMSC_DONE:
                    updateSmscButton.setEnabled(true);
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        smsc.setText("update error");
                    }
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.radio_info);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        phone = PhoneFactory.getDefaultPhone();

        mDeviceId= (TextView) findViewById(R.id.imei);
        number = (TextView) findViewById(R.id.number);
        callState = (TextView) findViewById(R.id.call);
        operatorName = (TextView) findViewById(R.id.operator);
        roamingState = (TextView) findViewById(R.id.roaming);
        gsmState = (TextView) findViewById(R.id.gsm);
        gprsState = (TextView) findViewById(R.id.gprs);
        network = (TextView) findViewById(R.id.network);
        dBm = (TextView) findViewById(R.id.dbm);
        mMwi = (TextView) findViewById(R.id.mwi);
        mCfi = (TextView) findViewById(R.id.cfi);
        mLocation = (TextView) findViewById(R.id.location);
        mNeighboringCids = (TextView) findViewById(R.id.neighboring);

        resets = (TextView) findViewById(R.id.resets);
        attempts = (TextView) findViewById(R.id.attempts);
        successes = (TextView) findViewById(R.id.successes);
        disconnects = (TextView) findViewById(R.id.disconnects);
        sentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        sent = (TextView) findViewById(R.id.sent);
        received = (TextView) findViewById(R.id.received);
        smsc = (EditText) findViewById(R.id.smsc);
        dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);

        mPingIpAddr = (TextView) findViewById(R.id.pingIpAddr);
        mPingHostname = (TextView) findViewById(R.id.pingHostname);
        mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);

// LGE_WCDMA_FEATURE_MERGE  START
// LGE_GMM_ATTACH_MODE START
        gmmModeCheckbox = (CheckBox) findViewById(R.id.gmm_mode_checkbox);
        int gmm = 1;
        try{
            gmm = Settings.Secure.getInt(getContentResolver(), Settings.Secure.GMM_ATTACH_MODE);
            Log.d(TAG, "[RadioInfo] GMM: got value from settings: " + gmm);
        } catch(SettingNotFoundException ex) {
            // shouldn't happen because GsmDataConnectionTracker will set it on RADIO_AVAILABLE event
            Log.d(TAG, "[RadioInfo] GMM: put value after exception");
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.GMM_ATTACH_MODE, gmm);
        }
        if(gmm == 0) {
            Log.d(TAG, "[RadioInfo] GMM: set checkbox to false");
        	gmmModeCheckbox.setChecked(false);
        } else {
            Log.d(TAG, "[RadioInfo] GMM: set checkbox to true");
        	gmmModeCheckbox.setChecked(true);
        }
        gmmModeCheckbox.setOnClickListener(mGmmModeListener);
// LGE_GMM_ATTACH_MODE END

// LGE_WCDMA_FEATURE_MERGE  END
        preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                android.R.layout.simple_spinner_item, mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        preferredNetworkType.setAdapter(adapter);
        preferredNetworkType.setOnItemSelectedListener(mPreferredNetworkHandler);

        radioPowerButton = (Button) findViewById(R.id.radio_power);
        radioPowerButton.setOnClickListener(mPowerButtonHandler);

        pingTestButton = (Button) findViewById(R.id.ping_test);
        pingTestButton.setOnClickListener(mPingButtonHandler);
        updateSmscButton = (Button) findViewById(R.id.update_smsc);
        updateSmscButton.setOnClickListener(mUpdateSmscButtonHandler);
        refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        refreshSmscButton.setOnClickListener(mRefreshSmscButtonHandler);
        dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        dnsCheckToggleButton.setOnClickListener(mDnsCheckButtonHandler);

        oemInfoButton = (Button) findViewById(R.id.oem_info);
        oemInfoButton.setOnClickListener(mOemInfoButtonHandler);
        PackageManager pm = getPackageManager();
        Intent oemInfoIntent = new Intent("com.android.settings.OEM_RADIO_INFO");
        List<ResolveInfo> oemInfoIntentList = pm.queryIntentActivities(oemInfoIntent, 0);
        if (oemInfoIntentList.size() == 0) {
            oemInfoButton.setEnabled(false);
        }

        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        mPhoneStateReceiver.notifyPhoneCallState(EVENT_PHONE_STATE_CHANGED);

        phone.getPreferredNetworkType(
                mHandler.obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
        phone.getNeighboringCids(
                mHandler.obtainMessage(EVENT_QUERY_NEIGHBORING_CIDS_DONE));

        netstat = INetStatService.Stub.asInterface(ServiceManager.getService("netstat"));

        CellLocation.requestLocationUpdate();
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MPDP_CHECK START
        mpdpButton = (Button) findViewById(R.id.mpdp_toggle);
        mpdpButton.setOnClickListener(mMPDPCheckListener);
        mpdpStateLabel = (TextView) findViewById(R.id.mpdpState);
        int mpdp = loadMpdpState(true);
        updateMpdpState(mpdp);
// LGE_MPDP_CHECK END

// LGE_WCDMA_FEATURE_MERGE  END
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePhoneState();
        updateSignalStrength();
        updateMessageWaiting();
        updateCallRedirect();
        updateServiceState();
        updateLocation(mTelephonyManager.getCellLocation());
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updatePowerState();
        updateProperties();
        updateDnsCheckState();

        Log.i(TAG, "[RadioInfo] onResume: register phone & data intents");

        mPhoneStateReceiver.registerIntent();
        mTelephonyManager.listen(mPhoneStateListener,
                  PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_DATA_ACTIVITY
                | PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "[RadioInfo] onPause: unregister phone & data intents");

        mPhoneStateReceiver.unregisterIntent();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SELECT_BAND, 0, R.string.radio_info_band_mode_label)
                .setOnMenuItemClickListener(mSelectBandCallback)
                .setAlphabeticShortcut('b');
        menu.add(1, MENU_ITEM_VIEW_ADN, 0,
                R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(mViewADNCallback);
        menu.add(1, MENU_ITEM_VIEW_FDN, 0,
                R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(mViewFDNCallback);
        menu.add(1, MENU_ITEM_VIEW_SDN, 0,
                R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(mViewSDNCallback);
        menu.add(1, MENU_ITEM_GET_PDP_LIST,
                0, R.string.radioInfo_menu_getPDP).setOnMenuItemClickListener(mGetPdpList);
        menu.add(1, MENU_ITEM_TOGGLE_DATA,
                0, DISABLE_DATA_STR).setOnMenuItemClickListener(mToggleData);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Get the TOGGLE DATA menu item in the right state.
        MenuItem item = menu.findItem(MENU_ITEM_TOGGLE_DATA);
        int state = mTelephonyManager.getDataState();
        boolean visible = true;

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
            case TelephonyManager.DATA_SUSPENDED:
                item.setTitle(DISABLE_DATA_STR);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                item.setTitle(ENABLE_DATA_STR);
                break;
            default:
                visible = false;
                break;
        }
        item.setVisible(visible);
        return true;
    }

    private boolean isRadioOn() {
        return phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
    }

    private void updatePowerState() {
        String buttonText = isRadioOn() ?
                            getString(R.string.turn_off_radio) :
                            getString(R.string.turn_on_radio);
        radioPowerButton.setText(buttonText);
    }

    private void updateDnsCheckState() {
        dnsCheckState.setText(phone.isDnsCheckDisabled() ?
                "0.0.0.0 allowed" :"0.0.0.0 not allowed");
    }

    private final void
    updateSignalStrength() {
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.
        int state = mPhoneStateReceiver.getServiceState().getState();
        Resources r = getResources();

        if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                (ServiceState.STATE_POWER_OFF == state)) {
            dBm.setText("0");
        }

        int signalDbm = mPhoneStateReceiver.getSignalStrengthDbm();

        if (-1 == signalDbm) signalDbm = 0;

        int signalAsu = mPhoneStateReceiver.getSignalStrength();

        if (-1 == signalAsu) signalAsu = 0;

        dBm.setText(String.valueOf(signalDbm) + " "
            + r.getString(R.string.radioInfo_display_dbm) + "   "
            + String.valueOf(signalAsu) + " "
            + r.getString(R.string.radioInfo_display_asu));
    }

    private final void updateLocation(CellLocation location) {
        Resources r = getResources();
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation)location;
            int lac = loc.getLac();
            int cid = loc.getCid();
            mLocation.setText(r.getString(R.string.radioInfo_lac) + " = "
                    + ((lac == -1) ? "unknown" : Integer.toHexString(lac))
                    + "   "
                    + r.getString(R.string.radioInfo_cid) + " = "
                    + ((cid == -1) ? "unknown" : Integer.toHexString(cid)));
        } else if (location instanceof CdmaCellLocation) {
            CdmaCellLocation loc = (CdmaCellLocation)location;
            int bid = loc.getBaseStationId();
            int sid = loc.getSystemId();
            int nid = loc.getNetworkId();
            int lat = loc.getBaseStationLatitude();
            int lon = loc.getBaseStationLongitude();
            mLocation.setText("BID = "
                    + ((bid == -1) ? "unknown" : Integer.toHexString(bid))
                    + "   "
                    + "SID = "
                    + ((sid == -1) ? "unknown" : Integer.toHexString(sid))
                    + "   "
                    + "NID = "
                    + ((nid == -1) ? "unknown" : Integer.toHexString(nid))
                    + "\n"
                    + "LAT = "
                    + ((lat == -1) ? "unknown" : Integer.toHexString(lat))
                    + "   "
                    + "LONG = "
                    + ((lon == -1) ? "unknown" : Integer.toHexString(lon)));
        } else {
            mLocation.setText("unknown");
        }


    }

    private final void updateNeighboringCids(ArrayList<NeighboringCellInfo> cids) {
        StringBuilder sb = new StringBuilder();

        if (cids != null) {
            if ( cids.isEmpty() ) {
                sb.append("no neighboring cells");
            } else {
                for (NeighboringCellInfo cell : cids) {
                    sb.append(cell.toString()).append(" ");
                }
            }
        } else {
            sb.append("unknown");
        }
        mNeighboringCids.setText(sb.toString());
    }

    private final void
    updateMessageWaiting() {
        mMwi.setText(String.valueOf(mMwiValue));
    }

    private final void
    updateCallRedirect() {
        mCfi.setText(String.valueOf(mCfiValue));
    }


    private final void
    updateServiceState() {
        ServiceState serviceState = mPhoneStateReceiver.getServiceState();
        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = r.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = r.getString(R.string.radioInfo_service_emergency);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = r.getString(R.string.radioInfo_service_off);
                break;
        }

        gsmState.setText(display);

        if (serviceState.getRoaming()) {
            roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            roamingState.setText(R.string.radioInfo_roaming_not);
        }

        operatorName.setText(serviceState.getOperatorAlphaLong());
    }

    private final void
    updatePhoneState() {
        Phone.State state = mPhoneStateReceiver.getPhoneState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case IDLE:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case RINGING:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
            case OFFHOOK:
                display = r.getString(R.string.radioInfo_phone_offhook);
                break;
        }

        callState.setText(display);
    }

    private final void
    updateDataState() {
        int state = mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = r.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = r.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = r.getString(R.string.radioInfo_data_disconnected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = r.getString(R.string.radioInfo_data_suspended);
                break;
        }

        gprsState.setText(display);
    }

    private final void updateNetworkType() {
        Resources r = getResources();
        String display = SystemProperties.get(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                r.getString(R.string.radioInfo_unknown));

        network.setText(display);
    }

    private final void
    updateProperties() {
        String s;
        Resources r = getResources();

        s = phone.getDeviceId();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        mDeviceId.setText(s);


        s = phone.getLine1Number();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        number.setText(s);
    }

    private final void updateDataStats() {
        String s;

        s = SystemProperties.get("net.gsm.radio-reset", "0");
        resets.setText(s);

        s = SystemProperties.get("net.gsm.attempt-gprs", "0");
        attempts.setText(s);

        s = SystemProperties.get("net.gsm.succeed-gprs", "0");
        successes.setText(s);

        //s = SystemProperties.get("net.gsm.disconnect", "0");
        //disconnects.setText(s);

        s = SystemProperties.get("net.ppp.reset-by-timeout", "0");
        sentSinceReceived.setText(s);
    }

    private final void updateDataStats2() {
        Resources r = getResources();

        try {
            long txPackets = netstat.getMobileTxPackets();
            long rxPackets = netstat.getMobileRxPackets();
            long txBytes   = netstat.getMobileTxBytes();
            long rxBytes   = netstat.getMobileRxBytes();

            String packets = r.getString(R.string.radioInfo_display_packets);
            String bytes   = r.getString(R.string.radioInfo_display_bytes);

            sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
            received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
        } catch (RemoteException e) {
        }
    }

    /**
     * Ping a IP address.
     */
    private final void pingIpAddr() {
        try {
            // This is hardcoded IP addr. This is for testing purposes.
            // We would need to get rid of this before release.
            String ipAddress = "74.125.47.104";
            Process p = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
            int status = p.waitFor();
            if (status == 0) {
                mPingIpAddrResult = "Pass";
            } else {
                mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e) {
            mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    /**
     *  Ping a host name
     */
    private final void pingHostname() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int status = p.waitFor();
            if (status == 0) {
                mPingHostnameResult = "Pass";
            } else {
                mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (UnknownHostException e) {
            mPingHostnameResult = "Fail: Unknown Host";
        } catch (IOException e) {
            mPingHostnameResult= "Fail: IOException";
        } catch (InterruptedException e) {
            mPingHostnameResult = "Fail: InterruptedException";
        }
    }

    /**
     * This function checks for basic functionality of HTTP Client.
     */
    private void httpClientTest() {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet("http://www.google.com");
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                mHttpClientTestResult = "Pass";
            } else {
                mHttpClientTestResult = "Fail: Code: " + String.valueOf(response);
            }
            request.abort();
        } catch (IOException e) {
            mHttpClientTestResult = "Fail: IOException";
        }
    }

    private void refreshSmsc() {
        phone.getSmscAddress(mHandler.obtainMessage(EVENT_QUERY_SMSC_DONE));
    }

    private final void updatePingState() {
        final Handler handler = new Handler();
        // Set all to unknown since the threads will take a few secs to update.
        mPingIpAddrResult = getResources().getString(R.string.radioInfo_unknown);
        mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);

        mPingIpAddr.setText(mPingIpAddrResult);
        mPingHostname.setText(mPingHostnameResult);
        mHttpClientTest.setText(mHttpClientTestResult);

        final Runnable updatePingResults = new Runnable() {
            public void run() {
                mPingIpAddr.setText(mPingIpAddrResult);
                mPingHostname.setText(mPingHostnameResult);
                mHttpClientTest.setText(mHttpClientTestResult);
            }
        };
        Thread ipAddr = new Thread() {
            @Override
            public void run() {
                pingIpAddr();
                handler.post(updatePingResults);
            }
        };
        ipAddr.start();

        Thread hostname = new Thread() {
            @Override
            public void run() {
                pingHostname();
                handler.post(updatePingResults);
            }
        };
        hostname.start();

        Thread httpClient = new Thread() {
            @Override
            public void run() {
                httpClientTest();
                handler.post(updatePingResults);
            }
        };
        httpClient.start();
    }

// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MPDP START
// LGE_WCDMA_FEATURE_MERGE  END
    private final void updatePdpList() {
        StringBuilder sb = new StringBuilder("========DATA=======\n");

// LGE_WCDMA_FEATURE_MERGE  START
        Map<Object, DataConnection> dcs = phone.getCurrentDataConnectionList();
// LGE_WCDMA_FEATURE_MERGE  END

// LGE_WCDMA_FEATURE_MERGE  START
        for (Map.Entry<Object,DataConnection> entry : dcs.entrySet()) {
            Object token = entry.getKey();
            DataConnection dc = entry.getValue();
            
            sb.append("    State: ").append(dc.getState().toString()).append("\n");
            if (dc.getState().isActive()) {
// LGE_WCDMA_FEATURE_MERGE  END
                long timeElapsed =
                    (System.currentTimeMillis() - dc.getConnectionTime())/1000;
                sb.append("    connected at ")
                  .append(DateUtils.timeString(dc.getConnectionTime()))
                  .append(" and elapsed ")
                  .append(DateUtils.formatElapsedTime(timeElapsed));

// LGE_WCDMA_FEATURE_MERGE  START
                if (dc instanceof PdpConnection) {
                    PdpConnection pdp = (PdpConnection)dc;
// LGE_WCDMA_FEATURE_MERGE  END
                    sb.append("\n    to ")
                      .append(pdp.getApn().toString());
                }
// LGE_WCDMA_FEATURE_MERGE  START
                sb.append("\nfeature id: ")
                  .append(phone.getFeatureIds(token))
                  .append("\ninterface: ")
                  .append(phone.getInterfaceName(token))
// LGE_WCDMA_FEATURE_MERGE  END
                  .append("\naddress: ")
// LGE_WCDMA_FEATURE_MERGE  START
                  .append(phone.getIpAddress(token))
// LGE_WCDMA_FEATURE_MERGE  END
                  .append("\ngateway: ")
// LGE_WCDMA_FEATURE_MERGE  START
                  .append(phone.getGateway(token));
                String[] dns = phone.getDnsServers(token);
// LGE_WCDMA_FEATURE_MERGE  END
                if (dns != null) {
                    sb.append("\ndns: ").append(dns[0]).append(", ").append(dns[1]);
                }
// LGE_WCDMA_FEATURE_MERGE  START
            } else if (dc.getState().isInactive()) {
// LGE_WCDMA_FEATURE_MERGE  END
                sb.append("    disconnected with last try at ")
                  .append(DateUtils.timeString(dc.getLastFailTime()))
                  .append("\n    fail because ")
                  .append(dc.getLastFailCause().toString());
            } else {
// LGE_WCDMA_FEATURE_MERGE  START
                if (dc instanceof PdpConnection) {
                    PdpConnection pdp = (PdpConnection)dc;
// LGE_WCDMA_FEATURE_MERGE  END
                    sb.append("    is connecting to ")
                      .append(pdp.getApn().toString());
                } else {
                    sb.append("    is connecting");
                }
            }
            sb.append("\n===================");
        }


        disconnects.setText(sb.toString());
    }
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MODO END
// LGE_WCDMA_FEATURE_MERGE  END

    private MenuItem.OnMenuItemClickListener mViewADNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.SimContacts");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mViewFDNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.FdnList");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mViewSDNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse("content://icc/sdn"));
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.ADNList");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mGetPdpList = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            phone.getDataCallList(null);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mSelectBandCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent();
            intent.setClass(RadioInfo.this, BandMode.class);
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mToggleData = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            int state = mTelephonyManager.getDataState();
            switch (state) {
                case TelephonyManager.DATA_CONNECTED:
                    phone.disableDataConnectivity();
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    phone.enableDataConnectivity();
                    break;
                default:
                    // do nothing
                    break;
            }
            return true;
        }
    };

    OnClickListener mPowerButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            //log("toggle radio power: currently " + (isRadioOn()?"on":"off"));
            phone.setRadioPower(!isRadioOn());
        }
    };

// LGE_WCDMA_FEATURE_MERGE  START
// LGE_GMM_ATTACH_MODE START
    OnClickListener mGmmModeListener = new OnClickListener() {
        public void onClick(View v) {
            boolean isChecked = gmmModeCheckbox.isChecked();
            Log.d(TAG, "[RadioInfo] GMM: clicked: " + isChecked);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.GMM_ATTACH_MODE, isChecked ? 1 : 0);
        }
    };
// LGE_GMM_ATTACH_MODE END
    
// LGE_WCDMA_FEATURE_MERGE  END
    OnClickListener mDnsCheckButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            phone.disableDnsCheck(!phone.isDnsCheckDisabled());
            updateDnsCheckState();
        }
    };

    OnClickListener mOemInfoButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent("com.android.settings.OEM_RADIO_INFO");
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Log.d(TAG, "OEM-specific Info/Settings Activity Not Found : " + ex);
                // If the activity does not exist, there are no OEM
                // settings, and so we can just do nothing...
            }
        }
    };

    OnClickListener mPingButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updatePingState();
        }
    };

    OnClickListener mUpdateSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updateSmscButton.setEnabled(false);
            phone.setSmscAddress(smsc.getText().toString(),
                    mHandler.obtainMessage(EVENT_UPDATE_SMSC_DONE));
        }
    };

    OnClickListener mRefreshSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            refreshSmsc();
        }
    };

    AdapterView.OnItemSelectedListener
            mPreferredNetworkHandler = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            Message msg = mHandler.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE);
            if (pos>=0 && pos<=7) { //IS THIS NEEDED to extend to the entire range of values
                phone.setPreferredNetworkType(pos, msg);
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };

    private String[] mPreferredNetworkLabels = {
            "WCDMA preferred",
            "GSM only",
            "WCDMA only",
            "GSM auto (PRL)",
            "CDMA auto (PRL)",
            "CDMA only",
            "EvDo only",
            "GSM/CDMA auto (PRL)",
            "Unknown"};
// LGE_WCDMA_FEATURE_MERGE  START
// LGE_MPDP_CHECK START
    private void updateMpdpState(int mpdp) {
        Log.d(TAG, "[RadioInfo] MPDP: set state to " + mpdp);
        switch(mpdp) {
            case MPDP_DISABLED:
                mpdpStateLabel.setText(R.string.mpdp_disabled);
                break;
            case MPDP_ENABLED_PRIMARY:
                mpdpStateLabel.setText(R.string.mpdp_enabled_primary);
                break;
            case MPDP_ENABLED_FULL:
                mpdpStateLabel.setText(R.string.mpdp_enabled_full);
                break;
            default:
                mpdpStateLabel.setText("Error! Unexpected value("+mpdp+")");
                break;
        }
    }

    private int loadMpdpState(boolean putDefault) {
        int mpdp = -1;
        try{
            mpdp = Settings.Secure.getInt(getContentResolver(), Settings.Secure.MPDP_CHECK);
            Log.d(TAG, "[RadioInfo] MPDP: get value from settings: " + mpdp);
        } catch(SettingNotFoundException ex) {
            // shouldn't happen because GsmDataConnectionTracker will set it on RADIO_AVAILABLE event
            if(putDefault) {
                Log.d(TAG, "[RadioInfo] MPDP: put value after exception");
                mpdp = MPDP_ENABLED_FULL;
                Settings.Secure.putInt(getContentResolver(), Settings.Secure.MPDP_CHECK, mpdp);
            }
        }

        return mpdp;
    }

    OnClickListener mMPDPCheckListener = new OnClickListener() {
        public void onClick(View v) {
            int mpdp = loadMpdpState(true);

            switch(mpdp) {
                case MPDP_DISABLED:
                    mpdp = MPDP_ENABLED_PRIMARY;
                    break;
                case MPDP_ENABLED_PRIMARY:
                    mpdp = MPDP_ENABLED_FULL;
                    break;
                case MPDP_ENABLED_FULL:
                    mpdp = MPDP_DISABLED;
                    break;
                default:
                    mpdp = MPDP_ENABLED_FULL;
                    break;
            }
            
            Log.d(TAG, "[RadioInfo] MPDP: put value to settings: " + mpdp);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.MPDP_CHECK, mpdp);
            
            mpdp = loadMpdpState(false);
            updateMpdpState(mpdp);
            Toast.makeText(RadioInfo.this, R.string.mpdp_restart, Toast.LENGTH_SHORT).show();
        }
    };
// LGE_MPDP_CHECK END
// LGE_WCDMA_FEATURE_MERGE  END
}