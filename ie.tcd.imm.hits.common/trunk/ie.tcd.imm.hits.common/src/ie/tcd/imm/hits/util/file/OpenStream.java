/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy.Type;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Opens stream either from zip, http, or file connections.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class OpenStream {
	/**  */
	static final NodeLogger logger = NodeLogger.getLogger(OpenStream.class);
	static final String CORE_NET_BUNDLE = "org.eclipse.core.net";
	static final Set<String> supportedArchiveContentTypes = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);
	static {
		supportedArchiveContentTypes.add("application/java-archive");
		supportedArchiveContentTypes.add("application/x-jar");
		supportedArchiveContentTypes.add("application/x-java-jar");
		supportedArchiveContentTypes.add("application/zip");
		supportedArchiveContentTypes.add("application/gzip");
		supportedArchiveContentTypes.add("application/x-gzip");
	}

	/**
	 * Opens a stream uncompressed if the content inside a zip, and the URI
	 * specified the inner location.
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public static InputStream open(final URI uri) throws IOException {

		try {
			final URL url = uri.toURL();
			if (url.getProtocol().toLowerCase().startsWith("http")) {
				final IProxyData proxyDataForHost = OpenStream
						.getProxyService().getProxyDataForHost(uri.getHost(),
								uri.getScheme().toUpperCase());
				final InetSocketAddress sockAddr = new InetSocketAddress(
						InetAddress.getByName(proxyDataForHost.getHost()),
						proxyDataForHost.getPort());
				final Proxy proxy = new Proxy(Type.HTTP, sockAddr);
				final URLConnection connection = openConnection(uri,
						proxyDataForHost, proxy);
				try {
					return connection.getInputStream();
				} catch (final IOException e) {
					URI truncated = uri;
					while (truncated.getFragment() != null
							&& truncated.getFragment().length() > 1) {
						try {
							truncated = new URI(truncated.toString().substring(
									0, truncated.toString().lastIndexOf("/")));
							final URLConnection possConnection = openConnection(
									truncated, proxyDataForHost, proxy);
							if (supportedArchiveContentTypes
									.contains(possConnection.getContentType())) {
								if (possConnection.getContentType()
										.toLowerCase().contains("gzip")) {
									return new GZIPInputStream(possConnection
											.getInputStream());
								}
							} else {
								final String path = uri.relativize(truncated)
										.toString();
								final ZipInputStream zis = new ZipInputStream(
										possConnection.getInputStream());
								ZipEntry zipEntry;
								while ((zipEntry = zis.getNextEntry()) != null) {
									if (zipEntry.getName().equalsIgnoreCase(
											path)) {
										return zis;
									}
								}
								throw e;
							}
						} catch (final URISyntaxException e1) {
							throw e;
						}
					}
					throw e;
				}
			}
			throw new UnsupportedOperationException("Not supported protocol: "
					+ url.getProtocol());
		} catch (final MalformedURLException e) {
			throw e;
		}
	}

	public static IProxyService getProxyService() {
		final Bundle bundle = Platform.getBundle(CORE_NET_BUNDLE);
		while (bundle.getState() != Bundle.ACTIVE) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				ListContents.logger.warn(CORE_NET_BUNDLE
						+ " bundle not activated.", e);
			}
		}
		final ServiceReference ref = bundle.getBundleContext()
				.getServiceReference(IProxyService.class.getName());
		if (ref != null) {
			return (IProxyService) bundle.getBundleContext().getService(ref);
		}
		return null;
	}

	/**
	 * @param root
	 * @param proxyDataForHost
	 * @param proxy
	 * @return
	 * @throws IOException
	 */
	static URLConnection openConnection(final URI root,
			final IProxyData proxyDataForHost, final Proxy proxy)
			throws IOException {
		final URL url = convertURI(root).toURL();
		return openConnection(url, proxyDataForHost, proxy);
	}

	/**
	 * @param url
	 * @param proxyDataForHost
	 * @param proxy
	 * @return
	 * @throws IOException
	 */
	private static URLConnection openConnection(final URL url,
			final IProxyData proxyDataForHost, final Proxy proxy)
			throws IOException {
		URLConnection connection;
		try {
			connection = url
					.openConnection(proxyDataForHost == null ? Proxy.NO_PROXY
							: proxy);
		} catch (final IOException e1) {
			throw e1;
		}
		if (proxyDataForHost.isRequiresAuthentication()) {
			final String proxyLogin = proxyDataForHost.getUserId() + ":"
					+ proxyDataForHost.getPassword();
			connection.setRequestProperty("Proxy-Authorization", "Basic "
					+ EncodingUtils.encodeBase64(proxyLogin.getBytes()));
		}
		return connection;
	}

	/**
	 * @param uri
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI convertURI(final String uri) throws URISyntaxException {
		final URI root = new URI(uri.replace('\\', '/'));
		return convertURI(root);
	}

	/**
	 * @param root
	 * @return
	 */
	public static URI convertURI(URI root) {
		if (root.getScheme() == null || // Windows drive letter
				root.getScheme().length() == 1) {
			root = new File(root.toString()).toURI();
		}
		return root;
	}

}
