package com.noplanbees.tbm;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


public class DataHolderService extends NonStopIntentService {

	public DataHolderService() {
		super("DataHolderService");
	}

	private static final String TAG = "DataHolderService";

	private ActiveModelsHandler dataManager;

	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}

	public class LocalBinder extends Binder {
		public ActiveModelsHandler getDataManager() {
			return DataHolderService.this.dataManager;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "onCreate");

		dataManager = ActiveModelsHandler.getInstance(this);
		//--------------------------
		// Retrieve or create User model from local storage
		//--------------------------
		dataManager.ensureUser();
		//--------------------------
        // Try to retrieve all models from local storage
		//--------------------------
		dataManager.ensureAll();
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.d(TAG, "onDestroy");
		dataManager.saveAll();
	}

	@Override
	protected void onHandleIntent(Intent intent, int startId) {
		new IntentHandler(this, intent).handle();
	}
	

}
