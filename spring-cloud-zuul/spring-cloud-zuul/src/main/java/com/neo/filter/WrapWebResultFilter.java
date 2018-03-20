package com.neo.filter;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
//import com.yonghui.gateway.config.WrapWebResultConfiguration;
//import com.yonghui.gateway.utils.SpringBeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * 结果转换过滤器
 * wrap the api result to base result
 * Created by fred.zhu on 17/10/27.
 */
public class WrapWebResultFilter extends ZuulFilter {
    private static Logger log = LoggerFactory.getLogger(WrapWebResultFilter.class);
    private final static String DEFAULT_ERROR_MSG = "当前服务繁忙，请稍后再试。";
    private final static String DEFAULT_PARAM_ERROR_MSG = "请求参数非法，请检查。";

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 900;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            RequestContext reqCtx = RequestContext.getCurrentContext();
            reqCtx.setResponseStatusCode(200);
            InputStream inStream = (InputStream) reqCtx.get("responseDataStream");
            String body = StreamUtils.copyToString(inStream, Charset.forName("UTF-8"));
            WebResult webResult;
            if (body.isEmpty()) {
                webResult = WebResult.ok(null);
            } else if (isNotJson(body.trim())) {
                webResult = WebResult.ok(body);
            } else {
                webResult = WebResult.ok(JSONObject.parse(body));
            }
            reqCtx.setResponseBody(JSONObject.toJSONString(webResult));
            String responseBody = reqCtx.getResponseBody();
            return null;
        } catch (Exception e) {
            log.error("wrap web result exception", e);
            return null;
        }
    }

    private boolean isNotJson(String body) {
        if (body != null && (body.startsWith("{") || body.startsWith("["))) {
            return false;
        }
        return true;
    }

    static class WebResult {
        private int code;
        private String message;
        private Object data;
        private Long now = System.currentTimeMillis();

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public Long getNow() {
            return now;
        }

        public void setNow(Long now) {
            this.now = now;
        }

        public WebResult() {
        }

        public WebResult(int code, String message, Object data, Long now) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.now = now;
        }

        public static WebResult ok(Object data) {
            WebResult wr = new WebResult();
            wr.setCode(0);
            wr.setData(data);
            return wr;
        }

        public static WebResult error(int code, String message) {
            WebResult wr = new WebResult();
            wr.setCode(code);
            wr.setMessage(message);
            return wr;
        }
    }
}


