package jp.glassmoon.mbga.imascg.presentlister.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import jp.glassmoon.mbga.imascg.presentlister.R;
import jp.glassmoon.mbga.imascg.presentlister.SettingsActivity;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class UploadLoader extends AsyncTaskLoader<Bundle> {
	public static final String DIRECTORY = "directory";
	public static final String FILENAME = "filename";
	public static final String TOKEN_KEY = "token_key";
	public static final String TOKEN_SECRET = "token_secret";
	
	public static final String RESULT = "result";
	public static final String CONTENT = "content";
	public static final String RES_ID = "res_id";
	
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private File mDirectory;
	private String mFileName;
	
	private String mKey;
	private String mSecret;

	public UploadLoader(Context context, Bundle bundle) {
		super(context);
		// TODO Auto-generated constructor stub
		if (bundle != null) {
			mDirectory = new File(bundle.getString(DIRECTORY));
			mFileName = bundle.getString(FILENAME);
			mKey = bundle.getString(TOKEN_KEY);
			mSecret = bundle.getString(TOKEN_SECRET);
			
	        // Dropbox
	        AppKeyPair appKeys = new AppKeyPair(SettingsActivity.APP_KEY, SettingsActivity.APP_SECRET);
	        AndroidAuthSession session = new AndroidAuthSession(appKeys, SettingsActivity.ACCESS_TYPE);
	        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		}
	}

	@Override
	public Bundle loadInBackground() {
		// TODO Auto-generated method stub
		Bundle bundle = new Bundle();
		bundle.putBoolean(RESULT, true);
		bundle.putString(CONTENT, mFileName);
		
		AccessTokenPair tokenPair = new AccessTokenPair(mKey, mSecret);
		mDBApi.getSession().setAccessTokenPair(tokenPair);
		
		FileInputStream fis = null;
		try {
			File uploadFile = new File(mDirectory, mFileName);
			fis = new FileInputStream(uploadFile);
			Entry newEntry = mDBApi.putFile("/" + mFileName, fis, uploadFile.length(), null, null);
			Log.i("DbExampleLog", "The uploaded file's rev is: " + newEntry.rev);
		}
		catch (DropboxUnlinkedException e) {
			// User has unlinked, ask them to link again here.
			bundle.putBoolean(RESULT, false);
			bundle.putInt(RES_ID, R.string.toast_dropbox_unlinked);
			Log.e("DbExampleLog", "User has unlinked.");
		}
		catch (DropboxException e) {
			bundle.putBoolean(RESULT, false);
			bundle.putInt(RES_ID, R.string.toast_dropbox_went_wrong);
			Log.e("DbExampleLog", "Something went wrong while uploading.");
		}
		catch (FileNotFoundException e) {
			bundle.putBoolean(RESULT, false);
			bundle.putInt(RES_ID, R.string.toast_dropbox_file_not_found);
			Log.e("DbExampleLog", "File not found.");
		}
		finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {}
			}
		}
		return bundle;
	}

}
