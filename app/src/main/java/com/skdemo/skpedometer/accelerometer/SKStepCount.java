package com.skdemo.skpedometer.accelerometer;

/*
 * 根据StepDetector传入的步点"数"步子
 * */
public class SKStepCount implements SKStepCountListener {

    private int count = 0;
    private int mCount = 0;
    private SKStepValuePassListener mStepValuePassListener;
    private long timeOfLastPeak = 0;    // 上一个峰值
    private long timeOfThisPeak = 0;    // 当前的峰值
    private StepDetector stepDetector; // 步数检测器

    public SKStepCount() {
        stepDetector = new StepDetector();
        stepDetector.initListener(this); // 注册监听
    }

    public StepDetector getStepDetector() {
        return stepDetector;
    }

    /*
     * 连续走十步才会开始计步
     * 连续走了9步以下,停留超过3秒,则计数清空
     * */
    @Override
    public void countStep() {
        this.timeOfLastPeak = this.timeOfThisPeak;
        this.timeOfThisPeak = System.currentTimeMillis();
        if (this.timeOfThisPeak - this.timeOfLastPeak <= 3000L) {
            if (this.count < 9) {
                this.count++;
            } else if (this.count == 9) {
                this.count++;
                this.mCount += this.count;
                notifyListener();
            } else {
                this.mCount++;
                notifyListener();
            }
        } else {//超时
            this.count = 1;//为1,不是0
        }
    }

    // 设置步数变化监听者
    public void initListener(SKStepValuePassListener listener) {
        this.mStepValuePassListener = listener;
    }

    // 回调监听者
    public void notifyListener() {
        if (this.mStepValuePassListener != null) {
            this.mStepValuePassListener.stepChanged(this.mCount);
        }
    }

    // 供外部调用，初始化步数
    public void setSteps(int initValue) {
        this.mCount = initValue;
        this.count = 0;
        timeOfThisPeak = 0;
        timeOfLastPeak = 0;
        notifyListener();
    }
}
