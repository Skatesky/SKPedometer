package com.skdemo.skpedometer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.skdemo.skpedometer.Interface.UpdateUICallBack;
import com.skdemo.skpedometer.R;
import com.skdemo.skpedometer.accelerometer.SKStepCount;
import com.skdemo.skpedometer.accelerometer.SKStepValuePassListener;
import com.skdemo.skpedometer.activity.MainActivity;
import com.skdemo.skpedometer.bean.SKStepData;
import com.skdemo.skpedometer.utils.SKDataBaseUtil;
import com.skdemo.skpedometer.utils.SKSharedPreferencesUtil;
import com.skdemo.skpedometer.utils.SKStepConstant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import android.app.Service;

public class SKStepService extends Service implements SensorEventListener {

    private String TAG = "StepService";

    // 默认为30秒进行一次存储
    private static int duration = 30 * 1000;

    // 计步传感器类型  Sensor.TYPE_STEP_COUNTER或者Sensor.TYPE_STEP_DETECTOR
    private static int stepSensorType = -1;

    // 每次第一次启动记步服务时是否从系统中获取了已有的步数记录
    private boolean hasRecord = false;

    // 系统中获取到的已有的步数
    private int hasStepCount = 0;

    // 上一次的步数
    private int previousStepCount = 0;

    // 当前步数
    private int currentStep;

    // 加速度传感器中获取的步数
    private SKStepCount mStepCount;

    // 传感器管理对象
    private SensorManager sensorManager;

    // IBinder对象，向Activity传递数据的桥梁
    private StepBinder stepBinder = new StepBinder();

    // 保存记步计时器
    private TimeCount time;

