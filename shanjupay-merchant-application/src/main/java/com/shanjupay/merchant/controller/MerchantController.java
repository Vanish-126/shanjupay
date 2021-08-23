package com.shanjupay.merchant.controller;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.PhoneUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import com.shanjupay.merchant.convert.MerchantDetailConvert;
import com.shanjupay.merchant.convert.MerchantRegisterConvert;
import com.shanjupay.merchant.service.FileService;
import com.shanjupay.merchant.service.SmsService;
import com.shanjupay.merchant.vo.MerchantDetailVO;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


@RestController
@Api(value="value=商户平台应用接口",tags="tags=商户平台应用接口",description="description=商户平台应用接口")
public class MerchantController {
    /**
     * org.apache.dubbo.config.annotation.Reference
     * Reference注解：注入远程调用的接口
     */
    @Reference
    MerchantService merchantService;

    /**
     * org.springframework.beans.factory.annotation.Autowired
     * Autowired注解：将本地的Bean注入进入
     * SmsService实现类在shanjupay-merchant-application：com.shanjupay.merchant.service中
     */
    @Autowired
    SmsService smsService;

    @Autowired
    FileService fileService;


    /**
     * 测试1：
     * 根据id查询商户信息
     * 测试接口：localhost:56010/merchant/merchants/1
     * 56010是网关启动端口
     * 注意：Header中添加授权token和租户id
     * @param id
     * @return
     */
    @ApiOperation(value="根据id查询商户信息")
    @GetMapping("/merchants/{id}")
    public MerchantDTO queryMerchantById(@PathVariable("id") Long id){
        MerchantDTO merchantDTO = merchantService.queryMerchantById(id);
        return merchantDTO;
    }

    /**
     * 测试2：
     * 获取登录用户的商户信息
     * 测试接口：localhost:56010/merchant/my/merchants
     * @return
     */
    @ApiOperation("获取登录用户的商户信息")
    @GetMapping(value="/my/merchants")
    public MerchantDTO getMyMerchantInfo(){
        // 从token中获取商户id
        Long merchantId = SecurityUtil.getMerchantId();
        return merchantService.queryMerchantById(merchantId);
    }

    /**
     * 接口1：获取手机验证码
     * ApiImplicitParam ：表示单独的请求参数。
     * required ：参数是否必填。
     * paramType ：查询参数类型。
     * query ：直接跟参数完成自动映射赋值。
     * RequestParam ：主要用于将请求参数的数据映射到控制层方法的参数上
     * 测试接口：localhost:57010/merchant/sms?phone=13334444
     * @param phone
     * @return
     */
    @ApiOperation("获取手机验证码")
    @ApiImplicitParam(value = "手机号",name = "phone",required = true,dataType = "string",paramType = "query")
    @GetMapping("/sms")
    public String getSMSCode(@RequestParam("phone") String phone){
        // 发送验证码
        return smsService.sendMsg(phone);
    }


    /**
     * 接口3：商户平台应用的商户注册接口（controller层的实现）
     * 测试时需要在body中指定JSON内容
     * merchantRegisterVO：从前端传来的JSON信息
     * @param merchantRegisterVO
     * @return
     */
    @ApiOperation("商户注册")
    @ApiImplicitParam(value = "商户注册信息",name = "merchantRegisterVO",required = true,dataType = "MerchantRegisterVO",paramType = "body")
    @PostMapping("/merchants/register")
    public MerchantRegisterVO registerMerchant(@RequestBody MerchantRegisterVO merchantRegisterVO){
        // 校验参数的合法性
        // E_100108(100108,"传入对象为空")
        if(merchantRegisterVO == null){
            throw new BusinessException(CommonErrorCode.E_100108);
        }
        // E_100112(100112,"手机号为空")
        if(StringUtils.isBlank(merchantRegisterVO.getMobile())){
            throw new BusinessException(CommonErrorCode.E_100112);
        }
        // 手机号格式校验
        // E_100109(100109,"手机号格式不正确")
        if(!PhoneUtil.isMatches(merchantRegisterVO.getMobile())){
            throw new BusinessException(CommonErrorCode.E_100109);
        }

        // 校验验证码，需要sms KEY和短信验证码，可以查阅redis
        smsService.checkVerifiyCode(merchantRegisterVO.getVerifykey(), merchantRegisterVO.getVerifyCode());

        // 使用MapStruct转换对象：VO -> DTO
        MerchantDTO merchantDTO = MerchantRegisterConvert.INSTANCE.vo2dto(merchantRegisterVO);
        merchantService.createMerchant(merchantDTO);

        return merchantRegisterVO;
    }


    /**
     * 接口4：商户平台应用上传证件信息（controller层实现）
     * MultipartFile可以将请求的file转换为字节流和文件名
     * 测试时需要在body中添加文件所在地址
     * @param multipartFile
     * @return
     * @throws IOException
     */
    @ApiOperation("上传证件照")
    @PostMapping("/upload")
    public String upload(@ApiParam(value = "证件照",required = true) @RequestParam("file") MultipartFile multipartFile) throws IOException {
        // 调用fileService上传文件
        // 生成的文件名称fileName，要保证它的唯一
        // 文件原始名称
        String originalFilename = multipartFile.getOriginalFilename();
        // 扩展名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") - 1);
        // 文件名称修改为全局唯一码 + 后缀名
        String fileName = UUID.randomUUID() + suffix;
        return fileService.upload(multipartFile.getBytes(), fileName);
    }


    /**
     * 接口6：商户平台应用资质申请
     * 测试时需要在header中添加authorization属性，内容为token字符串
     * 并指定Content-Type为application/json
     * @param merchantInfo
     */
    @ApiOperation("资质申请")
    @PostMapping("/my/merchants/save")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "merchantInfo", value = "商户认证资料", required = true, dataType = "MerchantDetailVO", paramType = "body")
    })
    public void saveMerchant(@RequestBody MerchantDetailVO merchantInfo){
        //解析token，取出当前登录商户的id
        Long merchantId = SecurityUtil.getMerchantId();
        //Long merchantId, MerchantDTO merchantDTO
        System.out.println(merchantId);
        MerchantDTO merchantDTO = MerchantDetailConvert.INSTANCE.vo2dto(merchantInfo);
        merchantService.applyMerchant(merchantId, merchantDTO);
    }


//    @ApiOperation("测试")
//    @GetMapping(path = "/hello")
//    public String hello(){
//        return "hello";
//    }
//
//    @ApiOperation("测试")
//    @ApiImplicitParam(name = "name", value = "姓名", required = true, dataType = "string")
//    @PostMapping(value = "/hi")
//    public String hi(String name) {
//        return "hi,"+name;
//    }

}
