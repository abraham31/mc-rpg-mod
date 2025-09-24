package com.tuempresa.rpgcore.pack;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModList;

/**
 * ServerSavedData que persiste la lista de packs de contenido detectados en la
 * instalación actual. Se almacena únicamente con fines de diagnóstico.
 */
public final class PackManifestData extends SavedData {
  public static final String DATA_NAME = "rpg_core_pack_manifest";
  public static final SavedData.Factory<PackManifestData> FACTORY = new SavedData.Factory<>(
      PackManifestData::new, PackManifestData::load, null);

  private final Map<String, String> packs = new TreeMap<>();

  public PackManifestData() {}

  public static PackManifestData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  public static PackManifestData load(CompoundTag tag, HolderLookup.Provider registries) {
    PackManifestData data = new PackManifestData();
    ListTag list = tag.getList("packs", Tag.TAG_COMPOUND);
    for (Tag element : list) {
      if (!(element instanceof CompoundTag entry)) {
        continue;
      }
      String modId = entry.getString("modId");
      String version = entry.getString("version");
      if (!modId.isEmpty()) {
        data.packs.put(modId, version);
      }
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (Map.Entry<String, String> entry : packs.entrySet()) {
      CompoundTag item = new CompoundTag();
      item.putString("modId", entry.getKey());
      item.putString("version", entry.getValue());
      list.add(item);
    }
    tag.put("packs", list);
    return tag;
  }

  /**
   * Actualiza el manifiesto detectando mods cuyo id comienza con {@code rpg_}.
   */
  public void updateFromModList() {
    packs.clear();
    ModList.get().getMods().forEach(modInfo -> {
      String modId = modInfo.getModId();
      if (modId.startsWith("rpg_")) {
        packs.put(modId, modInfo.getVersion().toString());
      }
    });
    setDirty();
  }

  public Map<String, String> getPacks() {
    return Collections.unmodifiableMap(packs);
  }
}
