package jp.ac.saga_u.gpstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GPSReceiver extends BroadcastReceiver{

	private boolean GPSFlag = false;
	private String provider;
	private double latitude;
	private double longitude;
	private double accuracy;
	private double altitude;
	private String address;
	private double speed;
	private double bearing;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		GPSFlag = intent.getExtras().getBoolean("GPSFlag");
		provider = intent.getStringExtra("provider");
		address = intent.getStringExtra("address");
		latitude = intent.getExtras().getDouble("latitude");
		longitude = intent.getExtras().getDouble("longitude");
		accuracy = intent.getExtras().getDouble("accuracy");
		altitude = intent.getExtras().getDouble("altitude");
		speed = intent.getExtras().getDouble("speed");
		bearing = intent.getExtras().getDouble("bearing");
	}
	
	public boolean getGPSFlag(){
		return GPSFlag;
	}
	
	public String getProvider(){
		return provider;
	}
	
	public String getAddress(){
		return address;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public double getAccuracy(){
		return accuracy;
	}
	
	public double getAltitude(){
		return altitude;
	}
	
	public double getBearing(){
		return bearing;
	}
	
	public double getSpeed(){
		return speed;
	}
}