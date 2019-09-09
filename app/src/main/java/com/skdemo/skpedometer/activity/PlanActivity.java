package com.skdemo.skpedometer.activity;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.skdemo.skpedometer.R;
import com.skdemo.skpedometer.utils.SKSharedPreferencesUtil;
import com.skdemo.skpedometer.utils.SKStepConstant;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PlanActivity extends Activity implements View.OnClickListener,TextWatcher {

    private SKSharedPreferencesUtil preferencesUtil;

    private ImageView leftImageView;    // 左边图片
    private ImageView rightImageView;   // 右边的图片
    private EditText stepEditText;  // 步数编辑
    private CheckBox remindCheckBox;    // 开关
    private TextView timeTextView;  // 提醒时间
    private Button saveButton;  // 保存

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);
        assignViews();
        addListener();
        initData();
    }

    private void assignViews() {
        leftImageView = (ImageView) findViewById(R.id.iv_left);
        rightImageView = (ImageView) findViewById(R.id.iv_right);
        stepEditText = (EditText) findViewById(R.id.edit_step);
        remindCheckBox = (CheckBox) findViewById(R.id.cb_remind);
        saveButton = (Button) findViewById(R.id.btn_save);
        timeTextView = (TextView) findViewById(R.id.tv_remind_time);
    }

    private void addListener() {
        leftImageView.setOnClickListener(this);
        rightImageView.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        timeTextView.setOnClickListener(this);
        stepEditText.addTextChangedListener(this);
    }

    /**
     * 读取锻炼计划
     */
    public void initData() {
        preferencesUtil = new SKSharedPreferencesUtil(this);
        Integer planStep = (Integer) preferencesUtil.getParam(SKStepConstant.kPlanStepKey, SKStepConstant.kDefaultPlanStep);
        Boolean isRemind = (Boolean) preferencesUtil.getParam(SKStepConstant.kPlanIsRemindKey, true);
        String remindTime = (String) preferencesUtil.getParam(SKStepConstant.kPlanRemingTimeKey, "22:00");
        if (planStep == 0) {
            stepEditText.setText(String.valueOf(SKStepConstant.kDefaultPlanStep));
        } else {
            stepEditText.setText(String.valueOf(planStep));
        }
        remindCheckBox.setChecked(isRemind);
        if (!remindTime.isEmpty()) {
            timeTextView.setText(remindTime);
        }
    }

    // View.OnClickListener
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_left:
                finish();
                break;
            case R.id.btn_save:
                save();
                break;
            case R.id.tv_remind_time:
                showTimePicker();
                break;
        }
    }

    private void save() {
        String planStep = stepEditText.getText().toString().trim();
        boolean isRemind = remindCheckBox.isChecked();
        String remindTime = timeTextView.getText().toString().trim();
        if (planStep.isEmpty() || "0".equals(planStep)) {
            preferencesUtil.setParam(SKStepConstant.kPlanStepKey, SKStepConstant.kDefaultPlanStep);
        } else {
            preferencesUtil.setParam(SKStepConstant.kPlanStepKey, Integer.parseInt(planStep));
        }
        preferencesUtil.setParam(SKStepConstant.kPlanIsRemindKey, isRemind);
        if (remindTime.isEmpty()) {
            preferencesUtil.setParam(SKStepConstant.kPlanRemingTimeKey, "22:00");
        } else {
            preferencesUtil.setParam(SKStepConstant.kPlanRemingTimeKey, remindTime);
        }
        finish();
    }

    private void showTimePicker() {

        final Calendar calendar = Calendar.getInstance(Locale.CHINA);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        final DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectHour, int selectMinute) {
                calendar.set(Calendar.HOUR_OF_DAY, selectHour);
                calendar.set(Calendar.MINUTE, selectMinute);
                String remindTime = calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE);
                Date date = null;
                try {
                    date = dateFormat.parse(remindTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (date != null) {
                    calendar.setTime(date);
                }
                timeTextView.setText(dateFormat.format(date));
            }
        }, hour, minute, true).show();
    }

    @Override
    public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence sequence, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        Log.d("PlanActivity", "afterTextChanged");
        String text = editable.toString();
        if (text.indexOf("\r")>=0 || text.indexOf("\n")>=0) { //发现输入回车符或换行符
            stepEditText.setText(text.replace("\r", "").replace("\n", ""));

            InputMethodManager manager = ((InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE));
            if (manager != null) {
                manager.hideSoftInputFromWindow(stepEditText.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
}
