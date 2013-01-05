package jp.glassmoon.mbga.imascg.presentlister.loader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import jp.glassmoon.mbga.imascg.presentlister.CardStorage;
import jp.glassmoon.mbga.imascg.presentlister.ExportActivity;
import jp.glassmoon.mbga.imascg.presentlister.R;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

public class ExportLoader extends AsyncTaskLoader<Bundle> {
	public static final String RESULT = "result";
	public static final String CONTENT = "content";
	
	private String mExportFile;
	
	public ExportLoader(Context context, Bundle bundle) {
		super(context);
		// TODO Auto-generated constructor stub
		if (bundle != null)
			mExportFile = bundle.getString(ExportActivity.KEY_EXPORT_FILE);
	}

	@Override
	public Bundle loadInBackground() {
		// TODO Auto-generated method stub
		File exportFile = new File(mExportFile);
		Bundle result = new Bundle();
		result.putBoolean(RESULT, true);
		result.putString(CONTENT, mExportFile);
		
		String authStr = getContext().getString(R.string.progress_type_auth);
		String normalStr = getContext().getString(R.string.progress_type_normal);

		FileWriter fw = null;
		try {
			fw = new FileWriter(exportFile, false);
			StringBuilder builder = new StringBuilder();

			//header
			Context context = getContext();
			builder.append(context.getString(R.string.export_column_auth));
			builder.append(",");
			builder.append(context.getString(R.string.export_column_index));
			builder.append(",");
			builder.append(context.getString(R.string.export_column_name));
			builder.append(System.getProperty("line.separator"));
			fw.append(builder.toString());
			builder.setLength(0);
			
			// data
			Cursor c = CardStorage.get(getContext()).getData();
			c.moveToFirst();
			int idxName = c.getColumnIndex(CardStorage.COLUMN_CARD_NAME);
			int idxAuth = c.getColumnIndex(CardStorage.COLUMN_CARD_AUTHTYPE);
			int idxPos = c.getColumnIndex(CardStorage.COLUMN_CARD_INDEX);
			
			do {
				switch(c.getInt(idxAuth)) {
				case CardStorage.CARD_TYPE_AUTH:
					builder.append(authStr);
					break;
				case CardStorage.CARD_TYPE_NORMAL:
					builder.append(normalStr);
					break;
				}
				builder.append(",");
				builder.append(c.getInt(idxPos));
				builder.append(",");
				builder.append(c.getString(idxName));
				builder.append(System.getProperty("line.separator"));
				fw.append(builder.toString());
				builder.setLength(0);
			} while (c.moveToNext());
		}
		catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			result.putBoolean(RESULT, false);
			result.putString(CONTENT, e.getMessage());
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			result = null;
		}
		finally {
			if(fw != null ) {
				try {
					fw.close();
				}
				catch (Exception e) {}
			}
		}
		return result;
	}
}
