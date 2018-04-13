package com.eis.upgrade.dao;

import com.eis.common.model.TenantMerchantInfo;

import java.util.List;
import java.util.Map;

public interface TenantMerchantInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TenantMerchantInfo record);

    int insertSelective(TenantMerchantInfo record);

    TenantMerchantInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TenantMerchantInfo record);

    int updateByPrimaryKey(TenantMerchantInfo record);

	List<Map> queryMyMemberCards(String phone, String keyword);

	Map getMyMemberCardDetail(Integer tenantId, String phone);

	List queryHotMerchantsPaging(Map map);

	List queryNearbyMerchantsPaging(Map map);

    TenantMerchantInfo selectByTenantId(Integer tenantId);

	TenantMerchantInfo getMerchantInfoByTenantId(Integer tenantId);

    Integer insertTenantMerchantInfo(TenantMerchantInfo tenantMerchantInfo);
}