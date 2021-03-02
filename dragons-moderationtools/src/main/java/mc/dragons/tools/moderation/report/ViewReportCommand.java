package mc.dragons.tools.moderation.report;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;

public class ViewReportCommand implements CommandExecutor {

	private ReportLoader reportLoader;
	
	public ViewReportCommand(Dragons instance) {
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User user = UserLoader.fromPlayer((Player) sender);
		if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/vr <ID>");
			sender.sendMessage(ChatColor.RED + "/vr <ID> -addnote <Note>");
			sender.sendMessage(ChatColor.RED + "/vr <ID> -setstatus <OPEN|NO_ACTION|ACTION_TAKEN>");
			return true;
		}
		
		Report report = reportLoader.getReportById(Integer.valueOf(args[0]));
		if(report == null) {
			sender.sendMessage(ChatColor.RED + "No report with ID " + args[0] + " was found!");
			return true;
		}
		if(args.length == 1) {
			sender.sendMessage(ChatColor.DARK_GREEN + "Report #" + report.getId());
			sender.sendMessage(ChatColor.YELLOW + "" + report.getType() + "/" + report.getStatus());
			sender.sendMessage(ChatColor.GRAY + "Filed Against: " + ChatColor.RESET + report.getTarget().getName());
			sender.sendMessage(ChatColor.GRAY + "Filed By: " + ChatColor.RESET + report.getFiledBy().getName());
			sender.sendMessage(ChatColor.GRAY + "Filing Date: " + ChatColor.RESET + report.getFiledOn());
			if(report.getReviewedBy() != null) {
				sender.sendMessage(ChatColor.GRAY + "Primary Reviewer: " + ChatColor.RESET + report.getReviewedBy().getName());
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "Unreviewed.");
			}
			if(report.getData().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Data: ");
				for(Entry<String, Object> entry : report.getData().entrySet()) {
					sender.sendMessage(ChatColor.GRAY + "- " + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + entry.getValue());
				}
			}
			if(report.getNotes().size() > 0) {
				sender.sendMessage(ChatColor.GRAY + "Notes: ");
				for(String note : report.getNotes()) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + note);
				}
			}
			return true;
		}
		if(args[1].equalsIgnoreCase("-addnote")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vr <ID> -addnote <Note>");
				return true;
			}
			report.addNote(StringUtil.concatArgs(args, 2) + " (by " + user.getName() + ")");
			sender.sendMessage(ChatColor.GREEN + "Note added successfully.");
			return true;
		}
		if(args[1].equalsIgnoreCase("-setstatus")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.RED + "/vr <ID> -setstatus <OPEN|NO_ACTION|ACTION_TAKEN>");
				return true;
			}
			ReportStatus status = StringUtil.parseEnum(sender, ReportStatus.class, args[2]);
			if(status == null) return true;
			report.setStatus(status);
			report.addNote("Status set to " + status + " by " + user.getName());
			if(status != ReportStatus.OPEN) {
				report.setReviewedBy(user);
			}
			sender.sendMessage(ChatColor.GREEN + "Status changed successfully.");
			return true;
		}
		
		return true;
	}
	
	
}
