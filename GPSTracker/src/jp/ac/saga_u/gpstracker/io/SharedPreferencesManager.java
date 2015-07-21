package jp.ac.saga_u.gpstracker.io;

import android.content.Context;
import android.content.SharedPreferences;

import jp.ac.saga_u.gpstracker.util.Constants;

/**
 * Created by yuta on 2015/03/31.
 */
public class SharedPreferencesManager {

    public final static int SETTING_IP = 0;
    public final static int SETTING_PORT = 1;
    public final static int SETTING_DIR = 2;
    public final static int SETTING_ACC = 3;
    public final static int SETTING_ID = 4;
    public final static int SETTING_PASS = 5;
    public final static int SETTING_TIME = 6;
    public final static int SETTING_FLAG = 7;
    public final static int TIMES_LOG = 20;
    public final static int SUCCESS_LOG = 21;
    public final static int OPTION_LOGFILE = 40;

    private Context context;
    private SharedPreferences sharedPreferences;

    public SharedPreferencesManager(Context context, String name){
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public void makeNewPreferences(String name){
        sharedPreferences = this.context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public Object getPreferencesData(int mode){
        switch(mode){

            // SETTING_DATA
            case SETTING_IP:
                return sharedPreferences.getString("IP_DATA", Constants.EX_IP);
            case SETTING_PORT:
                return sharedPreferences.getString("PORT_DATA", Constants.EX_PORT);
            case SETTING_DIR:
                return sharedPreferences.getString("DIR_DATA", Constants.EX_DIR);
            case SETTING_ACC:
                return sharedPreferences.getString("ACC_DATA", Constants.EX_ACC);
            case SETTING_ID:
                return sharedPreferences.getString("ID_DATA", "");
            case SETTING_PASS:
                return sharedPreferences.getString("PASS_DATA", "");
            case SETTING_TIME:
                return sharedPreferences.getString("TIME_DATA", Constants.EX_INTERVAL);
            case SETTING_FLAG:
                return sharedPreferences.getString("SENDING_FLAG", "false");

            // SENDING_LOG
            case TIMES_LOG:
                return sharedPreferences.getInt("TIMES_LOG", 0);
            case SUCCESS_LOG:
                return sharedPreferences.getInt("SUCCESS_LOG", 0);

            // OPTION_DATA
            case OPTION_LOGFILE:
                return sharedPreferences.getBoolean("OPTION_LOGFILE", false);
        }
        return null;
    }

    public boolean savePreferencesData(int mode, Object input){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch(mode){

            // SETTING_DATA
            case SETTING_IP:
                editor.putString("IP_DATA", (String)input);
                break;
            case SETTING_PORT:
                editor.putString("PORT_DATA", (String)input);
                break;
            case SETTING_DIR:
                editor.putString("DIR_DATA", (String)input);
                break;
            case SETTING_ACC:
                editor.putString("ACC_DATA", (String)input);
                break;
            case SETTING_ID:
                editor.putString("ID_DATA", (String)input);
                break;
            case SETTING_PASS:
                editor.putString("PASS_DATA", (String)input);
                break;
            case SETTING_TIME:
                editor.putString("TIME_DATA", (String)input);
                break;
            case SETTING_FLAG:
                editor.putString("SENDING_FLAG", (String)input);
                break;

            // SENDING_LOG
            case  TIMES_LOG:
                editor.putInt("TIMES_LOG", Integer.valueOf(input.toString()));
                break;
            case  SUCCESS_LOG:
                editor.putInt("SUCCESS_LOG", Integer.valueOf(input.toString()));
                break;

            // OPTION_DATA
            case OPTION_LOGFILE:
                editor.putBoolean("OPTION_LOGFILE", Boolean.valueOf(input.toString()));
        }
        return editor.commit();
    }
}
