package jp.ac.saga_u.gpstracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.ac.saga_u.gpstracker.io.SharedPreferencesManager;
import jp.ac.saga_u.gpstracker.services.GPSService;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GpsMain extends ActionBarActivity implements ActionBar.TabListener {

    final static String GPS_SERVISE = "jp.ac.saga_u.gpstracker.services.GPSService";
    private String TAG = getClass().getName();

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    static Context mContext;

    final static int MAX_PAGES = 3;
    private final static long ONE_SECOND = (long) 1000.0;

    // Flag
    static boolean RecordingFlag;
    static boolean GPSFlag = false;
    static boolean LogFlag = false;
    static boolean isPaused = false;

    // Handler
    static Handler logHandler;
    static Handler GPSHandler;

    // Receiver
    static GPSReceiver mGPSReceiver;

    // Log
    static int sendcount = 0;
    static int successcount = 0;
    static int failedcount = 0;

    // GUI parts
    // 1st page
    private static TextView TimesView;
    private static TextView SuccessView;
    private static TextView FailedView;
    private static TextView StatusView;

    // 2nd page
    private static TextView timeView;
    private static TextView dateView;
    private static TextView providerView;
    private static TextView latitudeView;
    private static TextView longitudeView;
    private static TextView accuracyView;
    private static TextView altitudeView;
    // private static TextView addressView;
    private static TextView speedView;
    private static TextView bearingView;

    // 3rd page
    private static EditText DialogEdittext;

    final static int POSITION_IP = 0;
    final static int POSITION_PORT = 1;
    final static int POSITION_DIR = 2;
    final static int POSITION_ACC = 3;
    final static int POSITION_ID = 4;
    final static int POSITION_PASS = 5;
    final static int POSITION_TIME = 6;

    final static int TIME_INTERVAL_MAX = 1200;
    final static int TIME_INTERVAL_MIN = 1;

    // Runnables
    // Logger
    private static Runnable LogRunnable = new Runnable() {
        public void run() {
            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SENDING_LOG");
            sendcount = (Integer) sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.TIMES_LOG);
            successcount = (Integer) sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SUCCESS_LOG);
            failedcount = sendcount - successcount;

            TimesView.setText("Send:" + sendcount);
            SuccessView.setText("Success:" + successcount);
            FailedView.setText("Failed:" + failedcount);

            if (GpsMain.RecordingFlag) {
                StatusView.setText("NO SIGNAL");
                StatusView.setTextColor(Color.RED);
                if (mGPSReceiver.getGPSFlag()) {
                    StatusView.setText("Recording");
                    StatusView.setTextColor(Color.BLUE);
                }
            } else {
                StatusView.setText("STANBY");
                StatusView.setTextColor(Color.BLACK);
            }

            if (!GpsMain.isPaused) {
                GpsMain.logHandler.postDelayed(this, ONE_SECOND);
            } else {
                GpsMain.logHandler.removeCallbacks(this);
            }
        }
    };

    // GPS datagrum
    // get data from mGPSReceiver
    private final static Runnable GPSRunnable = new Runnable() {
        public void run() {

            Time time = new Time("Asia/Tokyo");
            time.setToNow();

            // Setting date
            dateView.setText(time.year + "/" +
                    formatTime(time.month + 1) + "/" +
                    formatTime(time.monthDay));

            // Setting time
            timeView.setText(formatTime(time.hour) + ":" +
                    formatTime(time.minute) + ":" +
                    formatTime(time.second));

            if (mGPSReceiver.getGPSFlag()) {
                providerView.setText(mGPSReceiver.getProvider());
                latitudeView.setText(String.valueOf(mGPSReceiver.getLatitude()));
                longitudeView.setText(String.valueOf(mGPSReceiver.getLongitude()));
                accuracyView.setText(String.valueOf(mGPSReceiver.getAccuracy()));
                altitudeView.setText(String.valueOf(mGPSReceiver.getAltitude()));
                // addressView.setText(mGPSReceiver.getAddress());
                speedView.setText(String.valueOf(mGPSReceiver.getSpeed()));
                bearingView.setText(String.valueOf(mGPSReceiver.getBearing()));
            }
            if (!GpsMain.isPaused) {
                GpsMain.GPSHandler.postDelayed(this, ONE_SECOND);
            } else {
                GpsMain.GPSHandler.removeCallbacks(this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_main);
        mContext = getApplicationContext();

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
                .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        if (!checkServiceRunning()) {
            Intent mIntent = new Intent(mContext, GPSService.class);
            mIntent.putExtra("FLAG", false);
            startService(mIntent);
            RecordingFlag = false;
        } else {
            RecordingFlag = true;
        }

        mGPSReceiver = new GPSReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GPSService.GPS_ACTION);
        registerReceiver(mGPSReceiver, intentFilter);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");

        //don't stop the service when recording
        if (!RecordingFlag) {
            stopService(new Intent(this, GPSService.class));
        }
        unregisterReceiver(mGPSReceiver);
    }

    @Override
    public void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");

        isPaused = true;

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");

        if (isPaused) {
            logHandler.postDelayed(LogRunnable, ONE_SECOND);
            GPSHandler.postDelayed(GPSRunnable, ONE_SECOND);
        }

        isPaused = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gps_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent menuIntent = new Intent();
        if (id == R.id.action_option) {
            menuIntent.setClassName("jp.ac.saga_u.gpstracker", "jp.ac.saga_u.gpstracker.OptionsActivity");
            startActivity(menuIntent);
            return true;
        } else if (id == R.id.action_imfo) {
            menuIntent.setClassName("jp.ac.saga_u.gpstracker", "jp.ac.saga_u.gpstracker.InfoActivity");
            startActivity(menuIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab,
                              FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab,
                                FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab,
                                FragmentTransaction fragmentTransaction) {
    }

    public boolean checkServiceRunning() {
        ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<RunningServiceInfo> listServiceInfo = mActivityManager.getRunningServices(Integer.MAX_VALUE);
        for (RunningServiceInfo rsi : listServiceInfo) {
            if (rsi.service.getClassName().equals(GPS_SERVISE)) return true;
            // System.out.println(rsi.service.getClassName());
        }
        return false;
    }

    public static String formatTime(int integerTime) {
        String stringTime = String.valueOf(integerTime);
        if (stringTime.length() == 1) {
            return "0" + stringTime;
        }
        return stringTime;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class
            // below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return MAX_PAGES;
        }

        // セクション名の指定
        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section0).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static class PlaceholderFragment extends Fragment {

        private View rootView;
        private ImageButton redButton;
        private ImageButton blueButton;
        private boolean RedButtonFlag = !GpsMain.RecordingFlag;
        private String TAG = getClass().getName();

        private ListView lv;

        //Notification
        private static int NOTIFICATION_ID = R.layout.fragment_gps_main;
        NotificationManager mNotificationManager;

        final static String[] SettingMembers = {
                "IP Adress", "Port", "Directory", "Account",
                "ID", "Pass", "Time Interval"
        };

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.v(TAG, "onCreateView()");

            switch (getCurrentPage()) {

                case 1:

                    // Set default GUI part
                    rootView = inflater.inflate(R.layout.fragment_gps_action, container, false);

                    TimesView = (TextView) rootView.findViewById(R.id.textView2);
                    SuccessView = (TextView) rootView.findViewById(R.id.textView3);
                    FailedView = (TextView) rootView.findViewById(R.id.textView4);
                    StatusView = (TextView) rootView.findViewById(R.id.textView1);

                    // Set buttons
                    // Set start recording button listner
                    redButton = (ImageButton) rootView.findViewById(R.id.imageButton1);
                    redButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            redButton.setEnabled(false);
                            blueButton.setEnabled(true);
                            RedButtonFlag = false;
                            GpsMain.RecordingFlag = true;

                            mContext.stopService(new Intent(GpsMain.mContext, GPSService.class));

                            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SETTING_DATA");
                            sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.SETTING_FLAG, "true");
                            sharedPreferencesManager.makeNewPreferences("SENDING_LOG");
                            sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.TIMES_LOG, Integer.valueOf(0));
                            sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.SUCCESS_LOG, Integer.valueOf(0));

                            mContext.startService(new Intent(GpsMain.mContext, GPSService.class));

                            checkGps();
                        }
                    });

                    //Set stop recording button listner
                    blueButton = (ImageButton) rootView.findViewById(R.id.imageButton2);
                    blueButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            redButton.setEnabled(true);
                            blueButton.setEnabled(false);
                            RedButtonFlag = true;
                            GpsMain.RecordingFlag = false;

                            mContext.stopService(new Intent(GpsMain.mContext, GPSService.class));

                            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SETTING_DATA");
                            sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.SETTING_FLAG, "false");

                            mContext.startService(new Intent(mContext, GPSService.class));

                            sharedPreferencesManager.makeNewPreferences("SENDING_LOG");
                            sendcount = (Integer) sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.TIMES_LOG);
                            successcount = (Integer) sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SUCCESS_LOG);
                            failedcount = sendcount - successcount;

                            TimesView.setText("Send:" + sendcount);
                            SuccessView.setText("Success:" + successcount);
                            FailedView.setText("Failed:" + failedcount);
                        }
                    });

                    // Set buttons status
                    if (RedButtonFlag) {
                        redButton.setEnabled(true);
                        blueButton.setEnabled(false);
                    } else {
                        redButton.setEnabled(false);
                        blueButton.setEnabled(true);
                    }

                    // Set default log text
                    TimesView.setText("Send:" + sendcount);
                    SuccessView.setText("Success:" + successcount);
                    FailedView.setText("Failed:" + failedcount);

                    // Set logging flag
                    if (!GpsMain.LogFlag) {
                        GpsMain.logHandler = new Handler();
                        GpsMain.logHandler.postDelayed(LogRunnable, ONE_SECOND);
                        GpsMain.LogFlag = true;
                    }

                    break;

                case 2:
                    rootView = inflater.inflate(R.layout.fragment_gps_data,
                            container, false);

                    // Initialize GUI parts for GPS Data
                    providerView = (TextView) rootView.findViewById(R.id.textView6);
                    latitudeView = (TextView) rootView.findViewById(R.id.textView8);
                    longitudeView = (TextView) rootView.findViewById(R.id.textView10);
                    accuracyView = (TextView) rootView.findViewById(R.id.textView12);
                    altitudeView = (TextView) rootView.findViewById(R.id.textView14);
                    // addressView = (TextView)rootView.findViewById(R.id.textView16);
                    speedView = (TextView) rootView.findViewById(R.id.textView18);
                    bearingView = (TextView) rootView.findViewById(R.id.textView20);

                    // Initialize timer parts
                    dateView = (TextView) rootView.findViewById(R.id.textView2);
                    timeView = (TextView) rootView.findViewById(R.id.textView4);

                    // Set current GPSFlag
                    if (!GpsMain.GPSFlag) {
                        GpsMain.GPSHandler = new Handler();
                        GpsMain.GPSHandler.postDelayed(GPSRunnable, ONE_SECOND);
                        GpsMain.GPSFlag = true;
                    }

                    break;

                case 3:
                    rootView = inflater.inflate(R.layout.fragment_gps_setting, container, false);

                    // CustomAdapter
                    CustomAdapter customadapter = getCurrentCustomAdapter();

                    // Initialize listview
                    lv = (ListView) rootView.findViewById(R.id.listView1);

                    // Set listener for listview
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                            LayoutInflater inflater = LayoutInflater.from(getActivity());
                            View dialogView = inflater.inflate(R.layout.plane_context_menu, null);
                            DialogEdittext = (EditText) dialogView.findViewById(R.id.editText);

                            // EditTextに表示するテキスト
                            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SETTING_DATA");
                            String settingDataText = sharedPreferencesManager.getPreferencesData(position).toString();
                            DialogEdittext.setText(settingDataText);

                            // PortとIntervalのInputTypeをNumber
                            if (position == POSITION_PORT || position == POSITION_TIME) {
                                DialogEdittext.setInputType(InputType.TYPE_CLASS_NUMBER);
                            }

                            // PASSのInputTypeをPassword
                            if (position == POSITION_PASS) {
                                DialogEdittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            }

                            // カーソルの位置指定
                            DialogEdittext.setSelection(settingDataText.length());

                            final AlertDialog alertdialog = new AlertDialog.Builder(getActivity())
                                    .setTitle(SettingMembers[position])
                                    .setView(dialogView)
                                    .setPositiveButton(
                                            "OK", null)
                                    .setNegativeButton("Cancel",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            }).create();

                            // Show dialog (Cancel or OK)
                            alertdialog.show();
                            Button positiveButton = alertdialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    SpannableStringBuilder sb = (SpannableStringBuilder) DialogEdittext.getText();
                                    Log.v(TAG, sb.toString());

                                    SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SETTING_DATA");
                                    switch (position) {
                                        case POSITION_TIME:

                                            int timeInterval = 1;
                                            if (sb.toString().length() == 0) {
                                                Toast.makeText(getActivity(), "Please Type Time Interval.", Toast.LENGTH_LONG).show();
                                                break;
                                            } else {
                                                timeInterval = Integer.parseInt(sb.toString());
                                            }

                                            // error処理
                                            if (timeInterval < TIME_INTERVAL_MIN || timeInterval > TIME_INTERVAL_MAX) {
                                                Toast.makeText(getActivity(), "Please Type Time Interval From " + TIME_INTERVAL_MIN +
                                                        "-" + TIME_INTERVAL_MAX + "[sec]", Toast.LENGTH_LONG).show();
                                            } else {
                                                sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.SETTING_TIME, sb.toString());
                                                lv.setAdapter(getCurrentCustomAdapter());
                                                alertdialog.dismiss();
                                            }
                                            break;

                                        default:
                                            sharedPreferencesManager.savePreferencesData(position, sb.toString());
                                            lv.setAdapter(getCurrentCustomAdapter());
                                            alertdialog.dismiss();
                                    }
                                }
                            });
                        }
                    });

                    lv.setAdapter(customadapter);

                    break;

                default:
                    rootView = inflater.inflate(R.layout.fragment_gps_main,
                            container, false);
                    TextView textView = (TextView) rootView
                            .findViewById(R.id.section_label);
                    textView.setText(Integer.toString(getArguments().getInt(
                            ARG_SECTION_NUMBER)));
                    break;
            }
            return rootView;
        }

        public int getCurrentPage() {
            return getArguments().getInt(ARG_SECTION_NUMBER);
        }

        public CustomAdapter getCurrentCustomAdapter() {
            List<SettingData> objects = new ArrayList<SettingData>();
            SettingData[] item = new SettingData[SettingMembers.length];

            for (int i = 0; i < SettingMembers.length; i++) {
                item[i] = new SettingData();
                item[i].setTitle(SettingMembers[i]);
                SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(GpsMain.mContext, "SETTING_DATA");
                switch (i) {
                    case POSITION_PASS:
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SETTING_PASS).toString().length(); j++) {
                            sb.append("*");
                        }
                        item[i].setValue(sb.toString());
                        break;
                    default:
                        item[i].setValue((String) sharedPreferencesManager.getPreferencesData(i));
                }
                objects.add(item[i]);
            }

            return new CustomAdapter(getActivity(), 0, objects);
        }

        void showNotification() {
            Notification mNotice = new Notification();
            mNotice.icon = R.drawable.ic_launcher;
            mNotice.tickerText = "Recording...";
            mNotice.flags = Notification.FLAG_NO_CLEAR;

            //PendingIntent
            Intent mIntent = new Intent(getActivity().getApplicationContext(), GpsMain.class);
            PendingIntent mPending = PendingIntent.getActivity(getActivity(), 0, mIntent, 0);
            mNotice.setLatestEventInfo(getActivity().getApplicationContext(), "GPSTracker", "Recording GPS...", mPending);
            mNotificationManager =
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, mNotice);

        }

        void delNotification() {
            mNotificationManager =
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        void checkGps() {
            String gpsStatus = android.provider.Settings.Secure.getString(getActivity().getContentResolver(), Secure.LOCATION_PROVIDERS_ALLOWED);
            Log.v(TAG, gpsStatus);
            if (gpsStatus.indexOf("gps", 0) < 0)
                Toast.makeText(getActivity(), "GPSがオフになっています.", Toast.LENGTH_LONG).show();
        }
    }
}