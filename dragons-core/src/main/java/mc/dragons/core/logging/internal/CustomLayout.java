package mc.dragons.core.logging.internal;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import mc.dragons.core.gameobject.user.UserLoader;
import net.md_5.bungee.api.ChatColor;

/**
 * The custom log4j layout used for logging "under the hood."
 * 
 * @author Adam
 *
 */
public class CustomLayout extends AbstractStringLayout {
	private final String DAY_PATTERN = Pattern.quote("%d");
	private final String TIME_PATTERN = Pattern.quote("%t");
	private final String LEVEL_PATTERN = Pattern.quote("%l");
	private final String SOURCE_PATTERN = Pattern.quote("%s");
	private final String MESSAGE_PATTERN = Pattern.quote("%m");
	
	private boolean hideEmpty = true;
	private boolean hideMinecraft = true;
	private boolean truncatePackageNames = true;
	private boolean specialPackageNames = true;
	private String formatWithLogger = "[%t %l] [%s] %m";
	private String formatWithoutLogger = "[%t %l] %m";

	public CustomLayout(Charset charset) {
		super(charset);
	}

	private String format(String pattern, String date, String time, String level, String source, String message) {
		return pattern.replaceAll(DAY_PATTERN, date).replaceAll(TIME_PATTERN, time).replaceAll(LEVEL_PATTERN, level).replaceAll(SOURCE_PATTERN, source)
				.replaceAll(MESSAGE_PATTERN, Matcher.quoteReplacement(message == null ? "" : message));
	}

	@Override
	public String toSerializable(LogEvent logEvent) {
		ReadOnlyStringMap ctx = logEvent.getContextData();
		String level = ctx.containsKey("OriginalLevel") ? ctx.getValue("OriginalLevel") : logEvent.getLevel().toString();
		String loggerName = logEvent.getLoggerName();
		if (loggerName.contains(".")) {
			int lastIndex = loggerName.lastIndexOf(".");
			String mostSpecific = loggerName.substring(lastIndex + 1);
			boolean special = false;
			if (specialPackageNames) {
				if (loggerName.contains("net.minecraft.server")) {
					loggerName = "NMS." + mostSpecific;
					special = true;
				} else if (loggerName.contains("org.bukkit.craftbukkit")) {
					loggerName = "OBC." + mostSpecific;
					special = true;
				} else if (loggerName.contains("org.mongodb.driver")) {
					loggerName = "OMD." + mostSpecific;
					special = true;
				}
			}
			if (!special && truncatePackageNames) {
				String truncatedLoggerName = String.valueOf(loggerName.charAt(0)) + ".";
				int dotIndex = loggerName.indexOf(".", 0);
				while (dotIndex != lastIndex) {
					truncatedLoggerName = String.valueOf(truncatedLoggerName) + loggerName.charAt(dotIndex + 1) + ".";
					dotIndex = loggerName.indexOf(".", dotIndex + 1);
				}
				truncatedLoggerName = String.valueOf(truncatedLoggerName) + mostSpecific;
				loggerName = truncatedLoggerName;
			}
		}
		boolean includeLogger = true;
		if (hideMinecraft && (loggerName.equals("Minecraft") || loggerName.contains("MinecraftServer") || loggerName.contains("DedicatedServer"))) {
			includeLogger = false;
		}
		if (hideEmpty && loggerName.length() == 0) {
			includeLogger = false;
		}
		String timestamp = new SimpleDateFormat("HH:mm:ss").format(logEvent.getTimeMillis());
		String datestamp = new SimpleDateFormat("yyyy-MM-dd").format(logEvent.getTimeMillis());
		String message = ChatColor.stripColor(logEvent.getMessage().getFormattedMessage());
		if (logEvent.getThrown() != null) {
			Throwable buf = logEvent.getThrown();
			while (buf != null) {
				message += "\n" + format(includeLogger ? formatWithLogger : formatWithoutLogger, datestamp, timestamp, level, loggerName,
						String.valueOf(buf.getClass().getName()) + ": " + buf.getMessage());
				for(StackTraceElement elem : buf.getStackTrace()) {
					message += "\n"	+ format(includeLogger ? formatWithLogger : formatWithoutLogger, datestamp, timestamp, level, loggerName, "    " + elem.toString());
				}
				buf = buf.getCause();
				if (buf != null) {
					message += "\n" + format(includeLogger ? formatWithLogger : formatWithoutLogger, datestamp, timestamp, level, loggerName, "Caused by:");
				}
			}
		}
		final String finalMessage;
		if (includeLogger) {
			finalMessage = format(formatWithLogger, datestamp, timestamp, level, loggerName, message);
		} else {
			finalMessage = format(formatWithoutLogger, datestamp, timestamp, level, loggerName, message);
		}
	
		if(logEvent.getLevel() == Level.ERROR || logEvent.getLevel() == Level.FATAL) {
			UserLoader.allUsers().stream().filter(u -> u.isDebuggingErrors()).forEach(u -> u.getPlayer().sendMessage(finalMessage));
		}
		
		return finalMessage;
	}
}