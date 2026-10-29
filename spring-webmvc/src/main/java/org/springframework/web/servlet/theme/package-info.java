/**
 * Spring Web MVC 框架的主题支持类。
 * 提供标准的 ThemeResolver 实现，
 * 以及用于主题变更的 HandlerInterceptor。
 *
 * <p>
 * <ul>
 * <li>如果你未提供这些类之一的 bean 作为 {@code themeResolver}，
 * 则将提供一个 {@code FixedThemeResolver}，其默认主题名称为 'theme'。</li>
 * <li>如果你使用已定义的 {@code FixedThemeResolver}，则可以为默认值使用另一个主题
 * 名称，但用户将固定使用该主题。</li>
 * <li>使用 {@code CookieThemeResolver} 或 {@code SessionThemeResolver}，你可以允许
 * 用户更改其当前主题。</li>
 * <li>通常，你会将 CSS 文件、图片和 HTML 结构体的路径放入主题资源包中。</li>
 * <li>要获取主题数据，你可以在 JSP 中使用 spring:theme 标签，或通过
 * {@code RequestContext} 访问（适用于其他视图技术）。</li>
 * <li>{@code pagedlist} 示例应用使用了主题</li>
 * </ul>
 */
@NonNullApi
@NonNullFields
package org.springframework.web.servlet.theme;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
