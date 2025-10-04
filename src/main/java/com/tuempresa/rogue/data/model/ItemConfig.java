package com.tuempresa.rogue.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.ItemConfigException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración genérica para ítems definida vía data packs.
 */
public record ItemConfig(List<Integer> attackIntervals,
                         float baseDamage,
                         List<Float> damageMultipliers) {

    public ItemConfig {
        attackIntervals = List.copyOf(attackIntervals);
        damageMultipliers = List.copyOf(damageMultipliers);
    }

    public static ItemConfig fromJson(ResourceLocation id, JsonObject json) {
        if (json == null) {
            throw new ItemConfigException("El archivo de configuración de " + id + " está vacío");
        }

        List<Integer> attackIntervals = parseAttackIntervals(id, json);
        float baseDamage = GsonHelper.getAsFloat(json, "baseDamage", 0.0F);
        if (baseDamage < 0.0F) {
            throw new ItemConfigException("baseDamage debe ser >= 0 en " + id);
        }
        List<Float> damageMultipliers = parseDamageMultipliers(id, json);

        return new ItemConfig(attackIntervals, baseDamage, damageMultipliers);
    }

    public int attackInterval(int awakeningLevel) {
        if (attackIntervals.isEmpty()) {
            return 0;
        }
        int index = Math.max(0, Math.min(awakeningLevel, attackIntervals.size() - 1));
        return attackIntervals.get(index);
    }

    public float damageMultiplier(int awakeningLevel) {
        if (damageMultipliers.isEmpty()) {
            return 1.0F;
        }
        int index = Math.max(0, Math.min(awakeningLevel, damageMultipliers.size() - 1));
        return damageMultipliers.get(index);
    }

    private static List<Integer> parseAttackIntervals(ResourceLocation id, JsonObject json) {
        if (!json.has("attackIntervalTicks")) {
            return Collections.singletonList(0);
        }
        JsonElement element = json.get("attackIntervalTicks");
        if (element.isJsonArray()) {
            List<Integer> values = new ArrayList<>();
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                int value = GsonHelper.convertToInt(array.get(i), "attackIntervalTicks[" + i + "]");
                if (value < 0) {
                    throw new ItemConfigException("attackIntervalTicks debe ser >= 0 en " + id);
                }
                values.add(value);
            }
            if (values.isEmpty()) {
                throw new ItemConfigException("attackIntervalTicks no puede ser una lista vacía en " + id);
            }
            return values;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int value = GsonHelper.convertToInt(element, "attackIntervalTicks");
            if (value < 0) {
                throw new ItemConfigException("attackIntervalTicks debe ser >= 0 en " + id);
            }
            return Collections.singletonList(value);
        }
        throw new ItemConfigException("attackIntervalTicks debe ser un número o una lista en " + id);
    }

    private static List<Float> parseDamageMultipliers(ResourceLocation id, JsonObject json) {
        if (!json.has("damageMultiplier")) {
            return Collections.singletonList(1.0F);
        }
        JsonElement element = json.get("damageMultiplier");
        if (element.isJsonArray()) {
            List<Float> values = new ArrayList<>();
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                float value = GsonHelper.convertToFloat(array.get(i), "damageMultiplier[" + i + "]");
                if (value < 0.0F) {
                    throw new ItemConfigException("damageMultiplier debe ser >= 0 en " + id);
                }
                values.add(value);
            }
            if (values.isEmpty()) {
                throw new ItemConfigException("damageMultiplier no puede ser una lista vacía en " + id);
            }
            return values;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            float value = GsonHelper.convertToFloat(element, "damageMultiplier");
            if (value < 0.0F) {
                throw new ItemConfigException("damageMultiplier debe ser >= 0 en " + id);
            }
            return Collections.singletonList(value);
        }
        throw new ItemConfigException("damageMultiplier debe ser un número o una lista en " + id);
    }
}
