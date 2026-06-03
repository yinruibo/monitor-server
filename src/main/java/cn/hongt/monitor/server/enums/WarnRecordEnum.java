package cn.hongt.monitor.server.enums;

import lombok.Getter;

import java.io.File;

@Getter
public enum WarnRecordEnum {

    //todo 待修改，需要将名称配置添加到 配置数据库表中

    // Linux系统监控
    linux_cpu("linux_cpu","CPU告警","此时CPU占用过高导致触发了CPU告警"),
    linux_memory("linux_memory","内存告警","此时内存占用过高导致触发了内存告警"),
    linux_disk("linux_disk","磁盘存储告警","此时文件磁盘存储占用过多导致触发了磁盘告警"),
    linux_IO_read("linux_IO_read","磁盘IO告警","此时磁盘读取过快导致触发了磁盘IO告警"),
    linux_IO_write("linux_IO_write","磁盘IO告警","此时磁盘写入过快导致触发了磁盘IO告警"),
    linux_network_up("linux_network_up","网络速率告警","此时文件上传过多导致触发了网络告警"),
    linux_network_down("linux_network_down","网络速率告警","此时文件下载过多导致触发了网络告警"),

    // Docker镜像监控
    docker_cpu("docker_cpu","CPU告警","此时CPU占用过高导致触发了CPU告警"),
    docker_memory("docker_memory","内存告警","此时内存占用过高导致触发了内存告警"),
    docker_IO_read("docker_IO_read","磁盘IO告警","此时磁盘读取过快导致触发了磁盘IO告警"),
    docker_IO_write("docker_IO_write","磁盘IO告警","此时磁盘写入过快导致触发了磁盘IO警"),
    docker_network_up("docker_network_up","网络速率告警","此时文件上传过多导致触发了网络告警"),
    docker_network_down("docker_network_down","网络速率告警","此时文件下载过多导致触发了网络告警"),

    // Windows系统监控
    win_cpu("win_cpu","CPU告警","此时CPU占用过高导致触发了CPU告警"),
    win_memory("win_memory","内存告警","此时内存占用过高导致触发了内存告警"),
    win_disk("win_disk","磁盘存储告警","此时文件磁盘存储占用过多导致触发了磁盘IO告警"),
    win_IO_read("win_IO_read","磁盘IO告警","此时磁盘读取过快导致触发了磁盘告警"),
    win_IO_write("win_IO_write","磁盘IO告警","此时磁盘写入过快导致触发了磁盘告警"),
    win_network_up("win_network_up","网络速率告警","此时文件上传过多导致触发了网络告警"),
    win_network_down("win_network_down","网络速率告警","此时文件下载过多导致触发了网络告警"),

    // 常规观测报文资料
    conven_surf("conven_surf","地面报告警","常规地面资料解析失败"),
    conven_upar("conven_upar","探空报告警","常规探空资料解析失败"),

    // 卫星资料
    sate_fy4a("sate_fy4a","FY4A数据告警",null),
    sate_fy4b("sate_fy4b","FY4B数据告警",null),

    // 雷达资料
    radar_cr("radar_cr","拼图雷达组合反射率告警",null),
    radar_br("radar_br","拼图雷达基本反射率告警",null),
    radar_scr("radar_scr","单站雷达组合反射率告警",null),
    radar_sbr("radar_sbr","单站雷达基本反射率告警",null),
    radar_svr("radar_svr","单站雷达径向速度告警",null),
    radar_ohp1h("radar_ohp1h","拼图雷达1小时降水告警",null),
    radar_ohp3h("radar_ohp3h","拼图雷达3小时降水告警",null),
    radar_ohp6h("radar_ohp6h","拼图雷达6小时降水告警",null),
    radar_ohp12h("radar_ohp12h","拼图雷达12小时降水告警",null),
    radar_ohp24h("radar_ohp24h","拼图雷达24小时降水告警",null),

    // 实况观测资料
    real_WNP("real_WNP","海洋实况告警",null),
    real_CLDAS("real_CLDAS","CLDAS实况告警",null),

    // 数值预报资料
    fc_ECMWF("fc_ECMWF","EC数据预报告警",null),
    fc_AGENT_GRID_CHINA("fc_AGENT_GRID_CHINA","智能网格-全国-预报告警",null),
    fc_AGENT_GRID_NANSEA("fc_AGENT_GRID_NANSEA","智能网格-南海及周边-预报告警",null),
    fc_AGENT_GRID_NORTHTAI("fc_AGENT_GRID_NORTHTAI","智能网格-东北亚、台湾-预报告警",null),
    fc_AGENT_GRID_INDIA("fc_AGENT_GRID_INDIA","智能网格-印度-预报告警",null),
    fc_GRAPES("fc_GRAPES","GRAPES数据预报告警",null),
    fc_MESO("fc_MESO","MESO数据预报告警",null),
    fc_T799("fc_T799","T1279数据预报告警",null),
    fc_HJJ("fc_HJJ","T799数据预报告警",null),
    fc_CGYSDA("fc_CGYSDA","需方A区域数据预报告警",null),
    fc_CGYSDB("fc_CGYSDB","需方B区域数据预报告警",null),
    fc_CGYSDC("fc_CGYSDC","需方C区域数据预报告警",null),
    fc_CGYSDD("fc_CGYSDD","需方D区域数据预报告警",null),

    null1("fc_null",null,null);


    private String code;
    private String sign;
    private String explain;

    WarnRecordEnum(String code, String sign, String explain){
        this.code=code;
        this.sign=sign;
        this.explain=explain;
    }

    public String getCode() {
        return code;
    }

    public String getSign() {
        return sign;
    }

    public String getExplain() {
        return explain;
    }

    public static String getExplainByCode(String code){
        for(WarnRecordEnum n: WarnRecordEnum.values()){
            if(n.getCode().equals(code)){
                return n.getExplain();
            }
        }
        return "";
    }

    public static String getSignByCode(String code){
        String signStr = "";
        for(WarnRecordEnum n: WarnRecordEnum.values()){
            if(n.getCode().equals(code)){
                return n.getSign();
            }
        }
        // 根据磁盘进行返回不同的告警信息
        if(code.contains(linux_disk.getCode())){
            signStr = code.replace(linux_disk.getCode()+"_","")+" 磁盘路径告警";
        }
        // 根据网卡进行返回不同的告警信息 -- 现在网络监控为 总网卡
//        else if(code.contains(linux_network_up.getCode())){
//            String[] netWorkSplit = code.split("/");
//            signStr = netWorkSplit[netWorkSplit.length -2]+" 网卡上传速率告警";
//        }else if(code.contains(linux_network_down.getCode())){
//            String[] netWorkSplit = code.split("/");
//            signStr = netWorkSplit[netWorkSplit.length -2]+" 网卡下载速率告警";
//        }
        return signStr;
    }

}
