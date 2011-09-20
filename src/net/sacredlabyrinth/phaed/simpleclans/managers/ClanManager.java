package net.sacredlabyrinth.phaed.simpleclans.managers;

import net.sacredlabyrinth.phaed.simpleclans.*;
import net.sacredlabyrinth.register.payment.Method.MethodAccount;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * @author phaed
 */
public final class ClanManager
{
    private SimpleClans plugin;
    private HashMap<String, Clan> clans = new HashMap<String, Clan>();
    private HashMap<String, ClanPlayer> clanPlayers = new HashMap<String, ClanPlayer>();

    /**
     *
     */
    public ClanManager()
    {
        plugin = SimpleClans.getInstance();
    }

    /**
     * Deletes all clans and clan players in memory
     */
    public void cleanData()
    {
        clans.clear();
        clanPlayers.clear();
    }

    /**
     * Import a clan into the in-memory store
     *
     * @param clan
     */
    public void importClan(Clan clan)
    {
        this.clans.put(clan.getTag(), clan);
    }

    /**
     * Import a clan player into the in-memory store
     *
     * @param cp
     */
    public void importClanPlayer(ClanPlayer cp)
    {
        this.clanPlayers.put(cp.getCleanName(), cp);
    }

    /**
     * Create a new clan
     *
     * @param player
     * @param colorTag
     * @param name
     */
    public void createClan(Player player, String colorTag, String name)
    {
        ClanPlayer cp = getCreateClanPlayer(player.getName());

        boolean verified = !plugin.getSettingsManager().isRequireVerification() || plugin.getPermissionsManager().has(player, "simpleclans.mod.verify");

        Clan clan = new Clan(cp, colorTag, name, verified);
        clan.addPlayerToClan(cp);
        cp.setLeader(true);

        plugin.getStorageManager().insertClan(clan);
        importClan(clan);
        plugin.getStorageManager().updateClanPlayer(cp);

        plugin.getSpoutPluginManager().processPlayer(player.getName());
    }

    /**
     * Delete a players data file
     *
     * @param cp
     */
    public void deleteClanPlayer(ClanPlayer cp)
    {
        clanPlayers.remove(cp.getCleanName());
        plugin.getStorageManager().deleteClanPlayer(cp);
    }

    /**
     * Whether the tag belongs to a clan
     *
     * @param tag
     * @return
     */
    public boolean isClan(String tag)
    {
        return clans.containsKey(Helper.cleanTag(tag));

    }

    /**
     * Returns the clan the tag belongs to
     *
     * @param tag
     * @return
     */
    public Clan getClan(String tag)
    {
        return clans.get(Helper.cleanTag(tag));
    }

    /**
     * Get a player's clan
     *
     * @param playerName
     * @return null if not in a clan
     */
    public Clan getClanByPlayerName(String playerName)
    {
        ClanPlayer cp = getClanPlayer(playerName);

        if (cp != null)
        {
            return cp.getClan();
        }

        return null;
    }

    /**
     * @return the clans
     */
    public List<Clan> getClans()
    {
        return new ArrayList<Clan>(clans.values());
    }

