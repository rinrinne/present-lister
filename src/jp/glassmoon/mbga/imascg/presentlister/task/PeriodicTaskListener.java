package jp.glassmoon.mbga.imascg.presentlister.task;

import android.os.Bundle;


public interface PeriodicTaskListener {
		public boolean OnPeriodic(Bundle bundle);
		public void OnFinish();
		public void OnCancel();
}
