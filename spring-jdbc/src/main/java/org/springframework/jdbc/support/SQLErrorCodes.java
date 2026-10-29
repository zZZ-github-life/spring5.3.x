/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于保存特定数据库的 JDBC 错误码的 JavaBean。
 * 此类的实例通常通过 bean 工厂加载。
 *
 * <p>由 Spring 的 {@link SQLErrorCodeSQLExceptionTranslator} 使用。
 * 本包中的 "sql-error-codes.xml" 文件包含各种数据库的默认
 * {@code SQLErrorCodes} 实例。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLErrorCodesFactory
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLErrorCodes {

	@Nullable
	private String[] databaseProductNames;

	private boolean useSqlStateForTranslation = false;

	private String[] badSqlGrammarCodes = new String[0];

	private String[] invalidResultSetAccessCodes = new String[0];

	private String[] duplicateKeyCodes = new String[0];

	private String[] dataIntegrityViolationCodes = new String[0];

	private String[] permissionDeniedCodes = new String[0];

	private String[] dataAccessResourceFailureCodes = new String[0];

	private String[] transientDataAccessResourceCodes = new String[0];

	private String[] cannotAcquireLockCodes = new String[0];

	private String[] deadlockLoserCodes = new String[0];

	private String[] cannotSerializeTransactionCodes = new String[0];

	@Nullable
	private CustomSQLErrorCodesTranslation[] customTranslations;

	@Nullable
	private SQLExceptionTranslator customSqlExceptionTranslator;


	/**
	 * 如果数据库名称包含空格（此时无法使用 bean 名称进行查找），
	 * 请设置此属性。
	 */
	public void setDatabaseProductName(@Nullable String databaseProductName) {
		this.databaseProductNames = new String[] {databaseProductName};
	}

	@Nullable
	public String getDatabaseProductName() {
		return (this.databaseProductNames != null && this.databaseProductNames.length > 0 ?
				this.databaseProductNames[0] : null);
	}

	/**
	 * 设置此属性以指定多个包含空格的数据库名称
	 * （此时无法使用 bean 名称进行查找）。
	 */
	public void setDatabaseProductNames(@Nullable String... databaseProductNames) {
		this.databaseProductNames = databaseProductNames;
	}

	@Nullable
	public String[] getDatabaseProductNames() {
		return this.databaseProductNames;
	}

	/**
	 * 对于不提供错误码但提供 SQL State 的数据库（包括 PostgreSQL），
	 * 请将此属性设置为 true。
	 */
	public void setUseSqlStateForTranslation(boolean useStateCodeForTranslation) {
		this.useSqlStateForTranslation = useStateCodeForTranslation;
	}

	public boolean isUseSqlStateForTranslation() {
		return this.useSqlStateForTranslation;
	}

	public void setBadSqlGrammarCodes(String... badSqlGrammarCodes) {
		this.badSqlGrammarCodes = StringUtils.sortStringArray(badSqlGrammarCodes);
	}

	public String[] getBadSqlGrammarCodes() {
		return this.badSqlGrammarCodes;
	}

	public void setInvalidResultSetAccessCodes(String... invalidResultSetAccessCodes) {
		this.invalidResultSetAccessCodes = StringUtils.sortStringArray(invalidResultSetAccessCodes);
	}

	public String[] getInvalidResultSetAccessCodes() {
		return this.invalidResultSetAccessCodes;
	}

	public String[] getDuplicateKeyCodes() {
		return this.duplicateKeyCodes;
	}

	public void setDuplicateKeyCodes(String... duplicateKeyCodes) {
		this.duplicateKeyCodes = duplicateKeyCodes;
	}

	public void setDataIntegrityViolationCodes(String... dataIntegrityViolationCodes) {
		this.dataIntegrityViolationCodes = StringUtils.sortStringArray(dataIntegrityViolationCodes);
	}

	public String[] getDataIntegrityViolationCodes() {
		return this.dataIntegrityViolationCodes;
	}

	public void setPermissionDeniedCodes(String... permissionDeniedCodes) {
		this.permissionDeniedCodes = StringUtils.sortStringArray(permissionDeniedCodes);
	}

	public String[] getPermissionDeniedCodes() {
		return this.permissionDeniedCodes;
	}

	public void setDataAccessResourceFailureCodes(String... dataAccessResourceFailureCodes) {
		this.dataAccessResourceFailureCodes = StringUtils.sortStringArray(dataAccessResourceFailureCodes);
	}

	public String[] getDataAccessResourceFailureCodes() {
		return this.dataAccessResourceFailureCodes;
	}

	public void setTransientDataAccessResourceCodes(String... transientDataAccessResourceCodes) {
		this.transientDataAccessResourceCodes = StringUtils.sortStringArray(transientDataAccessResourceCodes);
	}

	public String[] getTransientDataAccessResourceCodes() {
		return this.transientDataAccessResourceCodes;
	}

	public void setCannotAcquireLockCodes(String... cannotAcquireLockCodes) {
		this.cannotAcquireLockCodes = StringUtils.sortStringArray(cannotAcquireLockCodes);
	}

	public String[] getCannotAcquireLockCodes() {
		return this.cannotAcquireLockCodes;
	}

	public void setDeadlockLoserCodes(String... deadlockLoserCodes) {
		this.deadlockLoserCodes = StringUtils.sortStringArray(deadlockLoserCodes);
	}

	public String[] getDeadlockLoserCodes() {
		return this.deadlockLoserCodes;
	}

	public void setCannotSerializeTransactionCodes(String... cannotSerializeTransactionCodes) {
		this.cannotSerializeTransactionCodes = StringUtils.sortStringArray(cannotSerializeTransactionCodes);
	}

	public String[] getCannotSerializeTransactionCodes() {
		return this.cannotSerializeTransactionCodes;
	}

	public void setCustomTranslations(CustomSQLErrorCodesTranslation... customTranslations) {
		this.customTranslations = customTranslations;
	}

	@Nullable
	public CustomSQLErrorCodesTranslation[] getCustomTranslations() {
		return this.customTranslations;
	}

	public void setCustomSqlExceptionTranslatorClass(@Nullable Class<? extends SQLExceptionTranslator> customTranslatorClass) {
		if (customTranslatorClass != null) {
			try {
				this.customSqlExceptionTranslator =
						ReflectionUtils.accessibleConstructor(customTranslatorClass).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to instantiate custom translator", ex);
			}
		}
		else {
			this.customSqlExceptionTranslator = null;
		}
	}

	public void setCustomSqlExceptionTranslator(@Nullable SQLExceptionTranslator customSqlExceptionTranslator) {
		this.customSqlExceptionTranslator = customSqlExceptionTranslator;
	}

	@Nullable
	public SQLExceptionTranslator getCustomSqlExceptionTranslator() {
		return this.customSqlExceptionTranslator;
	}

}
