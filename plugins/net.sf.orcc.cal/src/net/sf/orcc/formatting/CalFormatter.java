/*
 * generated by Xtext
 */
package net.sf.orcc.formatting;

import net.sf.orcc.services.CalGrammarAccess;
import net.sf.orcc.services.CalGrammarAccess.ActionElements;
import net.sf.orcc.services.CalGrammarAccess.ActorElements;
import net.sf.orcc.services.CalGrammarAccess.IntTypeElements;
import net.sf.orcc.services.CalGrammarAccess.PriorityElements;
import net.sf.orcc.services.CalGrammarAccess.ScheduleElements;
import net.sf.orcc.services.CalGrammarAccess.StateVariableElements;
import net.sf.orcc.services.CalGrammarAccess.UintTypeElements;

import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter;
import org.eclipse.xtext.formatting.impl.FormattingConfig;

/**
 * This class contains custom formatting description.
 * 
 * see : http://www.eclipse.org/Xtext/documentation/latest/xtext.html#formatting
 * on how and when to use it
 * 
 * Also see {@link org.eclipse.xtext.xtext.XtextFormattingTokenSerializer} as an
 * example
 */
public class CalFormatter extends AbstractDeclarativeFormatter {

	private CalGrammarAccess f;

	/**
	 * <pre>
	 * do
	 *   xxx
	 * end
	 * 
	 * </pre>
	 * 
	 * @param c
	 * @param kwdDo
	 * @param end
	 */
	private void body(FormattingConfig c, Keyword kwdDo, Keyword end) {
		// "do" indents until "end"
		c.setIndentation(kwdDo, end);

		// new line before and after "do"
		c.setLinewrap().before(kwdDo);
		c.setLinewrap().after(kwdDo);

		// 2 new lines after "end"
		c.setLinewrap(2).after(end);
	}

	/**
	 * <pre>
	 * end
	 * </pre>
	 * 
	 * @param c
	 * @param end
	 */
	private void bodyEmpty(FormattingConfig c, Keyword end) {
		// "end" unindents
		c.setIndentation(null, end);

		// new line before and 2 new lines after "end"
		c.setLinewrap().before(end);
		c.setLinewrap(2).after(end);
	}

	private void configureAction(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		c.setLinewrap().before(access.getTagAssignment_1_0());
		c.setNoSpace().before(access.getColonKeyword_1_1());

		configureActionInputs(c);
		configureActionOutputs(c);

		// empty action (no guards, no vars, no body)
		c.setLinewrap().before(access.getEndKeyword_6_0());
		c.setLinewrap(2).after(access.getEndKeyword_6_0());

		// action with guards (and possibly vars and body)
		configureActionGuards(c);

		// action with vars (and possibly a body)
		configureActionVars(c);

		// action with only a body
		body(c, access.getDoKeyword_6_3_0(), access.getEndKeyword_6_3_2());
	}

	/**
	 * <pre>
	 * guards
	 *   xxx
	 * ...
	 * </pre>
	 * 
	 * @param c
	 */
	private void configureActionGuards(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		keywordAndCommas(c, access.getGuardKeyword_6_1_0(), access
				.getCommaKeyword_6_1_2_0());

		configureActionGuardsVars(c);

		bodyEmpty(c, access.getEndKeyword_6_1_3_1_0());

		c.setIndentation(null, access.getDoKeyword_6_1_3_1_1_0());
		body(c, access.getDoKeyword_6_1_3_1_1_0(), access
				.getEndKeyword_6_1_3_1_1_2());
	}

	/**
	 * <pre>
	 * guards
	 *   xxx
	 * var
	 *   yyy
	 * ...
	 * </pre>
	 * 
	 * @param c
	 */
	private void configureActionGuardsVars(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		// "var" unindents
		c.setIndentation(null, access.getVarKeyword_6_1_3_0_0());

		keywordAndCommas(c, access.getVarKeyword_6_1_3_0_0(), access
				.getCommaKeyword_6_1_3_0_2_0());

		bodyEmpty(c, access.getEndKeyword_6_1_3_0_3_0());

		c.setIndentation(null, access.getDoKeyword_6_1_3_0_3_1_0());
		body(c, access.getDoKeyword_6_1_3_0_3_1_0(), access
				.getEndKeyword_6_1_3_0_3_1_2());
	}

