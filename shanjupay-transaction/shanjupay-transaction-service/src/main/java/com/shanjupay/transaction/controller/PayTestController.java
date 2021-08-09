package com.shanjupay.transaction.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 支付宝接口对接测试类
 * @author Administrator
 * @version 1.0
 **/

@Slf4j
@Controller
//@RestController//请求方法响应统一为json格式
public class PayTestController {

    //应用id
    String APP_ID = "2021000117680470";
    //应用私钥
    String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDaIypRcK5jj00u0Pg6kyowd+/B857Uo60d5DwLgb5T7Bla5uqEzmHEPWnjq+vKBIthagoELO0qFNdv5zWW/2XVY8MBnzwQGrXuU7B6kGEquxPtHtiKVgAIRfyDkj6xbREVR8BUdDmkC4b89r7tZJ0wMHNdPZ2Ne051ZFNLKW/fllqiiZlCZ9sCr31ioe9jcGB59cxbNm+8GMiTOgqAwWZbqAzR2OuNTCKb3GmrCiMpnIZ96XJlk3u5xygGm0zGbshQt4ybgOahoC1RW+kUYuJ+GMPCrGK1jDKPjv+twEB6bMbu50s2ggpdwY+bu+a7JCUknt1QWDxyqGAMEO7i6ylXAgMBAAECggEAdwF8UtXwrew+JW5oHjyvZaXpLZzlaAirp4UepQBxf4NtCcS06SNW/yKqJEVk69+y0sBxGnoQIUchibFP8UMAoXw9pSLkN7z6yDzsKNGf0Roi8thDNqVYeysDahxdC5r/GOb+LJUD5VVMnbFX1Fdx0hp8tb+ptsgws6a6MmU23Xk9q37nZR3NOcRIhXKpnkeXHNo1qyhKSf35oLTwp4bgNVIz1oOSoxL3HKzcVkmmfh5Qa0tnF/NsRoAoC2mzG1gJDl2zRtb5yK51SJvxQ2P0VNqjHTz8srbWCCxqv1Tfd20Srw03VB2N12L3d6+7srTpBb+f+PohterHFroOeDOkkQKBgQDxxfE1XKRPWom/tCVwBTWcN56lmsJSzUj/RmDu14N0RcDf808634/TkC0xC0kUpqThLz/207rAGtc+obzMrG4caMYGegn518cL0ohsk21cB19Kzxbr86wKSJvrh59vQcn2fJdYeNgBmmK7sXXFUFDQbt2JD4IP98uoJK7cgnOcxQKBgQDm+SyNvZoOzZy9HFdqS+fQlecme/YABMMv+GJbMijapGYvoMA9zGIl8xjaZ27wlZPjYzoZLlyBLmDV/k+CvknmyY0DjnBC+8Qm9UI5cpZbI8aTWCOBU+LuGg2JHfuX+1PwQCKuAmFCibZGPfADld4HqcsVRO5w/KCRHULnGYZHawKBgQCTI+FplWkWNkaxGeTS6Qi4ew0dQjNTi0YKvf1OPnDtOWppoT0t2lNA8XTfbTvbypN7zHLEOBVmxq8Kmgo6EhKtTYJ+/JTHre+gnU+TrKGYqY/wClqr3M9uLsRdsltwrXxJe4fsOxdS2m+ORTEklX2pbF99A6gnKaqTOUgGQYo09QKBgDrfrrSebKNzU418qOeGW5t2akoL9OOCU0Jp/KAkQ/efShC/1+CotSfZNC+Ph965Mc4XH9sFTzz6VmKVhSzdVjcGjvQmHYVlaVVI1M2R4LNwhhBOuhYgDMsGRwb5ZoUR4g5uOqjCIIp2UcjLxt8TF1o6tHJCdMjGDCORItmHTwvrAoGBALOiYRXc9sG3ulXsEby1L3Wbp/rk8o/twDmZd0Zye6PfBf0122lR2FSpW24FQ8LxtI4hO/P2aCpSY/fnV6hGT55sTY2b0zDdfHbe82wmaP2BXUN4jxvMj9qr3xnSDzo4h7LORCbRci6rg4hhq3Pclo1qLeJlqtFX6pXdzXQIPiYs";
    /**
     * 支付宝公钥
     */
    String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm2JLwe35mX5aT4uLY3IQgxLjf1ARsxsPoHGwJdAwmzcOqu1QHKzr2hA8SPJF+99EUVJS75lMMmFSaGvUemezuWE+msNUWg1gMDySrvLHdoMBFdo3Yr4WzoInfuignaFSMEvEoOkQz1rPYzDo/iYAZre0nUPSs48EWcza7LgIpHnrTTJixH4NNu7VM/dEfnZqSMabHE7kQeiFo4qBs9ulI95fkLmtd6EYS9FLNNeZFOqpo4Te6zwqoFmEj6AgljhZa0RZu0OHEg8BMJZgwTIt1wJaxkusP2W7geyae1sAvL5gB3h8W9QfUZn6ugvwxWHZgcODFvdE9r5eRLrjzUEMkwIDAQAB";
    //请求使用的编码方式
    String CHARSET = "utf-8";
    //支付宝接口的网关地址
    String serverUrl = "https://openapi.alipaydev.com/gateway.do";
    //签名算法类型
    String sign_type = "RSA2";

    @GetMapping("/alipaytest")
    public void alipaytest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
        //构造sdk的客户端对象
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, APP_ID, APP_PRIVATE_KEY, "json", CHARSET, alipayPublicKey, sign_type); //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
//        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        //填充业务参数
        alipayRequest.setBizContent
                ("{" +
                //商户网站唯一订单号
                " \"out_trade_no\":\"20151420010101101\"," +
                //订单总金额
                " \"total_amount\":\"88.88\"," +
                //商品/交易/订单标题
                " \"subject\":\"Iphone6 16G\"," +
                //销售产品码，商家和支付宝签约的产品码
                " \"product_code\":\"QUICK_WAP_PAY\"" +
                " }");
        String form="";
        try {
            //请求支付宝下单接口, 发起http请求
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        System.out.println(form);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

}
