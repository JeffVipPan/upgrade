package com.eis.upgrade.service;

import com.eis.common.model.TenantMerchantInfo;
import com.eis.common.model.TenantRegister;
import com.eis.common.model.TenantRegisterLog;
import com.eis.common.model.TenantVerifyLog;

import java.util.List;
import java.util.Map;

/**
 * 
 * @FileName：TenantRegisterService.java
 * @Description：@TODO
 * @Author：thy
 * @CreateTime：上午9:47:21
 */
public interface TenantRegisterService {

	TenantRegister register(TenantRegister tenantRegister) throws Exception;

	boolean active(String phone) throws Exception;

	Map toRegister(Integer id, String templateName);

	String getRandomDoMain();

	String getBatchNo();

	String checkDbIsExist(String db);

	Integer checkTenantIsExist(Map map);

	Integer selectTenantType(Map map);

	Integer insertTenantVerify(Map map);

	List<Integer> getTenantInfoByBatchNo(String batchNo);

	Map tenantVerifyDetail(Map map);

	Map toCheck(String dbVerify, Integer tenantVerify, String upgradeDb, Integer upgradeTenant);

	List<TenantVerifyLog> getTenantVerifyLogByNowAndTenandId(Map map);

	Map toUpgrade(String upgradeDb, String templateDb, Integer upgradeTenant, Integer templateTenant);

	List<String> getNeedDeleteTenant(Map map);

	Integer toDeleteTable(Map map);

	Integer toDeleteSysTable(Map map);

	Integer insertRegisterLog(TenantRegisterLog trl);

	TenantRegister getTenantRegister(Map map);

	TenantMerchantInfo getTenantMerchantInfo(Map map);

	Integer insertTenantRegister(TenantRegister tenantRegister);

	Integer deleteByPrimaryKey(Integer id);

	String toCheckDbIsExist(String dbName);

	List<String> toCheckDbAndTenantIsExist(Map map);

}