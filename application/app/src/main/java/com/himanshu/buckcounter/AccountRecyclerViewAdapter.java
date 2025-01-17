package com.himanshu.buckcounter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.himanshu.buckcounter.beans.Account;
import com.himanshu.buckcounter.business.Constants;
import com.himanshu.buckcounter.business.DatabaseHelper;

import java.util.List;

import static com.himanshu.buckcounter.business.Constants.DECIMAL_FORMAT;
import static com.himanshu.buckcounter.business.Constants.VALID_AMOUNT_REGEX;
import static com.himanshu.buckcounter.business.Constants.VALID_TEXT_REGEX;

public class AccountRecyclerViewAdapter extends RecyclerView.Adapter<AccountRecyclerViewAdapter.ViewHolder> {
    private final List<Account> mValues;
    private Context context;
    private boolean isArchivedAccountsList;

    public AccountRecyclerViewAdapter(List<Account> items, Context context, boolean isArchivedAccountsList) {
        mValues = items;
        this.context = context;
        this.isArchivedAccountsList = isArchivedAccountsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Account account = mValues.get(position);
        holder.mItem = account;
        holder.mAccountName.setText(account.getName());
        holder.mAccountBalance.setText(DECIMAL_FORMAT.format(account.getBalance()));
        if (position == 0 && !isArchivedAccountsList) {
            holder.mAccountContextMenu.setVisibility(View.GONE);
            holder.mAccountIcon.setVisibility(View.GONE);
            holder.mAccountName.setTypeface(null, Typeface.BOLD);
        } else {
            holder.mAccountIcon.setVisibility(View.VISIBLE);
            holder.mAccountName.setTypeface(null, Typeface.NORMAL);
            holder.mAccountContextMenu.setVisibility(View.VISIBLE);
            holder.mAccountContextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(holder.mAccountContextMenu, account, position);
                }
            });
        }
        if (account.isCreditCard()) {
            ((TextView)holder.mCreditCardDetails.findViewById(R.id.credit_limit)).setText(DECIMAL_FORMAT.format(account.getCreditLimit()));
            ((TextView)holder.mCreditCardDetails.findViewById(R.id.remaining_credit_limit)).setText(DECIMAL_FORMAT.format(account.getCreditLimit() + account.getBalance()));
            holder.mCreditCardDetails.setVisibility(View.VISIBLE);
            holder.mAccountIcon.setImageResource(R.mipmap.credit_card);
        } else {
            holder.mCreditCardDetails.setVisibility(View.GONE);
            holder.mAccountIcon.setImageResource(R.mipmap.account);
        }
        if (position != 0 && !isArchivedAccountsList) {
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, TransactionsActivity.class);
                    intent.putExtra(Constants.BUNDLE_ACCOUNTS_NAME, account.getName());
                    context.startActivity(intent);
                }
            });
        } else {
            holder.mView.setOnClickListener(null);
        }
    }

    private void showPopupMenu(View view, final Account account, final int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        Menu menu = popupMenu.getMenu();
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        if (isArchivedAccountsList) {
            menuInflater.inflate(R.menu.archived_account_context_menu, menu);
        } else {
            menuInflater.inflate(R.menu.account_context_menu, menu);
            if (account.isCreditCard()) {
                menu.findItem(R.id.edit_account_credit_limit).setVisible(true);
            }
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.edit_account_name:
                        final AlertDialog editName = new AlertDialog.Builder(context)
                                .setIcon(R.mipmap.ic_launcher_round)
                                .setTitle(R.string.edit_account_name)
                                .setView(R.layout.edit_account_name)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, null)
                                .create();
                        editName.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                final EditText accountName = editName.findViewById(R.id.edit_account_name);
                                accountName.setText(account.getName());
                                accountName.selectAll();
                                editName.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (accountName.getText() == null || accountName.getText().toString().isEmpty() || !accountName.getText().toString().trim().toLowerCase().matches(VALID_TEXT_REGEX)) {
                                            ((TextInputLayout) editName.findViewById(R.id.edit_account_name_container)).setError(context.getText(R.string.add_account_name_error));
                                            return;
                                        } else {
                                            ((TextInputLayout) editName.findViewById(R.id.edit_account_name_container)).setErrorEnabled(false);
                                        }
                                        String newName = accountName.getText().toString().trim().toLowerCase();
                                        if (newName.equals(account.getName())) {
                                            editName.dismiss();
                                            return;
                                        }
                                        boolean editNameSuccessful = false;
                                        try {
                                            editNameSuccessful = DatabaseHelper.getInstance(context).editAccountName(account, newName);
                                        } catch (SQLiteConstraintException e) {
                                            Toast.makeText(context, "Account name must be unique", Toast.LENGTH_LONG).show();
                                        }
                                        if (editNameSuccessful) {
                                            mValues.get(position).setName(newName);
                                            AccountRecyclerViewAdapter.this.notifyItemChanged(position);
                                        }
                                        editName.dismiss();
                                    }
                                });
                            }
                        });
                        editName.show();
                        return true;
                    case R.id.edit_account_credit_limit:
                        final AlertDialog editCreditLimit = new AlertDialog.Builder(context)
                                .setIcon(R.mipmap.ic_launcher_round)
                                .setTitle(R.string.edit_account_credit_limit)
                                .setView(R.layout.edit_account_credit_limit)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, null)
                                .create();
                        editCreditLimit.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                final EditText accountCreditLimit = editCreditLimit.findViewById(R.id.edit_credit_limit);
                                accountCreditLimit.setText(String.valueOf(account.getCreditLimit()));
                                accountCreditLimit.selectAll();
                                editCreditLimit.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (accountCreditLimit.getText() == null || accountCreditLimit.getText().toString().isEmpty() || !accountCreditLimit.getText().toString().matches(VALID_AMOUNT_REGEX)) {
                                            ((TextInputLayout)editCreditLimit.findViewById(R.id.edit_credit_limit_container)).setError(context.getText(R.string.add_credit_limit_error));
                                            return;
                                        } else {
                                            ((TextInputLayout)editCreditLimit.findViewById(R.id.edit_credit_limit_container)).setErrorEnabled(false);
                                        }
                                        double newCreditLimit = Double.parseDouble(accountCreditLimit.getText().toString().trim().toLowerCase());
                                        if (newCreditLimit == account.getCreditLimit()) {
                                            editCreditLimit.dismiss();
                                            return;
                                        }
                                        boolean editCreditLimitSuccessful = DatabaseHelper.getInstance(context).editAccountCreditLimit(account, newCreditLimit);
                                        if (editCreditLimitSuccessful) {
                                            mValues.get(position).setCreditLimit(newCreditLimit);
                                            AccountRecyclerViewAdapter.this.notifyItemChanged(position);
                                        }
                                        editCreditLimit.dismiss();
                                    }
                                });
                            }
                        });
                        editCreditLimit.show();
                        return true;
                    case R.id.archive_account:
                        boolean accountArchivedSuccessfully = DatabaseHelper.getInstance(context).archiveAccount(account);
                        if (accountArchivedSuccessfully) {
                            mValues.remove(position);
                            AccountRecyclerViewAdapter.this.notifyItemRemoved(position);
                            AccountRecyclerViewAdapter.this.notifyItemRangeChanged(position, mValues.size());
                            mValues.get(0).setBalance(DatabaseHelper.getInstance(context).getTotalAccountBalance(false));
                            AccountRecyclerViewAdapter.this.notifyItemChanged(0);
                        }
                        return true;
                    case R.id.delete_account:
                        AlertDialog deleteAccount = new AlertDialog.Builder(context)
                                .setIcon(R.mipmap.ic_launcher_round)
                                .setTitle(R.string.delete_account)
                                .setMessage(R.string.delete_account_confirm)
                                .setNegativeButton(android.R.string.no, null)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        boolean accountDeletedSuccessfully = DatabaseHelper.getInstance(context).deleteAccount(account);
                                        if (accountDeletedSuccessfully) {
                                            mValues.remove(position);
                                            AccountRecyclerViewAdapter.this.notifyItemRemoved(position);
                                            AccountRecyclerViewAdapter.this.notifyItemRangeChanged(position, mValues.size());
                                            mValues.get(0).setBalance(DatabaseHelper.getInstance(context).getTotalAccountBalance(false));
                                            AccountRecyclerViewAdapter.this.notifyItemChanged(0);
                                        }
                                    }
                                })
                                .create();
                        deleteAccount.show();

                        return true;
                    case R.id.unarchive_account:
                        boolean accountUnarchivedSuccessfully = DatabaseHelper.getInstance(context).unarchiveAccount(account);

                        if (accountUnarchivedSuccessfully) {
                            mValues.remove(position);
                            AccountRecyclerViewAdapter.this.notifyItemRemoved(position);
                            AccountRecyclerViewAdapter.this.notifyItemRangeChanged(position, mValues.size());
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mAccountName;
        public final TextView mAccountBalance;
        public final ImageView mAccountIcon;
        public final ImageView mAccountContextMenu;
        public final View mCreditCardDetails;
        public Account mItem;

        public ViewHolder(@NonNull View view) {
            super(view);
            mView = view;
            mAccountName = view.findViewById(R.id.account_name);
            mAccountBalance = view.findViewById(R.id.account_balance);
            mAccountIcon = view.findViewById(R.id.account_icon);
            mAccountContextMenu = view.findViewById(R.id.account_context_menu);
            mCreditCardDetails = view.findViewById(R.id.credit_card_details);
        }
    }
}
