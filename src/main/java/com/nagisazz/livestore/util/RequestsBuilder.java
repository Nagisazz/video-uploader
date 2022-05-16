package com.nagisazz.livestore.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class RequestsBuilder {
    
    private static Integer TIMEOUT = 5000;

    private RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(TIMEOUT).setConnectionRequestTimeout(TIMEOUT)
            .setSocketTimeout(TIMEOUT).build();

    public String sendGet(String url, Map<String, String> headers) {
        try (CloseableHttpClient client = HttpClients.createDefault();) {
            HttpGet get = new HttpGet(url);

            get.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                get.setHeader(entry.getKey(), entry.getValue());
            }
            HttpResponse response = client.execute(get);
            String resStr = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info("调用GET成功，url：{}，返回：{}", url, resStr);
            return resStr;
        } catch (IOException e) {
            log.error("调用GET失败，url：{}", url, e);
        }
        return null;
    }

    public String sendPost(String url, String entity, Map<String, String> headers) {
        try (CloseableHttpClient client = HttpClients.createDefault();) {
            HttpPost post = new HttpPost(url);
            post.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.setHeader(entry.getKey(), entry.getValue());
            }
            if (StringUtils.isNotBlank(entity)) {
                StringEntity postingString = new StringEntity(entity, "utf-8");
                post.setEntity(postingString);
            }
            HttpResponse response = client.execute(post);
            String resStr = EntityUtils.toString(response.getEntity());
            log.info("调用POST成功，url：{}，entity：{}，返回：{}", url, entity, resStr);
            return resStr;
        } catch (IOException e) {
            log.error("调用POST失败，url：{}，entity：{}", url, entity, e);
        }
        return null;
    }

    public String sendPut(String url, String entity, Map<String, String> headers) {
        try (CloseableHttpClient client = HttpClients.createDefault();) {
            HttpPut put = new HttpPut(url);
            put.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                put.setHeader(entry.getKey(), entry.getValue());
            }
            if (StringUtils.isNotBlank(entity)) {
                StringEntity postingString = new StringEntity(entity, "utf-8");
                put.setEntity(postingString);
            }
            HttpResponse response = client.execute(put);
            String resStr = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info("调用PUT成功，url：{}，entity：{}，返回：{}", url, entity, resStr);
            return resStr;
        } catch (IOException e) {
            log.error("调用PUT失败，url：{}，entity：{}", url, entity, e);
        }
        return null;
    }

    public String sendFilePut(String url, InputStreamEntity entity, Map<String, String> headers) {
        try (CloseableHttpClient client = HttpClients.createDefault();) {
            HttpPut put = new HttpPut(url);
            put.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                put.setHeader(entry.getKey(), entry.getValue());
            }
            if (entity !=null) {
                put.setEntity(entity);
            }
            HttpResponse response = client.execute(put);
            String resStr = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info("调用FilePUT成功，url：{}，返回：{}", url, resStr);
            return resStr;
        } catch (IOException e) {
            log.error("调用FilePUT失败，url：{}", url, e);
        }
        return null;
    }
}
