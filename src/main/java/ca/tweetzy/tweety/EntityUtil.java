package ca.tweetzy.tweety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import ca.tweetzy.tweety.EntityUtil.HitListener;
import ca.tweetzy.tweety.collection.expiringmap.ExpiringMap;
import ca.tweetzy.tweety.exception.TweetyException;
import ca.tweetzy.tweety.model.HookManager;
import ca.tweetzy.tweety.remain.Remain;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityUtil {

	static {
		Common.registerEvents(new HitTracking());
	}

	/**
	 * Returns the closest entity to the center location within the given 3-dimensional range
	 * that matches the given entity class, or null if not found.
	 *
	 * @param <T>
	 * @param center
	 * @param range3D
	 * @param entityClass
	 * @return
	 */
	public static <T extends LivingEntity> T findNearestEntity(Location center, double range3D, Class<T> entityClass) {
		final List<T> found = new ArrayList<>();

		for (final Entity nearby : center.getWorld().getNearbyEntities(center, range3D, range3D, range3D))
			if (nearby instanceof LivingEntity && entityClass.isAssignableFrom(nearby.getClass()))
				found.add((T) nearby);

		Collections.sort(found, (first, second) -> Double.compare(first.getLocation().distance(center), second.getLocation().distance(center)));

		return found.isEmpty() ? null : found.get(0);
	}

	/**
	 * Returns the entity target only if it is a player, or null
	 *
	 * @param entity
	 * @return
	 */
	public static Player getTargetPlayer(Entity entity) {
		final LivingEntity target = getTarget(entity);

		return target instanceof Player && !HookManager.isNPC(target) ? (Player) target : null;
	}

	/**
	 * Return the target for the given entity
	 *
	 * @param entity
	 * @return the target, or null if does not have / unsupported
	 */
	public static LivingEntity getTarget(Entity entity) {
		return entity instanceof Creature ? ((Creature) entity).getTarget() : null;
	}

	/**
	 * Return if this entity is creature and aggressive (not an animal)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isAggressive(Entity entity) {
		if (entity instanceof Ghast || entity instanceof Slime)
			return true;

		if (entity instanceof Wolf && ((Wolf) entity).isAngry())
			return true;

		if (entity instanceof Animals)
			return false;

		return entity instanceof Creature;
	}

	/**
	 * Return if this entity is a {@link Creature}, {@link Slime} or {@link Wolf}
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isCreature(Entity entity) {
		return entity instanceof Slime ||
				entity instanceof Wolf ||
				entity instanceof Creature;
	}

	/**
	 * Return if this entity is suitable for removing (e.g. dropped items,
	 * falling blocks, arrows, projectiles)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean canBeCleaned(Entity entity) {
		return entity instanceof FallingBlock ||
				entity instanceof Item ||
				entity instanceof Projectile ||
				entity instanceof ExperienceOrb;
	}

	// ----------------------------------------------------------------------------------------------------
	// Dropping
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Attempts to drop the item allowing space for applying properties to the item
	 * before it is spawned
	 *
	 * @param location
	 * @param item
	 * @param modifier
	 * @return the item
	 */
	public static Item dropItem(Location location, ItemStack item, Consumer<Item> modifier) {
		return Remain.spawnItem(location, item, modifier);
	}

	// ----------------------------------------------------------------------------------------------------
	// Tracking
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 * <p>
	 * If the entity still flies after 30 seconds, nothing is called
	 *
	 * @param entity
	 * @param hitGroundListener
	 */
	public static void trackFalling(Entity entity, Runnable hitGroundListener) {
		track(entity, 30 * 20, null, hitGroundListener);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your fly listener
	 * each tick until entity is either removed or isOnGround
	 * <p>
	 * If the entity still flies after 30 seconds, nothing is called
	 *
	 * @param entity
	 * @param flyListener
	 */
	public static void trackFlying(Entity entity, Runnable flyListener) {
		track(entity, 30 * 20, flyListener, null);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 * <p>
	 * The fly listener is called every tick
	 *
	 * @param entity
	 * @param timeoutTicks
	 * @param flyListener
	 * @param hitGroundListener
	 */
	public static void track(Entity entity, int timeoutTicks, Runnable flyListener, Runnable hitGroundListener) {
		if (flyListener == null && hitGroundListener == null)
			throw new TweetyException("Cannot track entity with fly and hit listeners on null!");

		Common.runTimer(1, new BukkitRunnable() {

			private int elapsedTicks = 0;

			@Override
			public void run() {

				// Cancel after the given timeout to save performance
				if (elapsedTicks++ > timeoutTicks) {
					cancel();

					return;
				}

				// Cancel when invalid
				if (entity == null || entity.isDead() || !entity.isValid()) {
					if (entity instanceof FallingBlock && hitGroundListener != null)
						hitGroundListener.run();

					cancel();
					return;
				}

				// Run the hit listener
				if (entity.isOnGround()) {
					if (hitGroundListener != null)
						hitGroundListener.run();

					cancel();

				} else if (flyListener != null)
					flyListener.run();
			}
		});
	}

	/**
	 * (No timer task) Starts tracking a projectile's impact and executes the hit
	 * task when it hits something. After 30 seconds of flight we stop tracking
	 * to save performance
	 *
	 * @param projectile
	 * @param hitTask
	 */
	public static void trackHit(Projectile projectile, HitListener hitTask) {
		HitTracking.addFlyingProjectile(projectile, hitTask);
	}

	/**
	 * The class responsible for tracking projectile's impact
	 */
	public interface HitListener {

		/**
		 * What should happen when the projectile hits something?
		 *
		 * @param event
		 */
		void onHit(ProjectileHitEvent event);
	}
}

/**
 * Class responsible for tracking connection between projectile launch and projectile hit event
 */
class HitTracking implements Listener {

	/**
	 * List of flying projectiles with code to run on impact,
	 * stop tracking after 30 seconds to prevent overloading the map
	 */
	private static volatile ExpiringMap<UUID, List<HitListener>> flyingProjectiles = ExpiringMap.builder().expiration(30, TimeUnit.SECONDS).build();

	/**
	 * Add a new flying projectile that will be pending and execute code when collide
	 *
	 * @param projectile
	 * @param hitTask
	 */
	static void addFlyingProjectile(Projectile projectile, HitListener hitTask) {
		synchronized (flyingProjectiles) {
			final UUID uniqueId = projectile.getUniqueId();
			final List<HitListener> listeners = flyingProjectiles.getOrDefault(uniqueId, new ArrayList<>());

			listeners.add(hitTask);
			flyingProjectiles.put(uniqueId, listeners);
		}
	}

	/**
	 * Invoke the hit listener when the registered projectile hits something
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHit(ProjectileHitEvent event) {

		synchronized (flyingProjectiles) {
			final List<HitListener> hitListeners = flyingProjectiles.remove(event.getEntity().getUniqueId());

			if (hitListeners != null)
				for (final HitListener listener : hitListeners)
					listener.onHit(event);
		}
	}
}
