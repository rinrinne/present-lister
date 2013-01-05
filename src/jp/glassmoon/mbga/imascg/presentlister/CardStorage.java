package jp.glassmoon.mbga.imascg.presentlister;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CardStorage {

	public static final int CARD_TYPE_NORMAL = 1;
	public static final int CARD_TYPE_AUTH =  2;
	
	private static final int STATE_NORMAL = 0;
	private static final int STATE_INCOMING = 1;
	private static final int STATE_DELETABLE = 2;
	

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "present.db";

	private static final String TABLE_CARD_NAME = "cards";
	public static final String COLUMN_CARD_ID = "_id";
	public static final String COLUMN_CARD_NAME = "name";
	public static final String COLUMN_CARD_AUTHTYPE = "auth_type";
	public static final String COLUMN_CARD_INDEX = "position";
	public static final String COLUMN_CARD_STATE = "state";
	
	private static final String[] DATABASE_TABLE_CARD_COLUMNS = {
		String.format("%1$s INTEGER PRIMARY KEY AUTOINCREMENT", COLUMN_CARD_ID),
		String.format("%1$s TEXT", COLUMN_CARD_NAME),
		String.format("%1$s INTEGER", COLUMN_CARD_AUTHTYPE),
		String.format("%1$s INTEGER", COLUMN_CARD_INDEX),
		String.format("%1$s INTEGER DEFAULT %2$d", COLUMN_CARD_STATE, STATE_INCOMING)
	};
	
	private static final String TABLE_COUNT_NAME = "count_cards";
	public static final String COLUMN_COUNT_ID = "_id";
	public static final String COLUMN_COUNT_NAME = "name";
	public static final String COLUMN_COUNT_NORMALIZED_NAME = "normalized_name";
	public static final String COLUMN_COUNT_SUM = "count";
	
	private static final String[] DATABASE_TABLE_COUNT_COLUMNS = {
		String.format("%1$s INTEGER PRIMARY KEY AUTOINCREMENT", COLUMN_COUNT_ID),
		String.format("%1$s TEXT", COLUMN_COUNT_NAME),
		String.format("%1$s TEXT", COLUMN_COUNT_NORMALIZED_NAME),
		String.format("%1$s INTEGER", COLUMN_COUNT_SUM)
	};
	
	private static CardStorage storeCards = null;
	private PresentDbOpenHelper openHelper;
	
	private CardStorage(Context context) {
		super();
		// TODO Auto-generated constructor stub
		openHelper = new PresentDbOpenHelper(context);
	}
	
	public static CardStorage get(Context context) {
		if (storeCards == null) {
			storeCards = new CardStorage(context);
		}
		return storeCards;
	}
	
	private PresentDbOpenHelper getHelper() {
		return openHelper;
	}
	
	public Editor edit() {
		return new Editor();
	}
	
	public int getDataSize() {
		String query = String.format("SELECT COUNT(*) FROM %1$s WHERE %2$s=%3$d",
				TABLE_CARD_NAME, COLUMN_CARD_STATE, STATE_NORMAL);
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = db.rawQuery(query, null);
		c.moveToFirst();
		return c.getInt(0);
	}

	public int getCountDataSize() {
		String query = String.format("SELECT COUNT(*) FROM %1$s", TABLE_COUNT_NAME);
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = db.rawQuery(query, null);
		c.moveToFirst();
		return c.getInt(0);
	}

	public Cursor getData() {
		String selection = String.format("%1$s=%2$d", COLUMN_CARD_STATE, STATE_NORMAL);
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cs = db.query(TABLE_CARD_NAME, null, selection, null, null, null, COLUMN_CARD_ID, null);
		return cs;
	}

	public Cursor getCountData() {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cs = db.query(TABLE_COUNT_NAME, null, null, null, null, null, COLUMN_COUNT_ID, null);
		return cs;
	}

	public void updateCount() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		Cursor cs = db.query(
				TABLE_CARD_NAME,
				new String[] {COLUMN_CARD_NAME, String.format("COUNT(%1$s)", COLUMN_CARD_NAME)},
				String.format("%1$s=%2$d", COLUMN_CARD_STATE, STATE_NORMAL),
				null, COLUMN_CARD_NAME, null, COLUMN_CARD_NAME);
		if (cs.getCount() > 0) {
			db.beginTransaction();
			try {
				db.delete(TABLE_COUNT_NAME, null, null);

				ContentValues cv = new ContentValues();
				cs.moveToFirst();
				do {
					cv.clear();
					cv.put(COLUMN_COUNT_NAME, cs.getString(0));
					cv.put(COLUMN_COUNT_NORMALIZED_NAME, Normalizer.normalize(cs.getString(0), Form.NFKC));
					cv.put(COLUMN_COUNT_SUM, cs.getInt(1));
					db.insert(TABLE_COUNT_NAME, null, cv);
				} while(cs.moveToNext());
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
	}
	
	public Cursor searchCountData(String searchText) {
		Log.d("CardStorage", "searchCountData");
		
		if (searchText == null)
			return null;
		
		String[] words = searchText.split(" +");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (builder.length() > 0) builder.append(" AND ");
			String where = String.format("(%1$s LIKE '%%%2$s%%')", COLUMN_COUNT_NORMALIZED_NAME, word);
			builder.append(where);
		}
		Log.d("WHERE", builder.toString());
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = db.query(TABLE_COUNT_NAME, null, builder.toString(), null, null, null, COLUMN_COUNT_ID);
		return c;
	}
	
	public class Editor {
		private SQLiteDatabase editDb;
		private ArrayList<ContentValues> arrayCards;

		private Editor() {
			super();
			// TODO Auto-generated constructor stub
			editDb = getDatabase();
			arrayCards = new ArrayList<ContentValues>();
		}
		
		private SQLiteDatabase getDatabase() {
			SQLiteDatabase db = getHelper().getWritableDatabase();
			db.setLocale(Locale.JAPAN);
			return db;
		}

		public void clear() {
			if (!editDb.isOpen()) editDb = getDatabase();
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_CARD_STATE, STATE_DELETABLE);
			editDb.update(TABLE_CARD_NAME, cv, COLUMN_CARD_STATE + "=" + String.valueOf(STATE_INCOMING), null);
		}
		
		public void gc() {
			if (!editDb.isOpen()) editDb = getDatabase();
			editDb.delete(TABLE_CARD_NAME, COLUMN_CARD_STATE + "=" + String.valueOf(STATE_DELETABLE), null);
			editDb.execSQL("VACUUM");
		}
		
		public ContentValues insert(String name, int authType, int index) {
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_CARD_NAME, name);
			cv.put(COLUMN_CARD_AUTHTYPE, String.valueOf(authType));
			cv.put(COLUMN_CARD_INDEX, String.valueOf(index));
			
			arrayCards.add(cv);
			return cv;
		}
		
		public boolean commit() {
			boolean ret = false;
			if (!editDb.isOpen()) editDb = getDatabase();
			if (arrayCards.size() > 0) {
				editDb.beginTransaction();
				try {
					for (ContentValues cv : arrayCards) {
						cv.put(COLUMN_CARD_STATE, String.valueOf(STATE_INCOMING));
						editDb.insert(TABLE_CARD_NAME, null, cv);
					}
					editDb.setTransactionSuccessful();
					ret = true;
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				finally {
					editDb.endTransaction();
				}
			}
			else {
				Log.d("SQL", "No card records.");
			}
			
			if (ret) arrayCards.clear();
			return ret;
		}
		
		public void promote() {
			if (!editDb.isOpen()) editDb = getDatabase();
			ContentValues cv = new ContentValues();

			// normal -> deletable
			cv.put(COLUMN_CARD_STATE, STATE_DELETABLE);
			editDb.update(TABLE_CARD_NAME, cv, COLUMN_CARD_STATE + "=" + String.valueOf(STATE_NORMAL), null);
			
			// incoming -> normal
			cv.clear();
			cv.put(COLUMN_CARD_STATE, STATE_NORMAL);
			editDb.update(TABLE_CARD_NAME, cv, COLUMN_CARD_STATE + "=" + String.valueOf(STATE_INCOMING), null);
			
			// Garbage collect
			gc();
		}
		
		public int getDataCount() {
			String query = String.format("SELECT COUNT(*) FROM %1$s WHERE %2$s=%3$d",
					TABLE_CARD_NAME, COLUMN_CARD_STATE, STATE_INCOMING);
			Cursor c = editDb.rawQuery(query, null);
			c.moveToFirst();
			return c.getInt(0);
		}
	}
	
	private class PresentDbOpenHelper extends SQLiteOpenHelper {

		public PresentDbOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			// TODO Auto-generated constructor stub	
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			StringBuilder builder;
			String query;

			// Table: card
			builder = new StringBuilder();
			for (String column : DATABASE_TABLE_CARD_COLUMNS) {
				if(builder.length() > 0) builder.append(", ");
				builder.append(column);
			}
			query = String.format("CREATE TABLE %1$s (%2$s)", TABLE_CARD_NAME, builder.toString());
			Log.d("SQL", query);
			db.execSQL(query);

			// Table: count
			builder = new StringBuilder();
			for (String column : DATABASE_TABLE_COUNT_COLUMNS) {
				if(builder.length() > 0) builder.append(", ");
				builder.append(column);
			}
			query = String.format("CREATE TABLE %1$s (%2$s)", TABLE_COUNT_NAME, builder.toString());
			Log.d("SQL", query);
			db.execSQL(query);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			return;
		}
	}
}
