package io.github.thebusybiscuit.slimefun4.api.blocks;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Location;

import com.google.gson.JsonObject;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;

/**
 * This class is used to speed up parsing of a {@link JsonObject} that is stored at
 * a given {@link Location}.
 * 
 * @author TheBusyBiscuit
 * @author creator3
 * 
 * @see BlockDatabase
 *
 */
public class SlimefunBlockData extends AbstractDataObject {

    /**
     * This creates a new {@link SlimefunBlockData} object.
     * It is initialized with an empty {@link HashMap}.
     */
    public SlimefunBlockData() {
        this(new HashMap<>());
    }

    /**
     * This creates a new {@link SlimefunBlockData} object
     * and also initializes it using the given {@link Map}
     * 
     * @param data
     *            The data {@link Map}
     */
    public SlimefunBlockData(@Nonnull Map<String, String> data) {
        super(data);
    }

    @Nonnull
    public String getId() {
        return getValue("id");
    }

    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return SlimefunItem.getByID(getId());
    }

}