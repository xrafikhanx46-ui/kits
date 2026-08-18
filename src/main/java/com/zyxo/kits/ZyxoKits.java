package com.zyxo.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ZyxoKits extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey kitKey;
    private NamespacedKey claimedKey;
    private final List<String> kits = List.of("starter", "scout", "warrior", "guardian", "champion", "master", "conqueror", "immortal");

    @Override public void onEnable() {
        saveDefaultConfig();
        kitKey = new NamespacedKey(this, "kit");
        claimedKey = new NamespacedKey(this, "claimed");
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("kits")).setExecutor(this);
        Objects.requireNonNull(getCommand("kits")).setTabCompleter(this);
        getLogger().info("ZyxoKits enabled.");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (command.getName().equalsIgnoreCase("kits")) { openMenu(p); return true; }
        return false;
    }

    private void openMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, color(getConfig().getString("menu-title", "§8Zyxo Kits")));
        int[] slots = {0,1,2,3,4,5,6,7,8, 9,10,11,12,13,14,15,16,17};
        for (int i = 0; i < kits.size(); i++) {
            String kit = kits.get(i);
            String base = "kits." + kit;
            Material mat = material(getConfig().getString(base + ".icon", "SHULKER_BOX"), Material.SHULKER_BOX);
            String display = color(getConfig().getString(base + ".display", kit));
            ItemStack item = named(mat, display, List.of(
                    "§7Right Click §f→ §aClaim",
                    "§7Left Click §f→ §bPreview",
                    "",
                    p.hasPermission(getConfig().getString(base + ".permission", "zyxo.kit." + kit + ".claim"))
                            ? "§aYou have permission to claim this kit."
                            : "§cYou don't have permission to claim this kit."
            ));
            setTag(item, kitKey, kit);
            inv.setItem(slots[i], item);
        }
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, "§8", Collections.emptyList());
        for (int i = 18; i < 27; i++) inv.setItem(i, filler);
        p.openInventory(inv);
    }

    @EventHandler public void onDrag(InventoryDragEvent e) {
        if (isOurMenu(e.getView().getTitle()) || isOurPreview(e.getView().getTitle())) e.setCancelled(true);
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!isOurMenu(title) && !isOurPreview(title)) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        String kit = e.getCurrentItem().getItemMeta() == null ? null : e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
        if (kit == null) {
            if (isOurPreview(title) && e.getRawSlot() == 26) openMenu(p);
            return;
        }
        if (isOurMenu(title)) {
            if (e.isRightClick()) claim(p, kit);
            else if (e.isLeftClick()) openPreview(p, kit);
        }
    }

    private void claim(Player p, String kit) {
        String permission = getConfig().getString("kits." + kit + ".permission", "zyxo.kit." + kit + ".claim");
        if (!p.hasPermission(permission)) { p.sendMessage("§cYou need §f" + permission + " §cto claim this kit."); return; }
        boolean once = getConfig().getBoolean("claim-once", true);
        if (once && hasClaimed(p, kit)) { p.sendMessage("§cYou have already claimed the " + display(kit) + "§c kit."); return; }
        giveKit(p, kit);
        if (once) setClaimed(p, kit, true);
        p.sendMessage("§aSuccessfully claimed the " + display(kit) + "§a kit!");
        p.closeInventory();
    }

    private void openPreview(Player p, String kit) {
        Inventory inv = Bukkit.createInventory(null, 27, color(getConfig().getString("preview-title", "§8Preview: %kit%").replace("%kit%", display(kit))));
        List<ItemStack> items = kitItems(kit);
        int slot = 0;
        for (ItemStack item : items) { if (slot >= 26) break; inv.setItem(slot++, item); }
        inv.setItem(26, named(Material.BARRIER, "§cBack", List.of("§7Return to kits")));
        p.openInventory(inv);
    }

    private void giveKit(Player p, String kit) {
        for (ItemStack item : kitItems(kit)) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(item);
            left.values().forEach(x -> p.getWorld().dropItemNaturally(p.getLocation(), x));
        }
    }

    private List<ItemStack> kitItems(String kit) {
        return switch (kit) {
            case "starter" -> starter();
            case "scout" -> scout();
            case "warrior" -> netherKit(5, "Warrior", false);
            case "guardian" -> netherKit(6, "Guardian", false);
            case "champion" -> champion();
            case "master" -> master();
            case "conqueror" -> highMaceKit(9, "Conqueror", 7);
            case "immortal" -> highMaceKit(10, "Immortal", 10);
            default -> List.of();
        };
    }

    private List<ItemStack> starter() {
        List<ItemStack> out = new ArrayList<>();
        for (Material m : List.of(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS)) out.add(armor(m, 2, 1));
        out.add(tool(Material.IRON_SWORD, 2, 1));
        out.add(tool(Material.IRON_PICKAXE, 2, 1));
        out.add(item(Material.COOKED_BEEF, 32, "§aStarter Food"));
        return out;
    }

    private List<ItemStack> scout() {
        List<ItemStack> out = new ArrayList<>();
        for (Material m : List.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS)) {
            ItemStack x = armor(m, 4, 3); add(x, Enchantment.RESPIRATION, 3); add(x, Enchantment.AQUA_AFFINITY, 1); if (m == Material.DIAMOND_BOOTS) { add(x, Enchantment.FEATHER_FALLING, 4); add(x, Enchantment.DEPTH_STRIDER, 3); }
            out.add(x);
        }
        out.add(tool(Material.DIAMOND_SWORD, 5, 3));
        out.add(tool(Material.DIAMOND_PICKAXE, 5, 3));
        out.add(tool(Material.DIAMOND_AXE, 5, 3));
        out.add(tool(Material.DIAMOND_SHOVEL, 5, 3));
        out.add(item(Material.WIND_CHARGE, 64, "§bScout Wind Charges"));
        out.add(item(Material.EXPERIENCE_BOTTLE, 64, "§aXP Bottles"));
        out.add(item(Material.COOKED_BEEF, 64, "§6Food"));
        return out;
    }

    private List<ItemStack> netherKit(int prot, String name, boolean bow) {
        List<ItemStack> out = new ArrayList<>();
        for (Material m : List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS)) {
            ItemStack x = armor(m, prot, 5); add(x, Enchantment.RESPIRATION, 3); add(x, Enchantment.AQUA_AFFINITY, 1); if (m == Material.NETHERITE_BOOTS) { add(x, Enchantment.FEATHER_FALLING, 5); add(x, Enchantment.SOUL_SPEED, 3); }
            out.add(x);
        }
        out.add(tool(Material.NETHERITE_SWORD, prot, 5));
        out.add(tool(Material.NETHERITE_PICKAXE, prot, 5));
        out.add(tool(Material.NETHERITE_AXE, prot, 5));
        out.add(tool(Material.NETHERITE_SHOVEL, prot, 5));
        if (bow) out.add(bow(5));
        out.add(item(Material.WIND_CHARGE, 64, "§bWind Charges"));
        out.add(item(Material.EXPERIENCE_BOTTLE, 64, "§aXP Bottles"));
        out.add(item(Material.GOLDEN_APPLE, 16, "§6Golden Apples"));
        return out;
    }

    private List<ItemStack> champion() {
        List<ItemStack> out = netherKit(7, "Champion", true);
        ItemStack b = bow(7); add(b, Enchantment.POWER, 7); add(b, Enchantment.PUNCH, 4); add(b, Enchantment.FLAME, 1); add(b, Enchantment.INFINITY, 1); add(b, Enchantment.UNBREAKING, 7); out.add(b);
        return out;
    }

    private List<ItemStack> master() {
        List<ItemStack> out = netherKit(8, "Master", false);
        ItemStack b = bow(8); add(b, Enchantment.POWER, 8); add(b, Enchantment.PUNCH, 5); add(b, Enchantment.FLAME, 1); add(b, Enchantment.INFINITY, 1); add(b, Enchantment.UNBREAKING, 8); out.add(b);
        return out;
    }

    private List<ItemStack> highMaceKit(int prot, String name, int maceLevel) {
        List<ItemStack> out = netherKit(prot, name, false);
        ItemStack mace = item(Material.MACE, 1, "§d" + name + " Mace");
        add(mace, Enchantment.DENSITY, maceLevel); add(mace, Enchantment.BREACH, maceLevel); add(mace, Enchantment.WIND_BURST, Math.min(3, Math.max(1, maceLevel - 5))); add(mace, Enchantment.UNBREAKING, prot); add(mace, Enchantment.MENDING, 1);
        out.add(mace);
        return out;
    }

    private ItemStack armor(Material m, int prot, int unbreaking) { ItemStack x = new ItemStack(m); add(x, Enchantment.PROTECTION, prot); add(x, Enchantment.UNBREAKING, unbreaking); add(x, Enchantment.MENDING, 1); return x; }
    private ItemStack tool(Material m, int level, int unbreaking) { ItemStack x = new ItemStack(m); if (m == Material.IRON_SWORD || m == Material.DIAMOND_SWORD || m == Material.NETHERITE_SWORD) { add(x, Enchantment.SHARPNESS, level); add(x, Enchantment.LOOTING, Math.min(level, 5)); add(x, Enchantment.SWEEPING_EDGE, Math.min(level, 5)); } else { add(x, Enchantment.EFFICIENCY, level); add(x, Enchantment.FORTUNE, Math.min(level, 5)); } add(x, Enchantment.UNBREAKING, unbreaking); add(x, Enchantment.MENDING, 1); return x; }
    private ItemStack bow(int level) { ItemStack x = new ItemStack(Material.BOW); add(x, Enchantment.POWER, level); add(x, Enchantment.PUNCH, Math.min(5, Math.max(1, level / 2))); add(x, Enchantment.FLAME, 1); add(x, Enchantment.INFINITY, 1); add(x, Enchantment.UNBREAKING, level); add(x, Enchantment.MENDING, 1); return x; }
    private ItemStack item(Material m, int amount, String name) { ItemStack x = new ItemStack(m, amount); ItemMeta meta = x.getItemMeta(); meta.setDisplayName(color(name)); x.setItemMeta(meta); return x; }
    private ItemStack named(Material m, String name, List<String> lore) { ItemStack x = new ItemStack(m); ItemMeta meta = x.getItemMeta(); meta.setDisplayName(color(name)); meta.setLore(lore.stream().map(this::color).toList()); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); x.setItemMeta(meta); return x; }
    private void add(ItemStack x, Enchantment e, int level) { x.addUnsafeEnchantment(e, level); }
    private void setTag(ItemStack x, NamespacedKey key, String value) { ItemMeta m=x.getItemMeta(); m.getPersistentDataContainer().set(key, PersistentDataType.STRING, value); x.setItemMeta(m); }
    private boolean hasClaimed(Player p, String kit) { String v=p.getPersistentDataContainer().get(claimedKey, PersistentDataType.STRING); return v != null && Arrays.asList(v.split(",")).contains(kit); }
    private void setClaimed(Player p, String kit, boolean value) { if (!value) return; Set<String> s=new LinkedHashSet<>(); String v=p.getPersistentDataContainer().get(claimedKey, PersistentDataType.STRING); if(v!=null&&!v.isBlank()) s.addAll(Arrays.asList(v.split(","))); s.add(kit); p.getPersistentDataContainer().set(claimedKey, PersistentDataType.STRING, String.join(",",s)); }
    private boolean isOurMenu(String title) { return ChatColor.stripColor(title).equals(ChatColor.stripColor(color(getConfig().getString("menu-title", "§8Zyxo Kits")))); }
    private boolean isOurPreview(String title) { return ChatColor.stripColor(title).startsWith("Preview: "); }
    private String display(String kit) { return color(getConfig().getString("kits."+kit+".display", kit)); }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    private Material material(String s, Material fallback) { try { return Material.valueOf(s.toUpperCase(Locale.ROOT)); } catch(Exception e) { return fallback; } }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return Collections.emptyList(); }
}
