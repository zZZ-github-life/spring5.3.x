/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.dao.support;

import java.util.Collection;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;

/**
 * DAO 实现的各种实用工具方法。
 * 适用于任何数据访问技术。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public abstract class DataAccessUtils {

	/**
	 * 从给定的 Collection 中返回单个结果对象。
	 * <p>如果找到 0 个结果对象则返回 {@code null}；
	 * 如果找到多于 1 个元素则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}）
	 * @return 单个结果对象；如果没有则为 {@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个元素
	 */
	@Nullable
	public static <T> T singleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			return null;
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的 Collection 中返回单个结果对象。
	 * <p>如果找到 0 个或多于 1 个元素则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 但不期望其中包含 {@code null} 元素）
	 * @return 单个结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个元素
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何元素
	 */
	public static <T> T requiredSingleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			throw new EmptyResultDataAccessException(1);
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的 Collection 中返回单个结果对象。
	 * <p>如果找到 0 个或多于 1 个元素则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 并且期望其中包含 {@code null} 元素）
	 * @return 单个结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个元素
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何元素
	 * @since 5.0.2
	 */
	@Nullable
	public static <T> T nullableSingleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
		// This is identical to the requiredSingleResult implementation but differs in the
		// semantics of the incoming Collection (which we currently can't formally express)
		if (CollectionUtils.isEmpty(results)) {
			throw new EmptyResultDataAccessException(1);
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的 Collection 中返回唯一的结果对象。
	 * <p>如果找到 0 个结果对象则返回 {@code null}；
	 * 如果找到多于 1 个实例则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}）
	 * @return 唯一结果对象；如果没有则为 {@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个结果对象
	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
	 */
	@Nullable
	public static <T> T uniqueResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			return null;
		}
		if (!CollectionUtils.hasUniqueObject(results)) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的 Collection 中返回唯一的结果对象。
	 * <p>如果找到 0 个或多于 1 个实例则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 但不期望其中包含 {@code null} 元素）
	 * @return 唯一结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何结果对象
	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
	 */
	public static <T> T requiredUniqueResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			throw new EmptyResultDataAccessException(1);
		}
		if (!CollectionUtils.hasUniqueObject(results)) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的 Collection 中返回唯一的结果对象。
	 * 如果找到 0 个或多于 1 个结果对象，或者唯一的结果对象
	 * 无法转换为指定的必需类型，则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 但不期望其中包含 {@code null} 元素）
	 * @return 唯一结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果唯一对象与指定的必需类型不匹配
	 */
	@SuppressWarnings("unchecked")
	public static <T> T objectResult(@Nullable Collection<?> results, @Nullable Class<T> requiredType)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		Object result = requiredUniqueResult(results);
		if (requiredType != null && !requiredType.isInstance(result)) {
			if (String.class == requiredType) {
				result = result.toString();
			}
			else if (Number.class.isAssignableFrom(requiredType) && result instanceof Number) {
				try {
					result = NumberUtils.convertNumberToTargetClass(((Number) result), (Class<? extends Number>) requiredType);
				}
				catch (IllegalArgumentException ex) {
					throw new TypeMismatchDataAccessException(ex.getMessage());
				}
			}
			else {
				throw new TypeMismatchDataAccessException(
						"Result object is of type [" + result.getClass().getName() +
						"] and could not be converted to required type [" + requiredType.getName() + "]");
			}
		}
		return (T) result;
	}

	/**
	 * 从给定的 Collection 中返回唯一的 int 结果。
	 * 如果找到 0 个或多于 1 个结果对象，或者唯一的结果对象
	 * 无法转换为 int，则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 但不期望其中包含 {@code null} 元素）
	 * @return 唯一的 int 结果
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果集合中的唯一对象无法转换为 int
	 */
	public static int intResult(@Nullable Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).intValue();
	}

	/**
	 * 从给定的 Collection 中返回唯一的 long 结果。
	 * 如果找到 0 个或多于 1 个结果对象，或者唯一的结果对象
	 * 无法转换为 long，则抛出异常。
	 * @param results 结果 Collection（可以为 {@code null}，
	 * 但不期望其中包含 {@code null} 元素）
	 * @return 唯一的 long 结果
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的 Collection 中
	 * 找到多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的 Collection 中
	 * 没有找到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果集合中的唯一对象无法转换为 long
	 */
	public static long longResult(@Nullable Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).longValue();
	}


	/**
	 * 如果合适则返回翻译后的异常，
	 * 否则返回给定的原始异常。
	 * @param rawException 我们可能希望转换的异常
	 * @param pet 用于执行转换的 PersistenceExceptionTranslator
	 * @return 如果可以进行转换，则为转换后的持久化异常，
	 * 否则为原始异常
	 */
	public static RuntimeException translateIfNecessary(
			RuntimeException rawException, PersistenceExceptionTranslator pet) {

		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		DataAccessException dae = pet.translateExceptionIfPossible(rawException);
		return (dae != null ? dae : rawException);
	}

}
