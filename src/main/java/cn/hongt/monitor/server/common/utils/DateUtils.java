package cn.hongt.monitor.server.common.utils;

import lombok.SneakyThrows;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @author yrb
 * @date 2021/7/8
 * @Description:时间日期转化工具类
 */
public class DateUtils {

    //时间格式
    public static final String dateType1 = "yyyyMMddHH";
    public static final String dateType2 = "yyyy-MM-dd HH";
    public static final String dateType3 = "yyyyMMdd HH";
    public static final String dateType4 = "yyyyMMdd HH:mm:ss";
    public static final String dateType5 = "yyyy-MM-dd HH:mm:ss";
    public static final String dateType6 = "yyyyMMddHHmm";
    public static final String dateType7 = "yyyyMMddHHmmss";
    public static final String dateType8 = "yyyyMMdd";

    public static final String DATE_FORMAT_YYYY = "yyyy";
    public static final String DATE_FORMAT_YYYYMM = "yyyyMM";
    public static final String DATE_FORMAT_YY_MM_DD = "yy-MM-dd";
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String DATE_TIME_FORMAT_YYYYMMDDHHMISS = "yyyyMMddHHmmss";
    public static final String DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI = "yyyy-MM-dd HH:mm";
    public static final String DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * ThreadLocal 缓存 SimpleDateFormat，避免每次调用都创建新对象
     * key: 日期格式字符串
     * value: SimpleDateFormat 实例（每个线程独立副本）
     */
    private static final ThreadLocal<Map<String, SimpleDateFormat>> SDF_CACHE = ThreadLocal.withInitial(HashMap::new);

    /**
     * 获取线程安全的 SimpleDateFormat 实例
     * @param pattern 日期格式
     * @return SimpleDateFormat 实例
     */
    private static SimpleDateFormat getSdf(String pattern) {
        Map<String, SimpleDateFormat> cache = SDF_CACHE.get();
        SimpleDateFormat sdf = cache.get(pattern);
        if (sdf == null) {
            sdf = new SimpleDateFormat(pattern);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            cache.put(pattern, sdf);
        }
        return sdf;
    }

    public static LocalDateTime currentTime() {
        return LocalDateTime.now();
    }

    //date to string
    public static String dateToString(Date date, String format){
        return getSdf(format).format(date);
    }

    //LocalDateTime 转 Date
    public static Date LocalDateTimeToDate(LocalDateTime date){
        return Date.from(date.atZone( ZoneId.systemDefault()).toInstant());
    }

