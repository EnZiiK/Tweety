package ca.tweetzy.tweety.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import ca.tweetzy.tweety.collection.SerializedMap;
import ca.tweetzy.tweety.command.annotation.Permission;
import ca.tweetzy.tweety.command.annotation.PermissionGroup;
import ca.tweetzy.tweety.constants.TweetyPermissions;
import ca.tweetzy.tweety.exception.TweetyException;
import ca.tweetzy.tweety.model.ChatPaginator;
import ca.tweetzy.tweety.model.Replacer;
import ca.tweetzy.tweety.model.SimpleComponent;
import ca.tweetzy.tweety.settings.SimpleLocalization.Commands;
import ca.tweetzy.tweety.settings.SimpleSettings;
import lombok.NonNull;

/**
 * A simple predefined command for quickly listing all permissions
 * the plugin uses, given they are stored in a {@link TweetyPermissions} class.
 */
public final class PermsCommand extends SimpleSubCommand {

	/*
	 * Classes with permissions listed as fields
	 */
	private final Class<? extends TweetyPermissions> classToList;

	/*
	 * Special variables to replace in the annotation of each permission field in permissions class
	 */
	private final SerializedMap variables;

	/**
	 * Create a new "permisions|perms" subcommand using the given class
	 * that automatically replaces {label} in the \@PermissionGroup annotation in that class. 
	 * 
	 * @param classToList
	 */
	public PermsCommand(@NonNull Class<? extends TweetyPermissions> classToList) {
		this(classToList, SerializedMap
				.of("label", SimpleSettings.MAIN_COMMAND_ALIASES.get(0)));
	}

	/**
	 * Create a new "permisions|perms" subcommand using the given class
	 * that automatically replaces {label} in the \@PermissionGroup annotation in that class
	 * and the given command permission. 
	 * 
	 * @param classToList
	 * @param permission
	 */
	public PermsCommand(@NonNull Class<? extends TweetyPermissions> classToList, String permission) {
		this(classToList, SerializedMap
				.of("label", SimpleSettings.MAIN_COMMAND_ALIASES.get(0)));

		setPermission(permission);
	}

	/**
	 * Create a new "permisions|perms" subcommand using the given class with
	 * the given variables to replace in the \@PermissionGroup annotation in that class.
	 * 
	 * @param classToList
	 * @param variables
	 */
	public PermsCommand(@NonNull Class<? extends TweetyPermissions> classToList, SerializedMap variables) {
		super("permissions|perms");

		this.classToList = classToList;
		this.variables = variables;

		setDescription(Commands.PERMS_DESCRIPTION);
		setUsage(Commands.PERMS_USAGE);

		// Invoke to check for errors early
		list();
	}

	@Override
	protected void onCommand() {

		final String phrase = args.length > 0 ? joinArgs(0) : null;

		new ChatPaginator(15)
				.setFoundationHeader(Commands.PERMS_HEADER)
				.setPages(list(phrase))
				.send(sender);
	}

	/*
	 * Iterate through all classes and superclasses in the given classes and fill their permissions
	 */
	private List<SimpleComponent> list() {
		return this.list(null);
	}

	/*
	 * Iterate through all classes and superclasses in the given classes and fill their permissions
	 * that match the given phrase
	 */
	private List<SimpleComponent> list(String phrase) {
		final List<SimpleComponent> messages = new ArrayList<>();
		Class<?> iteratedClass = classToList;

		try {
			do {
				listIn(iteratedClass, messages, phrase);

			} while (!(iteratedClass = iteratedClass.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		return messages;
	}

	/*
	 * Find annotations and compile permissions list from the given class and given existing
	 * permissions that match the given phrase
	 */
	private void listIn(Class<?> clazz, List<SimpleComponent> messages, String phrase) throws ReflectiveOperationException {

		if (!clazz.isAssignableFrom(TweetyPermissions.class)) {
			final PermissionGroup group = clazz.getAnnotation(PermissionGroup.class);

			if (!messages.isEmpty() && !clazz.isAnnotationPresent(PermissionGroup.class))
				throw new TweetyException("Please place @PermissionGroup over " + clazz);

			messages.add(SimpleComponent
					.of("&7- " + (messages.isEmpty() ? Commands.PERMS_MAIN : group.value()) + " " + Commands.PERMS_PERMISSIONS)
					.onClickOpenUrl(""));
		}

		for (final Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Permission.class))
				continue;

			final Permission annotation = field.getAnnotation(Permission.class);

			final String info = Replacer.replaceVariables(annotation.value(), variables);
			final boolean def = annotation.def();

			if (info.contains("{") && info.contains("}"))
				throw new TweetyException("Forgotten unreplaced variable in " + info + " for field " + field + " in " + clazz);

			final String node = Replacer.replaceVariables((String) field.get(null), variables);
			final boolean has = sender != null && hasPerm(node);

			if (phrase == null || node.contains(phrase))
				messages.add(SimpleComponent
						.of("  " + (has ? "&a" : "&7") + node + (def ? " " + Commands.PERMS_TRUE_BY_DEFAULT : ""))
						.onClickOpenUrl("")
						.onHover(Commands.PERMS_INFO + info,
								Commands.PERMS_DEFAULT + (def ? Commands.PERMS_YES : Commands.PERMS_NO),
								Commands.PERMS_APPLIED + (has ? Commands.PERMS_YES : Commands.PERMS_NO)));
		}

		for (final Class<?> inner : clazz.getDeclaredClasses()) {
			messages.add(SimpleComponent.of("&r "));

			listIn(inner, messages, phrase);
		}
	}

	/**
	 * @see SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}