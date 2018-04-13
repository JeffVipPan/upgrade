package com.eis.upgrade.service.impl;

import com.eis.common.model.FieldData;
import com.eis.core.service.BusinessComponentTriggerService;
import com.eis.exception.EISException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * @FileName：BusinessComponentTriggerServiceImpl.java
 * @Description：@TODO
 * @Author：thy
 * @CreateTime：下午9:53:39
 */
@Service
public class BusinessComponentTriggerServiceImpl implements BusinessComponentTriggerService {

	@Override
	public void afterInsert(Integer userId, Integer tenantId, String bcTableName, String fullTableName, List<FieldData> fieldDataList, Integer valueId) throws EISException {

	}

	@Override
	public void afterUpdate(Integer tenantId, String bcTableName, String fullTableName, List<FieldData> fieldDataList, Integer valueId, HashMap dataBeforeUpdate)  throws EISException {

	}
}

