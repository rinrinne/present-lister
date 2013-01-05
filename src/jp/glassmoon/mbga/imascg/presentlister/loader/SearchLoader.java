package jp.glassmoon.mbga.imascg.presentlister.loader;

import jp.glassmoon.mbga.imascg.presentlister.CardListActivity;
import jp.glassmoon.mbga.imascg.presentlister.CardStorage;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

public class SearchLoader extends AsyncTaskLoader<Cursor> {
	private String mSearchText;

	public SearchLoader(Context context, Bundle bundle) {
		super(context);
		// TODO Auto-generated constructor stub
		if (bundle != null)
			mSearchText = bundle.getString(CardListActivity.KEY_SEARCH_TEXT);
	}

	@Override
	public Cursor loadInBackground() {
		// TODO Auto-generated method stub
		Cursor c = CardStorage.get(getContext()).searchCountData(mSearchText);
		
		return c;
	}

}
