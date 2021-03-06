package mc.dragons.core.logging;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.util.StringUtil;

/**
 * Custom log levels for Dragons Online.
 * 
 * @author Adam
 *
 */
public class LogLevel extends Level {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3411290874107041049L;

	public static final Level NOTICE = new LogLevel("NOTICE", 850);
	public static final Level DEBUG = new LogLevel("DEBUG", 600);
	public static final Level TRACE = new LogLevel("TRACE", 550);
	public static final Level VERBOSE = new LogLevel("VERBOSE", 100);

	private LogLevel(String name, int value) {
		super(name, value);
	}
	
	public static void setupLog4jParallels() {
		registerLevel("NOTICE", 350);
		registerLevel("CONFIG", 450);
		registerLevel("FINE", 700);
		registerLevel("FINER", 800);
		registerLevel("FINEST", 900);
		registerLevel("VERBOSE", 1000);
	}
	
	private static void registerLevel(String name, int level) {
		org.apache.logging.log4j.Level.forName(name, level);
	}
	
	public static org.apache.logging.log4j.Level log4j(String name) {
		return org.apache.logging.log4j.Level.getLevel(name);
	}

	/**
	 * Marshal between JUL and log4j levels.
	 * 
	 * @param level The JUL level
	 * @return The corresponding log4j level
	 */
	public static org.apache.logging.log4j.Level fromJUL(Level level) {
		if (level == Level.SEVERE) {
			return org.apache.logging.log4j.Level.ERROR;
		}
		else if (level == Level.WARNING) {
			return org.apache.logging.log4j.Level.WARN;
		}
		else {
			return log4j(level.getName());
		}
	}
	
	/**
	 * Marshal between JUL and log4j levels.
	 * 
	 * @param level The log4j level
	 * @return The corresponding JUL level
	 */
	public static Level fromLog4j(org.apache.logging.log4j.Level level) {
		if(level == org.apache.logging.log4j.Level.ERROR) {
			return Level.SEVERE;
		}
		else if(level == org.apache.logging.log4j.Level.WARN) {
			return Level.WARNING;
		}
		else {
			return Level.parse(level.toString());
		}
	}
	
	public static final Level parseLevel(CommandSender sender, String level) {
		try {
			return Level.parse(level);
		}
		catch(Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid log level! Valid levels are " + StringUtil.parseList(getApprovedLevels(), ", "));
			return null;
		}
	}
	
	public static Level[] getApprovedLevels() {
		return new Level[] { LogLevel.SEVERE, LogLevel.WARNING, LogLevel.NOTICE, LogLevel.INFO, LogLevel.CONFIG, LogLevel.DEBUG, LogLevel.TRACE, LogLevel.VERBOSE };
	}
}