    //Date 转 LocalDateTime
    public static LocalDateTime DateToLocalDateTime(Date date){
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    //string to date
    @SneakyThrows
    public static Date stringToDate(String date, String format){
        return getSdf(format).parse(date);
    }

    //时间格式转化
    @SneakyThrows
    public static Date dateFormat(Date date, String str){
        SimpleDateFormat sdf = getSdf(str);
        return sdf.parse(sdf.format(date));
    }

    /**
     * 获取某日期的年份
     *
     * @param date
     * @return
     */
    public static Integer getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    /**
     * 获取某日期的月份
     *
     * @param date
     * @return
     */
    public static Integer getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取某日期的日数
     *
     * @param date
     * @return
     */
    public static Integer getDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DATE);//获取日
        return day;
    }

    /**
     * 获取某日期的小時
     *
     * @param date
     * @return
     */
    public static Integer getHour(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);//获取小時
    }

    /**
     * 获取某日期的分鐘
     *
     * @param date
     * @return
     */
    public static Integer getMinute(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MINUTE);//获取分鐘
    }

    /**
     * 获取某日期的秒
     *
     * @param date
     * @return
     */
    public static Integer getSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.SECOND);//获取秒
    }

    /**
     * 获取某日期的毫秒
     *
     * @param date
     * @return
     */
    public static Integer getMSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MILLISECOND);//获取毫秒
    }

    /**
     * 获取某年某月有多少天
     * * @param year
     * * @param month
     * * @return
     */
    public static int getDayOfMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 0); //输入类型为int类型
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获得指定时间加减参数后的日期(不计算则输入0)
     *
     * @param date        指定日期
     * @param year        年数，可正可负
     * @param month       月数，可正可负
     * @param day         天数，可正可负
     * @param hour        小时数，可正可负
     * @param minute      分钟数，可正可负
     * @param second      秒数，可正可负
     * @param millisecond 毫秒数，可正可负
     * @return 计算后的日期
     */
    public static Date addDate(Date date, int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, year);//加减年数
        c.add(Calendar.MONTH, month);//加减月数
        c.add(Calendar.DATE, day);//加减天数
        c.add(Calendar.HOUR, hour);//加减小时数
        c.add(Calendar.MINUTE, minute);//加减分钟数
        c.add(Calendar.SECOND, second);//加减秒
        c.add(Calendar.MILLISECOND, millisecond);//加减毫秒数

        return c.getTime();
    }

    /**
     * 计算两个日期之间相差的天数
     * @param smdate 较小的时间
     * @param bdate  较大的时间
     * @return 相差天数
     * @throws ParseException
     */
    public static int daysBetween(Date smdate,Date bdate) throws ParseException {
        SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
        smdate=sdf.parse(sdf.format(smdate));
        bdate=sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        long between_days=(time2-time1)/(1000*3600*24);

        // 【修复#77】long已整除，直接强转，去掉Integer.parseInt(String.valueOf(...))绕路
        return (int) between_days;
    }

    /**
     *字符串的日期格式的计算
     */
    public static int daysBetween(String smdate,String bdate) throws ParseException{
        SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(smdate));
        long time1 = cal.getTimeInMillis();
        cal.setTime(sdf.parse(bdate));
        long time2 = cal.getTimeInMillis();
        long between_days=(time2-time1)/(1000*3600*24);

        return (int) between_days;
    }

    /**
     * 获取 获取某年某月 所有日期（yyyyMMdd格式字符串）
     * @param year
     * @param month
     * @return
     */
    public static List<String> getMonthFullDay(int year , int month){
        SimpleDateFormat dateFormatYYYYMMDD = getSdf("yyyyMMdd");
        List<String> fullDayList = new ArrayList<>(32);
        // 获得当前日期对象
        Calendar cal = Calendar.getInstance();
        cal.clear();// 清除信息
        cal.set(Calendar.YEAR, year);
        // 1月从0开始
        cal.set(Calendar.MONTH, month-1 );
        // 当月1号
        cal.set(Calendar.DAY_OF_MONTH,1);
        int count = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int j = 1; j <= count ; j++) {
            fullDayList.add(dateFormatYYYYMMDD.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH,1);
        }
        return fullDayList;
    }

    /**
     *字符串的日期格式的小时计算
     * @param maxTime 大时间 例如：1900010108
     * @param minTime 小时间  例如：2021010100
     */
    public static int hoursDiffByString(String maxTime,String minTime) throws ParseException{
        SimpleDateFormat sdf = getSdf(dateType1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(maxTime));
        long time1 = cal.getTimeInMillis();
        cal.setTime(sdf.parse(minTime));
        long time2 = cal.getTimeInMillis();
        long between_hours=(time2-time1)/(1000*3600);
        return (int) between_hours;
    }

    /**
     *字符串的日期格式的小时计算
     * @param maxTime 大时间
     * @param minTime 小时间
     */
    public static int hoursDiffByDate(Date maxTime,Date minTime){
        Calendar cal = Calendar.getInstance();
        cal.setTime(maxTime);
        long time1 = cal.getTimeInMillis();
        cal.setTime(minTime);
        long time2 = cal.getTimeInMillis();
        long between_hours=(time1-time2)/(1000*3600);
        return (int) between_hours;
    }

    public static Date getDateByHour(Date date, int hour){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hour);
        Date time = calendar.getTime();
       return time;
    }

    /**
     * 去除时间中的秒值转化为00
     * @param time
     * @return
     */
    public static Date deleteSecond(Date time){
        return stringToDate(DateUtils.dateToString(time, DateUtils.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI),DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI);
    }
}
