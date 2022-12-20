package chat.dim.game1248.table;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import chat.dim.g1248.model.History;
import chat.dim.g1248.model.Square;
import chat.dim.g1248.model.State;
import chat.dim.game1248.R;
import chat.dim.threading.BackgroundThreads;
import chat.dim.threading.MainThread;

public class BoardFragment extends Fragment {

    TableViewModel mViewModel = null;
    private BoardAdapter adapter = null;

    private TextView scoreView = null;
    private GridView boardView = null;

    int tableId;
    int boardId;
    int score;
    final List<Integer> state = new ArrayList<>();

    public BoardFragment(int tid, int bid) {
        super();
        tableId = tid;
        boardId = bid;
        score = 0;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.board_fragment, container, false);

        scoreView = view.findViewById(R.id.score_text_view);
        boardView = view.findViewById(R.id.board_grid_view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(TableViewModel.class);
        // TODO: Use the ViewModel

        // create adapter
        adapter = new BoardAdapter(getContext(), R.layout.griditem_squares, state);
        boardView.setAdapter(adapter);
        // load data in background
        BackgroundThreads.rush(this::reloadData);
    }

    protected void reloadData() {
        History history = mViewModel.getHistory(12301);
        if (history == null) {
            return;
        }
        score = history.getScore();
        State matrix = history.getState();
        List<Square> squares = matrix.getSquares();
        assert squares != null && squares.size() == 16 : "state error: " + squares;
        state.clear();
        state.addAll(Square.revert(squares));

        MainThread.call(this::onReload);
    }

    protected void onReload() {
        scoreView.setText("Score: " + score);
        adapter.notifyDataSetChanged();
    }
}
