package com.maxwell3025.vats.content;

import com.maxwell3025.vats.api.Mixture;
import com.maxwell3025.vats.content.chemEngine.ChemicalTickEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ChemicalMixBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    int time = 0;
    private static BlockEntityType<ChemicalMixBlockEntity> typeInstance = null;
    private float heat;
    private Mixture contents;

    public static BlockEntityType<ChemicalMixBlockEntity> getTypeInstance() {
        if (typeInstance == null) {
            typeInstance = BlockEntityType.Builder.of(ChemicalMixBlockEntity::new, ChemicalMixBlock.getInstance()).build(null);
        }
        return typeInstance;
    }

    public ChemicalMixBlockEntity(BlockPos pos, BlockState state) {
        super(getTypeInstance(), pos, state);
        state.getBlock();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public ChemicalMixBlockEntity getNeighbor(Direction direction) {
        assert this.level != null;
        BlockPos neighborPos = this.worldPosition.relative(direction);
        BlockEntity neighborUntyped = this.level.getBlockEntity(neighborPos);
        if (neighborUntyped == null) return null;
        if (neighborUntyped.getType() != getTypeInstance()) return null;
        ChemicalMixBlockEntity neighbor = ((ChemicalMixBlockEntity) neighborUntyped);
        if (!neighbor.shouldTick()) return null;
        return ((ChemicalMixBlockEntity) neighborUntyped);
    }

    /**
     * This returns true iff this instance should process chemistry ticks
     */
    public boolean shouldTick(){
        if (!this.hasLevel()) {
            LOGGER.error("ChemicalMixBlockEntity should always have a level");
            return false;
        }
        assert this.level != null;
        if(!this.level.isClientSide) {
            ChunkAccess chunkAccess = this.level.getChunk(this.worldPosition);
            if(chunkAccess instanceof LevelChunk levelChunk){
                ServerLevel serverLevel = ((ServerLevel) levelChunk.getLevel());
                boolean validStatus = levelChunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING);
                boolean loadedProperly = serverLevel.areEntitiesLoaded(ChunkPos.asLong(this.worldPosition));
                return validStatus && loadedProperly;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onChemTick(ChemicalTickEvent tick) {
        // Validate that tick is intended for this BlockEntity
        if (this.isRemoved()) {
            MinecraftForge.EVENT_BUS.unregister(this);
            LOGGER.warn("Deregistered event bus");
            return;
        }
        if (tick.level != this.level) {
            return;
        }

        if (!this.shouldTick()) {
            return;
        }
        assert this.level != null;

        int neighborCount = 0;
        for (Direction direction : Direction.values()) {
            if (getNeighbor(direction) != null) neighborCount++;
        }
        LOGGER.warn((this.level.isClientSide() ? "Client" : "Server") + " Found " + neighborCount + " neighbors");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
    }
}