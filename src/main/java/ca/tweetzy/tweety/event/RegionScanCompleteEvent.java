package ca.tweetzy.tweety.event;

import org.bukkit.World;
import org.bukkit.event.HandlerList;

import ca.tweetzy.tweety.model.OfflineRegionScanner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triggered when {@link OfflineRegionScanner} finishes scanning all offline regions on your disk
 */
@Getter
@RequiredArgsConstructor
public final class RegionScanCompleteEvent extends SimpleEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The world this scanner operated in
	 */
	private final World world;

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}