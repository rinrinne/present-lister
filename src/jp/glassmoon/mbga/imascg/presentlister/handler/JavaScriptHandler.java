package jp.glassmoon.mbga.imascg.presentlister.handler;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class JavaScriptHandler {

	public static final String INTERFACE = "presentlister";
	public static final String CARD = "card";
	public static final String PAGE = "page";
	

	public interface CallbackListener {
		public abstract void OnReceiveCards(Bundle bundle);
		public abstract void OnReceiveNextPage(String url);
	}

	public static final String JS_GET_CARDS = 
			"javascript:var r=[];$('label#Label1 td:odd').each(function(){r.push($.trim($(this).html().split('<br>')[0]))});window.presentlister.getCards(r);";

	public static final String JS_GET_NEXTPAGE =
			"javascript:window.presentlister.getNextUrl($('a.a_link[accesskey=\"#\"]').attr('href'));";
	
	private Handler handler;
	private CallbackListener listener;
	
	public JavaScriptHandler(CallbackListener listener) {
		// TODO Auto-generated constructor stub
		this.handler = new Handler();
		this.listener = null;
		if (listener instanceof CallbackListener) this.listener = listener;
	}

	// JavaScript Interfaces
	public void getCards(final String[] cards) {
		Bundle bundle = new Bundle();
		bundle.putStringArray(CARD, cards);
		onReceiveCards(bundle);
	}

	public void getNextUrl(final String url) {
		Log.d("LISTING", "url: " + url);
		if (url == null || "".equals(url) || "undefined".equals(url)) {
			onReceiveNextPage(null);
		}
		else {
			onReceiveNextPage(url);
		}
	}
	
	// Inner methods
	private void onReceiveCards(final Bundle bundle) {
		this.handler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(listener != null) listener.OnReceiveCards(bundle);
			}
		});
	}
	
	private void onReceiveNextPage(final String url) {
		this.handler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(listener != null) listener.OnReceiveNextPage(url);
			}
		});
	}
}
