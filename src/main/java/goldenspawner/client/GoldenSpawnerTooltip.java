package goldenspawner.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "goldenspawner", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GoldenSpawnerTooltip {

    private GoldenSpawnerTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains("gs_marker")) return;

        List<ITextComponent> tooltip = event.getToolTip();
        tooltip.add(new StringTextComponent(""));
        tooltip.add(new StringTextComponent("\u00a76\u00a7l\u2550\u2550\u2550 \u0417\u043e\u043b\u043e\u0442\u043e\u0439 \u0421\u043f\u0430\u0432\u043d\u0435\u0440 \u2550\u2550\u2550"));

        String mobType = tag.contains("gs_mob_type") ? tag.getString("gs_mob_type") : null;
        if (mobType != null && !mobType.isEmpty()) {
            tooltip.add(new StringTextComponent("\u00a7e\u041c\u043e\u0431: \u00a7f" + translateMobName(mobType)));
        }

        if (tag.contains("gs_sword_item")) {
            String swordStr = tag.getString("gs_sword_item");
            try {
                JsonObject swordJson = new JsonParser().parse(swordStr).getAsJsonObject();
                String swordName = parseSwordName(swordJson);
                String swordType = swordJson.has("type") ? swordJson.get("type").getAsString() : "unknown";

                if (swordName != null && !swordName.isEmpty()) {
                    tooltip.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7b" + swordName));
                } else {
                    tooltip.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7f" + translateSwordType(swordType)));
                }
                if (swordJson.has("enchants")) {
                    addEnchantLine(tooltip, swordJson.getAsJsonObject("enchants"));
                }
            } catch (Exception e) {
                tooltip.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7c\u043d\u0435\u0442"));
            }
        }

        if (tag.contains("gs_kills_total")) {
            tooltip.add(new StringTextComponent("\u00a7e\u0423\u0431\u0438\u0442\u043e: \u00a7f" + formatNumber(tag.getInt("gs_kills_total"))));
        }

        if (tag.contains("gs_kills_remaining")) {
            int remaining = tag.getInt("gs_kills_remaining");
            if (remaining > 0) {
                tooltip.add(new StringTextComponent("\u00a7e\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: \u00a7c" + formatNumber(remaining)));
            } else {
                tooltip.add(new StringTextComponent("\u00a7e\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: \u00a7a0"));
            }
        }

        if (tag.contains("gs_total_eggs")) {
            int eggs = tag.getInt("gs_total_eggs");
            double chance = breakdownChance(eggs);
            int maxEggs = maxEggsBeforeBreak();
            int remaining = maxEggs - eggs;
            tooltip.add(new StringTextComponent("\u00a7e\u042f\u0438\u0446: \u00a7f" + eggs + " / \u00a7c" + maxEggs));
            String chanceColor = chance < 30.0 ? "\u00a7a" : chance < 60.0 ? "\u00a7e" : "\u00a7c";
            tooltip.add(new StringTextComponent("\u00a7e\u0428\u0430\u043d\u0441 \u043f\u043e\u043b\u043e\u043c\u043a\u0438: " + chanceColor + String.format("%.1f%%", chance)));
            if (remaining > 0) {
                tooltip.add(new StringTextComponent("\u00a7e\u0415\u0449\u0451 \u043c\u043e\u0436\u043d\u043e: \u00a7a~" + remaining + " \u044f\u0438\u0446"));
            } else {
                tooltip.add(new StringTextComponent("\u00a7e\u0415\u0449\u0451 \u043c\u043e\u0436\u043d\u043e: \u00a7c0"));
            }
        }

    }

    private static void addEnchantLine(List<ITextComponent> tooltip, JsonObject enchants) {
        StringTextComponent line = new StringTextComponent("\u00a7e\u0417\u0430\u0447. \u043c\u0435\u0447\u0430: ");
        boolean first = true;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : enchants.entrySet()) {
            String enchId = entry.getKey().replace("minecraft:", "").replace("enchantments:", "").toLowerCase();
            int lvl = entry.getValue().getAsInt();
            String name = translateEnchant(enchId);
            String color = enchId.equals("mob-farmer-enchant") ? "\u00a7a" : "\u00a77";
            if (!first) line.append(new StringTextComponent("\u00a77, "));
            first = false;
            line.append(new StringTextComponent(color + name + " " + toRoman(lvl)));
        }
        tooltip.add(line);
    }

    private static String parseSwordName(JsonObject json) {
        if (!json.has("name")) return null;
        String raw = json.get("name").getAsString();
        return raw.isEmpty() ? null : stripColorCodes(raw);
    }

    private static double breakdownChance(int eggs) {
        double n = eggs;
        return 1.9 + 3.4 * (n - 1) + 0.5 * (n - 1) * (n - 2);
    }

    private static int maxEggsBeforeBreak() {
        for (int i = 1; i <= 50; i++) {
            if (breakdownChance(i) >= 100.0) return i - 1;
        }
        return 50;
    }

    private static String translateEnchant(String id) {
        switch (id) {
            case "sharpness": return "\u041e\u0441\u0442\u0440\u043e\u0442\u0430";
            case "smite": return "\u041d\u0435\u0431\u0435\u0441\u043d\u0430\u044f \u043a\u0430\u0440\u0430";
            case "bane_of_arthropods": return "\u0411\u0438\u0447 \u0447\u043b\u0435\u043d\u0438\u0441\u0442\u043e\u043d\u043e\u0433\u0438\u0445";
            case "knockback": return "\u041e\u0442\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u0435";
            case "fire_aspect": return "\u0417\u0430\u0433\u043e\u0432\u043e\u0440 \u043e\u0433\u043d\u044f";
            case "looting": return "\u0414\u043e\u0431\u044b\u0447\u0430";
            case "sweeping": return "\u0420\u0430\u0437\u044f\u0449\u0438\u0439 \u043a\u043b\u0438\u043d\u043e\u043a";
            case "efficiency": return "\u042d\u0444\u0444\u0435\u043a\u0442\u0438\u0432\u043d\u043e\u0441\u0442\u044c";
            case "silk_touch": return "\u041c\u0430\u0433\u0438\u0447. \u0440\u0443\u043a\u0430";
            case "fortune": return "\u0423\u0434\u0430\u0447\u0430";
            case "unbreaking": return "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c";
            case "mending": return "\u041f\u043e\u0447\u0438\u043d\u043a\u0430";
            case "thorns": return "\u0428\u0438\u043f\u044b";
            case "protection": return "\u0417\u0430\u0449\u0438\u0442\u0430";
            case "projectile_protection": return "\u0417\u0430\u0449. \u043e\u0442 \u0441\u0442\u0440\u0435\u043b\u043a\u043e\u0432";
            case "fire_protection": return "\u0417\u0430\u0449. \u043e\u0442 \u043e\u0433\u043d\u044f";
            case "blast_protection": return "\u0417\u0430\u0449. \u043e\u0442 \u0432\u0437\u0440\u044b\u0432\u0430";
            case "respiration": return "\u0412\u043e\u0434\u043e\u0434\u044b\u0448\u0430\u043d\u0438\u0435";
            case "aqua_affinity": return "\u0412\u043e\u0434\u043e\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c";
            case "depth_strider": return "\u0421\u0442\u0440. \u0433\u043b\u0443\u0431\u0438\u043d";
            case "frost_walker": return "\u041c\u043e\u0440\u043e\u0437\u043d. \u0445\u043e\u0434\u043e\u043a";
            case "binding_curse": return "\u041f\u0440\u043e\u043a\u043b. \u0441\u0432\u044f\u0437\u044b\u0432\u0430\u043d\u0438\u044f";
            case "vanishing_curse": return "\u041f\u0440\u043e\u043a\u043b. \u0438\u0441\u0447\u0435\u0437\u043d\u043e\u0432\u0430\u043d\u0438\u044f";
            case "power": return "\u041c\u043e\u0449\u044c";
            case "punch": return "\u0423\u0434\u0430\u0440";
            case "flame": return "\u041f\u043b\u0430\u043c\u044f";
            case "infinity": return "\u0411\u0435\u0441\u043a\u043e\u043d\u0435\u0447\u043d\u043e\u0441\u0442\u044c";
            case "luck_of_the_sea": return "\u0423\u0434\u0430\u0447\u0430 \u043c\u043e\u0440\u044f";
            case "lure": return "\u041f\u0440\u0438\u0432\u043b\u0435\u043a\u0430\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c";
            case "loyalty": return "\u0412\u0435\u0440\u043d\u043e\u0441\u0442\u044c";
            case "channeling": return "\u041a\u0430\u043d\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044f";
            case "impaling": return "\u041f\u0440\u043e\u043d\u0438\u0437\u0430\u043d\u0438\u0435";
            case "riptide": return "\u0412\u043e\u043b\u043d\u0430";
            case "multishot": return "\u041c\u043d\u043e\u0433\u043e\u0437\u0430\u0440\u044f\u0434\u043d\u043e\u0441\u0442\u044c";
            case "quick_charge": return "\u0411\u044b\u0441\u0442\u0440. \u0437\u0430\u0440\u044f\u0434\u043a\u0430";
            case "piercing": return "\u041f\u0440\u043e\u043d\u0438\u0437\u0430\u043d\u0438\u0435";
            case "soul_speed": return "\u0421\u043a\u043e\u0440. \u0434\u0443\u0448\u0438";
            case "critical-enchant-custom": return "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439";
            case "destroyer-enchant-custom": return "\u0420\u0430\u0437\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c";
            case "mob-farmer-enchant": return "\u0424\u0430\u0440\u043c\u0435\u0440";
            case "rich-enchant-custom": return "\u0411\u043e\u0433\u0430\u0447";
            case "filter-enchant-custom":
            case "filter": return "\u0424\u0438\u043b\u044c\u0442\u0440";
            default:
                return id.replace("_", " ").replace("-", " ");
        }
    }


    private static String translateMobName(String mobType) {
        switch (mobType.toUpperCase()) {
            case "ZOMBIE": return "\u0417\u043e\u043c\u0431\u0438";
            case "SKELETON": return "\u0421\u043a\u0435\u043b\u0435\u0442";
            case "SPIDER": return "\u041f\u0430\u0443\u043a";
            case "CREEPER": return "\u041a\u0440\u0438\u043f\u0435\u0440";
            case "ENDERMAN": return "\u042d\u043d\u0434\u0435\u0440\u043c\u0435\u043d";
            case "CAVE_SPIDER": return "\u041f\u0435\u0449. \u043f\u0430\u0443\u043a";
            case "BLAZE": return "\u0411\u043b\u0435\u0439\u0437";
            case "GHAST": return "\u0413\u0430\u0441\u0442";
            case "MAGMA_CUBE": return "\u041c\u0430\u0433\u043c. \u043a\u0443\u0431";
            case "SLIME": return "\u0421\u043b\u0430\u0439\u043c";
            case "WITCH": return "\u0412\u0435\u0434\u044c\u043c\u0430";
            case "PIGLIN": return "\u041f\u0438\u0433\u043b\u0438\u043d";
            case "PIGLIN_BRUTE": return "\u041f\u0438\u0433\u043b\u0438\u043d-\u0431\u0440\u0443\u0442";
            case "HOGLIN": return "\u0425\u043e\u0433\u043b\u0438\u043d";
            case "ZOMBIFIED_PIGLIN": return "\u0417\u043e\u043c\u0431. \u043f\u0438\u0433\u043b\u0438\u043d";
            case "SILVERFISH": return "\u0421\u0435\u0440\u0435\u0431\u0440. \u0440\u044b\u0431\u043a\u0430";
            case "IRON_GOLEM": return "\u0416\u0435\u043b. \u0433\u043e\u043b\u0435\u043c";
            case "SHULKER": return "\u0428\u0430\u043b\u043a\u0435\u0440";
            case "GUARDIAN": return "\u0421\u0442\u0440\u0430\u0436";
            case "ELDER_GUARDIAN": return "\u0414\u0440. \u0441\u0442\u0440\u0430\u0436";
            case "WITHER_SKELETON": return "\u0412\u0438\u0437\u0435\u0440-\u0441\u043a\u0435\u043b\u0435\u0442";
            case "PHANTOM": return "\u0424\u0430\u043d\u0442\u043e\u043c";
            case "VEX": return "\u0412\u0435\u043a\u0441";
            case "EVOKER": return "\u0412\u044b\u0437\u044b\u0432\u0430\u0442\u0435\u043b\u044c";
            case "VINDICATOR": return "\u0420\u0430\u0441\u0441\u0443\u0434\u0438\u0442\u0435\u043b\u044c";
            case "PILLAGER": return "\u041c\u0430\u0440\u043e\u0434\u0451\u0440";
            case "RAVAGER": return "\u0420\u0430\u0437\u043e\u0440\u0438\u0442\u0435\u043b\u044c";
            case "DROWNED": return "\u0423\u0442\u043e\u043f\u043b\u0435\u043d\u043d\u0438\u043a";
            case "HUSK": return "\u041f\u0443\u0441\u0442. \u0437\u043e\u043c\u0431\u0438";
            case "STRAY": return "\u0421\u0442\u0440\u044d\u0439";
            case "WARDEN": return "\u0421\u0442\u0440\u0435\u0436";
            case "ALLAY": return "\u042d\u043b\u043b\u0435\u0439";
            default: return mobType;
        }
    }

    private static String translateSwordType(String type) {
        switch (type) {
            case "minecraft:wooden_sword": return "\u0414\u0435\u0440. \u043c\u0435\u0447";
            case "minecraft:stone_sword": return "\u041a\u0430\u043c. \u043c\u0435\u0447";
            case "minecraft:iron_sword": return "\u0416\u0435\u043b. \u043c\u0435\u0447";
            case "minecraft:golden_sword": return "\u0417\u043e\u043b. \u043c\u0435\u0447";
            case "minecraft:diamond_sword": return "\u0410\u043b\u043c. \u043c\u0435\u0447";
            case "minecraft:netherite_sword": return "\u041d\u0435\u0437. \u043c\u0435\u0447";
            default: return type.replace("minecraft:", "").replace("_", " ");
        }
    }

    private static String formatSpawnConfig(CompoundNBT tag) {
        int count = tag.contains("gs_spawn_count") ? tag.getInt("gs_spawn_count") : -1;
        int range = tag.contains("gs_spawn_range") ? tag.getInt("gs_spawn_range") : -1;
        int minDelay = tag.contains("gs_min_spawn_delay") ? tag.getInt("gs_min_spawn_delay") : -1;
        int maxDelay = tag.contains("gs_max_spawn_delay") ? tag.getInt("gs_max_spawn_delay") : -1;

        StringBuilder sb = new StringBuilder();
        if (count >= 0) sb.append("\u00d7").append(count);
        if (range >= 0) sb.append(" | \u0440\u0430\u0434. ").append(range);
        if (minDelay >= 0 && maxDelay >= 0) sb.append(" | \u0437\u0430\u0434. ").append(minDelay).append("-").append(maxDelay);
        return sb.length() > 0 ? sb.toString() : "";
    }

    private static String formatNumber(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000.0);
        if (n >= 1000) return String.format("%.1fK", n / 1000.0);
        return String.valueOf(n);
    }

    private static String stripColorCodes(String s) {
        return s.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    private static String toRoman(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(level);
        }
    }
}
