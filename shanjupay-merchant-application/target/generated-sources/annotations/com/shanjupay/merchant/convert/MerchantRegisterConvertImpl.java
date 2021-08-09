package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2021-07-07T14:48:12+0800",
    comments = "version: 1.3.0.Final, compiler: javac, environment: Java 1.8.0_291 (Oracle Corporation)"
)
public class MerchantRegisterConvertImpl implements MerchantRegisterConvert {

    @Override
    public MerchantDTO vo2dto(MerchantRegisterVO vo) {
        if ( vo == null ) {
            return null;
        }

        MerchantDTO merchantDTO = new MerchantDTO();

        merchantDTO.setUsername( vo.getUsername() );
        merchantDTO.setPassword( vo.getPassword() );
        merchantDTO.setMobile( vo.getMobile() );

        return merchantDTO;
    }

    @Override
    public MerchantRegisterVO dto2vo(MerchantDTO dto) {
        if ( dto == null ) {
            return null;
        }

        MerchantRegisterVO merchantRegisterVO = new MerchantRegisterVO();

        merchantRegisterVO.setMobile( dto.getMobile() );
        merchantRegisterVO.setUsername( dto.getUsername() );
        merchantRegisterVO.setPassword( dto.getPassword() );

        return merchantRegisterVO;
    }
}
