package com.example.sudoku;

import android.nfc.Tag;
import android.util.Log;

public class SudokuManager {
    int[][] sudokuField;
    int[][] solvedField;
    int fieldSize = 9;

    public SudokuManager(int[][] input) {
        this.sudokuField = input;
    }

    private boolean isRowValid(int row, int[][] inputField) {
        int[] visited = new int[fieldSize+1];
        for(int i = 0; i < fieldSize; i++) {
            if(inputField[row][i] != 0 && visited[inputField[row][i]] > 0)
                return false;
            visited[inputField[row][i]]++;
        }
        return true;
    }
    private boolean isColumnValid(int column, int[][] inputField) {
        int[] visited = new int[fieldSize+1];
        for(int i = 0; i < fieldSize; i++) {
            if(inputField[i][column] != 0 && visited[inputField[i][column]] > 0)
                return false;
            visited[inputField[i][column]]++;
        }
        return true;
    }

    private boolean isBlockValid(int row, int column, int[][] inputField) {
        int[] visited = new int[fieldSize+1];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                if(inputField[row+i][column+j] != 0 && visited[inputField[row+i][column+j]] > 0)
                    return false;
                visited[inputField[row+i][column+j]]++;
            }
        }
        return true;
    }

    private boolean isFieldValid(int[][] inputField) {
        boolean result = true;
        for(int i = 0; i < 9; i++) {
            //if(!result) return false;
            result = result && isRowValid(i, inputField) && isColumnValid(i, inputField);
            if(i  % 3 == 0) {
                for(int j = 0; j < 9; j+=3) {
                    result = result && isBlockValid(i, j, inputField);
                }
            }
        }
        return result;
    }

    private boolean solveRecursive(int[][] board) {
        for(int row = 0; row <9;  row++) {
            for(int column = 0; column < 9; column++) {
                if(board[row][column] == 0) {
                    for(int i = 1; i <= 9; i++) {
                        board[row][column] = i;
                        if(isFieldValid(board) && solveRecursive(board)) {
                            return  true;
                        }
                        board[row][column] = 0;
                    }
                    return false;
                }
            }
        }
        solvedField = board;
        return true;
    }

    public boolean solve() {
        return solveRecursive(this.sudokuField);
    }

    public void printField(int[][] sudokuField) {
        for(int i = 0; i < fieldSize; i++) {
            String myRow = "| ";
            for(int j = 0; j < fieldSize; j++) {
                myRow+= sudokuField[i][j] + " | ";
            }
            Log.d("myField", myRow + "\n");
        }
    }
}
