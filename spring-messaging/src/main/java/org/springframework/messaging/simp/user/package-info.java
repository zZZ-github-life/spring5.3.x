/**
 * 支持处理发送到“用户”目标（即对用户会话唯一的目标）的消息，
 * 主要工作是转换目标，然后将更新后的消息转发到代理。
 *
 * <p>还包括用于跟踪已连接用户会话的
 * {@link org.springframework.messaging.simp.user.SimpUserRegistry}。
 */
@NonNullApi
@NonNullFields
package org.springframework.messaging.simp.user;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
