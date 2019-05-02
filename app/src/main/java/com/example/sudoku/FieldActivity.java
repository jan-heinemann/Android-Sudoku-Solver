package com.example.sudoku;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;

class MyAdapter extends BaseAdapter {

    private final Context mContext;
    int[][] field;

    // 1
    public MyAdapter(Context context, int[][] field) {
        this.mContext = context;
        this.field = field;
    }

    // 2
    @Override
    public int getCount() {
        return field.length * field[0].length;
    }

    // 3
    @Override
    public long getItemId(int position) {
        return 0;
    }

    // 4
    @Override
    public Object getItem(int position) {
        return null;
    }

    // 5
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        EditText dummyTextView = new EditText(mContext);

        int row  = -1;
        final int column = position % 9;
        if(position % 9 == 0) {
            row = position / 9;
        }
        else {
            row = (int)(1 + position/9) -1 ;
        }


        final int myRow = row;
        final int myColumn = column;

        if(field[row][column] != 0)
            dummyTextView.setText(String.valueOf(field[row][column]));


        dummyTextView.setBackgroundColor(Color.GRAY);


        if(row < 3 || row > 5) {
            if(column >2 && column < 6) {
                dummyTextView.setBackgroundColor(Color.rgb(0xd3, 0xd3, 0xd3));
            }
        }
        else {
            if(column < 3 || column > 5) {
                dummyTextView.setBackgroundColor(Color.rgb(0xd3, 0xd3, 0xd3));

            }
        }

        if(row % 2 == 0) {
            if(column % 2 == 0) {
                int c = ((ColorDrawable)dummyTextView.getBackground()).getColor();
                c += 0x101010;
                dummyTextView.setBackgroundColor(c);
            }
        }
        else {
            if(column % 2 == 1) {
                int c = ((ColorDrawable)dummyTextView.getBackground()).getColor();
                c += 0x101010;
                dummyTextView.setBackgroundColor(c);
            }
        }

        dummyTextView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});


        dummyTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        dummyTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("myTest", s.toString());

                Log.d("myTest1", Integer.toString(myRow));
                Log.d("myTest2", Integer.toString(myColumn));

                if(s.length() > 0) {
                    field[myRow][myColumn] = Integer.parseInt(s.toString());
                }
            }
        });

        return dummyTextView;
    }

}

public class FieldActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field);

        Intent myIntent = getIntent();


        int[][] board = (int[][])myIntent.getSerializableExtra("myBoard");


        final GridView gridView = (GridView)findViewById(R.id.gridView);
        /* board = {
                { 8, 0, 0, 0, 0, 0, 0, 0, 0 },
                { 0, 0, 3, 6, 0, 0, 0, 0, 0 },
                { 0, 7, 0, 0, 9, 0, 2, 0, 0 },
                { 0, 5, 0, 0, 0, 7, 0, 0, 0 },
                { 0, 0, 0, 0, 4, 5, 7, 0, 0 },
                { 0, 0, 0, 1, 0, 0, 0, 3, 0 },
                { 0, 0, 1, 0, 0, 0, 0, 6, 8 },
                { 0, 0, 8, 5, 0, 0, 0, 1, 0 },
                { 0, 9, 0, 0, 0, 0, 4, 0, 0 }
        };*/

        final MyAdapter booksAdapter = new MyAdapter(this, board);
        gridView.setAdapter(booksAdapter);

        Button btn_continue = findViewById(R.id.btn_continue);
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SudokuManager sM = new SudokuManager(booksAdapter.field);
                if(sM.solve()) {
                    MyAdapter booksAdapter = new MyAdapter(FieldActivity.this, sM.solvedField);
                    gridView.setAdapter(booksAdapter);
                }
                else {
                    Toast.makeText(getBaseContext(), "Could not solve Sudoku", Toast.LENGTH_LONG).show();

                }


            }
        });
    }
}
