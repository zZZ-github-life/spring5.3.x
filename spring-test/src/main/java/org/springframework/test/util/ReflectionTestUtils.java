/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ReflectionTestUtils} 是一组基于反射的实用方法，用于单元和集成测试场景。
 *
 * <p>在测试涉及以下内容的代码时，通常能够设置非 {@code public} 字段、调用非 {@code public} 的 setter 方法，或调用非 {@code public} 的<em>配置</em>或<em>生命周期</em>回调方法会很有帮助：
 *
 * <ul>
 * <li>ORM 框架（如 JPA 和 Hibernate），这些框架允许对域实体中的属性使用 {@code private} 或 {@code protected} 字段访问，而不是 {@code public} 的 setter 方法。</li>
 * <li>Spring 对 {@link org.springframework.beans.factory.annotation.Autowired @Autowired}、{@link javax.inject.Inject @Inject} 和 {@link javax.annotation.Resource @Resource} 等注解的支持，这些注解为 {@code private} 或 {@code protected} 字段、setter 方法和配置方法提供依赖注入。</li>
 * <li>将 {@link javax.annotation.PostConstruct @PostConstruct} 和 {@link javax.annotation.PreDestroy @PreDestroy} 等注解用于生命周期回调方法。</li>
 * </ul>
 *
 * <p>此外，此类中的若干方法还支持 {@code static} 字段和 {@code static} 方法 &mdash; 例如 {@link #setField(Class, String, Object)}、{@link #getField(Class, String)}、{@link #invokeMethod(Class, String, Object...)}、{@link #invokeMethod(Object, Class, String, Object...)} 等。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ReflectionUtils
 * @see AopTestUtils
 */
public abstract class ReflectionTestUtils {

	private static final String SETTER_PREFIX = "set";

	private static final String GETTER_PREFIX = "get";

	private static final Log logger = LogFactory.getLog(ReflectionTestUtils.class);

	private static final boolean springAopPresent = ClassUtils.isPresent(
			"org.springframework.aop.framework.Advised", ReflectionTestUtils.class.getClassLoader());


	/**
	 * 将所提供的 {@code targetObject} 上具有给定 {@code name} 的{@linkplain Field 字段}设置为所提供的 {@code value}。
	 * <p>此方法委托给 {@link #setField(Object, String, Object, Class)}，并为 {@code type} 参数提供 {@code null}。
	 * @param targetObject 要在其上设置字段的目标对象；永不为 {@code null}
	 * @param name 要设置的字段的名称；永不为 {@code null}
	 * @param value 要设置的值
	 */
	public static void setField(Object targetObject, String name, @Nullable Object value) {
		setField(targetObject, name, value, null);
	}

	/**
	 * 将所提供的 {@code targetObject} 上具有给定 {@code name}/{@code type} 的{@linkplain Field 字段}设置为所提供的 {@code value}。
	 * <p>此方法委托给 {@link #setField(Object, Class, String, Object, Class)}，并为 {@code targetClass} 参数提供 {@code null}。
	 * @param targetObject 要在其上设置字段的目标对象；永不为 {@code null}
	 * @param name 要设置的字段的名称；如果指定了 {@code type}，
	 * 则可以为 {@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型；如果指定了 {@code name}，
	 * 则可以为 {@code null}
	 */
	public static void setField(Object targetObject, @Nullable String name, @Nullable Object value, @Nullable Class<?> type) {
		setField(targetObject, null, name, value, type);
	}

	/**
	 * 将所提供的 {@code targetClass} 上具有给定 {@code name} 的静态{@linkplain Field 字段}设置为所提供的 {@code value}。
	 * <p>此方法委托给 {@link #setField(Object, Class, String, Object, Class)}，并为 {@code targetObject} 和 {@code type} 参数提供 {@code null}。
	 * <p>此方法不支持设置 {@code static final} 字段。
	 * @param targetClass 要在其上设置静态字段的目标类；
	 * 永不为 {@code null}
	 * @param name 要设置的字段的名称；永不为 {@code null}
	 * @param value 要设置的值
	 * @since 4.2
	 */
	public static void setField(Class<?> targetClass, String name, @Nullable Object value) {
		setField(null, targetClass, name, value, null);
	}

	/**
	 * 将所提供的 {@code targetClass} 上具有给定 {@code name}/{@code type} 的静态{@linkplain Field 字段}设置为所提供的 {@code value}。
	 * <p>此方法委托给 {@link #setField(Object, Class, String, Object, Class)}，并为 {@code targetObject} 参数提供 {@code null}。
	 * <p>此方法不支持设置 {@code static final} 字段。
	 * @param targetClass 要在其上设置静态字段的目标类；
	 * 永不为 {@code null}
	 * @param name 要设置的字段的名称；如果指定了 {@code type}，
	 * 则可以为 {@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型；如果指定了 {@code name}，
	 * 则可以为 {@code null}
	 * @since 4.2
	 */
	public static void setField(
			Class<?> targetClass, @Nullable String name, @Nullable Object value, @Nullable Class<?> type) {

		setField(null, targetClass, name, value, type);
	}

	/**
	 * 将所提供的 {@code targetObject}/{@code targetClass} 上具有给定 {@code name}/{@code type} 的{@linkplain Field 字段}设置为所提供的 {@code value}。
	 * <p>如果所提供的 {@code targetObject} 是<em>代理</em>，则将其{@linkplain AopTestUtils#getUltimateTargetObject 解包}，从而允许在该代理的最终目标上设置字段。
	 * <p>此方法遍历类层次结构以搜索所需字段。此外，还会尝试使非 {@code public} 字段<em>可访问</em>，从而允许设置 {@code protected}、{@code private} 和<em>包级私有</em>字段。
	 * <p>此方法不支持设置 {@code static final} 字段。
	 * @param targetObject 要在其上设置字段的目标对象；如果字段是静态的，
	 * 则可以为 {@code null}
	 * @param targetClass 要在其上设置字段的目标类；如果字段是实例字段，
	 * 则可以为 {@code null}
	 * @param name 要设置的字段的名称；如果指定了 {@code type}，
	 * 则可以为 {@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型；如果指定了 {@code name}，
	 * 则可以为 {@code null}
	 * @since 4.2
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#setField(Field, Object, Object)
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	public static void setField(@Nullable Object targetObject, @Nullable Class<?> targetClass,
			@Nullable String name, @Nullable Object value, @Nullable Class<?> type) {

		Assert.isTrue(targetObject != null || targetClass != null,
				"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name, type);
		if (field == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find field '%s' of type [%s] on %s or target class [%s]", name, type,
					safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Setting field '%s' of type [%s] on %s or target class [%s] to value [%s]", name, type,
					safeToString(targetObject), targetClass, value));
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, targetObject, value);
	}

	/**
	 * 从所提供的 {@code targetObject} 中获取具有给定 {@code name} 的{@linkplain Field 字段}的值。
	 * <p>此方法委托给 {@link #getField(Object, Class, String)}，并为 {@code targetClass} 参数提供 {@code null}。
	 * @param targetObject 要从中获取字段的目标对象；
	 * 永不为 {@code null}
	 * @param name 要获取的字段的名称；永不为 {@code null}
	 * @return 字段的当前值
	 * @see #getField(Class, String)
	 */
	@Nullable
	public static Object getField(Object targetObject, String name) {
		return getField(targetObject, null, name);
	}

	/**
	 * 从所提供的 {@code targetClass} 中获取具有给定 {@code name} 的静态{@linkplain Field 字段}的值。
	 * <p>此方法委托给 {@link #getField(Object, Class, String)}，并为 {@code targetObject} 参数提供 {@code null}。
	 * @param targetClass 要从中获取静态字段的目标类；
	 * 永不为 {@code null}
	 * @param name 要获取的字段的名称；永不为 {@code null}
	 * @return 字段的当前值
	 * @since 4.2
	 * @see #getField(Object, String)
	 */
	@Nullable
	public static Object getField(Class<?> targetClass, String name) {
		return getField(null, targetClass, name);
	}

	/**
	 * 从所提供的 {@code targetObject}/{@code targetClass} 中获取具有给定 {@code name} 的{@linkplain Field 字段}的值。
	 * <p>如果所提供的 {@code targetObject} 是<em>代理</em>，则将其{@linkplain AopTestUtils#getUltimateTargetObject 解包}，从而允许从该代理的最终目标中获取字段。
	 * <p>此方法遍历类层次结构以搜索所需字段。此外，还会尝试使非 {@code public} 字段<em>可访问</em>，从而允许获取 {@code protected}、{@code private} 和<em>包级私有</em>字段。
	 * @param targetObject 要从中获取字段的目标对象；如果字段是静态的，
	 * 则可以为 {@code null}
	 * @param targetClass 要从中获取字段的目标类；如果字段是实例字段，
	 * 则可以为 {@code null}
	 * @param name 要获取的字段的名称；永不为 {@code null}
	 * @return 字段的当前值
	 * @since 4.2
	 * @see #getField(Object, String)
	 * @see #getField(Class, String)
	 * @see ReflectionUtils#findField(Class, String, Class)
	 * @see ReflectionUtils#makeAccessible(Field)
	 * @see ReflectionUtils#getField(Field, Object)
	 * @see AopTestUtils#getUltimateTargetObject(Object)
	 */
	@Nullable
	public static Object getField(@Nullable Object targetObject, @Nullable Class<?> targetClass, String name) {
		Assert.isTrue(targetObject != null || targetClass != null,
			"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name);
		if (field == null) {
			throw new IllegalArgumentException(String.format("Could not find field '%s' on %s or target class [%s]",
					name, safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Getting field '%s' from %s or target class [%s]", name,
					safeToString(targetObject), targetClass));
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, targetObject);
	}

	/**
	 * 使用所提供的 {@code value} 在所提供的目标对象上调用具有给定 {@code name} 的 setter 方法。
	 * <p>此方法遍历类层次结构以搜索所需方法。此外，还会尝试使非 {@code public} 方法<em>可访问</em>，从而允许调用 {@code protected}、{@code private} 和<em>包级私有</em> setter 方法。
	 * <p>此外，此方法支持 JavaBean 风格的<em>属性</em>名。例如，如果希望设置目标对象上的 {@code name} 属性，可以传入 &quot;name&quot; 或 &quot;setName&quot; 作为方法名。
	 * @param target 要在其上调用指定 setter 方法的目标对象
	 * @param name 要调用的 setter 方法的名称，或对应的属性名
	 * @param value 提供给 setter 方法的值
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	public static void invokeSetterMethod(Object target, String name, Object value) {
		invokeSetterMethod(target, name, value, null);
	}

	/**
	 * 使用所提供的 {@code value} 在所提供的目标对象上调用具有给定 {@code name} 和 {@code paramType} 的 setter 方法。
	 * <p>此方法遍历类层次结构以搜索所需方法。此外，还会尝试使非 {@code public} 方法<em>可访问</em>，从而允许调用 {@code protected}、{@code private} 和<em>包级私有</em> setter 方法。
	 * <p>此外，此方法支持 JavaBean 风格的<em>属性</em>名。例如，如果希望设置目标对象上的 {@code name} 属性，可以传入 &quot;name&quot; 或 &quot;setName&quot; 作为方法名。
	 * @param target 要在其上调用指定 setter 方法的目标对象
	 * @param name 要调用的 setter 方法的名称，或对应的属性名
	 * @param value 提供给 setter 方法的值
	 * @param type setter 方法声明的形参类型
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	public static void invokeSetterMethod(Object target, String name, @Nullable Object value, @Nullable Class<?> type) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");
		Class<?>[] paramTypes = (type != null ? new Class<?>[] {type} : null);

		String setterMethodName = name;
		if (!name.startsWith(SETTER_PREFIX)) {
			setterMethodName = SETTER_PREFIX + StringUtils.capitalize(name);
		}

		Method method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		if (method == null && !setterMethodName.equals(name)) {
			setterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find setter method '%s' on %s with parameter type [%s]", setterMethodName,
					safeToString(target), type));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking setter method '%s' on %s with value [%s]", setterMethodName,
					safeToString(target), value));
		}

		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, target, value);
	}

	/**
	 * 使用所提供的 {@code value} 在所提供的目标对象上调用具有给定 {@code name} 的 getter 方法。
	 * <p>此方法遍历类层次结构以搜索所需方法。此外，还会尝试使非 {@code public} 方法<em>可访问</em>，从而允许调用 {@code protected}、{@code private} 和<em>包级私有</em> getter 方法。
	 * <p>此外，此方法支持 JavaBean 风格的<em>属性</em>名。例如，如果希望获取目标对象上的 {@code name} 属性，可以传入 &quot;name&quot; 或 &quot;getName&quot; 作为方法名。
	 * @param target 要在其上调用指定 getter 方法的目标对象
	 * @param name 要调用的 getter 方法的名称，或对应的属性名
	 * @return 调用返回的值
	 * @see ReflectionUtils#findMethod(Class, String, Class[])
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 */
	@Nullable
	public static Object invokeGetterMethod(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");

		String getterMethodName = name;
		if (!name.startsWith(GETTER_PREFIX)) {
			getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
		}
		Method method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		if (method == null && !getterMethodName.equals(name)) {
			getterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find getter method '%s' on %s", getterMethodName, safeToString(target)));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking getter method '%s' on %s", getterMethodName, safeToString(target)));
		}
		ReflectionUtils.makeAccessible(method);
		return ReflectionUtils.invokeMethod(method, target);
	}

	/**
	 * 使用所提供的参数在所提供的目标对象上调用具有给定 {@code name} 的方法。
	 * <p>此方法委托给 {@link #invokeMethod(Object, Class, String, Object...)}，并为 {@code targetClass} 参数提供 {@code null}。
	 * @param target 要在其上调用指定方法的目标对象
	 * @param name 要调用的方法的名称
	 * @param args 提供给方法的参数
	 * @return 调用结果（如果有）
	 * @see #invokeMethod(Class, String, Object...)
	 * @see #invokeMethod(Object, Class, String, Object...)
	 * @see MethodInvoker
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 * @see ReflectionUtils#handleReflectionException(Exception)
	 */
	@Nullable
	public static <T> T invokeMethod(Object target, String name, Object... args) {
		Assert.notNull(target, "Target object must not be null");
		return invokeMethod(target, null, name, args);
	}

	/**
	 * 使用所提供的参数在所提供的目标类上调用具有给定 {@code name} 的静态方法。
	 * <p>此方法委托给 {@link #invokeMethod(Object, Class, String, Object...)}，并为 {@code targetObject} 参数提供 {@code null}。
	 * @param targetClass 要在其上调用指定方法的目标类
	 * @param name 要调用的方法的名称
	 * @param args 提供给方法的参数
	 * @return 调用结果（如果有）
	 * @since 5.2
	 * @see #invokeMethod(Object, String, Object...)
	 * @see #invokeMethod(Object, Class, String, Object...)
	 * @see MethodInvoker
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 * @see ReflectionUtils#handleReflectionException(Exception)
	 */
	@Nullable
	public static <T> T invokeMethod(Class<?> targetClass, String name, Object... args) {
		Assert.notNull(targetClass, "Target class must not be null");
		return invokeMethod(null, targetClass, name, args);
	}

	/**
	 * 使用所提供的参数在所提供的 {@code targetObject}/{@code targetClass} 上调用具有给定 {@code name} 的方法。
	 * <p>此方法遍历类层次结构以搜索所需方法。此外，还会尝试使非 {@code public} 方法<em>可访问</em>，从而允许调用 {@code protected}、{@code private} 和<em>包级私有</em>方法。
	 * @param targetObject 要在其上调用方法的目标对象；如果方法是静态的，
	 * 则可以为 {@code null}
	 * @param targetClass 要在其上调用方法的目标类；如果方法是实例方法，
	 * 则可以为 {@code null}
	 * @param name 要调用的方法的名称
	 * @param args 提供给方法的参数
	 * @return 调用结果（如果有）
	 * @since 5.2
	 * @see #invokeMethod(Object, String, Object...)
	 * @see #invokeMethod(Class, String, Object...)
	 * @see MethodInvoker
	 * @see ReflectionUtils#makeAccessible(Method)
	 * @see ReflectionUtils#invokeMethod(Method, Object, Object[])
	 * @see ReflectionUtils#handleReflectionException(Exception)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T invokeMethod(@Nullable Object targetObject, @Nullable Class<?> targetClass, String name,
			Object... args) {

		Assert.isTrue(targetObject != null || targetClass != null,
				"Either 'targetObject' or 'targetClass' for the method must be specified");
		Assert.hasText(name, "Method name must not be empty");

		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(targetObject);
			if (targetClass != null) {
				methodInvoker.setTargetClass(targetClass);
			}
			methodInvoker.setTargetMethod(name);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Invoking method '%s' on %s or %s with arguments %s", name,
						safeToString(targetObject), safeToString(targetClass), ObjectUtils.nullSafeToString(args)));
			}

			return (T) methodInvoker.invoke();
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new IllegalStateException("Should never get here");
		}
	}

	private static String safeToString(@Nullable Object target) {
		try {
			return String.format("target object [%s]", target);
		}
		catch (Exception ex) {
			return String.format("target of type [%s] whose toString() method threw [%s]",
					(target != null ? target.getClass().getName() : "unknown"), ex);
		}
	}

	private static String safeToString(@Nullable Class<?> clazz) {
		return String.format("target class [%s]", (clazz != null ? clazz.getName() : null));
	}

}
