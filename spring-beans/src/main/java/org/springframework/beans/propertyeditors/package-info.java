/**
 * 属性编辑器，用于将 String 值转换为对象类型，例如 java.util.Properties。
 *
 * <p>其中一些编辑器由 BeanWrapperImpl 自动注册。"CustomXxxEditor" 类用于在特定的绑定过程中手动注册，
 * 因为它们是与区域设置相关的或类似的。
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.propertyeditors;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
