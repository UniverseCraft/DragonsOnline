package mc.dragons.social;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.social.duel.DuelCommands;
import mc.dragons.social.friend.FriendCommand;
import mc.dragons.social.guild.GuildAdminCommand;
import mc.dragons.social.guild.GuildChannelHandler;
import mc.dragons.social.guild.GuildCommand;
import mc.dragons.social.guild.GuildLoader;
import mc.dragons.social.messaging.PrivateMessageCommands;
import mc.dragons.social.party.PartyChannelHandler;
import mc.dragons.social.party.PartyCommand;
import mc.dragons.social.party.PartyLoader;
import mc.dragons.social.shout.ShoutCommand;

public class DragonsSocial extends DragonsJavaPlugin {
	private SocialUserHook socialHook;
	
	public void onEnable() {
		enableDebugLogging();
		
		Dragons dragons = getDragonsInstance();
		
		PartyLoader partyLoader = new PartyLoader(dragons);
		dragons.getLightweightLoaderRegistry().register(partyLoader);
		partyLoader.loadMessenger();
		
		dragons.getLightweightLoaderRegistry().register(new GuildLoader(dragons.getMongoConfig()));
		
		socialHook = new SocialUserHook();
		dragons.getUserHookRegistry().registerHook(socialHook);
		
		ChatChannel.GUILD.setHandler(new GuildChannelHandler(dragons));
		ChatChannel.PARTY.setHandler(new PartyChannelHandler(dragons));
		
		getCommand("guild").setExecutor(new GuildCommand());
		getCommand("guildadmin").setExecutor(new GuildAdminCommand());
		
		DuelCommands duelCommands = new DuelCommands();
		getCommand("duel").setExecutor(duelCommands);
		getCommand("listallduelstatus").setExecutor(duelCommands);
		getCommand("testduelwin").setExecutor(duelCommands);
		
		PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands(this);
		getCommand("msg").setExecutor(privateMessageCommands);
		getCommand("reply").setExecutor(privateMessageCommands);
		getCommand("chatspy").setExecutor(privateMessageCommands);
		getCommand("toggleselfmessage").setExecutor(privateMessageCommands);
		getCommand("togglemsg").setExecutor(privateMessageCommands);
		
		BlockCommands blockCommands = new BlockCommands();
		getCommand("block").setExecutor(blockCommands);
		getCommand("unblock").setExecutor(blockCommands);
		getCommand("toggleselfblock").setExecutor(blockCommands);
		
		FriendCommand friendCommand = new FriendCommand(this);
		getCommand("friend").setExecutor(friendCommand);
		getCommand("toggleselffriend").setExecutor(friendCommand);
		getCommand("dumpFriends").setExecutor(friendCommand);
		
		PartyCommand partyCommand = new PartyCommand(dragons);
		getCommand("party").setExecutor(partyCommand);
		getCommand("toggleselfparty").setExecutor(partyCommand);
		getCommand("dumpParties").setExecutor(partyCommand);
		
		getCommand("shout").setExecutor(new ShoutCommand());
		getCommand("channel").setExecutor(new ChannelCommand());
	}
	
	public SocialUserHook getSocialHook() {
		return socialHook;
	}
}
