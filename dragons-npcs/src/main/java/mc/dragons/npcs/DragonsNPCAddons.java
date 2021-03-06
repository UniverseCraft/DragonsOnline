package mc.dragons.npcs;

import mc.dragons.core.Dragons;
import mc.dragons.core.DragonsJavaPlugin;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.npcs.commands.CompanionCommand;
import mc.dragons.npcs.commands.IWannaCompanionCommand;
import mc.dragons.npcs.commands.SlayCommand;
import mc.dragons.npcs.commands.TestCompanionCommand;
import mc.dragons.npcs.commands.ToggleCompanionLaunch;
import mc.dragons.npcs.model.BoneCrusherAddon;
import mc.dragons.npcs.model.PossessedWoodChipsAddon;
import mc.dragons.npcs.model.SoulStealerAddon;
import mc.dragons.npcs.model.UndeadMurdererAddon;
import mc.dragons.npcs.model.WalkingArmorStandAddon;

public class DragonsNPCAddons extends DragonsJavaPlugin {
	private Dragons dragons;
	
	public void onLoad() {
		enableDebugLogging();
		
		dragons = Dragons.getInstance();
		
		getLogger().info("Loading NPC addons...");
		AddonRegistry registry = dragons.getAddonRegistry();
		
		registry.register(new GuardAddon(dragons));
		registry.register(new EnchanterAddon());
		registry.register(new BoneCrusherAddon());
		registry.register(new UndeadMurdererAddon());
		registry.register(new SoulStealerAddon(dragons));
		registry.register(new AuraAddon());
		registry.register(new WalkingArmorStandAddon());
		registry.register(new PossessedWoodChipsAddon(dragons));
		registry.register(new CompanionAddon());
		
		dragons.getUserHookRegistry().registerHook(new NPCUserHook(this));
	}

	public void onEnable() {
		getCommand("companion").setExecutor(new CompanionCommand());
		getCommand("testcompanion").setExecutor(new TestCompanionCommand());
		getCommand("iwannacompanion").setExecutor(new IWannaCompanionCommand());
		getCommand("togglecompanionlaunch").setExecutor(new ToggleCompanionLaunch());
		getCommand("/slay").setExecutor(new SlayCommand());
	}
	
	public Dragons getDragonsInstance() {
		return dragons;
	}
}
