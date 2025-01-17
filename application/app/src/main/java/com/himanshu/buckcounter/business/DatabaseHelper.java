package com.himanshu.buckcounter.business;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.himanshu.buckcounter.beans.Account;
import com.himanshu.buckcounter.beans.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static com.himanshu.buckcounter.business.Constants.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper mInstance = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean isCreating = false;
    private SQLiteDatabase currentDB = null;

    public static DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void onConfigure(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.setForeignKeyConstraintsEnabled(true);
        super.onConfigure(sqLiteDatabase);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + TABLE_ACCOUNTS + "("
                + KEY_ACCOUNTS_NAME + " text PRIMARY KEY,"
                + KEY_ACCOUNTS_BALANCE + " real NOT NULL,"
                + KEY_ACCOUNTS_IS_CREDIT_CARD + " integer DEFAULT 0,"
                + KEY_ACCOUNTS_CREDIT_LIMIT + " real,"
                + KEY_ACCOUNTS_IS_ARCHIVED + " integer DEFAULT 0"
                + ")"
        );
        sqLiteDatabase.execSQL("create table " + TABLE_TRANSACTIONS + "("
                + KEY_TRANSACTIONS_ID + " integer PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRANSACTIONS_TYPE + " text NOT NULL,"
                + KEY_TRANSACTIONS_PARTICULARS + " text,"
                + KEY_TRANSACTIONS_AMOUNT + " real NOT NULL,"
                + KEY_TRANSACTIONS_TIMESTAMP + " datetime NOT NULL,"
                + KEY_TRANSACTIONS_DR_ACCOUNT + " text,"
                + KEY_TRANSACTIONS_CR_ACCOUNT + " text,"
                + "FOREIGN KEY(" + KEY_TRANSACTIONS_DR_ACCOUNT + ") REFERENCES " + TABLE_ACCOUNTS + "(" + KEY_ACCOUNTS_NAME + ") ON UPDATE CASCADE ON DELETE CASCADE,"
                + "FOREIGN KEY(" + KEY_TRANSACTIONS_CR_ACCOUNT + ") REFERENCES " + TABLE_ACCOUNTS + "(" + KEY_ACCOUNTS_NAME + ") ON UPDATE CASCADE ON DELETE CASCADE"
                + ")"
        );
        sqLiteDatabase.execSQL("create trigger if not exists " + TRIGGER_TRANSACTIONS_INSERT
                + " after insert on " + TABLE_TRANSACTIONS + " for each row"
                + " begin"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " + " + NEW + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + NEW + KEY_TRANSACTIONS_DR_ACCOUNT
                + " and (" + NEW + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.DR.getName() + "' or " + NEW + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " - " + NEW + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + NEW + KEY_TRANSACTIONS_CR_ACCOUNT
                + " and (" + NEW + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CR.getName() + "' or " + NEW + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " end"
        );
        sqLiteDatabase.execSQL("create trigger if not exists " + TRIGGER_TRANSACTIONS_DELETE
                + " after delete on " + TABLE_TRANSACTIONS + " for each row"
                + " begin"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " - " + OLD + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + OLD + KEY_TRANSACTIONS_DR_ACCOUNT
                + " and (" + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.DR.getName() + "' or " + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " + " + OLD + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + OLD + KEY_TRANSACTIONS_CR_ACCOUNT
                + " and (" + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CR.getName() + "' or " + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " end"
        );
        sqLiteDatabase.execSQL("create trigger if not exists " + TRIGGER_TRANSACTIONS_UPDATE_AMOUNT
                + " after update of " + KEY_TRANSACTIONS_AMOUNT + " on " + TABLE_TRANSACTIONS + " for each row"
                + " begin"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " - " + OLD + KEY_TRANSACTIONS_AMOUNT + " + " + NEW + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + OLD + KEY_TRANSACTIONS_DR_ACCOUNT
                + " and (" + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.DR.getName() + "' or " + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " update " + TABLE_ACCOUNTS + " set " + KEY_ACCOUNTS_BALANCE + " = " + KEY_ACCOUNTS_BALANCE + " + " + OLD + KEY_TRANSACTIONS_AMOUNT + " - " + NEW + KEY_TRANSACTIONS_AMOUNT
                + " where " + KEY_ACCOUNTS_NAME + " = " + OLD + KEY_TRANSACTIONS_CR_ACCOUNT
                + " and (" + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CR.getName() + "' or " + OLD + KEY_TRANSACTIONS_TYPE + " = '" + Transaction.TransactionType.CONTRA.getName() + "');"
                + " end"
        );
        /*
        isCreating = true;
        currentDB = sqLiteDatabase;
        insertDummyContent(sqLiteDatabase);
        // release var
        isCreating = false;
        currentDB = null;
        */
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            sqLiteDatabase.execSQL("alter table " + TABLE_ACCOUNTS + " add column " + KEY_ACCOUNTS_IS_ARCHIVED + " INTEGER default 0");
        } else {
            sqLiteDatabase.execSQL("drop table " + TABLE_TRANSACTIONS);
            sqLiteDatabase.execSQL("drop table " + TABLE_ACCOUNTS);
            sqLiteDatabase.execSQL("drop trigger if exists " + TRIGGER_TRANSACTIONS_INSERT);
            sqLiteDatabase.execSQL("drop trigger if exists " + TRIGGER_TRANSACTIONS_DELETE);
            sqLiteDatabase.execSQL("drop trigger if exists " + TRIGGER_TRANSACTIONS_UPDATE_AMOUNT);
            onCreate(sqLiteDatabase);
        }
    }

    private void insertDummyContent(SQLiteDatabase sqLiteDatabase) {
        ArrayList<Account> accounts = new ArrayList<>();
        accounts.add(new Account("cash"));
        accounts.add(new Account("bank"));
        accounts.add(new Account("credit card"));
        ContentValues contentValues = new ContentValues();
        for (Account account : accounts) {
            contentValues.put(KEY_ACCOUNTS_NAME, account.getName());
            contentValues.put(KEY_ACCOUNTS_BALANCE, account.getBalance());
            sqLiteDatabase.insert(TABLE_ACCOUNTS, null, contentValues);
            contentValues.clear();
        }
        ArrayList<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(Transaction.TransactionType.DR, "opening balance", 230.0, new Date(), "cash"));
        transactions.add(new Transaction(Transaction.TransactionType.CR, "opening balance", 3000.0, new Date(), "credit card"));
        transactions.add(new Transaction(Transaction.TransactionType.DR, "opening balance", 2500.0, new Date(), "bank"));
        transactions.add(new Transaction(Transaction.TransactionType.CONTRA, "ATM Withdrawal", 500.0, new Date(), "cash", "bank"));
        for (Transaction transaction : transactions) {
            contentValues.put(KEY_TRANSACTIONS_TYPE, transaction.getTransactionType().getName());
            contentValues.put(KEY_TRANSACTIONS_PARTICULARS, transaction.getParticulars());
            contentValues.put(KEY_TRANSACTIONS_AMOUNT, transaction.getAmount());
            Date date = new Date();
            contentValues.put(KEY_TRANSACTIONS_TIMESTAMP, dateFormat.format(date));
            if (transaction.getTransactionType() == Transaction.TransactionType.DR || transaction.getTransactionType() == Transaction.TransactionType.CONTRA) {
                contentValues.put(KEY_TRANSACTIONS_DR_ACCOUNT, transaction.getDebitAccount());
            }
            if (transaction.getTransactionType() == Transaction.TransactionType.CR || transaction.getTransactionType() == Transaction.TransactionType.CONTRA) {
                contentValues.put(KEY_TRANSACTIONS_CR_ACCOUNT, transaction.getCreditAccount());
            }
            sqLiteDatabase.insert(TABLE_TRANSACTIONS, null, contentValues);
            contentValues.clear();
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if(isCreating && currentDB != null){
            return currentDB;
        }
        return super.getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if(isCreating && currentDB != null){
            return currentDB;
        }
        return super.getReadableDatabase();
    }

    public List<Account> getAllAccounts(boolean includeArchivedAccounts) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        List<Account> accounts = new ArrayList<>();
        Cursor cursor;

        if (includeArchivedAccounts) {
            cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_ACCOUNTS + " order by " + KEY_ACCOUNTS_IS_ARCHIVED + ", " + KEY_ACCOUNTS_IS_CREDIT_CARD + ", " + KEY_ACCOUNTS_NAME, null);
        } else {
            cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_ARCHIVED + " = ? order by " + KEY_ACCOUNTS_IS_CREDIT_CARD + ", " + KEY_ACCOUNTS_NAME, new String[]{Integer.toString(0)});
        }

        if (cursor != null) {
            while (cursor.moveToNext()) {
                accounts.add(new Account(
                        cursor.getString(cursor.getColumnIndex(KEY_ACCOUNTS_NAME)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_BALANCE)),
                        cursor.getInt(cursor.getColumnIndex(KEY_ACCOUNTS_IS_CREDIT_CARD)) == 1,
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_CREDIT_LIMIT))
                ));
            }
            cursor.close();
        }
        return accounts;
    }

    public List<Account> getArchivedAccounts() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        List<Account> accounts = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_ARCHIVED + " = ? order by " + KEY_ACCOUNTS_IS_CREDIT_CARD + ", " + KEY_ACCOUNTS_NAME, new String[]{Integer.toString(1)});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                accounts.add(new Account(
                        cursor.getString(cursor.getColumnIndex(KEY_ACCOUNTS_NAME)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_BALANCE)),
                        cursor.getInt(cursor.getColumnIndex(KEY_ACCOUNTS_IS_CREDIT_CARD)) == 1,
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_CREDIT_LIMIT))
                ));
            }
            cursor.close();
        }
        return accounts;
    }

    public List<Transaction> getAllTransactions(String accountName) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor;
        Account account = null;
        if (accountName == null) {
            cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_TRANSACTIONS + " order by " + KEY_TRANSACTIONS_TIMESTAMP + " desc, " + KEY_TRANSACTIONS_ID + " desc", null);
        } else {
            cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_TRANSACTIONS + " where " + KEY_TRANSACTIONS_CR_ACCOUNT + " = ? or " + KEY_TRANSACTIONS_DR_ACCOUNT + " = ? order by " + KEY_TRANSACTIONS_TIMESTAMP + " desc, " + KEY_TRANSACTIONS_ID + " desc", new String[]{accountName, accountName});
            account = DatabaseHelper.mInstance.getAccount(accountName);
        }
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Transaction transaction = new Transaction(
                            cursor.getInt(cursor.getColumnIndex(KEY_TRANSACTIONS_ID)),
                            Transaction.TransactionType.getTransactionType(cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_TYPE))),
                            cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_PARTICULARS)),
                            cursor.getDouble(cursor.getColumnIndex(KEY_TRANSACTIONS_AMOUNT)),
                            dateFormat.parse(cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_TIMESTAMP))),
                            cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_DR_ACCOUNT)),
                            cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_CR_ACCOUNT))
                    );
                    if (account != null) {
                        transaction.setAccountBalance(account.getBalance());

                        if (transaction.getDebitAccount() != null && transaction.getDebitAccount().equals(account.getName())) {
                            account.setBalance(account.getBalance() - transaction.getAmount());
                        } else if (transaction.getCreditAccount() != null && transaction.getCreditAccount().equals(account.getName())) {
                            account.setBalance(account.getBalance() + transaction.getAmount());
                        }
                    }
                    transactions.add(transaction);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return transactions;
    }

    public double getTotalAccountBalance(boolean isCreditCard){
        double total = 0.0;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor;
        if (isCreditCard) {
            cursor = sqLiteDatabase.rawQuery("select sum(" + KEY_ACCOUNTS_BALANCE + ") from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_CREDIT_CARD + " = ? and " + KEY_ACCOUNTS_IS_ARCHIVED + " = ?", new String[]{Integer.toString(1), Integer.toString(0)});
        } else {
            cursor = sqLiteDatabase.rawQuery("select sum(" + KEY_ACCOUNTS_BALANCE + ") from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_ARCHIVED + " = ?", new String[]{Integer.toString(0)});
        }
        if(cursor.moveToFirst()){
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTotalCreditLimit() {
        double total = 0.0;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select sum(" + KEY_ACCOUNTS_CREDIT_LIMIT + ") from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_ARCHIVED + " = ?", new String[]{String.valueOf(0)});
        if(cursor.moveToFirst()){
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public int getAccountsCount(boolean isCreditCard) {
        int count = 0;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor;
        if (isCreditCard){
            cursor = sqLiteDatabase.rawQuery("select count(" + KEY_ACCOUNTS_NAME + ") from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_CREDIT_CARD + " = ?", new String[]{Integer.toString(1)});
        } else {
            cursor = sqLiteDatabase.rawQuery("select count(" + KEY_ACCOUNTS_NAME + ") from " + TABLE_ACCOUNTS, null);
        }
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        return count;
    }

    public int getTransactionsCount(int numberOfDays) {
        int count = 0;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor;
        if (numberOfDays == -1){
            cursor = sqLiteDatabase.rawQuery("select count(" + KEY_TRANSACTIONS_ID + ") from " + TABLE_TRANSACTIONS, null);
        } else {
            cursor = sqLiteDatabase.rawQuery("select count(" + KEY_TRANSACTIONS_ID + ") from " + TABLE_TRANSACTIONS + " where julianday('now') - julianday(" + KEY_TRANSACTIONS_TIMESTAMP + ") <= " + numberOfDays , null);
        }
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        return count;
    }

    public boolean insertAccount(Account account) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ACCOUNTS_NAME, account.getName());
        contentValues.put(KEY_ACCOUNTS_BALANCE, account.getBalance());
        contentValues.put(KEY_ACCOUNTS_IS_CREDIT_CARD, account.isCreditCard() ? 1 : 0);
        contentValues.put(KEY_ACCOUNTS_CREDIT_LIMIT, account.getCreditLimit());
        contentValues.put(KEY_ACCOUNTS_IS_ARCHIVED, account.isArchived() ? 1 : 0);
        return sqLiteDatabase.insert(TABLE_ACCOUNTS, null, contentValues) > 0;
    }

    public boolean insertTransaction(Transaction transaction) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if (transaction.getId() != 0) {
            contentValues.put(KEY_TRANSACTIONS_ID, transaction.getId());
        }
        contentValues.put(KEY_TRANSACTIONS_PARTICULARS, transaction.getParticulars());
        contentValues.put(KEY_TRANSACTIONS_AMOUNT, transaction.getAmount());
        contentValues.put(KEY_TRANSACTIONS_TYPE, transaction.getTransactionType().getName());
        contentValues.put(KEY_TRANSACTIONS_TIMESTAMP, dateFormat.format(transaction.getTimestamp()));
        contentValues.put(KEY_TRANSACTIONS_CR_ACCOUNT, transaction.getCreditAccount());
        contentValues.put(KEY_TRANSACTIONS_DR_ACCOUNT, transaction.getDebitAccount());
        return sqLiteDatabase.insert(TABLE_TRANSACTIONS, null, contentValues) > 0;
    }

    public boolean deleteTransaction(Transaction transaction) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(TABLE_TRANSACTIONS, KEY_TRANSACTIONS_ID + " = ?", new String[]{String.valueOf(transaction.getId())}) > 0;
    }

    public boolean editTransactionParticulars(Transaction transaction, String newParticulars) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_TRANSACTIONS_PARTICULARS, newParticulars);
        return sqLiteDatabase.update(TABLE_TRANSACTIONS, contentValues, KEY_TRANSACTIONS_ID + " = ?", new String[]{String.valueOf(transaction.getId())}) > 0;
    }

    public boolean editTransactionAmount(Transaction transaction, double newAmount) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_TRANSACTIONS_AMOUNT, newAmount);
        return sqLiteDatabase.update(TABLE_TRANSACTIONS, contentValues, KEY_TRANSACTIONS_ID + " = ?", new String[]{String.valueOf(transaction.getId())}) > 0;
    }

    public Account getAccount(String accountName) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select * from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_NAME + " = ?", new String[]{accountName});
        Account account = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                account = new Account(
                        cursor.getString(cursor.getColumnIndex(KEY_ACCOUNTS_NAME)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_BALANCE)),
                        cursor.getInt(cursor.getColumnIndex(KEY_ACCOUNTS_IS_CREDIT_CARD)) == 1,
                        cursor.getDouble(cursor.getColumnIndex(KEY_ACCOUNTS_CREDIT_LIMIT))
                );
            }
            cursor.close();
        }
        return account;
    }

    public boolean editAccountName(Account account, String newName) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ACCOUNTS_NAME, newName);
        return sqLiteDatabase.update(TABLE_ACCOUNTS, contentValues, KEY_ACCOUNTS_NAME + " = ?", new String[]{account.getName()}) > 0;
    }

    public boolean editAccountCreditLimit(Account account, double newCreditLimit) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ACCOUNTS_CREDIT_LIMIT, newCreditLimit);
        return sqLiteDatabase.update(TABLE_ACCOUNTS, contentValues, KEY_ACCOUNTS_NAME + " = ?", new String[]{account.getName()}) > 0;
    }

    public boolean deleteAccount(Account account) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(TABLE_ACCOUNTS, KEY_ACCOUNTS_NAME + " = ?", new String[]{String.valueOf(account.getName())}) > 0;
    }

    public boolean deleteAllAccounts() {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        int accountCount = getAccountsCount(false);
        return sqLiteDatabase.delete(TABLE_ACCOUNTS, null, null) == accountCount;
    }

    public boolean archiveAccount(Account account) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ACCOUNTS_IS_ARCHIVED, 1);
        return sqLiteDatabase.update(TABLE_ACCOUNTS, contentValues, KEY_ACCOUNTS_NAME + " = ?", new String[]{account.getName()}) > 0;
    }

    public boolean unarchiveAccount(Account account) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ACCOUNTS_IS_ARCHIVED, 0);
        return sqLiteDatabase.update(TABLE_ACCOUNTS, contentValues, KEY_ACCOUNTS_NAME + " = ?", new String[]{account.getName()}) > 0;
    }

    public String[] getAllAccountNames() {
        ArrayList<String> accountNames = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select " + KEY_ACCOUNTS_NAME + " from " + TABLE_ACCOUNTS + " where " + KEY_ACCOUNTS_IS_ARCHIVED + " = ? order by " + KEY_ACCOUNTS_NAME, new String[]{String.valueOf(0)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                accountNames.add(cursor.getString(cursor.getColumnIndex(KEY_ACCOUNTS_NAME)).toUpperCase());
            }
            cursor.close();
        }
        String[] array = new String[accountNames.size()];
        return accountNames.toArray(array);
    }

    public HashSet<String> getAllTransactionParticulars() {
        HashSet<String> particulars = new HashSet<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select distinct " + KEY_TRANSACTIONS_PARTICULARS + " from " + TABLE_TRANSACTIONS, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                particulars.add(cursor.getString(cursor.getColumnIndex(KEY_TRANSACTIONS_PARTICULARS)));
            }
            cursor.close();
        }
        return particulars;
    }
}
