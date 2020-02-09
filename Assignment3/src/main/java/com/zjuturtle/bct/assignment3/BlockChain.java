package com.zjuturtle.bct.assignment3;
// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

import static java.lang.Integer.max;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;


    class BlockNode{
        Block block;
        UTXOPool pool;
        LinkedList<BlockNode> nextNodes = new LinkedList<>();
        BlockNode(Block block, UTXOPool pool){
            this.block = block;
            this.pool = pool;
        }
    }

    BlockNode root;
    private TransactionPool tPool = new TransactionPool();

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        root = new BlockNode(genesisBlock, new UTXOPool());

        Transaction coinBase = genesisBlock.getCoinbase();
        Transaction.Output output = coinBase.getOutputs().get(0);
        root.pool.addUTXO(new UTXO(coinBase.getHash(), 0), output);
    }

    class MaxHeightSearcher {
        BlockNode resNode;
        int resHeight;

        void maxBFS(BlockNode node) {
            resNode = node;
            resHeight = 0;
            maxBFS_(node, 0);
        }

        private void maxBFS_(BlockNode node, int currentHeight) {
            if (node.nextNodes.isEmpty()) {
                if (resHeight <= currentHeight) {
                    resHeight = currentHeight;
                    resNode = node;
                }
                return;
            }
            for (BlockNode n : node.nextNodes) {
                maxBFS_(n, currentHeight + 1);
            }
        }
    }

    /** Get the maximum height block */
    //BFS search
    public Block getMaxHeightBlock() {
        MaxHeightSearcher ms = new MaxHeightSearcher();
        ms.maxBFS(root);
        return ms.resNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        MaxHeightSearcher ms = new MaxHeightSearcher();
        ms.maxBFS(root);
        return ms.resNode.pool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return this.tPool;
    }

    class PreNodeSearcher{
        BlockNode resNode;
        int resHeight;
        int maxHeight;
        byte[] targetHash;

        void bfs(BlockNode node, byte[] targetHash) {
            resNode = null;
            resHeight = 0;
            maxHeight = 0;
            this.targetHash = targetHash;
            bfs_(node, 0);
        }

        private void bfs_(BlockNode start, int currentHeight){
            if (maxHeight < currentHeight) maxHeight = currentHeight;

            if(Arrays.equals(start.block.getHash(), targetHash)){
                resNode = start;
                resHeight = currentHeight;
                return;
            }

            for(BlockNode node: start.nextNodes){
                bfs_(node, currentHeight+1);
            }
        }
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        //deny genesisBlock
        if(null == block.getPrevBlockHash()) return false;

        //find pre
        PreNodeSearcher searcher = new PreNodeSearcher();
        searcher.bfs(root, block.getPrevBlockHash());
        if (null == searcher.resNode) return false;
        BlockNode preBlockNode = searcher.resNode;

        //check height
        if (searcher.resHeight+1 <= (searcher.maxHeight - CUT_OFF_AGE)) return false;

        //check transaction valid
        TxHandler txHandler = new TxHandler(preBlockNode.pool);
        Transaction[] tmp = new Transaction[block.getTransactions().size()];
        block.getTransactions().toArray(tmp);
        Transaction[] remainTx = txHandler.handleTxs(tmp);
        if (remainTx.length != tmp.length) return false;

        //add coinbase
        UTXOPool pool = txHandler.getUTXOPool();
        Transaction coinBase = block.getCoinbase();
        Transaction.Output output = coinBase.getOutputs().get(0);
        pool.addUTXO(new UTXO(coinBase.getHash(), 0), output);

        //create new BlockNode and insert
        preBlockNode.nextNodes.add(new BlockNode(block, pool));

        for(Transaction tx: block.getTransactions()) {
            tPool.removeTransaction(tx.getHash());
        }

        //add success, clean BlockNode chain(for some forks too old)
        ChainCleaner cleaner = new ChainCleaner();
        root = cleaner.clean(root);
        return true;
    }

    class ChainCleaner{
        BlockNode clean(BlockNode root){
            int maxHeight=0;
            ArrayList<BlockNode> mainFork=new ArrayList<>();
            for(BlockNode n:root.nextNodes){
                int subNodeHeight = getHeight(n);
                if(maxHeight < subNodeHeight+1) {
                    mainFork.clear();
                    mainFork.add(n);
                    maxHeight = subNodeHeight+1;
                    break;
                }
                if(maxHeight == subNodeHeight+1)
                    mainFork.add(n);
            }

            if(maxHeight > CUT_OFF_AGE && mainFork.size() == 1) {
                return mainFork.get(0);
            } else {
                return root;
            }
        }

        private int getHeight(BlockNode node){
            if(node.nextNodes.isEmpty())
                return 0;
            int res = 0;
            for(BlockNode n:node.nextNodes){
                res = max(getHeight(n)+1, res);
            }
            return res;
        }
    }


    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.tPool.addTransaction(tx);
    }
}