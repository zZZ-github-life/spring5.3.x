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

package org.springframework.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.lang.Nullable;

/**
 * 用于将资源位置解析为文件系统中文件的实用方法。主要用于框架内部。
 *
 * <p>考虑使用 Spring 核心包中的 Resource 抽象，以统一方式处理各种文件资源。
 * {@link org.springframework.core.io.ResourceLoader} 的 {@code getResource()} 方法可以将任意位置解
 * 析为 {@link org.springframework.core.io.Resource} 对象，进而可以通过其 {@code getFile()} 方法获
 * 取文件系统中的 {@code java.io.File}。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.core.io.FileSystemResource
 * @see org.springframework.core.io.UrlResource
 * @see org.springframework.core.io.ResourceLoader
 */
public abstract class ResourceUtils {

	/** 从类路径加载的伪 URL 前缀："classpath:"。 */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/** 用于从文件系统加载的 URL 前缀："file:"。 */
	public static final String FILE_URL_PREFIX = "file:";

	/** 用于从 jar 文件加载的 URL 前缀："jar:"。 */
	public static final String JAR_URL_PREFIX = "jar:";

	/** 用于从 Tomcat 上的 war 文件加载的 URL 前缀："war:"。 */
	public static final String WAR_URL_PREFIX = "war:";

	/** 文件系统中文件的 URL 协议："file"。 */
	public static final String URL_PROTOCOL_FILE = "file";

	/** jar 文件中条目的 URL 协议："jar"。 */
	public static final String URL_PROTOCOL_JAR = "jar";

	/** war 文件中条目的 URL 协议："war"。 */
	public static final String URL_PROTOCOL_WAR = "war";

	/** zip 文件中条目的 URL 协议："zip"。 */
	public static final String URL_PROTOCOL_ZIP = "zip";

	/** WebSphere jar 文件中条目的 URL 协议："wsjar"。 */
	public static final String URL_PROTOCOL_WSJAR = "wsjar";

	/** JBoss jar 文件中条目的 URL 协议："vfszip"。 */
	public static final String URL_PROTOCOL_VFSZIP = "vfszip";

	/** JBoss 文件系统资源的 URL 协议："vfsfile"。 */
	public static final String URL_PROTOCOL_VFSFILE = "vfsfile";

	/** 通用 JBoss VFS 资源的 URL 协议："vfs"。 */
	public static final String URL_PROTOCOL_VFS = "vfs";

	/** 普通 jar 文件的文件扩展名：".jar"。 */
	public static final String JAR_FILE_EXTENSION = ".jar";

	/** JAR URL 与 JAR 内文件路径之间的分隔符："!/"。 */
	public static final String JAR_URL_SEPARATOR = "!/";

	/** Tomcat 上 WAR URL 与 jar 部分之间的特殊分隔符。 */
	public static final String WAR_URL_SEPARATOR = "*/";


