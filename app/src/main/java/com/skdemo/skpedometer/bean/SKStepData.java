package com.skdemo.skpedometer.bean;

import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

@Table("step")
public class SKStepData {

    // 指定自增，每个对象需要一个主键
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    private String today;

    private String step;

    public int getId() { return id; }

    public void setId(int id) {
        this.id = id;
    }

    public String getToday() { return today; }

    public void setToday(String today) {
        this.today = today;
    }

    public String getStep() { return step; }

    public void setStep(String step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "StepData{" + "id=" + id + ", today='" + today + '\'' + ", step='" + step + '\'' + '}';
    }
}
