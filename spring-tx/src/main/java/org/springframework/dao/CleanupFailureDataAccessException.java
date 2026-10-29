/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.dao;

/**
 * 当数据访问操作完成后无法清理资源，但实际操作本身已成功时抛出的异常。
 *
 * <p>例如，如果 JDBC Connection 在成功使用后无法关闭，
 * 则可能抛出此异常或其子类。
 *
 * <p>请注意，数据访问代码可能会在 {@code finally} 块中执行资源清理，
 * 因此会选择记录清理失败而不是重新抛出，以保留原始的数据访问异常（如果有）。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CleanupFailureDataAccessException extends NonTransientDataAccessException {

	/**
	 * CleanupFailureDataAccessException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause from the underlying data access API,
	 * such as JDBC
	 */
	public CleanupFailureDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
