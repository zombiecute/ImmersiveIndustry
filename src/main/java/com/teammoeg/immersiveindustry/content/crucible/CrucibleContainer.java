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

package com.teammoeg.immersiveindustry.content.crucible;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class CrucibleContainer extends IEBaseContainer<CrucibleTileEntity> {
    public CrucibleTileEntity.CrucibleData data;

    public CrucibleContainer(int id, PlayerInventory inventoryPlayer, CrucibleTileEntity tile) {
        super(inventoryPlayer, tile, id);

        // input
        this.addSlot(new IESlot(this, this.inv, 0, 30, 12) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return CrucibleRecipe.isValidRecipeInput(itemStack, true);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 1, 51, 12) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return CrucibleRecipe.isValidRecipeInput(itemStack, false);
            }
        });
        // output
        this.addSlot(new IESlot.Output(this, this.inv, 2, 106, 12));
        // input fuel
        this.addSlot(new IESlot(this, this.inv, 3, 80, 51) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return itemStack.getItem().getTags().contains(CrucibleTileEntity.coal_coke);
            }
        });
        slotCount = 4;

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
        data = tile.guiData;
        trackIntArray(data);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slot) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slotObject = this.inventorySlots.get(slot);
        if (slotObject != null && slotObject.getHasStack()) {
            ItemStack itemstack1 = slotObject.getStack();
            itemstack = itemstack1.copy();
            if (slot < this.slotCount) {
                if (!this.mergeItemStack(itemstack1, this.slotCount, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, this.slotCount, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slotObject.putStack(ItemStack.EMPTY);
            } else {
                slotObject.onSlotChanged();
            }
        }

        return itemstack;
    }
}

