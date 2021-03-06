package mc.dragons.core.storage.local;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Manages non-persistent, in-memory data storage for game objects.
 * 
 * @author Adam
 *
 */
public class LocalStorageManager implements StorageManager {
	private Map<Identifier, LocalStorageAccess> localStorageAccesses = new HashMap<>();

	@Override
	public StorageAccess getStorageAccess(GameObjectType objectType, UUID objectUUID) {
		return localStorageAccesses.get(new Identifier(objectType, objectUUID));
	}

	@Override
	public StorageAccess getStorageAccess(GameObjectType objectType, Document search) {
		for (StorageAccess storageAccess : localStorageAccesses.values()) {
			if (storageAccess.getIdentifier().getType() == objectType) {
				for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) search.entrySet()) {
					if (!search.get(entry.getKey()).equals(entry.getValue())) {
						continue;
					}
				}
				return storageAccess;
			}
		}
		return null;
	}

	@Override
	public Set<StorageAccess> getAllStorageAccess(GameObjectType objectType) {
		Set<StorageAccess> results = new HashSet<>();
		for (StorageAccess storageAccess : localStorageAccesses.values()) {
			if (storageAccess.getIdentifier().getType() == objectType) {
				results.add(storageAccess);
			}
		}
		return results;
	}

	@Override
	public Set<StorageAccess> getAllStorageAccess(GameObjectType objectType, Document filter) {
		Set<StorageAccess> results = new HashSet<>();
		for (StorageAccess storageAccess : localStorageAccesses.values()) {
			if (storageAccess.getIdentifier().getType() == objectType) {
				for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) filter.entrySet()) {
					if (!filter.get(entry.getKey()).equals(entry.getValue())) {
						continue;
					}
					results.add(storageAccess);
				}
			}
		}
		return results;
	}

	@Override
	public StorageAccess getNewStorageAccess(GameObjectType objectType) {
		LocalStorageAccess storageAccess = new LocalStorageAccess(objectType, new Document());
		localStorageAccesses.put(storageAccess.getIdentifier(), storageAccess);
		return storageAccess;
	}

	@Override
	public StorageAccess getNewStorageAccess(GameObjectType objectType, UUID objectUUID) {
		LocalStorageAccess storageAccess = new LocalStorageAccess(new Identifier(objectType, objectUUID), new Document());
		localStorageAccesses.put(storageAccess.getIdentifier(), storageAccess);
		return storageAccess;
	}

	@Override
	public StorageAccess getNewStorageAccess(GameObjectType objectType, Document initialData) {
		LocalStorageAccess storageAccess = new LocalStorageAccess(objectType, initialData);
		localStorageAccesses.put(storageAccess.getIdentifier(), storageAccess);
		return storageAccess;
	}

	@Override
	public void storeObject(GameObject gameObject) {
		if (gameObject.getStorageAccess() instanceof LocalStorageAccess) {
			localStorageAccesses.put(gameObject.getIdentifier(), (LocalStorageAccess) gameObject.getStorageAccess());
		}
	}

	@Override
	public void removeObject(GameObject gameObject) {
		localStorageAccesses.remove(gameObject.getIdentifier());
	}

	public LocalStorageAccess downgrade(StorageAccess storageAccess) {
		LocalStorageAccess localStorageAccess = new LocalStorageAccess(storageAccess.getIdentifier(), storageAccess.getDocument());
		localStorageAccesses.put(localStorageAccess.getIdentifier(), localStorageAccess);
		return localStorageAccess;
	}

	@Override
	public void push(GameObjectType objectType, Document selector, Document update) {
		for (StorageAccess storageAccess : localStorageAccesses.values()) {
			if (storageAccess.getIdentifier().getType() == objectType) {
				for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) selector.entrySet()) {
					if (!selector.get(entry.getKey()).equals(entry.getValue())) {
						continue;
					}
					storageAccess.update(update);
				}
			}
		}
	}
}
