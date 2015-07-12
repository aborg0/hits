/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.knime.core.node.NodeLogger;

/**
 * This class helps to list all files in a folder available whether that folder
 * is a local, or a http, https located one.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ListContents {
	/** The logger for debug and error messages. */
	private static final NodeLogger logger = NodeLogger
			.getLogger(ListContents.class);
	private static final Pattern pattern = Pattern
			.compile("href\\s*=\\s*[\"']([^\"']+)[\"']");
	private static final Set<String> supportedContentTypes = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);
	private ExecutorService executorService;
	static {
		supportedContentTypes.add("text/html");
		supportedContentTypes.add("application/xhtml+xml");
	}

	/**
	 * Finds the sub resources within the {@code root} {@link URI}. The search
	 * is limited by the {@code maxDepth} parameter, and possible to find
	 * resources in archive files and files available through http(s) protocol.
	 * <p>
	 * This is a synchronous call.
	 * 
	 * @param root
	 *            The {@link URI} where the search begins.
	 * @param maxDepth
	 *            The maximal depth to search for.
	 * @return A {@link Map} from the relative path from {@code root} to the
	 *         actual {@link URI}s. The contents in archives are referenced as:
	 *         {@code .../archive.zip/content/x.txt}
	 * @throws IOException
	 *             Problem occurred.
	 * @see #asyncFindContents(URI, int)
	 */
	public static Map<String, URI> findContents(final URI root,
			final int maxDepth) throws IOException {
		final Map<String, URI> ret = new HashMap<String, URI>();
		final URI orig = OpenStream.convertURI(root);
		findContents(orig, orig, new HashSet<URI>(), ret, maxDepth);
		return ret;
	}

	private static void findContents(final URI origRoot, final URI root,
			final Set<URI> visited, final Map<String, URI> results,
			final int maxDepth) throws IOException {
		if (maxDepth == 0) {
			return;
		}
		if (root.getScheme().equalsIgnoreCase("file")) {
			final File[] listFiles = new File(root).listFiles();
			if (listFiles != null) {
				for (final File file : listFiles) {
					results.put(origRoot.relativize(file.toURI()).toString(),
							file.toURI());
					visited.add(file.toURI());
					findContents(origRoot, file.toURI(), visited, results,
							maxDepth - 1);
				}
			}
			for (final String extension : new String[] { ".zip", ".jar",
					".war", ".aar" }) {
				if (root.toString().endsWith(extension)) {
					final FileInputStream fis = new FileInputStream(new File(
							root));
					try {
						final ZipInputStream stream = new ZipInputStream(fis);
						try {
							URI rootWithSlash;
							try {
								rootWithSlash = new URI(root.toString() + "/");
								findContentsZip(origRoot, rootWithSlash,
										stream, visited, results, maxDepth - 1);
							} catch (final URISyntaxException e) {
								findContentsZip(origRoot, root, stream,
										visited, results, maxDepth - 1);
							}
						} finally {
							stream.close();
						}
					} finally {
						fis.close();
					}
				}
			}
		}
		if (root.getScheme().toLowerCase().startsWith("http")) {
			// http://torkildr.blogspot.com/2008/10/connecting-through-proxy.html
			final IProxyService proxyService = OpenStream.getProxyService();
			if (proxyService.isProxiesEnabled()) {
				IProxyData[] select = proxyService.select(root);
				if (select.length > 0) {
					final IProxyData pd = select[0];
					final InetSocketAddress sockAddr = new InetSocketAddress(
							InetAddress.getByName(pd.getHost()), pd.getPort());
					//We do not handle SOCKS yet.
					final Proxy proxy = new Proxy(Type.HTTP, sockAddr);
					findContentsHttp(origRoot, root, pd, proxy, visited,
							results, maxDepth);
				}
			} else {
				findContentsHttp(origRoot, root, null, null, visited, results,
						maxDepth);
			}
		}
	}

	private static void findContentsHttp(final URI origRoot, final URI root,
			final IProxyData proxyDataForHost, final Proxy proxy,
			final Set<URI> visited, final Map<String, URI> results,
			final int maxDepth) {
		visited.add(root);
		URLConnection connection;
		try {
			connection = OpenStream.openConnection(root, proxyDataForHost,
					proxy);
		} catch (final IOException e1) {
			logger.debug("Unable to open connection: " + root, e1);
			return;
		}
		String contentType = connection.getContentType() == null ? ""
				: connection.getContentType();
		if (contentType.contains(";")) {
			contentType = contentType.substring(0, contentType.indexOf(';'));
		}
		if (supportedContentTypes.contains(contentType)) {
			final String streamToString;
			try {
				final InputStream inputStream = connection.getInputStream();
				try {
					streamToString = convertStreamToString(inputStream,
							connection.getContentEncoding());
				} finally {
					inputStream.close();
				}
			} catch (final IOException e) {
				logger.debug("uri: " + root, e);
				return;
			}
			final Matcher matcher = pattern.matcher(streamToString);
			int currentStart = 0;
			final List<String> possibleURIs = new ArrayList<String>();
			while (matcher.find(currentStart)) {
				possibleURIs.add(matcher.group(1));
				currentStart = matcher.end();
			}
			for (final String possURI : possibleURIs) {
				try {
					final URI resolve = root.resolve(possURI);
					if (visited.contains(resolve)) {
						continue;
					}
					if (resolve.toString().startsWith(origRoot.toString())) {
						results.put(origRoot.relativize(resolve).toString(),
								resolve);
						findContentsHttp(origRoot, resolve, proxyDataForHost,
								proxy, visited, results, maxDepth - 1);
					}
				} catch (final RuntimeException e) {
					// Do not care, this might happen
					logger.debug("root: " + root + "URI: " + possURI, e);
				}
			}
		} else if (OpenStream.supportedArchiveContentTypes
				.contains(contentType)
				|| contentType.equalsIgnoreCase("application/octet-stream")) {
			try {
				final InputStream inputStream = connection.getInputStream();
				try {
					if (contentType.toLowerCase().contains("gzip")) {
						// findContentsGzip(root.relativize(origRoot),
						// inputStream, visited, results, maxDepth);
					} else {
						final ZipInputStream stream = new ZipInputStream(
								inputStream);
						try {
							findContentsZip(origRoot,
									origRoot.relativize(root), stream, visited,
									results, maxDepth);
						} finally {
							stream.close();
						}
					}
				} finally {
					inputStream.close();
				}
			} catch (final RuntimeException e) {
				logger.debug("", e);
			} catch (final IOException e) {
				logger.debug("", e);
			}
			if (root.resolve(origRoot).toString().startsWith(
					origRoot.toString())) {
				final URI rel = origRoot.relativize(root);
				results.put(rel.toString(), root);
			}
		} else {
			if (contentType.isEmpty()) {
				logger.debug("Unknown content, skipped: " + root);
			} else {
				if (root.resolve(origRoot).toString().startsWith(
						origRoot.toString())) {
					final URI rel = origRoot.relativize(root);
					results.put(rel.toString(), root);
				}
			}
		}
	}

	private static void findContentsZip(final URI origRoot, final URI root,
			final ZipInputStream inputStream, final Set<URI> visited,
			final Map<String, URI> results, final int maxDepth)
			throws IOException {
		ZipEntry nextEntry;
		while ((nextEntry = inputStream.getNextEntry()) != null) {
			try {
				final URI resolved = root.resolve("./"
						+ nextEntry.getName().replaceAll(" ", "%20").replace(
								'\\', '/'));
				final String key = // root + nextEntry.getName();//
				// resolved.relativize(origRoot).toString();
				origRoot.relativize(resolved).toString();
				results.put(key, resolved);
			} catch (final IllegalArgumentException e) {
				// skipping
				logger.debug("wrong path: " + nextEntry.getName(), e);
			}
		}
	}

	private static String convertStreamToString(final InputStream is,
			final String encoding) throws IOException {
		final InputStreamReader in = new InputStreamReader(is,
				encoding == null ? "UTF-8" : encoding);
		try {
			final BufferedReader reader = new BufferedReader(in);
			try {
				final StringBuilder sb = new StringBuilder();

				String line;
				try {
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
				} finally {
					is.close();
				}
				return sb.toString();
			} finally {
				reader.close();
			}
		} finally {
			in.close();
		}
	}

	/**
	 * A constructor to be able to use asynchronous calls.
	 */
	public ListContents() {
		super();
		createNewExecutor();
	}

	/**
	 * Creates a new {@link ExecutorService}.
	 */
	private void createNewExecutor() {
		executorService = Executors.newSingleThreadExecutor();
	}

	/**
	 * Finds the sub resources within the {@code root} {@link URI}. The search
	 * is limited by the {@code maxDepth} parameter, and possible to find
	 * resources in archive files and files available through http(s) protocol.
	 * <p>
	 * This is an asynchronous call.
	 * 
	 * @param root
	 *            The {@link URI} where the search begins.
	 * @param maxDepth
	 *            The maximal depth to search for.
	 * @return A {@link Map} from the relative path from {@code root} to the
	 *         actual {@link URI}s. The contents in archives are referenced as:
	 *         {@code .../archive.zip/content/x.txt}
	 * @see #findContents(URI, int)
	 */
	public Future<Map<String, URI>> asyncFindContents(final URI root,
			final int maxDepth) {
		executorService.shutdownNow();
		createNewExecutor();
		final Callable<Map<String, URI>> task = new Callable<Map<String, URI>>() {
			@Override
			public Map<String, URI> call() /* => */throws IOException {
				return findContents(root, maxDepth);
			}
		};
		return executorService.submit(task);
	}
}
