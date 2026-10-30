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

package org.springframework.validation;

import java.util.List;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * 存储并公开特定对象的数据绑定和校验错误信息。
 *
 * <p>字段名可以是目标对象的属性（例如绑定到 customer 对象时的 "name"），也可以是子对象中的嵌套字段
 * （例如 "address.street"）。支持通过 {@link #setNestedPath(String)} 进行子树导航：例如，
 * {@code AddressValidator} 校验的是 "address"，而并不知道它是 customer 的一个子对象。
 *
 * <p>注意：{@code Errors} 对象是单线程的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
 */
public interface Errors {

	/**
	 * 嵌套路径中各路径元素之间的分隔符，例如 "customer.name" 或 "customer.address.street" 中的分隔符。
	 *
	 * <p>"." 与 beans 包中的
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR nested property separator}
	 * 相同。
	 */
	String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;


	/**
	 * 返回所绑定根对象的名称。
	 */
	String getObjectName();

	/**
	 * 允许更改上下文，以便标准校验器可以校验子树。拒绝调用会为字段名加上给定路径作为前缀。
	 *
	 * <p>例如，地址校验器可以校验 customer 对象的子对象 "address"。
	 * @param nestedPath 此对象内的嵌套路径，
	 * 例如 “address”（默认为空字符串，也可接受 {@code null}）。
	 * 可以以点结尾：“address” 和 “address.” 均有效。
	 */
	void setNestedPath(String nestedPath);

	/**
	 * 返回此 {@link Errors} 对象的当前嵌套路径。
	 *
	 * <p>返回带点的嵌套路径，即 "address."，以便于构建拼接路径。默认为空字符串。
	 */
	String getNestedPath();

	/**
	 * 将给定的子路径压入嵌套路径栈。
	 *
	 * <p>{@link #popNestedPath()} 调用会将嵌套路径重置为相应 {@code pushNestedPath(String)} 调用之前的
	 * 原始嵌套路径。
	 *
	 * <p>使用嵌套路径栈可以为子对象设置临时嵌套路径，而无需担心临时路径的持有问题。
	 *
	 * <p>例如：当前路径为 "spouse."，pushNestedPath("child") &rarr; 结果路径为 "spouse.child."；
	 * popNestedPath() &rarr; 又回到 "spouse."。
	 * @param subPath 要压入嵌套路径栈的子路径。
	 * @see #popNestedPath
	 */
	void pushNestedPath(String subPath);

	/**
	 * 从嵌套路径栈中弹出前一个嵌套路径。
	 * @throws IllegalStateException 如果栈中没有更早的嵌套路径。
	 * @see #pushNestedPath
	 */
	void popNestedPath() throws IllegalStateException;

	/**
	 * 使用给定的错误描述，为整个目标对象注册一个全局错误。
	 * @param errorCode 错误代码，可作为消息键解释。
	 */
	void reject(String errorCode);

	/**
	 * 使用给定的错误描述，为整个目标对象注册一个全局错误。
	 * @param errorCode 错误代码，可作为消息键解释。
	 * @param defaultMessage 回退的默认消息。
	 */
	void reject(String errorCode, String defaultMessage);

	/**
	 * 使用给定的错误描述，为整个目标对象注册一个全局错误。
	 * @param errorCode 错误代码，可作为消息键解释。
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）。
	 * @param defaultMessage 回退的默认消息。
	 */
	void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * 为当前对象的指定字段（如果存在当前嵌套路径，则遵循该路径）注册字段错误，使用给定的错误描述。
	 *
	 * <p>字段名可以为 {@code null} 或空字符串，以表示当前对象本身而不是它的某个字段。这可能会在嵌套对
	 * 象图中产生相应的字段错误，或者如果当前对象是顶层对象，则产生全局错误。
	 * @param field 字段名（可以为 {@code null} 或空字符串）。
	 * @param errorCode 错误代码，可作为消息键解释。
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode);

	/**
	 * 为当前对象的指定字段（如果存在当前嵌套路径，则遵循该路径）注册字段错误，使用给定的错误描述。
	 *
	 * <p>字段名可以为 {@code null} 或空字符串，以表示当前对象本身而不是它的某个字段。这可能会在嵌套对
	 * 象图中产生相应的字段错误，或者如果当前对象是顶层对象，则产生全局错误。
	 * @param field 字段名（可以为 {@code null} 或空字符串）。
	 * @param errorCode 错误代码，可作为消息键解释。
	 * @param defaultMessage 回退的默认消息。
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode, String defaultMessage);

	/**
	 * 为当前对象的指定字段（如果存在当前嵌套路径，则遵循该路径）注册字段错误，使用给定的错误描述。
	 *
	 * <p>字段名可以为 {@code null} 或空字符串，以表示当前对象本身而不是它的某个字段。这可能会在嵌套对
	 * 象图中产生相应的字段错误，或者如果当前对象是顶层对象，则产生全局错误。
	 * @param field 字段名（可以为 {@code null} 或空字符串）。
	 * @param errorCode 错误代码，可作为消息键解释。
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）。
	 * @param defaultMessage 回退的默认消息。
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * 将给定 {@code Errors} 实例中的所有错误添加到此 {@code Errors} 实例。
	 *
	 * <p>这是一个便捷方法，可避免为了将一个 {@code Errors} 实例合并到另一个 {@code Errors} 实例而反复
	 * 调用 {@code reject(..)}。
	 *
	 * <p>请注意，传入的 {@code Errors} 实例应当引用同一个目标对象，或者至少包含适用于此 {@code Errors}
	 * 实例的目标对象的兼容错误。
	 * @param errors 要合并进来的 {@code Errors} 实例。
	 */
	void addAllErrors(Errors errors);

	/**
	 * 返回是否存在任何错误。
	 */
	boolean hasErrors();

	/**
	 * 返回错误总数。
	 */
	int getErrorCount();

	/**
	 * 获取所有错误，包括全局错误和字段错误。
	 * @return 由 {@link ObjectError} 实例组成的列表。
	 */
	List<ObjectError> getAllErrors();

	/**
	 * 是否存在任何全局错误？
	 * @return 如果存在任何全局错误，则为 {@code true}。
	 * @see #hasFieldErrors()
	 */
	boolean hasGlobalErrors();

	/**
	 * 返回全局错误数量。
	 * @return 全局错误的数量。
	 * @see #getFieldErrorCount()
	 */
	int getGlobalErrorCount();

	/**
	 * 获取所有全局错误。
	 * @return 由 {@link ObjectError} 实例组成的列表。
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * 获取<i>第一个</i>全局错误（如果有）。
	 * @return 全局错误，或 {@code null}。
	 */
	@Nullable
	ObjectError getGlobalError();

	/**
	 * 是否存在任何字段错误？
	 * @return 如果存在任何与字段关联的错误，则为 {@code true}。
	 * @see #hasGlobalErrors()
	 */
	boolean hasFieldErrors();

	/**
	 * 返回与某个字段关联的错误数量。
	 * @return 与字段关联的错误数量。
	 * @see #getGlobalErrorCount()
	 */
	int getFieldErrorCount();

	/**
	 * 获取与某个字段关联的所有错误。
	 * @return 由 {@link FieldError} 实例组成的 List。
	 */
	List<FieldError> getFieldErrors();

	/**
	 * 获取与某个字段关联的<i>第一个</i>错误（如果有）。
	 * @return 字段特定的错误，或 {@code null}。
	 */
	@Nullable
	FieldError getFieldError();

	/**
	 * 是否存在与给定字段关联的任何错误？
	 * @param field 字段名。
	 * @return 如果存在任何与给定字段关联的错误，则为 {@code true}。
	 */
	boolean hasFieldErrors(String field);

	/**
	 * 返回与给定字段关联的错误数量。
	 * @param field 字段名。
	 * @return 与给定字段关联的错误数量。
	 */
	int getFieldErrorCount(String field);

	/**
	 * 获取与给定字段关联的所有错误。
	 *
	 * <p>实现不仅应支持 "name" 这样的完整字段名，还应支持 "na*" 或 "address.*" 之类的模式匹配。
	 * @param field 字段名。
	 * @return 由 {@link FieldError} 实例组成的 List。
	 */
	List<FieldError> getFieldErrors(String field);

	/**
	 * 获取与给定字段关联的第一个错误（如果有）。
	 * @param field 字段名。
	 * @return 字段特定的错误，或 {@code null}。
	 */
	@Nullable
	FieldError getFieldError(String field);

	/**
	 * 返回给定字段的当前值，即当前的 Bean 属性值或上次绑定中被拒绝的更新值。
	 *
	 * <p>可以方便地访问用户指定的字段值，即使存在类型不匹配。
	 * @param field 字段名。
	 * @return 给定字段的当前值。
	 */
	@Nullable
	Object getFieldValue(String field);

	/**
	 * 返回给定字段的类型。
	 *
	 * <p>即使字段值为 {@code null}，实现也应能够确定类型，例如通过某个关联的描述符。
	 * @param field 字段名。
	 * @return 字段的类型；如果无法确定，则为 {@code null}。
	 */
	@Nullable
	Class<?> getFieldType(String field);

}
