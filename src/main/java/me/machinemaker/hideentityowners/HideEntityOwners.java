/*
 * GNU General Public License v3
 *
 * HideEntityOwners, a utility plugin to hide tamed entity owners
 *
 * Copyright (C) 2022 Machine_Maker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package me.machinemaker.hideentityowners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.Pair;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class HideEntityOwners extends JavaPlugin {

    private static final String BYPASS_PERM = "hide-entity-owners.bypass";
    private static final int OWNER_UUID_INDEX = 18;

    private final Supplier<ProtocolManager> pm = Suppliers.memoize(ProtocolLibrary::getProtocolManager);

    private ProtocolManager pm() {
        return this.pm.get();
    }

    @Override
    public void onEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(final PacketEvent event) {
                if (!event.getPlayer().hasPermission(BYPASS_PERM)) {
                    final @Nullable Entity entity = HideEntityOwners.this.getEntity(event.getPacket().getIntegers().read(0));
                    if (entity instanceof Tameable) {
                        final @Nullable Pair<WrappedWatchableObject, UUID> pair = HideEntityOwners.this.getOwnerUUIDWatchable(event.getPacket());
                        if (pair == null || event.getPlayer().getUniqueId().equals(pair.right())) {
                            return;
                        }
                        pair.left().setValue(Optional.empty());
                    }
                }
            }
        });
    }

    private @Nullable Pair<WrappedWatchableObject, UUID> getOwnerUUIDWatchable(final PacketContainer packetContainer) {
        for (final WrappedWatchableObject watchableObject : packetContainer.getWatchableCollectionModifier().read(0)) {
            if (watchableObject.getIndex() == OWNER_UUID_INDEX && watchableObject.getValue() instanceof Optional<?> opt) {
                return this.getUUIDPair(watchableObject, opt);
            }
        }
        return null;
    }

    private @Nullable Pair<WrappedWatchableObject, UUID> getUUIDPair(final WrappedWatchableObject watchableObject, final Optional<?> opt) {
        if (opt.isPresent() && opt.get() instanceof UUID uuid) {
            return Pair.of(watchableObject, uuid);
        }
        return null;
    }

    private @Nullable Entity getEntity(final int id) {
        for (final World world : Bukkit.getWorlds()) {
            final @Nullable Entity entity = this.pm().getEntityFromID(world, id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
