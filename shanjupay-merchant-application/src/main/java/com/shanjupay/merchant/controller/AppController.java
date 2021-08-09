package com.shanjupay.merchant.controller;

import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Administrator
 * @version 1.0
 **/

@Api(value = "商户平台-应用管理", tags = "商户平台-应用相关", description = "商户平台-应用相关")
@RestController
public class AppController {

    //import com.shanjupay.merchant.api.AppService;
    //import org.apache.dubbo.config.annotation.Reference;
    @Reference
    AppService appService;


    /**
     * 接口13：商户平台应用创建应用接口
     * @param app
     * @return
     */
    @ApiOperation("商户创建应用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用信息", required = true, dataType = "AppDTO", paramType = "body")})
    @PostMapping(value = "/my/apps")
    //@RequestBody：JSON
    public AppDTO createApp(@RequestBody AppDTO app){
        //需要TokenTemp，根据merchant.id和merchant.audit_status生成token，
        //然后再测试的请求头中加入authorization属性，比如：Authorization : Bearer eyJtZXJjaGFudElkIjoyfQ==
        Long merchantId = SecurityUtil.getMerchantId();
        return  appService.createApp(merchantId, app);
    }

    /**
     * 查询商户下的应用列表（多行）
     * @return
     */
    @ApiOperation("查询商户下的应用列表")
    @GetMapping(value = "/my/apps")
    public List<AppDTO> queryMyApps() {
        //商户id
        Long merchantId = SecurityUtil.getMerchantId();
        return appService.queryAppByMerchant(merchantId);
    }

    /**
     * 根据应用id查询应用信息（单行）
     * swagger: name = appId, Description = "应用id" type = String;
     * 关于paramType:
     * header: 请求参数的获取，@RequestHeader(代码中接收注解)
     * query: 请求参数的获取：@RequestParam(代码中接收注解)
     * path(用于restful接口): 请求参数的获取：@PathVariable(代码中接收注解)
     * body: 请求参数的获取：@RequestBody(代码中接收注解)
     * form: 不常用
     * @param appId
     * @return
     */
    @ApiOperation("根据应用id查询应用信息")
    @ApiImplicitParam(value = "应用id",name = "appId",dataType = "String",paramType = "path")
    @GetMapping(value = "/my/apps/{appId}")
    public AppDTO getApp(@PathVariable("appId") String appId){
        return appService.getAppById(appId);
    }

}
