package com.shanjupay.merchant.convert;


import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper注解快速实现对象转换
 */
@Mapper
public interface MerchantConvert {

    MerchantConvert INSTANCE = Mappers.getMapper(MerchantConvert.class);

    /**
     * entity -> dto
     * @param entity
     * @return
     */
    MerchantDTO entity2dto(Merchant entity);

    /**
     * dto -> entity
     * @param dto
     * @return
     */
    Merchant dto2entity(MerchantDTO dto);

    /**
     * 测试
     * @param args
     */
    public static void main(String[] args) {
        MerchantDTO merchantDTO = new MerchantDTO();
        merchantDTO.setUsername("测试");
        merchantDTO.setMobile("123456");
        //dto转entity
        Merchant entity = MerchantConvert.INSTANCE.dto2entity(merchantDTO);
        entity.setMobile("123444554");
        //entity转dto
        MerchantDTO merchantDTO1 = MerchantConvert.INSTANCE.entity2dto(entity);
        System.out.println(merchantDTO1);
    }
}
