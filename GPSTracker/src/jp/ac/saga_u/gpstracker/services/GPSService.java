package jp.ac.saga_u.gpstracker.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import jp.ac.saga_u.gpstracker.GpsMain;
import jp.ac.saga_u.gpstracker.R;
import jp.ac.saga_u.gpstracker.io.SharedPreferencesManager;
import jp.ac.saga_u.gpstracker.io.StorageStore;
import jp.ac.saga_u.gpstracker.networks.AsynchronousHttpClient;
import jp.ac.saga_u.gpstracker.networks.NetworkConnectionReceiver;
import jp.ac.saga_u.gpstracker.util.Constants;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class GPSService extends IntentService implements LocationListener {

    static String TAG = "GPSService";

    public static String GPS_ACTION = "GPS_ACTION";
    final static String IP_DATA = "IP_DATA";
    final static String PORT_DATA = "PORT_DATA";
    final static String DIR_DATA = "DIR_DATA";
    final static String ACC_DATA = "ACC_DATA";
    final static String ID_DATA = "ID_DATA";
    final static String PASS_DATA = "PASS_DATA";
    final static String TIME_DATA = "TIME_DATA";
    final static String SETTING_DATA = "SETTING_DATA";

    final static String SENDING_FLAG = "SENDING_FLAG";

    final static String SENDING_LOG = "SENDING_LOG";
    final static String TIMES_LOG = "TIMES_LOG";
    final static String SUCCESS_LOG = "SUCCESS_LOG";

    final static int SETTING_IP = 0;
    final static int SETTING_PORT = 1;
    final static int SETTING_DIR = 2;
    final static int SETTING_ACC = 3;
    final static int SETTING_ID = 4;
    final static int SETTING_PASS = 5;
    final static int SETTING_TIME = 6;
    final static int SETTING_FLAG = 100;

    private static int NOTIFICATION_ID = R.layout.fragment_gps_main;

    // fields
    private String provider;
    private double latitude;
    private double longitude;
    private double accuracy;
    private double altitude;
    private String address;
    private double speed;
    private double bearing;
    private long time;

    private String IP;
    private String PORT;
    private String ACC;
    private String ID;
    private String PASS;
    private String DIR;
    private int TIME;

    //Variables for GPS
    private LocationManager mLocationManager;
    private Criteria mCriteria;
    private Geocoder geocoder;
    private boolean GPSFlag = false;

    // for sending GPS datagrum
    private boolean running = false;
    private Intent intent;

    // for Network state
    private NetworkConnectionReceiver networkconnectionreceiver;

    // for Thread Timer
    private Timer sendTimer;
    private Handler sendHandler;

    // for Writing Data to file
    private StorageStore storageStore;

    public GPSService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate()");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mCriteria = new Criteria();
        mCriteria.setAccuracy(Criteria.ACCURACY_LOW);
        mCriteria.setPowerRequirement(Criteria.ACCURACY_LOW);

        geocoder = new Geocoder(this, Locale.ENGLISH);

        networkconnectionreceiver = new NetworkConnectionReceiver();
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(networkconnectionreceiver, intentfilter);
        networkconnectionreceiver.setLocationManager(mLocationManager);
        networkconnectionreceiver.setLocationListener(this);

        sendTimer = null;
        sendHandler = new Handler();

        intent = new Intent();

        startGps();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "onStartCommand()");

        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(this.getApplicationContext(), "SETTING_DATA");

        //Recording Setting
        if (sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_FLAG).toString().equals("true")) {

            IP = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_IP).toString();
            PORT = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_PORT).toString();
            DIR = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_DIR).toString();
            ID = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_ID).toString();
            PASS = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_PASS).toString();
            TIME = Integer.valueOf(sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_TIME).toString());
            ACC = sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_ACC).toString();

            if((boolean)sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.OPTION_LOGFILE)){
                storageStore = new StorageStore(this.getApplicationContext());
                if (storageStore.checkStorageMount()) {
                    if (storageStore.checkStorageVolume()) {
                        storageStore.init();
                        storageStore.writeStringToFile("Date,Time,Provider,Latitude," +
                                "Longitude,Altitude,Accuracy,Speed,Bearing");
                    }
                }
            }

            running = true;
            startSending();

            // Build and show notification
            Notification notification = new Notification(R.drawable.ic_launcher,
                    "Recording.", System.currentTimeMillis());
            Intent notificationIntent = new Intent(this, GpsMain.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification.setLatestEventInfo(this, "GPSTracker", "Start GPSTracking ...", pendingIntent);
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        // stop GPS update
        mLocationManager.removeUpdates(this);

        // Unregist Receiver
        unregisterReceiver(networkconnectionreceiver);

        // running flag is false
        running = false;

        if (sendTimer != null) sendTimer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind()");
        return null;
    }

    public void startGps() {
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public void startNetwork() {
        Log.v(TAG, "startNetwork()");
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged()");

        if (!GPSFlag) {
            GPSFlag = true;
            Log.v(TAG, "GPSFlag:" + GPSFlag);
        }

        provider = location.getProvider();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();
        altitude = location.getAltitude();
        speed = location.getSpeed();
        bearing = location.getBearing();
        time = location.getTime();
        // address = getLocationAddress(location);

        intent.setAction(GPS_ACTION);
        intent.putExtra("GPSFlag", GPSFlag);
        intent.putExtra("provider", provider);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("accuracy", accuracy);
        intent.putExtra("altitude", altitude);
        intent.putExtra("speed", speed * 1.94384);
        intent.putExtra("bearing", bearing);
        intent.putExtra("time", time);
        // intent.putExtra("address", address);
        sendBroadcast(intent);

        if (running) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
            dateFormat.setTimeZone(cal.getTimeZone());
            String time = dateFormat.format(cal.getTime());

            dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            dateFormat.setTimeZone(cal.getTimeZone());
            String date = dateFormat.format(cal.getTime());

            if(storageStore.isCanUse()){
                storageStore.writeStringToFile(date + "," + time + "," + provider + "," + latitude + "," +
                        longitude + "," + altitude + "," + accuracy + "," + speed + "," + bearing);
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    public String getLocationAddress(Location loc) {

        if (!isConnected()) return "failed";

        double latitude = loc.getLatitude();
        double longitude = loc.getLongitude();

        StringBuffer buff = new StringBuffer();
        try {
            List<Address> addrs = geocoder.getFromLocation(latitude, longitude, 1);
            for (Address addr : addrs) {
                int index = addr.getMaxAddressLineIndex();
                for (int i = 0; i <= index; i++) {
                    if (i == 0) {

                    } else {
                        buff.append(addr.getAddressLine(i));
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return buff.toString();
    }

    public String FullToHalf(String address) {
        String fullText = address;
        String half = "";
        for (int i = 0; i < fullText.length(); i++) {
            FullToHalf(fullText.substring(i, i + 1));
        }
        return "";
    }

    // Generate POST url
    public String getHttpFormat() {
        StringBuffer sb = new StringBuffer();
        sb.append("http://" + IP + ":" + PORT + DIR + "?");
        sb.append("acct=" + ACC + "&");
        sb.append("dev=");
        sb.append(ID + "&");
        sb.append("code=0xF020&");
        sb.append("altitude=");
        sb.append(altitude + "&");
        sb.append("&");
        sb.append("gprmc=");
        sb.append("$GPRMC,");

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        DateFormat dateFormat = new SimpleDateFormat("HHmmss");
        dateFormat.setTimeZone(cal.getTimeZone());
        sb.append(dateFormat.format(cal.getTime()) + ",");

        sb.append("A,");
        sb.append(get_decimal_degrees(String.valueOf(latitude)));
        sb.append(",N,");
        sb.append(get_decimal_degrees(String.valueOf(longitude)));
        sb.append(",E,"
                + speed + ","
                + bearing + ",");

        dateFormat = new SimpleDateFormat("ddMMyy");
        dateFormat.setTimeZone(cal.getTimeZone());
        sb.append(dateFormat.format(cal.getTime()) + ",,");

        return sb.toString();
    }

    // Fix location to decimal degrees
    public static String get_decimal_degrees(String stringNum) {
        Double num = new Double(Double.parseDouble(stringNum));
        Integer degrees = new Integer(num.intValue());
        num = new Double((num.doubleValue() - degrees.doubleValue()) * 60.0);
        Integer minutes = new Integer(num.intValue());
        num = new Double((num.doubleValue() - minutes.doubleValue()) * 10000000.0);
        Integer seconds = new Integer(num.intValue());
        StringBuffer sb = new StringBuffer();
        sb.append(degrees.toString());
        if (minutes.toString().length() == 1) {
            sb.append("0");
        }
        sb.append(minutes.toString());
        sb.append(".");
        if (seconds.toString().length() < 7) {
            for (int i = 0; i < 7 - seconds.toString().length(); i++) {
                sb.append("0");
            }
        }
        sb.append(seconds.toString());

        return sb.toString();
    }

    public void startSending() {
        Log.v(TAG, "startSending()");
        sendTimer = new Timer();
        sendTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (running && GPSFlag) {
                            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(getApplicationContext(), "SENDING_LOG");
                            if (isConnected()) {
                                String url = getHttpFormat();
                                Log.v(TAG, url);
                                new AsynchronousHttpClient(url, getApplicationContext()).doConnect();
                            }
                            int currentTimes = Integer.valueOf(sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.TIMES_LOG).toString());
                            sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.TIMES_LOG, new Integer(currentTimes + 1));
                            Log.v(TAG, "TIMES:" + (currentTimes + 1));
                        }

                    }
                });
            }
        }, TIME * 1000, TIME * 1000);
    }

    public String getSettingData(int mode) {
        Context context = getApplicationContext();
        SharedPreferences pref =
                context.getSharedPreferences(SETTING_DATA, Context.MODE_PRIVATE);
        switch (mode) {
            case SETTING_IP:
                return pref.getString(IP_DATA, Constants.EX_IP);
            case SETTING_PORT:
                return pref.getString(PORT_DATA, Constants.EX_PORT);
            case SETTING_DIR:
                return pref.getString(DIR_DATA, Constants.EX_DIR);
            case SETTING_ACC:
                return pref.getString(ACC_DATA, Constants.EX_ACC);
            case SETTING_ID:
                return pref.getString(ID_DATA, "");
            case SETTING_PASS:
                return pref.getString(PASS_DATA, "");
            case SETTING_TIME:
                return pref.getString(TIME_DATA, Constants.EX_INTERVAL);
            case SETTING_FLAG:
                return pref.getString(SENDING_FLAG, "false");
        }
        return null;
    }

    public int getTIME() {
        return TIME;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO Auto-generated method stub

    }

    boolean isConnected() {
        ConnectivityManager myConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnectivityManager.getActiveNetworkInfo();
        boolean connectFlag = false;
        try {
            connectFlag = myNetworkInfo.isConnected();
        } catch (NullPointerException e) {
        }
        return connectFlag;
    }
}
