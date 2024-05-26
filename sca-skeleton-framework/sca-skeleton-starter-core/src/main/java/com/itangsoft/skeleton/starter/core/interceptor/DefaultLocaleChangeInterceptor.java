package com.itangsoft.skeleton.starter.core.interceptor;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

/**
 * 国际化地区设置变更拦截器
 *
 * @author fushuwei
 */
@Slf4j
public class DefaultLocaleChangeInterceptor extends LocaleChangeInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver == null) {
            throw new IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?");
        }
        String newLocale = request.getHeader(getParamName());
        Locale locale = localeResolver.resolveLocale(request);
        if (newLocale != null && !newLocale.equalsIgnoreCase(locale.toString())) {
            try {
                localeResolver.setLocale(request, response, StringUtils.parseLocaleString(newLocale));
            } catch (Exception e) {
                logger.error("修改国际化地区设置失败", e);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (null != modelAndView) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver == null) {
                throw new IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?");
            }
            Locale locale = localeResolver.resolveLocale(request);
            modelAndView.setViewName(locale + "/" + modelAndView.getViewName());
        }
    }
}
