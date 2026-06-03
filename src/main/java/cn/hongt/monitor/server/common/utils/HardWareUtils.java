package cn.hongt.monitor.server.common.utils;

import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.LinuxValueOutput;
import cn.hongt.monitor.server.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
public class HardWareUtils {

    /**
     * linux数据分割，list
     * @param list  查询出来的cpu 的数据
     * @param  input  前端传过来的实体类
     */
    public static List<HardresultOutput> getLinuxInter(List<LinuxValueOutput> list, HardWareMonitorInput input){

        //        将String类型的时间转化为 Long时间戳
        Long start = getTimeToLong(input.getStartTime(), DateUtils.dateType5);
        //        将String类型的时间转化为 Long时间戳
        Long end = getTimeToLong(input.getEndTime(),DateUtils.dateType5);
        // 时间差 1小时、6小时、12小时，1天，3天，7天，14天 value

        // 时间相差小时数
        Long hourL = (end - start) / 60 / 60 / 1000;
        int hour = hourL.intValue();
        // todo 修改返回数据条数
        //        根据相差的小时  进行选择性的获取  数据
        List<LinuxValueOutput> linuxValueList = getValuesList(list,hour,input);
        // 求最大、最小、平均值
        List<HardresultOutput> outputLlist = ListToObject(linuxValueList);
        return outputLlist;
    }

    /**
     * 数据间隔取值
     * @param list  数据库数据 数据库查询出来的时间段之间的数据
     * @param hour 间隔
     * @param input 前端数据
     */
    private static List<LinuxValueOutput> getValuesList(List<LinuxValueOutput> list, int hour, HardWareMonitorInput input){
        // 要返回给前端的数据
        List<LinuxValueOutput> valueList = new ArrayList<>();
        int interval = 1;
//        boolean sfqt =false;
        if(12< hour && hour <=24){
            // 1 天   30秒interval = 6;
            interval = 6;
        }else if(24< hour && hour <=72){ // 1~3天 interval = 18;
            interval = 3;
        }else if(72< hour && hour <=168){ // 3~7天
            // 7 天   105秒interval = 21;
            interval = 3;
//            sfqt = true;
        }else if(168< hour && hour <=336){ // 7~14天
            interval = 6; // 间隔 84 * 5s / 60s= 7分钟 取一条数据 interval = 42;
        }else if(336< hour ){ // 大于14天
            interval = 6;
        }

//        if(sfqt){
//            return list;
//        }else {
            // 要返回给前端的时间段
            List<Date> minList = new ArrayList<>();
            // 根据传过来的开始、结束时间 创建时间列表，重新给 数据库数据 -> 赋予时间
            Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
            Date end = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
            // 当结束时间，大于等于开始时间时，执行此方法
            int x = 0;
            while (end.after(start)){
                int num = 0;
                if(x > 0)
                    num = 5;
                minList.add(DateUtils.addDate(start,0,0,0,0,interval*num,0,0));
                start = DateUtils.addDate(start,0,0,0,0,interval*num,0,0);
                x++;
            }
            //  list 是数据库数据
            boolean ifjj = false;// 如果变成true，那么数据库查询出来的数据的最后一个也放进去了
            // 【修复#70#71】空list保护 + 边界安全：i可能跳过list.size()-1
            if(list == null || list.isEmpty()){
                return valueList;
            }
            for(int i=0;i<list.size();i+=interval){
                valueList.add(list.get(i));
                if(i + interval >= list.size()){
                    ifjj = true;
                }
            }
            // 将数据库最后一个数据的时间也放进时间列表
            minList.add(list.get(list.size()-1).getTime());
            String s = DateUtils.dateToString(list.get(list.size() - 1).getTime(), DateUtils.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI_SS);
            //   valueList 要返回给前端的数据
            for(int k=0;k<valueList.size();k++){
                Date time = valueList.get(k).getTime();// 数据库时间
                Date nearestDate = findNearestDate(minList, time);
                valueList.get(k).setTime(nearestDate);
            }
            if(!ifjj){
                valueList.add(list.get(list.size()-1));
            }
            return valueList;
//        }
    }

