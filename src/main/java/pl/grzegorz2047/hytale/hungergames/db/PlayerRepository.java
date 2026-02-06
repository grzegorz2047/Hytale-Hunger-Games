package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfejs do zarządzania graczami i ich statystykami w bazie danych.
 */
public interface PlayerRepository {
    default void initialize() throws Exception {}

    /**
     * Tworzenie nowego gracza w bazie danych
     */
    void save(HgPlayer player) throws Exception;

    /**
     * Aktualizowanie istniejącego gracza w bazie danych
     */
    void update(HgPlayer player) throws Exception;

    /**
     * Pobieranie gracza po UUID
     */
    Optional<HgPlayer> findByUuid(UUID uuid) throws Exception;

    /**
     * Sprawdzenie czy gracz istnieje
     */
    boolean exists(UUID uuid) throws Exception;

    /**
     * Usunięcie gracza z bazy
     */
    void delete(UUID uuid) throws Exception;
}
