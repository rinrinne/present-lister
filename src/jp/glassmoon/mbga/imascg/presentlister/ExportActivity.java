package jp.glassmoon.mbga.imascg.presentlister;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import jp.glassmoon.mbga.imascg.presentlister.loader.ExportLoader;
import jp.glassmoon.mbga.imascg.presentlister.loader.UploadLoader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
//import android.app.Activity;
import android.widget.Toast;

public class ExportActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Bundle>, OnClickListener, OnCheckedChangeListener {

	private static final int LOADER_EXPORT = 1;
	private static final int LOADER_UPLOAD = 2;
	
	private static final int DIALOG_FILE_EXIST = 1;
	private static final int DIALOG_MEDIA_NOREADY = 1;
	
	public static final String KEY_EXPORT_FILE = "exportfile";
	private static final String KEY_CHECK_DROPBOX = "dropbox";
	
	private File mExportDirectory;
	private String mExportFileName;
	private boolean mDoDropbox;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        
        Date importedDate = (Date) getIntent().getSerializableExtra(CardListActivity.KEY_IMPORTED_DATE);
        
        if (importedDate == null) {
        	importedDate = Calendar.getInstance().getTime();
        }
        
        String dateStr = new SimpleDateFormat(
        		getString(R.string.export_file_date_format))
        		.format(importedDate);
        
        mExportDirectory = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				getString(R.string.export_app_directory));
        
        mExportFileName = String.format(getString(R.string.export_file_name_format), dateStr);
        
        TextView text = (TextView) findViewById(R.id.export_file_name);
        text.setText(mExportFileName);
        
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        mDoDropbox = pref.getBoolean(KEY_CHECK_DROPBOX, false);
        
        CheckBox checkBox = (CheckBox) findViewById(R.id.perform_dropbox);
        checkBox.setChecked(mDoDropbox);
        checkBox.setOnCheckedChangeListener(this);
        
        Button btn = (Button) findViewById(R.id.button_export);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.button_external);
        btn.setOnClickListener(this);
        
        getSupportLoaderManager().initLoader(LOADER_EXPORT, null, this);
        getSupportLoaderManager().initLoader(LOADER_UPLOAD, null, this);
    }

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		// dropbox
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean dropboxAuth = sp.getString(SettingsActivity.DROPBOX_TOKEN_KEY, null) == null ? false : true;
		
		CheckBox checkBox = (CheckBox) findViewById(R.id.perform_dropbox);
		checkBox.setEnabled(dropboxAuth);
		
		// external
		Button btn = (Button) findViewById(R.id.button_external);
		if (new File(mExportDirectory, mExportFileName).isFile()) {
			btn.setEnabled(true);
		}
		else {
			btn.setEnabled(false);
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putBoolean(KEY_CHECK_DROPBOX, mDoDropbox);
		editor.commit();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.button_export:
			// Check External Storage
			String state = Environment.getExternalStorageState();

			if (!Environment.MEDIA_MOUNTED.equals(state)) {
				showDialog(DIALOG_MEDIA_NOREADY);
				return;
			}
			
			mExportDirectory.mkdirs();
			File exportFile = new File(mExportDirectory, mExportFileName);

			Bundle bundle = new Bundle();
			bundle.putString(KEY_EXPORT_FILE, exportFile.getPath());
			
			getSupportLoaderManager().restartLoader(LOADER_EXPORT, bundle, this).forceLoad();
			break;
			
		case R.id.button_external:
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name) + " - " + mExportFileName);
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mExportDirectory, mExportFileName)));
			
			try {
				startActivity(Intent.createChooser(intent, getString(R.string.export_external_choose)));
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(this, getString(R.string.toast_external_failure), Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch (buttonView.getId()) {
		case R.id.perform_dropbox:
			mDoDropbox = isChecked;
			break;
		}
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_FILE_EXIST:
			builder.setTitle(R.string.export_warning_title);
			builder.setMessage(R.string.export_warning_file_exist);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			dialog = builder.create();
			break;
		}
		
		return dialog;
	}

	@Override
	public Loader<Bundle> onCreateLoader(int type, Bundle bundle) {
		// TODO Auto-generated method stub
		Loader<Bundle> loader = null;
		switch (type) {
		case LOADER_EXPORT:
			Log.d("LOADER", "Create export loader");
			loader = new ExportLoader(ExportActivity.this, bundle);
			break;
		case LOADER_UPLOAD:
			Log.d("LOADER", "Create upload loader");
			loader = new UploadLoader(ExportActivity.this, bundle);
			break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Bundle> loader, Bundle bundle) {
		// TODO Auto-generated method stub
		switch (loader.getId()) {
		case LOADER_EXPORT:
			if (bundle != null) {
				if (!bundle.getBoolean(ExportLoader.RESULT)) {
					Toast.makeText(ExportActivity.this, bundle.getString(ExportLoader.CONTENT), Toast.LENGTH_LONG).show();
					return;
				}
				else {
					Button btn = (Button) findViewById(R.id.button_external);
					btn.setEnabled(true);
					
					CheckBox checkBox = (CheckBox) findViewById(R.id.perform_dropbox);
					if (checkBox.isEnabled() && mDoDropbox) {
						UploadToDropbox();
					}
					else {
						Toast.makeText(ExportActivity.this, getString(R.string.toast_export_finish), Toast.LENGTH_LONG).show();
					}
				}
			}
			break;

		case LOADER_UPLOAD:
			if (bundle.getBoolean(UploadLoader.RESULT)) {
				Toast.makeText(this, R.string.toast_upload_complete, Toast.LENGTH_LONG).show();
			}
			else {
				Toast.makeText(this, bundle.getInt(UploadLoader.RES_ID), Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Bundle> loader) {
		// TODO Auto-generated method stub
		if (loader instanceof ExportLoader) {
			
		}
	}
	
	private void UploadToDropbox() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		Bundle bundle = new Bundle();
		bundle.putString(UploadLoader.TOKEN_KEY, sp.getString(SettingsActivity.DROPBOX_TOKEN_KEY, ""));
		bundle.putString(UploadLoader.TOKEN_SECRET, sp.getString(SettingsActivity.DROPBOX_TOKEN_SECRET, ""));
		bundle.putString(UploadLoader.DIRECTORY, mExportDirectory.getPath());
		bundle.putString(UploadLoader.FILENAME, mExportFileName);
		
		getSupportLoaderManager().restartLoader(LOADER_UPLOAD, bundle, this).forceLoad();
		Toast.makeText(this, getString(R.string.toast_upload_start), Toast.LENGTH_SHORT).show();
	}
}
