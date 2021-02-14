package mc.dragons.social;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.social.GuildLoader.Guild;

/**
 * Loads guilds from MongoDB. Guilds are pooled locally to reduce network usage.
 * 
 * @author Adam
 *
 */
public class GuildLoader extends AbstractLightweightLoader<Guild> {
	private static final String GUILD_COLLECTION = "guilds";
	
	private Map<Integer, Guild> guildPool = new HashMap<>();
	
	public static enum GuildAccessLevel {
		ALL("Public"),
		REQUEST("Request-to-join"),
		INVITE("Invite-only"),
		UNLISTED("Unlisted");
		
		private String friendlyName;
		
		GuildAccessLevel(String friendlyName) {
			this.friendlyName = friendlyName;
		}
		
		public String friendlyName() { return friendlyName; }
	}
	
	public static enum GuildThemeColor {
		GRAY(ChatColor.DARK_GRAY, ChatColor.GRAY, ChatColor.GRAY, 0),
		GREEN(ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.DARK_GREEN, 1000),
		BLUE(ChatColor.BLUE, ChatColor.AQUA, ChatColor.BLUE, 5000),
		GOLD(ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GOLD, 20000),
		RED(ChatColor.DARK_RED, ChatColor.RED, ChatColor.RED, 50000),
		PURPLE(ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE, 100000);
		
		public static final GuildThemeColor DEFAULT = GRAY;
		
		private ChatColor primary;
		private ChatColor secondary;
		private ChatColor tag;
		private int xpreq;
		
		GuildThemeColor(ChatColor primary, ChatColor secondary, ChatColor tag, int xpreq) {
			this.primary = primary;
			this.secondary = secondary;
			this.tag = tag;
			this.xpreq = xpreq;
		}
		
		public ChatColor primary() { return primary; }
		public ChatColor secondary() { return secondary; }
		public ChatColor tag() { return tag; }
		public int xpreq() { return xpreq; }
		
	}
	
	public static enum GuildEvent {
		JOIN,
		LEAVE,
		KICK,
		BAN,
		UNBAN,
		TRANSFER_OWNERSHIP
	}
	
	/**
	 * A guild is a named group of users. Users can belong to multiple guilds.
	 * Guilds are uniquely and stably identified by an integral ID, and are
	 * uniquely but possibly unstably identified by their name, which must not
	 * contain spaces.
	 * 
	 * Inspired by Hypixel's guild system, adapted for RPG usage.
	 * 
	 * @author Adam
	 *
	 */
	public static class Guild {
		private static GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
		private Document data;
		
		public Guild(Document data) {
			this.data = data;
		}
		
		public int getId() { return data.getInteger("_id"); }
		public String getName() { return data.getString("name"); }
		public UUID getOwner() { return data.get("owner", UUID.class); }
		public String getDescription() { return data.getString("description"); }
		public String getMOTD() { return data.getString("motd"); }
		public int getXP() { return data.getInteger("xp"); }
		public List<UUID> getMembers() { return data.getList("members", UUID.class); }
		public List<UUID> getBlacklist() { return data.getList("blacklist", UUID.class); }
		public List<UUID> getPending() { return data.getList("pending", UUID.class); }
		public List<UUID> getInvited() { return data.getList("invited", UUID.class); }
		public GuildAccessLevel getAccessLevel() { return GuildAccessLevel.valueOf(data.getString("accessLevel")); }
		public Date getCreatedOn() { return new Date(Instant.ofEpochSecond(data.getLong("createdOn")).toEpochMilli()); }
		public GuildThemeColor getThemeColor() { return GuildThemeColor.valueOf(data.getString("themeColor")); }
		
		public void update(GuildEvent event, User target) {
			String message = "";
			ChatColor primary = getThemeColor().primary();
			ChatColor secondary = getThemeColor().secondary();
			switch(event) {
			case JOIN:
				message = primary + target.getName() + secondary + " joined " + primary + getName() + secondary + "!";
				break;
			case LEAVE:
				message = primary + target.getName() + secondary + " left " + primary + getName() + secondary + "!";
				break;
			case KICK:
				message = primary + target.getName() + secondary + " was kicked from " + primary + getName() + secondary + "!";
				break;
			case BAN:
				message = primary + target.getName() + secondary + " was banned from " + primary + getName() + secondary + "!";
				break;
			case UNBAN:
				message = primary + target.getName() + secondary + " was unbanned from " + primary + getName() + secondary + "!";
				break;
			case TRANSFER_OWNERSHIP:
				message = secondary + "Ownership of " + primary + getName() + secondary + " was transferred to " + primary + target.getName() + secondary + "!";
				break;
			default:
				message = ChatColor.RED + "An error occurred sending a guild notification: " + event + "{" + getName() + "," + target.getName() + "}";
				break;
			}
			for(UUID uuid : getMembers()) {
				Player recipient = Bukkit.getPlayer(uuid);
				if(recipient == null) continue;
				recipient.sendMessage(message);
			}
			Player owner = Bukkit.getPlayer(getOwner());
			if(owner != null) {
				owner.sendMessage(message);
			}
		}
		
