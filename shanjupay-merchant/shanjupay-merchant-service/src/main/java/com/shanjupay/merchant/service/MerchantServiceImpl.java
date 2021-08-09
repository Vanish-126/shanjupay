package com.shanjupay.merchant.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.common.util.PhoneUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.StaffDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;
import com.shanjupay.merchant.convert.MerchantConvert;
import com.shanjupay.merchant.convert.StaffConvert;
import com.shanjupay.merchant.convert.StoreConvert;
import com.shanjupay.merchant.entity.Merchant;
import com.shanjupay.merchant.entity.Staff;
import com.shanjupay.merchant.entity.Store;
import com.shanjupay.merchant.entity.StoreStaff;
import com.shanjupay.merchant.mapper.MerchantMapper;
import com.shanjupay.merchant.mapper.StaffMapper;
import com.shanjupay.merchant.mapper.StoreMapper;
import com.shanjupay.merchant.mapper.StoreStaffMapper;
import com.shanjupay.user.api.TenantService;
import com.shanjupay.user.api.dto.tenant.CreateTenantRequestDTO;
import com.shanjupay.user.api.dto.tenant.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by Administrator.
 */
@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {


    @Autowired
    MerchantMapper merchantMapper;

    @Autowired
    StoreMapper storeMapper;

    @Autowired
    StaffMapper staffMapper;

    @Autowired
    StoreStaffMapper storeStaffMapper;

    @Reference//引入shanjupay-user-api
    TenantService tenantService;

    /**
     * 根据id查询商户信息
     * @param id
     * @return
     */
    @Override
    public MerchantDTO queryMerchantById(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        //convert类的功能相当于：
        //MerchantDTO merchantDTO = new MerchantDTO();
        //merchantDTO.setId(merchant.getId);
        //return merchantDTO;
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }

    /**
     * 根据租户id查询商户的信息
     * @param tenantId
     * @return
     */
    @Override
    public MerchantDTO queryMerchantByTenantId(Long tenantId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getTenantId, tenantId));
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }

    /**
     * 接口2/11：商户服务的商户注册接口（service层的实现）
     * 注册商户服务接口，接收账号、密码、手机号
     * 为了可扩展性使用merchantDto接收数据
     * 调用SaaS接口：新增租户、用户、绑定租户和用户的关系，初始化权限
     * 异常：不可预知异常统一定义为99999异常代码；自定义异常取出错误代码及错误信息
     * @param merchantDTO 商户注册信息
     * @return 注册成功的商户信息
     */
    @Override
    public MerchantDTO createMerchant(MerchantDTO merchantDTO) throws BusinessException {
//        ====================未接入SaaS=====================
//        Merchant merchant = new Merchant();
//        //设置审核状态0-未申请,1-已申请待审核,2-审核通过,3-审核拒绝  merchant.setAuditStatus("0");
//        //设置手机号
//        merchant.setMobile(merchantDTO.getMobile());
//        //...
//        //保存商户
//        merchantMapper.insert(merchant);
//        //将新增商户id返回
//        merchantDTO.setId(merchant.getId());
//        return merchantDTO;
//        ====================接入SaaS=======================
        //1.校验
        //E_100108(100108,"传入对象为空")
        if(merchantDTO == null){
            throw new BusinessException(CommonErrorCode.E_100108);
        }
        //E_100112(100112,"手机号为空")
        if(StringUtils.isBlank(merchantDTO.getMobile())){
            throw new BusinessException(CommonErrorCode.E_100112);
        }
        //E_100111(100111,"密码为空")
        if(StringUtils.isBlank(merchantDTO.getPassword())){
            throw new BusinessException(CommonErrorCode.E_100111);
        }
        //E_100109(100109,"手机号格式不正确")
        if(!PhoneUtil.isMatches(merchantDTO.getMobile())){
            throw new BusinessException(CommonErrorCode.E_100109);
        }
        //E_100113(100113,"手机号已存在")
        Integer count = merchantMapper
                .selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMobile, merchantDTO.getMobile()));
        if(count > 0){
            throw new BusinessException(CommonErrorCode.E_100113);
        }

        //2.添加租户和账号的绑定关系
        CreateTenantRequestDTO createTenantRequestDTO = new CreateTenantRequestDTO();
        //1、手机号
        createTenantRequestDTO.setMobile(merchantDTO.getMobile());
        //2、账号
        createTenantRequestDTO.setUsername(merchantDTO.getUsername());
        //3、密码
        createTenantRequestDTO.setPassword(merchantDTO.getPassword());
        //4、租户类型
        createTenantRequestDTO.setTenantTypeCode("shanju-merchant");
        //5、默认套餐
        //根据套餐进行分配权限
        createTenantRequestDTO.setBundleCode("shanju-merchant");
        //6、租户名称，同用户名
        createTenantRequestDTO.setName(merchantDTO.getUsername());

        //如果租户在SaaS已经存在，SaaS返回此租户的信息，否则添加信息
        TenantDTO tenantAndAccount = tenantService.createTenantAndAccount(createTenantRequestDTO);
        //E_200012(200012,"租户不存在")
        if(tenantAndAccount == null || tenantAndAccount.getId() == null){
            throw new BusinessException(CommonErrorCode.E_200012);
        }

        //判断租户是否已经注册过商户
        Long tenantId = tenantAndAccount.getId();
        //根据租户id从merchant表查询，如果存在记录则不允许添加商户
        Integer count1 = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getTenantId, tenantId));
        //E_200017(200017,"商户在当前租户下已经注册，不可重复注册")
        if(count1 > 0){
            throw new BusinessException(CommonErrorCode.E_200017);
        }

        //3.设置商户所属租户
        //DTO -> entity
        Merchant merchant = MerchantConvert.INSTANCE.dto2entity(merchantDTO);
        //设置所对应的租户的Id
        merchant.setTenantId(tenantId);
        //设置审核状态：0-未申请，1-已申请待审核，2-审核通过，3-审核拒绝
        merchant.setAuditStatus("0");
        //调用mapper向数据库写入记录
        merchantMapper.insert(merchant);

        //4.新增门店，创建根门店，调用接口7
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setStoreName("根门店");
        storeDTO.setMerchantId(merchant.getId());
        storeDTO.setStoreStatus(true);
        StoreDTO store = createStore(storeDTO);

        //5.新增员工，并设置归属门店，调用接口8
        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setMobile(merchantDTO.getMobile());
        staffDTO.setUsername(merchantDTO.getUsername());
        staffDTO.setStoreId(store.getId());
        staffDTO.setMerchantId(merchant.getId());
        staffDTO.setStaffStatus(true);
        StaffDTO staff = createStaff(staffDTO);

        //6.为门店设置管理员，调用接口9
        bindStaffToStore(store.getId(),staff.getId());

        //entity -> DTO
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }

    /**
     * 接口5：商户服务资质申请
     * @param merchantId  商户id
     * @param merchantDTO 资质申请的信息
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void applyMerchant(Long merchantId, MerchantDTO merchantDTO) throws BusinessException {
        //E_300009(300009,"传入对象为空或者缺少必要的参数")
        if(merchantId == null || merchantDTO == null){
            throw new BusinessException(CommonErrorCode.E_300009);
        }
        //校验merchantId合法性，查询商户表，如果查询不到记录，认为非法
        //E_200002(200002,"商户不存在")
        Merchant merchant = merchantMapper.selectById(merchantId);
        if(merchant == null){
            throw new BusinessException(CommonErrorCode.E_200002);
        }
        //将dto转成entity
        Merchant entity = MerchantConvert.INSTANCE.dto2entity(merchantDTO);
        //将必要的参数设置到entity
        entity.setId(merchant.getId());
        entity.setMobile(merchant.getMobile());//因为资质申请的时候手机号不让改，还使用数据库中原来的手机号
        entity.setAuditStatus("1");//审核状态1-已申请待审核
        entity.setTenantId(merchant.getTenantId());
        //调用mapper更新商户表
        merchantMapper.updateById(entity);
    }

    /**
     * 接口7：新增门店
     * @param storeDTO 门店信息
     * @return 新增成功的门店信息
     * @throws BusinessException
     */
    @Override
    public StoreDTO createStore(StoreDTO storeDTO) throws BusinessException {
        Store entity = StoreConvert.INSTANCE.dto2entity(storeDTO);
        log.info("新增门店：{}", JSON.toJSONString(entity));
        ////插入数据库shanjupay_merchant_service.store表
        storeMapper.insert(entity);
        return StoreConvert.INSTANCE.entity2dto(entity);
    }

    /**
     * 接口8：新增员工
     * @param staffDTO 员工信息
     * @return 新增成功的员工信息
     * @throws BusinessException
     */
    @Override
    public StaffDTO createStaff(StaffDTO staffDTO) throws BusinessException {
        //E_300009(300009,"传入对象为空或者缺少必要的参数")
        if(staffDTO ==  null
                || StringUtils.isBlank(staffDTO.getMobile())
                || StringUtils.isBlank(staffDTO.getUsername())
                || staffDTO.getStoreId() == null) {
            throw new BusinessException(CommonErrorCode.E_300009);
        }
        //E_100114(100114,"用户名已存在")
        if(isExistStaffByUserName(staffDTO.getUsername(), staffDTO.getMerchantId())){
            throw new BusinessException(CommonErrorCode.E_100114);
        }
        //E_100113(100113,"手机号已存在")
        if(isExistStaffByMobile(staffDTO.getMobile(), staffDTO.getMerchantId())){
            throw new BusinessException(CommonErrorCode.E_100113);
        }
        //dto->entity
        Staff staff = StaffConvert.INSTANCE.dto2entity(staffDTO);
        //插入数据库shanjupay_merchant_service.staff表
        staffMapper.insert(staff);
        return StaffConvert.INSTANCE.entity2dto(staff);
    }

    /**
     * 接口9：绑定门店和员工对应关系
     * @param storeId
     * @param staffId
     * @throws BusinessException
     */
    @Override
    public void bindStaffToStore(Long storeId, Long staffId) throws BusinessException {
        StoreStaff storeStaff = new StoreStaff();
        // 员工id
        storeStaff.setStaffId(staffId);
        // 门店id
        storeStaff.setStoreId(storeId);
        // 插入数据库shanjupay_merchant_service.store_staff表
        storeStaffMapper.insert(storeStaff);
    }



    //======================================================================================
    /**
     * 根据手机号判断员工是否已在指定商户存在
     * @param mobile
     * @param merchantId
     * @return
     */
    Boolean isExistStaffByMobile(String mobile, Long merchantId){
        Integer count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getMobile, mobile)
                .eq(Staff::getMerchantId, merchantId));
        return count > 0;
    }

    /**
     * 根据账号判断员工是否已在指定商户存在
     * @param username
     * @param merchantId
     * @return
     */
    Boolean isExistStaffByUserName(String username, Long merchantId){
        Integer count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, username)
                .eq(Staff::getMerchantId, merchantId));
        return count > 0;
    }

    /**
     * 分页条件查询商户下门店
     * @param storeDTO
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public PageVO<StoreDTO> queryStoreByPage(StoreDTO storeDTO, Integer pageNo, Integer pageSize) {
        // 创建分页
        Page<Store> page = new Page<>(pageNo, pageSize);
        // 构造查询条件
        LambdaQueryWrapper<Store> qw = new LambdaQueryWrapper();
        if(null != storeDTO && null != storeDTO.getMerchantId()) {
            qw.eq(Store::getMerchantId, storeDTO.getMerchantId());
        }
        // 再拼装其他查询条件，比如：门店名称
        if(null != storeDTO && StringUtils.isNotEmpty(storeDTO.getStoreName())) {
            qw.eq(Store::getStoreName, storeDTO.getStoreName());
        }
        // 执行查询
        IPage<Store> storeIPage = storeMapper.selectPage(page, qw);
        // 查询列表
        List<Store> records = storeIPage.getRecords();
        // entity List转DTO List
        List<StoreDTO> storeDTOS = StoreConvert.INSTANCE.listentity2dto(records);

        // 封装结果集
        return new PageVO<>(storeDTOS, storeIPage.getTotal(), pageNo, pageSize);

    }

    /**
     * 校验门店是否属于商户
     * @param storeId
     * @param merchantId
     * @return true存在，false不存在
     */
    @Override
    public Boolean queryStoreInMerchant(Long storeId, Long merchantId) {

        Integer count = storeMapper.selectCount(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, storeId)
                .eq(Store::getMerchantId, merchantId));
        return count > 0;
    }

}
