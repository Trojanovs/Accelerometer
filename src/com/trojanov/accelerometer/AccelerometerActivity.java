package com.trojanov.accelerometer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AccelerometerActivity extends Activity implements SensorEventListener{

	private SensorManager sensorManager;
	double ax,ay,az;
	
	Boolean streaming = false;
	Button startStreaminButton = null;
	Button sendCustomMessageButton = null;
	
	EditText ipAddressText;
	EditText serverPortText;
	EditText customMessageText;
	Socket socket = null;
	String serverIPAddress = null;
	String serverPort = null;
	String customMessage = null;
	Date timestamp = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accelerometer);		
		
		startStreaminButton = (Button)findViewById(R.id.startStreamingButton);
		sendCustomMessageButton = (Button)findViewById(R.id.sendCustomMessageButton);
		
		ipAddressText = (EditText)findViewById(R.id.ipAddressText);
		serverPortText = (EditText)findViewById(R.id.serverPortText);
		customMessageText = (EditText)findViewById(R.id.customMessageText);	
			
		getSettings();						
				
		startStreaminButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {				
				startStreaming();
			}
		});
		
		sendCustomMessageButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {				
				sendCustomMessage();
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.accelerometer, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {	
		TextView tvX= (TextView)findViewById(R.id.x_axis);
		TextView tvY= (TextView)findViewById(R.id.y_axis);
		TextView tvZ= (TextView)findViewById(R.id.z_axis);
		
        if (event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];    
        }
//        if (event.sensor.getType()==Sensor.TYPE_GRAVITY){
//            ax=ax-event.values[0];
//            ay=ay-event.values[1];
//            az=az-event.values[2];
//         
//        }        
        tvX.setText(String.format("%.3f", ax));
        tvY.setText(String.format("%.3f", ay));
        tvZ.setText(String.format("%.3f", az));  
        
        String message =String.format("%.3f", ax)+" "+String.format("%.3f", ay)+" "+String.format("%.3f", az);
        sendMessage(message);
	}
	

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void startStreaming() {
		if(streaming){
			streaming = false;
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			startStreaminButton.setText(R.string.startSendButton);
			sensorManager.unregisterListener(this);
		}			
		else {				
			streaming = true;
			
			serverIPAddress = ipAddressText.getText().toString();
			serverPort = serverPortText.getText().toString();
			
			setSettings();
			
			startStreaminButton.setText(R.string.stopSendButton);
			sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
		}						     
	}
	
	private void sendCustomMessage() {
						
		serverIPAddress = ipAddressText.getText().toString();
		serverPort = serverPortText.getText().toString();
		customMessage = customMessageText.getText().toString();
		
		setSettings();
		
		sendMessage(customMessage);
		
	}
		
	private void sendMessage(final String message) {		
		if(serverIPAddress.length()>0 && serverPort.length()>0 && message.length()>0){
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						socket = new Socket(serverIPAddress, Integer.valueOf(serverPort));			
						//Send the message to the server
			            OutputStream os = socket.getOutputStream();
			            OutputStreamWriter osw = new OutputStreamWriter(os);
			            BufferedWriter bw = new BufferedWriter(osw);
			            
			            bw.write(message);
			            bw.flush();					            
						
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} 					
				}
			});
			thread.start();	
		}
	}
	
	  public void getSettings() {
		  SharedPreferences settings = getSharedPreferences("server", 0);		  
		  ipAddressText.setText(settings.getString("ip", "").toString());
		  serverPortText.setText(settings.getString("port", "").toString());
	  }

	  public void setSettings() {
		  SharedPreferences settings = getSharedPreferences("server", 0);
		  SharedPreferences.Editor editor = settings.edit();
		  editor.putString("ip",serverIPAddress);
		  editor.putString("port",serverPort);
		  editor.commit();
	  }
}
