package mc.dragons.tools.content;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.addon.Addon;
import mc.dragons.tools.content.addon.NPCIdentifierAddon;
import mc.dragons.tools.content.command.PlaceholderCommand;
import mc.dragons.tools.content.command.builder.ClearInventoryCommand;
import mc.dragons.tools.content.command.builder.FixedCommand;
import mc.dragons.tools.content.command.builder.GamemodeCommand;
import mc.dragons.tools.content.command.builder.GoToFloorCommand;
import mc.dragons.tools.content.command.builder.SpeedCommand;
import mc.dragons.tools.content.command.builder.WarpCommands;
import mc.dragons.tools.content.command.gameobject.FloorCommand;
import mc.dragons.tools.content.command.gameobject.ItemCommand;
import mc.dragons.tools.content.command.gameobject.NPCCommand;
import mc.dragons.tools.content.command.gameobject.ObjectMetadataCommand;
import mc.dragons.tools.content.command.gameobject.QuestCommand;
import mc.dragons.tools.content.command.gameobject.RegionCommand;
import mc.dragons.tools.content.command.internal.DeleteNPCCommand;
import mc.dragons.tools.content.command.statistics.ReloreCommand;
import mc.dragons.tools.content.command.statistics.RenameCommand;
import mc.dragons.tools.content.command.statistics.ResetProfileCommand;
import mc.dragons.tools.content.command.statistics.RestatCommand;
import mc.dragons.tools.content.command.statistics.UpdateStatsCommand;
import mc.dragons.tools.content.command.testing.TestQuestCommand;
import mc.dragons.tools.content.event.PlayerChangedWorldListener;

public class DragonsContentTools extends DragonsJavaPlugin {
	public static String PUSH_FOLDER;
	
	public void onEnable() {
		enableDebugLogging();
		saveDefaultConfig();
		
		PUSH_FOLDER = getConfig().getString("push-folder", "C:\\DragonsPush\\");
		
		Dragons dragons = getDragonsInstance();
		dragons.getLightweightLoaderRegistry().register(new AuditLogLoader(dragons.getMongoConfig()));
		
		getCommand("region").setExecutor(new RegionCommand());
		getCommand("npc").setExecutor(new NPCCommand());
		getCommand("item").setExecutor(new ItemCommand());
		getCommand("floor").setExecutor(new FloorCommand());
		getCommand("clear").setExecutor(new ClearInventoryCommand());
		getCommand("testquest").setExecutor(new TestQuestCommand());
		getCommand("quest").setExecutor(new QuestCommand());
		getCommand("rename").setExecutor(new RenameCommand());
		getCommand("relore").setExecutor(new ReloreCommand());
		getCommand("restat").setExecutor(new RestatCommand());
		getCommand("resetprofile").setExecutor(new ResetProfileCommand());
		getCommand("placeholder").setExecutor(new PlaceholderCommand());
		getCommand("fixed").setExecutor(new FixedCommand());
		getCommand("objmeta").setExecutor(new ObjectMetadataCommand());
		getCommand("deletenpc").setExecutor(new DeleteNPCCommand());
		
		CommandExecutor gamemodeCommandExecutor = new GamemodeCommand();
		getCommand("gamemode").setExecutor(gamemodeCommandExecutor);
		getCommand("gma").setExecutor(gamemodeCommandExecutor);
		getCommand("gmc").setExecutor(gamemodeCommandExecutor);
		getCommand("gms").setExecutor(gamemodeCommandExecutor);
		
		getCommand("gotofloor").setExecutor(new GoToFloorCommand());
		getCommand("updatestats").setExecutor(new UpdateStatsCommand());
		CommandExecutor speedCommand = new SpeedCommand();
		getCommand("speed").setExecutor(speedCommand);
		getCommand("flyspeed").setExecutor(speedCommand);
		getCommand("walkspeed").setExecutor(speedCommand);
		
		CommandExecutor warpCommandsExecutor = new WarpCommands();
		getCommand("delwarp").setExecutor(warpCommandsExecutor);
		getCommand("setwarp").setExecutor(warpCommandsExecutor);
		getCommand("warp").setExecutor(warpCommandsExecutor);
		getCommand("warps").setExecutor(warpCommandsExecutor);
		
		Addon npcIdentifierAddon = new NPCIdentifierAddon();
		dragons.getAddonRegistry().register(npcIdentifierAddon);
		npcIdentifierAddon.apply();
		
		Bukkit.getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
	}
}
