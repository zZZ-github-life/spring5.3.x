/**
 * 消息监听器适配器机制，将消息委托给目标监听器方法，
 * 并将消息转换为适当类型的消息内容（例如 String 或字节数组）
 * 后传入监听器方法。
 */
@NonNullApi
@NonNullFields
package org.springframework.jms.listener.adapter;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
