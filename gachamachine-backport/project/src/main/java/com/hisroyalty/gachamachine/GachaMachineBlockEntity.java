package com.hisroyalty.gachamachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GachaMachineBlockEntity extends BlockEntity implements WorldlyContainer {
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int gachaLevel;
    private int cooldownTicks;
    private int usesBeforeCooldownRemaining = 1;

    public GachaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(GachaMachine.MACHINE_BE.get(), pos, state);
    }

    public int getGachaLevel() { return gachaLevel; }
    public void setGachaLevel(int value) { gachaLevel = value; setChanged(); }
    public int getCooldown() { return cooldownTicks; }
    public void setCooldown(int value) { cooldownTicks = Math.max(0, value); setChanged(); }
    public int getUsesBeforeCooldownRemaining() { return usesBeforeCooldownRemaining; }
    public void setUsesBeforeCooldownRemaining(int value) { usesBeforeCooldownRemaining = value; setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, GachaMachineBlockEntity be) {
        if (level.isClientSide) return;
        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
            be.setChanged();
            return;
        }
        if (!(state.getBlock() instanceof GachaMachineBlock block)) return;

        ItemStack input = be.items.get(0);
        if (!input.isEmpty() && input.is(block.getCurrencyTag())) {
            input.shrink(1);
            be.gachaLevel++;

            if (be.gachaLevel >= GachaMachineBlock.DEFAULT_MAX_CURRENCY) {
                be.gachaLevel = 0;
                ItemStack reward = block.rollReward((net.minecraft.server.level.ServerLevel) level, pos, null);
                if (!reward.isEmpty()) {
                    ItemStack output = be.items.get(1);
                    if (output.isEmpty()) {
                        be.items.set(1, reward);
                    } else if (ItemStack.isSameItemSameTags(output, reward)
                            && output.getCount() + reward.getCount() <= output.getMaxStackSize()) {
                        output.grow(reward.getCount());
                    } else {
                        net.minecraft.world.Containers.dropItemStack(
                            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, reward);
                    }
                }
            }
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("GachaLevel", gachaLevel);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("UsesBeforeCooldownRemaining", usesBeforeCooldownRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        gachaLevel = tag.getInt("GachaLevel");
        cooldownTicks = tag.getInt("CooldownTicks");
        usesBeforeCooldownRemaining = tag.contains("UsesBeforeCooldownRemaining")
            ? tag.getInt("UsesBeforeCooldownRemaining") : 1;
    }

    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{1} : new int[]{0}; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && getBlockState().getBlock() instanceof GachaMachineBlock b && stack.is(b.getCurrencyTag());
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1 && side == Direction.DOWN; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
