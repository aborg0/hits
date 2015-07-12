/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.util;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.meta.When;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Some utility methods for Rserve.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class RUtil {

	/**
	 * Hide constructor.
	 */
	private RUtil() {
		super();
	}

	/**
	 * Executes {@code command} on {@code conn}, discards the result.
	 * 
	 * @param conn
	 *            An {@link RConnection}.
	 * @param command
	 *            An R command.
	 * @throws RserveException
	 *             Problem with execution. Might contain the original command,
	 *             and the error message from R.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	@CheckReturnValue(when = When.NEVER)
	public static/* RConnection */void voidEval(final RConnection conn,
			final String command) throws RserveException, REXPMismatchException {
		eval(conn, command);
		// return conn;
	}

	/**
	 * Executes {@code command} on {@code conn}.
	 * 
	 * @param <ResultType>
	 *            Type of the expected result.
	 * @param conn
	 *            An {@link RConnection}.
	 * @param command
	 *            An R command.
	 * @return The result of the {@code command}.
	 * @throws RserveException
	 *             Problem with execution. Might contain the original command,
	 *             and the error message from R.
	 * @throws REXPMismatchException
	 *             Problem getting the error message.
	 */
	@SuppressWarnings("unchecked")
	public static <ResultType extends REXP> ResultType eval(
			final RConnection conn, final String command)
			throws RserveException, REXPMismatchException {
		final REXP eval;
		try {
			eval = conn.eval("try(" + command + ")");
		} catch (final RserveException e) {
			throw new RserveException(conn, e.getMessage() + "\nin command: "
					+ command);
		}
		final REXP attribute = eval.getAttribute("class");
		if (attribute != null && "try-error".equals(attribute.asString())) {
			throw new RserveException(conn, eval.asString() + "\nin command: "
					+ command);
		}
		return (ResultType) eval;
	}
}
