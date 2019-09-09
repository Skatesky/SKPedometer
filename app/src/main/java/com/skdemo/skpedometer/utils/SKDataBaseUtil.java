package com.skdemo.skpedometer.utils;

// 第三方轻量数据库
import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import java.util.List;

public class SKDataBaseUtil {
    public static String dataBaseName;  // 数据库名字
    public static LiteOrm liteOrm;

    public static void createDataBase(Context _activity, String dbName) {
        dataBaseName = dbName + ".db";
        if (liteOrm == null) {
            liteOrm = LiteOrm.newCascadeInstance(_activity, dataBaseName);
            liteOrm.setDebugged(true);
        }
    }

    public static LiteOrm getLiteOrm() { return liteOrm; }

    /**
     * 插入一条记录
     *
     * @param <T> the type parameter
     * @param t   the t
     */
    public static <T> void insert(T t) {
        liteOrm.save(t);
    }

    /**
     * 查询所有
     *
     * @param <T>    the type parameter
     * @param tClass the t class
     * @return the query all
     */
    public static <T> List<T> getQueryAll(Class<T> tClass) {
        return liteOrm.query(tClass);
    }

    /**
     * 查询  某字段 等于 Value的值
     *
     * @param <T>    the type parameter
     * @param tClass the t class
     * @param field  the field
     * @param value  the value
     * @return the query by where
     */
    public static <T> List<T> getQueryByWhere(Class<T> tClass, String field, String[] value) {
        return liteOrm.<T>query(new QueryBuilder(tClass).where(field+"=?", value));
    }

    /**
     * 查询  某字段 等于 Value的值  可以指定从1-20，就是分页
     *
     * @param <T>    the type parameter
     * @param tClass the t class
     * @param field  the field
     * @param value  the value
     * @param start  the start
     * @param length the length
     * @return the query by where length
     */
    public static <T> List<T> getQueryByWhereLength(Class<T> tClass, String field, String[] value, int start, int length) {
        return liteOrm.<T>query(new QueryBuilder(tClass).where(field + "=?", value).limit(start, length));
    }

    /**
     * 删除所有.
     *
     * @param <T>    the type parameter
     * @param tClass the t class
     */
    public static <T> void deleteAll(Class<T> tClass) {
        liteOrm.deleteAll(tClass);
    }

    /**
     * 仅在以存在时更新.
     *
     * @param <T> the type parameter
     * @param t   the t
     */
    public static <T> void update(T t) {
        liteOrm.update(t, ConflictAlgorithm.Replace);
    }

    /**
     * Close data base.
     */
    public static void closeDataBase(){
        liteOrm.close();
    }
}
