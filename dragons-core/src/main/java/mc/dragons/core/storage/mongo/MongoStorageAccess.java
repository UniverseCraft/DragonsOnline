package mc.dragons.core.storage.mongo;

import static mc.dragons.core.util.BukkitUtil.rollingAsync;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;

/**
 * Persistent unit of data storage backed by a MongoDB instance.
 * 
 * @author Adam
 *
 */
public class MongoStorageAccess implements StorageAccess {
	private Identifier identifier;
	private Document document;
	private MongoCollection<Document> collection;

	public MongoStorageAccess(Identifier identifier, Document document, MongoCollection<Document> collection) {
		this.identifier = identifier;
		this.document = document.append("type", identifier.getType().toString()).append("_id", identifier.getUUID());
		this.collection = collection;
	}

	@Override
	public void set(String key, Object value) {
		if (key.equals("type") || key.equals("_id")) {
			throw new IllegalArgumentException("Cannot modify type or UUID of storage access once instantiated");
		}
		document.append(key, value);
		update(new Document(key, value));
	}

	@Override
	public void update(Document document) {
		this.document.putAll(document);
		rollingAsync(() -> collection.updateOne(identifier.getDocument(), new Document("$set", document)));
	}
	
	@Override
	public void delete(String key) {
		this.document.remove(key);
		rollingAsync(() -> collection.updateOne(identifier.getDocument(), new Document("$unset", new Document(key, null))));
	}

	@Override
	public Object get(String key) {
		return document.get(key);
	}
	
	@Override
	public <T> T get(String key, Class<? extends T> clazz) {
		return document.get(key, clazz);
	}
	
	@Override
	public <T> T pull(String key, Class<? extends T> clazz) {
		Document remote = collection.find(identifier.getDocument()).first();
		document.append(key, remote.get(key, clazz));
		return remote.get(key, clazz);
	}

	@Override
	public Set<Entry<String, Object>> getAll() {
		return document.entrySet();
	}

	@Override
	public Document getDocument() {
		return document;
	}

	@Override
	public Identifier getIdentifier() {
		return identifier;
	}
}
