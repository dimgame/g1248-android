package chat.dim.g1248.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.cache.game.HistoryCache;
import chat.dim.g1248.NotificationNames;
import chat.dim.g1248.PlayerOne;
import chat.dim.g1248.SharedDatabase;
import chat.dim.g1248.model.Board;
import chat.dim.g1248.model.Room;
import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.Content;
import chat.dim.protocol.CustomizedContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.utils.Log;

public class RoomHandler extends GameRoomContentHandler {

    private final SharedDatabase database;

    public RoomHandler(SharedDatabase db) {
        super();
        database = db;
    }

    @Override
    protected List<Content> handleWatchRequest(ID sender, CustomizedContent content, ReliableMessage rMsg) {
        // C -> S: "watching"
        throw new AssertionError("should not happen: " + content);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Content> handleWatchResponse(ID sender, CustomizedContent content, ReliableMessage rMsg) {
        Log.info("[GAME] received watch response: " + sender + ", " + content);
        // S -> C: "boards"
        int rid;
        Object integer;
        integer = content.get("rid");
        if (integer == null) {
            throw new AssertionError("room id not found: " + content);
        } else {
            rid = ((Number) integer).intValue();
        }

        Object array = content.get("boards");
        if (array instanceof List) {
            PlayerOne theOne = PlayerOne.getInstance();
            int myTid = theOne.getRid();
            int myBid = theOne.getBid();
            int myGid = theOne.getGid();

            List<Board> boards = Board.convertBoards((List<Object>) array);
            for (Board item : boards) {
                if (rid == myTid && item.getBid() == myBid) {
                    Log.debug("this board is conflict, check player");
                    ID player = item.getPlayer();
                    if (player == null || theOne.equals(player) || myGid > 0) {
                        // this board is mine now
                        Log.info("skip my board: rid=" + rid + ", " + item);
                        continue;
                    }
                    // this board has been occupied by other player, refresh it
                    Log.warning("this board is occupied by " + player);
                    theOne.board = item;
                }
                Log.info("update board: rid=" + rid + ", " + item);

                if (!database.updateBoard(rid, item)) {
                    Log.error("failed to update board: rid=" + rid + ", " + item);
                    continue;
                }

                Map<String, Object> info = new HashMap<>();
                info.put("rid", rid);
                info.put("bid", item.getBid());
                info.put("board", item);
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(NotificationNames.GameBoardUpdated, this, info);
            }
        }

        return null;
    }

    @Override
    protected List<Content> handlePlayRequest(ID sender, CustomizedContent content, ReliableMessage rMsg) {
        // C -> S: "playing"
        throw new AssertionError("should not happen: " + content);
    }

    @Override
    protected List<Content> handlePlayResponse(ID sender, CustomizedContent content, ReliableMessage rMsg) {
        Log.info("[GAME] received play response: " + sender + ", " + content);
        // S -> C: "played"
        int rid = (int) content.get("rid");
        int bid = (int) content.get("bid");
        int gid = (int) content.get("gid");
        ID player = ID.parse(content.get("player"));
        if (rid <= 0 || bid < 0 || gid <= 0 || player == null) {
            Log.error("play response error: " + content);
            return null;
        }

        PlayerOne theOne = PlayerOne.getInstance();
        Room room = theOne.room;
        Board board = theOne.board;
        if (room == null || board == null) {
            Log.error("not playing now");
        } else if (room.getRid() != rid || board.getBid() != bid) {
            Log.error("play response not match: " + content);
        } else if (board.getGid() == 0) {
            Log.info("update current board: gid=" + gid + ", player=" + player);
            board.setGid(gid);
            board.setPlayer(player);
            database.updateBoard(rid, board);
            HistoryCache hdb = (HistoryCache) database.historyDatabase;
            hdb.updatePlayingHistory(rid, bid, gid, player);
        } else {
            // if player changed, means this seat is token away by another player
            board.setPlayer(player);
        }

        return null;
    }
}
