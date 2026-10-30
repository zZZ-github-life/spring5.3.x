/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用于从 Web 请求参数向 JavaBean 对象进行数据绑定的专用 {@link DataBinder}。
 * 为 Web 环境而设计，但不依赖于 Servlet API；作为更具体的 DataBinder 变体
 * （例如 {@link org.springframework.web.bind.ServletRequestDataBinder}）的基类。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，应仔细考虑数据绑定的设计与使用对安全性的影响。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和 <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>包含对字段标记（field marker）的支持，用于解决 HTML 复选框和选择项的一个常见问题：
 * 检测某个字段属于表单的一部分，但由于为空而未生成请求参数。
 * 字段标记允许检测该状态并相应地重置对应的 Bean 属性。
 * 对于其他情况下不存在的参数，默认值可以为字段指定一个非空的值。
 *
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Brian Clozel
 * @since 1.2
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @see #setFieldDefaultPrefix
 * @see ServletRequestDataBinder
 */
public class WebDataBinder extends DataBinder {

	/**
	 * 字段标记参数默认使用的前缀，其后跟字段名：例如字段
	 * "subscribeToNewsletter" 对应 "_subscribeToNewsletter"。
	 * <p>此类标记参数表示该字段是可见的，即存在于导致提交的表单中。
	 * 如果未找到对应的字段值参数，该字段将被重置。此时字段标记参数的值
	 * 无关紧要；可以使用任意值。这对 HTML 复选框和选择项特别有用。
	 * @see #setFieldMarkerPrefix
	 */
	public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";

	/**
	 * 字段默认值参数默认使用的前缀，其后跟字段名：例如字段
	 * "subscribeToNewsletter" 对应 "!subscribeToNewsletter"。
	 * <p>默认参数与字段标记的区别在于，它们提供的是默认值而不是空值。
	 * @see #setFieldDefaultPrefix
	 */
	public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";

	@Nullable
	private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;

	@Nullable
	private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;

	private boolean bindEmptyMultipartFiles = true;


	/**
	 * 创建一个新的 WebDataBinder 实例，使用默认对象名称。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的 WebDataBinder 实例。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @param objectName 目标对象的名称
	 */
	public WebDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 指定可用于标记潜在空字段的参数前缀，名称为 "prefix + field"。
	 * 此类标记参数按存在性进行检查：可以为其发送任意值，例如 "visible"。
	 * 这对 HTML 复选框和选择项特别有用。
	 * <p>默认值为 "_"，用于 "_FIELD" 参数（例如 "_subscribeToNewsletter"）。
	 * 如果希望完全关闭空字段检查，请将其设置为 null。
	 * <p>HTML 复选框只在被选中时发送值，因此无法检测以前选中的复选框是否刚刚
	 * 被取消选中，至少用标准 HTML 手段无法做到。
	 * <p>解决此问题的一种方式是：如果知道复选框在表单中可见，则查找复选框参数值，
	 * 若未找到值则重置该复选框。在 Spring Web MVC 中，这通常发生在自定义的
	 * {@code onBind} 实现中。
	 * <p>此自动重置机制解决了该缺陷，前提是为每个复选框字段发送一个标记参数，
	 * 例如为 "subscribeToNewsletter" 字段发送 "_subscribeToNewsletter"。
	 * 由于标记参数在任何情况下都会发送，数据绑定器可以检测到空字段
	 * 并自动重置其值。
	 * @see #DEFAULT_FIELD_MARKER_PREFIX
	 */
	public void setFieldMarkerPrefix(@Nullable String fieldMarkerPrefix) {
		this.fieldMarkerPrefix = fieldMarkerPrefix;
	}

	/**
	 * 返回用于标记潜在空字段的参数前缀。
	 */
	@Nullable
	public String getFieldMarkerPrefix() {
		return this.fieldMarkerPrefix;
	}

	/**
	 * 指定可用于指示默认值字段的参数前缀，名称为 "prefix + field"。
	 * 当未提供该字段时，将使用默认字段的值。
	 * <p>默认值为 "!"，用于 "!FIELD" 参数（例如 "!subscribeToNewsletter"）。
	 * 如果希望完全关闭字段默认值，请将其设置为 null。
	 * <p>HTML 复选框只在被选中时发送值，因此无法检测以前选中的复选框是否刚刚
	 * 被取消选中，至少用标准 HTML 手段无法做到。当复选框表示非布尔值时，
	 * 默认字段特别有用。
	 * <p>默认参数的存在会优先于给定字段的字段标记行为。
	 * @see #DEFAULT_FIELD_DEFAULT_PREFIX
	 */
	public void setFieldDefaultPrefix(@Nullable String fieldDefaultPrefix) {
		this.fieldDefaultPrefix = fieldDefaultPrefix;
	}

	/**
	 * 返回用于标记默认字段的参数前缀。
	 */
	@Nullable
	public String getFieldDefaultPrefix() {
		return this.fieldDefaultPrefix;
	}

