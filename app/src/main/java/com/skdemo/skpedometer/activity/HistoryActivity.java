package com.skdemo.skpedometer.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.skdemo.skpedometer.R;
import com.skdemo.skpedometer.adapter.CommonAdapter;
import com.skdemo.skpedometer.adapter.CommonViewHolder;
import com.skdemo.skpedometer.bean.SKStepData;
import com.skdemo.skpedometer.utils.SKDataBaseUtil;

import java.util.List;
import java.util.logging.Logger;

/**
 * 历史页面.
 */
public class HistoryActivity extends Activity {

    private static final String TAG = "History";

    private LinearLayout layoutTitleBar;
    private ImageView leftImageView;
    private ImageView rightImageView;
    private ListView historyListView; // 历史列表

    private void assignViews() {
        layoutTitleBar = (LinearLayout) findViewById(R.id.layout_titleBar);
        leftImageView = (ImageView) findViewById(R.id.iv_left);
        rightImageView = (ImageView) findViewById(R.id.iv_right);
        historyListView = (ListView) findViewById(R.id.lv_history);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        assignViews();
        leftImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        initData();
    }

    private void initData() {
        setEmptyView(historyListView);
        if (SKDataBaseUtil.getLiteOrm()==null) {
            SKDataBaseUtil.createDataBase(this, "jingzhi");
        }
        List<SKStepData> stepDatas = SKDataBaseUtil.getQueryAll(SKStepData.class);
        Log.d(TAG, "stepDatas="+stepDatas);
        historyListView.setAdapter(new CommonAdapter<SKStepData>(this, stepDatas, R.layout.item) {
            @Override
            protected void convertView(View item, SKStepData stepData) {
                TextView dateTextView = CommonViewHolder.get(item, R.id.tv_date);
                TextView stepTextView = CommonViewHolder.get(item, R.id.tv_step);
                dateTextView.setText(stepData.getToday());
                stepTextView.setText(stepData.getStep()+"步");
            }
        });
    }

    protected <T extends View> T setEmptyView(ListView listView) {
        TextView emptyView = new TextView(this);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setText("暂无数据！");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        emptyView.setVisibility(View.GONE);
        ((ViewGroup) listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);
        return (T) emptyView;
    }
}
