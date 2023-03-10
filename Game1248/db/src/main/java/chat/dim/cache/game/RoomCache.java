package chat.dim.cache.game;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.g1248.dbi.RoomDBI;
import chat.dim.g1248.model.Board;
import chat.dim.utils.Log;

public class RoomCache implements RoomDBI {

    public static final int MAX_BOARDS_COUNT = 4;

    // rid => sorted boards
    private final SparseArray<List<Board>> cachedBoards = new SparseArray<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static void fillBoards(int rid, List<Board> boards) {
        int index;
        boolean exists;
        for (index = 0; index < MAX_BOARDS_COUNT && boards.size() < MAX_BOARDS_COUNT; ++index) {
            exists = false;
            for (Board item : boards) {
                if (item.getBid() == index) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                boards.add(index, new Board(rid, index, Board.DEFAULT_SIZE));
            }
        }
    }

    @Override
    public List<Board> getBoards(int rid) {
        List<Board> boards = cachedBoards.get(rid);
        if (boards == null || boards.size() < MAX_BOARDS_COUNT) {
            // lock to fill
            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                if (boards == null) {
                    boards = new ArrayList<>();
                    cachedBoards.put(rid, boards);
                }
                fillBoards(rid, boards);
            } finally {
                writeLock.unlock();
            }
        }
        //assert boards.size() == MAX_BOARDS_COUNT : "boards error: " + boards;
        return boards;
    }

    @Override
    public Board getBoard(int rid, int bid) {
        List<Board> boards = getBoards(rid);
        //assert boards.size() == MAX_BOARDS_COUNT : "boards error: " + boards;
        Iterator<Board> iterator = boards.iterator();
        Board item;
        while (iterator.hasNext()) {
            item = iterator.next();
            if (/*item.getRid() == rid && */item.getBid() == bid) {
                return item;
            }
        }
        throw new IndexOutOfBoundsException("failed to get board: rid=" + rid + ", bid=" + bid);
    }

    @Override
    public boolean updateBoard(int rid, Board board) {
        if (board.getGid() <= 0) {
            Log.error("board error: rid=" + rid + ", " + board);
            return false;
        }
        List<Board> array = getBoards(rid);
        int bid = board.getBid();
        if (bid < 0 || bid >= array.size()) {
            Log.error("board error: rid=" + rid + ", " + board);
            return false;
        }
        Board old = array.get(bid);
        // if old board's gid is 0, means the bot not respond new gid yet
        // if new time not after current time, it's expired.
        if (old.getGid() <= 0 || !board.before(old.getTime())) {
            // lock to update
            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                array.set(bid, board);
            } finally {
                writeLock.unlock();
            }
            return true;
        } else {
            Log.warning("Board expired: old time=" + old.getTime() + ", new time=" + board.getTime());
            return false;
        }
    }
}
