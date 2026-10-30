/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 此类可用于解析其他在 public static final 成员中包含常量定义的类。
 * 本类的 {@code asXXXX} 方法允许通过常量的字符串名称访问这些常量值。
 *
 * <p>假设类 Foo 包含 {@code public final static int CONSTANT1 = 66;}，
 * 那么包装 {@code Foo.class} 的此类实例在向 {@code asNumber} 方法传入参数
 * {@code "CONSTANT1"} 时，
 * 将返回常量值 66。
 *
 * <p>此类非常适合在 PropertyEditor 中使用，使它们能够识别与常量本身相同的名称，
 * 从而无需自行维护映射。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
public class Constants {

	/** 被内省类的名称。 */
	private final String className;

	/** 从 String 字段名到对象值的 Map。 */
	private final Map<String, Object> fieldCache = new HashMap<>();


	/**
	 * 创建包装给定类的新的 Constants 转换器类。
	 * <p>所有 <b>public</b> static final 变量都会被公开，无论其类型如何。
	 * @param clazz 要分析的类
	 * @throws IllegalArgumentException 如果提供的 {@code clazz} 为 {@code null}
	 */
	public Constants(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		this.className = clazz.getName();
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (ReflectionUtils.isPublicStaticFinal(field)) {
				String name = field.getName();
				try {
					Object value = field.get(null);
					this.fieldCache.put(name, value);
				}
				catch (IllegalAccessException ex) {
					// just leave this field and continue
				}
			}
		}
	}


	/**
	 * 返回被分析类的名称。
	 */
	public final String getClassName() {
		return this.className;
	}

	/**
	 * 返回公开的常量数量。
	 */
	public final int getSize() {
		return this.fieldCache.size();
	}

	/**
	 * 向子类公开字段缓存：从 String 字段名到对象值的 Map。
	 */
	protected final Map<String, Object> getFieldCache() {
		return this.fieldCache;
	}


	/**
	 * 返回转换为 Number 的常量值。
	 * @param code 字段的名称（永不为 {@code null}）
	 * @return Number 值
	 * @throws ConstantException 如果未找到字段名，或类型与 Number 不兼容
	 * @see #asObject
	 */
	public Number asNumber(String code) throws ConstantException {
		Object obj = asObject(code);
		if (!(obj instanceof Number)) {
			throw new ConstantException(this.className, code, "not a Number");
		}
		return (Number) obj;
	}

	/**
	 * 以 String 形式返回常量值。
	 * @param code 字段的名称（永不为 {@code null}）
	 * @return String 值。即使它不是字符串也能工作（会调用 {@code toString()}）。
	 * @throws ConstantException 如果未找到字段名
	 * @see #asObject
	 */
	public String asString(String code) throws ConstantException {
		return asObject(code).toString();
	}

	/**
	 * 解析给定的 String（接受大写或小写），如果它是我们正在分析的类中某个常量字段的名称，
	 * 则返回相应的值。
	 * @param code 字段的名称（永不为 {@code null}）
	 * @return Object 值
	 * @throws ConstantException 如果不存在该字段
	 */
	public Object asObject(String code) throws ConstantException {
		Assert.notNull(code, "Code must not be null");
		String codeToUse = code.toUpperCase(Locale.ENGLISH);
		Object val = this.fieldCache.get(codeToUse);
		if (val == null) {
			throw new ConstantException(this.className, codeToUse, "not found");
		}
		return val;
	}


	/**
	 * 返回给定常量组的所有名称。
	 * <p>请注意，此方法假定常量的命名遵循 Java 常量值的标准约定（即全部大写）。
	 * 在进入此方法的主要逻辑之前，
	 * 提供的 {@code namePrefix} 会以与区域设置无关的方式转换为大写。
	 * @param namePrefix 要搜索的常量名前缀（可以为 {@code null}）
	 * @return 常量名称集合
	 */
	public Set<String> getNames(@Nullable String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<>();
		for (String code : this.fieldCache.keySet()) {
			if (code.startsWith(prefixToUse)) {
				names.add(code);
			}
		}
		return names;
	}

	/**
	 * 返回给定 Bean 属性名对应常量组的所有名称。
	 * @param propertyName Bean 属性的名称
	 * @return 值的集合
	 * @see #propertyToConstantNamePrefix
	 */
	public Set<String> getNamesForProperty(String propertyName) {
		return getNames(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回给定常量组的所有名称。
	 * <p>请注意，此方法假定常量的命名遵循 Java 常量值的标准约定（即全部大写）。
	 * 在进入此方法的主要逻辑之前，
	 * 提供的 {@code nameSuffix} 会以与区域设置无关的方式转换为大写。
	 * @param nameSuffix 要搜索的常量名后缀（可以为 {@code null}）
	 * @return 常量名称集合
	 */
	public Set<String> getNamesForSuffix(@Nullable String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<>();
		for (String code : this.fieldCache.keySet()) {
			if (code.endsWith(suffixToUse)) {
				names.add(code);
			}
		}
		return names;
	}


	/**
	 * 返回给定常量组的所有值。
	 * <p>请注意，此方法假定常量的命名遵循 Java 常量值的标准约定（即全部大写）。
	 * 在进入此方法的主要逻辑之前，
	 * 提供的 {@code namePrefix} 会以与区域设置无关的方式转换为大写。
	 * @param namePrefix 要搜索的常量名前缀（可以为 {@code null}）
	 * @return 值的集合
	 */
	public Set<Object> getValues(@Nullable String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<>();
		this.fieldCache.forEach((code, value) -> {
			if (code.startsWith(prefixToUse)) {
				values.add(value);
			}
		});
		return values;
	}

	/**
	 * 返回给定 Bean 属性名对应常量组的所有值。
	 * @param propertyName Bean 属性的名称
	 * @return 值的集合
	 * @see #propertyToConstantNamePrefix
	 */
	public Set<Object> getValuesForProperty(String propertyName) {
		return getValues(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回给定常量组的所有值。
	 * <p>请注意，此方法假定常量的命名遵循 Java 常量值的标准约定（即全部大写）。
	 * 在进入此方法的主要逻辑之前，
	 * 提供的 {@code nameSuffix} 会以与区域设置无关的方式转换为大写。
	 * @param nameSuffix 要搜索的常量名后缀（可以为 {@code null}）
	 * @return 值的集合
	 */
	public Set<Object> getValuesForSuffix(@Nullable String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<>();
		this.fieldCache.forEach((code, value) -> {
			if (code.endsWith(suffixToUse)) {
				values.add(value);
			}
		});
		return values;
	}


	/**
	 * 在给定常量组中查找给定的值。
	 * <p>将返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param namePrefix 要搜索的常量名前缀（可以为 {@code null}）
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到该值
	 */
	public String toCode(Object value, @Nullable String namePrefix) throws ConstantException {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().startsWith(prefixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, prefixToUse, value);
	}

	/**
	 * 在给定 Bean 属性名对应的常量组中查找给定的值。将返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param propertyName Bean 属性的名称
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到该值
	 * @see #propertyToConstantNamePrefix
	 */
	public String toCodeForProperty(Object value, String propertyName) throws ConstantException {
		return toCode(value, propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 在给定常量组中查找给定的值。
	 * <p>将返回第一个匹配项。
	 * @param value 要查找的常量值
	 * @param nameSuffix 要搜索的常量名后缀（可以为 {@code null}）
	 * @return 常量字段的名称
	 * @throws ConstantException 如果未找到该值
	 */
	public String toCodeForSuffix(Object value, @Nullable String nameSuffix) throws ConstantException {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().endsWith(suffixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, suffixToUse, value);
	}


	/**
	 * 将给定的 Bean 属性名转换为常量名前缀。
	 * <p>使用一种常见的命名惯例：将所有小写字符转换为大写，并在大写字符前加上下划线。
	 * <p>示例："imageSize" &rarr; "IMAGE_SIZE"<br>
	 * 示例："imagesize" &rarr; "IMAGESIZE".<br>
	 * 示例："ImageSize" &rarr; "_IMAGE_SIZE".<br>
	 * 示例："IMAGESIZE" &rarr; "_I_M_A_G_E_S_I_Z_E"
	 * @param propertyName Bean 属性的名称
	 * @return 对应的常量名前缀
	 * @see #getValuesForProperty
	 * @see #toCodeForProperty
	 */
	public String propertyToConstantNamePrefix(String propertyName) {
		StringBuilder parsedPrefix = new StringBuilder();
		for (int i = 0; i < propertyName.length(); i++) {
			char c = propertyName.charAt(i);
			if (Character.isUpperCase(c)) {
				parsedPrefix.append('_');
				parsedPrefix.append(c);
			}
			else {
				parsedPrefix.append(Character.toUpperCase(c));
			}
		}
		return parsedPrefix.toString();
	}


	/**
	 * 当向 {@link Constants} 类请求无效的常量名时抛出的异常。
	 */
	@SuppressWarnings("serial")
	public static class ConstantException extends IllegalArgumentException {

		/**
		 * 当请求无效的常量名时抛出。
		 * @param className 包含常量定义的类的名称
		 * @param field 无效的常量名
		 * @param message 问题的描述
		 */
		public ConstantException(String className, String field, String message) {
			super("Field '" + field + "' " + message + " in class [" + className + "]");
		}

		/**
		 * 当查找无效的常量值时抛出。
		 * @param className 包含常量定义的类的名称
		 * @param namePrefix 所搜索常量名的前缀
		 * @param value 所查找的常量值
		 */
		public ConstantException(String className, String namePrefix, Object value) {
			super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
		}
	}

}
