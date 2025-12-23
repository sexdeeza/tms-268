/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  Client.MaplePartTimeJob
 *  Client.inventory.MaplePotionPot
 *  Client.inventory.MapleRing
 *  Net.server.shop.BuyLimitData
 *  Net.server.shop.NpcShopBuyLimit
 *  Net.server.shops.AbstractPlayerStore
 *  Net.server.shops.MapleMiniGame
 */
package Packet;

import Client.MapleCharacter;
import Client.MapleClient;
import Client.MapleCoolDownValueHolder;
import Client.MaplePartTimeJob;
import Client.MapleQuestStatus;
import Client.SecondaryStat;
import Client.SecondaryStatValueHolder;
import Client.hexa.MapleHexaSkill;
import Client.hexa.MapleHexaStat;
import Client.inventory.Equip;
import Client.inventory.Item;
import Client.inventory.MapleAndroid;
import Client.inventory.MapleInventory;
import Client.inventory.MapleInventoryType;
import Client.inventory.MaplePet;
import Client.inventory.MaplePotionPot;
import Client.inventory.MapleRing;
import Client.skills.InnerSkillEntry;
import Client.skills.Skill;
import Client.skills.SkillEntry;
import Client.skills.SkillFactory;
import Client.stat.MapleHyperStats;
import Config.configs.ServerConfig;
import Config.constants.GameConstants;
import Config.constants.ItemConstants;
import Config.constants.JobConstants;
import Net.server.MapleItemInformationProvider;
import Net.server.movement.LifeMovementFragment;
import Net.server.quest.MapleQuest;
import Net.server.shop.BuyLimitData;
import Net.server.shop.MapleShop;
import Net.server.shop.MapleShopFactory;
import Net.server.shop.MapleShopItem;
import Net.server.shop.NpcShopBuyLimit;
import Net.server.shops.AbstractPlayerStore;
import Net.server.shops.IMaplePlayerShop;
import Net.server.shops.MapleMiniGame;
import Packet.VCorePacket;
import connection.OutPacket;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import tools.DateUtil;
import tools.Pair;
import tools.Randomizer;
import tools.StringUtil;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

public class PacketHelper {
    public static final long MAX_TIME = 150842304000000000L;
    public static final long ZERO_TIME = 94354848000000000L;
    public static final long PERMANENT = 150841440000000000L;

    public static long getKoreanTimestamp(long realTimestamp) {
        return realTimestamp * 10000L + 116445060000000000L;
    }

    public static final long getTime(long realTimestamp) {
        if (realTimestamp == -1L) {
            return 150842304000000000L;
        }
        if (realTimestamp == -2L) {
            return 94354848000000000L;
        }
        if (realTimestamp == -3L) {
            return 94354848000000000L;
        }
        if (realTimestamp == -4L) {
            return 94354848000000000L;
        }
        return realTimestamp * 10000L + 116445060000000000L;
    }

    public static void addPotionPotInfo(MaplePacketLittleEndianWriter mplew, MaplePotionPot potionPot) {
        mplew.writeInt(potionPot.getItmeId());
        mplew.writeInt(potionPot.getChrId());
        mplew.writeInt(potionPot.getMaxValue());
        mplew.writeInt(potionPot.getHp());
        mplew.writeInt(potionPot.getMp());
        mplew.writeLong(PacketHelper.getTime(potionPot.getStartDate()));
        mplew.writeLong(PacketHelper.getTime(potionPot.getEndDate()));
    }

