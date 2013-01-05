package jp.glassmoon.mbga.imascg.presentlister;

import android.content.SearchRecentSuggestionsProvider;

public class SuggestionProvider extends SearchRecentSuggestionsProvider {
    public static final String AUTHORITY = "jp.glassmoon.mbga.imascg.presentlister.SuggestionProvider";
    public static final int MODE = DATABASE_MODE_QUERIES;
	
	public SuggestionProvider() {
		// TODO Auto-generated constructor stub
		setupSuggestions(AUTHORITY, MODE);
	}

}
