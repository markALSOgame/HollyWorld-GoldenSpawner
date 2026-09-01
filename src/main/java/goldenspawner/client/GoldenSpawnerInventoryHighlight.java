package goldenspawner.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "goldenspawner", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GoldenSpawnerInventoryHighlight {

    private static final int PANEL_W = 176;
    private static final int BG = 0xF0100010;
    private static final int BORDER_TOP = 0xFF5028FF;
    private static final int BORDER_BOT = 0xFF287FFF;

    private GoldenSpawnerInventoryHighlight() {
    }

    @SubscribeEvent
    public static void onCancelVanillaTooltip(RenderTooltipEvent.Pre event) {
        CompoundNBT tag = event.getStack().getTag();
        if (tag != null && tag.contains("gs_marker")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof ContainerScreen)) return;
        ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();

        int mx = event.getMouseX();
        int my = event.getMouseY();

        MatrixStack ms = event.getMatrixStack();

        ItemStack hovered = null;

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            CompoundNBT tag = stack.getTag();
            if (tag == null || !tag.contains("gs_marker")) continue;

            boolean[] an = analyze(stack);

            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;

            if (an[0]) {
                AbstractGui.fill(ms, x, y, x + 16, y + 16, an[1] ? 0x5900FF00 : 0x599900FF);
            }

            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
                hovered = stack;
            }
        }

        if (hovered == null) return;

        List<ITextComponent> lines = buildLines(hovered);

        FontRenderer font = screen.getMinecraft().font;

        int w = 80;
        for (ITextComponent line : lines) w = Math.max(w, font.width(line) + 12);

        int invLeft = (screen.width - 176) / 2;
        int invTop = (screen.height - 166) / 2;

        int h = lines.size() * 11 + 10;
        int px = Math.max(2, invLeft - 10 - w);
        int py = Math.max(2, Math.min(invTop, screen.height - h - 4));

        AbstractGui.fill(ms, px, py, px + w, py + h, BG);
        AbstractGui.fill(ms, px, py, px + w, py + 1, BORDER_TOP);
        AbstractGui.fill(ms, px, py + h - 1, px + w, py + h, BORDER_BOT);
        AbstractGui.fill(ms, px, py, px + 1, py + h, BORDER_TOP);
        AbstractGui.fill(ms, px + w - 1, py, px + w, py + h, BORDER_BOT);

        int ty = py + 6;
        for (ITextComponent line : lines) {
            font.drawShadow(ms, line, px + 6, ty, 0xFFFFFF);
            ty += 11;
        }
    }

    private static boolean[] analyze(ItemStack stack) {
        boolean[] res = new boolean[2];
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains("gs_sword_item")) return res;
        try {
            JsonObject obj = new JsonParser().parse(tag.getString("gs_sword_item")).getAsJsonObject();
            boolean okName = obj.has("name") && !obj.get("name").getAsString().isEmpty();
            boolean okType = obj.has("type") && !obj.get("type").getAsString().equals("unknown");
            res[0] = okName || okType;
            if (res[0] && obj.has("enchants")) {
                res[1] = obj.getAsJsonObject("enchants").has("enchantments:mob-farmer-enchant");
            }
        } catch (Exception ignored) {
        }
        return res;
    }

    private static List<ITextComponent> buildLines(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        List<ITextComponent> lines = new ArrayList<>();
        lines.add(new StringTextComponent("\u00a76\u0417\u043e\u043b\u043e\u0442\u043e\u0439 \u0421\u043f\u0430\u0432\u043d\u0435\u0440"));

        if (tag.contains("gs_mob_type") && !tag.getString("gs_mob_type").isEmpty()) {
            lines.add(new StringTextComponent("\u00a7e\u041c\u043e\u0431: \u00a7f" + translateMobName(tag.getString("gs_mob_type"))));
        }

        boolean hasSword = false;
        if (tag.contains("gs_sword_item")) {
            try {
                JsonObject obj = new JsonParser().parse(tag.getString("gs_sword_item")).getAsJsonObject();
                boolean okName = obj.has("name") && !obj.get("name").getAsString().isEmpty();
                boolean okType = obj.has("type") && !obj.get("type").getAsString().equals("unknown");
                if (okName || okType) {
                    hasSword = true;
                    if (okName) {
                        lines.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7b" + stripColorCodes(obj.get("name").getAsString())));
                    } else {
                        lines.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7f" + translateSwordType(obj.get("type").getAsString())));
                    }
                    if (obj.has("enchants")) {
                        JsonObject enchants = obj.getAsJsonObject("enchants");
                        if (enchants.size() > 0) {
                            lines.add(new StringTextComponent("\u00a7e\u0427\u0430\u0440\u044b \u043c\u0435\u0447\u0430:"));
                            for (Map.Entry<String, JsonElement> en : enchants.entrySet()) {
                                String id = en.getKey().replace("minecraft:", "").replace("enchantments:", "").toLowerCase();
                                String col = id.equals("mob-farmer-enchant") ? "\u00a7a" : "\u00a77";
                                lines.add(new StringTextComponent(col + "  " + translateEnchant(id) + " " + toRoman(en.getValue().getAsInt())));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (!hasSword) lines.add(new StringTextComponent("\u00a7e\u041c\u0435\u0447: \u00a7c\u043d\u0435\u0442"));

        if (tag.contains("gs_kills_total")) {
            lines.add(new StringTextComponent("\u00a7e\u0423\u0431\u0438\u0442\u043e: \u00a7f" + formatNumber(tag.getInt("gs_kills_total"))));
        }
        if (tag.contains("gs_kills_remaining")) {
            lines.add(new StringTextComponent("\u00a7e\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: \u00a7c" + formatNumber(tag.getInt("gs_kills_remaining"))));
        }
        if (tag.contains("gs_total_eggs")) {
            int eggs = tag.getInt("gs_total_eggs");
            double ch = breakdownChance(eggs);
            String cc = ch < 30.0 ? "\u00a7a" : ch < 60.0 ? "\u00a7e" : "\u00a7c";
            lines.add(new StringTextComponent("\u00a7e\u00a7l\u0428\u0430\u043d\u0441 \u043f\u043e\u043b\u043e\u043c\u043a\u0438: \u00a7r" + cc + String.format("%.1f%%", ch)));
        }

        addServerLore(tag, lines);

        return lines;
    }

    private static void addServerLore(CompoundNBT tag, List<ITextComponent> lines) {
        if (!tag.contains("display")) return;
        CompoundNBT display = tag.getCompound("display");
        if (!display.contains("Lore", 9)) return;
        net.minecraft.nbt.ListNBT lore = display.getList("Lore", 8);
        for (int i = 0; i < lore.size(); i++) {
            String json = lore.getString(i);
            String s;
            try {
                s = loreText(new JsonParser().parse(json));
            } catch (Exception e) {
                s = json;
            }
            if (s == null) s = "";
            String plain = stripColorCodes(s).trim();
            if (plain.isEmpty()) continue;
            if (plain.startsWith("\u041e\u0441\u043e\u0431\u0435\u043d\u043d\u043e\u0441\u0442\u0438")) continue;
            if (plain.startsWith("-")) continue;
            if (plain.startsWith("\u0428\u0430\u043d\u0441 \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0438\u044f")) {
                int idx = plain.indexOf(':');
                String value = idx >= 0 ? plain.substring(idx + 1).trim() : "";
                lines.add(new StringTextComponent("\u00a7a\u00a7l\u0428\u0430\u043d\u0441 \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0438\u044f: \u00a7c" + value));
                continue;
            }
            lines.add(new StringTextComponent(s));
        }
    }

    private static String loreText(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        if (!el.isJsonObject()) return "";
        JsonObject obj = el.getAsJsonObject();
        StringBuilder sb = new StringBuilder();
        if (obj.has("color")) sb.append(colorCode(obj.get("color").getAsString()));
        if (obj.has("bold") && obj.get("bold").getAsBoolean()) sb.append("\u00a7l");
        if (obj.has("italic") && obj.get("italic").getAsBoolean()) sb.append("\u00a7o");
        if (obj.has("underlined") && obj.get("underlined").getAsBoolean()) sb.append("\u00a7n");
        if (obj.has("strikethrough") && obj.get("strikethrough").getAsBoolean()) sb.append("\u00a7m");
        if (obj.has("obfuscated") && obj.get("obfuscated").getAsBoolean()) sb.append("\u00a7k");
        if (obj.has("text")) sb.append(obj.get("text").getAsString());
        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("extra")) {
                sb.append(loreText(e));
            }
        }
        return sb.toString();
    }

    private static String colorCode(String color) {
        switch (color) {
            case "black": return "\u00a70";
            case "dark_blue": return "\u00a71";
            case "dark_green": return "\u00a72";
            case "dark_aqua": return "\u00a73";
            case "dark_red": return "\u00a74";
            case "dark_purple": return "\u00a75";
            case "gold": return "\u00a76";
            case "gray": return "\u00a77";
            case "dark_gray": return "\u00a78";
            case "blue": return "\u00a79";
            case "green": return "\u00a7a";
            case "aqua": return "\u00a7b";
            case "red": return "\u00a7c";
            case "light_purple": return "\u00a7d";
            case "yellow": return "\u00a7e";
            case "white": return "\u00a7f";
            default: return "";
        }
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

    private static String translateEnchant(String id) {
        switch (id) {
            case "sharpness": return "\u041e\u0441\u0442\u0440\u043e\u0442\u0430";
            case "smite": return "\u041d\u0435\u0431\u0435\u0441\u043d\u0430\u044f \u043a\u0430\u0440\u0430";
            case "bane_of_arthropods": return "\u0411\u0438\u0447 \u0447\u043b\u0435\u043d\u0438\u0441\u0442\u043e\u043d\u043e\u0433\u0438\u0445";
            case "knockback": return "\u041e\u0442\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u0435";
            case "fire_aspect": return "\u0417\u0430\u0433\u043e\u0432\u043e\u0440 \u043e\u0433\u043d\u044f";
            case "looting": return "\u0414\u043e\u0431\u044b\u0447\u0430";
            case "sweeping": return "\u0420\u0430\u0437\u044f\u0449\u0438\u0439 \u043a\u043b\u0438\u043d\u043e\u043a";
            case "unbreaking": return "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c";
            case "mending": return "\u041f\u043e\u0447\u0438\u043d\u043a\u0430";
            case "critical-enchant-custom": return "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439";
            case "destroyer-enchant-custom": return "\u0420\u0430\u0437\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c";
            case "mob-farmer-enchant": return "\u0424\u0430\u0440\u043c\u0435\u0440";
            case "rich-enchant-custom": return "\u0411\u043e\u0433\u0430\u0447";
            case "filter-enchant-custom":
            case "filter": return "\u0424\u0438\u043b\u044c\u0442\u0440";
            default: return id.replace("_", " ").replace("-", " ");
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
}