    // 生命周期重写
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        // 初始化通知
        initNotification();
        // 初始化当天数据
        initTodayData();
        // 初始化广播
        initBroadcastReceiver();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 开启计步服务
                startStepService();
            }
        }).start();
        // 开始计时服务
        startTimeCount();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.stepBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    public class StepBinder extends Binder {
        /**
         * 获取当前service对象.
         *
         * @return the service
         */
        public SKStepService getService() {
            return SKStepService.this;
        }
    }

    // 初始化步数
    private void initTodayData() {
        CURRENT_DATE = getTodayDate();
        SKDataBaseUtil.createDataBase(this, "SKStepCount");
        SKDataBaseUtil.getLiteOrm().setDebugged(true);
        List<SKStepData> list = SKDataBaseUtil.getQueryByWhere(SKStepData.class, "today", new String[]{CURRENT_DATE});
        if (list.size() == 0 || list.isEmpty()) {
            currentStep = 0;
        } else if (list.size() == 1) {
            Log.v(TAG, "StepDate=" + list.get(0).toString());
            currentStep = Integer.parseInt(list.get(0).getStep());
        } else {
            Log.v(TAG, "出错了!");
        }
        if (mStepCount != null) {
            mStepCount.setSteps(currentStep);
        }
        updateNotification();
    }

    // 计时器
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }
    }

    // 开始保存记步数据
    private void startTimeCount() {
        if (time == null) {
            time = new TimeCount(duration, 100);
        }
        time.start();
    }

    // 获取当前步数
    public int getStepCount() {
        return currentStep;
    }

    // =========== SensorEventListener 接口重写 =================
    @Override
    public void onAccuracyChanged(Sensor var1, int var2) {

    }

    /**
     * 传感器监听回调
     * 记步的关键代码
     * 1. TYPE_STEP_COUNTER API的解释说返回从开机被激活后统计的步数，当重启手机后该数据归零，
     * 该传感器是一个硬件传感器所以它是低功耗的。
     * 为了能持续的计步，请不要反注册事件，就算手机处于休眠状态它依然会计步。
     * 当激活的时候依然会上报步数。该sensor适合在长时间的计步需求。
     * <p>
     * 2.TYPE_STEP_DETECTOR翻译过来就是走路检测，
     * API文档也确实是这样说的，该sensor只用来监监测走步，每次返回数字1.0。
     * 如果需要长事件的计步请使用TYPE_STEP_COUNTER。
     *
     * @param event
     */
    public void onSensorChanged(SensorEvent event) {
        if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
            // 获取当前传感器返回的临时步数
            int tempStep = (int) event.values[0];
            // 首次如果没有获取手机系统中已有的步数则获取一次系统中APP还未开始记步的步数
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                // 获取APP打开到现在的总步数=本次系统回调的总步数-APP打开之前已有的步数
                int thisStepCount = tempStep - hasStepCount;
                // 本次有效步数=（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                int thisStep = thisStepCount - previousStepCount;
                // 总步数=现有的步数+本次有效步数
                currentStep += (thisStep);
                // 记录最后一次APP打开到现在的总步数
                previousStepCount = thisStepCount;
            }
            Log.d(TAG, "tempStep" + tempStep);
        } else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0) {
                currentStep++;
            }
        }
        updateNotification();
    }

    // =========  通知相关 =======
    // UI监听器对象
    private UpdateUICallBack mCallback;

    // 注册UI更新监听
    public void registerCallBack(UpdateUICallBack callBack) {
        this.mCallback = callBack;
    }

    // 记步Notification的ID
    int notifyIdOfStep = 100;

    // 提醒锻炼的Notification的ID
    int notifyIdOfRemind = 200;

    public static final String CHANNEL_ID = "default";
    private static final CharSequence CHANNEL_NAME        = "Default Channel";
    private static final String CHANNEL_DESCRIPTION = "this is default channel!";

    // 通知构建者
    private NotificationCompat.Builder mBuilder;

    private NotificationChannel mNotificationChannel;

    // 通知管理对象
    private NotificationManager mNotificationManager;

    // 注册通知
    private void initNotification() {

        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);

        mBuilder.setContentTitle("计步器")
                .setContentText("今日步数" + currentStep + " 步")
                .setContentIntent(getDefaultIntent(Notification.FLAG_ONGOING_EVENT))
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setPriority(Notification.PRIORITY_DEFAULT)//设置该通知优先级
                .setAutoCancel(false)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(true)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setSmallIcon(R.mipmap.logo);
        Notification notification = mBuilder.build();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(notifyIdOfStep, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationChannel.setDescription(CHANNEL_DESCRIPTION);
            mNotificationManager.createNotificationChannel(mNotificationChannel);
        }

        Log.d(TAG, "initNotification()");
    }

    // 更新步数通知
    private void updateNotification() {
        // 设置点击跳转
        Intent hangIntent = new Intent(this, MainActivity.class);
        PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = mBuilder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("今日步数" + currentStep + " 步")
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setContentIntent(hangPendingIntent)
                .build();
        mNotificationManager.notify(notifyIdOfStep, notification);
        if (mCallback != null) {
            mCallback.updateUI(currentStep);
        }
        Log.d(TAG, "updateNotification()");
    }

    // 提醒锻炼通知栏
    private void remindNotify() {
        // 设置点击跳转
        Intent hangIntent = new Intent(this, MainActivity.class);
        PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Integer planStep = this.getSharedPreferences(SKStepConstant.kSharePreferenceName, Context.MODE_MULTI_PROCESS).getInt(SKStepConstant.kPlanStepKey, SKStepConstant.kDefaultPlanStep);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "ccc");
        mBuilder.setContentTitle("今日步数" + currentStep + " 步")
                .setContentText("距离目标还差" + (planStep - currentStep) + "步，加油！")
                .setContentIntent(hangPendingIntent)
                .setTicker(getResources().getString(R.string.app_name) + "提醒您开始锻炼了")//通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setPriority(Notification.PRIORITY_DEFAULT)//设置该通知优先级
                .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：
                //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.mipmap.logo);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyIdOfRemind, mBuilder.build());
    }

    /**
     * @获取默认的pendingIntent,为了防止2.3及以下版本报错
     * @flags属性: 在顶部常驻:Notification.FLAG_ONGOING_EVENT
     * 点击去除： Notification.FLAG_AUTO_CANCEL
     */
    public PendingIntent getDefaultIntent(int flags) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }

    // ========= 广播相关 ======
    // 广播接受者
    private BroadcastReceiver mBatInfoReceiver;

    // 注册广播
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        // 监听时间的变化
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "screen off");
                    //改为60秒一存储
                    duration = 60000;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d(TAG, "screen unlock");
