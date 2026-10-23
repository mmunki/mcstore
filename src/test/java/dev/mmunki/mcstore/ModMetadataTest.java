package dev.mmunki.mcstore;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single most important packaging property of this mod is that it is not
 * marked as a server-only mod.  With "environment": "server" Fabric Loader
 * silently skips the jar on a client, so singleplayer worlds never get the
 * commands or the GUI.  These checks keep that regression out.
 */
class ModMetadataTest {

    private static JsonObject metadata() {
        try (InputStream stream = ModMetadataTest.class.getResourceAsStream("/fabric.mod.json")) {
            assertTrue(stream != null, "fabric.mod.json is missing from the jar resources");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("could not read fabric.mod.json", e);
        }
    }

    @Test
    void loadsOnBothClientAndServer() {
        assertEquals("*", metadata().get("environment").getAsString());
    }

    @Test
    void hasACommonMainEntrypoint() {
        JsonArray main = metadata().getAsJsonObject("entrypoints").getAsJsonArray("main");
        assertTrue(main != null && !main.isEmpty(), "a main entrypoint is required");
        assertEquals("dev.mmunki.mcstore.MCStoreMod", main.get(0).getAsString());
    }

    @Test
    void modVersionMatchesTheBuildVersion() throws Exception {
        try (InputStream stream = ModMetadataTest.class.getResourceAsStream("/fabric.mod.json")) {
            assertTrue(stream != null);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals(McStore.VERSION, root.get("version").getAsString());
        }
    }
}
