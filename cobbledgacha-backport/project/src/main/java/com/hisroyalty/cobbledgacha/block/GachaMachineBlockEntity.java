package com.hisroyalty.cobbledgacha.block;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import com.hisroyalty.cobbledgacha.config.DatapackConfig;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GachaMachineBlockEntity extends BlockEntity implements WorldlyContainer, GeoBlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int gachaLevel;
    private ResourceLocation lockedCurrencyId;
    private long lastAutomationTick;

    public GachaMachineBlockEntity(BlockPos pos,BlockState state){super(CobbledGacha.GACHA_MACHINE_BE.get(),pos,state);}
    public int getGachaLevel(){return gachaLevel;}
    public void setGachaLevel(int v){gachaLevel=Math.max(0,v);setChanged();}
    public ResourceLocation getLockedCurrencyId(){return lockedCurrencyId;}
    public void setLockedCurrencyId(ResourceLocation id){lockedCurrencyId=id;setChanged();}
    public void clearLockedCurrency(){lockedCurrencyId=null;setChanged();}

    public static void tick(Level level,BlockPos pos,BlockState state,GachaMachineBlockEntity be){
        if(level.isClientSide||!DatapackConfig.automation())return;
        if(level.getGameTime()-be.lastAutomationTick<8)return; be.lastAutomationTick=level.getGameTime();
        if(state.getBlock() instanceof GachaMachineBlock block){
            ItemStack input=be.items.get(0);
            if(!input.isEmpty())block.processCurrency((net.minecraft.server.level.ServerLevel)level,pos,be,input,null,true);
        }
    }

    public void playDispenseAnimation(){if(level!=null&&!level.isClientSide)triggerAnim("dispense_controller","dispense");}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(
            new AnimationController<>(this,"idle_controller",0,state->{
                if(!(getBlockState().getBlock() instanceof GachaMachineBlock block))return PlayState.STOP;
                state.setAnimation(RawAnimation.begin().thenLoop("animation."+block.getLootKey()+".idle")); return PlayState.CONTINUE;
            }),
            new AnimationController<>(this,"dispense_controller",0,state->PlayState.STOP)
                .triggerableAnim("dispense",RawAnimation.begin().thenPlay(
                    getBlockState().getBlock() instanceof GachaMachineBlock block
                        ?"animation."+block.getLootKey()+".dispense":"animation.gacha_machine.dispense"))
        );
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}

    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); tag.putInt("GachaLevel",gachaLevel);
        if(lockedCurrencyId!=null)tag.putString("LockedCurrency",lockedCurrencyId.toString());
    }
    @Override public void load(CompoundTag tag){
        super.load(tag); ContainerHelper.loadAllItems(tag,items); gachaLevel=tag.getInt("GachaLevel");
        lockedCurrencyId=tag.contains("LockedCurrency")?ResourceLocation.tryParse(tag.getString("LockedCurrency")):null;
    }
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{1}:new int[]{0};}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&getBlockState().getBlock() instanceof GachaMachineBlock b&&stack.is(b.getCurrencyTag());}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==1&&side==Direction.DOWN;}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int amount){ItemStack r=ContainerHelper.removeItem(items,slot,amount);if(!r.isEmpty())setChanged();return r;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public boolean stillValid(Player p){return p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public void clearContent(){items.clear();setChanged();}
}
