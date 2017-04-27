package com.microsoft.hsg.android.hvsample;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.BottomNavigationView;

import com.microsoft.hsg.HVException;
import com.microsoft.hsg.android.simplexml.HealthVaultApp;
import com.microsoft.hsg.android.simplexml.HealthVaultInitializationHandler;
import com.microsoft.hsg.android.simplexml.HealthVaultSettings;
import com.microsoft.hsg.android.simplexml.client.HealthVaultClient;
import com.microsoft.hsg.android.simplexml.client.RequestCallback;
import com.microsoft.hsg.android.simplexml.things.types.types.PersonInfo;
import com.microsoft.hsg.android.simplexml.things.types.types.Record;

public class MainActivity
	extends ListActivity
	implements HealthVaultInitializationHandler {

	private HealthVaultApp mService;
	private HealthVaultClient mHVClient;
	private BottomNavigationView mBottomNav;
	private static final String mMasterAppId  = "c6ba979f-c342-4408-a2bc-0dfb43b2bf8d";
	private static final String mServiceUrl  = "https://platform.healthvault-ppe.com/platform/wildcat.ashx";
	private static final String mShellUrl  = "https://account.healthvault-ppe.com";
	private ProgressDialog mConnectProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mService = HealthVaultApp.getInstance(this);
		if (mService.isAppConnected()) {
			mService.start(this, this);
		}

		mHVClient = new HealthVaultClient();
		
		mBottomNav = (BottomNavigationView) findViewById(R.id.navigation);

		mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				// handle desired action here
				// One possibility of action is to replace the contents above the nav bar
				// return true if you want the item to be displayed as the selected item
				return true;
			}
		});

		final LinearLayout weightTile = (LinearLayout) findViewById(R.id.weightTile);
		weightTile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = null;
				if (mService.isAppConnected()) {
					intent = new Intent(MainActivity.this, WeightActivity.class);
				} else {
					Toast.makeText(MainActivity.this, "Please connect to HV from Setting menu!", Toast.LENGTH_SHORT).show();
				} if(intent != null) {
					startActivity(intent);
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		mHVClient.start();
	}

	protected void onPause(){
		super.onPause();
		if(mConnectProgressDialog != null) {
			mConnectProgressDialog.dismiss();
		}

		mHVClient.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		final LinearLayout recordNameLayout = (LinearLayout) menu.findItem(R.id.recordNameLayout).getActionView();
		final TextView textView = (TextView) recordNameLayout.findViewById(R.id.currentRecordName);
		final HealthVaultApp application = HealthVaultApp.getInstance();
		if(mService.isAppConnected() && application.getCurrentRecord() != null) {
			textView.setText(application.getCurrentRecord().getName());
			recordNameLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) { Intent intent = new Intent(MainActivity.this, RecordPickerActivity.class);
					startActivity(intent);
				}
			});
		} else {
			textView.setVisibility(View.GONE);
		}

		return true;
	}

	public void onConnected() {
	}

	public void onError(Exception e) {
		Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.actionConnect:
				doConnect();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void doConnect() {
		if (!mService.isAppConnected()) {
			mConnectProgressDialog = ProgressDialog.show(
				MainActivity.this, "", "Please wait...", true);
			
			HealthVaultSettings settings = mService.getSettings();
			settings.setMasterAppId(mMasterAppId);
			settings.setServiceUrl(mServiceUrl);
			settings.setShellUrl(mShellUrl);
			settings.setIsMultiInstanceAware(true);
			settings.setIsMRA(true);
			mService.start(MainActivity.this, MainActivity.this);
    	} else {
			Toast.makeText(MainActivity.this, "App is already connected", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final TextView item = (TextView) v;
		Intent intent = null;
		if (mService.isAppConnected()) {
			switch(position) {
				case 0:
					intent = new Intent(MainActivity.this, WeightActivity.class);
					break;
			}
		}
		if(intent != null) {
			startActivity(intent);
		}
	}

	private class HVConnect extends AsyncTask<Void, Void, Void> {
		private ProgressDialog mProgressDialog;
        public HVConnect() {
			mProgressDialog = ProgressDialog.show(MainActivity.this, "", "Please wait...", true);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			HealthVaultSettings settings = mService.getSettings();
			settings.setMasterAppId(mMasterAppId);
			settings.setServiceUrl(mServiceUrl);
			settings.setShellUrl(mShellUrl);
			settings.setIsMultiInstanceAware(true);

			mService.start(MainActivity.this, MainActivity.this);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
        }
	}
	
	public class MainActivityCallback<Object> implements RequestCallback {
		public final static int UpdateRecords = 0;

		private int mEvent;

		public MainActivityCallback(int event) {
			MainActivity.this.setProgressBarIndeterminateVisibility(true);
			mEvent = event;
		}

		@Override
		public void onError(HVException exception) {
			MainActivity.this.setProgressBarIndeterminateVisibility(false);
			Toast.makeText(MainActivity.this, "An error occurred.  " + exception.getMessage(), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onSuccess(java.lang.Object obj) {
			MainActivity.this.setProgressBarIndeterminateVisibility(false);
		}
	}
}
