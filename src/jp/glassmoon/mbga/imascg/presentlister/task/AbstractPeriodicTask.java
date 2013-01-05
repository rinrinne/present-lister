package jp.glassmoon.mbga.imascg.presentlister.task;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public abstract class AbstractPeriodicTask {
	
	private Timer timer;
	private Handler handler;
	private long period;
	private boolean isDaemon;
	private boolean isCanceled;
	private boolean isRunnning;
	private PeriodicTaskListener listener;
	
	public AbstractPeriodicTask(long period, boolean isDaemon, PeriodicTaskListener listener) {
		handler = new Handler();
		this.period = period;
		this.isDaemon = isDaemon;
		this.isCanceled = false;
		this.isRunnning = false;
		this.listener = null;
		if (listener instanceof PeriodicTaskListener) this.listener = listener;
	}
	
	public AbstractPeriodicTask(long period, PeriodicTaskListener listener) {
		this(period, false, listener);
	}
	
	public AbstractPeriodicTask(PeriodicTaskListener listener) {
		this(0, listener);
	}
	
	public boolean execute() {
		
		if (isCanceled) return false;
		if (period == 0L) return false;
		if (isRunnning) return false;
		
		Log.d("AbstractPeriodicTask", "execute");

		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				final Bundle bundle = doInPeriod();
				if(bundle == null) return;
				
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (listener != null) {
							boolean isFinish = listener.OnPeriodic(bundle);
							if(isFinish) finish();
						}
					}
				});
			}
		};
		
		timer = new Timer(isDaemon);
		timer.scheduleAtFixedRate(timerTask, 100, period);
		isRunnning = true;
		return isRunnning;
	}
	
	public void cancel() {
		if (timer == null) return;
		Log.d("AbstractPeriodicTask", "cancel");
		timer.cancel();
		timer = null;
		isCanceled = true;
		isRunnning = false;
		doCancel();
		
		if(listener != null) {
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					listener.OnCancel();
				}
			});
		}
	}
	
	public void finish() {
		if (timer == null) return;
		Log.d("AbstractPeriodicTask", "finish");
		timer.cancel();
		timer = null;
		isRunnning = false;
		doFinish();
		
		if (listener != null) {
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					listener.OnFinish();
				}
			});
		}
	}
	
	public boolean setup(boolean isResume) {
		if(isRunnning) return false;
		Log.d("AbstractPeriodicTask", "setup");
		isCanceled = false;
		doSetup(isResume);
		return true;
	}

	public boolean isCanceled() {
		return isCanceled;
	}
	
	public boolean isRunning() {
		return isRunnning;
	}

	public long getPeriod() {
		return period;
	}

	public long setPeriod(long period) {
		if(!isRunnning) this.period = period;
		return this.period;
	}
	
	abstract protected Bundle doInPeriod();
	abstract protected void doSetup(boolean isResume);
	abstract protected void doCancel();
	abstract protected void doFinish();
}
