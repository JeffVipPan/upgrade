package com.eis.upgrade.service.impl;

import com.eis.common.model.*;
import com.eis.core.dao.*;
import com.eis.core.model.BusinessComponent;
import com.eis.core.service.TenantService;
import com.eis.core.util.PropertiesFileUtil;
import com.eis.core.util.RandomUtil;
import com.eis.core.util.SimpleDateUtil;
import com.eis.core.util.StringUtil;
import com.eis.exception.EISException;
import com.eis.upgrade.dao.IndustryMapper;
import com.eis.upgrade.dao.TenantMerchantInfoMapper;
import com.eis.upgrade.dao.TenantRegisterMapper;
import com.eis.upgrade.service.TenantRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @FileName：TenantRegisterServiceImpl.java
 * @Description：@TODO
 * @Author：thy
 * @CreateTime：上午9:47:46
 */
@Service
public class TenantRegisterServiceImpl implements TenantRegisterService {
	@Autowired
	private TenantRegisterMapper tenantRegisterMapper;

	@Autowired
	private IndustryMapper industryMapper;

	@Autowired
	private IndustryTemplateMapper industryTemplateMapper;

	@Autowired
	private TenantMapper tenantMapper;

	@Autowired
	private TenantMappingMapper tenantMappingMapper;

	@Autowired
	private EmployeeMapper employeeMapper;

	@Autowired
	private TenantMerchantInfoMapper tenantMerchantInfoMapper;

	@Autowired
	private TenantService tenantService;

	@Autowired
	private FieldMapper fieldMapper;

	@Autowired
	private BusinessComponentMapper businessComponentMapper;

	@Override
	public TenantRegister register(TenantRegister tenantRegister)  throws EISException {
		//检查邮箱是否已注册
		TenantRegister old = tenantRegisterMapper.selectByPhone(tenantRegister.getPhone());
		if(old!=null)
		{
			throw new EISException("邮箱已注册");
		}

		//检查行业是否存在
		Industry industry = industryMapper.selectByPrimaryKey(tenantRegister.getIndustry());
		if(industry==null)
		{
			throw new EISException("行业不存在");
		}
		//检查模板是否存在
		IndustryTemplate industryTemplate = industryTemplateMapper.selectByPrimaryKey(tenantRegister.getTemplate());
		if(industryTemplate==null)
		{
			throw new EISException("模板不存在");
		}
		if(industryTemplate.getIndustry().intValue()!=industry.getId().intValue())
		{
			throw new EISException("模板不属于该行业");
		}
		tenantRegister.setActiveCode(UUID.randomUUID().toString());
		tenantRegister.setRegisterTime(new Date());
		tenantRegisterMapper.insertSelective(tenantRegister);
		return tenantRegisterMapper.selectByPrimaryKey(tenantRegister.getId());
	}

	@Override
	public boolean active(String phone)  throws EISException {
		TenantRegister tenantRegister = tenantRegisterMapper.selectByPhone(phone);
		if(tenantRegister==null)
		{
			throw new EISException("激活码错误");
		}
		TenantRegister record = new TenantRegister();
		record.setId(tenantRegister.getId());
		record.setActiveTime(new Date());
		record.setStatus("1");
		tenantRegisterMapper.updateByPrimaryKeySelective(record);
		//自动开通

		return false;
	}

