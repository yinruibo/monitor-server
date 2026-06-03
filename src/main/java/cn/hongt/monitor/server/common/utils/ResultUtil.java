package cn.hongt.monitor.server.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
/**
 * @author yrb
 * @date 2021/7/1
 * @Description: 返回结果工具类
 */
public class ResultUtil {

    public static<T> Result<T> ok(T data){
        Result<T> result=new Result<T>();
        result.setCode("200");
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static<T> Result<T> status(String code, String msg, T data) {
        Result<T> result=new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

//    200：成功，有返回数据体
    public static<T> Result<T> success(T data){
       return ok(data);
    }

//    200：成功，无返回数据体
    public static<T> Result<T> success(){
        return ok(null);
    }

//    500：表示错误，错误信息在msg字段中
    public static<T> Result<T> errorMsg(String msg){
        return status("500",msg,null);
    }
    public static<T> Result<T> errorMsg(String code,String msg){
        return status(code,msg,null);
    }

//    501：bean验证错误，不管多少个错误都以map形式返回
    public static<T> Result<T> errorMap(T data){
        return status("501","error",data);
    }

//    502：拦截器拦截到用户token出错
    public static<T> Result<T> errorTokenMsg(String msg){
        return status("502",msg,null);
    }

//    555：异常抛出信息
    public static<T> Result<T> errorException(String msg){
        return status("555",msg,null);
    }

//    自定义
    public static<T> Result<T> build(String code, String msg, T data) {
        return status(code, msg, data);
    }

//    将JSON字符串集转化为Result对象，需要转换的对象是一个类
    public static<T> Result<T> formatToPojo(String jsonData, Class<T> clazz) {
        try {
            if (clazz == null) {
                return MAPPER.readValue(jsonData, Result.class);
            }
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            JsonNode data = jsonNode.get("data");
            T obj = null;
            if (clazz != null) {
                if (data.isObject()) {
                    obj = MAPPER.readValue(data.traverse(), clazz);
                } else if (data.isTextual()) {
                    obj = MAPPER.readValue(data.asText(), clazz);
                }
            }
            return build(jsonNode.get("code").asText(), jsonNode.get("msg").asText(), obj);
        } catch (Exception e) {
            // 【修复#76】返回错误Result而非null，防止调用方NPE
            log.error("JSON反序列化失败: {}", jsonData, e);
            return errorMsg("数据格式异常");
        }
    }

//    String转化为Result对象
    public static Result format(String json) {
        try {
            return MAPPER.readValue(json, Result.class);
        } catch (Exception e) {
            // 【修复#76】返回错误Result而非null
            log.error("Result反序列化失败: {}", json, e);
            return errorMsg("数据格式异常");
        }
    }

//    Object是集合转化，需要转换的对象是一个list
    public static<T> Result<T> formatToList(String jsonData, Class<T> clazz) {
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            JsonNode data = jsonNode.get("data");
            T obj = null;
            if (data.isArray() && data.size() > 0) {
                obj = MAPPER.readValue(data.traverse(),
                        MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
            }
            return build(jsonNode.get("code").asText(), jsonNode.get("msg").asText(), obj);
        } catch (Exception e) {
            // 【修复#76】返回错误Result而非null
            log.error("JSON反序列化失败: {}", jsonData, e);
            return errorMsg("数据格式异常");
        }
    }



}

