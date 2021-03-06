package mc.dragons.core.gameobject.user.chat.channel_handlers;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChannelHandler;
import mc.dragons.core.gameobject.user.chat.ChatChannel;

public class AlvadorChannelHandler implements ChannelHandler {
	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.ALVADOR);
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.ALVADOR;
	}
}
