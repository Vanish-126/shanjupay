package com.shanjupay.merchant.common.intercept;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.domain.ErrorCode;
import com.shanjupay.common.domain.RestErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器
 * 与@Exceptionhandler配合使用实现全局异常处理
 * @author Administrator
 * @version 1.0
 **/
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    //捕获Exception异常
    @ExceptionHandler(value = Exception.class)
    //转换为JSON
    @ResponseBody
    //500错误
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse processException(HttpServletRequest request,
                                              HttpServletResponse response,
                                              Exception e){
        // 解析异常信息
        // 如果是系统自定义异常，直接取出errCode和errMessage
        if(e instanceof BusinessException){
            LOGGER.info(e.getMessage(),e);
            //解析系统自定义异常信息
            BusinessException businessException= (BusinessException) e;
            ErrorCode errorCode = businessException.getErrorCode();
            //错误代码
            int code = errorCode.getCode();
            //错误信息
            String desc = errorCode.getDesc();
            //int -> String
            return new RestErrorResponse(String.valueOf(code), desc);
        }

        LOGGER.error("系统异常：",e);
        //UNKNOWN(999999,"未知错误");
        int code = CommonErrorCode.UNKNOWN.getCode();
        String desc = CommonErrorCode.UNKNOWN.getDesc();
        return new RestErrorResponse(String.valueOf(code), desc);

    }
}
