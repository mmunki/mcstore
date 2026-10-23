package dev.mmunki.mcstore.config;

import dev.mmunki.mcstore.economy.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDefaults() throws IOException {
        Path file = tempDir.resolve("config.json");
        StoreConfig config = StoreConfig.loadOrCreate(file);
        assertEquals(Money.STARTING_BALANCE_CENTS, config.startingBalanceCents());
        assertTrue(config.refreshOpenGuis());
        assertEquals(600, config.restockSeconds());
        assertEquals(100L, config.xpCentsPerLevel());
        assertTrue(Files.exists(file));
    }

    @Test
    void readsHumanEditableValues() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, """
                {
                  "startingBalance": 250.75,
                  "refreshOpenGuis": false,
                  "restockSeconds": 120,
                  "xpCentsPerLevel": 2.50
                }
                """);
        StoreConfig config = StoreConfig.loadOrCreate(file);
        assertEquals(25_075L, config.startingBalanceCents());
        assertFalse(config.refreshOpenGuis());
        assertEquals(120, config.restockSeconds());
        assertEquals(250L, config.xpCentsPerLevel());
    }

    @Test
    void adminsFileAcceptsBareAndWrappedArrays() throws IOException {
        Path wrapped = tempDir.resolve("admins.json");
        Files.writeString(wrapped, """
                {"admins": ["00000000-0000-0000-0000-000000000001"]}
                """);
        AdminList list = AdminList.loadOrCreate(wrapped);
        assertTrue(list.contains(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")));

        Path bare = tempDir.resolve("admins-bare.json");
        Files.writeString(bare, "[\"00000000-0000-0000-0000-000000000002\"]");
        AdminList bareList = AdminList.loadOrCreate(bare);
        assertTrue(bareList.contains(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")));
    }
}
