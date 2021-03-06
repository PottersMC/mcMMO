package com.gmail.nossr50.skills.herbalism;

import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.config.treasure.TreasureConfig;
import com.gmail.nossr50.datatypes.mods.CustomBlock;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.*;
import com.gmail.nossr50.datatypes.treasure.HylianTreasure;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.runnables.skills.HerbalismBlockUpdaterTask;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.*;
import com.gmail.nossr50.util.skills.SubSkillActivationType;
import com.gmail.nossr50.util.skills.SkillUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.List;

public class HerbalismManager extends SkillManager {
    public HerbalismManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, PrimarySkill.HERBALISM);
    }

    public boolean canBlockCheck() {
        return !(Config.getInstance().getHerbalismPreventAFK() && getPlayer().isInsideVehicle());
    }

    public boolean canGreenThumbBlock(BlockState blockState) {
        Player player = getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        return item.getAmount() > 0 && item.getType() == Material.WHEAT_SEEDS && BlockUtils.canMakeMossy(blockState) && Permissions.greenThumbBlock(player, blockState.getType());
    }

    public boolean canUseShroomThumb(BlockState blockState) {
        Player player = getPlayer();
        PlayerInventory inventory = player.getInventory();
        Material itemType = inventory.getItemInMainHand().getType();

        return (itemType == Material.BROWN_MUSHROOM || itemType == Material.RED_MUSHROOM) && inventory.contains(Material.BROWN_MUSHROOM, 1) && inventory.contains(Material.RED_MUSHROOM, 1) && BlockUtils.canMakeShroomy(blockState) && Permissions.isSubSkillEnabled(player, SubSkill.HERBALISM_SHROOM_THUMB);
    }

    public boolean canUseHylianLuck() {
        return Permissions.isSubSkillEnabled(getPlayer(), SubSkill.HERBALISM_HYLIAN_LUCK);
    }

    public boolean canGreenTerraBlock(BlockState blockState) {
        return mcMMOPlayer.getAbilityMode(SuperAbility.GREEN_TERRA) && BlockUtils.canMakeMossy(blockState);
    }

    public boolean canActivateAbility() {
        return mcMMOPlayer.getToolPreparationMode(ToolType.HOE) && Permissions.greenTerra(getPlayer());
    }

    public boolean canGreenTerraPlant() {
        return mcMMOPlayer.getAbilityMode(SuperAbility.GREEN_TERRA);
    }

    /**
     * Handle the Farmer's Diet ability
     *
     * @param rankChange The # of levels to change rank for the food
     * @param eventFoodLevel The initial change in hunger from the event
     * @return the modified change in hunger for the event
     */
    public int farmersDiet(int rankChange, int eventFoodLevel) {
        return SkillUtils.handleFoodSkills(getPlayer(), skill, eventFoodLevel, Herbalism.farmersDietRankLevel1, Herbalism.farmersDietMaxLevel, rankChange);
    }

    /**
     * Process the Green Terra ability.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    public boolean processGreenTerra(BlockState blockState) {
        Player player = getPlayer();

        if (!Permissions.greenThumbBlock(player, blockState.getType())) {
            return false;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS);

        if (!playerInventory.containsAtLeast(seed, 1)) {
            player.sendMessage(LocaleLoader.getString("Herbalism.Ability.GTe.NeedMore"));
            return false;
        }

        playerInventory.removeItem(seed);
        player.updateInventory(); // Needed until replacement available

        return Herbalism.convertGreenTerraBlocks(blockState);
    }

    /**
     * @param blockState The {@link BlockState} to check ability activation for
     */
    public void herbalismBlockCheck(BlockState blockState) {
        Player player = getPlayer();
        Material material = blockState.getType();
        boolean oneBlockPlant = !(material == Material.CACTUS || material == Material.CHORUS_PLANT || material == Material.SUGAR_CANE);

        // Prevents placing and immediately breaking blocks for exp
        if (oneBlockPlant && mcMMO.getPlaceStore().isTrue(blockState)) {
            return;
        }

        if (!canBlockCheck()) {
            return;
        }

        Collection<ItemStack> drops = null;
        int amount = 1;
        int xp;
        boolean greenTerra = mcMMOPlayer.getAbilityMode(skill.getAbility());

        if (mcMMO.getModManager().isCustomHerbalismBlock(blockState)) {
            CustomBlock customBlock = mcMMO.getModManager().getBlock(blockState);
            xp = customBlock.getXpGain();

            if (Permissions.isSubSkillEnabled(player, SubSkill.HERBALISM_DOUBLE_DROPS) && customBlock.isDoubleDropEnabled()) {
                drops = blockState.getBlock().getDrops();
            }
        }
        else {
            xp = ExperienceConfig.getInstance().getXp(skill, blockState.getBlockData());

            if (Config.getInstance().getDoubleDropsEnabled(skill, material) && Permissions.isSubSkillEnabled(player, SubSkill.HERBALISM_DOUBLE_DROPS)) {
                drops = blockState.getBlock().getDrops();
            }

            if (!oneBlockPlant) {
                amount = Herbalism.calculateMultiBlockPlantDrops(blockState);
                xp *= amount;
            }
            
            if (Permissions.greenThumbPlant(player, material)) {
                processGreenThumbPlants(blockState, greenTerra);
            }
        }

        applyXpGain(xp, XPGainReason.PVE);

        if (drops == null) {
            return;
        }

        for (int i = greenTerra ? 2 : 1; i != 0; i--) {
            if (SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.HERBALISM_DOUBLE_DROPS, player, this.skill, getSkillLevel(), activationChance)) {
                for (ItemStack item : drops) {
                    Misc.dropItems(Misc.getBlockCenter(blockState), item, amount);
                }
            }
        }
    }

    /**
     * Process the Green Thumb ability for blocks.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    public boolean processGreenThumbBlocks(BlockState blockState) {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.HERBALISM_GREEN_THUMB, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            getPlayer().sendMessage(LocaleLoader.getString("Herbalism.Ability.GTh.Fail"));
            return false;
        }

        return Herbalism.convertGreenTerraBlocks(blockState);
    }

    /**
     * Process the Hylian Luck ability.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    public boolean processHylianLuck(BlockState blockState) {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.HERBALISM_HYLIAN_LUCK, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            return false;
        }

        String friendly = StringUtils.getFriendlyConfigBlockDataString(blockState.getBlockData());
        if (!TreasureConfig.getInstance().hylianMap.containsKey(friendly))
            return false;
        List<HylianTreasure> treasures = TreasureConfig.getInstance().hylianMap.get(friendly);

        Player player = getPlayer();

        if (treasures.isEmpty()) {
            return false;
        }
        int skillLevel = getSkillLevel();
        Location location = Misc.getBlockCenter(blockState);

        for (HylianTreasure treasure : treasures) {
            if (skillLevel >= treasure.getDropLevel() && SkillUtils.treasureDropSuccessful(getPlayer(), treasure.getDropChance(), activationChance)) {
                if (!EventUtils.simulateBlockBreak(blockState.getBlock(), player, false)) {
                    return false;
                }
                blockState.setType(Material.AIR);
                Misc.dropItem(location, treasure.getDrop());
                player.sendMessage(LocaleLoader.getString("Herbalism.HylianLuck"));
                return true;
            }
        }
        return false;
    }

    /**
     * Process the Shroom Thumb ability.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    public boolean processShroomThumb(BlockState blockState) {
        Player player = getPlayer();
        PlayerInventory playerInventory = player.getInventory();
        
        if (!playerInventory.contains(Material.BROWN_MUSHROOM, 1)) {
            player.sendMessage(LocaleLoader.getString("Skills.NeedMore", StringUtils.getPrettyItemString(Material.BROWN_MUSHROOM)));
            return false;
        }

        if (!playerInventory.contains(Material.RED_MUSHROOM, 1)) {
            player.sendMessage(LocaleLoader.getString("Skills.NeedMore", StringUtils.getPrettyItemString(Material.RED_MUSHROOM)));
            return false;
        }

        playerInventory.removeItem(new ItemStack(Material.BROWN_MUSHROOM));
        playerInventory.removeItem(new ItemStack(Material.RED_MUSHROOM));
        player.updateInventory();

        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.HERBALISM_SHROOM_THUMB, player, this.skill, getSkillLevel(), activationChance)) {
            player.sendMessage(LocaleLoader.getString("Herbalism.Ability.ShroomThumb.Fail"));
            return false;
        }

        return Herbalism.convertShroomThumb(blockState);
    }

    /**
     * Process the Green Thumb ability for plants.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     * @param greenTerra boolean to determine if greenTerra is active or not
     */
    private void processGreenThumbPlants(BlockState blockState, boolean greenTerra) {
        if (!BlockUtils.isFullyGrown(blockState))
            return;

        Player player = getPlayer();
        PlayerInventory playerInventory = player.getInventory();
        Material seed = null;

        switch (blockState.getType()) {
            case CARROTS:
                seed = Material.CARROT;
                break;

            case WHEAT:
                seed = Material.WHEAT_SEEDS;
                break;

            case NETHER_WART:
                seed = Material.NETHER_WART;
                break;

            case POTATOES:
                seed = Material.POTATO;
                break;

            case BEETROOTS:
                seed = Material.BEETROOT_SEEDS;
                break;

            case COCOA:
                seed = Material.COCOA_BEANS;
                break;

            default:
                return;
        }

        ItemStack seedStack = new ItemStack(seed);

        if (!playerInventory.containsAtLeast(seedStack, 1)) {
            return;
        }

        if (!greenTerra && !SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.HERBALISM_GREEN_THUMB, player, this.skill, getSkillLevel(), activationChance)) {
            return;
        }

        if (!handleBlockState(blockState, greenTerra)) {
            return;
        }

        playerInventory.removeItem(seedStack);
        player.updateInventory(); // Needed until replacement available
        new HerbalismBlockUpdaterTask(blockState).runTaskLater(mcMMO.p, 0);
    }

    private boolean handleBlockState(BlockState blockState, boolean greenTerra) {
        byte greenThumbStage = getGreenThumbStage();

        blockState.setMetadata(mcMMO.greenThumbDataKey, new FixedMetadataValue(mcMMO.p, (int) (System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR)));
        Ageable crops = (Ageable) blockState.getBlockData();

        switch (blockState.getType()) {

            case POTATOES:
            case CARROTS:
            case WHEAT:

                if (greenTerra) {
                    crops.setAge(3);
                }
                else {
                    crops.setAge(greenThumbStage);
                }
                break;

            case BEETROOTS:
            case NETHER_WART:

                if (greenTerra || greenThumbStage > 2) {
                    crops.setAge(2);
                }
                else if (greenThumbStage == 2) {
                    crops.setAge(1);
                }
                else {
                    crops.setAge(0);
                }
               break;

            case COCOA:

                if (greenTerra || getGreenThumbStage() > 1) {
                    crops.setAge(1);
                }
                else {
                    crops.setAge(0);
                }
                break;

            default:
                return false;
        }
        blockState.setBlockData(crops);
        return true;
    }

    private byte getGreenThumbStage() {
        return (byte) Math.min(Math.min(getSkillLevel(), Herbalism.greenThumbStageMaxLevel) / Herbalism.greenThumbStageChangeLevel, 4);
    }
}
