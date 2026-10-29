/**
 * 提供标准的 View 和 ViewResolver 实现，
 * 包括用于自定义实现的抽象基类。
 *
 * <p>应用开发者通常无需实现视图，
 * 因为框架已为 JSP、FreeMarker、
 * XSLT 等提供了标准视图。不过，如果应用有特殊的视图需求，
 * 通过继承此包中的 AbstractView 类来轻松实现自定义视图
 * 可能非常有帮助。
 */
@NonNullApi
@NonNullFields
package org.springframework.web.servlet.view;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
