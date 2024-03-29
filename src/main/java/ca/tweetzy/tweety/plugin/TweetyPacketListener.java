package ca.tweetzy.tweety.plugin;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.StructureModifier;

import ca.tweetzy.tweety.PacketUtil;
import ca.tweetzy.tweety.model.HookManager;
import ca.tweetzy.tweety.model.SimpleEnchantment;
import ca.tweetzy.tweety.remain.CompMaterial;

/**
 * Listens to and intercepts packets using Foundation inbuilt features
 */
final class TweetyPacketListener {

	/**
	 * Registers our packet listener for some of the more advanced features of Foundation
	 */
	static void addNativeListener() {
		if (HookManager.isProtocolLibLoaded())

			// Auto placement of our lore when items are custom enchanted
			PacketUtil.addSendingListener(PacketType.Play.Server.SET_SLOT, event -> {
				final StructureModifier<ItemStack> itemModifier = event.getPacket().getItemModifier();
				ItemStack item = itemModifier.read(0);

				if (item != null && !CompMaterial.isAir(item.getType())) {
					item = SimpleEnchantment.addEnchantmentLores(item);

					// Write the item
					if (item != null)
						itemModifier.write(0, item);
				}
			});
	}
}
