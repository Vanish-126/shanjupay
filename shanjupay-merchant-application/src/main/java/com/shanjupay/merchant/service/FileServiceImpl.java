package com.shanjupay.merchant.service;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.QiniuUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 文件上传到七牛云
 * Service：spring注解，供本地调用
 * @author Administrator
 * @version 1.0
 **/
@Service
public class FileServiceImpl implements FileService{
    //nacos配置：merchant-application.yaml
    @Value("${oss.qiniu.url}")//qv77t6gt1.hd-bkt.clouddn.com/
    private String qiniuUrl;
    @Value("${oss.qiniu.accessKey}")//0wmyLj0s2-f-y3wgmZvGzEUk9YPLjBMhbPhL53FE
    private String accessKey;
    @Value("${oss.qiniu.secretKey}")//WfGr1nDFgV3f12uedi8ayMjoVmkGQJzpHYD6A6nX
    private String secretKey;
    @Value("${oss.qiniu.bucket}")//zhangsan-shanju222
    private String bucket;

    /**
     * 上传证件信息
     * @param bytes    文件字节数组
     * @param fileName 文件名
     * @return 文件访问路径（绝对的url）
     * @throws BusinessException
     */
    @Override
    public String upload(byte[] bytes, String fileName) throws BusinessException {
        //调用common 下的工具类
        //String accessKey,String secretKey,String bucket, byte[] bytes,String fileName
        try {
            QiniuUtils.upload2qiniu(accessKey, secretKey, bucket, bytes, fileName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            //E_100106(100106,"上传错误")
            throw new BusinessException(CommonErrorCode.E_100106);
        }
        //上传成功返回文件的访问地址（绝对路径）
        String url = qiniuUrl + fileName;
        return url;
    }
}
