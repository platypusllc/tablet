package com.platypus.android.tablet;

import java.net.InetSocketAddress;

//import com.google.android.gms.maps.MapFragment;

import com.platypus.crw.CrwNetworkUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

public class ConnectScreen extends Activity implements OnClickListener {

	public EditText ipAddress = null;
	public EditText phoneIDNumber = null;
	public EditText color = null;
	public RadioButton actualBoat = null;
	public RadioButton simulation = null;
	public Button submitButton = null;
	public static String textIpAddress;
	public static String phoneID = "";
	public static boolean simul = false;
	public static boolean actual = false;
	public static boolean validIP;
	public static Boat boat;
	public static InetSocketAddress address;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.connectscreen);
		ipAddress = (EditText) this.findViewById(R.id.ipAddress1);
		//phoneIDNumber = (EditText) this.findViewById(R.id.phoneIDNumber);
		//color = (EditText) this.findViewById(R.id.colorBox);
		//actualBoat = (RadioButton) this.findViewById(R.id.actualBoatRadio);
		//simulation = (RadioButton) this.findViewById(R.id.simulationRadio);
		submitButton = (Button) this.findViewById(R.id.submit);
		submitButton.setOnClickListener(this);

		
		// @Override
		// public boolean onKey(View v, int keyCode, KeyEvent event)
		// {
		// if (event.getAction() == KeyEvent.ACTION_DOWN)
		// {
		//
		// if(keyCode == KeyEvent.KEYCODE_ENTER)
		// {
		// if(phoneIDNumber.getText().toString().charAt(phoneIDNumber.getText().length()-1)
		// == '\n')
		// {
		// phoneIDNumber.setText(phoneIDNumber.getText().toString().substring(0,phoneIDNumber.getText().length()-1));
		// }
		// return true;
		// }
		// }
		// return false;
		// }

		// @Override
		// public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		// return false;
		// }
		// });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		// if (textIpAddress != "")
		if (actualBoat.isChecked()) {
			actual = true;
			simul = false;
		}
		if (simulation.isChecked()) {
			simul = true;
			actual = false;
		}

		textIpAddress = ipAddress.getText().toString();
		address = CrwNetworkUtils.toInetSocketAddress(textIpAddress + ":11411");
		//phoneID = phoneIDNumber.getText().toString();
		// startActivity(new Intent(this,MapTest.class));
		if (address != null)
			{
			//	boat = new Boat(address);
			}
		boat = new Boat(address);
		startActivity(new Intent(this, TeleOpPanel.class));
	}

	public static InetSocketAddress getAddress()
		{
			return address;
		}
	public static String getIpAddress() {
		return textIpAddress;
	}

	public static boolean getBoatType() {
		return actual;
	}
}
