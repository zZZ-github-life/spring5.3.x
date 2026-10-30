/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 一个 {@link ResourcePatternResolver} 实现，能够将指定的资源位置路径解析为一个或多个匹配的资源。
 * 源路径可以是与目标 {@link org.springframework.core.io.Resource} 一一对应的简单路径，也可以包含特
 * 殊的 "{@code classpath*:}" 前缀和/或内部 Ant 风格的正则表达式（使用 Spring 的
 * {@link org.springframework.util.AntPathMatcher} 工具进行匹配）。后两者实际上都是通配符。
 *
 * <p><b>无通配符：</b>
 *
 * <p>在简单情况下，如果指定的位置路径不以 "{@code "classpath*:}" 前缀开头，且不包含 PathMatcher 模
 * 式，则此解析器将通过对底层 {@code ResourceLoader} 调用 {@code getResource()} 简单地返回单个资源。
 * 示例包括真实 URL（例如 "{@code file:C:/context.xml}"）、伪 URL（例如 "
 * {@code classpath:/context.xml}"）以及不带前缀的简单路径（例如 "{@code /WEB-INF/context.xml}"）。
 * 后者将以特定于底层 {@code ResourceLoader} 的方式解析（例如，对于 {@code WebApplicationContext}
 * 使用 {@code ServletContextResource}）。
 *
 * <p><b>Ant 风格模式：</b>
 *
 * <p>当路径位置包含 Ant 风格模式时，例如：
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/mycompany/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/mycompany/**&#47;applicationContext.xml</pre>
 * 解析器会遵循一个更复杂但已定义的过程来尝试解析该通配符。它为直到最后一个非通配符段的路径生成一个
 * {@code Resource}，并从中获取一个 {@code URL}。如果此 URL 不是 "{@code jar:}" URL 或容器特有的变
 * 体（例如 WebLogic 中的 "{@code zip:}"、WebSphere 中的 "{@code wsjar}" 等），则从中获取一个
 * {@code java.io.File}，并通过遍历文件系统来解析通配符。对于 jar URL，解析器要么从中获取
 * {@code java.net.JarURLConnection}，要么手动解析 jar URL，然后遍历 jar 文件的内容来解析通配符。
 *
 * <p><b>对可移植性的影响：</b>
 *
 * <p>如果指定的路径已经是文件 URL（无论是显式的，还是因为基础 {@code ResourceLoader} 是文件系统实
 * 现而隐式的），那么通配符匹配可以保证以完全可移植的方式工作。
 *
 * <p>如果指定的路径是类路径位置，则解析器必须通过 {@code Classloader.getResource()} 调用获取最后一
 * 个非通配符路径段的 URL。由于这只是路径的一个节点（而不是末端的文件），因此（在 ClassLoader
 * Javadoc 中）并未确切定义这种情况下返回的是何种 URL。在实践中，当类路径资源解析到文件系统位置时，
 * 通常是表示该目录的 {@code java.io.File}；当类路径资源解析到 jar 位置时，通常是某种 jar URL。尽管
 * 如此，此操作仍存在可移植性方面的顾虑。
 *
 * <p>如果为最后一个非通配符段获取到的是 jar URL，则解析器必须能够从中获取
 * {@code java.net.JarURLConnection}，或手动解析该 jar URL，才能遍历 jar 的内容并解析通配符。这在大
 * 多数环境中可以工作，但在其他环境中会失败，强烈建议在依赖它之前，在您的特定环境中对来自 jar 的资
 * 源的通配符解析进行充分测试。
 *
 * <p><b>{@code classpath*:} 前缀：</b>
 *
 * <p>通过 "{@code classpath*:}" 前缀，对检索多个同名类路径资源提供了特殊支持。例如，"
 * {@code classpath*:META-INF/beans.xml}" 将查找类路径中所有的 "beans.xml" 文件，无论是在 "classes"
 * 目录中还是在 JAR 文件中。这对于在每个 jar 文件的同一位置自动检测同名配置文件特别有用。在内部，这
 * 是通过 {@code ClassLoader.getResources()} 调用实现的，并且完全可移植。
 *
 * <p>"classpath*:" 前缀也可以与位置路径其余部分的 PathMatcher 模式组合使用，例如
 * "classpath*:META-INF/*-beans.xml"。在这种情况下，解析策略相当简单：对最后一个非通配符路径段使用
 * {@code ClassLoader.getResources()} 调用，以获取类加载器层次结构中所有匹配的资源，然后对每个资源，
 * 针对通配符子路径使用上述相同的 PathMatcher 解析策略。
 *
 * <p><b>其他说明：</b>
 *
 * <p><b>警告：</b>请注意，"{@code classpath*:}" 与 Ant 风格模式组合使用时，只有在模式开始之前至少
 * 有一个根目录时才能可靠工作，除非实际的目标文件位于文件系统中。这意味着像 "
 * {@code classpath*:*.xml}" 这样的模式<i>不会</i>从 jar 文件的根目录检索文件，而只会从展开目录的根
 * 目录检索文件。这源于 JDK 的 {@code ClassLoader.getResources()} 方法的限制，该方法对于传入的空
 * String（表示可能要搜索的根）只返回文件系统位置。此 {@code ResourcePatternResolver} 实现试图通过
 * {@link URLClassLoader} 内省和 "java.class.path" 清单评估来缓解 jar 根查找的限制；但这没有可移植
 * 性保证。
 *
 * <p><b>警告：</b>带有 "classpath:" 资源的 Ant 风格模式，如果要搜索的根包在多个类路径位置可用，则
 * 不保证能找到匹配的资源。这是因为像这样的资源
 * <pre class="code">
 *     com/mycompany/package1/service-context.xml
 * </pre>
 * 可能只存在于一个位置，但当使用像这样的路径
 * <pre class="code">
 *     classpath:com/mycompany/**&#47;service-context.xml
 * </pre>
 * 来尝试解析它时，解析器会基于 {@code getResource("com/mycompany");} 返回的（第一个）URL 进行工作。
 * 如果此基础包节点存在于多个类加载器位置，则实际的最终资源可能并不在其下。因此，在这种情况下，最
 * 好使用 "{@code classpath*:}" 配合相同的 Ant 风格模式，这将搜索包含根包的<i>所有</i>类路径位置。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Marius Bogoevici
 * @author Costin Leau
 * @author Phillip Webb
 * @since 1.0.2
 * @see #CLASSPATH_ALL_URL_PREFIX
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.core.io.ResourceLoader#getResource(String)
 * @see ClassLoader#getResources(String)
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

	@Nullable
	private static Method equinoxResolveMethod;

	static {
		try {
			// Detect Equinox OSGi (e.g. on WebSphere 6.1)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
		}
		catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}


	private final ResourceLoader resourceLoader;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * 使用 DefaultResourceLoader 创建一个新的 PathMatchingResourcePatternResolver。
	 * <p>ClassLoader 访问将通过线程上下文类加载器进行。
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * 创建一个新的 PathMatchingResourcePatternResolver。
	 * <p>ClassLoader 访问将通过线程上下文类加载器进行。
	 * @param resourceLoader 用于加载根目录和
	 * 实际资源的 ResourceLoader
	 */
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 使用 DefaultResourceLoader 创建一个新的 PathMatchingResourcePatternResolver。
	 * @param classLoader 用于加载类路径资源的 ClassLoader；
	 * 若为 {@code null}，则在实际访问资源时
	 * 使用线程上下文类加载器
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}


	/**
	 * 返回此模式解析器所使用的 ResourceLoader。
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * 设置用于此资源模式解析器的 PathMatcher 实现。默认为 AntPathMatcher。
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回此资源模式解析器使用的 PathMatcher。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	public Resource getResource(String location) {
		return getResourceLoader().getResource(location);
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
				// a class path resource pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// all class path resources with the given name
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
			}
		}
		else {
			// Generally only look for a pattern after a prefix here,
			// and on Tomcat only after the "*/" separator for its "war:" protocol.
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}

	/**
	 * 通过 ClassLoader 查找具有给定位置的所有类位置资源。委托给
	 * {@link #doFindAllClassPathResources(String)}。
	 * @param location 类路径内的绝对路径
	 * @return 作为 Resource 数组的结果
	 * @throws IOException 在出现 I/O 错误时
	 * @see java.lang.ClassLoader#getResources
	 * @see #convertClassLoaderURL
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = location;
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved classpath location [" + location + "] to resources " + result);
		}
		return result.toArray(new Resource[0]);
	}

	/**
	 * 通过 ClassLoader 查找具有给定路径的所有类位置资源。由 {@link #findAllClassPathResources(String)}
	 * 调用。
	 * @param path 类路径内的绝对路径（不含前导斜杠）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @since 4.1.1
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(16);
		ClassLoader cl = getClassLoader();
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		if (!StringUtils.hasLength(path)) {
			// The above result is likely to be incomplete, i.e. only containing file system references.
			// We need to have pointers to each of the jar files on the classpath as well...
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * 将从 ClassLoader 返回的给定 URL 转换为 {@link Resource}。
	 * <p>默认实现只是创建一个 {@link UrlResource} 实例。
	 * @param url 由 ClassLoader 返回的 URL
	 * @return 对应的 Resource 对象
	 * @see java.lang.ClassLoader#getResources
	 * @see org.springframework.core.io.Resource
	 */
	protected Resource convertClassLoaderURL(URL url) {
		return new UrlResource(url);
	}

	/**
	 * 搜索所有 {@link URLClassLoader} URL 中的 jar 文件引用，并以指向 jar 文件内容根部的指针形式将它们
	 * 添加到给定的资源集合中。
	 * @param classLoader 要搜索的 ClassLoader（包括其祖先）
	 * @param result 用于添加 jar 根的资源集合
	 * @since 4.1.1
	 */
	protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, Set<Resource> result) {
		if (classLoader instanceof URLClassLoader) {
			try {
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					try {
						UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
								new UrlResource(url) :
								new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					}
					catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}

		if (classLoader == ClassLoader.getSystemClassLoader()) {
			// "java.class.path" manifest evaluation...
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				// Hierarchy traversal...
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}

	/**
	 * 从 "java.class.path." 清单属性中确定 jar 文件引用，并以指向 jar 文件内容根部的指针形式将它们添加
	 * 到给定的资源集合中。
	 * @param result 用于添加 jar 根的资源集合
	 * @since 4.3
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		try {
			String javaClassPathProperty = System.getProperty("java.class.path");
			for (String path : StringUtils.delimitedListToStringArray(
					javaClassPathProperty, System.getProperty("path.separator"))) {
				try {
					String filePath = new File(path).getAbsolutePath();
					int prefixIndex = filePath.indexOf(':');
					if (prefixIndex == 1) {
						// Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
						filePath = StringUtils.capitalize(filePath);
					}
					// # can appear in directories/filenames, java.net.URL should not treat it as a fragment
					filePath = StringUtils.replace(filePath, "#", "%23");
					// Build URL that points to the root of the jar file
					UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
							ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
					// Potentially overlapping with URLClassLoader.getURLs() result above!
					if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
						result.add(jarResource);
					}
				}
				catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
		}
	}

	/**
	 * 检查给定的文件路径在现有结果中是否已有重复但结构不同的条目，即有或没有前导斜杠。
	 * @param filePath 文件路径（可带或不带前导斜杠）
	 * @param result 当前结果
	 * @return 如果存在重复项（即忽略给定的文件路径）则返回 {@code true}，
	 * 如果继续将对应的资源添加到当前结果则返回 {@code false}
	 */
	private boolean hasDuplicate(String filePath, Set<Resource> result) {
		if (result.isEmpty()) {
			return false;
		}
		String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
		try {
			return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
					duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
		}
		catch (MalformedURLException ex) {
			// Ignore: just for testing against duplicate.
			return false;
		}
	}

	/**
	 * 通过 Ant 风格 PathMatcher 查找与给定位置模式匹配的所有资源。支持 jar 文件、zip 文件以及文件系统
	 * 中的资源。
	 * @param locationPattern 要匹配的位置模式
	 * @return 作为 Resource 数组的结果
	 * @throws IOException 在出现 I/O 错误时
	 * @see #doFindPathMatchingJarResources
	 * @see #doFindPathMatchingFileResources
	 * @see org.springframework.util.PathMatcher
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		String subPattern = locationPattern.substring(rootDirPath.length());
		Resource[] rootDirResources = getResources(rootDirPath);
		Set<Resource> result = new LinkedHashSet<>(16);
		for (Resource rootDirResource : rootDirResources) {
			rootDirResource = resolveRootDirResource(rootDirResource);
			URL rootDirUrl = rootDirResource.getURL();
			if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
				URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
				if (resolvedUrl != null) {
					rootDirUrl = resolvedUrl;
				}
				rootDirResource = new UrlResource(rootDirUrl);
			}
			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
			}
			else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
			}
			else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return result.toArray(new Resource[0]);
	}

	/**
	 * 确定给定位置的根目录。
	 * <p>用于确定文件匹配的起点，将根目录位置解析为 {@code java.io.File} 并传入
	 * {@code retrieveMatchingFiles}，其余部分作为模式。
	 * <p>例如，对于模式 "/WEB-INF/*.xml"，将返回 "/WEB-INF/"。
	 * @param location 要检查的位置
	 * @return 位置中表示根目录的部分
	 * @see #retrieveMatchingFiles
	 */
	protected String determineRootDir(String location) {
		int prefixEnd = location.indexOf(':') + 1;
		int rootDirEnd = location.length();
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		return location.substring(0, rootDirEnd);
	}

	/**
	 * 解析指定资源以进行路径匹配。
	 * <p>默认情况下，Equinox OSGi 的 "bundleresource:" / "bundleentry:" URL 将被解析为标准的 jar 文件
	 * URL，以便使用 Spring 的标准 jar 文件遍历算法进行遍历。对于任何前置的自定义解析，请重写此方法并相
	 * 应地替换资源句柄。
	 * @param original 要解析的资源
	 * @return 解析后的资源（可能与传入的资源相同）
	 * @throws IOException 在解析失败时
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * 返回给定资源句柄是否表示 {@code doFindPathMatchingJarResources} 方法可以处理的 jar 资源。
	 * <p>默认情况下，URL 协议 "jar"、"zip"、"vfszip" 和 "wsjar" 将被视为 jar 资源。此模板方法允许检测
	 * 更多种类的类 jar 资源，例如通过对资源句柄类型进行 {@code instanceof} 检查。
	 * @param resource 要检查的资源句柄
	 * （通常是要开始路径匹配的根目录）
	 * @see #doFindPathMatchingJarResources
	 * @see org.springframework.util.ResourceUtils#isJarURL
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * 通过 Ant 风格 PathMatcher 查找 jar 文件中与给定位置模式匹配的所有资源。
	 * @param rootDirResource 作为 Resource 的根目录
	 * @param rootDirURL 预先解析的根目录 URL
	 * @param subPattern 要匹配的子模式（位于根目录之下）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 在出现 I/O 错误时
	 * @since 4.3
	 * @see java.net.JarURLConnection
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirURL, String subPattern)
			throws IOException {

		URLConnection con = rootDirURL.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection) {
			// Should usually be the case for traditional JAR files.
			JarURLConnection jarCon = (JarURLConnection) con;
			ResourceUtils.useCachesIfNecessary(jarCon);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		}
		else {
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			String urlFile = rootDirURL.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
				if (separatorIndex == -1) {
					separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				}
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// Root entry path must end with slash to allow for proper matching.
				// The Sun JRE does not return a slash here, but BEA JRockit does.
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(8);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * 将给定的 jar 文件 URL 解析为 JarFile 对象。
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * 通过 Ant 风格 PathMatcher 查找文件系统中与给定位置模式匹配的所有资源。
	 * @param rootDirResource 作为 Resource 的根目录
	 * @param subPattern 要匹配的子模式（位于根目录之下）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 在出现 I/O 错误时
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		File rootDir;
		try {
			rootDir = rootDirResource.getFile().getAbsoluteFile();
		}
		catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot search for matching files underneath " + rootDirResource +
						" in the file system: " + ex.getMessage());
			}
			return Collections.emptySet();
		}
		catch (Exception ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
			}
			return Collections.emptySet();
		}
		return doFindMatchingFileSystemResources(rootDir, subPattern);
	}

	/**
	 * 通过 Ant 风格 PathMatcher 查找文件系统中与给定位置模式匹配的所有资源。
	 * @param rootDir 文件系统中的根目录
	 * @param subPattern 要匹配的子模式（位于根目录之下）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 在出现 I/O 错误时
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
		}
		Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
		Set<Resource> result = new LinkedHashSet<>(matchingFiles.size());
		for (File file : matchingFiles) {
			result.add(new FileSystemResource(file));
		}
		return result;
	}

	/**
	 * 检索与给定路径模式匹配的文件，检查给定目录及其子目录。
	 * @param rootDir 起始目录
	 * @param pattern 要匹配的模式，相对于根目录
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 如果无法获取目录内容
	 */
	protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
		if (!rootDir.exists()) {
			// Silently skip non-existing directories.
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
			}
			return Collections.emptySet();
		}
		if (!rootDir.isDirectory()) {
			// Complain louder if it exists but is no directory.
			if (logger.isInfoEnabled()) {
				logger.info("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
			}
			return Collections.emptySet();
		}
		if (!rootDir.canRead()) {
			if (logger.isInfoEnabled()) {
				logger.info("Skipping search for matching files underneath directory [" + rootDir.getAbsolutePath() +
						"] because the application is not allowed to read the directory");
			}
			return Collections.emptySet();
		}
		String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
		if (!pattern.startsWith("/")) {
			fullPattern += "/";
		}
		fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
		Set<File> result = new LinkedHashSet<>(8);
		doRetrieveMatchingFiles(fullPattern, rootDir, result);
		return result;
	}

	/**
	 * 递归检索与给定模式匹配的文件，并将其添加到给定的结果列表中。
	 * @param fullPattern 要匹配的模式，前面附加根目录路径
	 * @param dir 当前目录
	 * @param result 要添加到的匹配 File 实例的 Set
	 * @throws IOException 如果无法获取目录内容
	 */
	protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Searching directory [" + dir.getAbsolutePath() +
					"] for files matching pattern [" + fullPattern + "]");
		}
		for (File content : listDirectory(dir)) {
			String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
			if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
				if (!content.canRead()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
								"] because the application is not allowed to read the directory");
					}
				}
				else {
					doRetrieveMatchingFiles(fullPattern, content, result);
				}
			}
			if (getPathMatcher().match(fullPattern, currPath)) {
				result.add(content);
			}
		}
	}

	/**
	 * 确定给定目录中文件的排序列表。
	 * @param dir 要检查的目录
	 * @return 排序后的文件列表（默认按字母顺序）
	 * @since 5.1
	 * @see File#listFiles()
	 */
	protected File[] listDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
			}
			return new File[0];
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		return files;
	}


	/**
	 * 内部委托类，避免在运行时对 JBoss VFS API 产生硬依赖。
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirURL, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirURL);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * 用于路径匹配目的的 VFS 访问器。
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<>();

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				if (methodName.equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				else if (methodName.equals("hashCode")) {
					return System.identityHashCode(proxy);
				}
			}
			else if ("getAttributes".equals(methodName)) {
				return getAttributes();
			}
			else if ("visit".equals(methodName)) {
				visit(args[0]);
				return null;
			}
			else if ("toString".equals(methodName)) {
				return toString();
			}

			throw new IllegalStateException("Unexpected method invocation: " + method);
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		@Nullable
		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttributes();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}

}
