/**
 * Spring 对 <a href="https://commons.apache.org/logging">Commons Logging API</a> 的变体实现：特别
 * 支持 Log4J 2、SLF4J 和 {@code java.util.logging}。
 *
 * <p>这是沿 {@code jcl-over-slf4j} 思路实现的自定义桥接器。如果你更倾向于硬绑定的 SLF4J 桥接器，可
 * 以排除 {@code spring-jcl} 并改用 {@code jcl-over-slf4j}。不过，在使用 Log4J 2 或
 * {@code java.util.logging} 时，Spring 自带的桥接器开箱即用体验更好，无需额外的桥接 jar；使用
 * Logback 配置 SLF4J 也更简单（无需排除 JCL，也无需 JCL 桥接器）。
 *
 * <p>{@link org.apache.commons.logging.Log} 与原版等价。但
 * {@link org.apache.commons.logging.LogFactory} 是一个完全不同的实现，它针对 Spring 的用途做了精简
 * 和优化，会检测框架类路径中的 Log4J 2.x 和 SLF4J 1.7，并在都不可用时回退到
 * {@code java.util.logging}。如果使用此实现遇到任何问题，可考虑排除 {@code spring-jcl}，改用标准的
 * {@code commons-logging} 构件或 {@code jcl-over-slf4j}。
 *
 * <p>请注意，此 Commons Logging 桥接器仅用于框架日志记录目的，包括核心框架和扩展。对于应用程序，建
 * 议直接使用 Log4J/SLF4J 或 {@code java.util.logging}。
 */
package org.apache.commons.logging;
