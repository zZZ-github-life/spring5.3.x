/**
 * 此包包含用于确定请求中所请求的媒体类型的类。
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationStrategy} 是确定所请求
 * {@linkplain org.springframework.http.MediaType 媒体类型}的主要抽象，其实现基于
 * {@linkplain org.springframework.web.accept.PathExtensionContentNegotiationStrategy 路径扩展名}、
 * {@linkplain org.springframework.web.accept.ParameterContentNegotiationStrategy 请求参数}、
 * {@linkplain org.springframework.web.accept.HeaderContentNegotiationStrategy 'Accept' 头}
 * 或 {@linkplain org.springframework.web.accept.FixedContentNegotiationStrategy 默认内容类型}。
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationManager} 用于按特定顺序
 * 委托给上述一个或多个策略。
 */
@NonNullApi
@NonNullFields
package org.springframework.web.accept;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
