package jp.ac.saga_u.gpstracker.networks;
import org.apache.http.Header;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import jp.ac.saga_u.gpstracker.io.SharedPreferencesManager;

public class AsynchronousHttpClient {

	String TAG = getClass().getName();
	private String url;
	private Context context;
    private SharedPreferencesManager sharedPreferencesManager;
	
	private AsyncHttpClient client;
	private AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
		
		@Override
		public void onSuccess(int statusCode, Header[] headers, byte[] response) {
			Log.v(TAG, "onSuccess()");
            int successTimes = (Integer) sharedPreferencesManager.getPreferencesData(SharedPreferencesManager.SUCCESS_LOG);
			sharedPreferencesManager.savePreferencesData(SharedPreferencesManager.SUCCESS_LOG, new Integer(successTimes + 1));
		}
		
		@Override
		public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	public AsynchronousHttpClient(String url, Context context) {
		this.url = url;
		this.context = context;
		this.client = new AsyncHttpClient();
        this.sharedPreferencesManager = new SharedPreferencesManager(this.context, "SENDING_LOG");
	}
	
	public void doConnect(){
		client.get(url, responseHandler);
	}
	
	public void setUrl(String url){
		this.url = url;
	}
	
}
