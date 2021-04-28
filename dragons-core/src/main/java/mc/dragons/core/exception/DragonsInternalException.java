package mc.dragons.core.exception;

import java.util.UUID;
import java.util.logging.Level;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

/**
 * Exception class for internal errors with DragonsOnline.
 * 
 * @author Adam
 *
 */
public class DragonsInternalException extends RuntimeException {
	private static final long serialVersionUID = -7426259903091848617L;	
	protected static final DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	public DragonsInternalException(String message, Exception ex) {
		super(message, ex);
	}
	
	public DragonsInternalException(String message, UUID correlationID) {
		super(message);
		LOGGER.log(correlationID, Level.SEVERE, "[EXCEPTION] " + message);
	}


}
