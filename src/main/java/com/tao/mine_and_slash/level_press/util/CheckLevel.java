package com.tao.mine_and_slash.level_press.util;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * @author AIERXUAN
 * @date 2022/6/17 - 11:30
 * @description 等级压制设置
 */
public class CheckLevel {

    public static boolean checkLevel(LivingEntity target, MobEntity mob){
        if (!(target instanceof PlayerEntity))
            return false;
        PlayerEntity player = (PlayerEntity) target;
        int playerLevel = Load.Unit(player).getLevel();
        int mobLevel = Load.Unit(mob).getLevel();
        return mobLevel - 5 <= playerLevel && playerLevel <= mobLevel + 5;
    }
}