	@Override
	public Map toRegister(Integer id, String templateName) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "fail");
		result.put("retmsg", "注册失败");
		// 查询租户注册关键信息
		Map registerInfo = tenantRegisterMapper.selectTenantRegisterInfo(id);
		if (registerInfo != null) {
			Integer tenantId = (Integer) registerInfo.get("tenant_id");
			Integer tenantType = (Integer) registerInfo.get("tenant_type");
			String companyName = (String) registerInfo.get("company_name");
			String phone = (String) registerInfo.get("phone");
			String pwd = (String) registerInfo.get("pwd");
			String employeeName = (String) registerInfo.get("employee_name");
			Integer template = (Integer) registerInfo.get("template");

			Integer newTenantId = null;
			if (tenantId == 0) {
				result.put("retcode", "fail");
				result.put("retmsg", "原租户不存在");
				throw new EISException("原租户不存在");
			} else {
				// 创建新租户
				Tenant tenant = new Tenant();
				tenant.setIdTemp(tenantId);
				tenant.setTenantId(null);
				tenant.setIndustryName((String) registerInfo.get("name"));
				tenant.setPickTemplate((Integer) registerInfo.get("template"));
				tenant.setCompanyInfo((String) registerInfo.get("company_name"));
				tenant.setDomain((String) registerInfo.get("domain"));
				tenant.setSmsRemain(0);
				tenant.setTenantType(tenantType);
				tenant.setCreateTime(new Date());
				tenant.setIndustryName(templateName);
				int count = tenantMapper.insertSelective(tenant);
				if (count > 0) {
					// 更新租户ID
					newTenantId = tenant.getId();
					tenant.setTenantId(tenant.getId());
					tenantMapper.updateByPrimaryKey(tenant);
				}
			}
			// 判断租户类型 (1=甲, 2=乙, 3=丙)
			if (tenantType == 3) {
				TenantMapping tm = new TenantMapping();
				tm.setmTenantId(tenantId);
				tm.setcTenantId(newTenantId);
				tenantMappingMapper.insertSelective(tm);

			}
			if (tenantType == 1 || tenantType == 2) {
				// 获取到需要复制的系统配置表
				List<String> copyTables = tenantRegisterMapper.selectNeedCopyConfigTableList();
				if (copyTables != null && copyTables.size() > 0) {
					for (String tableName : copyTables) {
						if (!StringUtil.isEmpty(tableName)) {
							StringBuffer sb = new StringBuffer();
							// 插入系统配置数据
							Map map = new HashMap();
							map.put("tableName", tableName.toLowerCase());
							map.put("tenantId", newTenantId);
							// 获取到表里面的字段
							List<String> list = tenantRegisterMapper.selectTableDataByTableName(map);
							for (int i = 0; i < list.size(); i++) {
								sb.append(list.get(i));
								if (i < list.size() - 1) {
									sb.append(",");
								}
							}
							Map param = new HashMap();
							param.put("tableName", "`" + tableName.toLowerCase() + "`");
							param.put("column", sb.toString());
							param.put("tenantId", tenantId);
							tenantRegisterMapper.insertSysConfigData(param);

							TenantRegisterLog trl = new TenantRegisterLog();
							trl.setCreateTime(new Date());
							trl.setTenantId(newTenantId);
							trl.setType("register");
							trl.setRemark("用户" + newTenantId + "注册了" + tableName.toLowerCase() + "表");
							tenantRegisterMapper.insertRegisterLog(trl);
						}
					}
				}
			}

			// 循环创建租户业务表
			List<BusinessComponent> businessTablesList = tenantRegisterMapper.selectBusinessTablesByTenantId(tenantId);
			if (businessTablesList != null && businessTablesList.size() > 0) {
				for (int i = 0; i < businessTablesList.size(); i++) {
					BusinessComponent bc = businessTablesList.get(i);
					if (bc != null) {
						if (!StringUtil.isEmpty(bc.getUuid()) && !StringUtil.isEmpty(bc.getTableName())) {
							// 获取到基本column之外的所有column
							Map map = new HashMap();
							map.put("bcId", bc.getUuid());
							map.put("tenantId", tenantId);
							String anotherColumn = tenantRegisterMapper.getOtherColumns(map);
							anotherColumn = anotherColumn.replace("`id` int(11) NULL","`id` int(11) NOT NULL AUTO_INCREMENT");
							// 创建表
							Map m = new HashMap();
							m.put("tableName", bc.getTableName());
							m.put("tenantId", newTenantId);
							if (!StringUtil.isEmpty(anotherColumn)) {
								m.put("column", anotherColumn);
							}
							tenantRegisterMapper.createBusinessTable(m);

							// 加索引
							IndustryTemplate industryTemplate = industryTemplateMapper.selectByPrimaryKey(template);
							Integer tempTenantId = industryTemplate.getTenantId();

							Map mas = new HashMap();
							mas.put("tenantId", tempTenantId);
							String tableName = "t" + tempTenantId + "_" + bc.getTableName();
							mas.put("tableName", tableName);
							String upgradeDb = PropertiesFileUtil.getPro("config.properties", "jdbc_sid");
							mas.put("dbName", upgradeDb);
							List<TableIndex> indexList = tenantRegisterMapper.getTableIndexInfo(mas);
							if (indexList != null && indexList.size() > 0) {
								for (TableIndex ti : indexList) {
									Map pa = new HashMap();
									//String tName = ti.getTableName();
									String iName = ti.getIndexName();
									//pa.put("tableName", tName.replaceAll("t" + tempTenantId, "t" + newTenantId));
									pa.put("tableName", tableName.replaceAll("t" + tempTenantId, "t" + newTenantId));
									if (ti.getIndexName().contains("t" + tempTenantId)) {
										pa.put("indexName", ti.getIndexName().replaceAll("t" + tempTenantId, "t" + newTenantId));
									} else {
										pa.put("indexName", ti.getIndexName());
									}
									//pa.put("indexName", iName.replaceAll("t" + tempTenantId, "t" + newTenantId));
									pa.put("columnName", ti.getColumnName());
									tenantRegisterMapper.toAddTableIndex(pa);
								}
							}

							TenantRegisterLog trl = new TenantRegisterLog();
							trl.setCreateTime(new Date());
							trl.setTenantId(newTenantId);
							trl.setType("register");
							trl.setRemark("用户" + newTenantId + "注册了" + bc.getTableName().toLowerCase() + "表");
							tenantRegisterMapper.insertRegisterLog(trl);
						}
					}
				}
			}

			// 创建定制表 : 获取到需要复制数据的定制表
			List<String> needCopyDataList = tenantRegisterMapper.getNeedCopyTableData();
			if (needCopyDataList != null && needCopyDataList.size() > 0) {
				for (int i = 0; i < needCopyDataList.size(); i++) {
					// 获取到每个表名
					String tableName = needCopyDataList.get(i);
					// 插入数据
					Map map = new HashMap();
					map.put("oldTenantId", tenantId);
					map.put("newTenantId", newTenantId);
					map.put("tableName", tableName);
					tenantRegisterMapper.insertNeedCopyData(map);
					// 更新数据
					tenantRegisterMapper.updateNeedCopyData(map);

					TenantRegisterLog trl = new TenantRegisterLog();
					trl.setCreateTime(new Date());
					trl.setTenantId(newTenantId);
					trl.setType("register");
					trl.setRemark("用户" + newTenantId + "注册了" + tableName.toLowerCase() + "表");
					tenantRegisterMapper.insertRegisterLog(trl);

				}
			}

			// 生成employee数据
			Date now = new Date();
			Employee emp = new Employee();
			emp.setTenantId(newTenantId);
			emp.setCreateTime(now);
			emp.setOwner(1);
			emp.setPosition(1);
			emp.setOrganization(1);
			if (!StringUtil.isEmpty(employeeName)) {
				emp.setName(employeeName);
			} else {
				emp.setName("");
			}

			emp.setJobTitle("管理员");
			emp.setLoginName(phone);
			if (!StringUtil.isEmpty(pwd)) {
				emp.setLoginPwd(pwd);
			} else {
				emp.setLoginPwd("");
			}
			emp.setPhone(phone);
			emp.setEmail(null);
			emp.setResponsibilityId(null);
			emp.setRoleType("admin");
			employeeMapper.insertSelective(emp);

			// 生成插入tenant_merchant_info数据
			TenantMerchantInfo tmi = new TenantMerchantInfo();
			tmi.setTenantId(newTenantId);
			if (!StringUtil.isEmpty(employeeName)) {
				tmi.setMerchantName(companyName);
			} else {
				tmi.setMerchantName("");
			}
			tmi.setContactPhone(phone);
			tmi.setIndustry(templateName);
			tenantMerchantInfoMapper.insertSelective(tmi);

			// 更新注册结果
			TenantRegister tr = new TenantRegister();
			tr.setActiveTime(now);
			tr.setTenantId(newTenantId);
			tr.setStatus("1");
			tr.setId(id);
			tenantRegisterMapper.updateByPrimaryKeySelective(tr);

			// 删除拷贝多余的数据
			Map m = new HashMap();
			m.put("tenantId", newTenantId);
			String resUuid = tenantRegisterMapper.getResponsibilityId(m);
			m.put("empId", emp.getId());
			tenantRegisterMapper.deleteEmpResLink(m);
			List<String> linkTables = tenantRegisterMapper.getNeedDeleteLinkTables(m);
			if (linkTables != null && linkTables.size() > 0) {
				for (String tableName : linkTables) {
					if (!StringUtil.isEmpty(tableName)) {
						Map map = new HashMap();
						map.put("resId", resUuid);
						map.put("tableName", tableName);
						tenantRegisterMapper.deleteResLink(map);
					}
				}
			}


			result.put("retcode", "ok");
			result.put("retmsg", "注册成功");

		}
		return result;
	}

	@Override
	public String getRandomDoMain() throws EISException {
		String domain = "";
		String temp = RandomUtil.getStringRandom(8);
		// 根据生成的domain去查询，保证domain唯一
		List<TenantRegister> tenantRegisterList = tenantRegisterMapper.selectByDomain(temp);
		if (tenantRegisterList != null && tenantRegisterList.size() > 0) {
			// 如果存在，需要重新生成
			getRandomDoMain();
		} else {
			domain = temp;
		}
		return domain;
	}

	@Override
	public String getBatchNo() throws EISException {
		return tenantRegisterMapper.getBatchNo();
	}

	@Override
	public String checkDbIsExist(String db) throws EISException {
		return tenantRegisterMapper.checkDbIsExist(db);
	}

	@Override
	public Integer checkTenantIsExist(Map map) throws EISException {
		return tenantRegisterMapper.checkTenantIsExist(map);
	}

	@Override
	public Integer selectTenantType(Map map) throws EISException {
		return tenantRegisterMapper.selectTenantType(map);
	}

	@Override
	public Integer insertTenantVerify(Map map) throws EISException {
		return tenantRegisterMapper.insertTenantVerify(map);
	}

	@Override
	public List<Integer> getTenantInfoByBatchNo(String batchNo) throws EISException {
		return tenantRegisterMapper.getTenantInfoByBatchNo(batchNo);
	}

	@Override
	public Map tenantVerifyDetail(Map map) throws EISException {
		Map result = new HashMap();
		Integer tenantTablesId = null;
		Integer verifyLogByBcConfigId = null;
		Integer verifyLogByTableId = null;
		List<Integer> logConfigList = new ArrayList<>();
		List<Integer> logColumnList = new ArrayList<>();
		try {
			// 校验租户的表
			tenantTablesId = tenantRegisterMapper.insertVerifyTenantTables(map);
			// 无BC配置数据
			verifyLogByBcConfigId = tenantRegisterMapper.insertVerifyLogByBcConfig(map);
			// 缺租户表
			verifyLogByTableId = tenantRegisterMapper.insertVerifyLogByTable(map);
			// BC配置表
			Map param = new HashMap();
			param.put("batchNo", map.get("batchNo"));
			param.put("tenantId", map.get("tenantId"));
			param.put("db", map.get("db"));
			List<TenantVerifyTenantTables> list = tenantRegisterMapper.getBcConfig(param);
			if (list != null && list.size() > 0) {
				for (TenantVerifyTenantTables tvtt : list) {
					if (!StringUtil.isEmpty(tvtt.getBcUuid()) && !StringUtil.isEmpty(tvtt.getTableName())) {
						map.put("uuid", tvtt.getBcUuid());
						map.put("tableName", tvtt.getTableName());
						Integer logConfigId = tenantRegisterMapper.insertVerifyLogByColumnConfig(map);
						logConfigList.add(logConfigId);
						Integer logColumnId = tenantRegisterMapper.insertVerifyLogByColumn(map);
						logColumnList.add(logColumnId);
					}
				}
			}
			result.put("retcode", "ok");
			result.put("retmsg", "校验租户配置成功");
		} catch (Exception e) {
			// 回滚
			if (tenantTablesId != null ) {
				tenantRegisterMapper.deleteVerifyTenantTablesById(tenantTablesId);
			}
			if (verifyLogByBcConfigId != null) {
				tenantRegisterMapper.deleteVerifyLogById(verifyLogByBcConfigId);
			}
			if (verifyLogByTableId != null) {
				tenantRegisterMapper.deleteVerifyLogById(verifyLogByTableId);
			}
			StringBuffer sbConfig = new StringBuffer();
			if (logConfigList != null && logConfigList.size() > 0) {
				for (int i = 0; i < logConfigList.size(); i++) {
					sbConfig.append("'");
					sbConfig.append(logConfigList.get(i));
					sbConfig.append("'");
					if (i < logConfigList.size() - 1) {
						sbConfig.append(",");
					}
				}
				tenantRegisterMapper.deleteVerifyLogByIds(sbConfig.toString());

			}
			StringBuffer sbColumn = new StringBuffer();
			if (logColumnList != null && logColumnList.size() > 0) {
				for (int i = 0; i < logColumnList.size(); i++) {
					sbColumn.append("'");
					sbColumn.append(logColumnList.get(i));
					sbColumn.append("'");
					if (i < logColumnList.size() - 1) {
						sbColumn.append(",");
					}
				}
				tenantRegisterMapper.deleteVerifyLogByIds(sbColumn.toString());
			}
			result.put("retcode", "fail");
			result.put("retmsg", "校验租户配置失败");
			throw new EISException("校验租户配置失败");
		}
		return result;
	}

	@Override
	public Map toCheck(String dbVerify, Integer tenantVerify, String upgradeDb, Integer upgradeTenant) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "校验租户信息成功");
		// 生成批次号
		String batchNo = tenantRegisterMapper.getBatchNo();
		Map param = new HashMap();
		if (!StringUtil.isEmpty(dbVerify)) {
			// 验证库表是否存在
			String isExist = tenantRegisterMapper.checkDbIsExist(dbVerify);
			if (StringUtil.isEmpty(isExist)) {
				result.put("retcode", "fail");
				result.put("retmsg", "库表不存在");
				throw new EISException("库表不存在");
			}
			// 检查校验租户是否存在
			param.put("db", dbVerify);
			param.put("tenantId", tenantVerify);
			Integer count = tenantRegisterMapper.checkTenantIsExist(param);
			if (count == 0) {
				result.put("retcode", "fail");
				result.put("retmsg", "校验租户不存在");
				throw new EISException("校验租户不存在");
			}
			// 查询校验租户类型
			Integer tenantType = tenantRegisterMapper.selectTenantType(param);
			if (tenantType != 1 && tenantType != 2) {
				result.put("retcode", "fail");
				result.put("retmsg", "校验租户类型必须为甲乙类");
				throw new EISException("校验租户类型必须为甲乙类");
			}
		}


		// 校验系统表里有无重复数据
		List<String> copyTables = tenantRegisterMapper.selectNeedCopyConfigTableList();
		if (copyTables != null && copyTables.size() > 0) {
			for (String tableName : copyTables) {
				Map m = new HashMap();
				m.put("tableName", tableName);
				if (!StringUtil.isEmpty(upgradeDb)) {
					m.put("tableName", tableName);
					m.put("dbName", upgradeDb);
					m.put("tenantId", upgradeTenant);
					Integer upgradeCount = tenantRegisterMapper.getRepeatDataBySysTable(m);
					if (upgradeCount != null && upgradeCount > 1) {
						// 插入错误日志
						TenantVerifyLog tvl = new TenantVerifyLog();
						tvl.setBatchNo(batchNo);
						tvl.setDbVerify(upgradeDb);
						tvl.setCreateTime(new Date());
						tvl.setOpType("system data repeat");
						tvl.setTenantId(upgradeTenant);
						//tvl.setmTenantId(tenantService.getConfigTenantId(upgradeTenant));
						tvl.setmTenantId(upgradeTenant);
						tvl.setTableName(tableName);
						tvl.setRemark("系统表中有多条应该唯一的数据");
						tenantRegisterMapper.insertSelectiveTenantVerifyLog(tvl);
					}
				}
				if (!StringUtil.isEmpty(dbVerify)) {
					m.put("dbName", dbVerify);
					m.put("tenantId", tenantVerify);
					Integer templateCount = tenantRegisterMapper.getRepeatDataBySysTable(m);
					if (templateCount != null &&templateCount > 1) {
						// 插入错误日志
						TenantVerifyLog tvl = new TenantVerifyLog();
						tvl.setBatchNo(batchNo);
						tvl.setDbVerify(dbVerify);
						tvl.setCreateTime(new Date());
						tvl.setOpType("system data repeat");
						tvl.setTenantId(tenantVerify);
						//tvl.setmTenantId(tenantService.getConfigTenantId(tenantVerify));
						tvl.setmTenantId(tenantVerify);
						tvl.setTableName(tableName);
						tvl.setRemark("系统表中有多条应该唯一的数据");
						tenantRegisterMapper.insertSelectiveTenantVerifyLog(tvl);
					}
				}
			}
		}

		if (!StringUtil.isEmpty(dbVerify)) {
			// 校验租户及关联的子租户（丙类）
			param.put("batchNo", batchNo);
			if (!StringUtil.isEmpty(dbVerify)) {
				param.put("db", dbVerify);
				param.put("tenantId", tenantVerify);
			}

			Integer tenantVerifyId = tenantRegisterMapper.insertTenantVerify(param);
			// 根据batchNo获取到tenantId
			List<Integer> tenantIdList = tenantRegisterMapper.getTenantInfoByBatchNo(batchNo);
			if (tenantIdList != null && tenantIdList.size() > 0) {
				for (Integer tenantId : tenantIdList) {
					if (tenantId != null) {
						Map map = new HashMap();
						map.put("batchNo", batchNo);
						map.put("tenantId", tenantId);
						if (!StringUtil.isEmpty(dbVerify)) {
							map.put("db", dbVerify);
							map.put("tenantVerify", tenantVerify);
							// 校验租户配置
							Map res = tenantVerifyDetail(map);
							if (res.get("retcode").equals("fail")) {
								// 回滚
								tenantRegisterMapper.deleteTenantVerifyById(tenantVerifyId);
								result.put("retcode", "fail");
								result.put("retmsg", "模版库校验租户配置失败");
								throw new EISException("模版库校验租户配置失败");
							}
						}

					}
				}
			}
		}

		/*if (!StringUtil.isEmpty(upgradeDb)) {
			if (!StringUtil.isEmpty(upgradeDb)) {
				param.put("db", upgradeDb);
				param.put("tenantId", upgradeTenant);
			}
			Integer upgradeId = tenantRegisterMapper.insertTenantVerify(param);
			// 根据batchNo获取到tenantId
			List<Integer> upgradeTenantIdList = tenantRegisterMapper.getTenantInfoByBatchNo(batchNo);
			if (upgradeTenantIdList != null && upgradeTenantIdList.size() > 0) {
				for (Integer tenantId : upgradeTenantIdList) {
					if (tenantId != null) {
						if (tenantId.equals(upgradeTenant)) {
							Map map = new HashMap();
							map.put("batchNo", batchNo);
							map.put("tenantId", tenantId);
							if (!StringUtil.isEmpty(upgradeDb)) {
								map.put("db", upgradeDb);
								map.put("tenantVerify", upgradeTenant);
								Map ress = tenantVerifyDetail(map);
								if (ress.get("retcode").equals("fail")) {
									// 回滚
									tenantRegisterMapper.deleteTenantVerifyById(upgradeId);
									result.put("retcode", "fail");
									result.put("retmsg", "升级库校验租户配置失败");
									throw new EISException("升级库校验租户配置失败");
								}
							}
						}
					}
				}
			}
		}*/

		return result;
	}

	@Override
	public List<TenantVerifyLog> getTenantVerifyLogByNowAndTenandId(Map map) throws EISException {
		return tenantRegisterMapper.getTenantVerifyLogByNowAndTenandId(map);
	}

	@Override
	public Map toUpgrade(String upgradeDb, String templateDb, Integer upgradeTenant, Integer templateTenant) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "租户升级成功");

		// 生成批次号
		String batchNo = tenantRegisterMapper.getBatchNo();
		// 备份库名
		String backDbName = "backup" + batchNo;
		// 插入更新日志
		Date now = new Date();
		TenantUpgradeLog tul = new TenantUpgradeLog();
		tul.setCreateTime(now);
		tul.setBatchNo(batchNo);
		tul.setDbBackup(backDbName);
		tul.setDbUpgrade(upgradeDb);
		tul.setIsSuccess(0);
		tul.setDbTemplate(templateDb);
		tul.setTenantUpgrade(upgradeTenant);
		tul.setTenantTemplate(templateTenant);
		tul.setResult("处理中");

		Integer logId = tenantRegisterMapper.insertTenantUpgradeLog(tul);

		// 检查升级租户是否存在
		Map isExist = new HashMap();
		isExist.put("db", upgradeDb);
		isExist.put("tenantId", upgradeTenant);
		Integer countIsExist = tenantRegisterMapper.checkTenantIsExist(isExist);
		if (countIsExist == 0) {
			// 回滚日志
			tenantRegisterMapper.deleteTenantUpgradeLogById(tul.getId());
			result.put("retcode", "fail");
			result.put("retmsg", "升级租户不存在");
			throw new EISException("升级租户不存在");
		}
		// 检查模板租户是否存在
		Map templateIsExist = new HashMap();
		templateIsExist.put("db", templateDb);
		templateIsExist.put("tenantId", templateTenant);
		Integer countTemplateIsExist = tenantRegisterMapper.checkTenantIsExist(templateIsExist);
		if (countTemplateIsExist == 0) {
			// 回滚日志
			tenantRegisterMapper.deleteTenantUpgradeLogById(tul.getId());
			result.put("retcode", "fail");
			result.put("retmsg", "模板租户不存在");
			throw new EISException("模板租户不存在");
		}
		// 查询升级租户类型
		Map upgradeType = new HashMap();
		upgradeType.put("db", upgradeDb);
		upgradeType.put("tenantId", upgradeTenant);
		Integer upgradeTenantType = tenantRegisterMapper.selectTenantType(upgradeType);
		if (upgradeTenantType != 1 && upgradeTenantType != 2) {
			// 回滚日志
			tenantRegisterMapper.deleteTenantUpgradeLogById(tul.getId());
			result.put("retcode", "fail");
			result.put("retmsg", "升级租户类型必须为甲乙类");
			throw new EISException("升级租户类型必须为甲乙类");
		}
		// 查询模板租户类型
		Map templateType = new HashMap();
		templateType.put("db", upgradeDb);
		templateType.put("tenantId", upgradeTenant);
		Integer templateTenantType = tenantRegisterMapper.selectTenantType(templateType);
		if (templateTenantType != 1 && templateTenantType != 2) {
			// 回滚日志
			tenantRegisterMapper.deleteTenantUpgradeLogById(tul.getId());
			result.put("retcode", "fail");
			result.put("retmsg", "模板租户类型必须为甲乙类");
			throw new EISException("模板租户类型必须为甲乙类");
		}
		// 配置租户及关联的子租户（丙类）
		TenantUpgradeTenants tut = new TenantUpgradeTenants();
		tut.setBatchNo(batchNo);
		tut.setTenantId(upgradeTenant);
		tut.setDbUpgrade(upgradeDb);
		Integer tenantUpgradeTenantsId = tenantRegisterMapper.insertTenantUpgradeTenants(tut);

		// 备份配置数据
		/*Map backupConfigMap = upgradeBackupConfig(upgradeDb, backDbName, upgradeTenant);
		if (backupConfigMap.get("retcode").equals("fail")) {
			// 回滚
			tenantRegisterMapper.deleteTenantUpgradeTenantsById(tut.getId());
			result.put("retcode", "fail");
			result.put("retmsg", "备份配置失败");
			return result;
		}*/
		// 备份租户表及数据
		List<Integer> backupTenantList = tenantRegisterMapper.selectTenantUpgradeTenantsByBatchNo(batchNo);
		/*if (backupTenantList != null && backupTenantList.size() > 0) {
			for (Integer tenantId : backupTenantList) {
				if (tenantId != null) {
					Map map = upgradeBackupTable(upgradeDb, backDbName, tenantId);
					if (map.get("retcode").equals("fail")) {
						// 回滚
						tenantRegisterMapper.deleteTenantUpgradeTenantsById(tut.getId());
						result.put("retcode", "fail");
						result.put("retmsg", "备份租户表数据失败");
						return result;
					}
				}
			}

		}*/

		// 更新租户表结构
		if (backupTenantList != null && backupTenantList.size() > 0) {
			/*for (Integer tenantId : backupTenantList) {
				if (tenantId != null) {*/
					Map map = upgradeModifyTable(batchNo, upgradeDb, templateDb, null, upgradeTenant, templateTenant);
					if (map.get("retcode").equals("fail")) {
						Map mas = new HashMap();
						mas.put("backDbName", backDbName);
						mas.put("tenantPrefix", "t" + upgradeTenant + "_%");
						// 获取到要升级的租户表
						List<String> list = tenantRegisterMapper.selectExistsTableByBackup(mas);
						if (list != null && list.size() > 0) {
							for (String tableName : list) {
								// 删除升级库下面的表
								mas.put("backDbName", upgradeDb);
								mas.put("tableName", tableName);
								tenantRegisterMapper.deleteTenantTableByUpgradeDbAndTenantId(mas);
								// 把表从备份库里复制到升级库
								mas.put("backDbName", backDbName);
								tenantRegisterMapper.createTenantTableByBackupDb(mas);

							}
						}
						// 回滚
						tenantRegisterMapper.deleteTenantUpgradeTenantsById(tut.getId());
						result.put("retcode", "fail");
						result.put("retmsg", "更新租户表结构失败");
						throw new EISException("更新租户表结构失败");
					}
				/*}
			}*/

		}

		// 生产环境不需要比对的数据
		Map ignoreMap = ignoreUpdateConfig(upgradeDb, upgradeTenant);
		// 模版库不需要比对的数据
		Map templateIgnoreMap = ignoreUpdateConfig(templateDb, templateTenant);

		// 更新租户配置数据
		Map res = upgradeUpdateConfig(batchNo, upgradeDb, templateDb, upgradeTenant, templateTenant, ignoreMap, templateIgnoreMap);
		//Map res = upgradeUpdateConfig(batchNo, upgradeDb, templateDb, upgradeTenant, templateTenant, new HashMap(), new HashMap());
		if (res.get("retcode").equals("fail")) {
			Map mas = new HashMap();
			mas.put("backDbName", backDbName);
			mas.put("tenantPrefix", "t" + upgradeTenant + "_%");
			// 获取到要升级的租户表
			List<String> list = tenantRegisterMapper.selectExistsTableByBackup(mas);
			if (list != null && list.size() > 0) {
				for (String tableName : list) {
					// 删除升级库下面的表
					mas.put("backDbName", upgradeDb);
					mas.put("tableName", tableName);
					tenantRegisterMapper.deleteTenantTableByUpgradeDbAndTenantId(mas);
					// 把表从备份库里复制到升级库
					mas.put("backDbName", backDbName);
					tenantRegisterMapper.createTenantTableByBackupDb(mas);

				}
			}
			result.put("retcode", "fail");
			result.put("retmsg", "更新配置数据失败");
			throw new EISException("更新配置数据失败");
		}

		// 唯一性约束校验 (field表)
		/*Map uniqueMap = new HashMap();
		uniqueMap.put("db", templateDb);
		List<Field> list = fieldMapper.getIsUniqueData(uniqueMap);
		if (list != null && list.size() > 0) {
			for (Field f : list) {
				if (f != null) {
					// 取到BC
					BusinessComponent bc = businessComponentMapper.selectByUuidAndDb(f.getTenantId(), f.getBc(), templateDb);
					// 获取到表名
					String tableName = "t" + upgradeTenant + "_" + bc.getTableName();
					// 根据columnName查询是否有唯一性约束冲突
					List<Integer> countList = tenantRegisterMapper.getIsUniqueInfo(upgradeTenant, tableName, f.getColumnName(), upgradeDb);
					if (countList != null && countList.size() > 0) {
						for (Integer counts : countList) {
							if (counts != null && counts > 1) {
								// 存在唯一性约束冲突
								result.put("retcode", "fail");
								result.put("retmsg", "租户" + upgradeTenant + "的" + tableName + "表中的列" + f.getColumnName() + "存在唯一性约束冲突");
								return result;
							}
						}
					}
				}
			}
		}*/


		// 更新处理状态
		Map upd = new HashMap();
		upd.put("batchNo", batchNo);
		tenantRegisterMapper.updateTenantUpgradeLogStatus(upd);

		// 更新索引
		Map mas = new HashMap();
		mas.put("dbName", templateDb);
		mas.put("tenantId", templateTenant);
		List<String> tableList = tenantRegisterMapper.toCheckDbAndTenantIsExist(mas);
		if (tableList != null && tableList.size() > 0) {
			for (String tableName : tableList) {
				if (!StringUtil.isEmpty(tableName)) {
					mas.put("tableName", tableName);
					mas.put("upgradeDb", upgradeDb);
					List<TableIndex> indexList = tenantRegisterMapper.getTableIndexInfo(mas);
					if (indexList != null && indexList.size() > 0) {
						for (TableIndex ti : indexList) {
							Map p = new HashMap();
							p.put("tableName", ti.getTableName().replaceAll("t" + templateTenant, "t" + upgradeTenant));
							if (ti.getIndexName().contains("t" + templateTenant)) {
								p.put("indexName", ti.getIndexName().replaceAll("t" + templateTenant, "t" + upgradeTenant));
							} else {
								p.put("indexName", ti.getIndexName());
							}
							if (ti.getColumnName().contains(",")) {
								String[] arr = ti.getColumnName().split(",");
								StringBuffer s = new StringBuffer();
								for (int i = 0; i < arr.length; i++) {
									s.append("column_name = '" + arr[i] + "'");
									if (i < arr.length - 1) {
										s.append(" or ");
									}
								}

								p.put("columnName", s.toString());
								p.put("flag", "1");
							} else {
								p.put("columnName", ti.getColumnName());
								p.put("flag", "0");
							}
							p.put("dbName", upgradeDb);
							Integer count = tenantRegisterMapper.toCheckIndexNameIsExist(p);
							if (count == 0) {
								Map pa = new HashMap();
								String tName = ti.getTableName();
								String iName = ti.getIndexName();
								pa.put("tableName", tName.replaceAll("t" + templateTenant, "t" + upgradeTenant));
								if (ti.getIndexName().contains("t" + templateTenant)) {
									p.put("indexName", ti.getIndexName().replaceAll("t" + templateTenant, "t" + upgradeTenant));
								} else {
									p.put("indexName", ti.getIndexName());
								}
								pa.put("columnName", ti.getColumnName());
								pa.put("dbName", upgradeDb);
								tenantRegisterMapper.toAddTableIndex(pa);
							}
						}
					}
				}
			}
		}



		return result;
	}

	@Override
	public List<String> getNeedDeleteTenant(Map map) throws EISException {
		return tenantRegisterMapper.getNeedDeleteTenant(map);
	}

	@Override
	public Integer toDeleteTable(Map map) throws EISException {
		return tenantRegisterMapper.toDeleteTable(map);
	}

	@Override
	public Integer toDeleteSysTable(Map map) throws EISException {
		return tenantRegisterMapper.toDeleteSysTable(map);
	}

	@Override
	public Integer insertRegisterLog(TenantRegisterLog trl) throws EISException {
		return tenantRegisterMapper.insertRegisterLog(trl);
	}

	@Override
	public TenantRegister getTenantRegister(Map map) throws EISException {
		return tenantRegisterMapper.getTenantRegister(map);
	}

	@Override
	public TenantMerchantInfo getTenantMerchantInfo(Map map) throws EISException {
		return tenantRegisterMapper.getTenantMerchantInfo(map);
	}

	@Override
	public Integer insertTenantRegister(TenantRegister tenantRegister) throws EISException {
		return tenantRegisterMapper.insertTenantRegister(tenantRegister);
	}

	@Override
	public Integer deleteByPrimaryKey(Integer id) throws EISException {
		return tenantRegisterMapper.deleteByPrimaryKey(id);
	}

	@Override
	public String toCheckDbIsExist(String dbName) throws EISException {
		return tenantRegisterMapper.toCheckDbIsExist(dbName);
	}

	@Override
	public List<String> toCheckDbAndTenantIsExist(Map map) throws EISException {
		return tenantRegisterMapper.toCheckDbAndTenantIsExist(map);
	}


	public Map upgradeBackupConfig(String upgradeDb, String backDbName, Integer upgradeTenant) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "备份成功");

		// 检查租户是否存在
		Map isExist = new HashMap();
		isExist.put("db", upgradeDb);
		isExist.put("tenantId", upgradeTenant);
		Integer countIsExist = tenantRegisterMapper.checkTenantIsExist(isExist);
		if (countIsExist == 0) {
			result.put("retcode", "fail");
			result.put("retmsg", "租户不存在");
			throw new EISException("租户不存在");
		}

		// 备份库若不存在创建
		try {
			Map param = new HashMap();
			param.put("backDbName", backDbName);
			String database = tenantRegisterMapper.selectDatabase(param);
			if (StringUtil.isEmpty(database)) {
				tenantRegisterMapper.createBackupDatabase(param);
			}
		} catch (Exception e) {
			result.put("retcode", "fail");
			result.put("retmsg", "备份库创建失败");
			throw new EISException("备份库创建失败");
		}

		// 备份tenant
		Map backupTenantMap = upgradeBackupConfigTable(upgradeDb, backDbName, upgradeTenant, "tenant");
		if (backupTenantMap.get("retcode").equals("fail")) {
			// 配置数据备份失败则回滚 ： 清空库
			tenantRegisterMapper.deleteBackupDatabase(backDbName);
			result.put("retcode", "fail");
			result.put("retmsg", "备份租户表失败");
			throw new EISException("备份租户表失败");
		}

		// 备份配置数据
		List<String> backupOtherTableList = tenantRegisterMapper.selectTableNameFromTenantUpgradeConfigTable();
		if (backupOtherTableList != null && backupOtherTableList.size() > 0) {
			for (String tableName : backupOtherTableList) {
				if (!StringUtil.isEmpty(tableName)) {
					Map backupOtherTableMap = upgradeBackupConfigTable(upgradeDb, backDbName, upgradeTenant, tableName);
					if (backupOtherTableMap.get("retcode").equals("fail")) {
						// 配置数据备份失败则回滚 ： 清空库
						tenantRegisterMapper.deleteBackupDatabase(backDbName);
						result.put("retcode", "fail");
						result.put("retmsg", "备份配置数据失败");
						throw new EISException("备份配置数据失败");
					}
				}
			}
		}

		return result;
	}

	public Map upgradeBackupConfigTable(String upgradeDb, String backDbName, Integer upgradeTenant, String tableName) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "备份租户表成功");

		Map map = new HashMap();
		map.put("upgradeDb", upgradeDb);
		map.put("backDbName", backDbName);
		map.put("upgradeTenant", upgradeTenant);
		map.put("tableName", tableName);
		try {
			// 若不存在创建配置表
			tenantRegisterMapper.createBackupTenant(map);
			// 删除已存在的配置数据
			tenantRegisterMapper.deleteBackupTenantData(map);
			// 复制配置数据
			tenantRegisterMapper.insertBackupTenantData(map);
		} catch (Exception e) {
			result.put("retcode", "fail");
			result.put("retmsg", "备份租户表系统异常");
			throw new EISException("备份租户表系统异常");
		}

		return result;
	}

	public Map upgradeBackupTable(String upgradeDb, String backDbName, Integer tenantId) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "备份租户表成功");

		// 若不存在创建备份库
		try {
			Map param = new HashMap();
			param.put("backDbName", backDbName);
			String database = tenantRegisterMapper.selectDatabase(param);
			if (StringUtil.isEmpty(database)) {
				tenantRegisterMapper.createBackupDatabase(param);
			}
		} catch (Exception e) {
			result.put("retcode", "fail");
			result.put("retmsg", "备份库创建失败");
			throw new EISException("备份库创建失败");
		}
		// 前缀
		String tenantPrefix = "t" + tenantId + "_%";
		// 获取存在的表
		Map map = new HashMap();
		map.put("tenantPrefix", tenantPrefix);
		map.put("backDbName", backDbName);
		List<String> list = tenantRegisterMapper.selectExistsTableByBackup(map);
		if (list != null && list.size() > 0) {
			for (String tableName : list) {
				if (!StringUtil.isEmpty(tableName)) {
					Map param = new HashMap();
					param.put("backDbName", backDbName);
					param.put("tableName", tableName);
					// 删除已存在的租户表
					tenantRegisterMapper.deleteExistsTableByBackDbNameAndTablename(param);
				}
			}
		}

		Map maps = new HashMap();
		maps.put("tenantPrefix", tenantPrefix);
		maps.put("backDbName", upgradeDb);
		// 需要备份的租户表
		List<String> backupList = tenantRegisterMapper.selectExistsTableByBackup(maps);
		if (backupList != null && backupList.size() > 0) {
			for (String tableName : backupList) {
				if (!StringUtil.isEmpty(tableName)) {
					try {
						Map param = new HashMap();
						param.put("backDbName", backDbName);
						param.put("tableName", tableName);
						param.put("upgradeDb", upgradeDb);
						// 复制租户表
						tenantRegisterMapper.createBackupTableFromUpgradeTable(param);
						// 复制表数据
						tenantRegisterMapper.insertBackupTableFromUpgradeTable(param);
					} catch (Exception e) {
						tenantRegisterMapper.deleteBackupDatabase(backDbName);
						result.put("retcode", "fail");
						result.put("retmsg", "租户表数据备份失败");
						throw new EISException("租户表数据备份失败");
					}
				}
			}
		}

		return result;
	}

	public Map upgradeModifyTable(String batchNo, String upgradeDb, String templateDb, Integer tenantId, Integer upgradeTenant, Integer templateTenant)  throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "租户表结构更新成功");

		tenantId = upgradeTenant;
		// 将需要更新的SQL插入到日志表
		Map map = new HashMap();
		map.put("batchNo", batchNo);
		map.put("upgradeDb", upgradeDb);
		map.put("templateDb", templateDb);
		map.put("tenantId", tenantId);
		map.put("upgradeTenant", upgradeTenant);
		map.put("templateTenant", templateTenant);

		Integer createTableId = null;
		Integer dropTableId = null;
		Integer renameTableId = null;
		Integer dropColumnId = null;
		Integer changeColumnId = null;
		Integer addColumnId = null;

		try {
			// create table
			createTableId = tenantRegisterMapper.insertTenantUpgradeTableLogByCreateTable(map);
			// drop table
			dropTableId = tenantRegisterMapper.insertTenantUpgradeTableLogByDropTable(map);
			// rename table
			renameTableId = tenantRegisterMapper.insertTenantUpgradeTableLogByRenameTable(map);
			// drop column
			dropColumnId = tenantRegisterMapper.insertTenantUpgradeTableLogByDropColumn(map);
			// change column
			changeColumnId = tenantRegisterMapper.insertTenantUpgradeTableLogByChangeColumn(map);
			// add column
			addColumnId = tenantRegisterMapper.insertTenantUpgradeTableLogByAddColumn(map);

			// 获取到插入的数据
			Map param = new HashMap();
			param.put("batchNo", batchNo);
			param.put("tenantId", tenantId);
			param.put("opType", "create table");
			List<String> createTableSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (createTableSql != null && createTableSql.size() > 0) {
				for (String sql : createTableSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

			param.put("opType", "drop table");
			List<String> dropTableSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (dropTableSql != null && dropTableSql.size() > 0) {
				for (String sql : dropTableSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

			param.put("opType", "rename table");
			List<String> renameTableSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (renameTableSql != null && renameTableSql.size() > 0) {
				for (String sql : renameTableSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

			param.put("opType", "drop column");
			List<String> dropColumnSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (dropColumnSql != null && dropColumnSql.size() > 0) {
				for (String sql : dropColumnSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

			param.put("opType", "change column");
			List<String> changeColumnSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (changeColumnSql != null && changeColumnSql.size() > 0) {
				for (String sql : changeColumnSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

			param.put("opType", "add column");
			List<String> addColumnSql = tenantRegisterMapper.selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(param);
			if (addColumnSql != null && addColumnSql.size() > 0) {
				for (String sql : addColumnSql) {
					tenantRegisterMapper.toExecSql(sql);
				}
			}

		} catch (Exception e) {
			Map err = new HashMap();
			err.put("tenantId", tenantId);
			err.put("batchNo", batchNo);
			if (createTableId != null) {
				err.put("opType", "create table");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			if (dropTableId != null) {
				err.put("opType", "drop table");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			if (renameTableId != null) {
				err.put("opType", "rename table");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			if (dropColumnId != null) {
				err.put("opType", "drop column");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			if (changeColumnId != null) {
				err.put("opType", "change column");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			if (addColumnId != null) {
				err.put("opType", "add column");
				tenantRegisterMapper.deleteTenantUpgradeTableByBatchNoAndTenantId(err);
			}
			result.put("retcode", "fail");
			result.put("retmsg", "租户表结构更新失败");
			throw new EISException("租户表结构更新失败");
		}

		return result;
	}

	public Map upgradeUpdateConfig(String batchNo, String upgradeDb, String templateDb, Integer upgradeTenant, Integer templateTenant, Map ignoreMap, Map templateIgnoreMap) throws EISException {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "更新配置数据成功");

		Map map = new HashMap();
		map.put("batchNo", batchNo);
		map.put("upgradeDb", upgradeDb);
		map.put("templateDb", templateDb);
		map.put("upgradeTenant", upgradeTenant);
		map.put("templateTenant", templateTenant);

		List<String> list = tenantRegisterMapper.selectTableNameFromTenantUpgradeConfigTable();
		Date now = new Date();
		try {
			if (list != null && list.size() > 0) {
				for (String tableName : list) {
					if (!StringUtil.isEmpty(tableName)) {
						map.put("tableName", tableName);
						map.put("opType", "delete");
						/*// 动态客户群--生产环境
						if (tableName.equals("applet") || tableName.equals("list_column") || tableName.equals("list_column_group_display_rule")
								|| tableName.equals("list_column_group_rule") || tableName.equals("report") || tableName.equals("applet_button")
								|| tableName.equals("pick_item")) {
							map.put("ids", ignoreMap.get(tableName));
						}
						// 动态客户群--本地模板
						if (tableName.equals("applet") || tableName.equals("list_column") || tableName.equals("list_column_group_display_rule")
								|| tableName.equals("list_column_group_rule") || tableName.equals("report") || tableName.equals("applet_button")
								|| tableName.equals("pick_item")) {
							map.put("templateIds", templateIgnoreMap.get(tableName));
						}*/

						// 需要删除的列
						Integer needDeleteId = tenantRegisterMapper.insertNeedDeleteColumn(map);
						// 需要插入的列
						map.put("opType", "insert");
						Integer needInsertId = tenantRegisterMapper.insertNeedInsertColumn(map);
					}
				}
			}
		} catch (Exception e) {
			// 回滚
			Map err = new HashMap();
			err.put("batchNo", batchNo);
			err.put("createTime", SimpleDateUtil.toString(now, SimpleDateUtil.PATTERN_yyyy_MM_dd_HH_mm_ss));
			tenantRegisterMapper.deleteTenantUpgradeConfigLogByBatchNoAndCreateTime(err);
			result.put("retcode", "fail");
			result.put("retmsg", "更新配置数据失败");
			throw new EISException("更新配置数据失败");
		}

		// 删除列
		map.put("opType", "delete");
		List<TenantUpgradeConfigLog> logDeleteList = tenantRegisterMapper.getColumnByOpType(map);
		if (logDeleteList != null && logDeleteList.size() > 0) {
			for (TenantUpgradeConfigLog tucl : logDeleteList) {
				if (!StringUtil.isEmpty(tucl.getTableName()) && tucl.getConfigId() != null) {
					Map param = new HashMap();
					param.put("upgradeDb", upgradeDb);
					param.put("tableName", tucl.getTableName());
					param.put("id", tucl.getConfigId());
					tenantRegisterMapper.deleteTenantUpgradeconfigLogById(param);
				}
			}
		}

		// 插入列
		map.put("opType", "insert");
		Map<Integer, String> temp = new HashMap<>();
		List<TenantUpgradeConfigLog> logInsertList = tenantRegisterMapper.getColumnByOpType(map);
		if (logInsertList != null && logInsertList.size() > 0) {
			for (TenantUpgradeConfigLog tucl : logInsertList) {
				if (!StringUtil.isEmpty(tucl.getTableName()) && tucl.getConfigId() != null) {
					Map param = new HashMap();
					param.put("upgradeDb", upgradeDb);
					param.put("tableName", tucl.getTableName());
					param.put("templateDb", templateDb);
					param.put("id", tucl.getConfigId());
					param.put("upgradeTenant", upgradeTenant);
					param.put("templateTenant", templateTenant);
					String sql = tenantRegisterMapper.getNeedInsertColumnSql(param);
					Integer id = tenantRegisterMapper.toExecSql(sql);
				}
			}
		}

		return result;
	}

	public Map ignoreUpdateConfig(String upgradeDb, Integer upgradeTenant) throws EISException {
		Map result = new HashMap();
		List<Integer> appList = new ArrayList<>();
		List<Integer> repList = new ArrayList<>();
		List<Integer> listCList = new ArrayList<>();
		List<Integer> listCGList = new ArrayList<>();
		List<Integer> listCGRList = new ArrayList<>();
		List<Integer> pickItemList = new ArrayList<>();
		Map m = new HashMap();
		m.put("tenantId", upgradeTenant);
		m.put("db", upgradeDb);
		List<String> datasetAppletIds = tenantRegisterMapper.getDataByContactGroup(m);
		if (datasetAppletIds != null && datasetAppletIds.size() > 0) {
			for (String datasetAppletId : datasetAppletIds) {
				Map p = new HashMap();
				p.put("db", upgradeDb);
				p.put("tenantId", upgradeTenant);
				p.put("appletId", datasetAppletId);
				p.put("db", upgradeDb);
				List<String> reportList = tenantRegisterMapper.getDataByAppletToReport(p);
				reportList.add(datasetAppletId);

				p.put("appletIds", list2String(reportList));
				List<Integer> appletList = tenantRegisterMapper.getDataByApplet(p);
				Integer appletId = tenantRegisterMapper.getDataAppletByContactGroup(p);
				appletList.add(appletId);

				for (Integer id : appletList) {
					appList.add(id);
				}

				List<Integer> reportsList = tenantRegisterMapper.getDataByReport(p);
				if (reportsList != null && reportsList.size() > 0) {
					for (Integer id : reportsList) {
						repList.add(id);
					}
				}

				List<Integer> listColumnList = tenantRegisterMapper.getDataByListColumn(p);
				if (listColumnList != null && listColumnList.size() > 0) {
					for (Integer id : listColumnList) {
						listCList.add(id);
					}
				}

				List<Integer> listColumnGroupDisplayRuleList = tenantRegisterMapper.getDataByListColumnGroupDisplayRule(p);
				if (listColumnGroupDisplayRuleList != null && listColumnGroupDisplayRuleList.size() > 0) {
					for (Integer id : listColumnGroupDisplayRuleList) {
						listCGList.add(id);
					}
				}

				List<Integer> listColumnGroupRuleList = tenantRegisterMapper.getDataByListColumnGroupRule(p);
				if (listColumnGroupRuleList != null && listColumnGroupRuleList.size() > 0) {
					for (Integer id : listColumnGroupRuleList) {
						listCGRList.add(id);
					}
				}
			}
			Map ma = new HashMap();
			ma.put("tenantId", upgradeTenant);
			ma.put("ids", list2String(datasetAppletIds));
			ma.put("db", upgradeDb);
			List<Integer> appletButtonList = tenantRegisterMapper.getDataByAppletButton(ma);

			result.put("applet", list2String(appList));
			result.put("report", list2String(repList));
			result.put("list_column", list2String(listCList));
			result.put("list_column_group_display_rule", list2String(listCGList));
			result.put("list_column_group_rule", list2String(listCGRList));
			result.put("applet_button", list2String(appletButtonList));

		}

		// pick_item 的处理
		List<Integer> pickItemLists = tenantRegisterMapper.getDataByPickItem(m);
		if (pickItemLists != null && pickItemLists.size() > 0) {
			for (Integer id : pickItemLists) {
				pickItemList.add(id);
			}
			result.put("pick_item", list2String(pickItemList));
		}

		return result;
	}

	public String list2String (List list) throws EISException {
		StringBuffer sb = new StringBuffer();
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				if (!StringUtil.isEmpty(String.valueOf(list.get(i)))) {
					sb.append("'");
					sb.append(list.get(i));
					sb.append("'");
					if (i < list.size() - 1) {
						sb.append(",");
					}
				}
			}
		}
		return sb.toString();
	}

}

