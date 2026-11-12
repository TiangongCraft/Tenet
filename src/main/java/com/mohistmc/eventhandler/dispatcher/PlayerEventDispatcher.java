/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2024.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.eventhandler.dispatcher;

import com.mohistmc.bukkit.inventory.MohistModsInventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v1_20_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;

public class PlayerEventDispatcher {

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        // Mohist start - Custom Container compatible with mods
        AbstractContainerMenu abstractcontainermenu = event.getContainer();
        abstractcontainermenu.containerOwner = event.getEntity();
        if (abstractcontainermenu.getBukkitView() == null) {
            org.bukkit.inventory.Inventory inventory = new CraftInventory(new MohistModsInventory(abstractcontainermenu, event.getEntity()));
            inventory.getType().setMods(true);
            abstractcontainermenu.bukkitView = new CraftInventoryView(event.getEntity().getBukkitEntity(), inventory, abstractcontainermenu);
        }
        // Mohist end
        CraftEventFactory.handleInventoryCloseEvent(event.getEntity()); // CraftBukkit
    }
}
