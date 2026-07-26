package com.zmyc.common.util;

import com.zmyc.common.enums.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 */
@Component
public class I18nUtil {
    
    private static MessageSource messageSource;
    
    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }
    
    /**
     * 获取国际化消息
     * @param key 消息键
     * @param args 参数
     * @return 国际化后的消息
     */
    public static String getMessage(String key, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            return key;
        }
    }
    
    /**
     * 根据错误码获取国际化消息
     * @param errorCode 错误码
     * @param args 参数
     * @return 国际化后的消息
     */
    public static String getMessage(ErrorCode errorCode, Object... args) {
        return getMessage(errorCode.getMessageKey(), args);
    }
    
    /**
     * 获取当前语言
     * @return 当前语言
     */
    public static Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }
}

