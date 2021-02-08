package mc.dragons.core.commands;

import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Command for users to report a problem with a quest,
 * for example deadlock.
 * 
 * @author Adam
 *
 */
public class StuckQuestCommand implements CommandExecutor {

	private QuestLoader questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
	private FeedbackLoader feedbackLoader;
	
	private final String[][] POSSIBLE_ISSUES = {
			{ "Deadlock", "There is no way to advance to the next objective of the quest", "deadlock" },
			{ "Looping", "The same objective or action keeps repeating", "looping" },
			{ "Missing Item or NPC", "An NPC or item that is required for the quest is missing", "missing" },
			{ "Wrong Objective", "You were given an objective that you already completed or that does not make sense", "wrong-objective" }
	};

	public StuckQuestCommand(Dragons instance) {
		feedbackLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(FeedbackLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(args.length == 0) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please confirm the quest you are having issues with:");
			for(Entry<Quest, QuestStep> entry : user.getQuestProgress().entrySet()) {
				if(!entry.getValue().getStepName().equalsIgnoreCase("Complete")) {
					TextComponent questOption = new TextComponent(ChatColor.GRAY + " • " + ChatColor.GREEN + entry.getKey().getQuestName());
					questOption.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							(new ComponentBuilder(ChatColor.GRAY + "Quest: " + ChatColor.RESET + entry.getKey().getQuestName()).create())));
					questOption.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stuckquest " + entry.getKey().getName()));
					user.getPlayer().spigot().sendMessage(questOption);
				}
			}
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.GRAY + "Click on one of the quests above to continue with the report.");
			sender.sendMessage(" ");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please select the issue with this quest:");
			for(String[] issue : POSSIBLE_ISSUES) {
				TextComponent issueOption = new TextComponent(ChatColor.GRAY + " • " + ChatColor.GREEN + issue[0]);
				issueOption.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						(new ComponentBuilder(ChatColor.GRAY + issue[1])).create()));
				issueOption.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stuckquest " + args[0] + " " + issue[2]));
				user.getPlayer().spigot().sendMessage(issueOption);
			}
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.GRAY + "Click on one of the issues above to submit the report.");
			sender.sendMessage(" ");
			return true;
		}
		
		if(args.length == 2) {
			Quest quest = questLoader.getQuestByName(args[0]);
			if(quest == null) {
				sender.sendMessage(ChatColor.RED + "Invalid quest name! /stuckquest");
				return true;
			}
			UUID cid = user.getQuestCorrelationID(quest);
			user.logQuestEvent(quest, Level.INFO, "User reported an issue with the quest: " + args[1]);
			user.logAllQuestData(quest);
			feedbackLoader.addFeedback("SYSTEM", "User " + user.getName() + " reported a problem with a quest. Correlation ID: " + cid);
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Quest report submitted successfully.");
			sender.sendMessage(ChatColor.YELLOW + "We're sorry you're having issues with this quest.");
			sender.sendMessage(ChatColor.GRAY + "In any follow-up communications with support staff, please include the following message.");
			sender.sendMessage(ChatColor.GRAY + StringUtil.toHdFont("Correlation ID: " + cid));
			sender.sendMessage(" ");
			return true;
		}
		
		return true;
	}

}