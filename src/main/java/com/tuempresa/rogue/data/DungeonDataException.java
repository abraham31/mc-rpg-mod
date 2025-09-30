package com.tuempresa.rogue.data;

/**
 * Generic runtime exception thrown when dungeon data fails validation or cannot be parsed.
 */
public class DungeonDataException extends RuntimeException {
    public DungeonDataException(String message) {
        super(message);
    }

    public DungeonDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
