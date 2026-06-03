package cn.hongt.monitor.server.common.exception;


import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ExceptionControllerAdvice {

    /**
     * 将请求体解析并绑定到 java bean 时，如果出错
     * 表单绑定到 java bean 出错
     * 校验参数 @RequestBody 时的异常
     * 注意 @Validated 需要放在 自定义实体类（入参） 的类上
     * 例如 ：
     * public Results testUpdate(@Validated(Update.class) @RequestBody TestDto dto){}
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        // 从异常对象中拿到ObjectError对象
        BindingResult br = e.getBindingResult();
        if (br.hasFieldErrors()) {
            List<FieldError> fieldErrorList = br.getFieldErrors();
            List<String> errors = new ArrayList<>(fieldErrorList.size());
            for (FieldError error : fieldErrorList) {
                errors.add(error.getField() + ":" + error.getDefaultMessage());
            }
            // 然后提取错误提示信息进行返回
            return ResultUtil.errorMsg(errors.toString());
        }
        // 然后提取错误提示信息进行返回
        return ResultUtil.errorMsg("校验错误");
    }

    /**
     * 表单绑定到 java bean 出错
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BindException.class)
    public Result MethodArgumentNotValidExceptionHandler(BindException e) {
        // 从异常对象中拿到ObjectError对象
        BindingResult br = e.getBindingResult();
        if (br.hasFieldErrors()) {
            List<FieldError> fieldErrorList = br.getFieldErrors();
            List<String> errors = new ArrayList<>(fieldErrorList.size());
            for (FieldError error : fieldErrorList) {
                errors.add(error.getField() + ":" + error.getDefaultMessage());
            }
            // 然后提取错误提示信息进行返回
            return ResultUtil.errorMsg(errors.toString());
        }
        // 然后提取错误提示信息进行返回
        return ResultUtil.errorMsg("校验错误");
    }

    /**
     * 普通参数(非 java bean)校验出错
     * 校验参数 @RequestParam @PathVariable 时的异常
     * 注意 @Validated 需要放在controller的类上
     * 例如：
     *
     * @RestController
     * @RequestMapping("/cs")
     * @Validated public class TestController {
     * @PostMapping("/test") public Results test(
     * @Pattern(regexp = "^\\d{19}$", message = "用户ID，应为19位数字") String id,
     * @NotBlank(message = "名字不能为空") String name
     * ) {}
     * }
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result constraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (CollectionUtils.isEmpty(violations)) {
            log.error("constraintViolationException violations 为空", e);
            return ResultUtil.errorMsg(e.getMessage());
        }
        Map<String, String> map = violations.stream()
                .collect(Collectors.toMap(o -> {
                    PathImpl x = (PathImpl) o.getPropertyPath();
                    return x.getLeafNode().toString();
                }, ConstraintViolation::getMessage, (k1, k2) -> k1));
        return ResultUtil.errorMsg(map.toString());
    }
}

