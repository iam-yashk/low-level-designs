package org.example.LLD.TicTacToe;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Math.abs;

// 1. Symbol Class
class Symbol {
    private final String value;
    public Symbol(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}

// 2. Position Class
class Position {
    private int row, col;
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
    public int getRow() {
        return row;
    }
    public int getCol() {
        return col;
    }
}

// 3. Board Class
class Board {
    private final Symbol[][] grid;
    private int size;
    private final Symbol EMPTY = new Symbol("-");

    public Board(int size) {
        this.size = size;
        this.grid = new Symbol[size][size];
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                grid[i][j] = EMPTY;
            }
        }
    }

    private boolean isValidMove(Position p) {
        return (p.getRow() < size && p.getRow() >= 0 && p.getCol() < size && p.getCol() >= 0 && grid[p.getRow()][p.getCol()].getValue().equals(EMPTY.getValue()));
    }

    public boolean makeMove(Position p, Symbol symbol) {
        if(!isValidMove(p))
            return false;

        grid[p.getRow()][p.getCol()] = symbol;
        return true;
    }

    public boolean isFull() {
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                if(grid[i][j] == EMPTY) return false;
            }
        }
        return true;
    }

    public boolean checkWin(Symbol symbol) {
        // for row and columns
        for(int i = 0; i < size; i++) {
            boolean row = true, col = true;
            for(int j = 0; j < size; j++) {
                row = row & (grid[i][j].getValue().equals(symbol.getValue()));
                col = col & (grid[j][i].getValue().equals(symbol.getValue()));
            }
            if(row || col) return true;
        }
        // for diagonal
        boolean diag1 = true, diag2 = true;
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                diag1 = diag1 & grid[i][i].getValue().equals(symbol.getValue());
                diag2 = diag2 & grid[i][size - i - 1].getValue().equals(symbol.getValue());
            }
        }
        return diag1 || diag2;
    }

    public void display() {
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                System.out.print(grid[i][j].getValue() + ' ');
            }
            System.out.println();
        }
    }
}

// 4. PlayerStrategy Interface
interface PlayerStrategy {
    Position makeMove(Board board);
}

// 5. HumanStrategy Class
class HumanStrategy implements PlayerStrategy {
    private final Scanner sc = new Scanner(System.in);

    public Position makeMove(Board board) {
        System.out.println("Enter row and column position");
        int row = sc.nextInt();
        int col = sc.nextInt();
        return new Position(row, col);
    }
}

// 6. Player Class
class Player {
    private String name;
    private Symbol symbol;
    private PlayerStrategy strategy;

    public Player(String name, Symbol symbol, PlayerStrategy strategy) {
        this.name = name;
        this.symbol = symbol;
        this.strategy = strategy;
    }

    public String getName() {
        return name;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Position getMove(Board board) {
        return strategy.makeMove(board);
    }
}

// 7. PlayerFactory
class PlayerFactory {
    public static Player createPlayer(String type, String name, Symbol symbol) {
        if(type.equalsIgnoreCase("human")) {
            return new Player(name, symbol, new HumanStrategy());
        }
        throw new IllegalArgumentException("Unsupported player type");
    }
}

// 8. GameObserver Interface
interface GameObserver {
    void onMove(Position p, Symbol symbol);
    void onGameOver(String message);
}

// 9. ConsoleRenderer Class (Observer)
class ConsoleRenderer implements GameObserver {
    @Override
    public void onMove(Position p, Symbol symbol) {
        System.out.println("Move at (" + p.getRow() + "," + p.getCol() + ") by " + symbol.getValue());
    }

    @Override
    public void onGameOver(String message) {
        System.out.println(message);
    }
}

// 10. GameState Interface
interface GameState {
    void play(Game game);
}

// 11. InProgressState
class InProgressState implements GameState {
    @Override
    public void play(Game game) {
        Board board = game.getBoard();
        Player player = game.getCurrentPlayer();

        board.display();
        Position p = player.getMove(board);
        if(!board.makeMove(p, player.getSymbol())) {
            System.out.println("Invalid Move, Try Again");
            return;
        }

        game.notifyMove(p, player.getSymbol());

        if(board.checkWin(player.getSymbol())) {
            game.setState(new WinState(player));
        } else if(board.isFull()) {
            game.setState(new DrawState());
        } else {
            game.switchPlayer();
        }
    }
}

// 12. WinState
class WinState implements GameState {
    private final Player winner;
    public WinState(Player winner) {
        this.winner = winner;
    }

    @Override
    public void play(Game game) {
        game.getBoard().display();
        game.notifyGameOver("Player " + winner.getSymbol().getValue() + " wins!");
        game.stop();
    }
}

// 13 DrawState
class DrawState implements GameState {
    @Override
    public void play(Game game) {
        game.getBoard().display();
        game.notifyGameOver("It's a Draw!");
        game.stop();
    }
}

// 14. Game Class (Controller)
class Game {
    private final Board board;
    private final Player playerX, playerO;
    private Player currentPlayer;
    private GameState state;
    private boolean running = true;
    private final List<GameObserver> observers;

    public Game(Board board, Player px, Player po) {
        this.board = board;
        playerX = px;
        playerO = po;
        this.currentPlayer = px;
        this.state = new InProgressState();
        observers = new ArrayList<>();
    }
    public void addObserver(GameObserver obs) {
        observers.add(obs);
    }
    public void notifyMove(Position p, Symbol s) {
        observers.forEach(o -> o.onMove(p, s));
    }
    public void notifyGameOver(String message) {
        observers.forEach(o -> o.onGameOver(message));
    }
    public Board getBoard() {
        return board;
    }
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public void switchPlayer() {
        currentPlayer = (currentPlayer == playerX) ? playerO : playerX;
    }
    public void setState(GameState state) {
        this.state = state;
    }
    public void stop() {
        running = false;
    }
    public void start() {
        while(running) {
            state.play(this);
        }
    }
}

// 15. Main
public class ticTacToeMain {
    public static void main(String[] args) {
        Symbol X = new Symbol("x");
        Symbol O = new Symbol("o");

        Player p1 = PlayerFactory.createPlayer("human", "Player 1", X);
        Player p2 = PlayerFactory.createPlayer("human", "Player 2", O);

        Board board = new Board(3);
        Game game = new Game(board, p1, p2);

        game.addObserver(new ConsoleRenderer());
        game.start();
    }
}
