package com.tao.mine_and_slash.level_press.event;

import com.tao.mine_and_slash.level_press.util.CheckLevel;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 更改敌对生物ai，高于或低于玩家等级5级的，不会对玩家攻击，僵尸疣猪兽未做更改、蜜蜂和北极熊未添加移除的MeleeAttackGoal
 * @author AIERXUAN
 * @date 2022/6/16 - 20:14
 * @description 更改敌对生物ai，高于或低于玩家等级5级的，不会对玩家攻击，僵尸疣猪兽未做更改、蜜蜂和北极熊未添加移除的MeleeAttackGoal
 */
public class AiExchange {

    private static final Set<Class> HNTG1 = new HashSet<>(4);
    private static final Set<Class> HNTG2 = new HashSet<>(7);

    static {
        HNTG1.add(AbstractSkeletonEntity.class);
        HNTG1.add(CreeperEntity.class);
        HNTG1.add(EndermanEntity.class);
        HNTG1.add(SpiderEntity.class);
        HNTG1.add(WitchEntity.class);

        HNTG2.add(EvokerEntity.class);
        HNTG2.add(IllusionerEntity.class);
        HNTG2.add(PillagerEntity.class);
        HNTG2.add(RavagerEntity.class);
        HNTG2.add(VexEntity.class);
        HNTG2.add(VindicatorEntity.class);
        HNTG2.add(WitchEntity.class);
    }

