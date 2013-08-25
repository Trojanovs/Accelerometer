package com.trojanov.accelerometer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
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
	Boolean recording = false;
	Button startStreaminButton = null;
	Button sendCustomMessageButton = null;
	Button startRecordingButton = null;
	
	EditText ipAddressText;
	EditText serverPortText;
	EditText customMessageText;
	EditText endOfMessageText;
	EditText periodText;
	EditText fileNameText;
	
	Socket socket = null;
	String serverIPAddress = null;
	String serverPort = null;
	String customMessage = null;
	String endOfMessage = null;
	String perioString = null;
	
	String message = null;
	
	Timer timer = null;
	Date timestamp = null;
	Long period = null;
	
	File file = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accelerometer);		
		
		startStreaminButton = (Button)findViewById(R.id.startStreamingButton);
		sendCustomMessageButton = (Button)findViewById(R.id.sendCustomMessageButton);
		startRecordingButton = (Button)findViewById(R.id.startRecordingButton);
		
		ipAddressText = (EditText)findViewById(R.id.ipAddressText);
		serverPortText = (EditText)findViewById(R.id.serverPortText);
		customMessageText = (EditText)findViewById(R.id.customMessageText);	
		endOfMessageText = (EditText)findViewById(R.id.endOfMessageText);
		periodText = (EditText)findViewById(R.id.periodText);		
		
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
		
		startRecordingButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				startRecordingToTheFile();
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
        
        tvX.setText(String.format("%.3f", ax));
        tvY.setText(String.format("%.3f", ay));
        tvZ.setText(String.format("%.3f", az)); 
        
        if (event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];    
        }
                 
        message =String.format("%.3f", ax)+" "+String.format("%.3f", ay)+" "+String.format("%.3f", az);
        
        //sendMessage(message);
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void startStreaming() {
		if(streaming){
			streaming = false;
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			startRecordingButton.setEnabled(true);
			startStreaminButton.setText(R.string.startSendButton);
			sensorManager.unregisterListener(this);
			timer.cancel();
			timer = null;
		}			
		else {				
			streaming = true;
			startRecordingButton.setEnabled(false);
			serverIPAddress = ipAddressText.getText().toString();
			serverPort = serverPortText.getText().toString();
			endOfMessage =endOfMessageText.getText().toString();
			period = Long.valueOf(periodText.getText().toString());
			
			setSettings();
			
			startStreaminButton.setText(R.string.stopSendButton);
			sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
	        timer = new Timer();
	        timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					sendMessage(message);
				}
			}, 1000, period);
		}						     
	}
	
	protected void createNewFile() {
		fileNameText = (EditText)findViewById(R.id.fileNameText);
		String fileName = fileNameText.getText().toString();
		String fileNameTmp = null;
		File sdCard = Environment.getExternalStorageDirectory();  
		File directory= new File (sdCard.getAbsolutePath()+ '/' + getString(R.string.app_name));          
		directory.mkdirs();   
		fileNameTmp = fileName+'0';
		file = new File(directory,fileNameTmp+".txt");  	
		
		Integer i = 1;
		while (file.exists()) {
			fileNameTmp = fileName + (i++).toString();
			file = new File(directory,fileNameTmp+".txt"); 
		}
		
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }		
			
	}
	
	protected void startRecordingToTheFile() {	
		if(recording){
			//stop recording to the file
			recording = false;
			startStreaminButton.setEnabled(true);
			startRecordingButton.setText(R.string.startRecordButton);
			sensorManager.unregisterListener(this);
			timer.cancel();
			timer = null;
		}			
		else {		
			//start recording to the file
			createNewFile();
			recording = true;
			startStreaminButton.setEnabled(false);
			serverIPAddress = ipAddressText.getText().toString();
			serverPort = serverPortText.getText().toString();
			endOfMessage =endOfMessageText.getText().toString();
			period = Long.valueOf(periodText.getText().toString());
			
			setSettings();
			
			startRecordingButton.setText(R.string.stopRecordButton);
			sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
	        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
	        timer = new Timer();
	        timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					writeMessageToFile(message);
				}
			}, 1000, period);
		}		
		
	}
		
	private void sendCustomMessage() {						
		serverIPAddress = ipAddressText.getText().toString();
		serverPort = serverPortText.getText().toString();
		customMessage = customMessageText.getText().toString();
		period = Long.valueOf(periodText.getText().toString());
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
			            bw.write(message+endOfMessage);
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
	
	private void writeMessageToFile(final String message) {
		if(message.length()>0){
			try {				
		        OutputStream outputStream =new FileOutputStream(file, true);
		        OutputStreamWriter sw=new OutputStreamWriter(outputStream );
		        sw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date()) +' '+message+System.getProperty("line.separator") );
		        sw.close();
		        
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	  public void getSettings() {
		  SharedPreferences settings = getSharedPreferences("server", 0);		  
		  ipAddressText.setText(settings.getString("ip", "").toString());
		  serverPortText.setText(settings.getString("port", "").toString());
		  endOfMessageText.setText(settings.getString("endMessage", "").toString());
		  periodText.setText(settings.getString("period", "").toString());
	  }

	  public void setSettings() {
		  SharedPreferences settings = getSharedPreferences("server", 0);
		  SharedPreferences.Editor editor = settings.edit();
		  editor.putString("ip",serverIPAddress);
		  editor.putString("port",serverPort);
		  editor.putString("endMessage",endOfMessage);
		  editor.putString("period",period.toString());
		  editor.commit();
	  }
}
