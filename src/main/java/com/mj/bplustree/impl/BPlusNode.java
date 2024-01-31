package com.mj.bplustree.impl;

import com.mj.bplustree.NodeBounds;
import com.mj.bplustree.fields.Field;
import com.mj.db.serialization.KeySerDeserializer;
import com.mj.db.serialization.RecordSerDeserializer;
import com.mj.util.KeyComparator;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BPlusNode {

    /**
     * Mleaf represent maximum number of entries per leaf node
     * Mnonleaf represent maximum number of keys per non-leaf node
     */
    private int Mleaf;
    private int Mnonleaf;

    /**
     * Represents the address within the disk for the current block.
     * It holds the location of the node's data on the disk.
     */
    private int blockPointer; // address within disk for this block

    /**
     * children Represents the list of pointers to child node blocks in the B+ tree.
     * For a non-leaf node, each entry in this list corresponds to a child node.
     * <p>
     * keys Represents the list of pointers to child node blocks in the B+ tree.
     * For a non-leaf node, each entry in this list corresponds to a child node.
     * <p>
     * data Represents the list of data items stored in the leaf node.
     * For each entry in the list, there is a corresponding key in the keys list.
     */
    private List<Integer> children = new LinkedList<>();

    private List<List<Object>> keys = new LinkedList<>();

    private List<List<Object>> data = new LinkedList<>();

    private boolean isLeaf = false;
    private boolean isRoot = false;
    /**
     * container: Holds a reference to the BPlusTreeImpl object.
     * It provides access to the B+ tree's specifications and methods.
     * <p>
     * nextBlockPointer: Represents the pointer to the next sibling leaf node.
     * It is used in leaf nodes to maintain a linked list of leaf nodes.
     * <p>
     * promotedKey : Temporary storage for the key that gets promoted during a node split operation.
     * <p>
     * promotedChildPointers: Temporary storage for the pointers that get promoted during a node split operation.
     * <p>
     * tableSpec: Represents the specifications of the fields in the table associated with the B+ tree
     * <p>
     * tableSpecMap:Maps field names to their corresponding Field objects in the table specifications.
     * <p>
     * fieldPositionMap:Maps field names to their positions in the table.
     * <p>
     * keySpec:Represents the specifications of the keys in the B+ tree.
     * <p>
     * keySerDeserializer: Handles the serialization and deserialization of keys
     * <p>
     * recordSerDeserializer:Handles the serialization and deserialization of records.
     * <p>
     * keyComparator:Compares keys based on the specified key specifications.
     */
    private BPlusTreeImpl container;
    private int nextBlockPointer;
    private List<Object> promotedKey;
    private int[] promotedChildPointers;

    private List<Field> tableSpec;

    private Map<String, Field> tableSpecMap;

    private Map<String, Integer> fieldPositionMap;

    private List<String> keySpec;

    private KeySerDeserializer keySerDeserializer;
    private RecordSerDeserializer recordSerDeserializer;

    private KeyComparator keyComparator;

    public BPlusNode(BPlusTreeImpl tree) {

        container = tree;
        blockPointer = container.getNextBlockPointer();
        keySpec = tree.getKeySpec();
        tableSpec = tree.getTableSpec();
        tableSpecMap = tree.getTableSpecMap();
        fieldPositionMap = tree.getFieldPositionMap();

        keySerDeserializer = new KeySerDeserializer(tableSpecMap, keySpec);
        recordSerDeserializer = new RecordSerDeserializer(tableSpec);
        keyComparator = new KeyComparator(keySpec, tableSpecMap);

        Mnonleaf = container.getNumKeysPerBlock();
        Mleaf = container.getNumRecordsPerBlock();

    }

    public BPlusNode(BPlusTreeImpl tree, byte[] b, int blockPointer) throws IOException {

        container = tree;
        this.blockPointer = blockPointer;
        keySpec = tree.getKeySpec();
        tableSpec = tree.getTableSpec();
        tableSpecMap = tree.getTableSpecMap();
        fieldPositionMap = tree.getFieldPositionMap();
        keySerDeserializer = new KeySerDeserializer(tableSpecMap, keySpec);
        recordSerDeserializer = new RecordSerDeserializer(tableSpec);
        keyComparator = new KeyComparator(keySpec, tableSpecMap);

        Mnonleaf = container.getNumKeysPerBlock();
        Mleaf = container.getNumRecordsPerBlock();

        if (blockPointer == 0)
            isRoot = true;

        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bis);

        readNode(dis);

    }

    public List<Object> find(List<Object> key) {
        int ptr = -1;
        int kSize;
        if (!isLeaf()) {
            kSize = keys.size();
            int i;
            for (i = 0; i < kSize; i++) {
                List<Object> k = keys.get(i);

                if (keyComparator.compare(key, k) < 0) {
                    ptr = children.get(i);
                    break;
                }
            }
            if (ptr == -1)
                ptr = children.get(i);


            BPlusNode nextNode = readFromDisk(ptr);
            return nextNode.find(key);
        }

        kSize = keys.size();
        int i;
        for (i = 0; i < kSize; i++) {
            List<Object> k = keys.get(i);

            if (keyComparator.compare(key, k) == 0) {
                return data.get(i);
            }
        }

        return null;
    }


    public void delete(List<Object> key) {
        int ptr = -1;
        int kSize;
        if (!isLeaf()) {
            kSize = keys.size();
            int i;
            for (i = 0; i < kSize; i++) {

                List<Object> k = keys.get(i);

                if (keyComparator.compare(key, k) < 0) {
                    ptr = children.get(i);
                    break;
                }
            }

            if (ptr == -1)
                ptr = children.get(i);

            BPlusNode nextNode = readFromDisk(ptr);
            nextNode.delete(key);
            nextNode.writeToDisk();
            return;
        }


        kSize = keys.size();
        int i;
        for (i = 0; i < kSize; i++) {

            List<Object> k = keys.get(i);
            if (keyComparator.compare(key, k) == 0) {
                keys.remove(i);
                data.remove(i);
                break;
            }
        }
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;

        if (isRoot()) {
            blockPointer = 0;
            container.decNextBlockPointer();
        }
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }


    public BPlusNode insert(List<Object> value) {
        List<Object> key = keyFromRecord(value);
        int kSize;
        if (!isLeaf()) {

            int ptr = -1;
            kSize = keys.size();
            int i;
            for (i = 0; i < kSize; i++) {
                List<Object> k = keys.get(i);
                if (keyComparator.compare(key, k) < 0) {
                    ptr = children.get(i);
                    break;
                }
            }

            if (ptr == -1)
                ptr = children.get(i);
            BPlusNode nextNode = readFromDisk(ptr);

            BPlusNode newChild = nextNode.insert(value); // on the way down to the leaf
            nextNode.writeToDisk();

            if (newChild == null) {
                return null;
            }

            List sKey = newChild.getPromotedKey();

            int[] pointers = newChild.getPromotedPointers();
            newChild = insert(sKey, pointers);

            return newChild;
        }
        if (isLeaf()) {
            int size = keys.size();

            boolean foundPos = false;
            for (int i = 0; i < size; i++) {
                List k = keys.get(i);

                if (keyComparator.compare(key, k) < 0) {
                    keys.add(i, key);
                    data.add(i, value);

                    foundPos = true;
                    break;
                } else if (keyComparator.compare(key, k) == 0) {
                    data.set(i, value);
                    foundPos = true;
                    break;
                }
            }
            if (!foundPos) {
                keys.add(key);
                data.add(value);
            }

            size = keys.size();
            if (size <= Mleaf) {
                writeToDisk();
                return null;
            }

            BPlusNode newNode = new BPlusNode(container);
            newNode.setLeaf(true);

            int s_half_b = Mleaf / 2;

            int s_half_e = size - 1;

            for (int i = s_half_b; i <= s_half_e; i++) {
                var lKey = getKey(i);
                var lData = getData(i);
                newNode.insert(lData);
            }
            var promotedKey = getKey(s_half_b);

            for (int i = s_half_e; i >= s_half_b; i--) {
                removeKey(i);
                removeData(i);
            }
            nextBlockPointer = newNode.getPointer();
            newNode.setPromotedKey(promotedKey);

            int[] promotedPointers = new int[2];
            promotedPointers[0] = getPointer();
            promotedPointers[1] = newNode.getPointer();
            newNode.setPromotedPointers(promotedPointers);

            writeToDisk();
            newNode.writeToDisk();
            return newNode;
        }
        return null;
    }


    public BPlusNode insert(List<Object> key, int[] blockPointer) {
        if (isLeaf()) {
            throw new RuntimeException("Method Applies only to Non Leaf nodes");
        }

        int size = keys.size();
        boolean foundPos = false;
        for (int i = 0; i < size; i++) {

            List<Object> k = keys.get(i);

            if (keyComparator.compare(key, k) < 0) {
                keys.add(i, key);
                children.add(i + 1, blockPointer[1]);
                foundPos = true;
                break;

            } else if (keyComparator.compare(key, k) == 0) {
                children.set(i + 1, blockPointer[1]);
                foundPos = true;
                break;
            }
        }
        if (!foundPos) {
            keys.add(size, key);
            children.add(size + 1, blockPointer[1]);
        }

        size = keys.size();
        if (size <= Mnonleaf) {
            writeToDisk();
            return null;
        }
        BPlusNode newNode = new BPlusNode(container);
        newNode.setLeaf(false);

        int s_half_b = Mnonleaf / 2;
        int s_half_e = size - 1;

        for (int i = s_half_b + 1; i <= s_half_e; i++) {
            List lKey = getKey(i);
            newNode.appendKey(lKey);
        }

        for (int i = s_half_e; i >= s_half_b + 1; i--) {
            removeKey(i);
        }

        for (int i = s_half_b + 1; i <= s_half_e + 1; i++) {
            newNode.appendChildPtr(getChildPtr(i));
        }

        for (int i = s_half_e + 1; i >= s_half_b + 1; i--) {
            removeChildPtr(i);
        }

        int middleIndex = s_half_b;
        newNode.setPromotedKey(keys.get(middleIndex));

        int[] promotedPointers = new int[2];
        promotedPointers[0] = getChildPtr(middleIndex);
        promotedPointers[1] = newNode.getPointer();
        newNode.setPromotedPointers(promotedPointers);

        keys.remove(middleIndex);
        newNode.writeToDisk();
        return newNode;
    }

    public Object getSmallestKey() {
        return keys.get(0);
    }

    public int getPointer() {
        return blockPointer;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public List<NodeBounds> getChildrenNodeBounds() {
        List<NodeBounds> clist = new ArrayList<>();
        int numChildren = children.size();

        for (int i = 0; i < numChildren; i++) {

            List<Object> low = new ArrayList<>();
            List<Object> high = new ArrayList<>();
            int ptr;

            if (i == 0) {
                high = keys.get(i);
            } else if (i == numChildren - 1) {
                low = keys.get(i - 1);
            } else {
                low = keys.get(i - 1);
                high = keys.get(i);
            }
            ptr = children.get(i);

            NodeBounds n = new NodeBounds(low, high, ptr);
            clist.add(n);
        }
        return clist;
    }

    public void addChildren(BPlusNode oldRoot, BPlusNode newChild) {


        if (!isRoot)
            throw new RuntimeException("Method should be called for root only!");
        List key = newChild.getPromotedKey();

        appendKey(key);
        int[] promotedPointers = newChild.getPromotedPointers();

        appendChildPtr(oldRoot.getPointer());
        appendChildPtr(promotedPointers[1]);
    }

    public List<Object> getKey(int index) {
        return keys.get(index);
    }

    public List get(List key) {

        int index = 0;
        for (List k : keys) {
            if (keyComparator.compare(k, key) == 0)
                break;
            ++index;
        }
        if (index == 10)
            return null;

        return data.get(index);
    }

    private void removeKey(int index) {
        keys.remove(index);
    }

    public int getChildPtr(int index) {
        return children.get(index);
    }

    private void removeChildPtr(int index) {
        children.remove(index);
    }

    public List<Object> getData(int index) {
        return data.get(index);
    }

    private void removeData(int index) {
        data.remove(index);
    }

    public void setPromotedKey(List<Object> key) {
        promotedKey = key;
    }

    public List<Object> getPromotedKey() {
        return promotedKey;
    }

    public void setPromotedPointers(int[] pointers) {
        promotedChildPointers = pointers;
    }

    public int[] getPromotedPointers() {
        return promotedChildPointers;
    }

    public void appendKey(List<Object> key) {
        keys.add(key);
    }

    public void appendChildPtr(int p) {
        children.add(p);
    }

    public void writeToDisk() {

        try {
            container.writeToDisk(this);
        } catch (IOException e) {
            // throw a domain exception
        }

    }

    private void readNode(DataInputStream ds) throws IOException {

        byte type = ds.readByte();

        if (type == 1) {
            this.isLeaf = true;
        } else if (type == 0) {
            this.isLeaf = false;
        } else {
            throw new RuntimeException("first byte of the block is not 0 or 1. Invalid");
        }
        if (isLeaf) {
            readLeaf(ds);
        } else {
            readNonLeaf(ds);
        }
    }

    private void readLeaf(DataInputStream ds) throws IOException {

        int numItems = ds.readInt();

        for (int i = 0; i < numItems; i++) {

            List<Object> dataItem = recordSerDeserializer.read(ds);
            data.add(dataItem);
            List<Object> key = keyFromRecord(dataItem);
            keys.add(key);
        }
        this.nextBlockPointer = ds.readInt();
    }

    private List<Object> keyFromRecord(List<Object> dataItem) {
        List<Object> ret = new ArrayList<>();
        for (String fieldName : keySpec) {
            Object val = dataItem.get(fieldPositionMap.get(fieldName));
            ret.add(val);
        }
        return ret;
    }

    public void writeLeaf(DataOutputStream ds) throws IOException {
        ds.writeByte(1);

        ds.writeByte(1);
        int num = data.size();
        ds.writeInt(num);

        for (int i = 0; i < num; i++) {
            List<Object> val = data.get(i);
            recordSerDeserializer.write(val, ds);
        }
        ds.writeInt(nextBlockPointer);
    }

    private void readNonLeaf(DataInputStream ds) throws IOException {

        int numKeys = ds.readInt();

        for (int i = 0; i < numKeys; i++) {
            List<Object> key = keySerDeserializer.read(ds);
            keys.add(key);
        }

        int numChildPointers = ds.readInt();

        for (int i = 0; i < numChildPointers; i++) {
            int ptr = ds.readInt();
            children.add(ptr);
        }
    }

    public void writeNonLeaf(DataOutputStream ds) throws IOException {
        ds.writeByte(1);

        ds.writeByte(0);

        int num_keys = keys.size();

        ds.writeInt(num_keys);

        for (int i = 0; i < num_keys; i++) {
            List <Object>val = keys.get(i);
            keySerDeserializer.write(val, ds);
        }
        int numChildPointers = children.size();
        ds.writeInt(numChildPointers);

        for (int i = 0; i < numChildPointers; i++) {
            Integer ptr = children.get(i);
            ds.writeInt(ptr.intValue());
        }
    }


    public void moveBlock() {
        blockPointer = container.getNextBlockPointer();
        writeToDisk();

    }
    public BPlusNode readFromDisk(int pointer) {

        try {
            return container.readFromDisk(pointer);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }


    public boolean isNodeValid(List low, List high) {

        for (int i = 0; i < keys.size(); i++) {
            List<Object> key = keys.get(i);
            if (keyComparator.compare(low, key) <= 0 && keyComparator.compare(key, high) <= 0) {
                continue;
            }
            else {
                System.out.println("Invalid node: low =" + low + " high=" + high + "key =" + key);
                printNode();
                return false;
            }
        }
        return true;
    }


    public void printNode() {

        System.out.println("---- Begin Node");

        System.out.println("BlockPointer : " + blockPointer);

        if (isRoot)
            System.out.println("Root \n");

        if (isLeaf) {
            System.out.println("Leaf ");
            System.out.println("data : " + keys.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.size(); i++) {
                sb.append(data.get(i)).append(",");
            }
            System.out.println(sb);
            System.out.println("--- End Node ");

            return;
        }
        else {
            System.out.println("Non leaf \n");
            System.out.println("Keys: " + keys.size() + "keys \n");

            StringBuilder kb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                kb.append(keys.get(i));
                kb.append(',');
            }

            System.out.println(kb.toString());

            System.out.println("Child Pointers: \n");

            StringBuilder cb = new StringBuilder();
            for (int i = 0; i < children.size(); i++) {
                cb.append(children.get(i));
                cb.append(',');
            }
            System.out.println(cb);
        }
        System.out.println("--- End Node ");

        return;
    }

    // @Override
    public int compare(Object arg0, Object arg1) {
        return 0;
    }

}
