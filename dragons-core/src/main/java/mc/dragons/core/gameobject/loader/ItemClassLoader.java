package mc.dragons.core.gameobject.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class ItemClassLoader extends GameObjectLoader<ItemClass> {
	private static ItemClassLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private GameObjectRegistry masterRegistry;

	private boolean allLoaded = false;

	private ItemClassLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		this.masterRegistry = instance.getGameObjectRegistry();
	}

	public static synchronized ItemClassLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new ItemClassLoader(instance, storageManager);
		return INSTANCE;
	}

	public ItemClass loadObject(StorageAccess storageAccess) {
		this.LOGGER.fine("Loading item class " + storageAccess.getIdentifier());
		ItemClass itemClass = new ItemClass(this.storageManager, storageAccess);
		this.masterRegistry.getRegisteredObjects().add(itemClass);
		return itemClass;
	}

	public ItemClass getItemClassByClassName(String itemClassName) {
		lazyLoadAll();
		for (GameObject gameObject : this.masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.ITEM_CLASS })) {
			ItemClass itemClass = (ItemClass) gameObject;
			if (itemClass.getClassName().equalsIgnoreCase(itemClassName))
				return itemClass;
		}
		return null;
	}

	public ItemClass registerNew(String className, String name, ChatColor nameColor, Material material, int levelMin, double cooldown, double speedBoost, boolean unbreakable, boolean undroppable,
			double damage, double armor, List<String> lore, int maxStackSize) {
		lazyLoadAll();
		this.LOGGER.fine("Registering new item class (" + className + ")");
		Document data = (new Document("_id", UUID.randomUUID())).append("className", className).append("name", name).append("nameColor", nameColor.name()).append("materialType", material.toString())
				.append("lvMin", Integer.valueOf(levelMin)).append("cooldown", Double.valueOf(cooldown)).append("unbreakable", Boolean.valueOf(unbreakable))
				.append("undroppable", Boolean.valueOf(undroppable)).append("damage", Double.valueOf(damage)).append("armor", Double.valueOf(armor)).append("speedBoost", Double.valueOf(speedBoost))
				.append("lore", lore).append("maxStackSize", Integer.valueOf(maxStackSize)).append("addons", new ArrayList<String>());
		StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.ITEM_CLASS, data);
		ItemClass itemClass = new ItemClass(this.storageManager, storageAccess);
		this.masterRegistry.getRegisteredObjects().add(itemClass);
		return itemClass;
	}

	public void loadAll(boolean force) {
		if (this.allLoaded && !force)
			return;
		this.LOGGER.fine("Loading all item classes...");
		this.allLoaded = true;
		this.masterRegistry.removeFromRegistry(GameObjectType.ITEM_CLASS);
		this.storageManager.getAllStorageAccess(GameObjectType.ITEM_CLASS).stream().forEach(storageAccess -> this.masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
