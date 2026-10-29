/**
 * 用于将 JSR-303 Bean Validation 提供者（如 Hibernate Validator）集成到 Spring ApplicationContext
 * 中、特别是与 Spring 的数据绑定和校验 API 配合使用的支持类。
 *
 * <p>核心类是 {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}，它
 * 定义了共享的 ValidatorFactory/Validator 设置，以供其他 Spring 组件使用。
 */
@NonNullApi
@NonNullFields
package org.springframework.validation.beanvalidation;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
