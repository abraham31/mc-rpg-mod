package com.tuempresa.rogue.data;

/**
 * Excepción dedicada para errores al cargar configuraciones de ítems.
 */
public class ItemConfigException extends RuntimeException {
    public ItemConfigException(String message) {
        super(message);
    }

    public ItemConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
