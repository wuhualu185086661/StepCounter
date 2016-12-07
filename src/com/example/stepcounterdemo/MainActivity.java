package com.example.stepcounterdemo;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager SensorManager;
	private TextView count;
	boolean activityRunning;
	/**
	 * �ǲ��㷨����ı���
	 *
	 *��ǰ��������ֵ
    float gravityNew = 0;
    //��ʼ��Χ
    float minValue = 11f;
    float maxValue = 19.6f;   	
	/**
	 * �������
	 */
	public static float average = 0;
	 //�ϴδ�������ֵ
    float gravityOld = 0;
	 //�˴β����ʱ��
    long timeOfThisPeak = 0;
    //�ϴβ����ʱ��
    long timeOfLastPeak = 0;
  //��ǰ��ʱ��
    long timeOfNow = 0;
    //����ֵ
    float peakOfWave = 0;
    //����ֵ
    float valleyOfWave = 0;
    //��ʼ��ֵ
    float ThreadValue = (float) 2.0;
    //�Ƿ������ı�־λ
    boolean isDirectionUp = false;
    //��һ���״̬�����������½�
    boolean lastStatus = false;
  //������������
    int continueUpCount = 0;
  //��һ��ĳ��������Ĵ�����Ϊ�˼�¼�������������
    int continueUpFormerCount = 0;
    //��̬��ֵ��Ҫ��̬�����ݣ����ֵ������Щ��̬���ݵ���ֵ
    final float initialValue = (float) 1.7;
    //�����������
    final int valueNum = 5;
  //���ڴ�ż�����ֵ�Ĳ��岨�Ȳ�ֵ
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    //��ӡҪ�õ���tag
    private final String TAG = "StepDcretor";
    /**
     * 0-׼����ʱ   1-��ʱ��   2-�����Ʋ���
     */
    private int CountTimeState = 0;
    // ����ʱ3.5�룬3.5���ڲ�����ʾ�Ʋ�����������ϸ΢����
