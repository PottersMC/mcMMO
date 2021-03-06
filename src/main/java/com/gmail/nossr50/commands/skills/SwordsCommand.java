package com.gmail.nossr50.commands.skills;

import java.util.ArrayList;
import java.util.List;

import com.gmail.nossr50.datatypes.skills.PrimarySkill;
import com.gmail.nossr50.datatypes.skills.SubSkill;
import com.gmail.nossr50.util.SkillTextComponentFactory;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.swords.Swords;
import com.gmail.nossr50.util.Permissions;

public class SwordsCommand extends SkillCommand {
    private String counterChance;
    private String counterChanceLucky;
    private int bleedLength;
    private String bleedChance;
    private String bleedChanceLucky;
    private String serratedStrikesLength;
    private String serratedStrikesLengthEndurance;

    private boolean canCounter;
    private boolean canSerratedStrike;
    private boolean canBleed;

    public SwordsCommand() {
        super(PrimarySkill.SWORDS);
    }

    @Override
    protected void dataCalculations(Player player, float skillValue, boolean isLucky) {
        // SERRATED STRIKES
        if (canSerratedStrike) {
            String[] serratedStrikesStrings = calculateLengthDisplayValues(player, skillValue);
            serratedStrikesLength = serratedStrikesStrings[0];
            serratedStrikesLengthEndurance = serratedStrikesStrings[1];
        }

        // SWORDS_BLEED
        if (canBleed) {
            bleedLength = (skillValue >= AdvancedConfig.getInstance().getMaxBonusLevel(SubSkill.SWORDS_BLEED)) ? Swords.bleedMaxTicks : Swords.bleedBaseTicks;

            String[] bleedStrings = calculateAbilityDisplayValues(skillValue, SubSkill.SWORDS_BLEED, isLucky);
            bleedChance = bleedStrings[0];
            bleedChanceLucky = bleedStrings[1];
        }

        // SWORDS_COUNTER_ATTACK
        if (canCounter) {
            String[] counterStrings = calculateAbilityDisplayValues(skillValue, SubSkill.SWORDS_COUNTER_ATTACK, isLucky);
            counterChance = counterStrings[0];
            counterChanceLucky = counterStrings[1];
        }
    }

    @Override
    protected void permissionsCheck(Player player) {
        canBleed = Permissions.isSubSkillEnabled(player, SubSkill.SWORDS_BLEED);
        canCounter = Permissions.isSubSkillEnabled(player, SubSkill.SWORDS_COUNTER_ATTACK);
        canSerratedStrike = Permissions.serratedStrikes(player);
    }

    @Override
    protected List<String> effectsDisplay() {
        List<String> messages = new ArrayList<String>();

        if (canCounter) {
            messages.add(LocaleLoader.getString("Effects.Template", LocaleLoader.getString("Swords.Effect.0"), LocaleLoader.getString("Swords.Effect.1", percent.format(1.0D / Swords.counterAttackModifier))));
        }

        if (canSerratedStrike) {
            messages.add(LocaleLoader.getString("Effects.Template", LocaleLoader.getString("Swords.Effect.2"), LocaleLoader.getString("Swords.Effect.3", percent.format(1.0D / Swords.serratedStrikesModifier))));
            messages.add(LocaleLoader.getString("Effects.Template", LocaleLoader.getString("Swords.Effect.4"), LocaleLoader.getString("Swords.Effect.5", Swords.serratedStrikesBleedTicks)));
        }

        if (canBleed) {
            messages.add(LocaleLoader.getString("Effects.Template", LocaleLoader.getString("Swords.Effect.6"), LocaleLoader.getString("Swords.Effect.7")));
        }

        return messages;
    }

    @Override
    protected List<String> statsDisplay(Player player, float skillValue, boolean hasEndurance, boolean isLucky) {
        List<String> messages = new ArrayList<String>();

        if (canCounter) {
            messages.add(LocaleLoader.getString("Swords.Combat.Counter.Chance", counterChance) + (isLucky ? LocaleLoader.getString("Perks.Lucky.Bonus", counterChanceLucky) : ""));
        }

        if (canBleed) {
            messages.add(LocaleLoader.getString("Swords.Combat.Bleed.Length", bleedLength));
            messages.add(LocaleLoader.getString("Swords.Combat.Bleed.Note"));
            messages.add(LocaleLoader.getString("Swords.Combat.Bleed.Chance", bleedChance) + (isLucky ? LocaleLoader.getString("Perks.Lucky.Bonus", bleedChanceLucky) : ""));
        }

        if (canSerratedStrike) {
            messages.add(LocaleLoader.getString("Swords.SS.Length", serratedStrikesLength) + (hasEndurance ? LocaleLoader.getString("Perks.ActivationTime.Bonus", serratedStrikesLengthEndurance) : ""));
        }

        return messages;
    }

    @Override
    protected List<TextComponent> getTextComponents(Player player) {
        List<TextComponent> textComponents = new ArrayList<>();

        SkillTextComponentFactory.getSubSkillTextComponents(player, textComponents, PrimarySkill.SWORDS);

        return textComponents;
    }
}
