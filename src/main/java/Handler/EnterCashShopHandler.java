package Handler;

import Client.MapleClient;
import Client.inventory.Item;
import Config.constants.ServerConstants;
import Opcode.Headler.OutHeader;
import Packet.MTSCSPacket;
import Packet.MaplePacketCreator;
import Server.cashshop.CashShopServer;
import connection.OutPacket;
import connection.packet.Login;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.util.List;

import static Server.cashshop.handler.CashShopOperation.*;

public class EnterCashShopHandler {
    private static MapleClient c;

    public static MapleClient getC() {
        return c;
    }

    public static void Start(MapleClient c) {
        c.write(Login.sendServerValues());
        c.write(Login.sendServerEnvironment());
        c.announce(c.getEncryptOpcodesData(ServerConstants.OpcodeEncryptionKey));
        c.outPacket(OutHeader.LP_LOGIN_ACTION_CHECK.getValue());
        c.announce(MTSCSPacket.warpchartoCS(c));
        c.announce(MTSCSPacket.warpCS(false));
        c.updateLoginState(2, c.getSessionIPAddress());
        c.write(chatServerResult());
        c.announce(MTSCSPacket.getCashShopStyleCouponPreviewInfo());
        c.announce(MTSCSPacket.loadLockerDone(c));
        c.write(EventNotice_unk());
        c.write(enterCashShop());
        c.write(CASH_SHOP_ENTER_TYPE());
        c.outPacket(2515, "06 00 00 00 30 00 35 00 00 00 11 00");
        c.outPacket(OutHeader.LP_Parcel.getValue(), 0);
        c.outPacket(2515, "07 00 00");
        c.outPacket(2515, "09 00 00");
        c.outPacket(2536, "01 03 00 6E 65 77");
        c.outPacket(2515, "0B 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
        c.outPacket(2521, "00 00 00 00 00 00 00 00 00 00 00 00");
        c.announce(CashShopQueryCashResult(c, true, true, true));
        CashShopServer.getPlayerStorage().registerPlayer(c.getPlayer());
        c.announce(MaplePacketCreator.serverMessage(""));
        List<Pair<Item, String>> gifts = c.getPlayer().getCashInventory().loadGifts();
        c.announce(MTSCSPacket.商城禮物信息(gifts));
        c.announce(MTSCSPacket.sendWishList(c.getPlayer(), false));
        c.getPlayer().getCashInventory().checkExpire(c);
    }

    public static OutPacket CASH_SHOP_ENTER_TYPE() {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_ENTER_TYPE);
        outPacket.encodeInt(3);
        outPacket.encodeByte((int)0);
        return outPacket;
    }

    public static byte[] CashShopQueryCashResult(MapleClient c, boolean cs_1, boolean cs_2, boolean cs_3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(OutHeader.LP_CashShopQueryCashResult.getValue());
        if (cs_1) {
            mplew.writeInt(c.getPlayer().getCSPoints(1));
        } else {
            mplew.writeInt(0);
        }

        if (cs_2) {
            mplew.writeInt(c.getPlayer().getCSPoints(2));
        } else {
            mplew.writeInt(0);
        }

        if (cs_3) {
            mplew.writeInt(c.getPlayer().getMileage());
        } else {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static OutPacket chatServerResult() {
        OutPacket outPacket = new OutPacket(OutHeader.CHAT_SERVER_RESULT);
        outPacket.encodeInt(0);
        outPacket.encodeShort((int)0);
        return outPacket;
    }

    public static OutPacket EventNotice_unk() {
        OutPacket outPacket = new OutPacket(OutHeader.LP_CheckProcess.getValue());
        outPacket.encodeInt(25);
        outPacket.encodeInt(1);
        outPacket.encodeString("布萊爾小姐的夢幻快遞");
        outPacket.encodeInt(25);
        outPacket.encodeInt(44);
        outPacket.encodeInt(1);
        outPacket.encodeInt(20220225);
        outPacket.encodeInt(20220301);
        outPacket.encodeInt(100000);
        outPacket.encodeInt(100000);
        outPacket.encodeInt(0);
        outPacket.encodeInt(-1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte((int)0);
        outPacket.encodeString("如果有想移動的現金道具，請利用布萊爾小姐的夢幻快遞！");
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket getShowQuestCompletion(int quest_cid) {
        OutPacket say = new OutPacket(OutHeader.LP_QuestClear.getValue());
        if (c.getPlayer() != null) {
            say.encodeInt(quest_cid);
        }

        return say;
    }

    public static OutPacket enterCashShop() {
        OutPacket outPacket = new OutPacket(OutHeader.LP_CashShopChargeParamResult);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket enterCashShopOpenWeb() {
        OutPacket outPacket = new OutPacket(OutHeader.ENTER_CASH_SHOP);
        outPacket.encodeByte((int)3);
        outPacket.encodeInt(0);
        return outPacket;
    }
}