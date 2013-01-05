package jp.glassmoon.mbga.imascg.presentlister.task;

import android.os.Bundle;
import android.util.Log;

public class WebCrewerTask extends AbstractPeriodicTask {
	public static final String URL = "url";
	public static final int LIMIT_RETRY = 5;
	
	private String url;
	private String prevUrl;
	private int retryCount;

	public WebCrewerTask(long period, PeriodicTaskListener listener) {
		super(period, listener);
		// TODO Auto-generated constructor stub
		url = null;
		prevUrl = "";
		retryCount = 0;
	}
	
	public WebCrewerTask(PeriodicTaskListener listener) {
		this(0, listener);
	}

	@Override
	protected Bundle doInPeriod() {
		// TODO Auto-generated method stub
		Log.d("WebCrewlerTask", "doInPeriod");
		String url = getUrl();
		if(getPrevUrl().equals(url)) retryCount++;
		
		if (retryCount == LIMIT_RETRY) {
			cancel();
			return null;
		}
		
		Bundle bundle = new Bundle();
		bundle.putString(URL, url);
		return bundle;
	}

	@Override
	protected void doSetup(boolean isResume) {
		// TODO Auto-generated method stub
		Log.d("WebCrewlerTask", "doSetup");
		if(!isResume) {
			url = null;
			prevUrl = "";
		}
		retryCount = 0;
	}

	@Override
	protected void doCancel() {
		// TODO Auto-generated method stub
		Log.d("WebCrewlerTask", "doCancel");
		retryCount = 0;
	}

	@Override
	protected void doFinish() {
		// TODO Auto-generated method stub
		Log.d("WebCrewlerTask", "doFinish");
		retryCount = 0;
	}
	
	public synchronized String getUrl() {
		return url;
	}

	public synchronized void setUrl(String url) {
		this.prevUrl = this.url == null ? "" : this.url;
		this.url = url;
	}

	private synchronized String getPrevUrl() {
		return prevUrl;
	}

}