	private void configureActionInputs(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		c.setNoSpace().before(access.getCommaKeyword_3_1_0());
		c.setNoSpace().before(f.getInputPatternAccess().getColonKeyword_0_1());
		c.setNoSpace().before(f.getInputPatternAccess().getCommaKeyword_3_0());
		c.setNoSpace().before(
				f.getInputPatternAccess().getLeftSquareBracketKeyword_1());
	}

	private void configureActionOutputs(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		c.setNoSpace().before(access.getCommaKeyword_5_1_0());
		c.setNoSpace().before(f.getOutputPatternAccess().getColonKeyword_0_1());
		c.setNoSpace().before(f.getOutputPatternAccess().getCommaKeyword_3_0());
		c.setNoSpace().before(
				f.getOutputPatternAccess().getLeftSquareBracketKeyword_1());
	}

	/**
	 * <pre>
	 * var
	 *   xxx
	 * ...
	 * </pre>
	 * 
	 * @param c
	 */
	private void configureActionVars(FormattingConfig c) {
		ActionElements access = f.getActionAccess();

		keywordAndCommas(c, access.getVarKeyword_6_2_0(), access
				.getCommaKeyword_6_2_2_0());
		bodyEmpty(c, access.getEndKeyword_6_2_3_0());

		c.setIndentation(null, access.getDoKeyword_6_2_3_1_0());
		body(c, access.getDoKeyword_6_2_3_1_0(), access
				.getEndKeyword_6_2_3_1_2());
	}

	private void configureActorBody(FormattingConfig c) {
		ActorElements access = f.getActorAccess();

		c.setLinewrap(2).after(access.getColonKeyword_10());
		c.setIndentation(access.getColonKeyword_10(), access.getEndKeyword_14());

		c.setLinewrap(2).before(access.getEndKeyword_14());
		c.setLinewrap(2).after(access.getEndKeyword_14());
	}

	@Override
	protected void configureFormatting(FormattingConfig c) {
		f = (CalGrammarAccess) getGrammarAccess();

		c.setIndentationSpace("\t");

		// Tags
		c.setNoSpace().around(f.getTagAccess().getFullStopKeyword_1_0());

		// Imports
		c.setNoSpace().before(f.getImportAccess().getSemicolonKeyword_2());
		c.setLinewrap().after(f.getImportAccess().getSemicolonKeyword_2());
		c.setLinewrap(2).after(f.getActorAccess().getImportsAssignment_0());

		configureParameters(c);
		configurePorts(c);
		configureActorBody(c);

		configureStateVariable(c);

		configureAction(c);

		configureStatements(c);

		configureSchedule(c);

		configurePriorities(c);

		// Types
		configureIntType(c);
		configureUintType(c);
	}

	private void configureStateVariable(FormattingConfig c) {
		StateVariableElements access = f.getStateVariableAccess();

		c.setNoSpace().before(access.getSemicolonKeyword_1());
		c.setLinewrap(2).after(access.getSemicolonKeyword_1());
	}

	private void configureIntType(FormattingConfig c) {
		IntTypeElements access = f.getIntTypeAccess();

		c.setNoSpace().before(access.getLeftParenthesisKeyword_1_0());
		c.setNoSpace().after(access.getLeftParenthesisKeyword_1_0());
		c.setNoSpace().after(access.getSizeKeyword_1_1());
		c.setNoSpace().after(access.getEqualsSignKeyword_1_2());
		c.setNoSpace().before(access.getRightParenthesisKeyword_1_4());

		c.setNoLinewrap().around(access.getLeftParenthesisKeyword_1_0());
		c.setNoLinewrap().around(access.getEqualsSignKeyword_1_2());
		c.setNoLinewrap().around(access.getRightParenthesisKeyword_1_4());
	}

