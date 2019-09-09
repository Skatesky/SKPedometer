package com.skdemo.skpedometer.activity;

//import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.skdemo.skpedometer.Interface.UpdateUICallBack;
import com.skdemo.skpedometer.R;
import com.skdemo.skpedometer.service.SKStepService;
import com.skdemo.skpedometer.utils.SKSharedPreferencesUtil;
import com.skdemo.skpedometer.utils.SKStepConstant;
import com.skdemo.skpedometer.view.SKStepCircleView;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String kPlanWalkNumKey = "PlanWalkNum";

    private TextView planTextView;
    private TextView historyTextView;
    private SKStepCircleView stepCircleView;
    private TextView suportTextView;

    private SKSharedPreferencesUtil preferencesUtil;

    private void assignViews() {
        planTextView = (TextView) findViewById(R.id.tv_plan);
        historyTextView = (TextView) findViewById(R.id.tv_history);
        stepCircleView = (SKStepCircleView) findViewById(R.id.StepCircleView);
        suportTextView = (TextView) findViewById(R.id.tv_isSupport);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 绑定视图
        assignViews();
        // 初始化数据
        initData();
        // 监听
        addListener();
        Log.d("MainActivity", "onCreate");
    }

    private void initData() {
        preferencesUtil = new SKSharedPreferencesUtil(this);

        String planWalkNum = (String) preferencesUtil.getParam(kPlanWalkNumKey, "7000");
        stepCircleView.setCurrentCount(Integer.parseInt(planWalkNum), 0);
        suportTextView.setText("计步中...");
        setupService();
    }

    private void addListener() {
        planTextView.setOnClickListener(this);
        historyTextView.setOnClickListener(this);
    }

    private boolean isBind = false;

    // 开启计步服务
    private void setupService() {
        Intent intent = new Intent(this, SKStepService.class);
        isBind = bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    /**
     * 用于查询应用服务（application Service）的状态的一种interface，
     * 更详细的信息可以参考Service 和 context.bindService()中的描述，
     * 和许多来自系统的回调方式一样，ServiceConnection的方法都是进程的主线程中调用的。
     */
    ServiceConnection connection = new ServiceConnection() {

        /**
         * 在建立起于Service的连接时会调用该方法，目前Android是通过IBind机制实现与服务的连接。
         * @param componentName 实际所连接到的Service组件名称
         * @param iBinder 服务的通信信道的IBind，可以通过Service访问对应服务
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final SKStepService stepService = ((SKStepService.StepBinder) iBinder).getService();
            // 设置初始化数据
            Integer planWalk = (Integer) preferencesUtil.getParam(SKStepConstant.kPlanStepKey, SKStepConstant.kDefaultPlanStep);
            stepCircleView.setCurrentCount(planWalk, stepService.getStepCount());

            // 设置步数监听UI回调
            stepService.registerCallBack(new UpdateUICallBack() {
                @Override
                public void updateUI(int stepCount) {
                    // 更新UI
                    Integer planWalk = (Integer) preferencesUtil.getParam(SKStepConstant.kPlanStepKey, SKStepConstant.kDefaultPlanStep);
                    stepCircleView.setCurrentCount(planWalk, stepCount);
                }
            });
        }

        /**
         * 当与Service之间的连接丢失的时候会调用该方法，
         * 这种情况经常发生在Service所在的进程崩溃或者被Kill的时候调用，
         * 此方法不会移除与Service的连接，当服务重新启动的时候仍然会调用 onServiceConnected()。
         * @param componentName 丢失连接的组件名称
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    // View.OnClickListener
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_history: // 历史页面
                startActivity(new Intent(this, HistoryActivity.class));
                break;
            case R.id.tv_plan:  // 计划页面
                startActivity(new Intent(this, PlanActivity.class));
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBind) {
            this.unbindService(connection);
        }
    }
}
