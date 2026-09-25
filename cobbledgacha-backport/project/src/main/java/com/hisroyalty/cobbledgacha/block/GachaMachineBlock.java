package com.hisroyalty.cobbledgacha.block;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import com.hisroyalty.cobbledgacha.cobblemon.PokemonCommandSpawner;
import com.hisroyalty.cobbledgacha.config.*;
import com.hisroyalty.cobbledgacha.world.GachaCooldownData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GachaMachineBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final UUID AUTOMATION_UUID=UUID.nameUUIDFromBytes("cobbledgacha".getBytes(StandardCharsets.UTF_8));
    private final TagKey<Item> currencyTag; private final String lootKey; private final int configProperty;

    public GachaMachineBlock(Properties p,TagKey<Item> tag,String lootKey,int configProperty){
        super(p);this.currencyTag=tag;this.lootKey=lootKey;this.configProperty=configProperty;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));
    }
    public TagKey<Item> getCurrencyTag(){return currencyTag;}
    public String getLootKey(){return lootKey;}
    public String getMachineKey(){return "gacha_machine_"+configProperty;}
    public MachineType getMachineType(){return DatapackConfig.type(getMachineKey());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,HALF);}
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        BlockPos p=c.getClickedPos(); if(p.getY()>=c.getLevel().getMaxBuildHeight()-1||!c.getLevel().getBlockState(p.above()).canBeReplaced(c))return null;
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(HALF,DoubleBlockHalf.LOWER);
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity e,ItemStack stack){
        super.setPlacedBy(l,p,s,e,stack);l.setBlock(p.above(),s.setValue(HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);
    }
    private BlockPos lower(BlockState s,BlockPos p){return s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p;}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){
        if(!s.is(n.getBlock())){
            BlockPos low=lower(s,p);BlockEntity be=l.getBlockEntity(low);if(be instanceof GachaMachineBlockEntity m)Containers.dropContents(l,low,m);
            BlockPos other=s.getValue(HALF)==DoubleBlockHalf.LOWER?p.above():p.below();BlockState os=l.getBlockState(other);
            if(os.is(this)&&os.getValue(HALF)!=s.getValue(HALF))l.removeBlock(other,false);
        } super.onRemove(s,l,p,n,moving);
    }
    @Override public RenderShape getRenderShape(BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.INVISIBLE;}
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?new GachaMachineBlockEntity(p,s):null;}
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER)return null;return createTickerHelper(t,CobbledGacha.GACHA_MACHINE_BE.get(),GachaMachineBlockEntity::tick);
    }

    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(l.isClientSide)return InteractionResult.SUCCESS;BlockPos low=lower(s,p);BlockEntity raw=l.getBlockEntity(low);
        if(!(raw instanceof GachaMachineBlockEntity be))return InteractionResult.PASS;
        boolean ok=processCurrency((ServerLevel)l,low,be,player.getItemInHand(hand),player instanceof ServerPlayer sp?sp:null,false);
        return ok?InteractionResult.CONSUME:InteractionResult.FAIL;
    }

    public boolean processCurrency(ServerLevel level,BlockPos pos,GachaMachineBlockEntity be,ItemStack currency,@Nullable ServerPlayer player,boolean automation){
        if(currency.isEmpty()||!currency.is(currencyTag)){
            if(player!=null)player.displayClientMessage(Component.translatable("message.gacha_machine.invalid_currency").withStyle(ChatFormatting.RED),true);return false;
        }
        ResourceLocation cid=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(currency.getItem());
        if(getMachineType()==MachineType.SPECIFIC&&be.getLockedCurrencyId()!=null&&!be.getLockedCurrencyId().equals(cid)){
            if(player!=null)player.displayClientMessage(Component.translatable("message.gacha_machine.invalid_currency").withStyle(ChatFormatting.RED),true);return false;
        }
        UUID uid=player==null?AUTOMATION_UUID:player.getUUID();GachaCooldownData cd=GachaCooldownData.get(level);
        long remain=cd.remaining(level,getMachineKey(),uid);
        if(remain>0){if(player!=null)player.displayClientMessage(Component.translatable("message.gacha_machine.cooldown",Component.literal(Long.toString((remain+19)/20))).withStyle(ChatFormatting.RED),true);return false;}
        if(getMachineType()==MachineType.SPECIFIC&&be.getLockedCurrencyId()==null)be.setLockedCurrencyId(cid);
        currency.shrink(1);int next=be.getGachaLevel()+1,max=DatapackConfig.maxCurrency(getMachineKey());
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.CHAIN_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.75f,1f);
        if(next<max){be.setGachaLevel(next);if(player!=null)player.displayClientMessage(Component.literal("["+next+"/"+max+"]"),true);return true;}

        be.setGachaLevel(0);be.playDispenseAnimation();boolean dispensed;
        if(getMachineType()==MachineType.SPAWNER){
            Direction d=be.getBlockState().getValue(FACING);dispensed=PokemonCommandSpawner.spawn(level,pos.relative(d),player,false,lootKey);
        }else{
            String table=lootKey;if(getMachineType()==MachineType.SPECIFIC&&be.getLockedCurrencyId()!=null)table+="_"+be.getLockedCurrencyId().getPath();
            List<ItemStack> rewards=roll(level,pos,player,table);dispensed=!rewards.isEmpty();for(ItemStack reward:rewards)deliver(level,pos,be,player,reward);
        }
        be.clearLockedCurrency();cd.recordUse(level,getMachineKey(),uid,DatapackConfig.usesBeforeCooldown(getMachineKey()),DatapackConfig.cooldownSeconds(getMachineKey())*20);
        return dispensed||automation;
    }
    private List<ItemStack> roll(ServerLevel level,BlockPos pos,@Nullable ServerPlayer player,String tableKey){
        LootTable table=level.getServer().getLootData().getLootTable(CobbledGacha.id(tableKey));
        LootParams.Builder b=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,pos.getCenter());
        if(player!=null)b.withOptionalParameter(LootContextParams.THIS_ENTITY,player);
        return table.getRandomItems(b.create(LootContextParamSets.CHEST));
    }
    private void deliver(ServerLevel l,BlockPos p,GachaMachineBlockEntity be,@Nullable ServerPlayer player,ItemStack input){
        ItemStack r=input.copy();BlockEntity below=l.getBlockEntity(p.below());if(below instanceof Container c)r=insert(c,r);
        if(!r.isEmpty()&&DatapackConfig.pickup()&&player!=null&&player.addItem(r))r=ItemStack.EMPTY;
        if(!r.isEmpty()){Direction d=be.getBlockState().getValue(FACING);BlockPos out=p.relative(d);Containers.dropItemStack(l,out.getX()+.5,out.getY()+.5,out.getZ()+.5,r);}
    }
    private ItemStack insert(Container c,ItemStack in){
        ItemStack r=in.copy();for(int slot=0;slot<c.getContainerSize()&&!r.isEmpty();slot++){ItemStack e=c.getItem(slot);
            if(e.isEmpty()&&c.canPlaceItem(slot,r)){c.setItem(slot,r.copy());return ItemStack.EMPTY;}
            if(ItemStack.isSameItemSameTags(e,r)&&e.getCount()<e.getMaxStackSize()&&c.canPlaceItem(slot,r)){int m=Math.min(r.getCount(),e.getMaxStackSize()-e.getCount());e.grow(m);r.shrink(m);c.setChanged();}
        }return r;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){BlockEntity be=l.getBlockEntity(lower(s,p));return be instanceof GachaMachineBlockEntity m?Math.min(15,m.getGachaLevel()):0;}
    @Override public WorldlyContainer getContainer(BlockState s,LevelAccessor l,BlockPos p){BlockEntity be=l.getBlockEntity(lower(s,p));return be instanceof WorldlyContainer w?w:null;}
}
