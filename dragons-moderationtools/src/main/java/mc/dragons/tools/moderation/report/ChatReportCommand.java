package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.chat.MessageData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatReportCommand extends DragonsCommandExecutor {
	private static String CONFIRMATION_FLAG = " --internal-confirm-and-submit";
	
	private ChatMessageRegistry chatMessageRegistry = instance.getChatMessageRegistry();
	private ReportLoader reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Click on a chat message to report it.");
			return true;
		}
		
		User reporter = user(sender);
		
		Integer messageId = parseIntType(sender, args[0]);
		if(messageId == null) return true;
		
		MessageData messageData = chatMessageRegistry.get(messageId);
		if(messageData == null) {
			sender.sendMessage(ChatColor.RED + "No data found for the specified message!");
			return true;
		}
		
		if(messageData.getSender().equals(reporter) && !reporter.getLocalData().getBoolean("canSelfReport", false)) {
			sender.sendMessage(ChatColor.RED + "You can't report your own message!");
			return true;
		}
		
		if(args.length < 2 || args[1].equals(CONFIRMATION_FLAG)) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please review your chat report before submitting.");
			sender.sendMessage(ChatColor.GREEN + "Reporting: " + ChatColor.GRAY + messageData.getSender().getName());
			sender.sendMessage(ChatColor.GREEN + "Message: " + ChatColor.GRAY + messageData.getMessage());
			sender.sendMessage(" ");
			TextComponent submit = new TextComponent(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Submit]");
			submit.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ChatColor.GRAY + "By submitting, you confirm that this report is accurate to the best of your knowledge.").create()));
			submit.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatreport " + args[0] + " " + CONFIRMATION_FLAG));
			TextComponent cancel = new TextComponent(ChatColor.GRAY + "   " + ChatColor.BOLD + "[Cancel]");
			cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ChatColor.GRAY + "You can always create a new chat report by clicking on a different message.").create()));
			sender.spigot().sendMessage(submit, cancel);
			sender.sendMessage(" ");
			return true;
		}
		
		reportLoader.fileChatReport(messageData.getSender(), reporter, messageData);
		sender.sendMessage(ChatColor.GREEN + "Chat report filed successfully. A staff member will review it as soon as possible.");
		
		return true;
	}

}
