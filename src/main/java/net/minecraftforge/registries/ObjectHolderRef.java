/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mohistmc.MohistMC;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.ResourceLocationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("rawtypes")
class ObjectHolderRef implements Consumer<Predicate<ResourceLocation>>, Runnable
{
    private static final Logger LOGGER  = LogManager.getLogger();
    private final Field field;
    private final ResourceLocation injectedObject;
    private final ForgeRegistry<?> registry;

    @SuppressWarnings("unchecked")
    @Nullable
    static ObjectHolderRef create(ResourceLocation registryName, Field field, String injectedObject, boolean extractFromExistingValues)
    {
        ForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(registryName);
        if (registry == null)
            return null;

        ResourceLocation injectedObjectName;
        if (extractFromExistingValues)
        {
            try
            {
                Object existing = field.get(null);
                // Nothing is ever allowed to replace AIR
                if (existing == null || existing == registry.getDefault())
                    return null;
                injectedObjectName = ((ForgeRegistry) registry).getKey(existing);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            try
            {
                injectedObjectName = new ResourceLocation(injectedObject);
            }
            catch (ResourceLocationException e)
            {
                throw new IllegalArgumentException(MohistMC.i18n.as("mohist.i18n.208", field.toString()), e);
            }
        }

        if (injectedObjectName == null)
            throw new IllegalStateException(String.format(Locale.ENGLISH, MohistMC.i18n.as("mohist.i18n.209", field.getType().getName(), field.getDeclaringClass().getName(), field.getName())));

        field.setAccessible(true);

        if (Modifier.isFinal(field.getModifiers()))
        {
            throw new RuntimeException(MohistMC.i18n.as("mohist.i18n.210", field.getDeclaringClass().getName(), field.getName()));
        }

        return new ObjectHolderRef(registry, field, injectedObjectName);
    }

    private ObjectHolderRef(ForgeRegistry<?> registry, Field field, ResourceLocation injectedObject)
    {
        this.registry = registry;
        this.field = field;
        this.injectedObject = injectedObject;
    }

    @Override
    public void accept(Predicate<ResourceLocation> filter)
    {
        if (registry == null || !filter.test(registry.getRegistryName()))
            return;
        run();
    }

    @Override
    public void run()
    {
        Object thing;
        if (registry.containsKey(injectedObject))
        {
            thing = registry.getValue(injectedObject);
        }
        else
        {
            thing = null;
        }

        if (thing == null)
        {
            LOGGER.debug(MohistMC.i18n.as("mohist.i18n.211", injectedObject, field));
            return;
        }
        try
        {
            field.set(null, thing);
        }
        catch (IllegalArgumentException | ReflectiveOperationException e)
        {
            LOGGER.warn(MohistMC.i18n.as("mohist.i18n.212", this.field, thing, this.injectedObject), e);
        }
    }

    @Override
    public int hashCode()
    {
        return field.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ObjectHolderRef))
            return false;
        ObjectHolderRef o = (ObjectHolderRef)other;
        return this.field.equals(o.field);
    }
}
