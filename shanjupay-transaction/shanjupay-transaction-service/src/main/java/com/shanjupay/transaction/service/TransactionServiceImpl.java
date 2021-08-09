package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.entity.PayOrder;
import com.shanjupay.transaction.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    /**
     * 从配置文件读取支付入口地址
     */
    @Value("${shanjupay.payurl}")
    String payurl;

    @Reference
    AppService appService;

    @Reference
    MerchantService merchantService;

    @Autowired
    PayOrderMapper payOrderMapper;

    @Reference
    PayChannelAgentService payChannelAgentService;

    @Autowired
    PayChannelService payChannelService;


    /**
     * 生成门店二维码的url
     * @param qrCodeDto  支付入口（url），要携带参数（将传入的参数转成json，用base64编码）
     * @return
     * @throws BusinessException
     */
    @Override
    public String createStoreQRCode(QRCodeDto qrCodeDto) throws BusinessException {
        //校验商户id和应用id和门店id的合法性
        verifyAppAndStore(qrCodeDto.getMerchantId(),qrCodeDto.getAppId(),qrCodeDto.getStoreId());

        //组装url所需要的数据
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setMerchantId(qrCodeDto.getMerchantId());
        payOrderDTO.setAppId(qrCodeDto.getAppId());
        payOrderDTO.setStoreId(qrCodeDto.getStoreId());
        //显示订单标题
        payOrderDTO.setSubject(qrCodeDto.getSubject());
        //服务类型，要写为c扫b的服务类型
        payOrderDTO.setChannel("shanju_c2b");
        payOrderDTO.setBody(qrCodeDto.getBody());//订单内容

        //转成json
        String jsonString = JSON.toJSONString(payOrderDTO);
        //base64编码
        String ticket = EncryptUtil.encodeUTF8StringBase64(jsonString);
        //目标是生成一个支付入口 的url，需要携带参数将传入的参数转成json，用base64编码
        String url = payurl + ticket;
        return url;
    }


    /**
     * 保存支付宝订单，1、保存订单到闪聚平台，2、调用支付渠道代理服务调用支付宝的接口
     * @param payOrderDTO
     * @return
     * @throws BusinessException
     */
    @Override
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException {
        //保存订单到闪聚平台数据库
        payOrderDTO = save(payOrderDTO);

        //调用支付渠道代理服务支付宝下单接口
        PaymentResponseDTO paymentResponseDTO = alipayH5(payOrderDTO.getTradeNo());
        return paymentResponseDTO;
    }


    /**
     * 保存订单到数据库（公用）
     * @param payOrderDTO
     * @return
     * @throws BusinessException
     */
    private PayOrderDTO save(PayOrderDTO payOrderDTO) throws BusinessException{
        PayOrder payOrder = PayOrderConvert.INSTANCE.dto2entity(payOrderDTO);
        //采用雪花片算法
        payOrder.setTradeNo(PaymentUtil.genUniquePayOrderNo());
        //创建时间
        payOrder.setCreateTime(LocalDateTime.now());
        //过期时间是30分钟后
        payOrder.setExpireTime(LocalDateTime.now().plus(30, ChronoUnit.MINUTES));
        payOrder.setCurrency("CNY");
        //订单状态，0：订单生成
        payOrder.setTradeState("0");

        payOrderMapper.insert(payOrder);

        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    /**
     * 调用支付渠道代理服务的支付宝下单接口
     * @param tradeNo
     * @return
     */
    private  PaymentResponseDTO alipayH5(String tradeNo){
        //组装alipayBean
        AlipayBean alipayBean = new AlipayBean();
        //从刚用save方法保存到数据库中的订单中查询订单信息
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);
        alipayBean.setOutTradeNo(tradeNo);
        String totalAmount = null;
        try {
            //分转成元
            totalAmount = AmountUtil.changeF2Y(payOrderDTO.getTotalAmount().toString());
        } catch (Exception e) {
            e.printStackTrace();
            //E_300006(300006,"订单金额转换异常")
            throw new BusinessException(CommonErrorCode.E_300006);
        }
        alipayBean.setTotalAmount(totalAmount);
        alipayBean.setSubject(payOrderDTO.getSubject());
        alipayBean.setBody(payOrderDTO.getBody());
        alipayBean.setStoreId(payOrderDTO.getStoreId());
        alipayBean.setExpireTime("30m");

        //支付渠道配置参数，从数据库查询
        //String appId,String platformChannel,String payChannel
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(),"shanju_c2b","ALIPAY_WAP");
        if(payChannelParamDTO == null) {
            throw new BusinessException(CommonErrorCode.E_300007);
        }

        String paramJson = payChannelParamDTO.getParam();
        //支付渠道参数
        AliConfigParam aliConfigParam = JSON.parseObject(paramJson, AliConfigParam.class);
        aliConfigParam.setCharest("utf-8");
        PaymentResponseDTO payOrderByAliWAP = payChannelAgentService.createPayOrderByAliWAP(aliConfigParam, alipayBean);
        log.info("支付宝H5支付响应Content:" + payOrderByAliWAP.getContent());
        return payOrderByAliWAP;
    }

    /**
     * 根据订单号查询订单信息
     * @param tradeNo
     * @return
     */
    @Override
    public PayOrderDTO queryPayOrder(String tradeNo){
        PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getTradeNo, tradeNo));
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }


    /**
     * 校验商户id和应用id和门店id的合法性
     * @param merchantId
     * @param appId
     * @param storeId
     */
    private void verifyAppAndStore(Long merchantId, String appId, Long storeId) {
        //根据 应用id和商户id查询
        Boolean aBoolean = appService.queryAppInMerchant(appId, merchantId);
        //E_200005(200005,"应用不属于当前商户")
        if(!aBoolean){
            throw new BusinessException(CommonErrorCode.E_200005);
        }
        //根据 门店id和商户id查询
        Boolean aBoolean1 = merchantService.queryStoreInMerchant(storeId, merchantId);
        //E_200006(200006,"门店不属于当前商户")
        if(!aBoolean1){
            throw new BusinessException(CommonErrorCode.E_200006);
        }
    }

    /**
     * 更新订单支付状态
     * @param tradeNo           闪聚平台订单号
     * @param payChannelTradeNo 支付宝或微信的交易流水号(第三方支付系统的订单)
     * @param state             订单状态  交易状态支付状态,0-订单生成,1-支付中(目前未使用),2-支付成功,4-关闭 5--失败
     */
    @Override
    public void updateOrderTradeNoAndTradeState(String tradeNo, String payChannelTradeNo, String state) throws BusinessException {
        LambdaUpdateWrapper<PayOrder> payOrderLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        payOrderLambdaUpdateWrapper
                .eq(PayOrder::getTradeNo, tradeNo)
                .set(PayOrder::getTradeState, state)
                .set(PayOrder::getPayChannelTradeNo, payChannelTradeNo);
        if(state!=null && "2".equals(state)){
            payOrderLambdaUpdateWrapper.set(PayOrder::getPaySuccessTime,LocalDateTime.now());
        }
        payOrderMapper.update(null, payOrderLambdaUpdateWrapper);
    }


}
