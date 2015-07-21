package jp.ac.saga_u.gpstracker;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomAdapter extends ArrayAdapter<SettingData> {
	 private LayoutInflater layoutInflater_;
	 
	 public CustomAdapter(Context context, int textViewResourceId, List<SettingData> objects) {
	 super(context, textViewResourceId, objects);
	 layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 }
	 
	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) {
		 SettingData item = (SettingData)getItem(position);
	 
	 if (null == convertView) {
	 convertView = layoutInflater_.inflate(R.layout.listview, null);
	 }
	 
	 TextView TitleText;
	 TitleText = (TextView)convertView.findViewById(R.id.textView1);
	 TitleText.setText(item.getTitle());
	 
	 TextView ValueText;
	 ValueText = (TextView)convertView.findViewById(R.id.textView2);
	 ValueText.setText(item.getValue());
	 
	 return convertView;
	 }
}
