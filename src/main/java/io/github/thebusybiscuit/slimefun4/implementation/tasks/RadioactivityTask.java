package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.cscorelib2.data.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.player.StatusEffect;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.RadioactiveItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@link RadioactivityTask} handles radioactivity for
 * {@link Radioactive} items.
 *
 * @author Semisol
 *
 */
public class RadioactivityTask implements Runnable {

    private static final Symptom[] SYMPTOMS = Symptom.values();
    private final int duration = SlimefunPlugin.getCfg().getOrSetDefault("options.radiation-update-interval", 1) * 20 + 20;
    private final PotionEffect WITHER = new PotionEffect(PotionEffectType.WITHER, duration, 1);
    private final PotionEffect WITHER2 = new PotionEffect(PotionEffectType.WITHER, duration, 4);
    private final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, duration, 0);
    private final PotionEffect SLOW = new PotionEffect(PotionEffectType.SLOW, duration, 3);
    private static StatusEffect RADIATION_EFFECT;

    {
        assert SlimefunPlugin.instance() != null;
        RADIATION_EFFECT = new StatusEffect(new NamespacedKey(SlimefunPlugin.instance(), "radiation"));
    }

    public static void removePlayer(@Nonnull Player p) {
        RADIATION_EFFECT.clear(p);
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isValid() || p.isDead()) {
                continue;
            }

            PlayerProfile.get(p, profile -> handleRadiation(p, profile));
        }
    }

    private void handleRadiation(@Nonnull Player p, @Nonnull PlayerProfile profile) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        int exposureTotal = 0;

        if (!profile.hasFullProtectionAgainst(ProtectionType.RADIATION)) {
            for (ItemStack item : p.getInventory()) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                SlimefunItem sfItem = SlimefunItem.getByItem(item);
                if (sfItem instanceof RadioactiveItem) {
                    exposureTotal += item.getAmount() * ((RadioactiveItem) sfItem).getRadioactivity().getExposureModifier();
                }
            }
        }
        
        int exposureLevelBefore = RADIATION_EFFECT.getLevel(p).orElse(0);
        if (exposureTotal > 0) {
            if (exposureLevelBefore == 0) {
                SlimefunPlugin.getLocalization().sendMessage(p, "messages.radiation");
            }
            RADIATION_EFFECT.addPermanent(p, Math.min(exposureLevelBefore + exposureTotal, 100));
        } else if (exposureLevelBefore > 0) {
            RADIATION_EFFECT.addPermanent(p, RADIATION_EFFECT.getLevel(p).orElse(1) - 1);
        }
        
        int exposureLevelAfter = RADIATION_EFFECT.getLevel(p).orElse(0);
        for (Symptom symptom : SYMPTOMS) {
            if (symptom.minExposure <= exposureLevelAfter) {
                applySymptom(symptom, p);
            }
        }
        
        if (exposureLevelAfter > 0 || exposureLevelBefore > 0) {
            String msg = SlimefunPlugin.getLocalization().getMessage(p, "actionbar.radiation")
                    .replace("%level%", "" + exposureLevelAfter);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new ComponentBuilder().append(ChatColors.color(msg)).create()
            );
        }
    }

    /**
     * Applies a symptom to the player.
     *
     * @param s Symptom to apply
     * @param p Player to apply to
     */
    private void applySymptom(@Nonnull Symptom s, @Nonnull Player p) {
        SlimefunPlugin.runSync(() -> {
            switch (s) {
                case SLOW: {
                    p.addPotionEffect(SLOW);
                    break;
                }
                case WITHER_LOW: {
                    p.addPotionEffect(WITHER);
                    break;
                }
                case BLINDNESS: {
                    p.addPotionEffect(BLINDNESS);
                    break;
                }
                case WITHER_HIGH: {
                    p.addPotionEffect(WITHER2);
                    break;
                }
                case IMMINENT_DEATH: {
                    p.setHealth(0);
                    break;
                }
            }
        });
    }

    private enum Symptom {
        /**
         * An enum of potential radiation symptoms.
         */
        SLOW(10),
        WITHER_LOW(25),
        BLINDNESS(50),
        WITHER_HIGH(75),
        IMMINENT_DEATH(100);

        private final int minExposure;

        Symptom(int minExposure) {
            this.minExposure = minExposure;
        }
    }
}
