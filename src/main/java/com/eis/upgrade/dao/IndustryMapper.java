package com.eis.upgrade.dao;

import com.eis.common.model.Industry;

import java.util.List;

public interface IndustryMapper {

	int deleteByPrimaryKey(Integer id);

	int insert(Industry record);

	int insertSelective(Industry record);

	Industry selectByPrimaryKey(Integer id);

	int updateByPrimaryKeySelective(Industry record);

	int updateByPrimaryKey(Industry record);

	List<Industry> selectAll(Integer tenantId);

	Industry selectOne(Integer tenantId);

	List<Industry> selectByOpenFlag(String openFlag);
}