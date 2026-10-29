/**
 * 通过 HTTP invoker 实现透明 Java 到 Java 远程调用的远程调用类。
 * 与 RMI 一样使用 Java 序列化，但提供了与 Caucho 基于 HTTP 的
 * Hessian 协议同样简单的配置方式。
 *
 * <p><b>HTTP invoker 是 Java 到 Java 远程调用的推荐协议。</b>
 * 它比 Hessian 更强大、更具扩展性，代价是与 Java 绑定。
 * 不过，它的配置与 Hessian 一样简单，这是它相对于 RMI 的主要优势。
 */
@NonNullApi
@NonNullFields
package org.springframework.remoting.httpinvoker;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
