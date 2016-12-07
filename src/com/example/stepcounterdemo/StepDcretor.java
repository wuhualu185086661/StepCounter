package com.example.stepcounterdemo;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

public class StepDcretor extends Activity implements SensorEventListener {  
    private final String TAG = "StepDcretor";  
    // alpha �� t / (t + dT)������������� t �ǵ�ͨ�˲�����ʱ�䳣����dT ���¼�����Ƶ��  
    private final float alpha = 0.8f;  
    private long perCalTime = 0;  
    private final float minValue = 8.8f;  
    private final float maxValue = 10.5f;  
    private final float verminValue = 9.5f;  
    private final float vermaxValue = 10.0f;  
    private final float minTime = 150;  
    private final float maxTime = 2000;  
    /** 
     * 0-׼����ʱ   1-��ʱ��  2-׼��Ϊ�����Ʋ���ʱ  3-�����Ʋ��� 
     */  
    private int CountTimeState = 0;  
    public static int CURRENT_SETP = 0;  
    public static int TEMP_STEP = 0;  
    private int lastStep = -1;  
    // ���ټƵ�����ά����ֵ  
    public static float[] gravity = new float[3];  
    public static float[] linear_acceleration = new float[3];  
    //������ά�������ƽ��ֵ  
    public static float average = 0;  
  
    private Timer timer;  
    // ����ʱ3�룬3���ڲ�����ʾ�Ʋ�����������ϸ΢����  
    private long duration = 3000;  
    private TimeCount time;  
  
    OnSensorChangeListener onSensorChangeListener;  
  
    public StepDcretor(Context context) {  
        super();  
    }  
  
    public void onSensorChanged(SensorEvent event) {  
        Sensor sensor = event.sensor;  
        synchronized (this) {  
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {  
  
                // �õ�ͨ�˲���������������ٶ�  
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];  
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];  
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];  
  
                average = (float) Math.sqrt(Math.pow(gravity[0], 2)  
                        + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));  
  
                if (average <= verminValue) {  
                    if (average <= minValue) {  
                        perCalTime = System.currentTimeMillis();  
                    }  
                } else if (average >= vermaxValue) {  
                    if (average >= maxValue) {  
                        float betweentime = System.currentTimeMillis()  
                                - perCalTime;  
                        if (betweentime >= minTime && betweentime < maxTime) {  
                            perCalTime = 0;  
                            if (CountTimeState == 0) {  
                                // ������ʱ��  
                                time = new TimeCount(duration, 1000);  
                                time.start();  
                                CountTimeState = 1;  
                                Log.v(TAG, "������ʱ��");  
                                Toast.makeText(this, "������ʱ��", Toast.LENGTH_SHORT).show();
                            } else if (CountTimeState == 1) {  
                                TEMP_STEP++;  
                                Log.v(TAG, "�Ʋ��� TEMP_STEP:" + TEMP_STEP); 
                                Toast.makeText(this, "�Ʋ��� TEMP_STEP:", Toast.LENGTH_SHORT).show();
                            } else if (CountTimeState == 2) {  
                                timer = new Timer(true);  
                                TimerTask task = new TimerTask() {  
                                    public void run() {  
                                        if (lastStep == CURRENT_SETP) {  
                                            timer.cancel();  
                                            CountTimeState = 0;  
                                            lastStep = -1;  
                                            TEMP_STEP = 0;  
                                            Log.v(TAG, "ֹͣ�Ʋ���" + CURRENT_SETP);  
                                            Toast.makeText(StepDcretor.this, "ֹͣ�Ʋ�: ", Toast.LENGTH_SHORT).show();
                                        } else {  
                                            lastStep = CURRENT_SETP;  
                                        }  
                                    }  
                                };  
                                timer.schedule(task, 0, 2000);  
                                CountTimeState = 3;  
                            } else if (CountTimeState == 3) {  
                                CURRENT_SETP++;  
                            }  
                        }  
                    }  
                }  
  
                if (onSensorChangeListener != null) {  
                    onSensorChangeListener.onChange();  
                }  
            }  
        }  
    }  
  
  
    public void onAccuracyChanged(Sensor arg0, int arg1) {  
  
    }  
  
    public interface OnSensorChangeListener {  
        void onChange();  
    }  
  
    public OnSensorChangeListener getOnSensorChangeListener() {  
        return onSensorChangeListener;  
    }  
  
    public void setOnSensorChangeListener(  
            OnSensorChangeListener onSensorChangeListener) {  
        this.onSensorChangeListener = onSensorChangeListener;  
    }  
  
    class TimeCount extends CountDownTimer {  
        public TimeCount(long millisInFuture, long countDownInterval) {  
            super(millisInFuture, countDownInterval);  
        }  
  
        @Override  
        public void onFinish() {  
            // �����ʱ��������������ʼ�Ʋ�  
            time.cancel();  
            CURRENT_SETP += TEMP_STEP;  
            lastStep = -1;  
            CountTimeState = 2;  
            Log.v(TAG, "��ʱ��������");  
            Toast.makeText(StepDcretor.this, "��ʱ�������� ", Toast.LENGTH_SHORT).show();
        }  
  
        @Override  
        public void onTick(long millisUntilFinished) {  
            if (lastStep == TEMP_STEP) {  
                Log.v(TAG, "onTick ��ʱֹͣ");  
                time.cancel();  
                CountTimeState = 0;  
                lastStep = -1;  
                TEMP_STEP = 0;  
            } else {  
                lastStep = TEMP_STEP;  
            }  
        }  
  
    }  
}  

