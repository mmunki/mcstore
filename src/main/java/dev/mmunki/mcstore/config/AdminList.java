package dev.mmunki.mcstore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Extra admins that are allowed to use /mcstore even without op level 2.
 * The file accepts either {"admins": ["uuid", ...]} or a bare array.
 */
public final class AdminList {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Set<UUID> admins = new TreeSet<>();

    public static AdminList loadOrCreate(Path path) throws IOException {
        AdminList list = new AdminList();
        if (Files.exists(path)) {
            try {
                JsonElement root = JsonParser.parseString(JsonFiles.read(path));
                JsonArray array;
                if (root.isJsonObject() && root.getAsJsonObject().has("admins")) {
                    array = root.getAsJsonObject().getAsJsonArray("admins");
                } else if (root.isJsonArray()) {
                    array = root.getAsJsonArray();
                } else {
                    array = new JsonArray();
                }
                for (JsonElement element : array) {
                    try {
                        list.admins.add(UUID.fromString(element.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                        // one malformed UUID must not stop the server
                    }
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid admins.json: " + e.getMessage(), e);
            }
        }
        list.save(path);
        return list;
    }

    public void save(Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Extra MCStore admins by UUID. Op level 2 (GAMEMASTER) always has access.");
        JsonArray array = new JsonArray();
        for (UUID admin : admins) {
            array.add(admin.toString());
        }
        root.add("admins", array);
        JsonFiles.writeAtomic(path, GSON.toJson(root) + System.lineSeparator());
    }

    public boolean contains(UUID uuid) {
        return uuid != null && admins.contains(uuid);
    }

    public Set<UUID> all() {
        return Collections.unmodifiableSet(admins);
    }

    public boolean add(UUID uuid) {
        return admins.add(uuid);
    }

    public boolean remove(UUID uuid) {
        return admins.remove(uuid);
    }
}
