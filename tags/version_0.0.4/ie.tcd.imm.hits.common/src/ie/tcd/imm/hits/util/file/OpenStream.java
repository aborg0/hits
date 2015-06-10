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

import javax.annotation.Nullable;

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
	/** A logger for debug and error messages. */
	static final NodeLogger logger = NodeLogger.getLogger(OpenStream.class);
	/** The bundle to get the proxy settings from eclipse/KNIME. */
	static final String CORE_NET_BUNDLE = "org.eclipse.core.net";
	/** The supported archive content types. */
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

	private static interface AdjustConnection {
		public URLConnection open(URI uri) throws IOException;
	}

	/**
	 * Opens a stream uncompressed if the content inside a zip, and the URI
	 * specified the inner location.
	 * 
	 * @param uri
	 *            The {@link URI} to open (might be inside an archive file).
	 * @return The {@link InputStream} of the referenced resource.
	 * @throws IOException
	 *             If some problem occurred.
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
					return findZipEntry(uri, new AdjustConnection() {

						@Override
						public URLConnection open(final URI uri)
								throws IOException {
							return openConnection(uri, proxyDataForHost, proxy);
						}
					}, e);
				}
			}
			if (url.getProtocol().equalsIgnoreCase("file")) {
				try {
					return url.openStream();
				} catch (final IOException e) {
					return findZipEntry(uri, new AdjustConnection() {

						@Override
						public URLConnection open(final URI uri)
								throws IOException {
							return uri.toURL().openConnection();
						}
					}, e);
				}
			}
			throw new UnsupportedOperationException("Not supported protocol: "
					+ url.getProtocol());
		} catch (final MalformedURLException e) {
			throw e;
		}
	}

	private static InputStream findZipEntry(final URI uri,
			final AdjustConnection adjust, final IOException e)
			throws IOException {
		URI truncated = uri;
		while (truncated.getPath() != null && truncated.getPath().length() > 1) {
			try {
				truncated = new URI(truncated.toString().substring(0,
						truncated.toString().lastIndexOf("/")));
				final URLConnection possConnection = adjust.open(truncated);
				if (supportedArchiveContentTypes.contains(possConnection
						.getContentType())) {
					if (possConnection.getContentType().toLowerCase().contains(
							"gzip")) {
						return new GZIPInputStream(possConnection
								.getInputStream());
					}
					final ZipInputStream zis;
					zis = new ZipInputStream(possConnection.getInputStream());
					final String path = truncated.relativize(uri).toString();
					ZipEntry zipEntry;
					while ((zipEntry = zis.getNextEntry()) != null) {
						if (zipEntry.getName().equalsIgnoreCase(path)) {
							return zis;
						}
					}
				} else {
					try {
						final InputStream stream = possConnection
								.getInputStream();
						stream.close();
						throw e;
					} catch (final IOException e1) {
						continue;
					}
					// throw e;
				}
			} catch (final URISyntaxException e1) {
				throw e;
			}
		}
		throw e;
	}

	/**
	 * Based on eclipse proxy settings it finds an {@link IProxyService}. Based
	 * on code presented <a href=
	 * "http://torkildr.blogspot.com/2008/10/connecting-through-proxy.html"
	 * >here</a>
	 * 
	 * @return The found {@link IProxyService}, or {@code null} if none found.
	 */
	public static @Nullable
	IProxyService getProxyService() {
		final Bundle bundle = Platform.getBundle(CORE_NET_BUNDLE);
		while (bundle.getState() != Bundle.ACTIVE) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				logger.warn(CORE_NET_BUNDLE + " bundle not activated.", e);
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
	 * Opens a http(s) {@link URLConnection} with the specified proxy
	 * information.
	 * 
	 * @param root
	 *            The {@link URI} to open. (Will be {@link #convertURI(String)
	 *            converted} to {@link URL}.)
	 * @param proxyDataForHost
	 *            An {@link IProxyData}.
	 * @param proxy
	 *            The {@link Proxy} instance.
	 * @return The opened {@link URLConnection}.
	 * @throws IOException
	 *             Problem during open.
	 */
	static URLConnection openConnection(final URI root,
			final IProxyData proxyDataForHost, final Proxy proxy)
			throws IOException {
		final URL url = convertURI(root).toURL();
		return openConnection(url, proxyDataForHost, proxy);
	}

	/**
	 * Opens a http(s) {@link URLConnection} with the specified proxy
	 * information.
	 * 
	 * @param url
	 *            The {@link URL} to open.
	 * @param proxyDataForHost
	 *            An {@link IProxyData}.
	 * @param proxy
	 *            The {@link Proxy} instance.
	 * @return The opened {@link URLConnection}.
	 * @throws IOException
	 *             Problem during open.
	 */
	private static URLConnection openConnection(final URL url,
			@Nullable final IProxyData proxyDataForHost, final Proxy proxy)
			throws IOException {
		URLConnection connection;
		try {
			connection = url
					.openConnection(proxyDataForHost == null ? Proxy.NO_PROXY
							: proxy == null ? Proxy.NO_PROXY : proxy);
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
	 * Replaces all {@code \} characters to {@code /} and if the scheme is
	 * missing or single character it will interpret as a file reference.
	 * 
	 * @param uri
	 *            A possible {@link URI} as a {@link String}.
	 * @return A proper {@link URI}.
	 * @throws URISyntaxException
	 *             The {@code uri} is not correct.
	 */
	public static URI convertURI(final String uri) throws URISyntaxException {
		// final int colonPos = uri.indexOf(':');
		// final URI root = new URI(colonPos > 0 && colonPos < 3 ? uri.replace(
		// ":", "%58").replace('\\', '/') : uri.replace('\\', '/'));
		final URI root = new URI(uri.replace('\\', '/'));
		return convertURI(root);
	}

	/**
	 * if the scheme is missing or single character it will interpret as a file
	 * reference.
	 * 
	 * @param root
	 *            The {@link URI} to normalise.
	 * @return The converted {@link URI}.
	 */
	public static URI convertURI(URI root) {
		if (root.getScheme() == null || // Windows drive letter
				root.getScheme().length() == 1) {
			root = new File(root.toString()).toURI();
		}
		return root;
	}
}
