/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.action.api;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pro.octet.senses.bot.core.action.AbstractAction;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.entity.Parameter;
import pro.octet.senses.bot.core.enums.DataFormat;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.core.model.ApiConfig;
import pro.octet.senses.bot.core.model.HttpParam;
import pro.octet.senses.bot.exception.ActionExecutionException;
import pro.octet.senses.bot.utils.CommonUtils;
import pro.octet.senses.bot.utils.XmlParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API Action implementation.
 * By default, the return value of the interface request is parsed into a "MAP",
 * and only response data in XML or JSON format is supported.
 *
 * @author William
 * @see DataFormat
 * @see AbstractAction
 * @since 22.0816.2.6
 */
@Slf4j
public class ApiAction extends AbstractAction {

    private final static RestTemplate HTTP_REST_CLIENT;
    private final static int MAX_TIMEOUT_LIMITED = 1000 * 30;
    private final static int DEFAULT_TIMEOUT_LIMITED = 1000 * 3;

    static {
        HTTP_REST_CLIENT = new RestTemplate();
    }

    public ApiAction(Action action) {
        super(action);
        ApiConfig apiConfig = getActionConfig(ApiConfig.class);
        if (apiConfig.getTimeout() > MAX_TIMEOUT_LIMITED) {
            apiConfig.setTimeout(MAX_TIMEOUT_LIMITED);
        }
        if (apiConfig.getTimeout() <= 0) {
            apiConfig.setTimeout(DEFAULT_TIMEOUT_LIMITED);
        }
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(apiConfig.getTimeout());
        factory.setConnectionRequestTimeout(apiConfig.getTimeout());
        factory.setReadTimeout(apiConfig.getTimeout());
        HTTP_REST_CLIENT.setRequestFactory(factory);
    }

    private HttpHeaders buildHttpHeaders(List<HttpParam> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(param -> httpHeaders.add(param.getName(), CommonUtils.parameterBindingFormat(getActionParam(), param.getValue())));
        return httpHeaders;
    }

    private Map<String, Object> buildHttpRequest(List<HttpParam> request) {
        if (request == null || request.isEmpty()) {
            return null;
        }
        Map<String, Object> requestParams = Maps.newLinkedHashMap();
        request.forEach(param -> requestParams.put(param.getName(), CommonUtils.parameterBindingFormat(getActionParam(), param.getValue())));
        return requestParams;
    }

    private ResponseEntity<String> callApi(String url, HttpMethod method, HttpEntity<String> body, Map<String, Object> requestParams) {
        if (requestParams == null) {
            return HTTP_REST_CLIENT.exchange(url, method, body, String.class);
        } else {
            return HTTP_REST_CLIENT.exchange(url, method, body, String.class, requestParams);
        }
    }

    @Override
    public ExecuteResult execute() throws ActionExecutionException {
        Summary.of().info("action.start.regexp", getAction().getName());
        ExecuteResult executeResult = new ExecuteResult();
        try {
            // create http post parameters
            ApiConfig apiConfig = getActionConfig(ApiConfig.class);
            String url = CommonUtils.parameterBindingFormat(getActionParam(), apiConfig.getUrl());
            HttpHeaders httpHeaders = buildHttpHeaders(apiConfig.getHeaders());
            Map<String, Object> requestParams = buildHttpRequest(apiConfig.getRequest());
            HttpEntity<String> httpEntity = null;
            if (StringUtils.isNotEmpty(apiConfig.getBody())) {
                httpEntity = new HttpEntity<>(CommonUtils.parameterBindingFormat(getActionParam(), apiConfig.getBody().toString()), httpHeaders);
            }
            //print debug message
            String debugMessage = CommonUtils.format("API URL: {0}, Request method: {1}, Post parameters: {2}, Request parameters: {3}", url, apiConfig.getMethod(), CommonUtils.toJson(httpEntity), Optional.ofNullable(requestParams).orElse(null));
            log.debug(debugMessage);
            Summary.of().debug(debugMessage);
            // call http api
            ResponseEntity<String> responseEntity = callApi(url, HttpMethod.resolve(apiConfig.getMethod()), httpEntity, requestParams);
            if (responseEntity.hasBody()) {
                String responseBody = responseEntity.getBody();
                Summary.of().debug("API Response body: {0}", responseBody);
                switch (apiConfig.getFormat()) {
                    case JSON:
                        executeResult.putAll(CommonUtils.parseJsonToMap(responseBody, String.class, Object.class));
                        break;
                    case XML:
                        executeResult.putAll(XmlParser.parseXmlToMap(responseBody));
                        break;
                    case DEFAULT:
                        List<Parameter> output = getAction().getActionOutput();
                        if (output != null && output.size() == 1) {
                            executeResult.put(output.get(0).getParam(), Optional.ofNullable(responseBody).orElse(StringUtils.EMPTY));
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported response data format");
                }
            }
        } catch (Exception e) {
            setExecuteThrowable(e);
        }
        return executeResult;
    }

}
