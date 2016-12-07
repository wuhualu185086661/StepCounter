package com.example.stepcounterdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener{
	
	private SensorManager SensorManager;
	private TextView count;
	boolean activityRunning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		count=(TextView)findViewById(R.id.count);
		SensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		activityRunning=true;
		Sensor countSensor=SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if(countSensor!=null){
			SensorManager.registerListener(this, countSensor,SensorManager.SENSOR_DELAY_UI);
		}else{
			Toast.makeText(this, "您的手机不支持加速度传感器", Toast.LENGTH_LONG).show();
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		activityRunning=false;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(activityRunning){
			count.setText(String.valueOf(event.values[0]));
		}
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

}
