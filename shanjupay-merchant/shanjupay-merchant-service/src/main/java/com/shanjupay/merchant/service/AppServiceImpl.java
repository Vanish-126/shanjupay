package com.shanjupay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.convert.AppConvert;
import com.shanjupay.merchant.entity.App;
import com.shanjupay.merchant.entity.Merchant;
import com.shanjupay.merchant.mapper.AppMapper;
import com.shanjupay.merchant.mapper.MerchantMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * @author Administrator
 * @version 1.0
 **/
@org.apache.dubbo.config.annotation.Service
public class AppServiceImpl implements AppService {

    @Autowired
    AppMapper appMapper;

    @Autowired
    MerchantMapper merchantMapper;
    /**
     * 接口12：创建应用
     * @param merchantId 商户id
     * @param appDTO     应用信息
     * @return 创建成功的应用信息
     * @throws BusinessException
     */
    @Override
    public AppDTO createApp(Long merchantId, AppDTO appDTO) throws BusinessException {
        //1）异常处理
        //E_300009(300009,"传入对象为空或者缺少必要的参数")
        //StringUtils.isBlank 一次性校验字符串是否为null、""、"空格"三种情况
        //除了传入参数外，JSON不能省略AppName
        if(merchantId == null || appDTO == null || StringUtils.isBlank(appDTO.getAppName())){
            throw new BusinessException(CommonErrorCode.E_300009);
        }
        //E_200002(200002,"商户不存在")
        Merchant merchant = merchantMapper.selectById(merchantId);
        if(merchant == null){
            throw new BusinessException(CommonErrorCode.E_200002);
        }
        //E_200003(200003,"商户还未通过认证审核，不能创建应用")
        String auditStatus = merchant.getAuditStatus();
        //auditStatus = 2表示审核通过
        if(!"2".equals(auditStatus)){
            throw new BusinessException(CommonErrorCode.E_200003);
        }
        //E_200004(200004,"应用名称已经存在，请使用其他名称")
        String appName = appDTO.getAppName();
        //isExistAppName()见后面方法定义
        Boolean existAppName = isExistAppName(appName);
        if (existAppName){
            throw new BusinessException(CommonErrorCode.E_200004);
        }

        //2）生成应用ID
        String appId = UUID.randomUUID().toString();
        // DTO -> entity
        App entity = AppConvert.INSTANCE.dto2entity(appDTO);
        // 应用id:随机数
        entity.setAppId(appId);
        // 商户id：比如2
        entity.setMerchantId(merchantId);
        //向数据库的app表中插入数据
        appMapper.insert(entity);

        //entity -> DTO
        //返回DTO JSON格式
        return AppConvert.INSTANCE.entity2dto(entity);
    }

    /**
     * 判断应用名称是否存在
     * @param appName
     * @return
     */
    private Boolean isExistAppName(String appName){
        //查询app表中的appName字段，若存在，则计数大于0，返回true
        Integer count = appMapper.selectCount(new LambdaQueryWrapper<App>().eq(App::getAppName, appName));
        return count > 0;
    }

    /**
     * 根据商户id查询应用列表（多行）
     * @param merchantId
     * @return
     * @throws BusinessException
     */
    @Override
    public List<AppDTO> queryAppByMerchant(Long merchantId) throws BusinessException {
        //从数据库app表中获取merchantId,并与传入参数merchantId比较，若相等，返回app表对应行
        List<App> apps = appMapper.selectList(new LambdaQueryWrapper<App>()
                .eq(App::getMerchantId, merchantId));
        //返回DTO列表
        return AppConvert.INSTANCE.listEntity2dto(apps);
    }

    /**
     * 根据应用id查询应用信息（一行）
     * @param appId
     * @return
     * @throws BusinessException
     */
    @Override
    public AppDTO getAppById(String appId) throws BusinessException {
        //只返回一行
        App app = appMapper.selectOne(new LambdaQueryWrapper<App>().eq(App::getAppId, appId));
        return AppConvert.INSTANCE.entity2dto(app);
    }


    /**
     * 查询数据库表中是否存在应用和商户的对应关系
     * @param appId
     * @param merchantId
     * @return true存在，false不存在
     */
    @Override
    public Boolean queryAppInMerchant(String appId, Long merchantId) {
        Integer count = appMapper.selectCount(new LambdaQueryWrapper<App>()
                .eq(App::getAppId, appId)
                .eq(App::getMerchantId, merchantId));
        return count > 0;
    }

}
