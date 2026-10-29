/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.socket;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * WebSocket "close" 状态码及原因的描述。1xxx 范围内的状态码由协议预定义。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">
 *     RFC 6455, Section 7.4.1 "Defined Status Codes"</a>
 */
public final class CloseStatus {

	/**
	 * "1000 表示正常关闭，即建立连接的目的已经达成。"
	 */
	public static final CloseStatus NORMAL = new CloseStatus(1000);

	/**
	 * "1001 表示端点“正在离开”，例如服务器关闭或浏览器已导航离开某个页面。"
	 */
	public static final CloseStatus GOING_AWAY = new CloseStatus(1001);

	/**
	 * "1002 表示端点因协议错误而终止连接。"
	 */
	public static final CloseStatus PROTOCOL_ERROR  = new CloseStatus(1002);

	/**
	 * "1003 表示端点因接收到其无法接受的数据类型而终止连接（例如，仅理解文本数据的端点
	 * 在收到二进制消息时可能发送此状态码）。"
	 */
	public static final CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003);

	// 10004: Reserved.
	// The specific meaning might be defined in the future.

	/**
	 * "1005 是保留值，端点绝不能将其作为状态码设置在 Close 控制帧中。它指定用于
	 * 那些期望通过状态码表示实际上不存在状态码的应用程序。"
	 */
	public static final CloseStatus NO_STATUS_CODE = new CloseStatus(1005);

	/**
	 * "1006 是保留值，端点绝不能将其作为状态码设置在 Close 控制帧中。它指定用于
	 * 那些期望通过状态码表示连接异常关闭（例如未发送或接收 Close 控制帧）的
	 * 应用程序。"
	 */
	public static final CloseStatus NO_CLOSE_FRAME = new CloseStatus(1006);

	/**
	 * "1007 表示端点因收到的消息中的数据与消息类型不一致而终止连接（例如，文本消息中
	 * 出现非 UTF-8 [RFC3629] 数据）。"
	 */
	public static final CloseStatus BAD_DATA = new CloseStatus(1007);

	/**
	 * "1008 表示端点因收到的消息违反其策略而终止连接。这是一个通用状态码，在没有其他
	 * 更合适的状态码（例如 1003 或 1009）时，或者需要隐藏策略的具体细节时，可以返回
	 * 此状态码。"
	 */
	public static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008);

	/**
	 * "1009 表示端点因收到的消息过大而无法处理，从而终止连接。"
	 */
	public static final CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009);

	/**
	 * "1010 表示端点（客户端）因期望服务器协商一个或多个扩展，但服务器未在 WebSocket
	 * 握手的响应消息中返回这些扩展而终止连接。所需扩展的列表应出现在 Close 帧的
	 * /reason/ 部分。请注意，此状态码不由服务器使用，因为服务器可以改为让 WebSocket
	 * 握手失败。"
	 */
	public static final CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010);

	/**
	 * "1011 表示服务器因遇到阻止其完成请求的意外情况而终止连接。"
	 */
	public static final CloseStatus SERVER_ERROR = new CloseStatus(1011);

	/**
	 * "1012 表示服务正在重启。客户端可以重新连接，如果选择重新连接，应使用
	 * 5 - 30 秒之间的随机延迟。"
	 */
	public static final CloseStatus SERVICE_RESTARTED = new CloseStatus(1012);

	/**
	 * "1013 表示服务正在经历超载。客户端只应在用户操作时连接到不同的 IP（当目标有多个
	 * IP 时）或重新连接到同一 IP。"
	 */
	public static final CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013);

	/**
	 * "1015 是保留值，端点绝不能将其作为状态码设置在 Close 控制帧中。它指定用于
	 * 那些期望通过状态码表示连接因未能执行 TLS 握手（例如无法验证服务器证书）
	 * 而关闭的应用程序。"
	 */
	public static final CloseStatus TLS_HANDSHAKE_FAILURE = new CloseStatus(1015);


	private final int code;

	@Nullable
	private final String reason;


	/**
	 * 创建一个新的 {@link CloseStatus} 实例。
	 * @param code the status code
	 */
	public CloseStatus(int code) {
		this(code, null);
	}

	/**
	 * 创建一个新的 {@link CloseStatus} 实例。
	 * @param code the status code
	 * @param reason the reason
	 */
	public CloseStatus(int code, @Nullable String reason) {
		Assert.isTrue((code >= 1000 && code < 5000), () -> "Invalid status code: " + code);
		this.code = code;
		this.reason = reason;
	}


	/**
	 * 返回状态码。
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * 返回原因，如果没有则为 {@code null}。
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}

	/**
	 * 从此实例创建一个具有指定原因的新 {@link CloseStatus}。
	 * @param reason the reason
	 * @return a new {@link CloseStatus} instance
	 */
	public CloseStatus withReason(String reason) {
		Assert.hasText(reason, "Reason must not be empty");
		return new CloseStatus(this.code, reason);
	}

	/**
	 * @deprecated as of 5.3 in favor of comparing codes directly
	 */
	@Deprecated
	public boolean equalsCode(CloseStatus other) {
		return (this.code == other.code);
	}


	/**
	 * 返回给定状态码对应的常量；如果状态码不匹配或存在原因，则创建一个新实例。
	 * @since 5.3
	 */
	public static CloseStatus create(int code, @Nullable String reason) {
		if (!StringUtils.hasText(reason)) {
			switch (code) {
				case 1000:
					return NORMAL;
				case 1001:
					return GOING_AWAY;
				case 1002:
					return PROTOCOL_ERROR;
				case 1003:
					return NOT_ACCEPTABLE;
				case 1005:
					return NO_STATUS_CODE;
				case 1006:
					return NO_CLOSE_FRAME;
				case 1007:
					return BAD_DATA;
				case 1008:
					return POLICY_VIOLATION;
				case 1009:
					return TOO_BIG_TO_PROCESS;
				case 1010:
					return REQUIRED_EXTENSION;
				case 1011:
					return SERVER_ERROR;
				case 1012:
					return SERVICE_RESTARTED;
				case 1013:
					return SERVICE_OVERLOAD;
			}
		}
		return new CloseStatus(code, reason);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CloseStatus)) {
			return false;
		}
		CloseStatus otherStatus = (CloseStatus) other;
		return (this.code == otherStatus.code &&
				ObjectUtils.nullSafeEquals(this.reason, otherStatus.reason));
	}

	@Override
	public int hashCode() {
		return this.code * 29 + ObjectUtils.nullSafeHashCode(this.reason);
	}

	@Override
	public String toString() {
		return "CloseStatus[code=" + this.code + ", reason=" + this.reason + "]";
	}

}