    public static void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<SecondaryStat, Pair<Integer, Integer>>> statups) {
        int[] mask = new int[31];
        for (Pair<SecondaryStat, Pair<Integer, Integer>> statup : statups) {
            mask[((SecondaryStat)statup.left).getPosition() - 1] = mask[((SecondaryStat)statup.left).getPosition() - 1] | ((SecondaryStat)statup.left).getValue();
        }
        for (int i = mask.length; i >= 1; --i) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static List<Pair<SecondaryStat, List<SecondaryStatValueHolder>>> sortIndieBuffStats(Map<SecondaryStat, List<SecondaryStatValueHolder>> statups) {
        boolean changed;
        List<Pair<SecondaryStat, List<SecondaryStatValueHolder>>> statvals = new ArrayList<>();
        for (Map.Entry<SecondaryStat, List<SecondaryStatValueHolder>> stat : statups.entrySet()) {
            statvals.add(new Pair<SecondaryStat, List<SecondaryStatValueHolder>>(stat.getKey(), stat.getValue()));
        }
        do {
            changed = false;
            int i = 0;
            int k = 1;
            for (int iter = 0; iter < statvals.size() - 1; ++iter) {
                Pair<SecondaryStat, List<SecondaryStatValueHolder>> a = statvals.get(i);
                Pair<SecondaryStat, List<SecondaryStatValueHolder>> b = statvals.get(k);
                if (a != null && b != null && ((SecondaryStat)a.left).getFlag() > ((SecondaryStat)b.left).getFlag()) {
                    Pair<SecondaryStat, List<SecondaryStatValueHolder>> swap = new Pair<>(a.left, a.right);
                    statvals.remove(i);
                    statvals.add(i, b);
                    statvals.remove(k);
                    statvals.add(k, swap);
                    changed = true;
                }
                ++i;
                ++k;
            }
        } while (changed);
        return statvals;
    }

    public static void addPartTimeJob(MaplePacketLittleEndianWriter mplew, MaplePartTimeJob parttime) {
        mplew.write(parttime.getJob());
        if (parttime.getJob() > 0 && parttime.getJob() <= 5) {
            mplew.writeReversedLong(parttime.getTime());
        } else {
            mplew.writeReversedLong(PacketHelper.getTime(-2L));
        }
        mplew.writeInt(parttime.getReward());
        mplew.writeBool(parttime.getReward() > 0);
    }

    public static void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time) {
        mplew.writeLong(PacketHelper.getTime(time));
    }

    public static void addItemPosition(MaplePacketLittleEndianWriter mplew, Item item, boolean trade, boolean bagSlot) {
        short pos;
        if (item == null) {
            pos = 0;
        } else {
            pos = item.getPosition();
            if (pos < 0 && (pos = (short)Math.abs(pos)) > 100 && pos < 1000) {
                pos = (short)(pos - 100);
            }
            if (bagSlot) {
                pos = (short)(pos % 100 - 1);
            }
        }
        if (bagSlot) {
            mplew.writeInt(pos);
        } else if (!trade) {
            mplew.writeShort(pos);
        } else {
            mplew.write(pos);
        }
    }

    public static void GW_ItemSlotBase_Encode(MaplePacketLittleEndianWriter mplew, Item item) {
        if (item == null) {
            throw new NullPointerException("addItemInfo item is null.");
        }
        OutPacket outPacket = new OutPacket();
        item.encode(outPacket);
        mplew.write(outPacket.getData());
    }

    public static void serializeMovementList(MaplePacketLittleEndianWriter mplew, int gatherDuration, int nVal1, Point mPos, Point oPos, List<LifeMovementFragment> moves, int[] arrays) {
        mplew.writeInt(gatherDuration);
        mplew.writeInt(nVal1);
        mplew.writePos(mPos);
        mplew.writePos(oPos);
        if (moves == null) {
            mplew.writeShort(0);
        } else {
            mplew.writeShort(moves.size());
            for (LifeMovementFragment move : moves) {
                move.serialize(mplew);
            }
        }
        if (arrays != null) {
            mplew.writeShort(arrays.length);
            for (int nVal : arrays) {
                mplew.writeInt(nVal); // Assuming nVal should be written as an integer
            }
        }
    }

    public static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            PacketHelper.addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static void addInteraction(MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType());
        mplew.writeInt(((AbstractPlayerStore)shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != 1) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0);
        }
        int id = shop.getItemId() % 100;
        if (shop instanceof MapleMiniGame) {
            MapleMiniGame mini = (MapleMiniGame)shop;
            id = mini.getPieceType();
        } else if (shop.getShopType() == 75) {
            id = 0;
        }
        mplew.write(id);
        mplew.write(shop.getShopType() != 75 ? shop.getSize() : 0);
        mplew.write(shop.getMaxSize());
        if (shop.getShopType() != 1) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }
        PacketHelper.addChaterName(mplew, "", "");
    }

    /*
     * WARNING - void declaration
     */
    public static void addCharacterInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, long flag) {
        int var25_116 = 0;
        long date;
        int itemId;
        MapleInventoryType eiv;
        ArrayList<Item> items;
        int i;
        for (i = 0; i < 100; ++i) {
            mplew.write(1);
        }
        mplew.write(0);
        for (i = 0; i < 3; ++i) {
            mplew.writeInt(JobConstants.is皇家騎士團(chr.getJob()) || JobConstants.is米哈逸(chr.getJob()) ? -6 : -1);
        }
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        if ((flag & 1L) != 0L) {
            MapleQuestStatus ultExplorer;
            chr.getCharacterStat().encode(mplew);
            mplew.write(chr.getBuddylist().getCapacity());
            mplew.write(chr.getBlessOfFairyOrigin() != null);
            if (chr.getBlessOfFairyOrigin() != null) {
                mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
            }
            mplew.write(chr.getBlessOfEmpressOrigin() != null);
            if (chr.getBlessOfEmpressOrigin() != null) {
                mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
            }
            mplew.write((ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(111111))) != null && ultExplorer.getCustomData() != null);
            if (ultExplorer != null && ultExplorer.getCustomData() != null) {
                mplew.writeMapleAsciiString(ultExplorer.getCustomData());
            }
            mplew.writeLong(133709900400000000L);
            mplew.writeHexString("00 40 E0 FD 3B 37 4F 01");
            int v7 = 2;
            block2: do {
                mplew.writeInt(0);
                while (true) {
                    int res = -1;
                    mplew.write(res);
                    if (res == -1) continue block2;
                    mplew.writeInt(0);
                }
            } while ((v7 += 36) < 74);
        }
        mplew.writeShort(0);
        mplew.writeLong(PacketHelper.getTime(-2L));
        String questInfo = chr.getOneInfo(56829, "count");
        mplew.writeInt(questInfo == null ? ServerConfig.defaultDamageSkinSlot : Integer.valueOf(questInfo));
        PacketHelper.addDamageSkinInfo(mplew, chr);
        if ((flag & 2L) != 0L) {
            mplew.writeLong(chr.getMeso());
            mplew.writeInt(chr.getId());
            mplew.writeInt(chr.getBeans());
            mplew.writeInt(chr.getCSPoints(2));
        }
        if ((flag & 0x40L) != 0L) {
            mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIPPED).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.USE).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.ETC).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.CASH).getSlotLimit());
            mplew.writeInt(chr.getInventory(MapleInventoryType.DECORATION).getSlotLimit());
        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equippedList = iv.newList();
        Collections.sort(equippedList);
        ArrayList<Item> equipped = new ArrayList<Item>();
        ArrayList<Item> equippedCash = new ArrayList<Item>();
        ArrayList<Item> equippedDragon = new ArrayList<Item>();
        ArrayList<Item> equippedMechanic = new ArrayList<Item>();
        ArrayList<Item> equippedAndroid = new ArrayList<Item>();
        ArrayList<Item> equippedLolitaCash = new ArrayList<Item>();
        ArrayList<Item> equippedBit = new ArrayList<Item>();
        ArrayList<Item> equippedZeroBetaCash = new ArrayList<Item>();
        ArrayList<Item> equippedArcane = new ArrayList<Item>();
        ArrayList<Item> equippedAuthenticSymbol = new ArrayList<Item>();
        ArrayList<Item> equippedTotem = new ArrayList<Item>();
        ArrayList equippedMonsterEqp = new ArrayList();
        ArrayList<Item> equippedHakuFan = new ArrayList<Item>();
        ArrayList<Item> equippedUnknown = new ArrayList<Item>();
        ArrayList<Item> equippedCashPreset = new ArrayList<Item>();
        for (Item item2 : equippedList) {
            if (item2.getPosition() < 0 && item2.getPosition() > -100) {
                equipped.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1000 && item2.getPosition() > -1100) {
                equippedDragon.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1100 && item2.getPosition() > -1200) {
                equippedMechanic.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1400 && item2.getPosition() > -1500) {
                equippedBit.add(item2);
                continue;
            }
            if (item2.getPosition() <= -5000 && item2.getPosition() >= -5002) {
                equippedTotem.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1600 && item2.getPosition() > -1700) {
                equippedArcane.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1700 && item2.getPosition() > -1800) {
                equippedAuthenticSymbol.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1200 && item2.getPosition() > -1300) {
                equippedAndroid.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1300 && item2.getPosition() > -1400) {
                equippedLolitaCash.add(item2);
                continue;
            }
            if (item2.getPosition() <= -1500 && item2.getPosition() > -1600) {
                equippedZeroBetaCash.add(item2);
                continue;
            }
            if (item2.getPosition() <= -100 && item2.getPosition() > -1000) {
                equippedCash.add(item2);
                continue;
            }
            if (item2.getPosition() <= 10000 || item2.getPosition() >= 10200) continue;
            chr.getSkillSkin().put(MapleItemInformationProvider.getInstance().getSkillSkinFormSkillId(item2.getItemId()), item2.getItemId());
            equippedUnknown.add(item2);
        }
        if ((flag & 0x80L) != 0L) {
            mplew.writeBool(false);
            iv = chr.getInventory(MapleInventoryType.EQUIP);
            LinkedList<MapleAndroid> androids = new LinkedList<MapleAndroid>();
            ArrayList<Item> items20000 = new ArrayList();
            ArrayList<Item> arrayList = new ArrayList<Item>();
            ArrayList<Item> arrayList2 = new ArrayList<Item>();
            for (Item item2 : iv.list()) {
                if (((Equip)item2).getAndroid() != null) {
                    androids.add(((Equip)item2).getAndroid());
                }
                if (item2.getPosition() >= 21000) {
                    arrayList.add(item2);
                    continue;
                }
                if (item2.getPosition() >= 20000) {
                    items20000.add(item2);
                    continue;
                }
                arrayList2.add(item2);
            }
            mplew.writeShort(0);
            PacketHelper.encodeInventory(mplew, equipped, chr);
            PacketHelper.encodeInventory(mplew, arrayList2, chr);
            PacketHelper.encodeInventory(mplew, equippedDragon, chr);
            PacketHelper.encodeInventory(mplew, equippedMechanic, chr);
            PacketHelper.encodeInventory(mplew, equippedBit, chr);
            PacketHelper.encodeInventory(mplew, equippedTotem, chr);
            PacketHelper.encodeInventory(mplew, equippedArcane, chr);
            PacketHelper.encodeInventory(mplew, equippedAuthenticSymbol, chr);
            PacketHelper.encodeInventory(mplew, equippedHakuFan, chr);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
            PacketHelper.encodeInventory(mplew, equippedTotem, chr);
            PacketHelper.encodeInventory(mplew, equippedUnknown, chr);
            PacketHelper.encodeInventory(mplew, items20000, chr);
            PacketHelper.encodeInventory(mplew, arrayList, chr);
        }
        if ((flag & 0x10L) != 0L) {
            PacketHelper.encodeInventory(mplew, Collections.emptyList(), chr);
            PacketHelper.encodeInventory(mplew, Collections.emptyList(), chr);
        }
        if ((flag & 0x2000L) != 0L) {
            mplew.writeBool(false);
            PacketHelper.encodeInventory(mplew, equippedCash, chr);
            iv = chr.getInventory(MapleInventoryType.DECORATION);
            ArrayList<Item> decoration = new ArrayList<Item>();
            for (Item item3 : iv.list()) {
                if (item3.getPosition() >= 129) continue;
                decoration.add(item3);
            }
            PacketHelper.encodeInventory(mplew, decoration, chr);
            PacketHelper.encodeInventory(mplew, equippedAndroid, chr);
            PacketHelper.encodeInventory(mplew, equippedLolitaCash, chr);
            PacketHelper.encodeInventory(mplew, equippedZeroBetaCash, chr);
            PacketHelper.encodeInventory(mplew, equippedCashPreset, chr);
            mplew.writeShort(0);
            mplew.writeShort(0);
        }
        if ((flag & 8L) != 0L) {
            iv = chr.getInventory(MapleInventoryType.USE);
            items = new ArrayList<Item>();
            for (Item item4 : iv.list()) {
                if (item4.getPosition() >= 129) continue;
                items.add(item4);
            }
            PacketHelper.encodeInventory(mplew, items, chr);
        }
        if ((flag & 0x10L) != 0L) {
            iv = chr.getInventory(MapleInventoryType.SETUP);
            items = new ArrayList();
            for (Item item5 : iv.list()) {
                if (item5.getPosition() >= 129) continue;
                items.add(item5);
            }
            PacketHelper.encodeInventory(mplew, items, chr);
        }
        if ((flag & 0x80L) != 0L) {
            iv = chr.getInventory(MapleInventoryType.ETC);
            items = new ArrayList();
            for (Item item6 : iv.list()) {
                if (item6.getPosition() >= 129) continue;
                items.add(item6);
            }
            PacketHelper.encodeInventory(mplew, items, chr);
        }
        ArrayList<MaplePet> pets = new ArrayList<MaplePet>();
        if ((flag & 0x40L) != 0L) {
            iv = chr.getInventory(MapleInventoryType.CASH);
            ArrayList<Item> items2 = new ArrayList<Item>();
            for (Item item7 : iv.list()) {
                items2.add(item7);
                if (item7.getPet() == null) continue;
                pets.add(item7.getPet());
            }
            PacketHelper.encodeInventory(mplew, items2, chr);
        }
        if ((flag & 0x20L) != 0L) {
            eiv = MapleInventoryType.USE;
            List<Item> list = chr.getExtendedSlots(eiv.getType());
            mplew.writeInt(list.size());
            for (Item item8 : list) {
                mplew.writeInt(item8.getExtendSlot());
                mplew.writeInt(item8.getItemId());
                chr.getInventory(eiv).list().stream().filter(item -> item.getPosition() > item8.getExtendSlot() * 100 + 10100 && item.getPosition() < item8.getExtendSlot() * 100 + 10200).forEach(item -> {
                    PacketHelper.addItemPosition(mplew, item, false, true);
                    PacketHelper.GW_ItemSlotBase_Encode(mplew, item);
                });
                mplew.writeInt(-1);
            }
        }
        if ((flag & 0x30L) != 0L) {
            eiv = MapleInventoryType.SETUP;
            List<Item> list = chr.getExtendedSlots(eiv.getType());
            mplew.writeInt(list.size());
            for (Item item9 : list) {
                mplew.writeInt(item9.getExtendSlot());
                mplew.writeInt(item9.getItemId());
                chr.getInventory(eiv).list().stream().filter(item -> item.getPosition() > item9.getExtendSlot() * 100 + 10100 && item.getPosition() < item9.getExtendSlot() * 100 + 10200).forEach(item -> {
                    PacketHelper.addItemPosition(mplew, item, false, true);
                    PacketHelper.GW_ItemSlotBase_Encode(mplew, item);
                });
                mplew.writeInt(-1);
            }
        }
        if ((flag & 0x40L) != 0L) {
            eiv = MapleInventoryType.ETC;
            List<Item> list = chr.getExtendedSlots(eiv.getType());
            mplew.writeInt(list.size());
            for (Item item10 : list) {
                mplew.writeInt(item10.getExtendSlot());
                mplew.writeInt(item10.getItemId());
                chr.getInventory(eiv).list().stream().filter(item -> item.getPosition() > item10.getExtendSlot() * 100 + 10100 && item.getPosition() < item10.getExtendSlot() * 100 + 10200).forEach(item -> {
                    PacketHelper.addItemPosition(mplew, item, false, true);
                    PacketHelper.GW_ItemSlotBase_Encode(mplew, item);
                });
                mplew.writeInt(-1);
            }
        }
        if ((flag & 0x1000000L) != 0L) {
            mplew.writeInt(0);
        }
        if ((flag & 0x40000000L) != 0L) {
            mplew.writeInt(0);
        }
        if ((flag & 0x800000L) != 0L) {
            mplew.write(0);
        }
        if ((flag & 0x100L) != 0L) {
            int var27_192 = 0;
            Map<Integer, SkillEntry> map = chr.getSkills(true);
            mplew.write(1);
            mplew.writeShort(map.size());
            for (Map.Entry<Integer, SkillEntry> entry : map.entrySet()) {
                Skill skill = SkillFactory.getSkill(entry.getKey());
                mplew.writeInt(skill.getId());
                if (skill.isLinkSkills()) {
                    mplew.writeInt(entry.getValue().teachId);
                } else if (skill.isTeachSkills()) {
                    mplew.writeInt(entry.getValue().teachId > 0 ? entry.getValue().teachId : chr.getId());
                } else {
                    mplew.writeInt(entry.getValue().skillevel);
                }
                PacketHelper.addExpirationTime(mplew, entry.getValue().expiration);
                if (skill.isFourthJob()) {
                    mplew.writeInt(entry.getValue().masterlevel);
                }
                if (skill.getId() != 40020002 && skill.getId() != 80000004) continue;
                mplew.writeInt(entry.getValue().masterlevel);
            }
            Map<Integer, SkillEntry> map2 = chr.getLinkSkills();
            mplew.writeShort(map2.size());
            for (Map.Entry<Integer, SkillEntry> entry : map2.entrySet()) {
                mplew.writeInt(entry.getKey());
                mplew.writeShort(entry.getValue().skillevel - 1);
            }
            Map<Integer, Pair<Integer, SkillEntry>> map3 = chr.getSonOfLinkedSkills();
            mplew.writeInt(map3.size());
            for (Map.Entry<Integer, Pair<Integer, SkillEntry>> entry : map3.entrySet()) {
                PacketHelper.writeSonOfLinkedSkill(mplew, entry.getKey(), entry.getValue());
            }
            mplew.write((int)chr.getInfoQuestValueWithKey(2498, "hyperstats"));
            boolean bl = false;
            while (var27_192 <= 2) {
                mplew.writeInt(chr.loadHyperStats((int)var27_192).size());
                for (MapleHyperStats mapleHyperStats : chr.loadHyperStats((int)var27_192)) {
                    mplew.writeInt(mapleHyperStats.getPosition());
                    mplew.writeInt(mapleHyperStats.getSkillid());
                    mplew.writeInt(mapleHyperStats.getSkillLevel());
                }
                ++var27_192;
            }
        }
        if ((flag & 0x8000L) != 0L) {
            List<MapleCoolDownValueHolder> list = chr.getCooldowns();
            mplew.writeShort(list.size());
            for (MapleCoolDownValueHolder mapleCoolDownValueHolder : list) {
                mplew.writeInt(mapleCoolDownValueHolder.skillId);
                int n = (int)((long)mapleCoolDownValueHolder.length + mapleCoolDownValueHolder.startTime - System.currentTimeMillis());
                mplew.writeInt(n / 1000);
            }
        }
        if ((flag & 1L) != 0L) {
            int var24_51 = 0;
            int var24_49 = 0;
            boolean bl = false;
            while (var24_49 < 6) {
                mplew.writeInt(0);
                ++var24_49;
            }
            boolean bl2 = false;
            while (var24_51 < 6) {
                mplew.write(0);
                ++var24_51;
            }
        }
        if ((flag & 0x200L) != 0L) {
            List<MapleQuestStatus> list = chr.getStartedQuests();
            boolean bl = true;
            mplew.write(bl);
            mplew.writeShort(list.size());
            for (MapleQuestStatus mapleQuestStatus : list) {
                mplew.writeInt(mapleQuestStatus.getQuest().getId());
                if (mapleQuestStatus.hasMobKills()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int n : mapleQuestStatus.getMobKills().values()) {
                        stringBuilder.append(StringUtil.getLeftPaddedStr(String.valueOf(n), '0', 3));
                    }
                    mplew.writeMapleAsciiString(stringBuilder.toString());
                    continue;
                }
                mplew.writeMapleAsciiString(mapleQuestStatus.getCustomData() == null ? "" : mapleQuestStatus.getCustomData());
            }
            if (!bl) {
                mplew.writeShort(0);
            }
            mplew.writeShort(0);
        }
        if ((flag & 0x4000L) != 0L) {
            boolean bl = true;
            mplew.write(bl);
            List<MapleQuestStatus> list = chr.getCompletedQuests();
            if (ServerConfig.HideBulbQuest) {
                int[] nArray;
                LinkedList<MapleQuest> linkedList = new LinkedList<MapleQuest>();
                for (MapleQuestStatus mapleQuestStatus : list) {
                    linkedList.add(mapleQuestStatus.getQuest());
                }
                List<Integer> list2 = Arrays.asList(5741, 5742, 5743, 5744, 5745);
                for (MapleQuest mapleQuest : MapleQuest.GetBulbQuest()) {
                    if (linkedList.contains(mapleQuest) || list2.contains(mapleQuest.getId())) continue;
                    list.add(new MapleQuestStatus(mapleQuest, 2));
                    linkedList.add(mapleQuest);
                }
                for (int nQ : nArray = new int[]{32510}) {
                    MapleQuest q = MapleQuest.getInstance(nQ);
                    if (linkedList.contains(q)) continue;
                    list.add(new MapleQuestStatus(q, 2));
                    linkedList.add(q);
                }
            }
            mplew.writeShort(list.size());
            for (MapleQuestStatus mapleQuestStatus : list) {
                mplew.writeInt(mapleQuestStatus.getQuest().getId());
                mplew.writeLong(PacketHelper.getTime(mapleQuestStatus.getCompletionTime()));
            }
            if (!bl) {
                mplew.writeShort(0);
            }
        }
        if ((flag & 0x400L) != 0L) {
            mplew.writeShort(0);
        }
        if ((flag & 0x800L) != 0L) {
            Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> triple = chr.getRings(true);
            List<MapleRing> list = triple.getLeft();
            mplew.writeShort(list.size());
            for (MapleRing mapleRing : list) {
                mplew.writeInt(mapleRing.getPartnerChrId());
                mplew.writeAsciiString(mapleRing.getPartnerName(), 15);
                mplew.writeLong(mapleRing.getRingId());
                mplew.writeLong(mapleRing.getPartnerRingId());
            }
            List<MapleRing> list3 = triple.getMid();
            mplew.writeShort(list3.size());
            for (MapleRing mapleRing : list3) {
                mplew.writeInt(mapleRing.getPartnerChrId());
                mplew.writeAsciiString(mapleRing.getPartnerName(), 15);
                mplew.writeLong(mapleRing.getRingId());
                mplew.writeLong(mapleRing.getPartnerRingId());
                mplew.writeInt(mapleRing.getItemId());
            }
            List<MapleRing> list4 = triple.getRight();
            mplew.writeShort(list4.size());
            for (MapleRing mapleRing : list4) {
                mplew.writeInt(chr.getMarriageId());
                mplew.writeInt(chr.getId());
                mplew.writeInt(mapleRing.getPartnerChrId());
                mplew.writeShort(3);
                mplew.writeInt(mapleRing.getItemId());
                mplew.writeInt(mapleRing.getItemId());
                mplew.writeAsciiString(chr.getName(), 15);
                mplew.writeAsciiString(mapleRing.getPartnerName(), 15);
            }
        }
        if ((flag & 0x1000L) != 0L) {
            int var27_202 = 0;
            int var26_148 = 0;
            int var25_88 = 0;
            int[] nArray = chr.getRegRocks();
            boolean bl = false;
            while (var25_88 < 5) {
                mplew.writeInt(nArray[var25_88]);
                ++var25_88;
            }
            int[] nArray2 = chr.getRocks();
            boolean bl3 = false;
            while (var26_148 < 10) {
                mplew.writeInt(nArray2[var26_148]);
                ++var26_148;
            }
            int[] nArray3 = chr.getHyperRocks();
            boolean bl4 = false;
            while (var27_202 < 13) {
                mplew.writeInt(nArray3[var27_202]);
                ++var27_202;
            }
        }
        if ((flag & 0x40000L) != 0L) {
            LinkedHashMap<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();
            for (Map.Entry<Integer, String> entry : chr.getInfoQuest_Map().entrySet()) {
                linkedHashMap.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<Integer, String> entry : chr.getWorldShareInfo().entrySet()) {
                if (GameConstants.isWorldShareQuest(entry.getKey())) continue;
                linkedHashMap.put(entry.getKey(), entry.getValue());
            }
            mplew.writeShort(linkedHashMap.size());
            for (Map.Entry entry : linkedHashMap.entrySet()) {
                mplew.writeInt((Integer)entry.getKey());
                mplew.writeMapleAsciiString(entry.getValue() == null ? "" : (String)entry.getValue());
            }
        }
        if ((flag & 0x80000L) != 0L) {
            int var25_94 = 0;
            int n = 0;
            mplew.writeShort(n);
            boolean bl = false;
            while (var25_94 < n) {
                mplew.writeInt(0);
                mplew.writeShort(0);
                ++var25_94;
            }
        }
        mplew.writeBool(true);
        if ((flag & 0x8000000000L) != 0L) {
            int var25_96 = 0;
            int n = 0;
            mplew.writeInt(n);
            boolean bl = false;
            while (var25_96 < n) {
                mplew.writeInt(26);
                mplew.writeMapleAsciiString("Present=7");
                ++var25_96;
            }
        }
        if ((flag & 0x100000000000L) != 0L) {
            int var25_98 = 0;
            int n = 1;
            mplew.writeInt(n);
            boolean bl = false;
            while (var25_98 < n) {
                mplew.writeInt(4475);
                mplew.writeInt(-1);
                ++var25_98;
            }
        }
        if ((flag & 0x200000L) != 0L) {
            PacketHelper.addJaguarInfo(mplew, chr);
        }
        if ((flag & 0x800L) != 0L && JobConstants.is神之子(chr.getJob())) {
            chr.getStat().zeroData(mplew, chr, 65535, chr.isBeta());
        }
        if ((flag & 0x4000000L) != 0L) {
            mplew.writeShort(chr.getBuyLimit().size() + chr.getAccountBuyLimit().size());
            for (Map.Entry<Integer, NpcShopBuyLimit> entry : chr.getBuyLimit().entrySet()) {
                int n = entry.getKey();
                NpcShopBuyLimit npcShopBuyLimit = entry.getValue();
                MapleShop mapleShop = MapleShopFactory.getInstance().getShop(n);
                mplew.writeInt(n);
                mplew.writeShort(mapleShop != null ? npcShopBuyLimit.getData().size() : 0);
                if (mapleShop == null) continue;
                for (Map.Entry entry2 : npcShopBuyLimit.getData().entrySet()) {
                    itemId = (Integer)entry2.getKey();
                    BuyLimitData data = (BuyLimitData)entry2.getValue();
                    int count = data.getCount();
                    date = data.getDate();
                    mplew.writeInt(n);
                    mplew.writeShort(mapleShop.getBuyLimitItemIndex((Integer)entry2.getKey()));
                    mplew.writeInt(itemId);
                    mplew.writeShort(count);
                    PacketHelper.addExpirationTime(mplew, date);
                    mplew.writeMapleAsciiString("");
                    mplew.writeInt(0);
                }
            }
        }
        if ((flag & 0x4000000L) != 0L) {
            mplew.writeShort(chr.getBuyLimit().size() + chr.getAccountBuyLimit().size());
            for (Map.Entry<Integer, NpcShopBuyLimit> entry : chr.getBuyLimit().entrySet()) {
                int n = entry.getKey();
                NpcShopBuyLimit npcShopBuyLimit = entry.getValue();
                MapleShop mapleShop = MapleShopFactory.getInstance().getShop(n);
                mplew.writeInt(n);
                mplew.writeShort(mapleShop != null ? npcShopBuyLimit.getData().size() : 0);
                if (mapleShop == null) continue;
                for (Map.Entry entry3 : npcShopBuyLimit.getData().entrySet()) {
                    itemId = (Integer)entry3.getKey();
                    BuyLimitData data = (BuyLimitData)entry3.getValue();
                    int count = data.getCount();
                    date = data.getDate();
                    mplew.writeInt(n);
                    mplew.writeShort(mapleShop.getBuyLimitItemIndex((Integer)entry3.getKey()));
                    mplew.writeInt(itemId);
                    mplew.writeShort(count);
                    PacketHelper.addExpirationTime(mplew, date);
                    mplew.writeMapleAsciiString("");
                    mplew.writeInt(0);
                }
            }
        }
        if ((flag & 0x4000000L) != 0L) {
            mplew.writeShort(0);
        }
        mplew.writeInt(0);
        if ((flag & 0x20000000L) != 0L) {
            int var24_63 = 0;
            boolean bl = false;
            while (var24_63 < 16) {
                mplew.writeInt(chr.getStealMemorySkill((int)var24_63));
                ++var24_63;
            }
        }
        if ((flag & 0x10000000L) != 0L) {
            int var27_206 = 0;
            int[] nArray;
            int[] nArray4 = nArray = new int[]{24001001, 24101001, 24111001, 24121001, 24121054};
            int n = nArray4.length;
            boolean bl = false;
            while (var27_206 < n) {
                int n2 = nArray4[var27_206];
                mplew.writeInt(chr.getEquippedStealSkill(n2));
                ++var27_206;
            }
        }
        if ((flag & 0x80000000L) != 0L) {
            int var24_66 = 0;
            boolean bl = false;
            while (var24_66 < 3) {
                int var25_103 = 0;
                mplew.writeShort(chr.getInnerSkillSize());
                boolean bl5 = false;
                while (var25_103 < chr.getInnerSkillSize()) {
                    InnerSkillEntry innerSkillEntry = chr.getInnerSkills()[var25_103];
                    if (innerSkillEntry != null) {
                        mplew.write(innerSkillEntry.getPosition());
                        mplew.writeInt(innerSkillEntry.getSkillId());
                        mplew.write(innerSkillEntry.getSkillLevel());
                        mplew.write(innerSkillEntry.getRank());
                    } else {
                        mplew.writeZeroBytes(7);
                    }
                    ++var25_103;
                }
                ++var24_66;
            }
        }
        if ((flag & 0x40000000000000L) != 0L) {
            mplew.writeShort(chr.getSoulCollection().size());
            for (Map.Entry<Integer, Integer> entry : chr.getSoulCollection().entrySet()) {
                mplew.writeInt(entry.getKey());
                mplew.writeInt(entry.getValue());
            }
        }
        if ((flag & 0x100000000L) != 0L) {
            mplew.writeInt(1);
            mplew.writeInt(chr.getHonor());
        }
        if ((flag & 0x800000000000L) != 0L) {
            int var25_106 = 0;
            int n = 0;
            mplew.writeShort(n);
            boolean bl = false;
            while (var25_106 < n) {
                mplew.writeZeroBytes(20);
                ++var25_106;
            }
        }
        if ((flag & 0x1000000000000L) != 0L) {
            PacketHelper.addRedLeafInfo(mplew, chr);
        }
        if ((flag & 0x2000000000000L) != 0L) {
            mplew.writeShort(0);
        }
        if ((flag & 0x200000000L) != 0L) {
            mplew.write(1);
            mplew.writeShort(0);
        }
        if ((flag & 0x400000000L) != 0L) {
            mplew.write(0);
        }
        if ((flag & 0x800000000L) != 0L) {
            PacketHelper.writeDressUpInfo(mplew, chr);
        }
        if ((flag & 0x20000000000000L) != 0L) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2L));
        }
        if ((flag & 0x20000000000L) != 0L) {
            mplew.write(0);
        }
        if ((flag & 0x4000000000000000L) != 0L) {
            mplew.writeInt(-1);
            mplew.writeInt(-1157267456);
            mplew.writeLong(0L);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeShort(0);
        }
        if ((flag & 0x40000000000L) != 0L) {
            mplew.writeInt(chr.getLove());
            mplew.writeLong(PacketHelper.getTime(-2L));
            mplew.writeInt(0);
        }
        if ((flag & 0x80000000000000L) != 0L) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2L));
            mplew.writeInt(10);
        }
        if ((flag & 0x200000000000000L) != 0L) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(0L);
            mplew.write(0);
            mplew.write(0);
        }
        LinkedHashMap<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, String> entry : chr.getWorldShareInfo().entrySet()) {
            if (!GameConstants.isWorldShareQuest(entry.getKey())) continue;
            linkedHashMap.put(entry.getKey(), entry.getValue());
        }
        mplew.writeShort(linkedHashMap.size());
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            mplew.writeInt((Integer)entry.getKey());
            mplew.writeMapleAsciiString(entry.getValue() == null ? "" : (String)entry.getValue());
        }
        if ((flag & 0x100000000000000L) != 0L) {
            mplew.writeShort(chr.getMobCollection().size());
            for (Map.Entry<Integer, String> entry : chr.getMobCollection().entrySet()) {
                mplew.writeInt(entry.getKey());
                mplew.writeMapleAsciiString(entry.getValue());
            }
        }
        mplew.writeInt(0);
        if ((flag & 0x400000000000000L) != 0L) {
            mplew.writeShort(0);
        }
        if ((flag & 0x400000000000000L) != 0L) {
            mplew.writeShort(0);
        }
        if ((flag & 0x800000000000000L) != 0L) {
            VCorePacket.writeVCoreSkillData(mplew, chr);
        }
        chr.loadHexSkills();
        PacketHelper.encodeHexaSkills(mplew, chr);
        chr.loadHexStats();
        PacketHelper.encodeSixStats(mplew, chr);
        if ((flag & 0x1000000000000000L) != 0L) {
            int var26_165 = 0;
            int n = 0;
            mplew.writeInt(n);
            boolean bl = false;
            while (var26_165 < n) {
                int var27_208 = 0;
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                boolean bl6 = false;
                while (var27_208 < 3) {
                    mplew.writeInt(0);
                    ++var27_208;
                }
                mplew.writeLong(0L);
                ++var26_165;
            }
        }
        if ((flag & 0x1000000000000000L) != 0L) {
            int var26_167 = 0;
            int n = 0;
            mplew.writeInt(n);
            boolean bl = false;
            while (var26_167 < n) {
                mplew.writeInt(0);
                ++var26_167;
            }
        }
        if ((flag & 0x1000000000000000L) != 0L) {
            int var27_211 = 0;
            int var26_169 = 0;
            mplew.writeInt(chr.getClient().getAccID());
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
            mplew.writeInt(-1);
            mplew.writeInt(Integer.MAX_VALUE);
            mplew.writeLong(PacketHelper.getTime(-2L));
            int n = 0;
            mplew.writeInt(n);
            boolean bl = false;
            while (var26_169 < n) {
                mplew.writeInt(0);
                mplew.write(0);
                mplew.write(0);
                mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
                int n3 = 41;
                mplew.writeInt(n3);
                if (n3 == 42) {
                    mplew.writeMapleAsciiString("");
                } else {
                    mplew.writeLong(0L);
                }
                ++var26_169;
            }
            int n4 = 0;
            mplew.writeInt(n4);
            boolean bl7 = false;
            while (var27_211 < n4) {
                mplew.writeInt(0);
                mplew.write(0);
                mplew.writeLong(0L);
                ++var27_211;
            }
        }
        if ((flag & 0x20L) != 0L) {
            int var26_172 = 0;
            int n = 0;
            mplew.writeInt(n);
            boolean bl = false;
            while (var26_172 < n) {
                mplew.writeLong(0L);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeLong(0L);
                mplew.writeLong(0L);
                mplew.writeLong(0L);
                ++var26_172;
            }
        }
        int n = 3;
        while (var25_116 > 0) {
            if ((flag & (var25_116 <= 2 ? Long.MIN_VALUE : 0x10000000L)) != 0L) {
                PacketHelper.encodeCombingRoomInventory(mplew, chr.getSalon().getOrDefault((int)var25_116, new LinkedList()));
            }
            --var25_116;
        }
        if ((flag & 0x8000000L) != 0L) {
            int var26_180 = 0;
            int var26_178 = 0;
            int var26_176 = 0;
            int var26_174 = 0;
            int n5 = 0;
            mplew.writeInt(n5);
            boolean bl = false;
            while (var26_174 < n5) {
                mplew.writeInt(0);
                mplew.writeZeroBytes(14);
                ++var26_174;
            }
            int n6 = 0;
            mplew.writeInt(n6);
            boolean bl8 = false;
            while (var26_176 < n6) {
                mplew.writeShort(0);
                mplew.writeInt(0);
                ++var26_176;
            }
            mplew.writeShort(8);
            int n7 = 0;
            mplew.writeInt(n7);
            boolean bl9 = false;
            while (var26_178 < n7) {
                mplew.writeShort(0);
                mplew.writeZeroBytes(25);
                ++var26_178;
            }
            int n8 = 0;
            mplew.writeInt(n8);
            boolean bl10 = false;
            while (var26_180 < n8) {
                mplew.writeMapleAsciiString("");
                mplew.writeZeroBytes(25);
                ++var26_180;
            }
        }
        if ((flag & 0x2000000000000000L) != 0L) {
            mplew.writeInt(pets.size());
            for (MaplePet maplePet : pets) {
                int var28_236 = 0;
                mplew.writeLong(maplePet.getUniqueId());
                int n9 = 0;
                mplew.writeInt(n9);
                boolean bl = false;
                while (var28_236 < n9) {
                    mplew.writeInt(0);
                    ++var28_236;
                }
            }
        }
        if ((flag & 0x4000000000000L) != 0L) {
            mplew.write(1);
            String string = chr.getOneInfo(17008, "T");
            String string2 = chr.getOneInfo(17008, "L");
            String string3 = chr.getOneInfo(17008, "E");
            mplew.write(string == null ? 0 : Integer.valueOf(string));
            mplew.writeInt(string2 == null ? 1 : Integer.valueOf(string2));
            mplew.writeInt(string3 == null ? 0 : Integer.valueOf(string3));
            mplew.writeInt(100 - chr.getPQLog("航海能量"));
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
            String string4 = chr.getInfoQuest(17018);
            String[] stringArray = string4.split(";");
            mplew.writeShort(!string4.isEmpty() ? stringArray.length : 0);
            for (String questinfo1 : stringArray) {
                if (questinfo1.isEmpty()) continue;
                String[] split = questinfo1.split("=");
                mplew.write(Integer.valueOf(split[0]));
                mplew.writeInt(Integer.valueOf(split[1]));
                mplew.writeInt(0);
            }
            mplew.writeShort(ItemConstants.航海材料.length);
            for (int i17 : ItemConstants.航海材料) {
                mplew.writeInt(i17);
                mplew.writeInt(chr.getPQLog(String.valueOf(i17)));
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
            }
        }
        if ((flag & 0x8000000000000L) != 0L) {
            LinkedList<Integer> linkedList = new LinkedList<Integer>();
            if (chr.getKeyValue("InnerGlareBuffs") != null) {
                int var28_239 = 0;
                String[] stringArray = chr.getKeyValue("InnerGlareBuffs").split(",");
                int n10 = stringArray.length;
                boolean bl = false;
                while (var28_239 < n10) {
                    String string = stringArray[var28_239];
                    if (!string.isEmpty()) {
                        linkedList.add(Integer.parseInt(string));
                    }
                    ++var28_239;
                }
            }
            mplew.writeReversedVarints(linkedList.size());
            Iterator iterator = linkedList.iterator();
            while (iterator.hasNext()) {
                int n11 = (Integer)iterator.next();
                mplew.writeInt(n11);
            }
        }
        mplew.write(new byte[37]);
    }

    public static void showCharacterInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, long flag) {
        if ((flag & 0x80L) != 0L) {
            mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIPPED).getSlotLimit());
            MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
            List<Item> Inbudy = iv.newList();
            Collections.sort(Inbudy);
            for (short slotEquip = 0; slotEquip <= 128; slotEquip = (short)(slotEquip + 1)) {
                if (iv.getItem(slotEquip) == null) continue;
                mplew.writeShort(iv.getItem(slotEquip).getPosition());
                mplew.write(iv.getItem(slotEquip).getType());
                mplew.writeInt(iv.getItem(slotEquip).getItemId());
                mplew.write(0);
                mplew.writeLong(150842304000000000L);
                mplew.writeInt(iv.getItem(slotEquip).getExtendSlot());
                mplew.writeBool(false);
                mplew.writeShort(0);
            }
        }
    }

    public static void encodeHexaSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getHexaSkills().size());
        for (MapleHexaSkill mhs : chr.getHexaSkills().values()) {
            mplew.writeInt(mhs.getId());
            mplew.writeInt(mhs.getSkilllv());
            mplew.write(1);
            mplew.write(0);
        }
        mplew.write(0);
        int HEXA2_Size = 0;
        mplew.writeInt(HEXA2_Size);
        for (int i = 0; i < HEXA2_Size; ++i) {
            mplew.writeInt(0);
        }
    }

    public static void encodeSixStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getHexaStats().size());
        for (Map.Entry<Integer, MapleHexaStat> msss : chr.getHexaStats().entrySet()) {
            mplew.writeInt(msss.getValue().getSolt());
            mplew.writeInt(msss.getValue().getSolt());
            mplew.writeInt(0);
            mplew.writeInt(1);
            mplew.writeInt(msss.getValue().getMain0());
            mplew.writeInt(msss.getValue().getMain0Lv());
            mplew.writeInt(msss.getValue().getAddit0S1());
            mplew.writeInt(msss.getValue().getAddit0S1Lv());
            mplew.writeInt(msss.getValue().getAddit0S2());
            mplew.writeInt(msss.getValue().getAddit0S2Lv());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getSolt());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : 1);
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : 1);
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getMain1());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getMain1Lv());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getAddit1S1());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getAddit1S1Lv());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getAddit1S2());
            mplew.writeInt(msss.getValue().getMain1() == -1 ? 0 : msss.getValue().getAddit1S2Lv());
        }
        mplew.writeInt(chr.getHexaStats().size());
        for (Map.Entry<Integer, MapleHexaStat> msss : chr.getHexaStats().entrySet()) {
            mplew.writeInt(msss.getValue().getSolt());
            mplew.writeInt(msss.getValue().getSolt());
            mplew.writeInt(msss.getValue().getPreset());
        }
        int HEXA5_Size = 0;
        mplew.writeInt(HEXA5_Size);
        for (int i = 0; i < HEXA5_Size; ++i) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        int HEXA6_Size = 0;
        mplew.writeInt(HEXA6_Size);
        for (int i = 0; i < HEXA6_Size; ++i) {
            mplew.writeInt(0);
        }
    }

    public static void encodeCombingRoomInventory(MaplePacketLittleEndianWriter mplew, List<Integer> styles) {
        mplew.write(styles.size());
        mplew.write(styles.size());
        for (int i = 1; i <= 102; ++i) {
            mplew.write(styles.size() >= i);
            if (styles.size() < i) continue;
            PacketHelper.encodeCombingRoomSlot(mplew, styles.get(i - 1));
        }
    }

    public static void encodeCombingRoomSlot(MaplePacketLittleEndianWriter mplew, int styleID) {
        mplew.write(0);
        mplew.writeInt(styleID);
        mplew.writeInt(styleID);
    }

    public static void addRedLeafInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int[] idarr = new int[]{9410165, 9410166, 9410167, 9410168, 9410198};
        mplew.writeInt(chr.getClient().getAccID());
        mplew.writeInt(chr.getId());
        mplew.writeInt(idarr.length);
        mplew.writeInt(0);
        for (int i = 0; i < idarr.length; ++i) {
            mplew.writeInt(idarr[i]);
            mplew.writeInt(chr.getFriendShipPoints()[i]);
        }
    }

    public static void encodeInventory(MaplePacketLittleEndianWriter mplew, List<Item> list, MapleCharacter chr) {
        LinkedList<Item> items = new LinkedList<Item>(list);
        items.add(null);
        for (Item item : items) {
            PacketHelper.addItemPosition(mplew, item, false, false);
            if (item == null) break;
            PacketHelper.GW_ItemSlotBase_Encode(mplew, item);
        }
    }

    public static void addShopInfo(MaplePacketLittleEndianWriter mplew, MapleShop shop, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.writeInt(0);
        mplew.writeInt(2025021205);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        List<MapleShopItem> shopItems = shop.getItems(c);
        mplew.writeShort(shopItems.size());
        for (MapleShopItem item : shopItems) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(item.getItemId());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(94354848000000000L);
            mplew.writeInt(0);
            mplew.writeInt(item.getPrice());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(item.getPointQuestID());
            mplew.writeInt(0);
            mplew.writeInt(item.getPointPrice());
            mplew.writeInt(item.getBuyLimitWorldAccount());
            mplew.writeInt(item.getMinLevel());
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeLong(94354848000000000L);
            mplew.writeLong(150842304000000000L);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.writeInt(1);
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.writeMapleAsciiString("");
            short slotMax = ii.getSlotMax(item.getItemId());
            if (ItemConstants.類型.可充值道具(item.getItemId())) {
                mplew.writeDouble(ii.getUnitPrice(item.getItemId()));
            } else {
                short quantity = item.getQuantity() == 0 ? slotMax : item.getQuantity();
                mplew.writeShort(quantity);
                slotMax = quantity > 1 ? (short)1 : (item.getBuyable() == 0 ? slotMax : item.getBuyable());
            }
            mplew.writeShort(slotMax);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("1900010100");
            mplew.writeMapleAsciiString("2079010100");
            for (int i = 0; i < 4; ++i) {
                mplew.writeInt(0);
            }
            mplew.writeInt(9410165);
            mplew.writeInt(0);
            mplew.writeInt(9410166);
            mplew.writeInt(0);
            mplew.writeInt(9410167);
            mplew.writeInt(0);
            mplew.writeInt(9410168);
            mplew.writeInt(0);
            mplew.writeInt(9410198);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            Item rebuy = item.getRebuy();
            mplew.write(rebuy == null ? 0 : 1);
            if (rebuy == null) continue;
            PacketHelper.GW_ItemSlotBase_Encode(mplew, rebuy);
        }
    }

    public static void addJaguarInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (JobConstants.is狂豹獵人(chr.getJob())) {
            mplew.write(chr.getIntNoRecord(111112));
            for (int i = 1; i <= 5; ++i) {
                mplew.writeInt(0);
            }
        }
    }

    public static void addSkillPets(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (JobConstants.is陰陽師(chr.getJob()) && chr.getHaku() != null) {
            mplew.write(1);
            mplew.writeInt(chr.getHaku().getObjectId());
            mplew.writeInt(40020109);
            mplew.write(1);
            mplew.writePos(chr.getHaku().getPosition());
            mplew.write(0);
            mplew.writeShort(chr.getHaku().getStance());
        }
    }

    public static void writeDressUpInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter player) {
        boolean bl2 = JobConstants.is天使破壞者(player.getJob());
        if (bl2 && player.getKeyValue("Longcoat") == null) {
            player.setKeyValue("Longcoat", "1051291");
        }
        mplew.writeInt(bl2 ? player.getSecondFace() : 0);
        mplew.writeInt(bl2 ? player.getSecondHair() : 0);
        mplew.writeInt(bl2 ? Integer.valueOf(player.getKeyValue("Longcoat")) : 0);
        mplew.write(bl2 ? player.getSecondSkinColor() : (byte)0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
    }

    public static void write劍刃之壁(MaplePacketLittleEndianWriter mplew, MapleCharacter player, int sourceid) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        int n4 = 0;
        switch (sourceid) {
            case 61101002: {
                n4 = 1;
                break;
            }
            case 61120007: {
                n4 = 2;
                break;
            }
            case 61110211: {
                n4 = 3;
                break;
            }
            case 61121217: {
                n4 = 4;
            }
        }
        int n5 = sourceid == 61101002 || sourceid == 61110211 ? 3 : (sourceid == 0 ? 0 : 5);
        for (int i2 = 0; i2 < n5; ++i2) {
            arrayList.add(0);
        }
        mplew.writeInt(n4);
        mplew.writeInt(n5);
        mplew.writeInt(player.getBuffedIntZ(SecondaryStat.StopForceAtomInfo));
        mplew.writeInt(arrayList.size());
        arrayList.forEach(mplew::writeInt);
    }

    public static void writeSonOfLinkedSkill(MaplePacketLittleEndianWriter mplew, int skillId, Pair<Integer, SkillEntry> skillinfo) {
        mplew.writeInt(skillinfo.getLeft());
        mplew.writeInt(skillinfo.getRight().teachId);
        mplew.writeInt(skillId);
        mplew.writeShort(skillinfo.getRight().skillevel);
        mplew.writeLong(PacketHelper.getTime(skillinfo.getRight().expiration));
        mplew.writeInt(skillinfo.getRight().teachTimes);
    }

    public static void writeEsInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        MapleInventory inventory = chr.getInventory(MapleInventoryType.ELAB);
        List<Item> list = inventory.newList();
        ArrayList<Item> list2 = new ArrayList<Item>();
        for (Item item : list) {
            if (item.getESPos() == 0) continue;
            list2.add(item);
        }
        mplew.writeShort(list2.size());
        for (Item item : list2) {
            mplew.writeShort(item.getESPos() - 1);
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getQuantity());
        }
        mplew.writeShort(inventory.list().size());
        for (Item item : inventory.list()) {
            mplew.writeShort(item.getESPos() - 1);
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getQuantity());
        }
    }

    public static void addDamageSkinInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int skinId = chr.getDamageSkin();
        mplew.writeInt(chr.getDamSkinList().size());
        if (chr.isGm()) {
            chr.dropMessage(40, "[GM] 傷害字型總數欄位:" + chr.getDamSkinList().size() + ", 選擇的字型已經變更儲存。");
        }
        Iterator<Integer> iterator = chr.getDamSkinList().iterator();
        while (iterator.hasNext()) {
            int id = 0;
            PacketHelper.addDamageSkinInfo0(mplew, id, (id = iterator.next().intValue()) >= 0 ? MapleItemInformationProvider.getInstance().getDamageSkinItemId(id) : 0, 0, "");
        }
        PacketHelper.addDamageSkinInfo0(mplew, skinId, skinId >= 0 ? MapleItemInformationProvider.getInstance().getDamageSkinItemId(skinId) : 0, 0, "");
        PacketHelper.addDamageSkinInfo0(mplew, -1, 0, 1, "");
        PacketHelper.addDamageSkinInfo0(mplew, -1, 0, 1, "");
    }

    private static void addDamageSkinInfo0(MaplePacketLittleEndianWriter mplew, int skinId, int skinItemId, int b, String s) {
        mplew.writeInt(skinId);
        mplew.writeInt(skinItemId);
        mplew.writeLong(PacketHelper.getTime(-2L));
    }

    public static void addChaterName(MaplePacketLittleEndianWriter mplew, String speekerName, String text) {
        PacketHelper.addChaterName(mplew, speekerName, text, 0);
    }

    public static void addChaterName(MaplePacketLittleEndianWriter mplew, String speekerName, String text, int chrId) {
        mplew.writeMapleAsciiString(speekerName);
        mplew.writeMapleAsciiString(text);
        mplew.writeInt(Randomizer.nextInt());
        mplew.writeInt(Randomizer.nextInt());
        mplew.writeShort(6);
        mplew.writeInt(Randomizer.nextInt());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(3);
        mplew.write(0);
        mplew.write(6);
    }

    public static List<Pair<SecondaryStat, Pair<Integer, Integer>>> sortBuffStats(Map<SecondaryStat, Pair<Integer, Integer>> statups) {
        boolean changed;
        ArrayList<Pair<SecondaryStat, Pair<Integer, Integer>>> statvals = new ArrayList<Pair<SecondaryStat, Pair<Integer, Integer>>>();
        for (Map.Entry<SecondaryStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
            statvals.add(new Pair<SecondaryStat, Pair<Integer, Integer>>(stat.getKey(), stat.getValue()));
        }
        do {
            changed = false;
            int i = 0;
            int k = 1;
            for (int iter = 0; iter < statvals.size() - 1; ++iter) {
                Pair<SecondaryStat, Pair<Integer, Integer>> a = statvals.get(i);
                Pair<SecondaryStat, Pair<Integer, Integer>> b = statvals.get(k);
                if (a != null && b != null && ((SecondaryStat)a.left).getFlag() > ((SecondaryStat)b.left).getFlag()) {
                    Pair<SecondaryStat, Pair<Integer, Integer>> swap = new Pair<>(a.left, a.right);
                    statvals.remove(i);
                    statvals.add(i, b);
                    statvals.remove(k);
                    statvals.add(k, swap);
                    changed = true;
                }
                ++i;
                ++k;
            }
        } while (changed);
        return statvals;
    }
}

