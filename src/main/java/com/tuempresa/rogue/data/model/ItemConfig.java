package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.ItemConfigException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Configuración genérica para ítems definida vía data packs.
 */
public record ItemConfig(int attackIntervalTicks, float baseDamage) {
    public static ItemConfig fromJson(ResourceLocation id, JsonObject json) {
        if (json == null) {
            throw new ItemConfigException("El archivo de configuración de " + id + " está vacío");
        }

        int interval = GsonHelper.getAsInt(json, "attackIntervalTicks", 0);
        if (interval < 0) {
            throw new ItemConfigException("attackIntervalTicks debe ser >= 0 en " + id);
        }

        float baseDamage = GsonHelper.getAsFloat(json, "baseDamage", 0.0F);
        if (baseDamage < 0.0F) {
            throw new ItemConfigException("baseDamage debe ser >= 0 en " + id);
        }

        return new ItemConfig(interval, baseDamage);
    }
}