		public void save() { guildLoader.updateGuild(this); }
		
		public void setDescription(String description) {
			data.append("description", description);
			save();
		}
		
		public void setMOTD(String motd) {
			data.append("motd", motd);
			save();
		}
		
		public void setXP(int xp) {
			data.append("xp", xp);
			save();
		}
		
		public void addXP(int xp) {
			data.append("xp", getXP() + xp);
			save();
		}
		
		public void setAccessLevel(GuildAccessLevel accessLevel) {
			data.append("accessLevel", accessLevel.toString());
			save();
		}
		
		public void setOwner(UUID owner) {
			data.append("owner", owner);
			save();
		}
		
		public boolean setThemeColor(GuildThemeColor themeColor) {
			if(getXP() < themeColor.xpreq()) {
				return false;
			}
			data.append("themeColor", themeColor.toString());
			save();
			return true;
		}
		
		public Document toDocument() {
			return data;
		}
		
		public static Guild fromDocument(Document document) {
			if(document == null) return null;
			int id = document.getInteger("_id");
			if(guildLoader.guildPool.containsKey(id)) return guildLoader.guildPool.get(id);
			Guild guild = new Guild(document);
			guildLoader.guildPool.put(id, guild);
			return guild;
		}
		
		public static Guild newGuild(String name, UUID owner) {
			if(guildLoader.getGuildByName(name) != null) return null;
			int id = guildLoader.reserveNextId();
			Guild guild = new Guild(new Document("_id", id)
					.append("name", name)
					.append("owner", owner)
					.append("description", "")
					.append("motd", "")
					.append("xp", 0)
					.append("members", new ArrayList<String>())
					.append("blacklist", new ArrayList<String>())
					.append("pending", new ArrayList<String>())
					.append("invited", new ArrayList<String>())
					.append("accessLevel", GuildAccessLevel.INVITE.toString())
					.append("createdOn", Instant.now().getEpochSecond())
					.append("themeColor", GuildThemeColor.DEFAULT.toString()));
			guildLoader.guildPool.put(id, guild);
			guildLoader.collection.insertOne(guild.toDocument());
			return guild;
		}
	}
	
	public GuildLoader(MongoConfig config) {
		super(config, "guilds", GUILD_COLLECTION);
	}

	public List<Guild> asGuilds(FindIterable<Document> guilds) {
		List<Document> result = new ArrayList<>();
		for(Document guild : guilds) {
			result.add(guild);
		}
		return asGuilds(result);
	}
	
	public List<Guild> asGuilds(List<Document> guilds) {
		return guilds.stream().map(doc -> Guild.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public List<Guild> getAllGuilds() {
		return asGuilds(collection.find());
	}
	
	public List<Guild> getAllGuildsWith(UUID uuid) {
		return asGuilds(collection.find(new Document("$or", Arrays.asList(new Document("members", uuid), new Document("owner", uuid)))));
	}
	
	public Guild getGuildByName(String name) {
		for(Guild guild : guildPool.values()) {
			if(guild.getName().equalsIgnoreCase(name)) {
				return guild;
			}
		}
		FindIterable<Document> result = collection.find(new Document("name", name));
		if(result.first() == null) return null;
		return Guild.fromDocument(result.first());
	}
	
	public Guild getGuildById(int id) {
		return guildPool.computeIfAbsent(id, guildId -> Guild.fromDocument(collection.find(new Document("_id", guildId)).first()));
	}
	
	public void updateGuild(Guild update) {
		collection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public Guild addGuild(UUID owner, String name) {
		return Guild.newGuild(name.replaceAll(Pattern.quote(" "), ""), owner);
	}
	
	public void deleteGuild(int id) {
		collection.deleteOne(new Document("_id", id));
		guildPool.remove(id);
	}
	
	public int getLatestGuildId() {
		return getCurrentMaxId();
	}

}
