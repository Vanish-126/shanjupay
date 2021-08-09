package com.shanjupay.paymentagent.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.paymentagent.api.dto.TradeStatus;
import com.shanjupay.paymentagent.common.constant.AliCodeConstants;
import com.shanjupay.paymentagent.message.PayProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
@Slf4j
public class PayChannelAgentServiceImpl implements PayChannelAgentService {

    @Autowired
    PayProducer payProducer;

    /**
     * 调用支付宝的下单接口
     *
     * @param aliConfigParam 支付渠道配置的参数（配置的支付宝的必要参数）
     * @param alipayBean     业务参数（商户订单号，订单标题，订单描述...）
     * @return 统一返回PaymentResponseDTO
     */
    @Override
    public PaymentResponseDTO createPayOrderByAliWAP(AliConfigParam aliConfigParam, AlipayBean alipayBean) throws BusinessException {
        //支付宝接口网关地址
        String url = aliConfigParam.getUrl();
        //支付宝应用id
        String appId = aliConfigParam.getAppId();
        //应用私钥
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();
        //json格式
        String format = aliConfigParam.getFormat();
        //编码
        String charest = aliConfigParam.getCharest();
        //支付宝公钥
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();
        //签名算法
        String signtype = aliConfigParam.getSigntype();
        //构造sdk的客户端对象
        AlipayClient alipayClient = new DefaultAlipayClient(url, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype); //获得初始化的AlipayClient

        AlipayTradeWapPayModel model  = new AlipayTradeWapPayModel();
        //商户的订单，就是闪聚平台的订单
        model.setOutTradeNo(alipayBean.getOutTradeNo());
        //订单金额（元）
        model.setTotalAmount(alipayBean.getTotalAmount());
        model.setSubject(alipayBean.getSubject());
        model.setBody(alipayBean.getBody());
        //产品代码，固定QUICK_WAP_PAY
        model.setProductCode("QUICK_WAP_PAY");
        //订单过期时间
        model.setTimeoutExpress(alipayBean.getExpireTime());

        //创建API对应的request
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        alipayRequest.setBizModel(model);
        alipayRequest.setReturnUrl(aliConfigParam.getReturnUrl());
        //支付结果异步通知的url
        alipayRequest.setNotifyUrl(aliConfigParam.getNotifyUrl());

        try {
            // 请求支付宝下单接口,发起http请求
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(alipayRequest);
            PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
            log.info("调用支付宝下单接口，响应内容:{}", response.getBody());
            // 支付宝的响应结果由DTO接收
            paymentResponseDTO.setContent(response.getBody());

            //生产消息类
            //向MQ发一条延迟消息,支付结果查询
            PaymentResponseDTO<AliConfigParam> notice = new PaymentResponseDTO<AliConfigParam>();
            //闪聚平台的订单
            notice.setOutTradeNo(alipayBean.getOutTradeNo());
            notice.setContent(aliConfigParam);
            //标识是查询支付宝的接口
            notice.setMsg("ALIPAY_WAP");
            //发送延迟消息
            payProducer.payOrderNotice(notice);

            return paymentResponseDTO;
        } catch (AlipayApiException e) {
            e.printStackTrace();
            //E_400002(400002,"支付宝确认支付失败")
            throw new BusinessException(CommonErrorCode.E_400002);
        }
    }
    /**
     * 查询支付宝订单状态
     * @param aliConfigParam 支付渠道参数
     * @param outTradeNo     闪聚平台的订单号
     * @return
     */
    @Override
    public PaymentResponseDTO queryPayOrderByAli(AliConfigParam aliConfigParam, String outTradeNo) throws BusinessException{
        String url = aliConfigParam.getUrl();//支付宝接口网关地址
        String appId = aliConfigParam.getAppId();//支付宝应用id
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();//应用私钥
        String format = aliConfigParam.getFormat();//json格式
        String charest = aliConfigParam.getCharest();//编码
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();//支付宝公钥
        String signtype = aliConfigParam.getSigntype();//签名算法
        String returnUrl = aliConfigParam.getReturnUrl();//支付成功跳转的url
        String notifyUrl = aliConfigParam.getNotifyUrl();//支付结果异步通知的url
        //构造sdk的客户端对象
        AlipayClient alipayClient = new DefaultAlipayClient(url, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype); //获得初始化的AlipayClient

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeWapPayModel model  = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);//商户的订单，就是闪聚平台的订单
        request.setBizModel(model);

        AlipayTradeQueryResponse response = null;
        try {
            //请求支付宝订单状态查询接口
            response = alipayClient.execute(request);

            //支付宝响应的code，10000表示接口调用成功
            String code = response.getCode();
            if(AliCodeConstants.SUCCESSCODE.equals(code)){
                String tradeStatusString = response.getTradeStatus();
                //解析支付宝返回的状态，解析成闪聚平台的TradeStatus
                TradeStatus tradeStatus = covertAliTradeStatusToShanjuCode(tradeStatusString);
                //String tradeNo(支付宝订单号), String outTradeNo（闪聚平台的订单号）, TradeStatus tradeState（订单状态）, String msg（返回信息）
                return PaymentResponseDTO.success(response.getTradeNo(), response.getOutTradeNo(), tradeStatus, response.getMsg());
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //String msg, String outTradeNo, TradeStatus tradeState
        return PaymentResponseDTO.fail("支付宝订单状态查询失败",outTradeNo,TradeStatus.UNKNOWN);
    }

    /**
     * 解析支付宝的订单状态为闪聚平台的状态
     * @param aliTradeStatus
     * @return
     */
    private TradeStatus covertAliTradeStatusToShanjuCode(String aliTradeStatus){
        switch (aliTradeStatus){
            //注意case语句的位置
            case AliCodeConstants.TRADE_FINISHED:
            case AliCodeConstants.TRADE_SUCCESS:
                return TradeStatus.SUCCESS;//业务交易支付 明确成功
            case AliCodeConstants.TRADE_CLOSED:
                return TradeStatus.REVOKED;//交易已撤销
            case AliCodeConstants.WAIT_BUYER_PAY:
                return TradeStatus.USERPAYING;//交易新建，等待支付
            default:
                return TradeStatus.FAILED;//交易失败
        }
    }

}
