package mc.dragons.core.gameobject.user.chat;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all chat messages since the server was started
 * 
 * @author Adam
 *
 */
public class ChatMessageRegistry {
	private Map<Integer, MessageData> history;
	
	public ChatMessageRegistry() {
		history = new HashMap<>();
	}
	
	public void register(MessageData message) {
		history.put(message.getId(), message);
	}
	
	public MessageData get(int id) {
		return history.get(id);
	}
}
