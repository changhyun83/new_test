// LGE_WCDMA_FEATURE_MERGE  START
//LGE_ENGINEERING_INFO START

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.CGEDParser;
import com.android.settings.R;

/**
 * Display the following information

 */
public class Dump extends Activity implements OnClickListener {

    private static final String LOG_TAG = "GSM";

    private static final int EVENT_UPDATE_STATS = 500;
    private static final int EVENT_UPDATE = 501;

    private Phone mPhone = null;
    private CommandsInterface cm;
    private static String sUnknown;
    private static TextView mGsmModule;
    private static TextView mUmtsModule;
    private static TextView mMmModule;
    private Button start,stop;
    private static boolean started = false;
    private Message result;
    
    private CGEDParser cged;
    
    private int mFlagModule = 0;
    private final int gsmFlag = 1;
    private final int umtsFlag = 2;
    private final int mmFlag = 3;

    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {

                case EVENT_UPDATE_STATS:
                   ar = (AsyncResult) msg.obj;
                   cged = (CGEDParser)ar.result;
                   Log.d(LOG_TAG, "cged.mCurrentLog = " + cged.mCurrentLog);
                   switch (cged.mCurrentLog) {
                       case 1:  {// for UMTS log
                           mUmtsModule.setVisibility(View.VISIBLE);
                           mMmModule.setVisibility(View.VISIBLE);
                           break;
                       }
                       case 2:  {// for GSM log
                           mGsmModule.setVisibility(View.VISIBLE);
                           mMmModule.setVisibility(View.VISIBLE);
                           break;
                       }
                   }
                   break;
                case EVENT_UPDATE:
                   ar = (AsyncResult) msg.obj;
                   if (cged!=null) {
                   //Text.setText(cged.toString());
                       switch (mFlagModule) {
                       case gsmFlag:  {// for GSM log
                           createGsmLog();
                           break;
                       }
                       case umtsFlag:  {// for UMTS log
                           createUmtsLog();
                           break;
                       }
                       case mmFlag:  {// for MM log
                           createMmLog();
                           break;
                       }
                   }
                   }
                   sendEmptyMessageDelayed(EVENT_UPDATE, 3000);
                   break;
            }
        }
    };

  
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPhone = PhoneFactory.getDefaultPhone();
        setContentView(R.xml.dump);
        
        mGsmModule = (TextView)findViewById(R.id.gsmModule);
        mGsmModule.setText("AS Info");
        mGsmModule.setOnClickListener(this);
        mGsmModule.setVisibility(View.GONE);
        
        mUmtsModule = (TextView)findViewById(R.id.umtsModule);
        mUmtsModule.setText("AS Info");
        mUmtsModule.setOnClickListener(this);
        mUmtsModule.setVisibility(View.GONE);
        
        mMmModule = (TextView)findViewById(R.id.mmModule);
        mMmModule.setText("MM Module");
        mMmModule.setOnClickListener(this);
        mMmModule.setVisibility(View.GONE);
        
        start = (Button) findViewById(R.id.btn1);
        start.setText("START");
        start.setOnClickListener(this);

        stop = (Button) findViewById(R.id.btn2);
        stop.setText("STOP");
        stop.setOnClickListener(this);
    }
    
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1: {
                if (!started) {
                    mPhone.queryGprsCellEnvironmentDescription(1, result); 
                    mPhone.registerGprsCellInfo(mHandler,EVENT_UPDATE_STATS, null);
                    mHandler.sendEmptyMessageDelayed(EVENT_UPDATE, 3000);
                    started = true;
                }
                break;
            }
            case R.id.btn2: {
                if (started) {
                    mPhone.queryGprsCellEnvironmentDescription(2, result); 
                    mPhone.unregisterGprsCellInfo(mHandler);
                    mHandler.removeMessages(EVENT_UPDATE);
                    started = false;
                  }
                break;
            }
            case R.id.gsmModule: {
                mFlagModule = gsmFlag;
                Log.d(LOG_TAG, "gsmModule is started");
                setContentView(R.xml.view_log);
                
                start = (Button) findViewById(R.id.btn1);
                start.setText("START");
                start.setOnClickListener(this);

                stop = (Button) findViewById(R.id.btn2);
                stop.setText("STOP");
                stop.setOnClickListener(this);
                
                Log.d(LOG_TAG, "gsmModule - end");
                break;
            }
            case R.id.umtsModule: {
                mFlagModule = umtsFlag;
                Log.d(LOG_TAG, "umtsModule is started");
                setContentView(R.xml.view_log_umts);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                
                start = (Button) findViewById(R.id.btn1);
                start.setText("START");
                start.setOnClickListener(this);

                stop = (Button) findViewById(R.id.btn2);
                stop.setText("STOP");
                stop.setOnClickListener(this);
                
                Log.d(LOG_TAG, "umtsModule - end");
                break;
            }
            
            case R.id.mmModule: {
                mFlagModule = mmFlag;
                Log.d(LOG_TAG, "mmModule is started");
                setContentView(R.xml.view_log_mm);
                
                start = (Button) findViewById(R.id.btn1);
                start.setText("START");
                start.setOnClickListener(this);

                stop = (Button) findViewById(R.id.btn2);
                stop.setText("STOP");
                stop.setOnClickListener(this);
                
                Log.d(LOG_TAG, "mmModule - end");
                break;
            }
        }
    }
    
    private void createGsmLog() {
        String text = "---";
        TextView mccText;
        TextView mncText;
        TextView lacText;
        TextView racText;
        TextView actText;
        TextView ciText;
        TextView bText;
        TextView arfcnText;
        TextView rssiText;
        TextView c1Text;
        TextView bsicText;
        
        mccText = (TextView) findViewById(R.id.mcc_value);
        String mcc = cged.getServingPlmnMCC();
        if (mcc != null) {
            mccText.setText(mcc);
        } else {
            mccText.setText(text);
        }
        
        mncText = (TextView) findViewById(R.id.mnc_value);
        String mnc = cged.getServingPlmnMNC();
        if (mnc != null) {
            mncText.setText(mnc);
        } else {
            mncText.setText(text);
        }
        
        lacText = (TextView) findViewById(R.id.lac_value);
        String lac = cged.getServingPlmnLAC();
        if (lac != null) {
            lacText.setText(lac);
        } else {
            lacText.setText(text);
        }
        
        racText = (TextView) findViewById(R.id.rac_value);
        String rac = cged.getServingPlmnRAC();
        if (rac != null) {
            racText.setText(rac);
        } else {
            racText.setText(text);
        }
        
        actText = (TextView) findViewById(R.id.act_value);
        String act = cged.getServingPlmnAcT();
        if (act != null) {
            actText.setText(act);
        } else {
            actText.setText(text);
        }
        
        ciText = (TextView) findViewById(R.id.ci_value);
        String ci = cged.getCgedGsmCi(0);
        if (ci != null) {
            ciText.setText(ci);
        } else {
            ciText.setText(text);
        }
        
        bText = (TextView) findViewById(R.id.b_value);
        String b = cged.getCgedGsmB(0);
        if (b != null) {
            bText.setText(b);
        } else {
            bText.setText(text);
        }
        
        arfcnText = (TextView) findViewById(R.id.arfcn_value);
        String arfcn = cged.getCgedGsmArfcn(0);
        if (arfcn != null) {
            arfcnText.setText(arfcn);
        } else {
            arfcnText.setText(text);
        }
        
        rssiText = (TextView) findViewById(R.id.rssi_value);
        String rssi = cged.getCgedGsmRssi(0);
        if (rssi != null) {
            rssiText.setText(rssi);
        } else {
            rssiText.setText(text);
        }
        
        c1Text = (TextView) findViewById(R.id.c1_value);
        String c1 = cged.getCgedGsmC1(0);
        if (c1 != null) {
            c1Text.setText(c1);
        } else {
            c1Text.setText(text);
        }
        
        bsicText = (TextView) findViewById(R.id.bsic_value);
        String bsic = cged.getCgedGsmBsic(0);
        if (bsic != null) {
            bsicText.setText(bsic);
        } else {
            bsicText.setText(text);
        }
    }
    
    private void createUmtsLog() {
        String text = "-";
        TextView mccText;
        TextView mncText;
        TextView lacText;
        TextView racText;
        
        TextView cellText;
        TextView scText;
        TextView rscpText;
        TextView ecn0Text;
        TextView dlfText;
        TextView rvText;
        
        mccText = (TextView) findViewById(R.id.mcc_value);
        String mcc = cged.getServingPlmnMCC();
        if (mcc != null) {
            mccText.setText(mcc);
        } else {
            mccText.setText(text);
        }
        
        mncText = (TextView) findViewById(R.id.mnc_value);
        String mnc = cged.getServingPlmnMNC();
        if (mnc != null) {
            mncText.setText(mnc);
        } else {
            mncText.setText(text);
        }
        
        lacText = (TextView) findViewById(R.id.lac_value);
        String lac = cged.getServingPlmnLAC();
        if (lac != null) {
            lacText.setText(lac);
        } else {
            lacText.setText(text);
        }
        
        racText = (TextView) findViewById(R.id.rac_value);
        String rac = cged.getServingPlmnRAC();
        if (rac != null) {
            racText.setText(rac);
        } else {
            racText.setText(text);
        }
        
        // Cell parameters //
        cellText = (TextView) findViewById(R.id.cell_value);
        String cell = cged.getCellAsName(0);
        if (cell != null) {
            cellText.setText(cell);
        } else {
            cellText.setText(text);
        }
        
        scText = (TextView) findViewById(R.id.sc_value);
        String sc = cged.getCellAsSc(0);
        if (sc != null) {
            scText.setText(sc);
        } else {
            scText.setText(text);
        }
        
        rscpText = (TextView) findViewById(R.id.rscp_value);
        String rscp = cged.getCellAsRscp(0);
        if (rscp != null) {
            rscpText.setText(rscp);
        } else {
            rscpText.setText(text);
        }
        
        ecn0Text = (TextView) findViewById(R.id.ecno_value);
        String ecno = cged.getCellAsEcn0(0);
        if (ecno != null) {
            ecn0Text.setText(ecno);
        } else {
            ecn0Text.setText(text);
        }
        
        dlfText = (TextView) findViewById(R.id.dlf_value);
        String dlf = cged.getCellAsDlf(0);
        if (dlf != null) {
            dlfText.setText(dlf);
        } else {
            dlfText.setText(text);
        }
        
        rvText = (TextView) findViewById(R.id.rv_value);
        String rv = cged.getCellAsRv(0);
        if (rv != null) {
            rvText.setText(rv);
        } else {
            rvText.setText(text);
        }
    }
    
    private void createMmLog() {
        String text = "-";
        TextView processText;
        TextView mmsText;
        TextView mmssText;
        TextView mscText;
        TextView tText;
        
        processText = (TextView) findViewById(R.id.process_value);
        String process = cged.getProcessCOName(0);
        if (process != null) {
            processText.setText(process);
        } else {
            processText.setText(text);
        }
        
        mmsText = (TextView) findViewById(R.id.mms_value);
        String mms = cged.getProcessCOMms(0);
        if (mms != null) {
            mmsText.setText(mms);
        } else {
            mmsText.setText(text);
        }
        
        mmssText = (TextView) findViewById(R.id.mmss_value);
        String mmss = cged.getProcessCOMmss(0);
        if (mmss != null) {
            mmssText.setText(mmss);
        } else {
            mmssText.setText(text);
        }
        
        mscText = (TextView) findViewById(R.id.msc_value);
        String msc = cged.getProcessCOMsc(0);
        if (msc != null) {
            mscText.setText(msc);
        } else {
            mscText.setText(text);
        }
        
        tText = (TextView) findViewById(R.id.t_value);
        String t = cged.getProcessCOT(0);
        if (t != null) {
            tText.setText(t);
        } else {
            tText.setText(text);
        }
    }
    

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d(LOG_TAG, "KEYCODE_BACK");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                
                mPhone.queryGprsCellEnvironmentDescription(2, result); 
                mPhone.unregisterGprsCellInfo(mHandler);
                mHandler.removeMessages(EVENT_UPDATE);
                mHandler.removeMessages(EVENT_UPDATE_STATS);
                started = false;
                
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    } 
    
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
   	}

    @Override
    protected void onResume() {
        super.onResume();
        
        /*mPhone.queryGprsCellEnvironmentDescription(2, result); 
        mPhone.unregisterGprsCellInfo(mHandler);
        mHandler.removeMessages(EVENT_UPDATE);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
        started = false;*/
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
}
//LGE_ENGINEERING_INFO END
// LGE_WCDMA_FEATURE_MERGE  END
