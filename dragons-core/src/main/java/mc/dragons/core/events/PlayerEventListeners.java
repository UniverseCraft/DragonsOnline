package mc.dragons.core.events;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCConditionalActions;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class PlayerEventListeners implements Listener {
	public static final String GOLD_CURRENCY_DISPLAY_NAME = ChatColor.RESET + "" + ChatColor.GOLD + "Currency:Gold";
	
	private static ItemClassLoader itemClassLoader;
	public static ItemClass[] DEFAULT_INVENTORY;

	static {
		itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
		DEFAULT_INVENTORY = new ItemClass[] { itemClassLoader.getItemClassByClassName("LousyStick") };
	}
	
	private Dragons plugin;
	private Logger LOGGER;

	private UserLoader userLoader;
	private ItemLoader itemLoader;


	public PlayerEventListeners(Dragons instance) {
		this.plugin = instance;
		this.LOGGER = instance.getLogger();
		this.userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
		this.itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		this.LOGGER.finer("Chat event from player " + event.getPlayer().getName());
		User user = UserLoader.fromPlayer(event.getPlayer());
		event.setCancelled(true);
		user.chat(event.getMessage());
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);
		Action action = event.getAction();
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.EMPTY_MAP) {
			user.debug("Right click with map, denying");
			event.setUseItemInHand(Event.Result.DENY);
			event.setCancelled(true);
		}
		if (action == Action.RIGHT_CLICK_BLOCK) {
			user.debug("Right click block");
			Block clicked = event.getClickedBlock();
			if (clicked.getType() == Material.WALL_SIGN || clicked.getType() == Material.SIGN) {
				Sign sign = (Sign) clicked.getState();
				if (sign.getLine(0).equals("[RIGHT CLICK]"))
					if (sign.getLine(2).equals("Join as player")) {
						if (user.getSystemProfile() != null)
							user.setSystemProfile(null);
						player.sendMessage(ChatColor.GREEN + "Joining as a player. You can always sign in to your system profile later.");
						player.teleport(user.getSavedLocation());
						user.handleJoin(false);
					} else if (sign.getLine(2).equals("Join as staff")) {
						if (user.getSystemProfile() == null) {
							player.sendMessage(ChatColor.RED + "You must be logged in to your system profile to join as staff!");
							return;
						}
						if (sign.getLine(3).equals("Vanished (Mod+)")) {
							if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true))
								return;
							user.setVanished(true);
						}
						player.teleport(user.getSavedStaffLocation());
						user.handleJoin(false);
					} else {
						player.sendMessage(ChatColor.RED + "I don't know what to do with this!");
					}
			}
			return;
		}
		ItemStack heldItem = player.getInventory().getItemInMainHand();
		if (heldItem == null)
			return;
		Item item = ItemLoader.fromBukkit(heldItem);
		if (item == null)
			return;
		if (action == Action.LEFT_CLICK_AIR) {
			user.debug("Left click with " + item.getName());
			item.getItemClass().handleLeftClick(user);
		} else if (action == Action.RIGHT_CLICK_AIR) {
			user.debug("Right click with " + item.getName());
			item.getItemClass().handleRightClick(user);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		this.LOGGER.finer("Death event from " + event.getEntity().getName());
		Player player = event.getEntity();
		final User user = UserLoader.fromPlayer(player);
		player.sendMessage(ChatColor.DARK_RED + "You died!");
		final int countdown = this.plugin.getServerOptions().getDeathCountdown();
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
			@Override
			public void run() {
				user.sendToFloor("BeginnerTown");
				user.respawn();
				user.getPlayer().sendTitle(ChatColor.RED + "< " + ChatColor.DARK_RED + "You are dead" + ChatColor.RED + " >", ChatColor.GRAY + "Respawning on floor 1", 0, 20 * (countdown - 2), 40);
				user.setDeathCountdown(countdown);
			}
		}, 1L);
	}

	@EventHandler
	public void onDropItem(PlayerDropItemEvent event) {
		ItemStack drop = event.getItemDrop().getItemStack();
		int amt = drop.getAmount();
		Item item = ItemLoader.fromBukkit(drop);
		this.LOGGER.finer("Drop item event on " + event.getPlayer().getName() + " of " + ((item == null) ? "null" : item.getIdentifier()) + " (x" + amt + ")");
		if (item == null)
			return;
		User user = UserLoader.fromPlayer(event.getPlayer());
		if (item.isUndroppable()) {
			user.sendActionBar(ChatColor.DARK_RED + "You can't drop this item!");
			event.setCancelled(true);
			return;
		}
		Item dropItem = this.itemLoader.registerNew(item);
		dropItem.setQuantity(amt);
		event.getItemDrop().setItemStack(dropItem.getItemStack());
		user.takeItem(item, amt, true, false, true);
	}

	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		this.LOGGER.finer("Gamemode change event on " + event.getPlayer().getName() + " to " + event.getNewGameMode());
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.setGameMode(event.getNewGameMode(), false);
	}

	@EventHandler
	public void onHungerChangeEvent(FoodLevelChangeEvent event) {
		this.LOGGER.finer("Hunger change event on " + event.getEntity().getName());
		event.setCancelled(true);
		Player player = (Player) event.getEntity();
		player.setFoodLevel(20);
	}

	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		this.LOGGER.finer("Interact entity event on " + event.getPlayer().getName() + " to " + StringUtil.entityToString(event.getRightClicked()));
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.debug("Right-click");
		NPC npc = NPCLoader.fromBukkit(event.getRightClicked());
		if (npc != null) {
			user.debug("- Clicked an NPC");
			Item item = ItemLoader.fromBukkit(user.getPlayer().getInventory().getItemInMainHand());
			if (item != null) {
				user.debug("- Holding an RPG item");
				if (item.getClassName().equals("Special:ImmortalOverride")) {
					user.debug("- Destroy the NPC");
					npc.getEntity().remove();
					this.plugin.getGameObjectRegistry().removeFromDatabase(npc);
					user.getPlayer().sendMessage(ChatColor.GREEN + "Removed NPC successfully.");
					return;
				}
			}
			if (npc.getNPCType() == NPC.NPCType.QUEST && user.hasActiveDialogue() && System.currentTimeMillis() - user.getWhenBeganDialogue() > 1000L) {
				user.debug("Next dialogue");
				user.nextDialogue();
				return;
			}
			npc.getNPCClass().executeConditionals(NPCConditionalActions.NPCTrigger.CLICK, user, npc);
			npc.getNPCClass().getAddons().forEach(addon -> addon.onInteract(npc, user));
		}
		user.updateQuests(event);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		this.LOGGER.info("Join event on " + event.getPlayer().getName());
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		User user = this.userLoader.loadObject(uuid);
		event.setJoinMessage(null);
		boolean firstJoin = false;
		if (user == null) {
			firstJoin = true;
			this.plugin.getLogger().info("Player " + player.getName() + " joined for the first time");
			user = this.userLoader.registerNew(player);
			user.sendToFloor("BeginnerTown");
			for(ItemClass itemClass : DEFAULT_INVENTORY) {
				user.giveItem(this.itemLoader.registerNew(itemClass), true, false, true);
			}
		}
		User.PunishmentData banData = user.getActivePunishmentData(User.PunishmentType.BAN);
		if (banData != null) {
			player.kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You are banned.\n\n"
					+ (banData.getReason().equals("") ? "" : (ChatColor.GRAY + "Reason: " + ChatColor.WHITE + banData.getReason() + "\n")) + ChatColor.GRAY + "Expires: " + ChatColor.WHITE
					+ (banData.isPermanent() ? "Never" : banData.getExpiry().toString()));
			return;
		}
		if (user.getRank().ordinal() >= Rank.TRIAL_BUILDER.ordinal()) {
			GameMode restoreTo = user.getSavedGameMode();
			user.setGameMode(GameMode.ADVENTURE, true);
			user.setGameMode(restoreTo, false);
			user.sendToFloor("Staff");
			player.setPlayerListName(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "[Staff Joining]");
			player.sendMessage(ChatColor.AQUA + "Please login to your system profile or select \"Join as player\".");
		} else {
			user.handleJoin(firstJoin);
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		this.LOGGER.finest("Move event on " + event.getPlayer().getName() + " (" + StringUtil.locToString(event.getFrom()) + " [" + event.getFrom().getWorld().getName() + "] -> "
				+ StringUtil.locToString(event.getTo()) + " [" + event.getTo().getWorld().getName() + "])");
		User user = UserLoader.fromPlayer(event.getPlayer());
		if (user.hasDeathCountdown()) {
			event.setTo(event.getFrom());
			return;
		}
		user.handleMove();
	}

	@EventHandler
	public void onPickupItem(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		final Player player = (Player) event.getEntity();
		ItemStack pickup = event.getItem().getItemStack();
		User user = UserLoader.fromPlayer(player);
		if (pickup == null)
			return;
		if (pickup.getItemMeta() == null)
			return;
		if (pickup.getItemMeta().getDisplayName() == null)
			return;
		final Item item = ItemLoader.fromBukkit(pickup);
		if (item == null)
			return;
		this.LOGGER.finer("Pickup item event on " + player.getName() + " of " + ((item == null) ? "null" : item.getIdentifier()) + " (x" + pickup.getAmount() + ")");
		if (pickup.getItemMeta().getDisplayName().equals(GOLD_CURRENCY_DISPLAY_NAME)) {
			int amount = pickup.getAmount();
			user.giveGold(amount * 1.0D);
			(new BukkitRunnable() {
				@Override
				public void run() {
					Arrays.<ItemStack>asList(player.getInventory().getContents()).stream().filter(i -> (i != null)).filter(i -> (i.getItemMeta() != null))
							.filter(i -> (i.getItemMeta().getDisplayName() != null)).filter(i -> i.getItemMeta().getDisplayName().equals(PlayerEventListeners.GOLD_CURRENCY_DISPLAY_NAME))
							.forEach(i -> {
								player.getInventory().remove(i);
								PlayerEventListeners.this.plugin.getGameObjectRegistry().removeFromDatabase(item);
							});
				}
			}).runTaskLater(this.plugin, 1L);
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1.0F, 1.3F);
			return;
		}
		item.setQuantity(pickup.getAmount());
		user.giveItem(item, true, false, false);
		player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		event.setCancelled(true);
		event.getItem().remove();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.LOGGER.finer("Quit event on " + event.getPlayer().getName());
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.handleQuit();
		event.setQuitMessage(null);
	}
}
