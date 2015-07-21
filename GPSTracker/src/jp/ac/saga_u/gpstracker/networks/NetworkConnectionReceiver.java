package jp.ac.saga_u.gpstracker.networks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkConnectionReceiver extends BroadcastReceiver{

	private String TAG = getClass().getName();
	
	private LocationManager locationmanager;
	private LocationListener locationlistener;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connectivitymanager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean isConnect = isConnected(connectivitymanager);
		String message = isConnect ? "Connect" : "Disconnect";
		Log.d(TAG, message);
		
		if(locationmanager!=null && locationlistener!=null){
			if(isConnect){
				locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationlistener);
			}else{
				Log.d(TAG, "removeUpdate");
				locationmanager.removeUpdates(locationlistener);
			}
		}
	}
	
	public static boolean isConnected ( ConnectivityManager manager )
	{
		NetworkInfo info = manager.getActiveNetworkInfo();
		if (info == null ){
			return false;
		}
		return info.isConnected();
	}
	
	public void setLocationManager(LocationManager loc){
		locationmanager = loc;
	}
	
	public void setLocationListener(LocationListener locationlostener){
		this.locationlistener = locationlostener;
	}
}
