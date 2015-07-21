package jp.ac.saga_u.gpstracker.io;

import android.app.Activity;
import android.content.Context;
import android.graphics.AvoidXfermode;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import jp.ac.saga_u.gpstracker.R;

/**
 * Created by yuta on 2015/02/20.
 */
public class StorageStore{

    public static final String STORAGE_DIR = "GPSTrakcer2015";
    private String TAG = getClass().getName();

    private Context context;
    private File file;
    private String fileName;
    private boolean canUse;

    public StorageStore(Context context){
        this.context = context;
        fileName = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).getTimeInMillis() + "-data.csv";
        canUse = false;
    }

    public boolean checkStorageMount(){
        String storageStatus = Environment.getExternalStorageState();
        if (!storageStatus.equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
            Toast.makeText(context, "Storageを利用できません.", Toast.LENGTH_LONG).show();
            Log.v(TAG, "Can't use the Storage");
            return false;
        }

        Log.v(TAG, "Can use the Storage");
        return true;
    }

    public boolean checkStorageVolume(){

        long freeByteSIze = Environment.getExternalStorageDirectory().getFreeSpace();
        Log.v(TAG, "Free byte size is " + freeByteSIze/(1024*1024) + "[MB].");

        if(freeByteSIze/(1024*1024) < 1){
            Toast.makeText(context, "Storage容量が少なすぎます.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public boolean init(){

        String fileDir = Environment.getExternalStorageDirectory() + "/" + STORAGE_DIR;
        File dir = new File(fileDir);

        if(!dir.exists()){
            Log.v(TAG, STORAGE_DIR + " isn't exist.");
            if(dir.mkdir()){
                Toast.makeText(context, STORAGE_DIR + "を作成しました.", Toast.LENGTH_LONG).show();
                Log.v(TAG, "Made " + STORAGE_DIR);
            }else{
                Toast.makeText(context, STORAGE_DIR + "の作成に失敗しました.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Couldn't make " + STORAGE_DIR);
                return false;
            }
        }else{
            Log.v(TAG, STORAGE_DIR + " is exist.");
            file = new File(fileDir + "/" + fileName);
            try {
                if(file.createNewFile()){
                    Log.v(TAG, "create " + fileName);
                    canUse = true;
                    return true;
                }else{
                    Log.v(TAG, "Can't create " + fileName);
                    return false;
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean writeStringToFile(String content){
        try {
            if(file.isFile() && file.canWrite()){
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write(content + "\r\n");
                fileWriter.close();
                Log.v(TAG, "Can write the file.");
                return true;
            }else{
                Log.v(TAG, "Can't write the file.");
                return false;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean isCanUse(){
        return canUse;
    }
}
