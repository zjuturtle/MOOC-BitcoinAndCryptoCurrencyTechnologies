package com.zjuturtle.bct.assignment1;

import java.util.List;
import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        List<Transaction.Input>  inputs  = tx.getInputs();
        List<Transaction.Output> outputs = tx.getOutputs();

        //case 1 check
        for (Transaction.Input next : inputs){
            UTXO tmp = new UTXO(next.prevTxHash, next.outputIndex);
            if (!utxoPool.contains(tmp))
                return false;
        }

        //case 2 check
        int index = 0;
        for (Transaction.Input next : inputs){
            UTXO tmp = new UTXO(next.prevTxHash,next.outputIndex);
            Transaction.Output out = utxoPool.getTxOutput(tmp);
            if(!Crypto.verifySignature(out.address, tx.getRawDataToSign(index), next.signature))
                return false;
            index++;
        }

        //case 3 check
        List<UTXO> seen = new ArrayList<>();
        for (Transaction.Input next : inputs){
            UTXO tmp = new UTXO(next.prevTxHash, next.outputIndex);
            if (seen.contains(tmp))
                return false;
            seen.add(tmp);
        }

        //case 4 check
        double outSum = 0.0;
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) return false;
            outSum += out.value;
        }

        //case 5 check
        double inSum = 0.0;
        for (Transaction.Input next : tx.getInputs()) {
            UTXO tmp = new UTXO(next.prevTxHash, next.outputIndex);
            inSum += utxoPool.getTxOutput(tmp).value;
        }

        if (outSum > inSum)
            return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        int currentValidNum = 0;
        int lastValidNum = 0;
        List<Transaction> res = new ArrayList<>();
        do {
            lastValidNum = currentValidNum;
            for (Transaction next : possibleTxs) {
                if (isValidTx(next)) {
                    for (Transaction.Input in : next.getInputs()) {
                        UTXO del = new UTXO(in.prevTxHash, in.outputIndex);
                        utxoPool.removeUTXO(del);
                    }

                    int index = 0;
                    for (Transaction.Output out : next.getOutputs()) {
                        UTXO add = new UTXO(next.getHash(), index);
                        index++;
                        utxoPool.addUTXO(add, out);
                    }
                    currentValidNum++;
                    res.add(next);
                }
            }
        }while(currentValidNum != lastValidNum);
        Transaction[] resArr = new Transaction[res.size()];
        resArr = res.toArray(resArr);
        return resArr;
    }
}
