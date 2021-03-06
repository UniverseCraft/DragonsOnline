package mc.dragons.core.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

/**
 * Periodically updates users' sidebars with current contextual data.
 * 
 * @author Adam
 *
 */
public class UpdateScoreboardTask extends BukkitRunnable {
	private Dragons plugin;

	public UpdateScoreboardTask(Dragons instance) {
		plugin = instance;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			plugin.getSidebarManager().updateScoreboard(player);
		}
	}
}
