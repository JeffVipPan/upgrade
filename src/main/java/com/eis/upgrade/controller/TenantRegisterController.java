package com.eis.upgrade.controller;

import com.eis.common.model.IndustryTemplate;
import com.eis.common.model.Tenant;
import com.eis.common.model.TenantVerifyLog;
import com.eis.core.annotation.NotCheckToken;
import com.eis.core.controller.BaseController;
import com.eis.core.service.IndustryTemplateService;
import com.eis.core.service.TenantService;
import com.eis.core.util.StringUtil;
import com.eis.upgrade.service.TenantRegisterService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @FileName：TenantRegisterController.java
 * @Description：@TODO
 * @Author：thy
 * @CreateTime：上午9:13:44
 */
@RestController
@RequestMapping("/v1/service")
@Api(value = "tenant register", description = "Tenant register")
public class TenantRegisterController extends BaseController {
	private static final Logger log = Logger.getLogger(TenantRegisterController.class);

	@Autowired
	@Qualifier(value = "coreIndustryTemplateService")
	private IndustryTemplateService industryTemplateService;

	@Autowired
	private TenantService tenantService;


	@Autowired
	private TenantRegisterService tenantRegisterService;



	@RequestMapping(value = "/tenantUpgrade", method = RequestMethod.POST)
	@ApiOperation(value = "tenantUpgrade", notes = "tenantUpgrade")
	@NotCheckToken
	public Object tenantUpgrade(@RequestParam String upgradeDb, @RequestParam String templateDb, @RequestParam Integer upgradeTenant, @RequestParam Integer templateTenant) {
		Date now = new Date();
		Map result = new HashMap();
		result.put("retcode", "fail");
		result.put("retmsg", "校验租户系统异常");

		// 0. 校验升级库和模版库是否存在，是否有升级表和模板表
		String upgradeTemp = tenantRegisterService.toCheckDbIsExist(upgradeDb);
		if (StringUtil.isEmpty(upgradeTemp)) {
			result.put("retcode", "fail");
			result.put("retmsg", "升级库不存在");
			return result;
		}
		String templateTemp = tenantRegisterService.toCheckDbIsExist(templateDb);
		if (StringUtil.isEmpty(templateTemp)) {
			result.put("retcode", "fail");
			result.put("retmsg", "模板库不存在");
			return result;
		}
		Map p = new HashMap();
		p.put("dbName", upgradeDb);
		p.put("tenantId", upgradeTenant);
		List<String> upgradeList = tenantRegisterService.toCheckDbAndTenantIsExist(p);
		if (upgradeList == null || upgradeList.size() == 0) {
			result.put("retcode", "fail");
			result.put("retmsg", "升级租户不存在");
			return result;
		}
		p.put("dbName", templateDb);
		p.put("tenantId", templateTenant);
		List<String> templateList = tenantRegisterService.toCheckDbAndTenantIsExist(p);
		if (templateList == null || templateList.size() == 0) {
			result.put("retcode", "fail");
			result.put("retmsg", "模板租户不存在");
			return result;
		}

		// 1. 校验
		//result = tenantRegisterService.toCheck(templateDb, templateTenant, upgradeDb, upgradeTenant);
		//result = tenantRegisterService.toCheck(templateDb, templateTenant, null, null);
		/*if (result.get("retcode").equals("ok")) {
			// 2. 查询校验是否有报错
			Map m = new HashMap();
			m.put("now", now);
			m.put("tenantId", templateTenant);
			List<TenantVerifyLog> list = tenantRegisterService.getTenantVerifyLogByNowAndTenandId(m);
			m.put("tenantId", upgradeTenant);
			List<TenantVerifyLog> lists = tenantRegisterService.getTenantVerifyLogByNowAndTenandId(m);

			if (list != null && list.size() > 0) {
				result.put("retcode", "fail");
				result.put("retmsg", "校验租户有错误数据");
				return result;
			} else if (lists != null && lists.size() > 0) {
				result.put("retcode", "fail");
				result.put("retmsg", "校验租户有错误数据");
				return result;
			} else {*/
				// 3. 校验通过后升级
				Map upgradeInfo = tenantRegisterService.toUpgrade(upgradeDb, templateDb, upgradeTenant, templateTenant);
				if (upgradeInfo.get("retcode").equals("ok")) {
					/*// 4. 再次校验
					Map result2 = tenantRegisterService.toCheck(null, null, upgradeDb, upgradeTenant);
					if (result2.get("retcode").equals("ok")) {
						// 5. 再次查询校验是否有报错
						Map mas = new HashMap();
						mas.put("now", now);
						mas.put("tenantId", upgradeTenant);
						List<TenantVerifyLog> list2 = tenantRegisterService.getTenantVerifyLogByNowAndTenandId(m);
						if (list != null && list.size() > 0) {
							result.put("retcode", "fail");
							result.put("retmsg", "升级后校验有错");
							return result;
						} else {
							result.put("retcode", "ok");
							result.put("retmsg", "升级成功");
							return result;
						}
					} else {
						result.put("retcode", "fail");
						result.put("retmsg", "升级后校验失败");
						return result;
					}*/
					result.put("retcode", "ok");
					result.put("retmsg", "升级成功");
					return result;
				} else {
					return upgradeInfo;
				}
			//}

		//} else {
			//return result;
		//}
	}

