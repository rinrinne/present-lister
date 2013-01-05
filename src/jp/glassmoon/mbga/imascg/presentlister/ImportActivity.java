package jp.glassmoon.mbga.imascg.presentlister;

import java.net.URI;
import java.util.List;
import java.util.Random;

import jp.glassmoon.mbga.imascg.presentlister.handler.JavaScriptHandler;
import jp.glassmoon.mbga.imascg.presentlister.task.PeriodicTaskListener;
import jp.glassmoon.mbga.imascg.presentlister.task.WebCrewerTask;


import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class ImportActivity extends Activity implements OnClickListener  {
	
	private static final int DIALOG_CONFIRM_RESUME = 1;
	private static final int DIALOG_PROGRESS = 2;
	
	private ProgressDialog mProgress;
	private WebView mWeb;
	private CardStorage.Editor mStorageEditor;
	private WebCrewerTask mWebTask;
	
	private String mPlatHost;
	private int mCount;
	private int mAuthType;
	
	private final JavaScriptHandler.CallbackListener JsListener = new JavaScriptHandler.CallbackListener() {
		
		@Override
		public void OnReceiveNextPage(String url) {
			// TODO Auto-generated method stub
			String requestUrl = null;
			if (url == null) {
				Log.v("OnReceiveNextPage", "*empty*");
				switch (mAuthType) {
				case CardStorage.CARD_TYPE_NORMAL:
					Log.v("NextURL", "Swith AuthType: Normal -> Auth");
					mCount = 0;
					mAuthType = CardStorage.CARD_TYPE_AUTH;
					requestUrl = createPresentUrl(mAuthType, mCount);
					break;

				case CardStorage.CARD_TYPE_AUTH:
					Log.v("NextURL", "Finish!");
					endListing(true);
					break;
				}
			}
			else {
				Log.d("OnReceiveNextPage", url);
				requestUrl = url;
			}
			mWebTask.setUrl(requestUrl);
		}
		
		@Override
		public void OnReceiveCards(Bundle bundle) {
			// TODO Auto-generated method stub
			String[] cards = bundle.getStringArray(JavaScriptHandler.CARD);
			if (cards != null) {
				int idx = 0;
				for (String card : cards) {
					Log.v("OnReceiveCards", card);
					mStorageEditor.insert(card, mAuthType, mCount + idx);
					idx++;
				}
				mStorageEditor.commit();
				mCount += cards.length;
				
				// Update progress
				publishProgress(mAuthType, mCount);
			}
			else {
				Log.d("LISTING", "No cards found.");
			}
			
			// Get next page
			mWeb.loadUrl(JavaScriptHandler.JS_GET_NEXTPAGE);
		}
	};
	
	private final PeriodicTaskListener webListener = new PeriodicTaskListener() {
		
		@Override
		public boolean OnPeriodic(Bundle bundle) {
			// TODO Auto-generated method stub
			String url = bundle.getString(WebCrewerTask.URL);
			Log.d("WebListener", "OnPeriodic: " + url);
			mWeb.loadUrl(url);
			return false;
		}
		
		@Override
		public void OnFinish() {
			// TODO Auto-generated method stub
			Log.d("WebListener", "OnFinish");
			mCount = 0;
			mAuthType = CardStorage.CARD_TYPE_NORMAL;

			int total = mStorageEditor.getDataCount();
			if (total > 0) {
				String msg = String.format(getString(R.string.toast_total), total);
				Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		public void OnCancel() {
			// TODO Auto-generated method stub
			Log.d("WebListener", "OnCancel");
			String msg = getString(R.string.toast_error_listing);
			Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_LONG).show();
		}
	};
	
	private class ImportClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView view, String url) {
			// TODO Auto-generated method stub
			super.onPageFinished(view, url);
			
			Log.d("onPageFinished", url);
			
			Button btn = (Button) findViewById(R.id.button_listing);
			if (url.startsWith(getString(R.string.url_imascg_root) + "/?guid=ON")) {
				btn.setEnabled(true);
				btn.setText(R.string.button_listing_start_enable);
				if (mPlatHost == null) {
					try {
						List<NameValuePair> queries = URLEncodedUtils.parse(new URI(url), "UTF-8");
						for (NameValuePair pair : queries) {
							if ("url".equals(pair.getName())) {
								URI innerUri = new URI(pair.getValue());
								mPlatHost = innerUri.getHost();
							}
						}
					}
					catch (Exception e) {
						Log.d("onPageFinished", e.getMessage());
					}
				}
			}
			else {
				btn.setEnabled(false);
				btn.setText(R.string.button_listing_start);
			}
			
			if (mWebTask.isRunning() && mWebTask.getUrl().equals(url)) {
				view.loadUrl(JavaScriptHandler.JS_GET_CARDS);
			}
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        
         // Initialize
        mPlatHost = null;
        mCount = 0;
        mAuthType = CardStorage.CARD_TYPE_NORMAL;
        
        mProgress = null;
        mStorageEditor = CardStorage.get(this).edit();
        mWebTask = new WebCrewerTask(webListener);
        
        // Button
        Button btn = (Button) findViewById(R.id.button_listing);
        btn.setEnabled(false);
        btn.setOnClickListener(this);
      
        //WebView
        mWeb = (WebView) findViewById(R.id.webView);
        WebSettings settings = mWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginsEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setLoadsImagesAutomatically(true);
 
        mWeb.setWebViewClient(new ImportClient());
        mWeb.addJavascriptInterface(new JavaScriptHandler(JsListener), JavaScriptHandler.INTERFACE);
      
        mWeb.loadUrl(getString(R.string.url_mbga_top));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_import, menu);
        return true;
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()) {
		case R.id.menu_reload:
			Log.v("PL", "RELOAD");
			mWeb.reload();
		case R.id.menu_top:
			Log.v("PL", "TOP");
			mWeb.loadUrl(getString(R.string.url_mbga_top));
			break;
		case R.id.menu_home:
			Log.v("PL", "HOME");
			mWeb.loadUrl(getString(R.string.url_imascg_root));
			break;
		case R.id.menu_logout:
			Log.v("PL", "LOGOUT");
			mPlatHost = null;
			mWeb.loadUrl(getString(R.string.url_mbga_logout));
			break;
		case R.id.menu_settings:
			Log.v("PL", "SETTINGS");
			startActivity(new Intent(ImportActivity.this, SettingsActivity.class));
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mProgress = null;
		mWebTask.cancel();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.button_listing:
			Log.v("ENTER", "onClick - listing");
			if (mCount > 0 || mStorageEditor.getDataCount() > 0) {
				showDialog(DIALOG_CONFIRM_RESUME);
			}
			else {
				startListing(false);
			}
			break;
		}
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		Dialog dialog = super.onCreateDialog(id);

		Log.d("DIALOG", "onCreateDialog ID: " + String.valueOf(id));
		switch(id) {
		case DIALOG_CONFIRM_RESUME:
			dialog = createConfirmResumeDialog();
			break;
			
		case DIALOG_PROGRESS:
			dialog = createProgressDialog();
			break;
		}
		
		return dialog;
	}

	private Dialog createConfirmResumeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(ImportActivity.this);
		builder.setMessage(R.string.resume_description);
		builder.setPositiveButton(R.string.resume_yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				startListing(true);
			}
		});
		builder.setNegativeButton(R.string.resume_no, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				startListing(false);
			}
		});
		return builder.create();
	}
	
	private Dialog createProgressDialog() {
		Log.d("DIALOG", "createProgressDialog");
		ProgressDialog dialog = new ProgressDialog(ImportActivity.this);
		dialog.setTitle(R.string.progress_title);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				// TODO Auto-generated method stub
				Log.d("Progress", "onCancel");
				endListing(false);
			}
		});

		mProgress = dialog;
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		Log.d("DIALOG", "onPrepareDialog " + dialog.toString());
		super.onPrepareDialog(id, dialog);
		switch(id) {
		case DIALOG_CONFIRM_RESUME:
			break;

		case DIALOG_PROGRESS:
			prepareProgressDialog((ProgressDialog) dialog);
			break;
		}
	}
	
	private void prepareProgressDialog(ProgressDialog dialog) {
		String formattedMsg = getString(R.string.progress_message_format);
		String progressType = "";
		switch (mAuthType) {
		case CardStorage.CARD_TYPE_NORMAL:
			progressType = getString(R.string.progress_type_normal);
			break;
		case CardStorage.CARD_TYPE_AUTH:
			progressType = getString(R.string.progress_type_auth);
			break;
		}
		dialog.setMessage(String.format(formattedMsg, progressType, mCount));		
	}
	
	private void startListing(boolean isResume) {
		if(!mWebTask.setup(isResume)) return;

		if (!isResume) {
			mCount = 0;
			mAuthType = CardStorage.CARD_TYPE_NORMAL;
			mStorageEditor.clear();
			mWebTask.setUrl(createPresentUrl(mAuthType, mCount));
		}
		
		Log.d("LISTING", "0: " + mWebTask.getUrl());
		showDialog(DIALOG_PROGRESS);

        mWeb.getSettings().setPluginsEnabled(false);
		mWeb.getSettings().setLoadsImagesAutomatically(false);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		int interval = Integer.parseInt(
				PreferenceManager.getDefaultSharedPreferences(ImportActivity.this)
				.getString(SettingsActivity.KEY_PREF_WAITTIME, getString(R.string.pref_waittime_default)));

		interval *= 1000;
		mWebTask.setPeriod(interval);
		mWebTask.execute();
	}

	private void endListing(boolean isFinish) {
		if(isFinish) mWebTask.finish();
		else mWebTask.cancel();
		
		mProgress = null;
		dismissDialog(DIALOG_PROGRESS);

        mWeb.getSettings().setPluginsEnabled(true);
		mWeb.getSettings().setLoadsImagesAutomatically(true);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	private void publishProgress(int authType, int count) {
		if (mProgress != null && mWebTask.isRunning()) {
			// Update progress
			String formattedMsg = getString(R.string.progress_message_format);
			String progressType = "";
			switch(mAuthType) {
			case CardStorage.CARD_TYPE_NORMAL:
				progressType = getString(R.string.progress_type_normal);
				break;
			case CardStorage.CARD_TYPE_AUTH:
				progressType = getString(R.string.progress_type_auth);
				break;
			}
			mProgress.setMessage(String.format(formattedMsg, progressType, mCount));
		}
	}
	
	private String createPresentUrl(int authType, int count) {
		Uri.Builder builder = new Uri.Builder();
		try {
			URI uri = new URI(getString(R.string.url_imascg_root));
			String urlQuery = createPresentQuery(authType, count);

			builder.scheme(uri.getScheme());
			builder.authority(uri.getAuthority());
			builder.path(uri.getPath() + "/");
			builder.appendQueryParameter("guid", "ON");
			builder.appendQueryParameter("url", urlQuery);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return builder.build().toString();	
	}
	
	private String createPresentQuery(int authType, int count) {
		String path = getString(R.string.path_imascg_present);
		if (count > 0) {
			path = path + Integer.toString(count);
		}
		
		Uri.Builder builder = new Uri.Builder();
		
		
		builder.scheme("http");
		builder.authority(mPlatHost);
		builder.path(path);
		builder.appendQueryParameter("view_auth_type", Integer.toString(authType));
		builder.appendQueryParameter("sort_type", Integer.toString(3));
		builder.appendQueryParameter("rnd", Integer.toString(new Random().nextInt(100000000)));

		try {
			String uri = builder.build().toString();
			Log.d("LISTING", "createPresetnQuery: " + uri);
			URI uriQuery = new URI(uri);
			return uriQuery.toASCIIString();
		}
		catch (Exception e) {
			// TODO: handle exception
			Log.d("createPresentQuery", e.getMessage());
			return "";
		}
	}
}
