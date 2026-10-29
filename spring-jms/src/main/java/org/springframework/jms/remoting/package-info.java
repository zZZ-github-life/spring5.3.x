/**
 * 通过 JMS 提供者实现透明 Java 到 Java 远程调用的远程处理类。
 *
 * <p>允许目标服务在多个队列接收者之间进行负载均衡，
 * 并在客户端与服务之间提供一层间接性：
 * 它们只需就一个队列名称和一个服务接口达成一致。
 */
@NonNullApi
@NonNullFields
package org.springframework.jms.remoting;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
