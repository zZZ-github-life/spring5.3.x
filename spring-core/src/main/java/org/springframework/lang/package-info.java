/**
 * 具有语言级语义的常用注解：可空性以及 JDK API 指示。这些注解位于 Spring 包依赖安排中的最低层级，
 * 甚至低于 {@code org.springframework.util}，且不隐含任何 Spring 特有的概念。
 *
 * <p>在框架代码库中用于描述性目的。可由构建时工具（例如 FindBugs 或 Animal Sniffer）、替代 JVM 语
 * 言（例如 Kotlin）以及 IDE（例如 IntelliJ IDEA 或进行了相应项目配置的 Eclipse）进行校验。
 */
package org.springframework.lang;