//    private long duration = 3500;
    private long duration=1000;
    private Timer timer;
    private TimeCount time;
    public static int TEMP_STEP = 0;
    public static int CURRENT_SETP = 0;
    OnSensorChangeListener onSensorChangeListener;
    private int lastStep = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		count = (TextView) findViewById(R.id.count);
		SensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	}

	/**
	 * ���������˳��onCreate()->onStart()->onResume();
	 */
	@Override
	protected void onResume() {
		super.onResume();
		activityRunning = true;
		Sensor countSensor = SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (countSensor != null) {
			SensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
			Toast.makeText(MainActivity.this, "���ٶȴ�����ע��ɹ�", 0).show();
		} else {
			Toast.makeText(this, "�����ֻ���֧�ּ��ٶȴ�����", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		activityRunning = false;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		count.setText(String.valueOf(CURRENT_SETP));
		if (activityRunning) {
			Sensor sensor = event.sensor;
			synchronized (this) {
				if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					calc_step(event);
				}
			}
		} else {
			count.setText("�������ѹرգ�");
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	/**
	 * calc_step()����������ٶȴ�������x��y��z�����ƽ����ֵ��Ϊ��ƽ����ĳһ��������ֵ������ɵ�������
	 */
	synchronized private void calc_step(SensorEvent event) {
		average = (float) Math
				.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
		DetectorNewStep(average);
	}

	/**
	 * DetectorNewStep()������Բ���Ͳ��Ƚ��м��
	 */
	/*
	 * ��ⲽ�ӣ�����ʼ�Ʋ� 
	 * 1.����sersor�е����� 
	 * 2.�����⵽�˲��壬���ҷ���ʱ����Լ���ֵ�����������ж�Ϊ1��
	 * 3.����ʱ������������岨�Ȳ�ֵ����initialValue���򽫸ò�ֵ������ֵ�ļ�����
	 */
	public void DetectorNewStep(float values) {
		if (gravityOld == 0.0) {
			gravityOld = values;
		} else {
			//DetectorPeak(1,2)�������ڼ�Ⲩ��  1:��ǰ��ֵ  2:��һ���ֵ �������true�����õ��ǲ���
			if (DetectorPeak(values, gravityOld)) {
				timeOfLastPeak = timeOfThisPeak;
				timeOfNow = System.currentTimeMillis();
				if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleyOfWave >= ThreadValue)
						&& timeOfNow - timeOfLastPeak <= 2000) {
					timeOfThisPeak = timeOfNow;
					// ���½���Ĵ������漰���㷨
					preStep();
				}
				//initialValue:��̬��ֵ��Ҫ��̬�����ݣ����ֵ������Щ��̬���ݵ���ֵ
				if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleyOfWave >= initialValue)) {
					timeOfThisPeak = timeOfNow;
					//Peak_Valley_Thread()����������ֵ�ļ���
					ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
				}
			}
		}
		gravityOld = values;
	}
	
	/*
     * DetectorPeak()�������ڼ�Ⲩ��
     * �����ĸ������ж�Ϊ���壺
     * 1.Ŀǰ��Ϊ�½������ƣ�isDirectionUpΪfalse
     * 2.֮ǰ�ĵ�Ϊ���������ƣ�lastStatusΪtrue
     * 3.������Ϊֹ�������������ڵ���2��
     * 4.����ֵ����1.2g,С��2g
     * ��¼����ֵ
     * 1.�۲첨��ͼ�����Է����ڳ��ֲ��ӵĵط������ȵ���һ�����ǲ��壬�бȽ����Ե������Լ���ֵ
     * 2.����Ҫ��¼ÿ�εĲ���ֵ��Ϊ�˺��´εĲ������Ա�
     * */
    public boolean DetectorPeak(float newValue, float oldValue) {
    	//isDerectionUp:�Ƿ������ı�־λ;lastStatus:��һ���״̬�����������½�
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            //������������
            continueUpCount++;
        } else {
        	//continueUpFormerCount����һ��ĳ��������Ĵ�����Ϊ�˼�¼�������������
            continueUpFormerCount = continueUpCount;
          //������������
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 && (oldValue >= 11.76 && oldValue < 19.6))) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }
    
    /*
     * Peak_Valley_Thread()����������ֵ�ļ���
     * 1.ͨ�����岨�ȵĲ�ֵ������ֵ
     * 2.��¼4��ֵ������tempValue[]������
     * 3.�����鴫�뺯��averageValue�м�����ֵ
     * */
    public float Peak_Valley_Thread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < valueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, valueNum);
            for (int i = 1; i < valueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[valueNum - 1] = value;
        }
        return tempThread;

    }
    
    /*
     * �ݶȻ���ֵ
     * 1.��������ľ�ֵ
     * 2.ͨ����ֵ����ֵ�ݶȻ���һ����Χ��
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8) {
            Log.v(TAG, "����8");
            ave = (float) 4.3;
        } else if (ave >= 7 && ave < 8) {
            Log.v(TAG, "7-8");
            ave = (float) 3.3;
        } else if (ave >= 4 && ave < 7) {
            Log.v(TAG, "4-7");
            ave = (float) 2.3;
        } else if (ave >= 3 && ave < 4) {
            Log.v(TAG, "3-4");
            ave = (float) 2.0;
        } else {
            Log.v(TAG, "else");
            ave = (float) 1.7;
        }
        return ave;
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
    /**
     * preStep�������ͨ������CountTimeState�����Ʋ���Ϊ������ģʽ��
     * CountTimeState=0ʱ����δ�����Ʋ�����
     * CountTimeState=1ʱ����Ԥ����ģʽ��
     * Ҳ����˵TEMP_STEP��������ڹ涨��ʱ����һֱ�����ӣ�ֱ�����ģʽ��������ôTEMP_STEPֵ��Ч��
     * ��֮����Ч������Ŀ����Ϊ�˹��˵�һЩ�ֻ��ζ�������Ӱ�졣
     * CountTimeState=2ʱ���������Ʋ�ģʽ
     */
    private void preStep() {
        if (CountTimeState == 0) {
            // ������ʱ�� ����duration=1000
            time = new TimeCount(duration, 100);
            time.start();
            CountTimeState = 1;
            Log.v(TAG, "������ʱ��");
        } else if (CountTimeState == 1) {
        	//TEMP_STEP:��ʱ���沽��������ʾ
            TEMP_STEP++;
            Log.v(TAG, "�Ʋ��� TEMP_STEP:" + TEMP_STEP);
        } else if (CountTimeState == 2) {
            CURRENT_SETP++;
            count.setText(String.valueOf(CURRENT_SETP));
            if (onSensorChangeListener != null) {
                onSensorChangeListener.onChange();
            }
        }
    }
    class TimeCount extends CountDownTimer {
    	/**
    	 * �����������millisInFutureʱ����ڣ�ÿ��countDownInterval��ʱ�䣬����onTick()����
    	 * CountdownTimer(millisInFuture, countDownInterval)
    	 * @param millisInFuture����ʱ��
    	 * @param countDownInterval:onTick()�������õļ��
    	 */
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        /**
         *onFinish():����ʱ���ʱ������    
         */
        public void onFinish() {
            // �����ʱ��������������ʼ�Ʋ�
        	// ȡ��δ������time����
            time.cancel();
            // ����ʱ����ת����ȷ������	
            CURRENT_SETP += TEMP_STEP;
            lastStep = -1;
            Log.v(TAG, "��ʱ��������");
            timer = new Timer(true);
           
            TimerTask task = new TimerTask() {
                public void run() {
                    if (lastStep == CURRENT_SETP) {
                        timer.cancel();
                        CountTimeState = 0;
                        lastStep = -1;
                        TEMP_STEP = 0;
                        Log.v(TAG, "ֹͣ�Ʋ���" + CURRENT_SETP);
                    } else {
                        lastStep = CURRENT_SETP;
                    }
                }
            };
            /**
             * timer.schedule(1;2;3)
             * 1:��Ҫִ�еķ���
             * 2:�趨�ӳ�ʱ��
             * 3:�趨����
             */
            timer.schedule(task, 0, 3000);
            CountTimeState = 2;
        }

        @Override
        /**
         * millisUntilFinished:����ʱ��ʣ��ʱ�䡣
         */
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
