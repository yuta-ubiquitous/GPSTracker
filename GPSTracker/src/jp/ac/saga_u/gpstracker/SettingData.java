package jp.ac.saga_u.gpstracker;

public class SettingData {
	private String title_;
	private String value_;
	
	public void setTitle(String title){
		title_ = title;
	}

	public String getTitle(){
		return title_;
	}
	
	public void setValue(String value){
		value_ = value;
	}
	
	public String getValue(){
		return value_;
	}
}
