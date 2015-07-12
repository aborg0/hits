/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import ie.tcd.imm.hits.util.Displayable;

import org.knime.core.node.util.DefaultStringIconOption;
import org.knime.core.node.util.StringIconOption;

/**
 * A {@link SettingsModel} to store ({@link Displayable} and {@link HasIcon})
 * enums.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <EnumType>
 *            The type of the contained enum.
 */
public class SettingsModelEnumWithIcon<EnumType extends Enum<EnumType> & Displayable & HasIcon>
		extends SettingsModelEnum<EnumType> {

	/**
	 * @param configName
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value. (Should be one of the allowed
	 *            {@link Displayable#getDisplayText()}s.)
	 * @param enumValues
	 *            The allowed values.
	 */
	@SafeVarargs
	public SettingsModelEnumWithIcon(final String configName,
			final String defaultValue, final EnumType... enumValues) {
		super(configName, defaultValue, enumValues);
	}

	/**
	 * @param configName
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value. (One of the allowed values.)
	 * @param enumValues
	 *            The allowed values.
	 */
	@SafeVarargs
	public SettingsModelEnumWithIcon(final String configName,
			final EnumType defaultValue, final EnumType... enumValues) {
		super(configName, defaultValue.getDisplayText(), enumValues);
	}

	@Override
	protected SettingsModelEnumWithIcon<EnumType> createClone() {
		return new SettingsModelEnumWithIcon<EnumType>(getConfigName(),
				getStringValue(), getPossibleValues());
	}

	/**
	 * @return The {@link StringIconOption}s for the
	 *         {@link #getPossibleValues() possible values}.
	 */
	public StringIconOption[] getStringIcons() {
		final StringIconOption[] ret = new StringIconOption[getPossibleValues().length];
		for (int i = getPossibleValues().length; i-- > 0;) {
			final EnumType enumType = getPossibleValues()[i];
			ret[i] = new DefaultStringIconOption(enumType.getDisplayText(),
					enumType.getIcon());
		}
		return ret;
	}
}
