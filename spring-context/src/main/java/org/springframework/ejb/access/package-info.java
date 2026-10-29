/**
 * 此包包含便于访问 EJB 的类。其基础是在 EJB 调用前后运行的 AOP 拦截器。具体而言，此包中的类允许通
 * 过本地接口透明地访问无状态会话 Bean（SLSB），从而避免使用它们的应用代码依赖 EJB 专有 API 和 JNDI
 * 查找，并可基于无需使用 EJB 即可实现的业务接口工作。这实现了客户端（如 Web 组件）与业务对象（可能
 * 是也可能不是 EJB）之间宝贵的解耦。这样我们就能够在不影响使用业务对象的代码的情况下，选择在应用中
 * 引入 EJB（或将 EJB 从应用中移除）。
 *
 * <p>此包中类的设计动机在第 11 章中讨论，参见 Rod Johnson 所著
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE
 * Design and Development</a>（Wrox，2002 年）。
 *
 * <p>不过，此包中类的实现和命名已发生变化。它现在使用 FactoryBeans 和 AOP，而不是 <i>Expert
 * One-on-One J2EE</i> 中描述的自定义 Bean 定义。
 */
@NonNullApi
@NonNullFields
package org.springframework.ejb.access;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