	private void configureParameters(FormattingConfig c) {
		ActorElements access = f.getActorAccess();

		c.setIndentation(access.getLeftParenthesisKeyword_4(), access
				.getRightParenthesisKeyword_6());
		c.setNoSpace().after(access.getLeftParenthesisKeyword_4());
		c.setLinewrap().before(access.getParametersAssignment_5_0());
		c.setNoSpace().before(access.getCommaKeyword_5_1_0());
		c.setLinewrap().after(access.getCommaKeyword_5_1_0());
		c.setNoSpace().before(access.getRightParenthesisKeyword_6());
	}

	private void configurePorts(FormattingConfig c) {
		ActorElements access = f.getActorAccess();

		c.setNoLinewrap().around(access.getInputsAssignment_7_0());
		c.setNoLinewrap().around(access.getInputsAssignment_7_1_1());
		c.setNoLinewrap().around(access.getOutputsAssignment_9_0());
		c.setNoLinewrap().around(access.getOutputsAssignment_9_1_1());
		c.setNoSpace().before(access.getCommaKeyword_7_1_0());
	}

	private void configurePriorities(FormattingConfig c) {
		PriorityElements access = f.getPriorityAccess();

		c.setIndentation(access.getPriorityKeyword_1(), access
				.getEndKeyword_3());
		c.setLinewrap().after(access.getPriorityKeyword_1());
		c.setLinewrap(2).after(access.getEndKeyword_3());

		c.setNoSpace().before(f.getInequalityAccess().getSemicolonKeyword_2());
		c.setLinewrap().after(f.getInequalityAccess().getSemicolonKeyword_2());
	}

	private void configureSchedule(FormattingConfig c) {
		ScheduleElements access = f.getScheduleAccess();

		c.setIndentation(access.getScheduleKeyword_0(), access
				.getEndKeyword_5());
		c.setLinewrap().after(access.getColonKeyword_3());
		c.setLinewrap(2).after(access.getEndKeyword_5());

		c.setNoSpace().before(f.getTransitionAccess().getSemicolonKeyword_6());
		c.setLinewrap().after(f.getTransitionAccess().getSemicolonKeyword_6());
	}

	private void configureStatements(FormattingConfig c) {
		c.setLinewrap().after(f.getAssignAccess().getSemicolonKeyword_4());
		c.setNoSpace().before(f.getAssignAccess().getSemicolonKeyword_4());
	}

	private void configureUintType(FormattingConfig c) {
		UintTypeElements access = f.getUintTypeAccess();

		c.setNoSpace().before(access.getLeftParenthesisKeyword_1_0());
		c.setNoSpace().after(access.getLeftParenthesisKeyword_1_0());
		c.setNoSpace().after(access.getSizeKeyword_1_1());
		c.setNoSpace().after(access.getEqualsSignKeyword_1_2());
		c.setNoSpace().before(access.getRightParenthesisKeyword_1_4());

		c.setNoLinewrap().around(access.getLeftParenthesisKeyword_1_0());
		c.setNoLinewrap().around(access.getEqualsSignKeyword_1_2());
		c.setNoLinewrap().around(access.getRightParenthesisKeyword_1_4());
	}

	/**
	 * <pre>
	 * keyword
	 *   x,
	 *   y
	 * </pre>
	 * 
	 * @param c
	 * @param keyword
	 * @param comma
	 */
	private void keywordAndCommas(FormattingConfig c, Keyword keyword,
			Keyword comma) {
		// keyword indents
		c.setIndentation(keyword, null);

		// newline before and after keyword
		c.setLinewrap().before(keyword);
		c.setLinewrap().after(keyword);

		// no space before comma, new line after comma
		c.setLinewrap().after(comma);
		c.setNoSpace().before(comma);
	}
}
