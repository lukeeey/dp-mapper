package resourcegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import resourcegen.types.DyeColor;
import resourcegen.types.StoneType;
import resourcegen.types.WoodType;

import java.io.*;
import java.util.*;

public class Main {
    // Identifier, Runtime id
    private Map<String, JavaItem> JAVA_ITEMS = new HashMap<>();
    private Map<String, JavaItem> JAVA_BLOCKS = new HashMap<>();

    // Identifier, Runtime id, Data
    private Map<String, BedrockItem> BEDROCK_ITEMS = new HashMap<>();
    private Map<String, BedrockItem> BEDROCK_BLOCKS = new HashMap<>();

    public static void main(String[] args) {
        new Main().init();
    }

    public void init() {
        loadJavaData();
        loadBedrockItems();
        createBedrockItemJSONData();

        loadBedrockBlocks();
        createBedrockBlockJSONData();
    }

    public void loadJavaData() {
        System.out.println("Loading java items");

        try {
            File file = new File("java_items.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginArray();
            for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                Optional<Item> item = IRegistry.ITEM.getOptional(key);
                if (item.isPresent()) {
                    String name = key.getNamespace() + ":" + key.getKey();
                    JAVA_ITEMS.put(name, new JavaItem(name, Item.getId(item.get())));

                    jsonWriter.beginObject();
                    jsonWriter.name("identifier").value(key.getNamespace() + ":" + key.getKey());
                    jsonWriter.name("protocol_id").value(Item.getId(item.get()));
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endArray();
            jsonWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            File file = new File("java_blocks.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginArray();
            for (MinecraftKey key : IRegistry.BLOCK.keySet()) {
                Optional<Block> block = IRegistry.BLOCK.getOptional(key);
                if (block.isPresent()) {
                    String name = key.getNamespace() + ":" + key.getKey();
                    JAVA_BLOCKS.put(name, new JavaItem(name, Block.REGISTRY_ID.getId(block.get().getBlockData())));

                    jsonWriter.beginObject();
                    jsonWriter.name("identifier").value(key.getNamespace() + ":" + key.getKey());
                    jsonWriter.name("protocol_id").value(Block.REGISTRY_ID.getId(block.get().getBlockData()));
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endArray();
            jsonWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void loadBedrockItems() {
        System.out.println("Loading bedrock items");
        InputStream stream = Main.class.getResourceAsStream("/runtime_item_ids.json");

        if (stream == null) {
            throw new RuntimeException("Bedrock item Runtime ID table not found");
        }

        ObjectMapper mapper2 = new ObjectMapper();
        ArrayList<HashMap> s = new ArrayList<>();
        try {
            s = mapper2.readValue(stream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (HashMap e : s) {
            if (!BEDROCK_ITEMS.containsKey(e.get("name"))) {
                BedrockItem bedrockItem = new BedrockItem((String) e.get("name"), ((int) e.get("id")), 0);
                BEDROCK_ITEMS.put(bedrockItem.name, bedrockItem);
            }
        }
    }

    public void loadBedrockBlocks() {
        System.out.println("Loading bedrock blocks");
        InputStream stream = Main.class.getResourceAsStream("/runtime_block_ids.json");

        if (stream == null) {
            throw new RuntimeException("Bedrock block Runtime ID table not found");
        }

        ObjectMapper mapper2 = new ObjectMapper();
        ArrayList<HashMap> s = new ArrayList<>();
        try {
            s = mapper2.readValue(stream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (HashMap e : s) {
            if (!BEDROCK_BLOCKS.containsKey(e.get("name"))) {
                BedrockItem bedrockBlock = new BedrockItem((String) e.get("name"), ((int) e.get("id")), 0);
                BEDROCK_BLOCKS.put(bedrockBlock.name, bedrockBlock);
            }
        }
    }


    public void createBedrockItemJSONData() {
        Map<String, Map<Integer, String>> BEDROCK_TO_JAVA = new HashMap<>();
        Map<String, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

        try {
            File file = new File("item_mappings.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            Map<String, JavaItem> java = JAVA_ITEMS;

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginArray();

            Map<JavaItem, List<BedrockItem>> conversions = new HashMap<>();

            for(Map.Entry<String, JavaItem> javaItem : JAVA_ITEMS.entrySet()) {
                for(Map.Entry<String, BedrockItem> bedrockItem : BEDROCK_ITEMS.entrySet()) {
                    if (javaItem.getKey().contains("oak_")) {
                        String stripped = javaItem.getKey().replaceAll("oak_", "").replaceAll("terracotta", "stained_hardened_clay");

                        if (!stripped.equalsIgnoreCase(bedrockItem.getKey()) || bedrockItem.getValue().getData() != 0)
                            continue;

                        for (WoodType woodType : WoodType.values()) {
                            JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("oak_", woodType.name().toLowerCase() + "_"));
                            conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
                            conversions.get(j).add(new BedrockItem(bedrockItem.getValue().getName(), bedrockItem.getValue().getId(), woodType.ordinal()));
                        }
                    } else if (javaItem.getKey().equalsIgnoreCase("minecraft:stone") && bedrockItem.getKey().equalsIgnoreCase("minecraft:stone")) {
                        for (StoneType stoneType : StoneType.values()) {
                            JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("stone", stoneType.name().toLowerCase()));
                            conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
                            conversions.get(j).add(new BedrockItem(bedrockItem.getValue().getName(), bedrockItem.getValue().getId(), stoneType.ordinal()));
                        }
                    } else if (bedrockItem.getValue().getName().equalsIgnoreCase(javaItem.getKey())) {
                        JavaItem item = javaItem.getValue();
                        conversions.computeIfAbsent(item, (x) -> new ArrayList<>());
                        conversions.get(item).add(bedrockItem.getValue());
                    }
                }
            }

            for (DyeColor dyeColor : DyeColor.values()) {
                JavaItem j = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_dye");
                conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
                BedrockItem b = BEDROCK_ITEMS.get("minecraft:dye");
                conversions.get(j).add(new BedrockItem(b.getName(), b.getId(), 15 - dyeColor.ordinal()));

                JavaItem j2 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_wool");
                conversions.computeIfAbsent(j2, (x) -> new ArrayList<>());
                BedrockItem b2 = BEDROCK_ITEMS.get("minecraft:wool");
                conversions.get(j2).add(new BedrockItem(b2.getName(), b2.getId(), dyeColor.ordinal()));

                JavaItem j3 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_stained_glass");
                conversions.computeIfAbsent(j3, (x) -> new ArrayList<>());
                BedrockItem b3 = BEDROCK_ITEMS.get("minecraft:stained_glass");
                conversions.get(j3).add(new BedrockItem(b3.getName(), b3.getId(), dyeColor.ordinal()));

                JavaItem j4 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_stained_glass_pane");
                conversions.computeIfAbsent(j4, (x) -> new ArrayList<>());
                BedrockItem b4 = BEDROCK_ITEMS.get("minecraft:stained_glass_pane");
                conversions.get(j4).add(new BedrockItem(b4.getName(), b4.getId(), dyeColor.ordinal()));
            }

            // Custom remaps
            JavaItem j = java.get("minecraft:nether_star");
            conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
            conversions.get(j).add(BEDROCK_ITEMS.get("minecraft:netherstar"));

            for(Map.Entry<JavaItem, List<BedrockItem>> entry : conversions.entrySet()) {
                for (BedrockItem item : entry.getValue()) {
                    jsonWriter.beginObject();
                    jsonWriter.name("java_identifier").value(entry.getKey().getIdentifier());
                    jsonWriter.name("java_protocol_id").value(entry.getKey().getId());
                    jsonWriter.name("bedrock_identifier").value(item.getName());
                    jsonWriter.name("bedrock_runtime_id").value(item.getId());
                    jsonWriter.name("bedrock_data").value(item.getData());
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endArray();
            jsonWriter.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void createBedrockBlockJSONData() {
        Map<String, Map<Integer, String>> BEDROCK_TO_JAVA = new HashMap<>();
        Map<String, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

        try {
            File file = new File("block_mappings.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            Map<String, JavaItem> java = JAVA_BLOCKS;

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginArray();

            Map<JavaItem, List<BedrockItem>> conversions = new HashMap<>();

            for(Map.Entry<String, JavaItem> javaItem : JAVA_BLOCKS.entrySet()) {
                for(Map.Entry<String, BedrockItem> bedrockItem : BEDROCK_BLOCKS.entrySet()) {
                    if (javaItem.getKey().contains("oak_")) {
                        String stripped = javaItem.getKey().replaceAll("oak_", "").replaceAll("terracotta", "stained_hardened_clay");

                        if (!stripped.equalsIgnoreCase(bedrockItem.getKey()) || bedrockItem.getValue().getData() != 0)
                            continue;

                        for (WoodType woodType : WoodType.values()) {
                            JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("oak_", woodType.name().toLowerCase() + "_"));
                            conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
                            conversions.get(j).add(new BedrockItem(bedrockItem.getValue().getName(), bedrockItem.getValue().getId(), woodType.ordinal()));
                        }
                    } else if (javaItem.getKey().equalsIgnoreCase("minecraft:stone") && bedrockItem.getKey().equalsIgnoreCase("minecraft:stone")) {
                        for (StoneType stoneType : StoneType.values()) {
                            JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("stone", stoneType.name().toLowerCase()));
                            conversions.computeIfAbsent(j, (x) -> new ArrayList<>());
                            conversions.get(j).add(new BedrockItem(bedrockItem.getValue().getName(), bedrockItem.getValue().getId(), stoneType.ordinal()));
                        }
                    } else if (bedrockItem.getValue().getName().equalsIgnoreCase(javaItem.getKey())) {
                        JavaItem item = javaItem.getValue();
                        conversions.computeIfAbsent(item, (x) -> new ArrayList<>());
                        conversions.get(item).add(bedrockItem.getValue());
                    }
                }
            }

            for (DyeColor dyeColor : DyeColor.values()) {
                JavaItem j2 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_wool");
                conversions.computeIfAbsent(j2, (x) -> new ArrayList<>());
                BedrockItem b2 = BEDROCK_BLOCKS.get("minecraft:wool");
                conversions.get(j2).add(new BedrockItem(b2.getName(), b2.getId(), dyeColor.ordinal()));

                JavaItem j3 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_stained_glass");
                conversions.computeIfAbsent(j3, (x) -> new ArrayList<>());
                BedrockItem b3 = BEDROCK_BLOCKS.get("minecraft:stained_glass");
                conversions.get(j3).add(new BedrockItem(b3.getName(), b3.getId(), dyeColor.ordinal()));

                JavaItem j4 = java.get("minecraft:" + dyeColor.name().toLowerCase() + "_stained_glass_pane");
                conversions.computeIfAbsent(j4, (x) -> new ArrayList<>());
                BedrockItem b4 = BEDROCK_BLOCKS.get("minecraft:stained_glass_pane");
                conversions.get(j4).add(new BedrockItem(b4.getName(), b4.getId(), dyeColor.ordinal()));
            }

            for(Map.Entry<JavaItem, List<BedrockItem>> entry : conversions.entrySet()) {
                for (BedrockItem item : entry.getValue()) {
                    jsonWriter.beginObject();
                    jsonWriter.name("java_identifier").value(entry.getKey().getIdentifier());
                    jsonWriter.name("java_protocol_id").value(entry.getKey().getId());
                    jsonWriter.name("bedrock_identifier").value(item.getName());
                    jsonWriter.name("bedrock_runtime_id").value(item.getId());
                    jsonWriter.name("bedrock_data").value(item.getData());
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endArray();
            jsonWriter.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    @AllArgsConstructor
    @Getter
    public static class BedrockItem {
        @JsonProperty
        public String name;
        @JsonProperty
        public int id;

        public int data;
    }

    @Getter
    @AllArgsConstructor
    public static class JavaItem {
        public String identifier;
        public int id;
    }
}
