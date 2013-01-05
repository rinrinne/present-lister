package jp.glassmoon.mbga.imascg.presentlister;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import jp.glassmoon.mbga.imascg.presentlister.loader.ImportLoader;
import jp.glassmoon.mbga.imascg.presentlister.loader.ListLoader;
import jp.glassmoon.mbga.imascg.presentlister.loader.SearchLoader;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
//import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CardListActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
	private static final int DIALOG_CONFIRM_IMPORT = 1;
	
	private static final int LOADER_IMPORT = 1;
	private static final int LOADER_LIST = 2;
	private static final int LOADER_SEARCH = 3;
	
	private static final int INTENT_IMPORT = 1;

	public static final String KEY_SEARCH_TEXT = "searchtext";
	public static final String KEY_IMPORTED_DATE = "importeddate";

	private class CardAdapter extends ResourceCursorAdapter {
		
		public CardAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c, ResourceCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void bindView(View view, Context context, Cursor c) {
			// TODO Auto-generated method stub
			TextView nameView = (TextView) view.findViewById(R.id.list_name);
			TextView countView = (TextView) view.findViewById(R.id.list_count);
			
			nameView.setText(c.getString(c.getColumnIndex(CardStorage.COLUMN_COUNT_NAME)));
			countView.setText(c.getString(c.getColumnIndex(CardStorage.COLUMN_COUNT_SUM)));
		}

	}
	
	private CardStorage mStorage;
	private Date mImportedDate;
	private CardAdapter mAdapter;
	private boolean mSearchMode;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // custom title bar
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_card_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.list_titlebar);

        // Preferences
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        long importTime = pref.getLong(KEY_IMPORTED_DATE, 0L);

        mImportedDate = null;
        mSearchMode = false;

        if (importTime > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(importTime);
            mImportedDate = cal.getTime();
        	Log.d("TIME", "onCreate: " + mImportedDate.toString());
            
            TextView textView = (TextView) findViewById(R.id.date_imported);
            textView.setText(new SimpleDateFormat(
            		getString(R.string.imported_date_format)).format(mImportedDate));
        	Log.d("TIME", "onCreate leave: " + mImportedDate.toString());
        }
        
        // Search button
        Button btnSearch = (Button) findViewById(R.id.button_search);
        btnSearch.setOnClickListener(this);       

    	// List adapter
    	ListView listView = (ListView) findViewById(R.id.listCardView);
        
        mStorage = CardStorage.get(CardListActivity.this);
        mAdapter = new CardAdapter(CardListActivity.this, R.layout.item_card, null);
        
        listView.setAdapter(mAdapter);
        
        LoaderManager.enableDebugLogging(true);
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_IMPORT, null, this);
        lm.initLoader(LOADER_SEARCH, null, this);
        lm.initLoader(LOADER_LIST, null, this).forceLoad();
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		// clear suggestions
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (pref.getBoolean(SettingsActivity.KEY_PREF_CLEAR_SUGGESTIONS, false)) {
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
			suggestions.clearHistory();
		}
		
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        if (mImportedDate != null) {
        	Log.d("TIME", "onDestroy: " + mImportedDate.toString());
        	editor.putLong(KEY_IMPORTED_DATE, mImportedDate.getTime());
        	editor.commit();
        }
	}
    
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.button_search:
			if (mStorage.getCountDataSize() > 0)
				onSearchRequested();
			break;
		}
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (mSearchMode) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				Log.d("BACK", "BACKKEY in searchmode");
				mSearchMode = false;
				getSupportLoaderManager().restartLoader(LOADER_LIST, null, CardListActivity.this).forceLoad();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_card_list, menu);
        return true;
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.menu_import:
			Log.v("M1", "IMPORT");
			startActivityForResult(new Intent(CardListActivity.this, ImportActivity.class), INTENT_IMPORT);
			break;
		case R.id.menu_export:
			Log.v("M1", "EXPORT");
			if (mStorage.getDataSize() == 0) {
				Toast.makeText(CardListActivity.this, R.string.toast_nodata, Toast.LENGTH_SHORT).show();
				break;
			}
			Intent intent = new Intent(CardListActivity.this, ExportActivity.class);
			intent.putExtra(KEY_IMPORTED_DATE, mImportedDate);
			startActivity(intent);
			break;
		case R.id.menu_settings:
			Log.v("M1", "SETTINGS");
			startActivity(new Intent(CardListActivity.this, SettingsActivity.class));
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		Dialog dialog = super.onCreateDialog(id);
		switch (id) {
		case DIALOG_CONFIRM_IMPORT:
			dialog = createImportDialog();
			break;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case DIALOG_CONFIRM_IMPORT:
			prepareImportDialog((AlertDialog) dialog);
			break;
		}
	}
	
	private Dialog createImportDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(CardListActivity.this);
		builder.setMessage(R.string.import_description);
		builder.setPositiveButton(R.string.import_yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				getSupportLoaderManager().initLoader(LOADER_IMPORT, null, CardListActivity.this).forceLoad();
			}
		});
		builder.setNegativeButton(R.string.import_no, null);
		
		return builder.create();
	}
	
	private void prepareImportDialog(AlertDialog dialog) {
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case INTENT_IMPORT:
			CardStorage.Editor edit = mStorage.edit();
			if (edit.getDataCount() > 0) {
				showDialog(DIALOG_CONFIRM_IMPORT);
			}
			break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int type, Bundle bundle) {
		// TODO Auto-generated method stub
		Log.d("LOADER", "Create Loader");
		Loader<Cursor> loader = null;
		switch (type) {
		case LOADER_IMPORT:
			loader = new ImportLoader(CardListActivity.this);
			break;

		case LOADER_LIST:
			loader = new ListLoader(CardListActivity.this);
			break;
		case LOADER_SEARCH:
			loader = new SearchLoader(CardListActivity.this, bundle);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		// TODO Auto-generated method stub
		Log.d("LOADER", "onFinish");
		
		if (c == null)
			return;
		
		if (loader instanceof ImportLoader) {
			mAdapter.swapCursor(c);
			mAdapter.notifyDataSetChanged();
			
			//update total indicator
			int sizeData = mStorage.getDataSize();
			int sizeCount = mStorage.getCountDataSize();
			TextView indTotal = (TextView) findViewById(R.id.textTotalInd);
			indTotal.setText(String.format(getString(R.string.indicate_total_format), sizeCount, sizeData));
			
			// Update ImportedDate
            mImportedDate = Calendar.getInstance().getTime();
            
            TextView textView = (TextView) findViewById(R.id.date_imported);
            textView.setText(new SimpleDateFormat(
            		getString(R.string.imported_date_format)).format(mImportedDate));
            
		}
		else if (loader instanceof ListLoader) {
			mAdapter.swapCursor(c);
			mAdapter.notifyDataSetChanged();
			
			//update total indicator
			int sizeData = mStorage.getDataSize();
			int sizeCount = mStorage.getCountDataSize();
			TextView indTotal = (TextView) findViewById(R.id.textTotalInd);
			indTotal.setText(String.format(getString(R.string.indicate_total_format), sizeCount, sizeData));
		}
		else if (loader instanceof SearchLoader) {
			mAdapter.swapCursor(c);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub
		if (loader instanceof ImportLoader) {
			mAdapter.swapCursor(null);
		}
		else if (loader instanceof ListLoader) {
			mAdapter.swapCursor(null);
		}
		else if (loader instanceof SearchLoader) {
			mAdapter.swapCursor(null);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			Bundle bundle = new Bundle();
			String query = intent.getStringExtra(SearchManager.QUERY);
			
			// Save query to suggestion
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
			suggestions.saveRecentQuery(query, null);
			
			bundle.putString(KEY_SEARCH_TEXT, query);
			
			getSupportLoaderManager().restartLoader(LOADER_SEARCH, bundle, CardListActivity.this).forceLoad();
			
			TextView indTotal = (TextView) findViewById(R.id.textTotalInd);
			indTotal.setText(String.format(getString(R.string.indicate_total_search_format), query));
			mSearchMode = true;
		}
	}
}
