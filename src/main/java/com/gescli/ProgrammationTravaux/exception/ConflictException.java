package com.gescli.ProgrammationTravaux.exception;

/**
 * Erreur métier indiquant qu'une ressource entre en conflit avec un état existant
 * (doublon, compte déjà présent, etc.). Elle est exposée en HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
