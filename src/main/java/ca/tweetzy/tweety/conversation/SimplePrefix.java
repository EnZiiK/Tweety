package ca.tweetzy.tweety.conversation;

import ca.tweetzy.tweety.Common;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A simple conversation prefix with a static string
 */
@RequiredArgsConstructor
public final class SimplePrefix implements ConversationPrefix {

	/**
	 * The conversation prefix
	 */
	@Getter
	private final String prefix;

	@Override
	public String getPrefix(ConversationContext context) {
		return Common.colorize(prefix);
	}
}