    /**
     * 订阅事件，生成生物时，对其ai进行修改
     * @param e
     */
    @SubscribeEvent
    public static void onEntityJoinWorldEvent(EntityJoinWorldEvent e) { // 1
        Entity entity = e.getEntity();
        if (entity instanceof IMob) {
//            if (entity instanceof ZoglinEntity) {
//                ZoglinEntity zoglin = (ZoglinEntity) entity;
//                Brain<ZoglinEntity> brain = zoglin.getBrain();
//                brain
//                e.setCanceled(true);
//                return;
//            }
            try {
                MobEntity mob = (MobEntity) entity;
                Set<PrioritizedGoal> availableTargets = getField(GoalSelector.class, Set.class, mob.targetSelector);
                Set<PrioritizedGoal> availableGoals = getField(GoalSelector.class, Set.class, mob.goalSelector);
                replaceTargetGoal(mob, availableTargets);
                replaceTargetGoal(mob, availableGoals);
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 通过反射获取属性
     * @param targetClass
     * @param fieldClass
     * @param target
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     */
    private static <T> T getField(Class targetClass, Class<T> fieldClass, Object target) throws NoSuchFieldException {
        Field[] fields = targetClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == fieldClass) {
                field.setAccessible(true);
                try {
                    return (T) field.get(target);
                } catch (IllegalAccessException ex) {
                }
            }
        }
        throw new NoSuchFieldException();
    }

    /**
     * 替换ai的方法
     * @param mob
     * @param goals
     */
    private static void replaceTargetGoal(MobEntity mob, Set<PrioritizedGoal> goals) {
        if (goals != null) {
            Iterator<PrioritizedGoal> each = goals.iterator();
            Set<PrioritizedGoal> replace = new HashSet<>(3);
            while (each.hasNext()) {
                PrioritizedGoal next = each.next();
                Goal goal = next.getGoal();
                if (goal instanceof HurtByTargetGoal) {
                    each.remove();
                    replace.add(getSuitableHurtByTargetGoal(next.getPriority(), mob));
                } else if (goal instanceof MeleeAttackGoal) {
                    each.remove();
                    PrioritizedGoal prioritizedGoal = getSuitableMeleeAttackGoal(next.getPriority(), mob);
                    if (prioritizedGoal != null)
                        replace.add(prioritizedGoal);
                } else if (goal instanceof NearestAttackableTargetGoal) {
                    NearestAttackableTargetGoal nearestAttackableTargetGoal = (NearestAttackableTargetGoal) goal;
                    try {
                        Class targetType = getField(NearestAttackableTargetGoal.class, Class.class, nearestAttackableTargetGoal);
                        if (targetType == PlayerEntity.class) {
                            each.remove();
                            replace.add(getSuitableNearestAttackableTargetGoal(next.getPriority(), mob));
                        }
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
            goals.addAll(replace);
//            goals.removeIf(prioritizedGoal -> {
//                Goal goal = prioritizedGoal.getGoal();
//                if (goal instanceof HurtByTargetGoal)
//                    return true;
//                if (goal instanceof MeleeAttackGoal){
//                    meleeAttackGoal.set(prioritizedGoal);
//                    return true;
//                }
//                if (goal instanceof NearestAttackableTargetGoal) {
//                    NearestAttackableTargetGoal nearestAttackableTargetGoal = (NearestAttackableTargetGoal) goal;
//                    Class targetType = null;
//                    try {
//                        targetType = getField(NearestAttackableTargetGoal.class, Class.class, nearestAttackableTargetGoal);
//                    } catch (NoSuchFieldException e) {
//                        e.printStackTrace();
//                    }
//                    return targetType == PlayerEntity.class;
//                }
//                return false;
//            });
        }
    }

    private static PrioritizedGoal getSuitableNearestAttackableTargetGoal(int priority, MobEntity mob) {
        PrioritizedGoal goal;
        if (mob instanceof EvokerEntity || mob instanceof IllusionerEntity)
            goal = new PrioritizedGoal(priority, (new NearestAttackableTargetGoal(mob, PlayerEntity.class, true) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(target, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(target, mob);
                }
            }).setUnseenMemoryTicks(300));
        else if (mob instanceof GhastEntity || mob instanceof SlimeEntity)
            goal = new PrioritizedGoal(priority, new NearestAttackableTargetGoal(mob, PlayerEntity.class, 10, true, false,
                    o -> Math.abs(((LivingEntity) o).getY() - mob.getY()) <= 4.0D) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(target, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(target, mob);
                }
            });
        else if (mob instanceof ZombifiedPiglinEntity)
            goal = new PrioritizedGoal(priority, new NearestAttackableTargetGoal(mob, PlayerEntity.class, 10, true, false,
                    o -> ((ZombifiedPiglinEntity) mob).isAngryAt((ZombifiedPiglinEntity) o)) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(target, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(target, mob);
                }
            });
        else if (mob instanceof DrownedEntity)
            goal = new PrioritizedGoal(priority, new NearestAttackableTargetGoal(mob, PlayerEntity.class, 10, true, false,
                    o -> o != null && (!mob.level.isDay() || ((LivingEntity) o).isInWater())) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(target, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(target, mob);
                }
            });
        else
            goal = new PrioritizedGoal(priority, new NearestAttackableTargetGoal(mob, PlayerEntity.class, true) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(target, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(target, mob);
                }
            });
        return goal;
    }

    private static PrioritizedGoal getSuitableHurtByTargetGoal(int priority, MobEntity mob) {
        HurtByTargetGoal goal;
        if (mob instanceof DrownedEntity)
            goal = new HurtByTargetGoal((CreatureEntity) mob, DrownedEntity.class) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(targetMob, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(targetMob, mob);
                }
            };
        else if (HNTG2.contains(mob.getClass()))
            goal = new HurtByTargetGoal((CreatureEntity) mob, AbstractRaiderEntity.class) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(targetMob, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(targetMob, mob);
                }
            };
        else
            goal = new HurtByTargetGoal((CreatureEntity) mob) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(targetMob, mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(targetMob, mob);
                }
            };
        if (HNTG1.contains(mob.getClass()))
            return new PrioritizedGoal(priority, goal);
        else if (mob instanceof ZombieEntity || mob instanceof DrownedEntity)
            return new PrioritizedGoal(priority, goal.setAlertOthers(ZombifiedPiglinEntity.class));
        else
            return new PrioritizedGoal(priority, goal.setAlertOthers());
    }

    private static PrioritizedGoal getSuitableMeleeAttackGoal(int priority, MobEntity mob) {
        PrioritizedGoal prioritizedGoal = null;
        if (mob instanceof AbstractSkeletonEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((AbstractSkeletonEntity) mob, 1.2D, false) {
                public void stop() {
                    super.stop();
                    mob.setAggressive(false);
                }

                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                public void start() {
                    super.start();
                    mob.setAggressive(true);
                }
            });
        else if (mob instanceof DrownedEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((DrownedEntity) mob, 1.0D, false) {
                @Override
                public boolean canUse() {
                    return super.canUse() && ((DrownedEntity) mob).okTarget(mob.getTarget()) && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && ((DrownedEntity) mob).okTarget(mob.getTarget()) && CheckLevel.checkLevel(mob.getTarget(), mob);
                }
            });
        else if (mob instanceof PandaEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((PandaEntity) mob, 1.2D, true) {
                @Override
                public boolean canUse() {
                    return ((PandaEntity) mob).canPerformAction() && super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }
            });
        else if (mob instanceof RavagerEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((RavagerEntity) mob, 1.0D, true) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                protected double getAttackReachSqr(LivingEntity p_179512_1_) {
                    float f = mob.getBbWidth() - 0.1F;
                    return f * 2.0F * f * 2.0F + p_179512_1_.getBbWidth();
                }
            });
        else if (mob instanceof SpiderEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((SpiderEntity) mob, 1.0D, true) {
                @Override
                public boolean canUse() {
                    return super.canUse() && !this.mob.isVehicle();
                }

                @Override
                public boolean canContinueToUse() {
                    float f = this.mob.getBrightness();
                    if (f >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
                        this.mob.setTarget(null);
                        return false;
                    } else {
                        return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                    }
                }

                protected double getAttackReachSqr(LivingEntity p_179512_1_) {
                    return 4.0F + p_179512_1_.getBbWidth();
                }
            });
        else if (mob instanceof VindicatorEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((VindicatorEntity) mob, 1.0D, true) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                protected double getAttackReachSqr(LivingEntity p_179512_1_) {
                    if (this.mob.getVehicle() instanceof RavagerEntity) {
                        float f = this.mob.getVehicle().getBbWidth() - 0.1F;
                        return (double) (f * 2.0F * f * 2.0F + p_179512_1_.getBbWidth());
                    } else {
                        return super.getAttackReachSqr(p_179512_1_);
                    }
                }
            });
        else if (mob instanceof FoxEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((FoxEntity) mob, 1.2D, true) {
                protected void checkAndPerformAttack(LivingEntity living, double v) {
                    double d0 = this.getAttackReachSqr(living);
                    if (v <= d0 && this.isTimeToAttack()) {
                        this.resetAttackCooldown();
                        this.mob.doHurtTarget(living);
                        mob.playSound(SoundEvents.FOX_BITE, 1.0F, 1.0F);
                    }
                }

                public void start() {
                    ((FoxEntity) mob).setIsInterested(false);
                    super.start();
                }

                @Override
                public boolean canUse() {
                    return !((FoxEntity) mob).isSitting() && !mob.isSleeping() && !mob.isCrouching() &&
                            !((FoxEntity) mob).isFaceplanted() && super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }
            });
