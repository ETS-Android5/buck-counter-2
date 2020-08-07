package com.himanshu.buckcounter;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.himanshu.buckcounter.beans.Transaction;
import com.himanshu.buckcounter.business.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class TransactionsActivityFragment extends Fragment {
    TransactionRecyclerViewAdapter mTransactionRecyclerViewAdapter;
    List<Transaction> transactionList;
    Context context;

    public TransactionsActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        // set the adapter
        context = view.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        transactionList = new ArrayList<>();
        transactionList.addAll(DatabaseHelper.getInstance(context).getAllTransactions());
        mTransactionRecyclerViewAdapter = new TransactionRecyclerViewAdapter(transactionList);
        recyclerView.setAdapter(mTransactionRecyclerViewAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        transactionList.clear();
        transactionList.addAll(DatabaseHelper.getInstance(context).getAllTransactions());
        mTransactionRecyclerViewAdapter.notifyDataSetChanged();
    }
}
