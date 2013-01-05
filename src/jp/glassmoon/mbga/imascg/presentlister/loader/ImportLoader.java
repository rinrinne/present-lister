package jp.glassmoon.mbga.imascg.presentlister.loader;

import jp.glassmoon.mbga.imascg.presentlister.CardStorage;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class ImportLoader extends AsyncTaskLoader<Cursor> {

	public ImportLoader(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Cursor loadInBackground() {
		// TODO Auto-generated method stub
		Log.d("LOADER", "Start import in background");
		CardStorage cs = CardStorage.get(getContext());
		cs.edit().promote();
		cs.updateCount();
		return cs.getCountData();
	}
}