//        else if (mob instanceof RabbitEntity)
//            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((RabbitEntity) mob, 1.4D, true) {
//
//                protected double getAttackReachSqr(LivingEntity p_179512_1_) {
//                    return 4.0F + p_179512_1_.getBbWidth();
//                }
//
//                @Override
//                public boolean canUse() {
//                    return super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
//                }
//
//                @Override
//                public boolean canContinueToUse() {
//                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
//                }
//            });
//        else if (mob instanceof PolarBearEntity)
//            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((CreatureEntity) mob, 1.25D, true) {
//                protected void checkAndPerformAttack(LivingEntity p_190102_1_, double p_190102_2_) {
//                    double d0 = this.getAttackReachSqr(p_190102_1_);
//                    PolarBearEntity polarBear = (PolarBearEntity) mob;
//                    if (p_190102_2_ <= d0 && this.isTimeToAttack()) {
//                        this.resetAttackCooldown();
//                        this.mob.doHurtTarget(p_190102_1_);
//                        polarBear.setStanding(false);
//                    } else if (p_190102_2_ <= d0 * 2.0D) {
//                        if (this.isTimeToAttack()) {
//                            polarBear.setStanding(false);
//                            this.resetAttackCooldown();
//                        }
//
//                        if (this.getTicksUntilNextAttack() <= 10) {
//                            polarBear.setStanding(true);
////                            polarBear.playWarningSound();
//                        }
//                    } else {
//                        this.resetAttackCooldown();
//                        polarBear.setStanding(false);
//                    }
//                }
//                public void stop() {
//                    ((PolarBearEntity) mob).setStanding(false);
//                    super.stop();
//                }
//                protected double getAttackReachSqr(LivingEntity p_179512_1_) {
//                    return 4.0F + p_179512_1_.getBbWidth();
//                }
//            });
        else if (mob instanceof BeeEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new MeleeAttackGoal((BeeEntity) mob, 1.4D, true) {
                public boolean canUse() {
                    return super.canUse() && ((BeeEntity) mob).isAngry() && !((BeeEntity) mob).hasStung() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                public boolean canContinueToUse() {
                    return super.canContinueToUse() && ((BeeEntity) mob).isAngry() && !((BeeEntity) mob).hasStung() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }
            });
        else if (mob instanceof ZombieEntity)
            prioritizedGoal = new PrioritizedGoal(priority, new ZombieAttackGoal((ZombieEntity) mob, 1.0D, false) {
                @Override
                public boolean canUse() {
                    return super.canUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }

                @Override
                public boolean canContinueToUse() {
                    return super.canContinueToUse() && CheckLevel.checkLevel(mob.getTarget(), mob);
                }
            });
        return prioritizedGoal;
    }
}