	@RequestMapping(value = "/batchTenantUpgrade", method = RequestMethod.POST)
	@ApiOperation(value = "batchTenantUpgrade", notes = "batchTenantUpgrade")
	@NotCheckToken
	public Object batchTenantUpgrade(@RequestParam Integer template, @RequestParam final String upgradeDb, @RequestParam final String templateDb) {
		Map result = new HashMap();
		result.put("retcode", "ok");
		result.put("retmsg", "批量升级成功");
		Date now = new Date();
		// 库
		/*final String db = PropertiesFileUtil.getPro("config.properties", "jdbc_sid");
		if (StringUtil.isEmpty(db)) {
			result.put("retcode", "fail");
			result.put("retmsg", "升级库不存在");
			return result;
		}*/
		// 根据行业模板获取到模板租户
		//IndustryTemplate industryTemplate = industryTemplateMapper.selectByPrimaryKey(template);
		IndustryTemplate industryTemplate = industryTemplateService.selectByPrimaryKeyAndSchemaDb(template, templateDb);
		if (industryTemplate == null) {
			result.put("retcode", "fail");
			result.put("retmsg", "升级行业模板不存在");
			return result;
		}
		// 模板租户
		final Integer templateTenant = industryTemplate.getTenantId();
		List<Tenant> list = tenantService.getTenantListByTemplateIdAndDb(template, upgradeDb);
		if (list != null && list.size() > 0) {

			// 1. 校验
			result = tenantRegisterService.toCheck(templateDb, templateTenant, upgradeDb, templateTenant);
		    if (result.get("retcode").equals("ok")) {
				// 2. 查询校验是否有报错
				Map m = new HashMap();

				m.put("now", now);
				m.put("tenantId", templateTenant);
				List<TenantVerifyLog> lists = tenantRegisterService.getTenantVerifyLogByNowAndTenandId(m);
				if (lists != null && lists.size() > 0) {
					result.put("retcode", "fail");
					result.put("retmsg", "校验租户有错误数据");
					return result;
				} else {
					final StringBuffer sb = new StringBuffer();
					//  ========================  多线程 start  =============================
					// 开启多条线程去批量升级
					final List<CopyOnWriteArrayList<Tenant>> list0 = new ArrayList<CopyOnWriteArrayList<Tenant>>();

					int count = list.size()/20;
					int yu = list.size() % 20;
					int threadCount = 20;
					if (count == 0) {
						count = list.size();
						threadCount = count;
					}
					if (threadCount == 20) {
						for (int i = 0; i < 20; i++) {
							CopyOnWriteArrayList<Tenant> subList = new CopyOnWriteArrayList<Tenant>();
							if (i == 19) {
								subList = new CopyOnWriteArrayList<Tenant>(list.subList(i * count, count * (i + 1) + yu));
							} else {
								subList = new CopyOnWriteArrayList<Tenant>(list.subList(i * count, count * (i + 1)));
							}
							list0.add(subList);
						}
					} else {
						for (int i = 0; i < threadCount; i++) {
							CopyOnWriteArrayList<Tenant> subList = new CopyOnWriteArrayList<Tenant>();
							subList = new CopyOnWriteArrayList<Tenant>(list.subList(i, 1 * (i + 1)));
							list0.add(subList);
						}
					}

					// 线程池
					ExecutorService pool = Executors.newFixedThreadPool(threadCount);

					List<Future<Object>> resultList = new ArrayList();

					for (int i = 0; i < list0.size(); i++) {
						final int finalI = i;
						Future<Object> submit = pool.submit(new Callable<Object>() {
							@Override
							public synchronized Object call() throws Exception {
								String result = null;
								CopyOnWriteArrayList<Tenant> list2 = list0.get(finalI);
								if (list2 != null && list2.size() > 0) {
									result = autoExecute(list2, templateTenant, upgradeDb, templateDb, new StringBuffer());
								}
								return result;
							}
						});
						resultList.add(submit);
					}

					for (Future<Object> fs : resultList) {
						try {
							if (fs.get() != null) {
								System.out.println(fs.get());
								sb.append(fs.get());
								sb.append("; ");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						} finally {
							pool.shutdown();
						}
					}

					result.put("retmsg", sb.toString());
				}
			}
			//  ========================  多线程 end  =============================

			/*for (Tenant t : list) {
				if (t != null) {
					if (t.getTenantId() != templateTenant) {
						Map map = (Map) this.tenantUpgrade(db, db, t.getTenantId(), templateTenant);
						if (map.get("retcode").equals("ok")) {
							sb.append("租户" + t.getTenantId() + "升级成功");
						} else {
							sb.append("租户" + t.getTenantId() + "升级失败");
							continue;
						}

					}
				}
			}
			/*result.put("retcode", "ok");
			result.put("retmsg", sb.toString());*/
		} else {
			result.put("retcode", "fail");
			result.put("retmsg", "暂无要升级的租户");
			return result;
		}

		return result;
	}

	public List<CopyOnWriteArrayList<Tenant>> autoFillList(List<CopyOnWriteArrayList<Tenant>> list0) {
		if (list0.size() < 10) {
			CopyOnWriteArrayList<Tenant> list = new CopyOnWriteArrayList<>();
			list0.add(list);
			autoFillList(list0);
		}
		return list0;
	}

	public String autoExecute(CopyOnWriteArrayList<Tenant> list, Integer templateTenant, String upgradeDb, String templateDb, StringBuffer sb) {
		for (Tenant t : list) {
			if (t != null) {
				Map map = (Map) tenantUpgrade(upgradeDb, templateDb, t.getTenantId(), templateTenant);
				if (map.get("retcode").equals("ok")) {
					sb.append("租户" + t.getTenantId() + "升级成功");
				} else {
					sb.append("租户" + t.getTenantId() + "升级失败");
					continue;
				}
			}
		}
		return sb.toString();
	}


}