//                    save();
                    //改为30秒一存储
                    duration = 30000;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                    Log.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //保存一次
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                    Log.i(TAG, " receive ACTION_SHUTDOWN");
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//日期变化步数重置为0
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //时间变化步数重置为0
                    isCall();
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//日期变化步数重置为0
                    isCall();
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                }
            }
        };
    }

    // ========= 计步服务 ======
    // 获取传感器实例
    private void startStepService() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        // 获取传感器管理器的实例
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        // android4.4以后可以使用计步传感器
        int versionCodes = Build.VERSION.SDK_INT;
        if (versionCodes >= 19) {
            addCountStepListener();
        } else {
            addBasePedometerListener();
        }
    }

    /**
     * 添加传感器监听
     * 1. TYPE_STEP_COUNTER API的解释说返回从开机被激活后统计的步数，当重启手机后该数据归零，
     * 该传感器是一个硬件传感器所以它是低功耗的。
     * 为了能持续的计步，请不要反注册事件，就算手机处于休眠状态它依然会计步。
     * 当激活的时候依然会上报步数。该sensor适合在长时间的计步需求。
     * <p>
     * 2.TYPE_STEP_DETECTOR翻译过来就是走路检测，
     * API文档也确实是这样说的，该sensor只用来监监测走步，每次返回数字1.0。
     * 如果需要长事件的计步请使用TYPE_STEP_COUNTER。
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            Log.v(TAG, "Sensor.TYPE_STEP_COUNTER");
            sensorManager.registerListener(SKStepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            Log.v(TAG, "Sensor.TYPE_STEP_DETECTOR");
            sensorManager.registerListener(SKStepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else  {
            Log.v(TAG, "Count sensor not available!");
            addBasePedometerListener();
        }
    }

    // 通过加速度传感器来记步
    private void addBasePedometerListener() {
        mStepCount = new SKStepCount();
        mStepCount.setSteps(currentStep);
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean isAvailable = sensorManager.registerListener(mStepCount.getStepDetector(), sensor, SensorManager.SENSOR_DELAY_UI);
        mStepCount.initListener(new SKStepValuePassListener() {
            @Override
            public void stepChanged(int steps) {
                currentStep = steps;
                updateNotification();
            }
        });
        if (isAvailable) {
            Log.v(TAG, "加速度传感器可以使用");
        } else {
            Log.v(TAG, "加速度传感器无法使用");
        }
    }

    // ======== 数据存储 =======
    // 当前的日期
    private static String CURRENT_DATE = "";

    // 保存记步数据
    private void save() {
        int tempStep = currentStep;

        List<SKStepData> list = SKDataBaseUtil.getQueryByWhere(SKStepData.class, "today", new String[]{CURRENT_DATE});
        if (list.size() == 0 || list.isEmpty()) {
            SKStepData data = new SKStepData();
            data.setToday(CURRENT_DATE);
            data.setStep(tempStep + "");
            SKDataBaseUtil.insert(data);
        } else if (list.size() == 1) {
            SKStepData data = list.get(0);
            data.setStep(tempStep + "");
            SKDataBaseUtil.update(data);
        } else {

        }
    }

    // ====== 其他私有方法 ======
    // 监听晚上0点变化初始化数据
    private void isNewDay() {
        String time = "00:00";
        if (time.equals(new SimpleDateFormat("HH:mm").format(new Date())) || !CURRENT_DATE.equals(getTodayDate())) {
            initTodayData();
        }
    }

    // 获取当天日期
    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDate.format(date);
    }

    // 监听时间变化提醒用户锻炼
    private void isCall() {
        String time = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("achieveTime", "21:00");
        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        String remind = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("remind", "1");
        Log.d(TAG, "time=" + time + "\n" +
                "new SimpleDateFormat(\"HH: mm\").format(new Date()))=" + new SimpleDateFormat("HH:mm").format(new Date()));
        if (("1".equals(remind)) &&
                (currentStep < Integer.parseInt(plan)) &&
                (time.equals(new SimpleDateFormat("HH:mm").format(new Date())))
        ) {
            remindNotify();
        }
    }


}
