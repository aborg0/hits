/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses and creates well information descriptions.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class InfoParser {
	private static final Pattern pattern = Pattern
			.compile("(\\$\\{[^}(]+(\\([^\\)]+\\))?\\})");

	/**
	 * Parses {@code format} and with the other parameters it creates a label
	 * for the wells.
	 * 
	 * @param format
	 *            A formatting {@link String}. The special parts should start
	 *            with {@code $}, HTML tags are accepted.
	 * @param plate
	 *            The plate number, starting from {@code 1}.
	 * @param row
	 *            The row number, starting from {@code 0}.
	 * @param col
	 *            The column number, starting from {@code 0}.
	 * @param model
	 *            The {@link HeatmapNodeModel}.
	 * @return The formatted text.
	 */
	public static String parse(final String experiment,
			final String normalisation, final String format, final int plate,
			final int row, final int col, final HeatmapNodeModel model) {
		final StringBuilder sb = new StringBuilder();
		if (model == null || model.getTable() == null) {
			return "";
		}
		final Matcher matcher = pattern.matcher(format);
		int lastEnd = 0;
		while (matcher.find()) {
			sb.append(format.substring(lastEnd, matcher.start()));
			lastEnd = matcher.end();
			final String code = format.substring(matcher.start() + 2, matcher
					.group(2) != null ? matcher.start(1) - 1
					: matcher.end() - 1);
			final String additionalParams = matcher.groupCount() > 1 ? matcher
					.group(2) : null;
			if ("plate".equalsIgnoreCase(code)) {
				sb.append(plate);
			} else if ("well".equalsIgnoreCase(code)) {
				sb.append((char) (row + 'A')).append(col + 1);
			} else {
				try {
					final StatTypes statTypes = StatTypes.valueOf(code);
					final List<String> parameters = new ArrayList<String>();
					if (statTypes.isUseReplicates()) {
						final Map<Integer, Map<String, Map<StatTypes, double[]>>> replicates = model
								.getModelBuilder().getReplicates().get(
										experiment).get(normalisation).get(
										Integer.valueOf(plate));
						if (replicates != null) {
							for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> replEntry : replicates
									.entrySet()) {
								final Integer replicate = replEntry.getKey();
								final Map<String, Map<StatTypes, double[]>> paramMap = replEntry
										.getValue();
								if (paramMap != null) {
									if (parameters.isEmpty()) {
										parameters.addAll(paramMap.keySet());
										if (additionalParams == null) {
											sb.append("<th>");
											for (final String string : parameters) {
												sb.append("<td>")
														.append(string).append(
																"</td>");
											}
											sb.append("</th>\n");
										}
									}
									if (additionalParams == null) {
										sb.append("<tr><td>").append(replicate)
												.append("</td>");
									}
									// TODO else
									for (final String parameter : parameters) {

										final Map<StatTypes, double[]> stats = paramMap
												.get(parameter);
										if (stats != null) {
											final double[] ds = stats
													.get(statTypes);
											if (additionalParams == null) {
												sb.append("<td>").append(
														ds[row * 12 + col])
														.append("</td>");
											}
											// TODO else handle additionalParams
										}
									}
									if (additionalParams == null) {
										sb.append("</tr>\n");
									}
									// TODO else
								}
							}

						}
					} else// Does not depend on replicates
					{
						final Map<String, Map<StatTypes, double[]>> map = model
								.getModelBuilder().getScores().get(experiment)
								.get(normalisation).get(Integer.valueOf(plate));
						if (map != null) {
							if (parameters.isEmpty()) {
								parameters.addAll(map.keySet());
								if (additionalParams == null) {
									sb.append("<th>");
									for (final String param : parameters) {
										sb.append("<td>");
										sb.append(param);
										sb.append("</td>");
									}
									sb.append("</th>\n");
								}
							}
							if (additionalParams == null) {
								sb.append("<tr><td></td>");
							}
							for (final String param : parameters) {
								final Map<StatTypes, double[]> stats = map
										.get(param);
								if (stats != null) {
									final double[] values = stats
											.get(statTypes);
									if (additionalParams == null) {
										sb.append("<td>").append(
												values[row * 12 + col]).append(
												"</td>");
									}
								} else {
									if (additionalParams == null) {
										sb.append("<td></td>");
									}
								}
							}
							if (additionalParams == null) {
								sb.append("</tr>\n");
							}
						}
					}
				} catch (final RuntimeException e) {
					// No problem
				}
			}
			final Map<String, String[]> map = model.getModelBuilder()
					.getTexts().get(experiment).get(normalisation).get(
							Integer.valueOf(plate));
			if (map != null) {
				final String[] values = map.get(code);
				if (values != null) {
					final String value = values[row * 12 + col];
					if (value != null) {
						sb.append(value.replaceAll("\n", "<p>"));
					}
				}
			}
		}
		sb.append(format.substring(lastEnd));
		return sb.toString();
	}
}
