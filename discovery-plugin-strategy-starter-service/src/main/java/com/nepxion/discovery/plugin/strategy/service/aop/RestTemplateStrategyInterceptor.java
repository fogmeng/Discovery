package com.nepxion.discovery.plugin.strategy.service.aop;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nepxion.discovery.common.constant.DiscoveryConstant;
import com.nepxion.discovery.plugin.strategy.service.context.ServiceStrategyContextHolder;
import com.nepxion.discovery.plugin.strategy.service.trace.TraceIdGenerator;

public class RestTemplateStrategyInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateStrategyInterceptor.class);

    private String requestHeaders;

    @Autowired(required = false)
    private TraceIdGenerator traceIdGenerator;

    @Autowired
    private ServiceStrategyContextHolder serviceStrategyContextHolder;

    public RestTemplateStrategyInterceptor(String requestHeaders) {
        this.requestHeaders = requestHeaders.toLowerCase();

        LOG.info("------------- RestTemplate Proxy Information -----------");
        LOG.info("RestTemplate interceptor headers are '{}'", requestHeaders);
        LOG.info("-------------------------------------------------");
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes attributes = serviceStrategyContextHolder.getRestAttributes();
        if (attributes == null) {
            return execution.execute(request, body);
        }

        HttpServletRequest previousRequest = attributes.getRequest();
        Enumeration<String> headerNames = previousRequest.getHeaderNames();
        if (headerNames == null) {
            return execution.execute(request, body);
        }

        HttpHeaders headers = request.getHeaders();

        if (requestHeaders.contains(DiscoveryConstant.TRACE_ID.toLowerCase())) {
            String traceId = previousRequest.getHeader(DiscoveryConstant.TRACE_ID);
            if (StringUtils.isEmpty(traceId) && traceIdGenerator != null) {
                try {
                    traceId = traceIdGenerator.generate();
                } catch (Exception e) {
                    LOG.error("Generate trace id failed, ignore to set trace id", e);
                }
            }
            if (StringUtils.isNotEmpty(traceId)) {
                headers.add(DiscoveryConstant.TRACE_ID, traceId);
            }
        }

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String header = previousRequest.getHeader(headerName);

            if (requestHeaders.contains(headerName.toLowerCase())) {
                if (!StringUtils.equals(headerName, DiscoveryConstant.TRACE_ID)) {
                    headers.add(headerName, header);
                }
            }
        }

        return execution.execute(request, body);
    }
}