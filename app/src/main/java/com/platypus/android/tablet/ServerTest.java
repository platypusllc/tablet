package com.platypus.android.tablet;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;



public class ServerTest extends Activity{
	TextView testIP = null;
	protected void onCreate(Bundle savedInstanceState) 
	{
		testIP = (TextView)this.findViewById(R.id.iptest);
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.servertest);
        testIP.setText("test123123");	 
        
        
	}
	 public void serverTest() throws Exception 
	 {
		 
	 }
		 
}
