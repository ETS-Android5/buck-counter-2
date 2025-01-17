package com.himanshu.buckcounter;

import static com.himanshu.buckcounter.business.Constants.DECIMAL_FORMAT;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.himanshu.buckcounter.beans.Account;
import com.himanshu.buckcounter.beans.Transaction;
import com.himanshu.buckcounter.business.Constants;
import com.himanshu.buckcounter.business.DatabaseHelper;
import com.himanshu.buckcounter.view.EmptyRecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class TransactionsActivityFragment extends Fragment {
    TransactionRecyclerViewAdapter mTransactionRecyclerViewAdapter;
    List<Transaction> transactionList;
    Context context;
    View view;
    Account account;
    String accountName;

    public TransactionsActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_transactions, container, false);

        accountName = getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null ?
                getActivity().getIntent().getExtras().getString(Constants.BUNDLE_ACCOUNTS_NAME, null) :
                null;
        context = view.getContext();

        // set the adapter
        transactionList = new ArrayList<>();
        transactionList.addAll(DatabaseHelper.getInstance(context).getAllTransactions(accountName));

        if (accountName != null) {
            account = DatabaseHelper.getInstance(context).getAccount(accountName);
            Transaction transaction = new Transaction();
            transaction.setCreditAccount(getString(R.string.balance));
            transaction.setAmount(account.getBalance());

            transactionList.add(0, transaction);
        }

        EmptyRecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setEmptyView(view.findViewById(R.id.empty_list_card));
        mTransactionRecyclerViewAdapter = new TransactionRecyclerViewAdapter(transactionList, context, accountName);
        recyclerView.setAdapter(mTransactionRecyclerViewAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        transactionList.clear();
        transactionList.addAll(DatabaseHelper.getInstance(context).getAllTransactions(accountName));

        if (account != null) {
            Transaction transaction = new Transaction();
            transaction.setCreditAccount(getString(R.string.balance));
            transaction.setAmount(account.getBalance());

            transactionList.add(0, transaction);
        }

        mTransactionRecyclerViewAdapter.notifyDataSetChanged();
    }
}
