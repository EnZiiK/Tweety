package ca.tweetzy.tweety.conversation;

import java.util.function.Consumer;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import ca.tweetzy.tweety.Valid;
import ca.tweetzy.tweety.settings.SimpleLocalization;
import lombok.*;

/**
 * A prompt that only accepts whole or decimal numbers
 */
@NoArgsConstructor
@AllArgsConstructor
public class SimpleDecimalPrompt extends SimplePrompt {

	/**
	 * The question you can set in the constructor already
	 */
	@Setter(value = AccessLevel.PROTECTED)
	private String question = null;

	/**
	 * What happens when the number is entered
	 */
	@Setter(value = AccessLevel.PROTECTED)
	private Consumer<Double> successAction;

	/**
	 * Show the question with the action to the player
	 *
	 * @param player
	 * @param question
	 * @param successAction
	 */
	public static void show(final Player player, final String question, final Consumer<Double> successAction) {
		new SimpleDecimalPrompt(question, successAction).show(player);
	}

	/**
	 * The menu question
	 *
	 * @see SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
	 */
	@Override
	protected String getPrompt(final ConversationContext ctx) {
		Valid.checkNotNull(question, "Please either call setQuestion or override getPrompt");

		return "&6" + question;
	}

	/**
	 * Return true if input is a valid number
	 *
	 * @see SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected boolean isInputValid(final ConversationContext context, final String input) {
		return Valid.isDecimal(input) || Valid.isInteger(input);
	}

	/**
	 * Show the message when the input is not a number
	 *
	 * @see SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
		return SimpleLocalization.Commands.INVALID_NUMBER.replace("{input}", invalidInput);
	}

	/**
	 * Parse through
	 *
	 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
		return acceptValidatedInput(context, Double.parseDouble(input));
	}

	/**
	 * What happens when the number is entered
	 *
	 * @param context
	 * @param input
	 * @return the next prompt, or {@link Prompt#END_OF_CONVERSATION} (that is actualy null) to exit
	 */
	protected Prompt acceptValidatedInput(final ConversationContext context, final double input) {
		Valid.checkNotNull(question, "Please either call setSuccessAction or override acceptValidatedInput");

		successAction.accept(input);
		return END_OF_CONVERSATION;
	}
}
