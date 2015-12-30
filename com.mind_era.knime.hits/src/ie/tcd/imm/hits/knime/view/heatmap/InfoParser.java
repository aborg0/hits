/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.view.StatTypes;

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
	 * @param experiment
	 *            The name of the selected experiment.
	 * @param normalisation
	 *            The normalisations/scorings done for the experiment. (See
	 *            {@link ModelBuilder#getNormKey(org.knime.core.data.DataRow, int, int, int, int, int, int)}
	 *            )
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
		final Format predictedFormat = model.getModelBuilder()
				.getSpecAnalyser().getPredictedFormat();
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
														ds[predictedFormat
																.getPos(row,
																		col)])
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
												values[predictedFormat.getPos(
														row, col)]).append(
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
					final String value = values[predictedFormat
							.getPos(row, col)];
					if (value != null) {
						sb.append(value.replaceAll("\n", "<p>"));
					}
				}
			}
		}
		sb.append(format.substring(lastEnd));
		return sb.toString();
	}

	/**
	 * Parses {@code format} and with the other parameters it creates a label
	 * for the wells.
	 * 
	 * @param experiments
	 *            The name of the selected experiment.
	 * @param normalisations
	 *            The normalisations/scorings done for the experiment. (See
	 *            {@link ModelBuilder#getNormKey(org.knime.core.data.DataRow, int, int, int, int, int, int)}
	 *            )
	 * @param format
	 *            A formatting {@link String}. The special parts should start
	 *            with {@code $}, HTML tags are accepted.
	 * @param plates
	 *            The plate number, starting from {@code 1}.
	 * @param row
	 *            The row number, starting from {@code 0}.
	 * @param col
	 *            The column number, starting from {@code 0}.
	 * @param model
	 *            The {@link HeatmapNodeModel}.
	 * @return The formatted text.
	 */
	public static String parse(final List<String> experiments,
			final List<String> normalisations, final String format,
			final List<Integer> plates, final int row, final int col,
			final HeatmapNodeModel model) {
		final StringBuilder sb = new StringBuilder();
		if (model == null || model.getTable() == null) {
			return "";
		}
		final Format predictedFormat = model.getModelBuilder()
				.getSpecAnalyser().getPredictedFormat();
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
				sb.append(plates.size() == 1 ? plates.get(0) : plates);
			} else if ("well".equalsIgnoreCase(code)) {
				sb.append((char) (row + 'A')).append(col + 1);
			} else {
				try {
					final StatTypes statTypes = StatTypes.valueOf(code);
					final List<String> parameters = new ArrayList<String>();
					if (statTypes.isUseReplicates()) {
						for (final String experiment : experiments) {
							for (final String normalisation : normalisations) {
								for (final Integer plate : plates) {

									final Map<Integer, Map<String, Map<StatTypes, double[]>>> replicates = model
											.getModelBuilder().getReplicates()
											.get(experiment).get(normalisation)
											.get(plate);
									if (replicates != null) {
										for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> replEntry : replicates
												.entrySet()) {
											final Integer replicate = replEntry
													.getKey();
											final Map<String, Map<StatTypes, double[]>> paramMap = replEntry
													.getValue();
											if (paramMap != null) {
												if (parameters.isEmpty()) {
													parameters.addAll(paramMap
															.keySet());
													if (additionalParams == null) {
														sb.append("<th>");
														for (final String string : parameters) {
															sb
																	.append(
																			"<td>")
																	.append(
																			string)
																	.append(
																			"</td>");
														}
														if (experiments.size() > 1) {
															sb
																	.append(
																			"<td>")
																	.append(
																			"experiment")
																	.append(
																			"</td>");
														}
														if (normalisations
																.size() > 1) {
															sb
																	.append(
																			"<td>")
																	.append(
																			"normalisation")
																	.append(
																			"</td>");
														}
														if (plates.size() > 1) {
															sb
																	.append(
																			"<td>")
																	.append(
																			"plate")
																	.append(
																			"</td>");
														}
														sb.append("</th>\n");
													}
												}
												if (additionalParams == null) {
													sb.append("<tr><td>")
															.append(replicate)
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
															sb
																	.append(
																			"<td>")
																	.append(
																			ds == null ? ""
																					: ds[predictedFormat
																							.getPos(
																									row,
																									col)])
																	.append(
																			"</td>");
														}
														// TODO else handle
														// additionalParams
													}
												}
												if (experiments.size() > 1) {
													sb.append("<td>").append(
															experiment).append(
															"</td>");
												}
												if (normalisations.size() > 1) {
													sb.append("<td>").append(
															normalisation)
															.append("</td>");
												}
												if (plates.size() > 1) {
													sb.append("<td>").append(
															plate).append(
															"</td>");
												}
												if (additionalParams == null) {
													sb.append("</tr>\n");
												}
												// TODO else
											}
										}
									}
								}
							}
						}
					} else// Does not depend on replicates
					{
						for (final String experiment : experiments) {
							for (final String normalisation : normalisations) {
								for (final Integer plate : plates) {
									final Map<String, Map<StatTypes, double[]>> map = model
											.getModelBuilder().getScores().get(
													experiment).get(
													normalisation).get(plate);// FIXME
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
												if (experiments.size() > 1) {
													sb.append("<td>").append(
															"experiment")
															.append("</td>");
												}
												if (normalisations.size() > 1) {
													sb.append("<td>").append(
															"normalisation")
															.append("</td>");
												}
												if (plates.size() > 1) {
													sb.append("<td>").append(
															"plate").append(
															"</td>");
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
													sb
															.append("<td>")
															.append(
																	values[predictedFormat
																			.getPos(
																					row,
																					col)])
															.append("</td>");
												}
											} else {
												if (additionalParams == null) {
													sb.append("<td></td>");
												}
											}
										}
										if (experiments.size() > 1) {
											sb.append("<td>")
													.append(experiment).append(
															"</td>");
										}
										if (normalisations.size() > 1) {
											sb.append("<td>").append(
													normalisation).append(
													"</td>");
										}
										if (plates.size() > 1) {
											sb.append("<td>").append(plate)
													.append("</td>");
										}
										if (additionalParams == null) {
											sb.append("</tr>\n");
										}
									}
								}
							}
						}
					}
				} catch (final RuntimeException e) {
					// No problem
				}
			}
			final Map<String, String[]> map = model.getModelBuilder()
					.getTexts().get(experiments.get(0)).get(
							normalisations.get(0)).get(
							Integer.valueOf(plates.get(0)));// FIXME
			if (map != null) {
				final String[] values = map.get(code);
				if (values != null) {
					final String value = values[predictedFormat
							.getPos(row, col)];
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
