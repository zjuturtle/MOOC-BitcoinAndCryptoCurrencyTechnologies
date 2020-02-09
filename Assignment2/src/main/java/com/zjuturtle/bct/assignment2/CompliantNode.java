package com.zjuturtle.bct.assignment2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    private double p_graph;
    private double p_malicious;
    private double p_txDistribution;
    private int    totalRounds;
    private int    currentRound;

    //transaction we trusted, map value is the times we heard
    private Set<Transaction> txPool;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.totalRounds = numRounds;
        this.currentRound = 0;
        this.txPool = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {

    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        for(Transaction item : pendingTransactions) {
            txPool.add(item);
        }
    }

    public Set<Transaction> sendToFollowers() {
        return txPool;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate item: candidates) {
            txPool.add(item.tx);
        }
    }
}
