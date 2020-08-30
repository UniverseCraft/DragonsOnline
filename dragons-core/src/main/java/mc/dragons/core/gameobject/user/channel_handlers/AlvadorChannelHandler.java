 package mc.dragons.core.gameobject.user.channel_handlers;
 
 import mc.dragons.core.gameobject.user.ChannelHandler;
 import mc.dragons.core.gameobject.user.ChatChannel;
 import mc.dragons.core.gameobject.user.User;
 
 public class AlvadorChannelHandler implements ChannelHandler {
   public boolean canHear(User to, User from) {
     return to.getActiveChatChannels().contains(ChatChannel.ALVADOR);
   }
   
   public ChatChannel getChannel() {
     return ChatChannel.ALVADOR;
   }
 }


