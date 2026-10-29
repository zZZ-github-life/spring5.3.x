/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jmx.export.notification;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import org.springframework.util.Assert;

/**
 * {@link NotificationPublisher} 实现，它使用 {@link ModelMBean} 接口提供的基础设施来跟踪
 * {@link javax.management.NotificationListener javax.management.NotificationListeners}，并将
 * {@link Notification Notifications} 发送给这些监听器。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see javax.management.modelmbean.ModelMBeanNotificationBroadcaster
 * @see NotificationPublisherAware
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

	/**
	 * 包装受管资源的 {@link ModelMBean} 实例，此 {@code NotificationPublisher} 将被注入其中。
	 */
	private final ModelMBeanNotificationBroadcaster modelMBean;

	/**
	 * 与 {@link ModelMBean modelMBean} 关联的 {@link ObjectName}。
	 */
	private final ObjectName objectName;

	/**
	 * 与 {@link ModelMBean modelMBean} 关联的受管资源。
	 */
	private final Object managedResource;


	/**
	 * 创建 {@link ModelMBeanNotificationPublisher} 类的新实例，它会将所有
	 * {@link javax.management.Notification Notifications} 发布到提供的 {@link ModelMBean}。
	 * @param modelMBean the target {@link ModelMBean}; must not be {@code null}
	 * @param objectName the {@link ObjectName} of the source {@link ModelMBean}
	 * @param managedResource the managed resource exposed by the supplied {@link ModelMBean}
	 * @throws IllegalArgumentException if any of the parameters is {@code null}
	 */
	public ModelMBeanNotificationPublisher(
			ModelMBeanNotificationBroadcaster modelMBean, ObjectName objectName, Object managedResource) {

		Assert.notNull(modelMBean, "'modelMBean' must not be null");
		Assert.notNull(objectName, "'objectName' must not be null");
		Assert.notNull(managedResource, "'managedResource' must not be null");
		this.modelMBean = modelMBean;
		this.objectName = objectName;
		this.managedResource = managedResource;
	}


	/**
	 * 使用包装的 {@link ModelMBean} 实例发送提供的 {@link Notification}。
	 * @param notification the {@link Notification} to be sent
	 * @throws IllegalArgumentException if the supplied {@code notification} is {@code null}
	 * @throws UnableToSendNotificationException if the supplied {@code notification} could not be sent
	 */
	@Override
	public void sendNotification(Notification notification) {
		Assert.notNull(notification, "Notification must not be null");
		replaceNotificationSourceIfNecessary(notification);
		try {
			if (notification instanceof AttributeChangeNotification) {
				this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
			}
			else {
				this.modelMBean.sendNotification(notification);
			}
		}
		catch (MBeanException ex) {
			throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
		}
	}

	/**
	 * 必要时替换通知源。摘自 {@link Notification javadoc}：<i>“强烈建议通知发送者使用对象名称而不是
	 * MBean 对象的引用作为源。”</i>
	 * @param notification the {@link Notification} whose
	 * {@link javax.management.Notification#getSource()} might need massaging
	 */
	private void replaceNotificationSourceIfNecessary(Notification notification) {
		if (notification.getSource() == null || notification.getSource().equals(this.managedResource)) {
			notification.setSource(this.objectName);
		}
	}

}