    /**
     * Returns the collection of all clan players, including the disabled ones
     *
     * @return
     */
    public List<ClanPlayer> getAllClanPlayers()
    {
        return new ArrayList<ClanPlayer>(clanPlayers.values());
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null if he's not in a clan
     *
     * @param player
     * @return
     */
    public ClanPlayer getClanPlayer(Player player)
    {
        return getClanPlayer(player.getName());
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null if he's not in a clan
     *
     * @param playerName
     * @return
     */
    public ClanPlayer getClanPlayer(String playerName)
    {
        ClanPlayer cp = clanPlayers.get(playerName.toLowerCase());

        if (cp == null)
        {
            return null;
        }

        if (cp.getClan() == null)
        {
            return null;
        }

        return cp;
    }

    /**
     * Gets the ClanPlayer data object for the player, will retrieve disabled clan players as well, these are players who used to be in a clan but are not currently in one, their data file persists and can be accessed. their clan will be null though.
     *
     * @param playerName
     * @return
     */
    public ClanPlayer getAnyClanPlayer(String playerName)
    {
        return clanPlayers.get(playerName.toLowerCase());
    }

    /**
     * Gets the ClanPlayer object for the player, creates one if not found
     *
     * @param playerName
     * @return
     */
    public ClanPlayer getCreateClanPlayer(String playerName)
    {
        if (clanPlayers.containsKey(playerName.toLowerCase()))
        {
            return clanPlayers.get(playerName.toLowerCase());
        }

        ClanPlayer cp = new ClanPlayer(playerName);

        plugin.getStorageManager().insertClanPlayer(cp);
        importClanPlayer(cp);

        return cp;
    }

    /**
     * Announce message to the server
     *
     * @param msg
     */
    public void serverAnnounce(String msg)
    {
        Player[] players = plugin.getServer().getOnlinePlayers();

        for (Player player : players)
        {
            ChatBlock.sendMessage(player, ChatColor.DARK_GRAY + " * " + ChatColor.AQUA + msg);
        }

        SimpleClans.log(Level.INFO, "[Server Announce] {0}", msg);
    }

    /**
     * Update the players display name with his clan's tag
     *
     * @param player
     */
    public void updateDisplayName(Player player)
    {
        if (plugin.getSettingsManager().isChatTags())
        {
            String prefix = plugin.getPermissionsManager().getPrefix(player);
            String lastColor = Helper.getLastColorCode(prefix);
            String fullname = player.getName();

            ClanPlayer cp = plugin.getClanManager().getAnyClanPlayer(player.getName());

            if (cp == null)
            {
                return;
            }

            Clan clan = cp.getClan();

            if (clan != null)
            {
                String tag = plugin.getSettingsManager().getTagDefaultColor() + clan.getColorTag();
                String tagLabel = plugin.getSettingsManager().getTagBracketColor() + plugin.getSettingsManager().getTagBracketLeft() + tag + plugin.getSettingsManager().getTagBracketColor() + plugin.getSettingsManager().getTagBracketRight() + plugin.getSettingsManager().getTagSeparatorColor() + plugin.getSettingsManager().getTagSeparator();

                fullname = tagLabel + lastColor + fullname;
            }

            player.setDisplayName(fullname);
        }
    }

    /**
     * Process a player and his clan's last seen date
     *
     * @param player
     */
    public void updateLastSeen(Player player)
    {
        ClanPlayer cp = getAnyClanPlayer(player.getName());

        if (cp != null)
        {
            cp.updateLastSeen();
            plugin.getStorageManager().updateClanPlayer(cp);

            Clan clan = cp.getClan();

            if (clan != null)
            {
                clan.updateLastUsed();
                plugin.getStorageManager().updateClan(clan);
            }
        }
    }

    /**
     * @param playerName
     */
    public void ban(String playerName)
    {
        ClanPlayer cp = getClanPlayer(playerName);
        Clan clan = cp.getClan();

        if (clan != null)
        {
            if (clan.getSize() == 1)
            {
                clan.disband();
            }
            else
            {
                cp.setClan(null);
                cp.addPastClan(clan.getColorTag() + (cp.isLeader() ? ChatColor.DARK_RED + "*" : ""));
                cp.setLeader(false);
                cp.setJoinDate(0);
                clan.removeMember(playerName);

                plugin.getStorageManager().updateClanPlayer(cp);
                plugin.getStorageManager().updateClan(clan);
            }
        }

        plugin.getSettingsManager().addBanned(playerName);
    }

    /**
     * Get a count of rivable clans
     *
     * @return
     */
    public int getRivableClanCount()
    {
        int clanCount = 0;

        for (Clan tm : clans.values())
        {
            if (!SimpleClans.getInstance().getSettingsManager().isUnrivable(tm.getTag()))
            {
                clanCount++;
            }
        }

        return clanCount;
    }

    /**
     * Returns a formatted string detailing the players armor
     *
     * @param inv
     * @return
     */
    public String getArmorString(PlayerInventory inv)
    {
        String out = "";

        ItemStack h = inv.getHelmet();

        if (h.getType().equals(Material.CHAINMAIL_HELMET))
        {
            out += ChatColor.WHITE + plugin.getLang().getString("armor.h");
        }
        else if (h.getType().equals(Material.DIAMOND_HELMET))
        {
            out += ChatColor.AQUA + plugin.getLang().getString("armor.h");
        }
        else if (h.getType().equals(Material.GOLD_HELMET))
        {
            out += ChatColor.YELLOW + plugin.getLang().getString("armor.h");
        }
        else if (h.getType().equals(Material.IRON_HELMET))
        {
            out += ChatColor.GRAY + plugin.getLang().getString("armor.h");
        }
        else if (h.getType().equals(Material.LEATHER_HELMET))
        {
            out += ChatColor.GOLD + plugin.getLang().getString("armor.h");
        }
        else if (h.getType().equals(Material.AIR))
        {
            out += ChatColor.BLACK + plugin.getLang().getString("armor.h");
        }
        else
        {
            out += ChatColor.RED + plugin.getLang().getString("armor.h");
        }

        ItemStack c = inv.getChestplate();

        if (c.getType().equals(Material.CHAINMAIL_CHESTPLATE))
        {
            out += ChatColor.WHITE + plugin.getLang().getString("armor.c");
        }
        else if (c.getType().equals(Material.DIAMOND_CHESTPLATE))
        {
            out += ChatColor.AQUA + plugin.getLang().getString("armor.c");
        }
        else if (c.getType().equals(Material.GOLD_CHESTPLATE))
        {
            out += ChatColor.YELLOW + plugin.getLang().getString("armor.c");
        }
        else if (c.getType().equals(Material.IRON_CHESTPLATE))
        {
            out += ChatColor.GRAY + plugin.getLang().getString("armor.c");
        }
        else if (c.getType().equals(Material.LEATHER_CHESTPLATE))
        {
            out += ChatColor.GOLD + plugin.getLang().getString("armor.c");
        }
        else if (c.getType().equals(Material.AIR))
        {
            out += ChatColor.BLACK + plugin.getLang().getString("armor.c");
        }
        else
        {
            out += ChatColor.RED + plugin.getLang().getString("armor.c");
        }

        ItemStack l = inv.getLeggings();

        if (l.getType().equals(Material.CHAINMAIL_LEGGINGS))
        {
            out += ChatColor.WHITE + plugin.getLang().getString("armor.l");
        }
        else if (l.getType().equals(Material.DIAMOND_LEGGINGS))
        {
            out += plugin.getLang().getString("armor.l");
        }
        else if (l.getType().equals(Material.GOLD_LEGGINGS))
        {
            out += plugin.getLang().getString("armor.l");
        }
        else if (l.getType().equals(Material.IRON_LEGGINGS))
        {
            out += plugin.getLang().getString("armor.l");
        }
        else if (l.getType().equals(Material.LEATHER_LEGGINGS))
        {
            out += plugin.getLang().getString("armor.l");
        }
        else if (l.getType().equals(Material.AIR))
        {
            out += plugin.getLang().getString("armor.l");
        }
        else
        {
            out += plugin.getLang().getString("armor.l");
        }

        ItemStack b = inv.getBoots();

        if (b.getType().equals(Material.CHAINMAIL_BOOTS))
        {
            out += ChatColor.WHITE + plugin.getLang().getString("armor.B");
        }
        else if (b.getType().equals(Material.DIAMOND_BOOTS))
        {
            out += ChatColor.AQUA + plugin.getLang().getString("armor.B");
        }
        else if (b.getType().equals(Material.GOLD_BOOTS))
        {
            out += ChatColor.YELLOW + plugin.getLang().getString("armor.B");
        }
        else if (b.getType().equals(Material.IRON_BOOTS))
        {
            out += ChatColor.GRAY + plugin.getLang().getString("armor.B");
        }
        else if (b.getType().equals(Material.LEATHER_BOOTS))
        {
            out += ChatColor.GOLD + plugin.getLang().getString("armor.B");
        }
        else if (b.getType().equals(Material.AIR))
        {
            out += ChatColor.BLACK + plugin.getLang().getString("armor.B");
        }
        else
        {
            out += ChatColor.RED + plugin.getLang().getString("armor.B");
        }

        return out;
    }

    /**
     * Returns a formatted string detailing the players weapons
     *
     * @param inv
     * @return
     */
    public String getWeaponString(PlayerInventory inv)
    {
        String headColor = plugin.getSettingsManager().getPageHeadingsColor();

        String out = "";

        int count = getItemCount(inv.all(Material.DIAMOND_SWORD));

        if (count > 0)
        {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.AQUA + plugin.getLang().getString("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.GOLD_SWORD));

        if (count > 0)
        {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.YELLOW + plugin.getLang().getString("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.IRON_SWORD));

        if (count > 0)
        {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GRAY + plugin.getLang().getString("weapon.S") + countString;
        }

        count = getItemCount(inv.all(Material.WOOD_SWORD));

        if (count > 0)
        {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GOLD + plugin.getLang().getString("weapon.S") + countString;
        }

        count = getItemCount(inv.all(Material.BOW));

        if (count > 0)
        {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GOLD + plugin.getLang().getString("weapon.B") + countString;
        }

        count = getItemCount(inv.all(Material.ARROW));

        if (count > 0)
        {
            out += ChatColor.WHITE + plugin.getLang().getString("weapon.A") + count;
        }

        if (out.length() == 0)
        {
            out = ChatColor.BLACK + "None";
        }

        return out;
    }

    private int getItemCount(HashMap<Integer, ? extends ItemStack> all)
    {
        int count = 0;

        for (ItemStack is : all.values())
        {
            count += is.getAmount();
        }

        return count;
    }

    /**
     * Returns a formatted string detailing the players food
     *
     * @param inv
     * @return
     */
    public String getFoodString(PlayerInventory inv)
    {
        double out = 0;

        int count = getItemCount(inv.all(320)); // cooked porkchop

        if (count > 0)
        {
            out += count * 4;
        }

        count = getItemCount(inv.all(Material.COOKED_FISH));

        if (count > 0)
        {
            out += count * 3;
        }

        count = getItemCount(inv.all(Material.COOKIE));

        if (count > 0)
        {
            out += count * 1;
        }

        count = getItemCount(inv.all(Material.CAKE));

        if (count > 0)
        {
            out += count * 6;
        }

        count = getItemCount(inv.all(Material.CAKE_BLOCK));

        if (count > 0)
        {
            out += count * 9;
        }

        count = getItemCount(inv.all(Material.MUSHROOM_SOUP));

        if (count > 0)
        {
            out += count * 4;
        }

        count = getItemCount(inv.all(Material.BREAD));

        if (count > 0)
        {
            out += count * 3;
        }

        count = getItemCount(inv.all(Material.APPLE));

        if (count > 0)
        {
            out += count * 2;
        }

        count = getItemCount(inv.all(Material.GOLDEN_APPLE));

        if (count > 0)
        {
            out += count * 5;
        }

        count = getItemCount(inv.all(Material.RAW_BEEF));

        if (count > 0)
        {
            out += count * 2;
        }

        count = getItemCount(inv.all(364));  // steak

        if (count > 0)
        {
            out += count * 4;
        }

        count = getItemCount(inv.all(319)); // raw porkchop

        if (count > 0)
        {
            out += count * 2;
        }

        count = getItemCount(inv.all(Material.RAW_CHICKEN));

        if (count > 0)
        {
            out += count * 1;
        }

        count = getItemCount(inv.all(Material.COOKED_CHICKEN));

        if (count > 0)
        {
            out += count * 3;
        }

        count = getItemCount(inv.all(Material.ROTTEN_FLESH));

        if (count > 0)
        {
            out += count * 2;
        }

        count = getItemCount(inv.all(360));  // melon slice

        if (count > 0)
        {
            out += count * 2;
        }

        if (out == 0)
        {
            return ChatColor.BLACK + plugin.getLang().getString("none");
        }
        else
        {
            return new DecimalFormat("#.#").format(out) + "" + ChatColor.GOLD + "h";
        }
    }

    /**
     * Returns a formatted string detailing the players health
     *
     * @param health
     * @return
     */
    public String getHealthString(int health)
    {
        String out = "";

        if (health >= 16)
        {
            out += ChatColor.GREEN;
        }
        else if (health >= 8)
        {
            out += ChatColor.GOLD;
        }
        else
        {
            out += ChatColor.RED;
        }

        for (int i = 0; i < health; i++)
        {
            out += '|';
        }

        return out;
    }

    /**
     * Sort clans by KDR
     *
     * @param clans
     * @return
     */
    public void sortClansByKDR(List<Clan> clans)
    {
        Collections.sort(clans, new Comparator<Clan>()
        {
            public int compare(Clan c1, Clan c2)
            {
                Float o1 = c1.getTotalKDR();
                Float o2 = c2.getTotalKDR();

                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Sort clan players by KDR
     *
     * @param cps
     * @return
     */
    public void sortClanPlayersByKDR(List<ClanPlayer> cps)
    {
        Collections.sort(cps, new Comparator<ClanPlayer>()
        {
            public int compare(ClanPlayer c1, ClanPlayer c2)
            {
                Float o1 = c1.getKDR();
                Float o2 = c2.getKDR();

                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Sort clan players by last seen days
     *
     * @param cps
     * @return
     */
    public void sortClanPlayersByLastSeen(List<ClanPlayer> cps)
    {
        Collections.sort(cps, new Comparator<ClanPlayer>()
        {
            public int compare(ClanPlayer c1, ClanPlayer c2)
            {
                Double o1 = c1.getLastSeenDays();
                Double o2 = c2.getLastSeenDays();

                return o1.compareTo(o2);
            }
        });
    }

    /**
     * Purchase clan creation
     *
     * @param player
     * @return
     */
    public boolean purchaseCreation(Player player)
    {
        if (!plugin.getSettingsManager().isePurchaseCreation())
        {
            return true;
        }

        int price = plugin.getSettingsManager().geteCreationPrice();

        if (plugin.getMethod() != null)
        {
            MethodAccount account = plugin.getMethod().getAccount(player.getName());

            if (account.hasEnough(price))
            {
                account.subtract(price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang().getString("account.has.been.debited"), price));
            }
            else
            {
                player.sendMessage(ChatColor.RED + plugin.getLang().getString("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Purchase clan verification
     *
     * @param player
     * @return
     */
    public boolean purchaseVerification(Player player)
    {
        if (!plugin.getSettingsManager().isePurchaseVerification())
        {
            return true;
        }

        int price = plugin.getSettingsManager().geteVerificationPrice();

        if (plugin.getMethod() != null)
        {
            MethodAccount account = plugin.getMethod().getAccount(player.getName());

            if (account.hasEnough(price))
            {
                account.subtract(price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang().getString("account.has.been.debited"), price));
            }
            else
            {
                player.sendMessage(ChatColor.RED + plugin.getLang().getString("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }
}
