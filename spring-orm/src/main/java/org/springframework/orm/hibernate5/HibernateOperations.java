/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.orm.hibernate5;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.criterion.DetachedCriteria;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 接口，指定了一组通用的 Hibernate 操作，以及用于基于 Session 的 lambda 表达式的
 * 通用 {@link #execute} 方法。
 * 由 {@link HibernateTemplate} 实现。虽然不常使用，但它是增强可测试性的有用选项，
 * 因为它很容易被 mock 或 stub。
 *
 * <p>定义了 {@code HibernateTemplate} 的数据访问方法，这些方法镜像了各种
 * {@link org.hibernate.Session} 方法。强烈建议用户阅读
 * Hibernate {@code Session} 的 javadoc，以了解这些方法的语义细节。
 *
 * <p><b>弃用说明：</b>虽然 {@link HibernateTemplate} 和此操作接口
 * 出于 Spring 应用程序中数据访问实现风格的向后兼容性而被保留，但我们强烈建议
 * 对于非平凡的交互使用原生 {@link org.hibernate.Session} 访问代码。
 * 这尤其影响参数化查询——在 Java 8+ 上，自定义的
 * {@link HibernateCallback} lambda 代码块配合 {@code createQuery} 以及在
 * {@link org.hibernate.query.Query} 接口上的多次 {@code setParameter} 调用
 * 是一种优雅的解决方案，可通过通用 {@link #execute} 方法执行。
 * 所有受益于 lambda 变体的此类操作都已在此接口上标记为
 * {@code deprecated}。
 *
 * <p><b>Hibernate 兼容性说明：</b>{@link HibernateTemplate} 及
 * 此接口上的操作通常旨在适用于所有 Hibernate
 * 版本。在二进制兼容性方面，Spring 为每个主要的
 * Hibernate 代次（目前为 Hibernate ORM 5.x）提供了变体。然而，由于
 * Hibernate ORM 5.3 中的重构和移除，某些变体——特别是
 * 从索引 0 开始的旧式位置参数——不再有效。
 * 所有受影响的操作都标记为已弃用；请将它们替换为
 * 通用 {@link #execute} 方法以及创建查询的自定义 lambda 代码块，
 * 最好通过 {@link org.hibernate.query.Query} 设置命名参数。
 * <b>请注意，已弃用的操作已知可配合 Hibernate
 * ORM 5.2 使用，但可能不再适用于 Hibernate ORM 5.3 及更高版本。</b>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateTemplate
 * @see org.hibernate.Session
 * @see HibernateTransactionManager
 */
public interface HibernateOperations {

	/**
	 * 在 {@link org.hibernate.Session} 中执行给定操作对象所指定的动作。
	 * <p>操作对象抛出的应用异常会传播给调用方（只能是未检查异常）。
	 * Hibernate 异常会被转换为相应的 DAO 异常。允许返回结果对象，
	 * 即一个领域对象或一组领域对象。
	 * <p>注意：回调代码不应自行处理事务！
	 * 请使用合适的事务管理器，如
	 * {@link HibernateTransactionManager}。通常，回调代码不得
	 * 触碰任何 {@code Session} 生命周期方法，如 close、
	 * disconnect 或 reconnect，以便让模板完成其工作。
	 * @param action 指定 Hibernate 操作的回调对象
	 * @return 由操作返回的结果对象，或 {@code null}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see HibernateTransactionManager
	 * @see org.hibernate.Session
	 */
	@Nullable
	<T> T execute(HibernateCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则返回 {@code null}。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#get(Class, Serializable)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @return 持久化实例；如果未找到则返回 {@code null}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#get(Class, Serializable)
	 */
	@Nullable
	<T> T get(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则返回 {@code null}。
	 * <p>如果实例存在，则获取指定的锁模式。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#get(Class, Serializable, LockMode)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁模式
	 * @return 持久化实例；如果未找到则返回 {@code null}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#get(Class, Serializable, LockMode)
	 */
	@Nullable
	<T> T get(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则返回 {@code null}。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#get(String, Serializable)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @return 持久化实例；如果未找到则返回 {@code null}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#get(Class, Serializable)
	 */
	@Nullable
	Object get(String entityName, Serializable id) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则返回 {@code null}。
	 * 如果实例存在，则获取指定的锁模式。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#get(String, Serializable, LockMode)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁模式
	 * @return 持久化实例；如果未找到则返回 {@code null}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#get(Class, Serializable, LockMode)
	 */
	@Nullable
	Object get(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则抛出异常。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#load(Class, Serializable)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则抛出异常。
	 * 如果实例存在，则获取指定的锁模式。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#load(Class, Serializable, LockMode)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁模式
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则抛出异常。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#load(String, Serializable)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	Object load(String entityName, Serializable id) throws DataAccessException;

	/**
	 * 返回给定实体类中具有给定标识符的持久化实例，
	 * 如果未找到则抛出异常。
	 * <p>如果实例存在，则获取指定的锁模式。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#load(String, Serializable, LockMode)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁模式
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	Object load(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回给定实体类的所有持久化实例。
	 * 注意：如需检索特定子集，请使用查询或条件（criteria）。
	 * @param entityClass 持久化类
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#createCriteria
	 */
	<T> List<T> loadAll(Class<T> entityClass) throws DataAccessException;

	/**
	 * 将具有给定标识符的持久化实例加载到给定对象中，
	 * 如果未找到则抛出异常。
	 * <p>为方便起见，此方法是对
	 * {@link org.hibernate.Session#load(Object, Serializable)} 的轻量封装。
	 * 有关此方法确切语义的说明，请首先参阅
	 * Hibernate API 文档。
	 * @param entity 要加载到的（目标类的）对象
	 * @param id 持久化实例的标识符
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#load(Object, Serializable)
	 */
	void load(Object entity, Serializable id) throws DataAccessException;

	/**
	 * 重新读取给定持久化实例的状态。
	 * @param entity 要重新读取的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#refresh(Object)
	 */
	void refresh(Object entity) throws DataAccessException;

	/**
	 * 重新读取给定持久化实例的状态。
	 * 获取该实例的指定锁模式。
	 * @param entity 要重新读取的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#refresh(Object, LockMode)
	 */
	void refresh(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 检查给定对象是否在 Session 缓存中。
	 * @param entity 要检查的持久化实例
	 * @return 给定对象是否在 Session 缓存中
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#contains
	 */
	boolean contains(Object entity) throws DataAccessException;

	/**
	 * 从 {@link org.hibernate.Session} 缓存中移除给定对象。
	 * @param entity 要逐出的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#evict
	 */
	void evict(Object entity) throws DataAccessException;

	/**
	 * 强制初始化 Hibernate 代理或持久化集合。
	 * @param proxy 持久化对象或持久化集合的代理
	 * @throws DataAccessException 如果无法初始化该代理，例如因为它未与活动的 Session 关联
	 * @see org.hibernate.Hibernate#initialize
	 */
	void initialize(Object proxy) throws DataAccessException;

	/**
	 * 返回给定过滤器名称对应的已启用的 Hibernate {@link Filter}。
	 * 返回的 {@code Filter} 实例可用于设置过滤器参数。
	 * @param filterName 过滤器名称
	 * @return 已启用的 Hibernate {@code Filter}（事先已启用或由本操作即时启用）
	 * @throws IllegalStateException 如果当前未在事务性 Session 中运行（此时该操作没有意义）
	 */
	Filter enableFilter(String filterName) throws IllegalStateException;


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	/**
	 * 对给定对象获取指定的锁级别，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entity 要锁定的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 */
	void lock(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 对给定对象获取指定的锁级别，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entityName 持久化实体的名称
	 * @param entity 要锁定的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#lock(String, Object, LockMode)
	 */
	void lock(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例。
	 * @param entity 要持久化的瞬态实例
	 * @return 生成的标识符
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#save(Object)
	 */
	Serializable save(Object entity) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例。
	 * @param entityName 持久化实体的名称
	 * @param entity 要持久化的瞬态实例
	 * @return 生成的标识符
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#save(String, Object)
	 */
	Serializable save(String entityName, Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例，
	 * 并将其与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * @param entity 要更新的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#update(Object)
	 */
	void update(Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例，
	 * 并将其与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * <p>如果实例存在，则获取指定的锁模式，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entity 要更新的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#update(Object)
	 */
	void update(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 更新给定的持久化实例，
	 * 并将其与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * @param entityName 持久化实体的名称
	 * @param entity 要更新的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#update(String, Object)
	 */
	void update(String entityName, Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例，
	 * 并将其与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * <p>如果实例存在，则获取指定的锁模式，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entityName 持久化实体的名称
	 * @param entity 要更新的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#update(String, Object)
	 */
	void update(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 保存或更新给定的持久化实例，
	 * 依据其 id（匹配配置的 "unsaved-value"？）。
	 * 将该实例与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * @param entity 要保存或更新的持久化实例（将被关联到 Hibernate {@code Session}）
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#saveOrUpdate(Object)
	 */
	void saveOrUpdate(Object entity) throws DataAccessException;

	/**
	 * 保存或更新给定的持久化实例，
	 * 依据其 id（匹配配置的 "unsaved-value"？）。
	 * 将该实例与当前的 Hibernate {@code Session} 关联。
	 * @param entityName 持久化实体的名称
	 * @param entity 要保存或更新的持久化实例（将被关联到 Hibernate {@code Session}）
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#saveOrUpdate(String, Object)
	 */
	void saveOrUpdate(String entityName, Object entity) throws DataAccessException;

	/**
	 * 按照给定的复制模式持久化给定游离实例的状态，
	 * 并复用当前的标识符值。
	 * @param entity 要复制的持久化对象
	 * @param replicationMode Hibernate 的 ReplicationMode
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#replicate(Object, ReplicationMode)
	 */
	void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * 按照给定的复制模式持久化给定游离实例的状态，
	 * 并复用当前的标识符值。
	 * @param entityName 持久化实体的名称
	 * @param entity 要复制的持久化对象
	 * @param replicationMode Hibernate 的 ReplicationMode
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#replicate(String, Object, ReplicationMode)
	 */
	void replicate(String entityName, Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例。遵循 JSR-220 语义。
	 * <p>类似于 {@code save}，将给定对象
	 * 与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * @param entity 待持久化的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#persist(Object)
	 * @see #save
	 */
	void persist(Object entity) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例。遵循 JSR-220 语义。
	 * <p>类似于 {@code save}，将给定对象
	 * 与当前的 Hibernate {@link org.hibernate.Session} 关联。
	 * @param entityName 持久化实体的名称
	 * @param entity 待持久化的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#persist(String, Object)
	 * @see #save
	 */
	void persist(String entityName, Object entity) throws DataAccessException;

	/**
	 * 将给定对象的状态复制到具有相同标识符的持久化对象上。遵循 JSR-220 语义。
	 * <p>类似于 {@code saveOrUpdate}，但从不将给定对象
	 * 与当前的 Hibernate Session 关联。如果是新实体，
	 * 其状态也会被复制过去。
	 * <p>请注意，{@code merge} <i>不会</i>更新
	 * 传入对象图中的标识符（与 TopLink 不同）！如果
	 * 你希望新分配的 id 也传输到原始对象图，请考虑
	 * 注册 Spring 的 {@code IdTransferringMergeEventListener}。
	 * @param entity 要与相应持久化实例合并的对象
	 * @return 更新后的、已注册的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#merge(Object)
	 * @see #saveOrUpdate
	 */
	<T> T merge(T entity) throws DataAccessException;

	/**
	 * 将给定对象的状态复制到具有相同标识符的持久化对象上。遵循 JSR-220 语义。
	 * <p>类似于 {@code saveOrUpdate}，但从不将给定对象
	 * 与当前的 Hibernate {@link org.hibernate.Session} 关联。如果是
	 * 新实体，其状态也会被复制过去。
	 * <p>请注意，{@code merge} <i>不会</i>更新
	 * 传入对象图中的标识符（与 TopLink 不同）！请考虑
	 * 注册 Spring 的 {@code IdTransferringMergeEventListener}，
	 * 如果你希望新分配的 id 也传输到
	 * 原始对象图。
	 * @param entityName 持久化实体的名称
	 * @param entity 要与相应持久化实例合并的对象
	 * @return 更新后的、已注册的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#merge(String, Object)
	 * @see #saveOrUpdate
	 */
	<T> T merge(String entityName, T entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例。
	 * @param entity 要删除的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(Object entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例。
	 * <p>如果实例存在，则获取指定的锁模式，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entity 要删除的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 删除给定的持久化实例。
	 * @param entityName 持久化实体的名称
	 * @param entity 要删除的持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(String entityName, Object entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例。
	 * <p>如果实例存在，则获取指定的锁模式，并隐式
	 * 检查相应的数据库记录是否仍然存在。
	 * @param entityName 持久化实体的名称
	 * @param entity 要删除的持久化实例
	 * @param lockMode 要获取的锁模式
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 删除所有给定的持久化实例。
	 * <p>它可以与任意 find 方法结合，用两行代码
	 * 按查询删除。
	 * @param entities 要删除的多个持久化实例
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#delete(Object)
	 */
	void deleteAll(Collection<?> entities) throws DataAccessException;

	/**
	 * 将所有挂起的保存、更新和删除刷新到数据库。
	 * <p>仅在有选择地执行急切刷新时调用，例如当
	 * JDBC 代码需要在同一事务中看到某些更改时。
	 * 否则，最好依赖事务完成时的
	 * 自动刷新。
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#flush
	 */
	void flush() throws DataAccessException;

	/**
	 * 从 {@link org.hibernate.Session} 缓存中移除所有对象，并
	 * 取消所有挂起的保存、更新和删除。
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#clear
	 */
	void clear() throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	/**
	 * 基于给定的 Hibernate 条件对象执行查询。
	 * @param criteria 分离的 Hibernate 条件对象。
	 * <b>注意：不要重用条件对象！由于 Hibernate 条件设施的设计欠佳，
	 * 每次执行都需要重新创建它们。</b>
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 */
	List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException;

	/**
	 * 基于给定的 Hibernate 条件对象执行查询。
	 * @param criteria 分离的 Hibernate 条件对象。
	 * <b>注意：不要重用条件对象！由于 Hibernate 条件设施的设计欠佳，
	 * 每次执行都需要重新创建它们。</b>
	 * @param firstResult 要检索的第一个结果对象的索引（从 0 开始编号）
	 * @param maxResults 要检索的结果对象的最大数量（或 &lt;=0 表示不限）
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	List<?> findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询。
	 * @param exampleEntity 所需实体的一个实例，用作“query-by-example”的示例
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	<T> List<T> findByExample(T exampleEntity) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询。
	 * @param entityName 持久化实体的名称
	 * @param exampleEntity 所需实体的一个实例，用作“query-by-example”的示例
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException;

	/**
	 * 基于一个给定的示例实体对象执行查询。
	 * @param exampleEntity 所需实体的一个实例，用作“query-by-example”的示例
	 * @param firstResult 要检索的第一个结果对象的索引（从 0 开始编号）
	 * @param maxResults 要检索的结果对象的最大数量（或 &lt;=0 表示不限）
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	<T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * 基于一个给定的示例实体对象执行查询。
	 * @param entityName 持久化实体的名称
	 * @param exampleEntity 所需实体的一个实例，用作“query-by-example”的示例
	 * @param firstResult 要检索的第一个结果对象的索引（从 0 开始编号）
	 * @param maxResults 要检索的结果对象的最大数量（或 &lt;=0 表示不限）
	 * @return 包含 0 个或多个持久化实例的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity, int firstResult, int maxResults)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	/**
	 * 执行 HQL 查询，将若干值绑定到查询字符串中的 "?" 参数。
	 * @param queryString 以 Hibernate 查询语言表示的查询
	 * @param values 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#createQuery
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> find(String queryString, Object... values) throws DataAccessException;

	/**
	 * 执行 HQL 查询，将一个值绑定到查询字符串中的 ":" 命名参数。
	 * @param queryString 以 Hibernate 查询语言表示的查询
	 * @param paramName 参数名称
	 * @param value 参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedParam(String queryString, String paramName, Object value) throws DataAccessException;

	/**
	 * 执行 HQL 查询，将若干值绑定到查询字符串中的 ":" 命名参数。
	 * @param queryString 以 Hibernate 查询语言表示的查询
	 * @param paramNames 各参数的名称
	 * @param values 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedParam(String queryString, String[] paramNames, Object[] values) throws DataAccessException;

	/**
	 * 执行 HQL 查询，将给定 bean 的属性绑定到查询字符串中的
	 * <i>命名</i>参数。
	 * @param queryString 以 Hibernate 查询语言表示的查询
	 * @param valueBean 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#createQuery
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByValueBean(String queryString, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	/**
	 * 执行命名查询，将若干值绑定到查询字符串中的 "?" 参数。
	 * <p>命名查询在 Hibernate 映射文件中定义。
	 * @param queryName 映射文件中 Hibernate 查询的名称
	 * @param values 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQuery(String queryName, Object... values) throws DataAccessException;

	/**
	 * 执行命名查询，将一个值绑定到查询字符串中的 ":" 命名参数。
	 * <p>命名查询在 Hibernate 映射文件中定义。
	 * @param queryName 映射文件中 Hibernate 查询的名称
	 * @param paramName 参数名称
	 * @param value 参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException;

	/**
	 * 执行命名查询，将若干值绑定到查询字符串中的 ":" 命名参数。
	 * <p>命名查询在 Hibernate 映射文件中定义。
	 * @param queryName 映射文件中 Hibernate 查询的名称
	 * @param paramNames 各参数的名称
	 * @param values 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndNamedParam(String queryName, String[] paramNames, Object[] values)
			throws DataAccessException;

	/**
	 * 执行命名查询，将给定 bean 的属性绑定到
	 * 查询字符串中的 ":" 命名参数。
	 * <p>命名查询在 Hibernate 映射文件中定义。
	 * @param queryName 映射文件中 Hibernate 查询的名称
	 * @param valueBean 各参数的值
	 * @return 包含查询执行结果的 {@link List}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	/**
	 * 对持久化实例执行查询，将若干值绑定到
	 * 查询字符串中的 "?" 参数。
	 * <p>以 {@link Iterator} 形式返回结果。返回的实体会
	 * 按需初始化。详情请参阅 Hibernate API 文档。
	 * @param queryString 以 Hibernate 查询语言表示的查询
	 * @param values 各参数的值
	 * @return 包含 0 个或多个持久化实例的 {@link Iterator}
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#iterate
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	Iterator<?> iterate(String queryString, Object... values) throws DataAccessException;

	/**
	 * 立即关闭由各种 {@code iterate(..)} 操作创建的 {@link Iterator}，
	 * 而不是等待会话关闭或断开连接。
	 * @param it 要关闭的 {@code Iterator}
	 * @throws DataAccessException 如果无法关闭该 {@code Iterator}
	 * @see org.hibernate.Hibernate#close
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	void closeIterator(Iterator<?> it) throws DataAccessException;

	/**
	 * 根据给定查询更新/删除所有对象，并将若干值
	 * 绑定到查询字符串中的 "?" 参数。
	 * @param queryString 以 Hibernate 查询语言表示的更新/删除查询
	 * @param values 各参数的值
	 * @return 更新/删除的实例数量
	 * @throws DataAccessException 如果发生 Hibernate 错误
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#executeUpdate
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	int bulkUpdate(String queryString, Object... values) throws DataAccessException;

}
