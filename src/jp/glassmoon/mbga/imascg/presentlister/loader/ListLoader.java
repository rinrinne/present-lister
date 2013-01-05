package jp.glassmoon.mbga.imascg.presentlister.loader;

import jp.glassmoon.mbga.imascg.presentlister.CardStorage;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class ListLoader extends AsyncTaskLoader<Cursor> {

	public ListLoader(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Cursor loadInBackground() {
		// TODO Auto-generated method stub
		Log.d("LOADER", "Start list in background");
		CardStorage cs = CardStorage.get(getContext());
		return cs.getCountData();
	}

}
