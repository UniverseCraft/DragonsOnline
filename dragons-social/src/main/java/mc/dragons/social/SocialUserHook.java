package mc.dragons.social;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.GuildLoader.Guild;

public class SocialUserHook implements UserHook {
	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	private DragonsSocialPlugin instance = JavaPlugin.getPlugin(DragonsSocialPlugin.class);
	private DiscordNotifier buildNotifier = instance.getBuildNotifier();
	
	@Override
	public void onVerifiedJoin(User user) {
		
		/* Send guild welcome(s) */
		Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {		
			List<Guild> guilds = guildLoader.getAllGuildsWithRaw(user.getUUID());
			boolean shown = false;
			for(Guild guild : guilds) {
				if(!guild.getMOTD().equals("")) {
					if(!shown) {
						user.getPlayer().sendMessage(" ");
						user.getPlayer().sendMessage(ChatColor.GREEN + "---[ " + ChatColor.GOLD + "Guild Messages" + ChatColor.GREEN + " ]---");
					}
					shown = true;
					user.getPlayer().sendMessage(guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName());
					user.getPlayer().sendMessage(guild.getThemeColor().secondary() + guild.getMOTD());
					user.getPlayer().sendMessage(" ");
				}
			}
		}, 20L);

		/* Warn if others are online but cannot hear the user */
		boolean anyCanHear = false;
		for(User test : UserLoader.allUsers()) {
			if(test.equals(user)) continue;
			for(ChatChannel ch : test.getActiveChatChannels()) {
				if(ch.canHear(test, user)) {
					anyCanHear = true;
					break;
				}
			}
		}
		if(!anyCanHear && Bukkit.getOnlinePlayers().size() > 1) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {
				user.getPlayer().sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Warning: " + ChatColor.RED + "Nobody else can hear you in the channel you're speaking in.");
				user.getPlayer().sendMessage(ChatColor.GRAY + "Do " + ChatColor.RESET + "/channel " + ChatColor.GRAY + "to manage channels.");
			}, 20L * 2);
		}
		
		/* Notify Discord when builders join the server */
		Rank rank = user.getRank();
		if(rank == Rank.BUILDER || rank == Rank.BUILDER_CMD || rank == Rank.BUILD_MANAGER
				|| rank == Rank.HEAD_BUILDER || rank == Rank.NEW_BUILDER) {
			buildNotifier.sendNotification(rank.getRankName() + " " + user.getName() + " joined the server!");
		}
	}
	
	@Override
	public String getListNameSuffix(User user) {
		List<String> guildNames = guildLoader.getAllGuildsWithRaw(user.getUUID()).stream().map(g ->
			(g.getOwner().equals(user.getUUID()) ? g.getThemeColor().primary() + "*" : "") + g.getThemeColor().tag() + g.getName()).collect(Collectors.toList());
		if(guildNames.size() == 0) return "";
		return ChatColor.DARK_GRAY + "[" + StringUtil.parseList(guildNames, ChatColor.DARK_GRAY + "/") + ChatColor.DARK_GRAY + "]";
	}

	@Override
	public boolean onDeath(User user) {
		for(Set<User> dueling : DuelCommands.getActive()) {
			if(dueling.contains(user)) {
				User loser = user;
				dueling.remove(user);
				User winner = user;
				if(dueling.size() > 0) {
					winner = (User) dueling.toArray()[0];
				}
				
				String[] endMessage = new String[] {
					" ",
					DuelCommands.DUEL_MESSAGE_HEADER,
					ChatColor.GOLD + "" + ChatColor.BOLD + "DUEL ENDED",
					ChatColor.YELLOW + "Winner: " + ChatColor.GRAY + winner.getName(),
					ChatColor.RED + "Loser: " + ChatColor.GRAY + loser.getName(),
					DuelCommands.DUEL_MESSAGE_HEADER,
					" "
				};
				
				winner.getPlayer().sendMessage(endMessage);
				loser.getPlayer().sendMessage(endMessage);
				
				final User fWinner = winner;
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {
					DuelCommands.restore(fWinner);
					DuelCommands.restore(loser);
					
					DuelCommands.getActive().remove(dueling);
				}, 20L);
				
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void onQuit(User user) {
		for(Set<User> dueling : DuelCommands.getActive()) {
			if(dueling.contains(user)) {
				dueling.remove(user);
				if(dueling.size() == 0) {
					return; // Workaround for one-person duel, only necessary for TESTING PURPOSES.
				}
				User other = (User) dueling.toArray()[0];
				other.getPlayer().sendMessage(ChatColor.RED + user.getName() + " left while the duel was in progress!");

				Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {
					DuelCommands.restore(other);
					DuelCommands.getActive().remove(dueling);
				}, 20L);
			}
		}
	}
	
}
