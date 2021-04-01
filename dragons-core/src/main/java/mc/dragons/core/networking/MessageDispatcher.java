package mc.dragons.core.networking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.scheduler.BukkitRunnable;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import mc.dragons.core.Dragons;

/**
 * Interfaces with MongoDB to handle message receipt.
 * 
 * @author Adam
 *
 */
public class MessageDispatcher {
	private Dragons instance;
	private boolean debug;
	private Map<String, MessageHandler> handlers;
	
	private MongoCollection<Document> messages;
	
	public MessageDispatcher(Dragons instance) {
		this.instance = instance;
		handlers = new HashMap<>();
		debug = false;
		messages = instance.getMongoConfig().getDatabase().getCollection(MessageConstants.MESSAGE_COLLECTION);
	}
	
	public void setDebug(boolean debug) { this.debug = debug; }
	public boolean isDebug() { return debug; }
	
	/**
	 * Creates a change stream watching for messages of the given type inbound to this server.
	 * The handler cannot receive any data without being registered here.
	 * Handlers automatically register upon construction.
	 * 
	 * @param handler The handler to register
	 */
	protected void registerHandler(MessageHandler handler) {
		handlers.put(handler.getMessageType(), handler);
		new BukkitRunnable() {
			@Override public void run() {
				ChangeStreamIterable<Document> watcher = messages.watch(List.of(Aggregates.match(
						Filters.and(Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.TYPE_FIELD, handler.getMessageType()), 
								Filters.or(Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.DEST_FIELD, instance.getServerName()), 
										Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.DEST_FIELD, MessageConstants.DEST_ALL))))));
				watcher.forEach((Consumer<ChangeStreamDocument<Document>>) d -> {
					if(debug) {
						long latency = System.currentTimeMillis() - d.getFullDocument().getLong("timestamp");
						instance.getLogger().info("Message Received from " + d.getFullDocument().getString(MessageConstants.ORIG_FIELD) 
								+ " (" + latency + "ms) into " + handler.getClass().getSimpleName() + ": " 
								+ d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class).toJson());
					}
					handler.receive(d.getFullDocument().getString(MessageConstants.ORIG_FIELD), d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class));
				});
			}
		}.runTaskAsynchronously(instance);
	}
}