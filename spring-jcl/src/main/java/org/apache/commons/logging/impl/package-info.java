/**
 * Spring 对 <a href="https://commons.apache.org/logging">Commons Logging API</a> 的变体实现：特别
 * 支持 Log4J 2、SLF4J 和 {@code java.util.logging}。
 *
 * <p>此 {@code impl} 包仅为兼容现有 Commons Logging 用法（例如 Commons Configuration 中的用法）而
 * 保留二进制兼容性。{@code NoOpLog} 可作为 {@code Log} 的回退实例，而 {@code SimpleLog} 并不打算真
 * 正工作（使用时会发出警告）。
 */
package org.apache.commons.logging.impl;
