/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package org.knime.core.node.defaultnodesettings;

import ie.tcd.imm.hits.util.Displayable;
import ie.tcd.imm.hits.util.Displayable.Util;

/**
 * A {@link SettingsModel} to store ({@link Displayable}) enums.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 * @param <EnumType>
 *            The type of the contained enum.
 */
public class SettingsModelEnum<EnumType extends Enum<EnumType> & Displayable>
		extends SettingsModelString {

	private final EnumType[] values;

	/**
	 * @param configName
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value. (Should be one of the allowed
	 *            {@link Displayable#getDisplayText()}s.)
	 * @param enumValues
	 *            The allowed values.
	 */
	public SettingsModelEnum(final String configName,
			final String defaultValue, final EnumType... enumValues) {
		super(configName, defaultValue);
		values = enumValues.clone();
	}

	/**
	 * @param configName
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value. (One of the allowed values.)
	 * @param enumValues
	 *            The allowed values.
	 */
	public SettingsModelEnum(final String configName,
			final EnumType defaultValue, final EnumType... enumValues) {
		super(configName, defaultValue.getDisplayText());
		values = enumValues.clone();
	}

	@Override
	protected SettingsModelEnum<EnumType> createClone() {
		return new SettingsModelEnum<EnumType>(getConfigName(),
				getStringValue(), values);
	}

	/**
	 * @return The selected {@link Enum}.
	 */
	public EnumType getEnumValue() {
		return Util.findByDisplayText(getStringValue(), values);
	}

	/**
	 * @return The allowed {@link Displayable#getDisplayText() display texts}.
	 */
	public String[] getDisplayTexts() {
		final String[] ret = new String[values.length];
		for (int i = values.length; i-- > 0;) {
			ret[i] = values[i].getDisplayText();
		}
		return ret;
	}

	/**
	 * @return The allowed values.
	 */
	public EnumType[] getPossibleValues() {
		return values.clone();
	}
}
