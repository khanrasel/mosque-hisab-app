package com.khanrasel.mosquehisab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private DbHelper helper;
    private LinearLayout root, summaryBox, transactionBox;
    private TextView incomeText, expenseText, balanceText;
    private EditText amountInput, noteInput;
    private Spinner typeSpinner;

    private final String[] types = {"আয়", "ব্যয়", "দান/অনুদান"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        helper = new DbHelper();
        buildUi();
        refreshData();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFF202124);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(4), dp(5), dp(4), dp(5));
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        root.setBackgroundColor(0xFFF6F8F7);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("🕌 মসজিদ হিসাব", 26, true);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(0xFF176B45);
        root.addView(title);
        TextView subtitle = text("আয় • ব্যয় • দান-অনুদান", 14, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        summaryBox = new LinearLayout(this);
        summaryBox.setOrientation(LinearLayout.VERTICAL);
        summaryBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        summaryBox.setBackgroundColor(0xFFE4F3EA);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(summaryBox, cardParams);

        incomeText = text("মোট আয়: ৳০", 18, true);
        expenseText = text("মোট ব্যয়: ৳০", 18, true);
        balanceText = text("বর্তমান ব্যালেন্স: ৳০", 20, true);
        summaryBox.addView(incomeText);
        summaryBox.addView(expenseText);
        summaryBox.addView(balanceText);

        root.addView(text("নতুন হিসাব যোগ করুন", 20, true));
        typeSpinner = new Spinner(this);
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        root.addView(typeSpinner, new LinearLayout.LayoutParams(-1, -2));

        amountInput = new EditText(this);
        amountInput.setHint("টাকার পরিমাণ (যেমন ৫০০)");
        amountInput.setInputType(2);
        root.addView(amountInput, new LinearLayout.LayoutParams(-1, -2));

        noteInput = new EditText(this);
        noteInput.setHint("বিবরণ (যেমন বিদ্যুৎ বিল / দানকারীর নাম)");
        root.addView(noteInput, new LinearLayout.LayoutParams(-1, -2));

        Button add = button("হিসাব সংরক্ষণ করুন");
        add.setAllCaps(false);
        add.setOnClickListener(v -> addTransaction());
        root.addView(add);

        Button report = button("রিপোর্ট দেখুন");
        report.setAllCaps(false);
        report.setOnClickListener(v -> showReport());
        root.addView(report);

        root.addView(text("সাম্প্রতিক লেনদেন", 20, true));
        transactionBox = new LinearLayout(this);
        transactionBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(transactionBox);
        TextView footer = text("আপনার তথ্য এই ফোনেই সংরক্ষিত থাকে।", 12, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private void addTransaction() {
        String amountString = amountInput.getText().toString().trim().replace(",", "");
        String note = noteInput.getText().toString().trim();
        if (amountString.isEmpty()) {
            amountInput.setError("টাকার পরিমাণ লিখুন");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException ex) {
            amountInput.setError("সঠিক পরিমাণ লিখুন");
            return;
        }
        if (amount <= 0) {
            amountInput.setError("পরিমাণ শূন্যের বেশি হতে হবে");
            return;
        }
        if (note.isEmpty()) note = "বিবরণ দেওয়া হয়নি";

        ContentValues values = new ContentValues();
        values.put("type", types[typeSpinner.getSelectedItemPosition()]);
        values.put("amount", amount);
        values.put("note", note);
        values.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        helper.getWritableDatabase().insert("transactions", null, values);
        amountInput.setText("");
        noteInput.setText("");
        Toast.makeText(this, "হিসাব সংরক্ষণ হয়েছে", Toast.LENGTH_SHORT).show();
        refreshData();
    }

    private void refreshData() {
        SQLiteDatabase db = helper.getReadableDatabase();
        double income = total(db, "আয়") + total(db, "দান/অনুদান");
        double expense = total(db, "ব্যয়");
        incomeText.setText("মোট আয় ও অনুদান: " + money(income));
        expenseText.setText("মোট ব্যয়: " + money(expense));
        balanceText.setText("বর্তমান ব্যালেন্স: " + money(income - expense));

        transactionBox.removeAllViews();
        Cursor c = db.rawQuery("SELECT id,type,amount,note,date FROM transactions ORDER BY id DESC LIMIT 30", null);
        if (c.getCount() == 0) {
            transactionBox.addView(text("এখনো কোনো হিসাব যোগ করা হয়নি।", 14, false));
        }
        while (c.moveToNext()) {
            String line = c.getString(1) + " • " + money(c.getDouble(2));
            TextView row = text(line, 16, true);
            row.setBackgroundColor(0xFFFFFFFF);
            transactionBox.addView(row);
            TextView detail = text(c.getString(4) + "  |  " + c.getString(3), 13, false);
            detail.setBackgroundColor(0xFFFFFFFF);
            detail.setPadding(dp(8), 0, dp(8), dp(8));
            transactionBox.addView(detail);
            View divider = new View(this);
            divider.setBackgroundColor(0xFFDDE3DF);
            transactionBox.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        c.close();
    }

    private double total(SQLiteDatabase db, String type) {
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type=?", new String[]{type});
        double value = 0;
        if (c.moveToFirst()) value = c.getDouble(0);
        c.close();
        return value;
    }

    private String money(double amount) {
        return String.format(Locale.US, "৳%,.2f", amount);
    }

    private void showReport() {
        SQLiteDatabase db = helper.getReadableDatabase();
        double income = total(db, "আয়");
        double donation = total(db, "দান/অনুদান");
        double expense = total(db, "ব্যয়");
        String message = "মোট আয়: " + money(income)
                + "\nমোট দান/অনুদান: " + money(donation)
                + "\nমোট ব্যয়: " + money(expense)
                + "\n--------------------"
                + "\nঅবশিষ্ট ব্যালেন্স: " + money(income + donation - expense);
        new AlertDialog.Builder(this).setTitle("হিসাবের রিপোর্ট").setMessage(message)
                .setPositiveButton("বন্ধ করুন", null).show();
    }

    @Override protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }

    private class DbHelper extends SQLiteOpenHelper {
        DbHelper() { super(MainActivity.this, "mosque_hisab.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, amount REAL NOT NULL, note TEXT, date TEXT NOT NULL)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    }
}
