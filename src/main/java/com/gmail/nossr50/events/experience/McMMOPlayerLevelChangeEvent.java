package com.gmail.nossr50.events.experience;

import com.gmail.nossr50.datatypes.skills.PrimarySkill;
import org.bukkit.entity.Player;

import com.gmail.nossr50.datatypes.skills.XPGainReason;

/**
 * Called when a user levels change
 */
public abstract class McMMOPlayerLevelChangeEvent extends McMMOPlayerExperienceEvent {
    @Deprecated
    public McMMOPlayerLevelChangeEvent(Player player, PrimarySkill skill) {
        super(player, skill, XPGainReason.UNKNOWN);
    }

    public McMMOPlayerLevelChangeEvent(Player player, PrimarySkill skill, XPGainReason xpGainReason) {
        super(player, skill, xpGainReason);
    }
}
