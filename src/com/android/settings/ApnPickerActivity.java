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

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.provider.Telephony.QoSProfileColumns;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ListAdapter;
import com.android.internal.telephony.TelephonyProperties;
import java.util.HashMap;
import java.util.Map;

public class ApnPickerActivity extends ListActivity {

    static final String TAG = "ApnPickerActivity";
    private static final Uri mUri = Telephony.Carriers.CONTENT_URI;
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    public static final String EXTRA_INTENT_ID = "ID";
    public static final String EXTRA_INTENT_NAME = "Name";
    Map<Integer, Integer> positionToIdMap;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getListView().setItemsCanFocus(true);
        positionToIdMap = new HashMap<Integer, Integer>();
        fillList();   
    }
    
    private void fillList() {
         String where = "numeric=\""
            + android.os.SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "")
            + "\"";
         Cursor cursor = managedQuery(mUri, new String[] {
                "_id", "name"}, where, null, null);
        startManagingCursor(cursor);  
        int position = 0;
        if (cursor == null) {return;}
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            positionToIdMap.put(position++, cursor.getInt(ID_INDEX));
            cursor.moveToNext();
        }
        cursor.moveToFirst();
        ListAdapter adapter = new SimpleCursorAdapter(this, 
                                           android.R.layout.simple_list_item_1, 
                                           cursor,                                   
                                           new String[] {"name"}, 
                                           new int[]{android.R.id.text1});                                
        setListAdapter(adapter);
     }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        int index = positionToIdMap.get(position);
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, index);
        Cursor cursor = managedQuery(url, new String[] {
                "_id", "name"}, null, null, null);
		startManagingCursor(cursor); 
        cursor.moveToFirst();
        Intent data = new Intent();
        String name = cursor.getString(NAME_INDEX);
        int key = cursor.getInt(ID_INDEX);
        data.putExtra(EXTRA_INTENT_ID, cursor.getInt(ID_INDEX));    
        data.putExtra(EXTRA_INTENT_NAME, cursor.getString(NAME_INDEX));
        setResult(RESULT_OK, data);
        finish();
    }
}
// LGE_MPDP END
// LGE_WCDMA_FEATURE_MERGE  END
