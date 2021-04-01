package mc.dragons.tools.moderation.punishment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;

public class ViewPunishmentsCommand extends DragonsCommandExecutor {
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /viewpunishments <player>");
			return true;
		}
		
		Player targetPlayer = Bukkit.getPlayerExact(args[0]);
		User targetUser = lookupUser(sender, args[0]);
		
		if(targetUser == null) return true;

		PunishmentData banData = targetUser.getActivePunishmentData(PunishmentType.BAN);
		PunishmentData muteData = targetUser.getActivePunishmentData(PunishmentType.MUTE);
		
		sender.sendMessage(ChatColor.GOLD + "Punishment History for User " + targetUser.getName());
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "This player is offline. Showing cached data.");
		}
		sender.sendMessage(ChatColor.YELLOW + "Active Punishments:");
		if(banData == null || banData.hasExpired()) {
			sender.sendMessage(ChatColor.WHITE + "- Not banned");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Banned: " + banData.getReason() + " (" + (banData.isPermanent() ? "Permanent" : "Until " + banData.getExpiry().toString()) + ")");
		}
		if(muteData == null || banData.hasExpired()) {
			sender.sendMessage(ChatColor.WHITE + "- Not muted");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Muted: " + muteData.getReason() + " (" + (muteData.isPermanent() ? "Permanent" : "Until " + muteData.getExpiry().toString()) + ")");
		}
		
		sender.sendMessage(ChatColor.YELLOW + "Past Punishments:");
		
		int i = 1;
		for(PunishmentData entry : targetUser.getPunishmentHistory()) {
			String duration = "";
			if(entry.isPermanent()) duration = "(Permanent)";
			else if(entry.getExpiry() != null) duration = "(Until " + entry.getExpiry().toString() + ")";
			sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.RED + entry.getType() + ": " + ChatColor.WHITE + entry.getReason() + " " + duration);
			i++;
		}
		
		if(targetPlayer == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}