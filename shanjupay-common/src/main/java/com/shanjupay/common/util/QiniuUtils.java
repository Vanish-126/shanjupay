package com.shanjupay.common.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * 七牛云测试工具类
 * @author Administrator
 * @version 1.0
 **/
public class QiniuUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(QiniuUtils.class);

    /**
     *  文件上传的工具方法
     *  修改自官方文档Java SDK代码
     * @param accessKey 在传输中包含
     * @param secretKey 用来生成数字签名
     * @param bucket
     * @param bytes
     * @param fileName 外部传进来，七牛云上的文件名称和此保持一致
     */
    public static void upload2qiniu(String accessKey, String secretKey, String bucket, byte[] bytes,String fileName) throws RuntimeException{
        // 构造一个带指定Region对象的配置类，指定存储区域，和网站中存储空间选择的区域一致
        Configuration cfg = new Configuration(Region.huadong());
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        // 默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = fileName;
        try {
            // 认证
            Auth auth = Auth.create(accessKey, secretKey);
            // 认证通过后得到token（令牌）
            String upToken = auth.uploadToken(bucket);
            try {
                Response response = uploadManager.put(bytes, key, upToken);
                // 解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                System.out.println(putRet.key);
                System.out.println(putRet.hash);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                LOGGER.error("上传文件到七牛：{}",ex.getMessage());
                try {
                    LOGGER.error(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
                throw new RuntimeException(r.bodyString());
            }
        } catch (Exception ex) {
            LOGGER.error("上传文件到七牛：{}",ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * 测试文件上传
     * https://developer.qiniu.com/kodo/1239/java 样例代码-字节数组上传
     * @throws FileNotFoundException
     */
    private static void testUpload() throws FileNotFoundException {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.huadong());
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        String accessKey = "0wmyLj0s2-f-y3wgmZvGzEUk9YPLjBMhbPhL53FE";
        String secretKey = "WfGr1nDFgV3f12uedi8ayMjoVmkGQJzpHYD6A6nX";
        String bucket = "zhangsan-shanju222";
        // 如果不指定key，默认以文件内容的hash值作为文件名
        String key = UUID.randomUUID().toString() + ".jpg";
        FileInputStream fileInputStream = null;
        try {
            // 得到本地文件的字节数组
            String filePath = "C:\\Users\\Lenovo\\Pictures\\rei子\\16181677442101.jpg";
            fileInputStream = new FileInputStream(new File(filePath));
            byte[] bytes = IOUtils.toByteArray(fileInputStream);
            // byte[] uploadBytes = "hello qiniu cloud".getBytes("utf-8");
            // 认证
            Auth auth = Auth.create(accessKey, secretKey);
            // 认证通过后得到token（令牌）
            String upToken = auth.uploadToken(bucket);
            try {
                // 上传文件，参数：字节数组，key，token对象
                // key：建议我们自己生成一个不重复的名称
                Response response = uploadManager.put(bytes, key, upToken);
                // 解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                System.out.println(putRet.key);
                System.out.println(putRet.hash);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                try {
                    System.err.println(r.bodyString());
                } catch (QiniuException ex2) {
                    // ignore
                }
            }
        } catch (UnsupportedEncodingException ex) {
            // ignore
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getDownloadUrl() throws UnsupportedEncodingException {
        //私有空间
        String fileName = "0a3428f8-4735-48dd-9843-03f355321d01";
        String domainOfBucket = "qv77t6gt1.hd-bkt.clouddn.com";
        String encodedFileName = URLEncoder.encode(fileName, "utf-8").replace("+", "%20");
        String publicUrl = String.format("%s/%s", domainOfBucket, encodedFileName);
        String accessKey = "0wmyLj0s2-f-y3wgmZvGzEUk9YPLjBMhbPhL53FE";
        String secretKey = "WfGr1nDFgV3f12uedi8ayMjoVmkGQJzpHYD6A6nX";
        Auth auth = Auth.create(accessKey, secretKey);
        long expireInSeconds = 3600;//1小时，可以自定义链接过期时间
        String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);
        System.out.println(finalUrl);
    }

    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
        //上传测试
        QiniuUtils.getDownloadUrl();
//        QiniuUtils.testUpload();
    }
}
