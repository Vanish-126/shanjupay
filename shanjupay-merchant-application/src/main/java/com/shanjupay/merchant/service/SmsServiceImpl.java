package com.shanjupay.merchant.service;

import com.alibaba.fastjson.JSON;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@org.springframework.stereotype.Service  //实例为一个bean
@Slf4j
public class SmsServiceImpl implements SmsService{

    /**
     * url信息在
     * nacos的merchant-application.yaml中配置
     * sms.url: "http://localhost:56085/sailing"
     */
    @Value("${sms.url}")
    String url;

    /**
     * 有效时间在
     * nacos的merchant-application.yaml中配置
     * sms.effectiveTime: 600
     */
    @Value("${sms.effectiveTime}")
    String effectiveTime;

    @Autowired
    RestTemplate restTemplate;

    /**
     * 发送手机验证码
     * 使用restTemplate，用Java程序模拟客户端调用验证码服务的接口获取验证码
     * 以下代码通过RestTemplateTest.java中的getSmsCode方法修改得来
     * @param phone 手机号
     * @return 验证码对应的key
     */
    @Override
    public String sendMsg(String phone) throws BusinessException{
        //验证码服务的接口：http://localhost:56085/sailing/generate?effectiveTime=600&name=sms
        String sms_url = url + "/generate?name=sms&effectiveTime="+effectiveTime;

        //body：请求体
        Map<String,Object> body = new HashMap<>();
        body.put("mobile",phone);

        //header：请求头
        HttpHeaders httpHeaders =new HttpHeaders();
        //作为请求头告诉服务端消息主体是JSON
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        //entity：请求信息，传入body，header
        HttpEntity httpEntity = new HttpEntity(body,httpHeaders);

        //ResponseEntity设置http相应内容，请求url
        ResponseEntity<Map> exchange = null;
        Map bodyMap = null;
        //异常处理
        try {
            //注意设置请求方法：post, put, get, delete
            exchange = restTemplate.exchange(sms_url, HttpMethod.POST, httpEntity, Map.class);
            log.info("请求验证码服务，得到响应:{}", JSON.toJSONString(exchange));
            //获取响应
            bodyMap = exchange.getBody();
        } catch (RestClientException e) {
            e.printStackTrace();
            log.info(e.getMessage(), e);
            //E_100107(100107,"发送验证码错误")
            throw new BusinessException(CommonErrorCode.E_100107);
        }

        if(bodyMap == null || bodyMap.get("result") == null){
            //E_100107(100107,"发送验证码错误")
            throw new BusinessException(CommonErrorCode.E_100107);
        }

        //从请求体的result数据中获取key
        Map result = (Map) bodyMap.get("result");
        String key = (String) result.get("key");
        log.info("得到发送验证码对应的key:{}",key);
        return key;
    }

    /**
     * 校验手机验证码
     * @param verifiyKey  验证码的key
     * @param verifiyCode 验证码
     */
    @Override
    public void checkVerifiyCode(String verifiyKey, String verifiyCode) throws BusinessException {
        //校验验证码的url
        String sms_url = url + "/verify?name=sms&verificationCode=" + verifiyCode + "&verificationKey=" + verifiyKey;

        Map bodyMap = null;
        try {
            //使用restTemplate请求验证码服务
            ResponseEntity<Map> exchange = restTemplate.exchange(sms_url, HttpMethod.POST, HttpEntity.EMPTY, Map.class);
            log.info("请求验证码服务，得到响应:{}", JSON.toJSONString(exchange));
            bodyMap = exchange.getBody();
        }catch (Exception e){
            e.printStackTrace();
            log.info(e.getMessage(), e);
            //E_100102(100102,"验证码错误")
            throw new BusinessException(CommonErrorCode.E_100102);
        }

        if(bodyMap == null || bodyMap.get("result") == null || !(Boolean) bodyMap.get("result")){
            //E_100102(100102,"验证码错误")
            throw new BusinessException(CommonErrorCode.E_100102);
        }
    }
}
