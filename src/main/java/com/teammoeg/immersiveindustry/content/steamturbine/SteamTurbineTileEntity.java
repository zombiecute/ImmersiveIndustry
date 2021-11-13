/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Immersive Industry.
 *
 * Immersive Industry is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Immersive Industry is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Immersive Industry. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.immersiveindustry.content.steamturbine;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.utils.CapabilityReference;
import blusunrize.immersiveengineering.api.utils.DirectionalBlockPos;
import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.util.EnergyHelper;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.teammoeg.immersiveindustry.IIConfig;
import com.teammoeg.immersiveindustry.IIContent;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SteamTurbineTileEntity extends MultiblockPartTileEntity<SteamTurbineTileEntity> implements
        IEBlockInterfaces.IBlockBounds, IEBlockInterfaces.ISoundTile {
    public FluidTank tanks = new FluidTank(24 * FluidAttributes.BUCKET_VOLUME);
    public boolean active = false;
    public final int energy;
    private static final CachedShapesWithTransform<BlockPos, Pair<Direction, Boolean>> SHAPES = CachedShapesWithTransform.createForMultiblock(SteamTurbineTileEntity::getShape);

    //public static Fluid steam = Fluids.WATER;
    public SteamTurbineTileEntity() {
        super(IIContent.IIMultiblocks.STEAMTURBINE, IIContent.IITileTypes.STEAMTURBINE.get(), true);
        this.energy = IIConfig.COMMON.steamTurbineGenerator.get();
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        tanks.readFromNBT(nbt.getCompound("tank"));
        active = nbt.getBoolean("active");

    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.put("tank0", tanks.writeToNBT(new CompoundNBT()));
        nbt.putBoolean("active", active);
    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        SteamTurbineTileEntity master = master();
        if (master != null && (posInMultiblock.getZ() == 0 && posInMultiblock.getY() == 1 && posInMultiblock.getX() == 2)
                && (side == null || side == getFacing().getOpposite()))
            return new FluidTank[]{master.tanks};
        return new FluidTank[0];
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();
        if (!this.isDummy()) {
            if (!world.isRemote) {
                if (!isRSDisabled() && !tanks.getFluid().isEmpty()) {
                    List<IEnergyStorage> presentOutputs = outputs.stream()
                            .map(CapabilityReference::getNullable)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    if (!presentOutputs.isEmpty() &&
                            tanks.getFluidAmount() >= 64 &&
                            EnergyHelper.distributeFlux(presentOutputs, energy, false) < energy) {
                        tanks.drain(64, IFluidHandler.FluidAction.EXECUTE);
                        if (!active)
                            active = true;
                    }
                } else if (active)
                    active = false;
            } else if (active) {
                ImmersiveEngineering.proxy.handleTileSound(IESounds.dieselGenerator, this, this.active, 0.5F, 1.0F);
            }
        }
    }
    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack fluidStack) {
        ITag<Fluid> steamTag = FluidTags.getCollection().get(new ResourceLocation("forge", "steam"));
        if (steamTag != null)
            return fluidStack.getFluid().isIn(steamTag);
        else
            return fluidStack.getFluid() == ForgeRegistries.FLUIDS.getValue(new ResourceLocation("steampowered", "steam"));
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return this.getShape(SHAPES);
    }

    private static List<AxisAlignedBB> getShape(BlockPos posInMultiblock) {
        if (posInMultiblock.equals(new BlockPos(0, 1, 0)))
            return ImmutableList.of(new AxisAlignedBB(0.5D, 0D, 0D, 1.0D, 1D, 1.0D));
        else if (posInMultiblock.getZ() == 5 && posInMultiblock.getY() == 1) {
            if (posInMultiblock.getX() % 2 == 0)
                return Utils.flipBoxes(false, posInMultiblock.getX() == 2, new AxisAlignedBB(0D, 0D, 0D, 0.75D, 0.5D, 1.0D));
            else return ImmutableList.of(new AxisAlignedBB(0D, 0D, 0D, 1.0D, 0.5D, 1.0D));
        } else if (posInMultiblock.getX() % 2 == 0 && posInMultiblock.getZ() != 6) {
            if (posInMultiblock.getY() == 0)
                return ImmutableList.of(new AxisAlignedBB(0D, 0D, 0D, 1.0D, 0.5D, 1.0D));
            else if (posInMultiblock.getY() == 1)
                return Utils.flipBoxes(false, posInMultiblock.getX() == 2, new AxisAlignedBB(0D, 0D, 0D, 0.75D, 1D, 1.0D));
            else if (posInMultiblock.getZ() == 0)
                return ImmutableList.of(new AxisAlignedBB(0.25D, 0D, 0.0D, 1D, 0.5D, 0.2D));
            else return ImmutableList.of(new AxisAlignedBB(0.25D, 0D, 0D, 1D, 0.5D, 1.0D));
        } else if (posInMultiblock.getY() == 2) {
            return ImmutableList.of(new AxisAlignedBB(0D, 0D, 0D, 1.0D, 0.5D, 1.0D));
        } else if (posInMultiblock.equals(new BlockPos(1, 0, 0)))
            return ImmutableList.of(new AxisAlignedBB(0D, 0D, 0D, 1.0D, 0.5D, 1.0D));

        else return ImmutableList.of(new AxisAlignedBB(0, 0, 0, 1.0D, 1.0D, 1.0D));
    }

    @Override
    public Set<BlockPos> getRedstonePos() {
        return ImmutableSet.of(
                new BlockPos(0, 1, 0)
        );
    }

    private final List<CapabilityReference<IEnergyStorage>> outputs = Arrays.asList(
            CapabilityReference.forTileEntityAt(this,
                    () -> new DirectionalBlockPos(this.getBlockPosForPos(new BlockPos(0, 1, 6)).add(0, 1, 0), Direction.DOWN),
                    CapabilityEnergy.ENERGY),
            CapabilityReference.forTileEntityAt(this,
                    () -> new DirectionalBlockPos(this.getBlockPosForPos(new BlockPos(2, 1, 6)).add(0, 1, 0), Direction.DOWN),
                    CapabilityEnergy.ENERGY)
    );

    @Override
    public boolean shouldPlaySound(String sound) {
        return this.active;
    }
}