	/**
	 * 设置是否绑定空的 MultipartFile 参数。默认为 "true"。
	 * <p>如果希望在用户重新提交表单而未选择不同文件时保留已绑定的
	 * MultipartFile，请关闭此选项。否则，已绑定的 MultipartFile 将被
	 * 空的 MultipartFile 持有者替换。
	 * @see org.springframework.web.multipart.MultipartFile
	 */
	public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
		this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
	}

	/**
	 * 返回是否绑定空的 MultipartFile 参数。
	 */
	public boolean isBindEmptyMultipartFiles() {
		return this.bindEmptyMultipartFiles;
	}


	/**
	 * 此实现在委托给父类绑定过程之前，执行字段默认值和字段标记检查。
	 * @see #checkFieldDefaults
	 * @see #checkFieldMarkers
	 */
	@Override
	protected void doBind(MutablePropertyValues mpvs) {
		checkFieldDefaults(mpvs);
		checkFieldMarkers(mpvs);
		adaptEmptyArrayIndices(mpvs);
		super.doBind(mpvs);
	}

	/**
	 * 检查给定的属性值中的字段默认值，即以字段默认值前缀开头的字段。
	 * <p>字段默认值的存在表明，如果该字段在其他情况下不存在，
	 * 则应使用指定的值。
	 * @param mpvs 要绑定的属性值（可修改）
	 * @see #getFieldDefaultPrefix
	 */
	protected void checkFieldDefaults(MutablePropertyValues mpvs) {
		String fieldDefaultPrefix = getFieldDefaultPrefix();
		if (fieldDefaultPrefix != null) {
			PropertyValue[] pvArray = mpvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getName().startsWith(fieldDefaultPrefix)) {
					String field = pv.getName().substring(fieldDefaultPrefix.length());
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						mpvs.add(field, pv.getValue());
					}
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 检查给定的属性值中的字段标记，即以字段标记前缀开头的字段。
	 * <p>字段标记的存在表明该字段存在于表单中。如果属性值中不包含
	 * 对应的字段值，则该字段将被视为空，并会被相应地重置。
	 * @param mpvs 要绑定的属性值（可修改）
	 * @see #getFieldMarkerPrefix
	 * @see #getEmptyValue(String, Class)
	 */
	protected void checkFieldMarkers(MutablePropertyValues mpvs) {
		String fieldMarkerPrefix = getFieldMarkerPrefix();
		if (fieldMarkerPrefix != null) {
			PropertyValue[] pvArray = mpvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getName().startsWith(fieldMarkerPrefix)) {
					String field = pv.getName().substring(fieldMarkerPrefix.length());
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						Class<?> fieldType = getPropertyAccessor().getPropertyType(field);
						mpvs.add(field, getEmptyValue(field, fieldType));
					}
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 检查名称以 {@code "[]"} 结尾的属性值。某些客户端使用这种数组语法而
	 * 不带显式索引值。如果发现此类值，则去掉方括号，以符合数据绑定所期望的
	 * 表达方式。
	 * @param mpvs 要绑定的属性值（可修改）
	 * @since 5.3
	 */
	protected void adaptEmptyArrayIndices(MutablePropertyValues mpvs) {
		for (PropertyValue pv : mpvs.getPropertyValues()) {
			String name = pv.getName();
			if (name.endsWith("[]")) {
				String field = name.substring(0, name.length() - 2);
				if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
					mpvs.add(field, pv.getValue());
				}
				mpvs.removePropertyValue(pv);
			}
		}
	}

	/**
	 * 确定指定字段的空值。
	 * <p>如果已知字段类型，默认实现委托给 {@link #getEmptyValue(Class)}，
	 * 否则回退为 {@code null}。
	 * @param field 字段的名称
	 * @param fieldType 字段的类型
	 * @return 空值（对于大多数字段为 {@code null}）
	 */
	@Nullable
	protected Object getEmptyValue(String field, @Nullable Class<?> fieldType) {
		return (fieldType != null ? getEmptyValue(fieldType) : null);
	}

	/**
	 * 确定指定字段的空值。
	 * <p>默认实现返回：
	 * <ul>
	 * <li>对于布尔字段，返回 {@code Boolean.FALSE}
	 * <li>对于数组类型，返回空数组
	 * <li>对于 Collection 类型，返回 Collection 实现
	 * <li>对于 Map 类型，返回 Map 实现
	 * <li>否则，使用 {@code null} 作为默认值
	 * </ul>
	 * @param fieldType 字段的类型
	 * @return 空值（对于大多数字段为 {@code null}）
	 * @since 5.0
	 */
	@Nullable
	public Object getEmptyValue(Class<?> fieldType) {
		try {
			if (boolean.class == fieldType || Boolean.class == fieldType) {
				// Special handling of boolean property.
				return Boolean.FALSE;
			}
			else if (fieldType.isArray()) {
				// Special handling of array property.
				return Array.newInstance(fieldType.getComponentType(), 0);
			}
			else if (Collection.class.isAssignableFrom(fieldType)) {
				return CollectionFactory.createCollection(fieldType, 0);
			}
			else if (Map.class.isAssignableFrom(fieldType)) {
				return CollectionFactory.createMap(fieldType, 0);
			}
		}
		catch (IllegalArgumentException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to create default value - falling back to null: " + ex.getMessage());
			}
		}
		// Default value: null.
		return null;
	}


	/**
	 * 绑定给定请求中包含的所有 multipart 文件（如果是 multipart 请求）。
	 * 由子类调用。
	 * <p>仅当 multipart 文件不为空，或者配置为也绑定空的 multipart 文件时，
	 * 才会将其添加到属性值中。
	 * @param multipartFiles 从字段名 String 到 MultipartFile 对象的 Map
	 * @param mpvs 要绑定的属性值（可修改）
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see #setBindEmptyMultipartFiles
	 */
	protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, MutablePropertyValues mpvs) {
		multipartFiles.forEach((key, values) -> {
			if (values.size() == 1) {
				MultipartFile value = values.get(0);
				if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
					mpvs.add(key, value);
				}
			}
			else {
				mpvs.add(key, values);
			}
		});
	}

}