    /**
     *  将String类型的时间转化为 Long时间戳
     * @param
     * @return
     */
    private static Long getTimeToLong(String time,String str){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(str);
        try {
            calendar.setTime(df.parse(time));
        } catch (ParseException e) {
            // 【修复#50】抛出异常而非仅记日志，防止后续calendar.getTimeInMillis()返回错误值
            throw new RuntimeException("时间格式解析失败: time=" + time + ", pattern=" + str, e);
        }
        return calendar.getTimeInMillis();
    }

    // 集合计算最大最小值
    private static List<HardresultOutput> ListToObject(List<LinuxValueOutput> list){
        LinuxValueOutput  hardresultOutput= null;

        List<HardresultOutput> outList = new ArrayList<>();
        int rem = list.size() % 2;
        if (1 == rem){
            // 奇数
            if(list!=null && list.size()>0){
                hardresultOutput = list.get( list.size() -1);
            }
            // 【修复#72】创建新list副本再remove，不修改传入的原始列表
            list = new ArrayList<>(list);
            list.remove(list.size() -1);
        }
        for (int i = 0; i < list.size(); i+=2) {
            HardresultOutput output = new HardresultOutput();
            // 计算最大值
            if(list.get(i).getValues() < list.get(i+1).getValues()){
                output.setMaximum(list.get(i+1).getValues());
                output.setMinimum(list.get(i).getValues());
            }else {
                output.setMaximum(list.get(i).getValues());
                output.setMinimum(list.get(i+1).getValues());
            }
            output.setTimestamp(list.get(i+1).getTime());
            DecimalFormat df = new DecimalFormat("######0.00");
            // double avg = Double.valueOf(df.format(list.get(i).getValues() + list.get(i+1).getValues()/2));
            double avg = Double.valueOf(df.format((output.getMaximum()+output.getMinimum())/2));
            output.setAverage(avg);
            output.setUnit(list.get(i + 1).getUnit());
            outList.add(output);
        }
        if(hardresultOutput!=null){
            HardresultOutput output = new HardresultOutput();
            output.setMaximum(hardresultOutput.getValues());
            output.setAverage(hardresultOutput.getValues());
            output.setMinimum(hardresultOutput.getValues());
            output.setTimestamp(hardresultOutput.getTime());
            output.setUnit(hardresultOutput.getUnit());
            outList.add(output);
        }
        return outList;
    }


    /**
     * 【优化】使用二分查找替代线性遍历，时间复杂度从O(n)降到O(log n)
     * 查找dateList中距离targetDate最近的日期
     * 前提：dateList已按时间升序排列（minList由DateUtils.addDate递增构建，天然有序）
     */
    private static Date findNearestDate(List<Date> dateList, Date targetDate) {
        if (dateList == null || dateList.isEmpty()) {
            return null;
        }
        if (dateList.size() == 1) {
            return dateList.get(0);
        }

        long target = targetDate.getTime();

        // 用binarySearch找插入位置（第一个 >= target 的元素位置）
        int index = Collections.binarySearch(dateList, targetDate, Comparator.comparingLong(Date::getTime));

        if (index >= 0) {
            // 精确匹配
            return dateList.get(index);
        }

        // index = -(insertion point) - 1，即第一个大于target的位置
        int insertPoint = -index - 1;

        if (insertPoint == 0) {
            // target比所有日期都小，最近的是第一个
            return dateList.get(0);
        }
        if (insertPoint >= dateList.size()) {
            // target比所有日期都大，最近的是最后一个
            return dateList.get(dateList.size() - 1);
        }

        // 比较左右两个候选日期哪个更近
        Date left = dateList.get(insertPoint - 1);
        Date right = dateList.get(insertPoint);
        long leftDiff = target - left.getTime();
        long rightDiff = right.getTime() - target;
        return leftDiff <= rightDiff ? left : right;
    }
}
