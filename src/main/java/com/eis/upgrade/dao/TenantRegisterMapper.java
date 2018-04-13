package com.eis.upgrade.dao;

import com.eis.common.model.*;
import com.eis.core.model.BusinessComponent;

import java.util.List;
import java.util.Map;

public interface TenantRegisterMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TenantRegister record);

    int insertSelective(TenantRegister record);

    TenantRegister selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TenantRegister record);

    int updateByPrimaryKey(TenantRegister record);

    TenantRegister selectByPhone(String phone);

    void register(Map map);

    List<TenantRegister> selectByDomain(String domain);

    TenantRegister selectByTenantId(Integer tenantId);

    Map selectTenantRegisterInfo(Integer id);

    List<String> selectNeedCopyConfigTableList();

    List<String> selectTableDataByTableName(Map map);

    int insertSysConfigData(Map map);

    List<BusinessComponent> selectBusinessTablesByTenantId(Integer tenantId);

    void createBusinessTable(Map map);

    String getOtherColumns(Map map);

    List<String> getNeedCopyTableData();

    int insertNeedCopyData(Map map);

    int updateNeedCopyData(Map map);

    int insertTenantMerchantInfo(Map map);

    int updateStatus(Map map);

    String getBatchNo();

    String checkDbIsExist(String db);

    Integer checkTenantIsExist(Map map);

    Integer selectTenantType(Map map);

    Integer insertTenantVerify(Map map);

    List<Integer> getTenantInfoByBatchNo(String batchNo);

    Integer insertVerifyTenantTables(Map map);

    Integer insertVerifyLogByBcConfig(Map map);

    Integer insertVerifyLogByTable(Map map);

    List<TenantVerifyTenantTables> getBcConfig(Map map);

    Integer insertVerifyLogByColumnConfig(Map map);

    Integer insertVerifyLogByColumn(Map map);

    List<TenantVerifyLog> getTenantVerifyLogByNowAndTenandId(Map map);

    Integer insertTenantUpgradeLog(TenantUpgradeLog tul);

    Integer deleteTenantUpgradeLogById(Integer id);

    Integer deleteTenantVerifyById(Integer id);

    Integer deleteVerifyTenantTablesById(Integer id);

    Integer deleteVerifyLogById(Integer id);

    Integer deleteVerifyLogByIds(String ids);

    Integer insertTenantUpgradeTenants(TenantUpgradeTenants tut);

    Integer createBackupDatabase(Map map);

    String selectDatabase(Map map);

    Integer createBackupTenant(Map map);

    Integer deleteBackupTenantData(Map map);

    Integer insertBackupTenantData(Map map);

    Integer deleteBackupDatabase(String backupName);

    Integer deleteTenantUpgradeTenantsById(Integer id);

    List<Integer> selectTenantUpgradeTenantsByBatchNo(String batchNo);

    List<String> selectExistsTableByBackup(Map map);

    Integer deleteExistsTableByBackDbNameAndTablename(Map map);

    Integer createBackupTableFromUpgradeTable(Map map);

    Integer insertBackupTableFromUpgradeTable(Map map);

    Integer insertTenantUpgradeTableLogByCreateTable(Map map);

    Integer insertTenantUpgradeTableLogByDropTable(Map map);

    Integer insertTenantUpgradeTableLogByRenameTable(Map map);

    Integer insertTenantUpgradeTableLogByDropColumn(Map map);

    Integer insertTenantUpgradeTableLogByChangeColumn(Map map);

    Integer insertTenantUpgradeTableLogByAddColumn(Map map);

    Integer deleteTenantUpgradeTableByBatchNoAndTenantId(Map map);

    List<String> selectTenantUpgradeTableLogByBatchNoAndTenantIdAndOpType(Map map);

    Integer toExecSql(String sql);

    List<String> selectTableNameFromTenantUpgradeConfigTable();

    Integer insertNeedDeleteColumn(Map map);

    Integer insertNeedInsertColumn(Map map);

    Integer deleteTenantUpgradeConfigLogByBatchNoAndCreateTime(Map map);

    List<TenantUpgradeConfigLog> getColumnByOpType(Map map);

    Integer deleteTenantUpgradeconfigLogById(Map map);

    String getNeedInsertColumnSql(Map map);

    Integer updateTenantUpgradeLogStatus(Map map);

    Integer deleteTenantTableByUpgradeDbAndTenantId(Map map);

    Integer createTenantTableByBackupDb(Map map);

    Integer insertRegisterLog(TenantRegisterLog trl);

    List<String> getNeedDeleteTenant(Map map);

    Integer toDeleteTable(Map map);

    Integer toDeleteSysTable(Map map);

    TenantRegister getTenantRegister(Map map);

    TenantMerchantInfo getTenantMerchantInfo(Map map);

    Integer insertTenantRegister(TenantRegister tenantRegister);

    String toCheckDbIsExist(String dbName);

    List<String> toCheckDbAndTenantIsExist(Map map);

    String getResponsibilityId(Map map);

    Integer deleteEmpResLink(Map map);

    List<String> getNeedDeleteLinkTables(Map map);

    Integer deleteResLink(Map map);

    List<TableIndex> getTableIndexInfo(Map map);

    Integer toAddTableIndex(Map map);

    List<String> toCheckRegisterTemplateTables(Map map);

    Integer toCheckIndexNameIsExist(Map map);

    Integer getRepeatDataBySysTable(Map map);

    Integer insertSelectiveTenantVerifyLog(TenantVerifyLog tvl);

    List<String> getDataByContactGroup(Map map);

    Integer getDataAppletByContactGroup(Map map);

    List<String> getDataByAppletToReport(Map map);

    List<Integer> getDataByApplet(Map map);

    List<Integer> getDataByReport(Map map);

    List<Integer> getDataByListColumn(Map map);

    List<Integer> getDataByListColumnGroupDisplayRule(Map map);

    List<Integer> getDataByListColumnGroupRule(Map map);

    List<Integer> getDataByAppletButton(Map map);

    List<Integer> getIsUniqueInfo(Integer tenantId, String tableName, String columnName, String upgradeDb);

    List<Integer> getDataByPickItem(Map map);
}