package mc.dragons.core.events;

import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemConstants;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.ProgressBarUtil;
import mc.dragons.core.util.StringUtil;

public class EntityDamageListener implements Listener {
	private Dragons dragons;
	private Logger LOGGER;

	private RegionLoader regionLoader;

	public EntityDamageListener(Dragons instance) {
		regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
		dragons = instance;
		LOGGER = instance.getLogger();
	}

	/**
	 * Handles player-vs-player, player-vs-entity, and entity-vs-player interactions.
	 * Calculates damage amounts and applies any special damage effects.
	 * 
	 * @param event
	 */
	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		LOGGER.finer("Damage event on " + StringUtil.entityToString(event.getEntity()) + " by " + StringUtil.entityToString(event.getDamager()));
		Entity damager = event.getDamager();
		User userDamager = null;
		NPC npcDamager = null;
		Item attackerHeldItem = null;
		if (damager instanceof Player) {
			userDamager = UserLoader.fromPlayer((Player) damager);
			attackerHeldItem = ItemLoader.fromBukkit(userDamager.getPlayer().getInventory().getItemInMainHand());
			if(attackerHeldItem != null && !attackerHeldItem.getItemClass().canUse(userDamager)) {
				userDamager.debug("- GM Locked, cancelling");
				userDamager.sendActionBar(ChatColor.DARK_RED + "- This item is GM locked! -");
				return;
			}
		} else if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				npcDamager = NPCLoader.fromBukkit((Entity) arrow.getShooter());
			}
		} else {
			npcDamager = NPCLoader.fromBukkit(damager);
		}
		if (npcDamager != null && npcDamager.getNPCType() != NPC.NPCType.HOSTILE) {
			event.setCancelled(true);
			return;
		}
		boolean external = false;
		Entity target = event.getEntity();
		User userTarget = null;
		NPC npcTarget = null;
		if (target instanceof Player) {
			userTarget = UserLoader.fromPlayer((Player) target);
		} else if (target instanceof ArmorStand) {
			
			/*
			 * Complex entities are rendered as composites of simpler entities.
			 * When a player attacks a complex entity, one of those component
			 * entities actually receives the damage. We route it to the appropriate
			 * actual entity by checking for the appropriate metadata.
			 */
			if (target.hasMetadata("partOf")) {
				LOGGER.finer("-Target is part of a complex entity!");
				npcTarget = (NPC) target.getMetadata("partOf").get(0).value();
				external = true;
			}
		} else {
			npcTarget = NPCLoader.fromBukkit(target);
			if (npcTarget != null) {
				if (npcTarget.isImmortal() || target.isInvulnerable()) {
					event.setCancelled(true);
					if (userDamager != null) {
						Item item = ItemLoader.fromBukkit(userDamager.getPlayer().getInventory().getItemInMainHand());
						
						/*
						 * This specific item class allows removal of immortal entities,
						 * intended for use by the content team.
						 */
						if (item != null && item.getClassName().equals(ItemConstants.IMMORTAL_OVERRIDE_ITEM_CLASS)) {
							npcTarget.getEntity().remove();
							dragons.getGameObjectRegistry().removeFromDatabase(npcTarget);
							userDamager.getPlayer().sendMessage(ChatColor.GREEN + "Removed NPC successfully.");
							return;
						}
						immortalTarget(target, userDamager);
					}
					npcTarget.updateHealthBar();
					return;
				}
			} else {
				
				/*
				 * Sometimes vanilla entities will sneak their way into a production world.
				 */
				LOGGER.finer("-ERROR: Target is an entity but not an NPC! HasHandle=" + target.hasMetadata("handle"));
				if(userDamager != null && PermissionUtil.verifyActivePermissionLevel(userDamager, PermissionLevel.TESTER, false)) {
					HologramUtil.temporaryArmorStand(target, ChatColor.RED + "Error: Unbound Entity Target", 20 * 5, true);
				}
			}
		}
		
		/*
		 * When a player is talking to a quest NPC, they cannot interact with other entities.
		 */
		if (userDamager != null && npcTarget != null && userDamager.hasActiveDialogue()) {
			userDamager.sendActionBar(ChatColor.GRAY + "PVE is disabled during quest dialogue!");
			event.setCancelled(true);
			return;
		}
		if (npcDamager != null && userTarget != null && userTarget.hasActiveDialogue()) {
			event.setCancelled(true);
			return;
		}
		
		double distance = damager.getLocation().distance(target.getLocation());
		double damage = event.getDamage();
		if (userDamager == null && npcDamager == null || userTarget == null && npcTarget == null || npcDamager != null && npcTarget != null) {
			return;
		}
		if (userDamager != null && userDamager.isGodMode()) {
			if (npcTarget != null) {
				npcTarget.remove();
			} else if (!(target instanceof Player)) {
				target.remove();
			} else {
				((Player) target).setHealth(0.0D);
			}
			return;
		}
		if (userTarget != null && userTarget.isGodMode()) {
			immortalTarget(target, userDamager);
			event.setCancelled(true);
			return;
		}
		
		Set<Region> regions = regionLoader.getRegionsByLocation(target.getLocation());
		if (userTarget != null) {
			userTarget.debug("user target");
			for (Region region : regions) {
				if (!Boolean.valueOf(region.getFlags().getString("pve"))) {
					userTarget.debug("- Cancelled damage due to region " + region.getName() + ": PVE flag = false");
					event.setCancelled(true);
					return;
				}
			}
		}
		
		if (npcDamager != null) {
			
			/*
			 * Adjust damage amount based on the level difference. A high level entity will damage a
			 * low level player more than it would damage a high level player.
			 */
			double weightedLevelDiscrepancy = Math.max(0.0D, npcDamager.getLevel() - 0.3D * userTarget.getLevel());
			damage += 0.25D * weightedLevelDiscrepancy;
		} else {
			
			/*
			 * Apply item based damage modifiers and cooldowns
			 */
			double itemDamage = 0.5D;
			if (attackerHeldItem != null) {
				if (attackerHeldItem.hasCooldownRemaining()) {
					userDamager.sendActionBar(ChatColor.RED + "- WAIT - " + MathUtil.round(attackerHeldItem.getCooldownRemaining()) + "s -");
					event.setCancelled(true);
					return;
				}
				if (!external) {
					attackerHeldItem.registerUse();
				}
				final User fUserDamager = userDamager;
				final Item fAttackerHeldItem = attackerHeldItem;
				new BukkitRunnable() {
					@Override public void run() {
						Item currentHeldItem = ItemLoader.fromBukkit(fUserDamager.getPlayer().getInventory().getItemInMainHand());
						if (currentHeldItem == null || !currentHeldItem.equals(fAttackerHeldItem)) {
							return;
						}
						double percentRemaining = fAttackerHeldItem.getCooldownRemaining() / fAttackerHeldItem.getCooldown();
						String cooldownName = String.valueOf(fAttackerHeldItem.getDecoratedName()) + ChatColor.DARK_GRAY + " [" + ChatColor.RESET + "WAIT "
								+ ProgressBarUtil.getCountdownBar(percentRemaining) + ChatColor.DARK_GRAY + "]";
						fUserDamager.getPlayer().getInventory().setItemInMainHand(fAttackerHeldItem.localRename(cooldownName));
						if (!fAttackerHeldItem.hasCooldownRemaining()) {
							new BukkitRunnable() {
								@Override public void run() {
									Item currentHeldItem = ItemLoader.fromBukkit(fUserDamager.getPlayer().getInventory().getItemInMainHand());
									if (currentHeldItem == null || !currentHeldItem.equals(fAttackerHeldItem)) {
										return;
									}
									fUserDamager.getPlayer().getInventory().setItemInMainHand(fAttackerHeldItem.localRename(fAttackerHeldItem.getDecoratedName()));
								}
							}.runTaskLater(dragons, 10L);
							cancel();
						}
					}
				}.runTaskTimer(dragons, 0L, 5L);
				if(userDamager.getLevel() >= attackerHeldItem.getLevelMin()) {
					itemDamage = attackerHeldItem.getDamage();
				}
				else {
					damager.sendMessage(ChatColor.RED + "You must be level " + attackerHeldItem.getLevelMin() + " or higher to use this item!");
					userDamager.sendActionBar(ChatColor.RED + "You can't use this! Lv Min: " + attackerHeldItem.getLevelMin());
					event.setCancelled(true);
					return;
				}
				damage += itemDamage;
			}
			
			if (userTarget == null) {
				for (Region region : regions) {
					if (!Boolean.valueOf(region.getFlags().getString("pve"))) {
						event.setCancelled(true);
						userDamager.sendActionBar(ChatColor.GRAY + "PVE is disabled in this region.");
						return;
					}
				}
			} else {
				for (Region region : regions) {
					if (!Boolean.valueOf(region.getFlags().getString("pvp"))) {
						event.setCancelled(true);
						userDamager.sendActionBar(ChatColor.GRAY + "PVP is disabled in this region.");
						return;
					}
				}
			}
			
			/* Melee skill increases inversely proportionally with distance from target */
			userDamager.incrementSkillProgress(SkillType.MELEE, Math.min(0.5D, 1.0D / distance));
			double randomMelee = Math.random() * userDamager.getSkillLevel(SkillType.MELEE) / distance;
			damage += randomMelee;
		}
		if (userTarget != null) {
			double randomDefense = Math.random() * Math.random() * userTarget.getSkillLevel(SkillType.DEFENSE);
			damage -= randomDefense;
			Item targetHeldItem = ItemLoader.fromBukkit(userTarget.getPlayer().getInventory().getItemInMainHand());
			double itemDefense = 0.0D;
			if (targetHeldItem != null && targetHeldItem.getItemClass().canUse(userTarget)) {
				itemDefense = targetHeldItem.getArmor();
			}
			for(ItemStack itemStack : userTarget.getPlayer().getInventory().getArmorContents()) {
				Item armorItem = ItemLoader.fromBukkit(itemStack);
				if (armorItem != null && armorItem.getItemClass().canUse(userTarget)) {
					itemDefense += armorItem.getArmor();
				}
			}
			double actualItemDefense = Math.min(damage, Math.random() * itemDefense);
			damage -= actualItemDefense;
			userTarget.incrementSkillProgress(SkillType.DEFENSE, Math.random() * actualItemDefense);
		}
		
		damage = Math.max(0.0D, damage);
		
		if(userDamager != null) {
			userDamager.debug("DMG OUTGOING: " + damage + " to " + StringUtil.entityToString(target));
		}
		
		if(userTarget != null) {
			userTarget.debug("DMG INCOMING: " + damage + " from " + StringUtil.entityToString(damager));
		}
		
		if (npcTarget != null) {
			npcTarget.setDamageExternalized(external);
		}
		if (external) {
			npcTarget.damage(damage, damager);
			event.setDamage(0.0D);
			LOGGER.finer("-Damage event external from " + StringUtil.entityToString(target) + " to " + StringUtil.entityToString(npcTarget.getEntity()));
		} else {
			event.setDamage(damage);
			if (userDamager != null) {
				String tag = ChatColor.RED + "-" + Math.round(damage) + "❤";
				if (target.getNearbyEntities(10.0D, 10.0D, 10.0D).stream().filter(e -> (e.getType() == EntityType.PLAYER)).count() > 1L) {
					tag = String.valueOf(tag) + ChatColor.GRAY + " from " + userDamager.getName();
				}
				HologramUtil.temporaryArmorStand(target, tag, 20, false);
			}
		}
		if (npcTarget != null) {
			npcTarget.getNPCClass().handleTakeDamage(npcTarget, npcDamager != null ? (GameObject) npcDamager : (GameObject) userDamager, damage);
			npcTarget.updateHealthBar(damage);
			if (userDamager != null) {
				npcTarget.getNPCClass().executeConditionals(NPCTrigger.HIT, userDamager, npcTarget);
			}
		}
		if (npcDamager != null) {
			npcDamager.getNPCClass().handleDealDamage(npcDamager, npcTarget != null ? (GameObject) npcTarget : (GameObject) userTarget, damage);
		}
	}

	public void immortalTarget(Entity target, User damager) {
		if (damager == null) {
			return;
		}
		damager.sendActionBar(ChatColor.RED + "Target is immortal!");
		HologramUtil.temporaryArmorStand(target, ChatColor.LIGHT_PURPLE + "✦ Immortal Object", 40, false);
	}
}