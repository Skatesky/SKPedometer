package com.skdemo.skpedometer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * 万能适配器.
 *
 * @param <T> the type parameter
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
    private Context context;
    private List<T> datas;
    private int layoutId;

    public CommonAdapter(Context context, List<T> datas, int layoutId) {
        this.context = context;
        this.datas = datas;
        this.layoutId = layoutId;
    }

    // 复写Adapter方法
    @Override
    public int getCount(){
        return datas == null ? 0 : datas.size();
    }

    @Override
    public T getItem(int position) {
        return datas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutId, null);
        }
        T t = getItem(position);
        convertView(convertView, t);
        return convertView;
    }

    /**
     * 需要去实现的对item中的view的设置操作.
     *
     * @param item the item
     * @param t    the t
     */
    protected abstract void convertView(View item, T t);
}
