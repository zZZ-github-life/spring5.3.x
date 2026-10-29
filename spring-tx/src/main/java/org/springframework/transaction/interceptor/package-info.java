/**
 * 基于AOP的声明式事务界定解决方案。
 * 构建在org.springframework.aop.framework的AOP基础设施之上。
 * 任何POJO都可以通过Spring进行事务通知。
 *
 * <p>TransactionFactoryProxyBean可用于透明地为使用它的代码创建事务性AOP代理。
 *
 * <p>TransactionInterceptor是基于Spring事务抽象提供事务通知的AOP Alliance MethodInterceptor。
 * 这允许在任何环境中进行声明式事务管理, 即使在没有JTA的情况下(如果应用只使用单个数据库)。
 */
@NonNullApi
@NonNullFields
package org.springframework.transaction.interceptor;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
