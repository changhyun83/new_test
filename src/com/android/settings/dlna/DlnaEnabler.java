package com.android.settings.dlna;

import com.lge.dlnaserver.IDlnaEnabler;
import com.lge.dlnaserver.IDlnaEnablerListener;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.android.settings.R;

public class DlnaEnabler implements Preference.OnPreferenceChangeListener 
{
	private static final String TAG = "DlnaEnabler";

	private final Context mContext;
	
	private CheckBoxPreference mDlnaEnabled;
	private WifiManager mWifiManager;
	
	private IDlnaEnabler mBindService;
	
	private Handler mHandler = new Handler();
	
	public static final int DLNA_STATE_DISABLED = 0;
	public static final int DLNA_STATE_DISABLING = 1;
	public static final int DLNA_STATE_ENABLED = 2;
	public static final int DLNA_STATE_ENABLING = 3;
	public static final int DLNA_STATE_UNKNOWN = 4;

	//============================
    // dlna service callbacks
    //============================
    private IDlnaEnablerListener mListener = new IDlnaEnablerListener.Stub() {
		public void startShareComplete(int err) throws RemoteException {
			mHandler.post(new Runnable(){
				public void run() {
					mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_on));
					mDlnaEnabled.setEnabled(true);
					mDlnaEnabled.setChecked(true);
				}
			});
		}

		public void stopShareComplete(int err) throws RemoteException {
			mHandler.post(new Runnable(){
				public void run() {
					mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_off));
					mDlnaEnabled.setEnabled(true);
					mDlnaEnabled.setChecked(false);
				}
			});
		}

		public void statusUpdate() throws RemoteException {
			mHandler.post(new Runnable(){
				public void run() {
					setStatusSync();
				}
			});
		}
	};
	
	public DlnaEnabler(Context context, CheckBoxPreference dlnaEnabled, WifiManager wifiManager) {
	    mContext = context;
	    mDlnaEnabled = dlnaEnabled;
	    mWifiManager = wifiManager;
	    
	    bindDlnaServcie();
	}
	
	private void bindDlnaServcie()
	{
		Intent intent = new Intent("com.lge.dlna.DlnaService");
		mContext.startService(intent);
		
		if(mBindService == null)
		{
			Intent i = new Intent("com.lge.dlna.DlnaService");
			i.putExtra("isEnabler", true);
			boolean result = mContext.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	public void unbindDlnaService()
	{	
		if(mBindService != null)
		{
			try {
				if(mBindService != null)
					mBindService.unregisterListener(null);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			mBindService = null;
			
			mContext.unbindService(mConnection);
		}
	}
    
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBindService = IDlnaEnabler.Stub.asInterface(service);
			
			try {
				if(mBindService != null)
					mBindService.registerListener(mListener);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mHandler.post(new Runnable(){
				public void run() {
					setStatusSync();
				}
			});
		}

		public void onServiceDisconnected(ComponentName name) {
			try {
				if(mBindService != null)
					mBindService.unregisterListener(mListener);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mBindService = null;
		}
	};
	
	public void setStatusSync()
	{
		if(mBindService == null)
			return;
		
		int status = -1;
		try {
			status = mBindService.getState();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		switch (status) {
		case DLNA_STATE_DISABLED:
			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_off));
			mDlnaEnabled.setChecked(false);
			mDlnaEnabled.setEnabled(true);
			break;
		case DLNA_STATE_DISABLING:
			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_off_ing));
			mDlnaEnabled.setChecked(true);
			mDlnaEnabled.setEnabled(false);
			break;
		case DLNA_STATE_ENABLED:
			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_on));
			mDlnaEnabled.setChecked(true);
			mDlnaEnabled.setEnabled(true);
			break;
		case DLNA_STATE_ENABLING:
			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_on_ing));
			mDlnaEnabled.setChecked(false);
			mDlnaEnabled.setEnabled(false);
			break;
		case DLNA_STATE_UNKNOWN:
		default:
			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_off));
			mDlnaEnabled.setChecked(false);
			mDlnaEnabled.setEnabled(false);
			break;
		}
	}

	public void resume() {
		mDlnaEnabled.setOnPreferenceChangeListener(this);
		try {
			if(mBindService != null)
				mBindService.registerListener(mListener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setStatusSync();
	}

	public void pause() {
		mDlnaEnabled.setOnPreferenceChangeListener(null);
		try {
			if(mBindService != null)
				mBindService.unregisterListener(mListener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean onPreferenceChange(Preference preference, Object value) {
		if (preference == mDlnaEnabled) {
			setDlnaEnabled((Boolean) value);
        }
		
		// Don't update UI to opposite state until we're sure
		return false;
	}

	private void setDlnaEnabled(final boolean enable) {
	
		if(enable && !isWifiConnected()){
			createWifiConnectPopup();
			return;
		}
		
		if(mBindService != null)
    	{
    		mDlnaEnabled.setEnabled(false);
    		try {
	    		if(enable)
	    		{
	    			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_on_ing));
	    			mBindService.startShare();
	    		}else{
	    			mDlnaEnabled.setSummary(mContext.getString(R.string.dlna_summary_off_ing));
	    			mBindService.stopShare();
	    		}
    		} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public boolean isWifiConnected()
	{
		if(mWifiManager == null)
			return false;
		
		int ipAddress = 0;
		if(mWifiManager.isWifiEnabled())
		{
			WifiInfo xWifiInfo = mWifiManager.getConnectionInfo();
			if(xWifiInfo != null)
				ipAddress = xWifiInfo.getIpAddress();
		}
		
		if(ipAddress == 0)
			return false;
		else
			return true;
	}
	
	private void createWifiConnectPopup(){
		AlertDialog.Builder adb = new AlertDialog.Builder(mContext);
		adb.setCustomTitle(null);
		adb.setMessage(R.string.dlna_dlg_goto_wifi);
		
		adb.setPositiveButton(R.string.dlna_ok, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				mContext.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
			}
		});
		adb.setNegativeButton(R.string.dlna_cancel, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		adb.show();
	}
}