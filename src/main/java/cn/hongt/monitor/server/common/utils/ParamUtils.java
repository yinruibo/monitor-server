package cn.hongt.monitor.server.common.utils;



import cn.hongt.monitor.server.common.exception.InvalidArgumentException;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: AbstractPageQuery用
 */
public abstract class ParamUtils {

    public ParamUtils() {
    }

    /**
     * 方法名误导：isBlank让人以为"是blank就报错"，实际是"不是blank就报错"
     * 改名为 requireBlank 使语义清晰：断言字符串必须为空白，非空白时抛异常
     */
    public static void requireBlank(String string, String msg) {
        if (StringUtils.isNotBlank(string)) {
            throw new InvalidArgumentException(msg);
        }
    }

    /**
     * @deprecated 使用 {@link #requireBlank} 替代，原方法名 isBlank 语义误导
     */
    @Deprecated
    public static void isBlank(String string, String msg) {
        requireBlank(string, msg);
    }

    public static void notBlank(String string, String msg) {
        if (StringUtils.isBlank(string)) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void notEmpty(Collection collection, String msg) {
        if (null == collection || collection.isEmpty()) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void nonBlankElements(Collection<String> collection, String elementMsg) {
        Iterator var2 = collection.iterator();

        while (var2.hasNext()) {
            String str = (String) var2.next();
            notBlank(str, elementMsg);
        }

    }

    public static void nonNull(Object object, String msg) {
        if (null == object) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void isNull(Object object, String msg) {
        if (null != object) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void expectTrue(boolean boolExpression, String falseMsg) {
        if (!boolExpression) {
            throw new InvalidArgumentException(falseMsg);
        }
    }

    public static void expectFalse(boolean boolExpression, String trueMsg) {
        if (boolExpression) {
            throw new InvalidArgumentException(trueMsg);
        }
    }

    public static void expectAnyFalse(String msg, Boolean... booleans) throws InvalidArgumentException {
        if (Arrays.stream(booleans).allMatch((t) -> {
            return t;
        })) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void expectInRange(Collection collection, int minElements, int maxElements, String msg) {
        expectInRange(collection.size(), minElements, maxElements, msg);
    }

    public static void expectInRange(String string, int minLength, int maxLength, String msg) {
        if (StringUtils.isBlank(string) || string.length() < minLength || string.length() > maxLength) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void expectInRange(int value, int minValue, int maxValue, String msg) {
        if (value < minValue || value > maxValue) {
            throw new InvalidArgumentException(msg);
        }
    }

    public static void expectDateStrWithPattern(String sDate, String pattern, String msg) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            df.parse(sDate);
        } catch (Exception var4) {
            throw new InvalidArgumentException(msg);
        }
    }
}
