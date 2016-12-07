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
	 * 记步算法引入的变量
	 *
	 *当前传感器的值
    float gravityNew = 0;
    //初始范围
    float minValue = 11f;
    float maxValue = 19.6f;   	
	/**
	 * 引入完毕
	 */
	public static float average = 0;
	 //上次传感器的值
    float gravityOld = 0;
	 //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
  //当前的时间
    long timeOfNow = 0;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //初始阈值
    float ThreadValue = (float) 2.0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
  //持续上升次数
    int continueUpCount = 0;
  //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float initialValue = (float) 1.7;
    //存放三轴数据
    final int valueNum = 5;
  //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    //打印要用到的tag
    private final String TAG = "StepDcretor";
    /**
     * 0-准备计时   1-计时中   2-正常计步中
     */
    private int CountTimeState = 0;
    // 倒计时3.5秒，3.5秒内不会显示计步，用于屏蔽细微波动
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
	 * 程序的启动顺序：onCreate()->onStart()->onResume();
	 */
	@Override
	protected void onResume() {
		super.onResume();
		activityRunning = true;
		Sensor countSensor = SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (countSensor != null) {
			SensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
			Toast.makeText(MainActivity.this, "加速度传感器注册成功", 0).show();
		} else {
			Toast.makeText(this, "您的手机不支持加速度传感器", Toast.LENGTH_LONG).show();
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
			count.setText("传感器已关闭！");
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	/**
	 * calc_step()方法算出加速度传感器的x、y、z三轴的平均数值（为了平衡在某一个方向数值过大造成的数据误差）
	 */
	synchronized private void calc_step(SensorEvent event) {
		average = (float) Math
				.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
		DetectorNewStep(average);
	}

	/**
	 * DetectorNewStep()方法针对波峰和波谷进行检测
	 */
	/*
	 * 检测步子，并开始计步 
	 * 1.传入sersor中的数据 
	 * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
	 * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
	 */
	public void DetectorNewStep(float values) {
		if (gravityOld == 0.0) {
			gravityOld = values;
		} else {
			//DetectorPeak(1,2)方法用于检测波峰  1:当前的值  2:上一点的值 如果返回true表明该点是波峰
			if (DetectorPeak(values, gravityOld)) {
				timeOfLastPeak = timeOfThisPeak;
				timeOfNow = System.currentTimeMillis();
				if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleyOfWave >= ThreadValue)
						&& timeOfNow - timeOfLastPeak <= 2000) {
					timeOfThisPeak = timeOfNow;
					// 更新界面的处理，不涉及到算法
					preStep();
				}
				//initialValue:动态阈值需要动态的数据，这个值用于这些动态数据的阈值
				if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleyOfWave >= initialValue)) {
					timeOfThisPeak = timeOfNow;
					//Peak_Valley_Thread()方法用于阈值的计算
					ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
				}
			}
		}
		gravityOld = values;
	}
	
	/*
     * DetectorPeak()方法用于检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于1.2g,小于2g
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     * */
    public boolean DetectorPeak(float newValue, float oldValue) {
    	//isDerectionUp:是否上升的标志位;lastStatus:上一点的状态，上升还是下降
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            //持续上升次数
            continueUpCount++;
        } else {
        	//continueUpFormerCount：上一点的持续上升的次数，为了记录波峰的上升次数
            continueUpFormerCount = continueUpCount;
          //持续上升次数
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
     * Peak_Valley_Thread()方法用于阈值的计算
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.将数组传入函数averageValue中计算阈值
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
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8) {
            Log.v(TAG, "超过8");
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
     * preStep这个方法通过变量CountTimeState，将计步分为了三种模式，
     * CountTimeState=0时代表还未开启计步器。
     * CountTimeState=1时代表预处理模式，
     * 也就是说TEMP_STEP步数如果在规定的时间内一直在增加，直到这个模式结束，那么TEMP_STEP值有效，
     * 反之，无效舍弃，目的是为了过滤点一些手机晃动带来的影响。
     * CountTimeState=2时代表正常计步模式
     */
    private void preStep() {
        if (CountTimeState == 0) {
            // 开启计时器 这里duration=1000
            time = new TimeCount(duration, 100);
            time.start();
            CountTimeState = 1;
            Log.v(TAG, "开启计时器");
        } else if (CountTimeState == 1) {
        	//TEMP_STEP:暂时保存步数，不显示
            TEMP_STEP++;
            Log.v(TAG, "计步中 TEMP_STEP:" + TEMP_STEP);
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
    	 * 这个方法会在millisInFuture时间段内，每隔countDownInterval的时间，调用onTick()方法
    	 * CountdownTimer(millisInFuture, countDownInterval)
    	 * @param millisInFuture：总时间
    	 * @param countDownInterval:onTick()方法调用的间隔
    	 */
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        /**
         *onFinish():倒计时完成时被调用    
         */
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
        	// 取消未结束的time任务
            time.cancel();
            // 把临时步数转换成确定步数	
            CURRENT_SETP += TEMP_STEP;
            lastStep = -1;
            Log.v(TAG, "计时正常结束");
            timer = new Timer(true);
           
            TimerTask task = new TimerTask() {
                public void run() {
                    if (lastStep == CURRENT_SETP) {
                        timer.cancel();
                        CountTimeState = 0;
                        lastStep = -1;
                        TEMP_STEP = 0;
                        Log.v(TAG, "停止计步：" + CURRENT_SETP);
                    } else {
                        lastStep = CURRENT_SETP;
                    }
                }
            };
            /**
             * timer.schedule(1;2;3)
             * 1:需要执行的方法
             * 2:设定延迟时间
             * 3:设定周期
             */
            timer.schedule(task, 0, 3000);
            CountTimeState = 2;
        }

        @Override
        /**
         * millisUntilFinished:倒计时的剩余时间。
         */
        public void onTick(long millisUntilFinished) {
            if (lastStep == TEMP_STEP) {
                Log.v(TAG, "onTick 计时停止");
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