	/**
	 * 返回给定资源位置是否为 URL：特殊 "classpath" 伪 URL 或标准 URL。
	 * @param resourceLocation 要检查的位置 String
	 * @return 该位置是否符合 URL 的条件
	 * @see #CLASSPATH_URL_PREFIX
	 * @see java.net.URL
	 */
	public static boolean isUrl(@Nullable String resourceLocation) {
		if (resourceLocation == null) {
			return false;
		}
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			return true;
		}
		try {
			new URL(resourceLocation);
			return true;
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 将给定的资源位置解析为 {@code java.net.URL}。
	 * <p>不检查 URL 是否实际存在；仅返回给定位置所对应的 URL。
	 * @param resourceLocation 要解析的资源位置：可以是 "classpath:" 伪 URL、
	 * "file:" URL，或普通的文件路径
	 * @return 对应的 URL 对象
	 * @throws FileNotFoundException 如果资源无法解析为 URL
	 */
	public static URL getURL(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				String description = "class path resource [" + path + "]";
				throw new FileNotFoundException(description +
						" cannot be resolved to URL because it does not exist");
			}
			return url;
		}
		try {
			// try URL
			return new URL(resourceLocation);
		}
		catch (MalformedURLException ex) {
			// no URL -> treat as file path
			try {
				return new File(resourceLocation).toURI().toURL();
			}
			catch (MalformedURLException ex2) {
				throw new FileNotFoundException("Resource location [" + resourceLocation +
						"] is neither a URL not a well-formed file path");
			}
		}
	}

	/**
	 * 将给定的资源位置解析为 {@code java.io.File}，即文件系统中的文件。
	 * <p>不检查文件是否实际存在；仅返回给定位置所对应的 File。
	 * @param resourceLocation 要解析的资源位置：可以是 "classpath:" 伪 URL、
	 * "file:" URL，或普通的文件路径
	 * @return 对应的 File 对象
	 * @throws FileNotFoundException 如果资源无法解析为文件系统中的文件
	 */
	public static File getFile(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			String description = "class path resource [" + path + "]";
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				throw new FileNotFoundException(description +
						" cannot be resolved to absolute file path because it does not exist");
			}
			return getFile(url, description);
		}
		try {
			// try URL
			return getFile(new URL(resourceLocation));
		}
		catch (MalformedURLException ex) {
			// no URL -> treat as file path
			return new File(resourceLocation);
		}
	}

	/**
	 * 将给定的资源 URL 解析为 {@code java.io.File}，即文件系统中的文件。
	 * @param resourceUrl 要解析的资源 URL
	 * @return 对应的 File 对象
	 * @throws FileNotFoundException 如果该 URL 无法解析为文件系统中的文件
	 */
	public static File getFile(URL resourceUrl) throws FileNotFoundException {
		return getFile(resourceUrl, "URL");
	}

	/**
	 * 将给定的资源 URL 解析为 {@code java.io.File}，即文件系统中的文件。
	 * @param resourceUrl 要解析的资源 URL
	 * @param description 创建该 URL 所针对的原始资源的描述
	 * （例如类路径位置）
	 * @return 对应的 File 对象
	 * @throws FileNotFoundException 如果该 URL 无法解析为文件系统中的文件
	 */
	public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
		Assert.notNull(resourceUrl, "Resource URL must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
					"because it does not reside in the file system: " + resourceUrl);
		}
		try {
			return new File(toURI(resourceUrl).getSchemeSpecificPart());
		}
		catch (URISyntaxException ex) {
			// Fallback for URLs that are not valid URIs (should hardly ever happen).
			return new File(resourceUrl.getFile());
		}
	}

	/**
	 * 将给定的资源 URI 解析为 {@code java.io.File}，即文件系统中的文件。
	 * @param resourceUri 要解析的资源 URI
	 * @return 对应的 File 对象
	 * @throws FileNotFoundException 如果该 URL 无法解析为文件系统中的文件
	 * @since 2.5
	 */
	public static File getFile(URI resourceUri) throws FileNotFoundException {
		return getFile(resourceUri, "URI");
	}

	/**
	 * 将给定的资源 URI 解析为 {@code java.io.File}，即文件系统中的文件。
	 * @param resourceUri 要解析的资源 URI
	 * @param description 创建该 URI 所针对的原始资源的描述
	 * （例如类路径位置）
	 * @return 对应的 File 对象
	 * @throws FileNotFoundException 如果该 URL 无法解析为文件系统中的文件
	 * @since 2.5
	 */
	public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
		Assert.notNull(resourceUri, "Resource URI must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
					"because it does not reside in the file system: " + resourceUri);
		}
		return new File(resourceUri.getSchemeSpecificPart());
	}

	/**
	 * 判断给定的 URL 是否指向文件系统中的资源，即协议为 "file"、"vfsfile" 或 "vfs"。
	 * @param url 要检查的 URL
	 * @return 该 URL 是否被识别为文件系统 URL
	 */
	public static boolean isFileURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol) ||
				URL_PROTOCOL_VFS.equals(protocol));
	}

	/**
	 * 判断给定的 URL 是否指向 jar 文件中的资源，即协议为 "jar"、"war"、"zip"、"vfszip" 或 "wsjar"。
	 * @param url 要检查的 URL
	 * @return 该 URL 是否被识别为 JAR URL
	 */
	public static boolean isJarURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_JAR.equals(protocol) || URL_PROTOCOL_WAR.equals(protocol) ||
				URL_PROTOCOL_ZIP.equals(protocol) || URL_PROTOCOL_VFSZIP.equals(protocol) ||
				URL_PROTOCOL_WSJAR.equals(protocol));
	}

	/**
	 * 判断给定的 URL 是否指向 jar 文件本身，即协议为 "file" 且以 ".jar" 扩展名结尾。
	 * @param url 要检查的 URL
	 * @return 该 URL 是否被识别为 JAR 文件 URL
	 * @since 4.1
	 */
	public static boolean isJarFileURL(URL url) {
		return (URL_PROTOCOL_FILE.equals(url.getProtocol()) &&
				url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION));
	}

	/**
	 * 从给定的 URL（可能指向 jar 文件中的资源，也可能指向 jar 文件本身）中提取实际 jar 文件的 URL。
	 * @param jarUrl 原始 URL
	 * @return 实际 jar 文件的 URL
	 * @throws MalformedURLException 如果无法提取出有效的 jar 文件 URL
	 */
	public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();
		int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
		if (separatorIndex != -1) {
			String jarFile = urlFile.substring(0, separatorIndex);
			try {
				return new URL(jarFile);
			}
			catch (MalformedURLException ex) {
				// Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
				// This usually indicates that the jar file resides in the file system.
				if (!jarFile.startsWith("/")) {
					jarFile = "/" + jarFile;
				}
				return new URL(FILE_URL_PREFIX + jarFile);
			}
		}
		else {
			return jarUrl;
		}
	}

	/**
	 * 从给定的 jar/war URL（可能指向 jar 文件中的资源，也可能指向 jar 文件本身）中提取最外层归档文件的
	 * URL。
	 * <p>对于嵌套在 war 文件中的 jar 文件，这将返回 war 文件的 URL，因为那是在文件系统中可解析的文件。
	 * @param jarUrl 原始 URL
	 * @return 实际 jar 文件的 URL
	 * @throws MalformedURLException 如果无法提取出有效的 jar 文件 URL
	 * @since 4.1.8
	 * @see #extractJarFileURL(URL)
	 */
	public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();

		int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
		if (endIndex != -1) {
			// Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
			String warFile = urlFile.substring(0, endIndex);
			if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
				return new URL(warFile);
			}
			int startIndex = warFile.indexOf(WAR_URL_PREFIX);
			if (startIndex != -1) {
				return new URL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
			}
		}

		// Regular "jar:file:...myjar.jar!/myentry.txt"
		return extractJarFileURL(jarUrl);
	}

	/**
	 * 为给定的 URL 创建 URI 实例，首先将空格替换为 "%20" URI 编码。
	 * @param url 要转换为 URI 实例的 URL
	 * @return URI 实例
	 * @throws URISyntaxException 如果该 URL 不是有效的 URI
	 * @see java.net.URL#toURI()
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		return toURI(url.toString());
	}

	/**
	 * 为给定的位置字符串创建 URI 实例，首先将空格替换为 "%20" URI 编码。
	 * @param location 要转换为 URI 实例的位置 String
	 * @return URI 实例
	 * @throws URISyntaxException 如果该位置不是有效的 URI
	 */
	public static URI toURI(String location) throws URISyntaxException {
		return new URI(StringUtils.replace(location, " ", "%20"));
	}

	/**
	 * 在给定连接上设置 {@link URLConnection#setUseCaches "useCaches"} 标志，优先使用 {@code false}，但
	 * 对于基于 JNLP 的资源将该标志保留为 {@code true}。
	 * @param con 要设置标志的 URLConnection
	 */
	public static void useCachesIfNecessary(URLConnection con) {
		con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
	}

}
