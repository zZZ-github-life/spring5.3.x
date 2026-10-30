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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 提供基于某个源对象渲染一组 '{@code option}' 标签的辅助功能。
 * 该对象可以是数组、{@link Collection} 或 {@link Map}。
 * <h3>使用数组或 {@link Collection}：</h3>
 * <p>
 * 如果提供数组或 {@link Collection} 作为渲染内部 '{@code option}' 标签的源对象，
 * 你可以选择性地指定对象上与所渲染 '{@code option}' 的 <em>值</em> 对应的属性名
 * （即 {@code valueProperty}），以及与 <em>标签</em> 对应的属性名
 * （即 {@code labelProperty}）。随后在将数组/{@link Collection} 的每个元素
 * 渲染为 '{@code option}' 时会使用这些属性。如果省略了任一属性名，
 * 则改为使用对应数组/{@link Collection} 元素的 {@link Object#toString()} 值。
 * 不过，如果元素是枚举，则默认使用 {@link Enum#name()}。
 * </p>
 * <h3>使用 {@link Map}：</h3>
 * <p>
 * 你也可以选择通过提供 {@link Map} 作为源对象来渲染 '{@code option}' 标签。
 * </p>
 * <p>
 * 如果<strong>省略</strong> <em>值</em> 和 <em>标签</em> 的属性名：
 * </p>
 * <ul>
 * <li>每个 {@link Map} 条目的 {@code key} 将对应于所渲染 '{@code option}' 的
 * <em>值</em>，并且</li>
 * <li>每个 {@link Map} 条目的 {@code value} 将对应于所渲染 '{@code option}' 的
 * <em>标签</em>。</li>
 * </ul>
 * <p>
 * 如果<strong>提供</strong> <em>值</em> 和 <em>标签</em> 的属性名：
 * </p>
 * <ul>
 * <li>所渲染 '{@code option}' 的 <em>值</em> 将从每个 {@link Map} 条目的
 * {@code key} 所对应对象的 {@code valueProperty} 属性中获取，并且</li>
 * <li>所渲染 '{@code option}' 的 <em>标签</em> 将从每个 {@link Map} 条目的
 * {@code value} 所对应对象的 {@code labelProperty} 属性中获取。
 * </ul>
 * <h3>使用上述任一方式时：</h3>
 * <ul>
 * <li><em>值</em> 和 <em>标签</em> 的属性名作为参数传递给
 * {@link #OptionWriter(Object, BindStatus, String, String, boolean) 构造函数}。</li>
 * <li>如果 '{@code option}' 的键 {@link #isOptionSelected 匹配} 绑定到标签实例的值，
 * 则将该 '{@code option}' 标记为 'selected'。</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Scott Andrews
 * @since 2.0
 */
class OptionWriter {

	private final Object optionSource;

	private final BindStatus bindStatus;

	@Nullable
	private final String valueProperty;

	@Nullable
	private final String labelProperty;

	private final boolean htmlEscape;


	/**
	 * 为给定的 {@code objectSource} 创建一个新的 {@code OptionWriter}。
	 * @param optionSource {@code options} 的来源（永不为 {@code null}）
	 * @param bindStatus 绑定值的 {@link BindStatus}（永不为 {@code null}）
	 * @param valueProperty 用于渲染 {@code option} 值的属性名称（可选）
	 * @param labelProperty 用于渲染 {@code option} 标签的属性名称（可选）
	 */
	public OptionWriter(Object optionSource, BindStatus bindStatus,
			@Nullable String valueProperty, @Nullable String labelProperty, boolean htmlEscape) {

		Assert.notNull(optionSource, "'optionSource' must not be null");
		Assert.notNull(bindStatus, "'bindStatus' must not be null");
		this.optionSource = optionSource;
		this.bindStatus = bindStatus;
		this.valueProperty = valueProperty;
		this.labelProperty = labelProperty;
		this.htmlEscape = htmlEscape;
	}


	/**
	 * 将配置的 {@link #optionSource} 的 '{@code option}' 标签写入
	 * 给定的 {@link TagWriter}。
	 */
	public void writeOptions(TagWriter tagWriter) throws JspException {
		if (this.optionSource.getClass().isArray()) {
			renderFromArray(tagWriter);
		}
		else if (this.optionSource instanceof Collection) {
			renderFromCollection(tagWriter);
		}
		else if (this.optionSource instanceof Map) {
			renderFromMap(tagWriter);
		}
		else if (this.optionSource instanceof Class && ((Class<?>) this.optionSource).isEnum()) {
			renderFromEnum(tagWriter);
		}
		else {
			throw new JspException(
					"Type [" + this.optionSource.getClass().getName() + "] is not valid for option items");
		}
	}

	/**
	 * 使用 {@link #optionSource} 渲染内部的 '{@code option}' 标签。
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromArray(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(this.optionSource), tagWriter);
	}

	/**
	 * 使用给定的 {@link Map} 作为源渲染内部的 '{@code option}' 标签。
	 * @see #renderOption(TagWriter, Object, Object, Object)
	 */
	private void renderFromMap(TagWriter tagWriter) throws JspException {
		Map<?, ?> optionMap = (Map<?, ?>) this.optionSource;
		for (Map.Entry<?, ?> entry : optionMap.entrySet()) {
			Object mapKey = entry.getKey();
			Object mapValue = entry.getValue();
			Object renderValue = (this.valueProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapKey).getPropertyValue(this.valueProperty) :
					mapKey);
			Object renderLabel = (this.labelProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapValue).getPropertyValue(this.labelProperty) :
					mapValue);
			renderOption(tagWriter, mapKey, renderValue, renderLabel);
		}
	}

	/**
	 * 使用 {@link #optionSource} 渲染内部的 '{@code option}' 标签。
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromCollection(TagWriter tagWriter) throws JspException {
		doRenderFromCollection((Collection<?>) this.optionSource, tagWriter);
	}

	/**
	 * 使用 {@link #optionSource} 渲染内部的 '{@code option}' 标签。
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromEnum(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(((Class<?>) this.optionSource).getEnumConstants()), tagWriter);
	}

	/**
	 * 使用给定的对象 {@link Collection} 作为源渲染内部的 '{@code option}' 标签。
	 * 渲染 '{@code option}' 的 '{@code value}' 时使用 {@link #valueProperty} 字段的值，
	 * 渲染标签时使用 {@link #labelProperty} 属性的值。
	 */
	private void doRenderFromCollection(Collection<?> optionCollection, TagWriter tagWriter) throws JspException {
		for (Object item : optionCollection) {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
			Object value;
			if (this.valueProperty != null) {
				value = wrapper.getPropertyValue(this.valueProperty);
			}
			else if (item instanceof Enum) {
				value = ((Enum<?>) item).name();
			}
			else {
				value = item;
			}
			Object label = (this.labelProperty != null ? wrapper.getPropertyValue(this.labelProperty) : item);
			renderOption(tagWriter, item, value, label);
		}
	}

	/**
	 * 使用给定的值和标签渲染一个 HTML '{@code option}'。如果条目本身或其值
	 * 与绑定的值匹配，则将该值标记为 'selected'。
	 */
	private void renderOption(TagWriter tagWriter, Object item, @Nullable Object value, @Nullable Object label)
			throws JspException {

		tagWriter.startTag("option");
		writeCommonAttributes(tagWriter);

		String valueDisplayString = getDisplayString(value);
		String labelDisplayString = getDisplayString(label);

		valueDisplayString = processOptionValue(valueDisplayString);

		// allows render values to handle some strange browser compat issues.
		tagWriter.writeAttribute("value", valueDisplayString);

		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("selected", "selected");
		}
		if (isOptionDisabled()) {
			tagWriter.writeAttribute("disabled", "disabled");
		}
		tagWriter.appendValue(labelDisplayString);
		tagWriter.endTag();
	}

	/**
	 * 确定给定 {@code Object} 的显示值，并按需进行 HTML 转义。
	 */
	private String getDisplayString(@Nullable Object value) {
		PropertyEditor editor = (value != null ? this.bindStatus.findEditor(value.getClass()) : null);
		return ValueFormatter.getDisplayString(value, editor, this.htmlEscape);
	}

	/**
	 * 在写入之前处理 option 值。
	 * <p>默认实现只是原样返回相同的值。
	 */
	protected String processOptionValue(String resolvedValue) {
		return resolvedValue;
	}

	/**
	 * 确定给定值是否与选中的值匹配。
	 * <p>委托给 {@link SelectedValueComparator#isSelected}。
	 */
	private boolean isOptionSelected(@Nullable Object resolvedValue) {
		return SelectedValueComparator.isSelected(this.bindStatus, resolvedValue);
	}

	/**
	 * 确定 option 字段是否应被禁用。
	 */
	protected boolean isOptionDisabled() throws JspException {
		return false;
	}

	/**
	 * 将配置的默认属性写入给定的 {@link TagWriter}。
	 */
	protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
	}

}
