package com.mj.bplustree.impl;

import com.mj.bplustree.BPlusTree;
import com.mj.bplustree.NodeBounds;
import com.mj.bplustree.fields.Field;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BPlusTreeImpl implements BPlusTree {

    private final int BLOCK_SIZE = 1024;
    private final int VALUE_SIZE = 8;
    private int keySize;
    private int recordSize;
    private RandomAccessFile treeStore;
    private BPlusNode root = null;
    private int nextBlockPointer = 0;
    private boolean newTree = false;

    private int MLeaf = 0;
    private int MNonLeaf = 0;

    private List<String> keySpec = null;
    private List<Field> tableSpec;
    private Map<String, Field> tableSpecMap = new HashMap<>();
    private Map<String, Integer> fieldPositionMap = new HashMap<>();

    private boolean isClustered = false;

    public BPlusTreeImpl(String storeDir, String filename,
                         List<String> keySpec, List<Field> tableSpec) throws IOException {

        this.keySpec = keySpec;
        this.tableSpec = tableSpec;

        int i = 0;
        for (Field f : tableSpec) {
            tableSpecMap.put(f.getName(), f);
            fieldPositionMap.put(f.getName(), i);
            i++;
        }

        String fName;

        if (storeDir != null && storeDir.length() > 0) {
            fName = storeDir + "\\" + filename;
        } else {
            fName = filename;
        }

        File f = new File(fName);

        newTree = !f.exists();

        keySize = calcKeySize();
        System.out.println("keySize is " + keySize);
        recordSize = calcRecordSize();
        System.out.println("recordSize is " + recordSize);

        MLeaf = (BLOCK_SIZE - 14) / (recordSize);
        MNonLeaf = (BLOCK_SIZE - 14) / (recordSize);
        System.out.println("MLeaf is " + MLeaf);
        System.out.println("MNonLeaf is " + MLeaf);

        treeStore = new RandomAccessFile(fName, "rw");
        load();

    }

    private int calcRecordSize() {
        int size = 0;
        for (Field f : tableSpec) {
            size = size + f.getSize();
        }
        return size;
    }

    private int calcKeySize() {
        int size = 0;
        for (String s : keySpec) {
            size = size + tableSpecMap.get(s).getSize();
        }
        return size;
    }


    public List<String> getKeySpec() {
        return keySpec;
    }

    public List<Field> getTableSpec() {
        return tableSpec;
    }

    public Map<String, Field> getTableSpecMap() {
        return tableSpecMap;
    }

    public Map<String, Integer> getFieldPositionMap() {
        return fieldPositionMap;
    }

    protected int getNumKeysPerBlock() {
        return MNonLeaf;
    }

    protected int getNumRecordsPerBlock() {
        return MLeaf;
    }


    @Override
    public List find(List key) {
        return root.find(key);
    }

    public byte[] getNext() {
        return null;
    }


    @Override
    public void insert(List value) {
		BPlusNode node;

		if (root == null) {
			node = new BPlusNode(this);
			root = node;
			node.setRoot(true);
			node.setLeaf(true);
		}

		BPlusNode newChild = root.insert(value);
		if (newChild != null) {

			BPlusNode newNode = new BPlusNode(this);
			newNode.setRoot(true);
			newNode.setLeaf(false);
			root.setRoot(false);
			root.moveBlock();
			newNode.addChildren(root, newChild);
			newNode.writeToDisk();
			root = newNode;
		}
	}


	@Override
    public void delete(List key) {
		root.delete(key);
    }

    public BPlusNode readFromDisk(int blockPointer) throws IOException {
		treeStore.seek((long) blockPointer * BLOCK_SIZE);

        int freeOrNot = treeStore.readByte();

        if (freeOrNot == 1) {
			byte[] b = new byte[BLOCK_SIZE - 1];
			treeStore.readFully(b, 0, BLOCK_SIZE - 1);
			return new BPlusNode(this, b, blockPointer);
        }
        return null;
    }

    public void writeToDisk(BPlusNode node) throws IOException {
		int pointer = node.getPointer();
        treeStore.seek((long) pointer * BLOCK_SIZE);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(BLOCK_SIZE);
        DataOutputStream ds = new DataOutputStream(bos);

        if (node.isLeaf()) {
			node.writeLeaf(ds);
		}
        else {
			node.writeNonLeaf(ds);
		}
        int recSize = bos.size();
        int fill = BLOCK_SIZE - recSize;

        for (int i = 1; i <= fill; i++) {
			bos.write(0);
        }

        byte[] record = bos.toByteArray();

        if (record.length != BLOCK_SIZE) {
            node.printNode();
            throw new RuntimeException("rec size= " + record.length + " block size=" + BLOCK_SIZE);
        }

		treeStore.write(record);
    }


    public long getFilePointer() throws IOException {
		return treeStore.getFilePointer();
    }

    public int getNextBlockPointer() {
		int next = nextBlockPointer;
        nextBlockPointer++;
        return next;
    }

    public void decNextBlockPointer() {
		if (nextBlockPointer > 1)
            nextBlockPointer--;
    }

    public boolean isTreeValid() throws IOException {
		if (root == null) {
            System.out.println("Root is null. Tree is empty");
            return true;
        }
		ArrayDeque<NodeBounds> queue = new ArrayDeque<>(root.getChildrenNodeBounds());
        NodeBounds current;
        while (!queue.isEmpty() && (current = queue.poll()) != null) {
			BPlusNode cNode = readFromDisk(current.blockPointer);

			if (cNode.isNodeValid(current.low, current.high)) {
				queue.addAll(cNode.getChildrenNodeBounds());
			}
            else {
				return false;
			}
        }
		return true;
    }
	@Override
    public void printTree() throws IOException {
		if (root == null) {
            System.out.println("Root is null. Tree is empty");
            return;
        }
		root.printNode();
		ArrayDeque<Integer> queue = new ArrayDeque<>(root.getChildren());

        Integer current;
        while (!queue.isEmpty() && (current = queue.poll()) != null) {
			BPlusNode cNode = readFromDisk(current);
            cNode.printNode();
            queue.addAll(cNode.getChildren());
        }
    }

    private void load() throws IOException {

        try {
			long filesize = treeStore.length();
            int numBlocks = (int) filesize / this.BLOCK_SIZE;

            nextBlockPointer = numBlocks;
            root = readFromDisk(0);

        } catch (EOFException e) {
            root = null;
        }
    }

    public void close() throws IOException {
        treeStore.close();
    }

}