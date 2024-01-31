package com.mj.bplustree.impl;

import java.util.ArrayList;

public class BTree {
    private static final int T = 4;
    private Node mRootNode;
    private static final int LEFT_CHILD_NODE = 0;
    private static final int RIGHT_CHILD_NODE = 1;

    class Node {
        public int mNumKeys = 0;
        public int[] mKeys = new int[2 * T - 1];
        public Object[] mObjects = new Object[2 * T - 1];
        public Node[] mChildNodes = new Node[2 * T];
        public boolean mIsLeafNode;

        int binarySearch(int key) {
            int leftIndex = 0;
            int rightIndex = mNumKeys - 1;

            while (leftIndex <= rightIndex) {
                final int middleIndex = leftIndex + ((rightIndex - leftIndex) / 2);
                if (mKeys[middleIndex] < key) {
                    leftIndex = middleIndex + 1;
                } else if (mKeys[middleIndex] > key) {
                    rightIndex = middleIndex - 1;
                } else {
                    return middleIndex;
                }
            }

            return -1;
        }

        boolean contains(int key) {
            return binarySearch(key) != -1;
        }

        void remove(int index, int leftOrRightChild) {
            if (index >= 0) {
                int i;
                for (i = index; i < mNumKeys - 1; i++) {
                    mKeys[i] = mKeys[i + 1];
                    mObjects[i] = mObjects[i + 1];
                    if (!mIsLeafNode) {
                        if (i >= index + leftOrRightChild) {
                            mChildNodes[i] = mChildNodes[i + 1];
                        }
                    }
                }
                mKeys[i] = 0;
                mObjects[i] = null;
                if (!mIsLeafNode) {
                    if (i >= index + leftOrRightChild) {
                        mChildNodes[i] = mChildNodes[i + 1];
                    }
                    mChildNodes[i + 1] = null;
                }
                mNumKeys--;
            }
        }

        void shiftRightByOne() {
            if (!mIsLeafNode) {
                mChildNodes[mNumKeys + 1] = mChildNodes[mNumKeys];
            }
            for (int i = mNumKeys - 1; i >= 0; i--) {
                mKeys[i + 1] = mKeys[i];
                mObjects[i + 1] = mObjects[i];
                if (!mIsLeafNode) {
                    mChildNodes[i + 1] = mChildNodes[i];
                }
            }
        }

        int subtreeRootNodeIndex(int key) {
            for (int i = 0; i < mNumKeys; i++) {
                if (key < mKeys[i]) {
                    return i;
                }
            }
            return mNumKeys;
        }
    }

    public BTree() {
        mRootNode = new Node();
        mRootNode.mIsLeafNode = true;
    }

    public void add(int key, Object object) {
        Node rootNode = mRootNode;
        if (!update(mRootNode, key, object)) {
            if (rootNode.mNumKeys == (2 * T - 1)) {
                Node newRootNode = new Node();
                mRootNode = newRootNode;
                newRootNode.mIsLeafNode = false;
                mRootNode.mChildNodes[0] = rootNode;
                splitChildNode(newRootNode, 0, rootNode);
                insertIntoNonFullNode(newRootNode, key, object);
            } else {
                insertIntoNonFullNode(rootNode, key, object);
            }
        }
    }


    void splitChildNode(Node parentNode, int i, Node node) {
        Node newNode = new Node();
        newNode.mIsLeafNode = node.mIsLeafNode;
        newNode.mNumKeys = T - 1;
        for (int j = 0; j < T - 1; j++) {
            newNode.mKeys[j] = node.mKeys[j + T];
            newNode.mObjects[j] = node.mObjects[j + T];
        }
        if (!newNode.mIsLeafNode) {
            for (int j = 0; j < T; j++) {
                newNode.mChildNodes[j] = node.mChildNodes[j + T];
            }
            for (int j = T; j <= node.mNumKeys; j++) {
                node.mChildNodes[j] = null;
            }
        }
        for (int j = T; j < node.mNumKeys; j++) {
            node.mKeys[j] = 0;
            node.mObjects[j] = null;
        }
        node.mNumKeys = T - 1;


        for (int j = parentNode.mNumKeys; j >= i + 1; j--) {
            parentNode.mChildNodes[j + 1] = parentNode.mChildNodes[j];
        }
        parentNode.mChildNodes[i + 1] = newNode;
        for (int j = parentNode.mNumKeys - 1; j >= i; j--) {
            parentNode.mKeys[j + 1] = parentNode.mKeys[j];
            parentNode.mObjects[j + 1] = parentNode.mObjects[j];
        }
        parentNode.mKeys[i] = node.mKeys[T - 1];
        parentNode.mObjects[i] = node.mObjects[T - 1];
        node.mKeys[T - 1] = 0;
        node.mObjects[T - 1] = null;
        parentNode.mNumKeys++;
    }


    void insertIntoNonFullNode(Node node, int key, Object object) {
        int i = node.mNumKeys - 1;
        if (node.mIsLeafNode) {

            while (i >= 0 && key < node.mKeys[i]) {
                node.mKeys[i + 1] = node.mKeys[i];
                node.mObjects[i + 1] = node.mObjects[i];
                i--;
            }
            i++;
            node.mKeys[i] = key;
            node.mObjects[i] = object;
            node.mNumKeys++;
        } else {

            while (i >= 0 && key < node.mKeys[i]) {
                i--;
            }
            i++;
            if (node.mChildNodes[i].mNumKeys == (2 * T - 1)) {
                splitChildNode(node, i, node.mChildNodes[i]);
                if (key > node.mKeys[i]) {
                    i++;
                }
            }
            insertIntoNonFullNode(node.mChildNodes[i], key, object);
        }
    }

    public void delete(int key) {
        delete(mRootNode, key);
    }

    public void delete(Node node, int key) {
        if (node.mIsLeafNode) {
            int i;
            if ((i = node.binarySearch(key)) != -1) {
                node.remove(i, LEFT_CHILD_NODE);
            }
        } else {
            int i;
            if ((i = node.binarySearch(key)) != -1) {
                Node leftChildNode = node.mChildNodes[i];
                Node rightChildNode = node.mChildNodes[i + 1];
                if (leftChildNode.mNumKeys >= T) {
                    Node predecessorNode = leftChildNode;
                    Node erasureNode = predecessorNode;
                    while (!predecessorNode.mIsLeafNode) {
                        erasureNode = predecessorNode;
                        predecessorNode = predecessorNode.mChildNodes[node.mNumKeys - 1];
                    }
                    node.mKeys[i] = predecessorNode.mKeys[predecessorNode.mNumKeys - 1];
                    node.mObjects[i] = predecessorNode.mObjects[predecessorNode.mNumKeys - 1];
                    delete(erasureNode, node.mKeys[i]);
                } else if (rightChildNode.mNumKeys >= T) {
                    Node successorNode = rightChildNode;
                    Node erasureNode = successorNode;
                    while (!successorNode.mIsLeafNode) {
                        erasureNode = successorNode;
                        successorNode = successorNode.mChildNodes[0];
                    }
                    node.mKeys[i] = successorNode.mKeys[0];
                    node.mObjects[i] = successorNode.mObjects[0];
                    delete(erasureNode, node.mKeys[i]);
                } else {
                    int medianKeyIndex = mergeNodes(leftChildNode, rightChildNode);
                    moveKey(node, i, RIGHT_CHILD_NODE, leftChildNode, medianKeyIndex); // Delete i's right child pointer from node.
                    delete(leftChildNode, key);
                }
            } else {
                i = node.subtreeRootNodeIndex(key);
                Node childNode = node.mChildNodes[i];
                if (childNode.mNumKeys == T - 1) {

                    Node leftChildSibling = (i - 1 >= 0) ? node.mChildNodes[i - 1] : null;
                    Node rightChildSibling = (i + 1 <= node.mNumKeys) ? node.mChildNodes[i + 1] : null;
                    if (leftChildSibling != null && leftChildSibling.mNumKeys >= T) {

                        childNode.shiftRightByOne();
                        childNode.mKeys[0] = node.mKeys[i - 1];
                        childNode.mObjects[0] = node.mObjects[i - 1];
                        if (!childNode.mIsLeafNode) {
                            childNode.mChildNodes[0] = leftChildSibling.mChildNodes[leftChildSibling.mNumKeys];
                        }
                        childNode.mNumKeys++;


                        node.mKeys[i - 1] = leftChildSibling.mKeys[leftChildSibling.mNumKeys - 1];
                        node.mObjects[i - 1] = leftChildSibling.mObjects[leftChildSibling.mNumKeys - 1];


                        leftChildSibling.remove(leftChildSibling.mNumKeys - 1, RIGHT_CHILD_NODE);
                    } else if (rightChildSibling != null && rightChildSibling.mNumKeys >= T) {

                        childNode.mKeys[childNode.mNumKeys] = node.mKeys[i];
                        childNode.mObjects[childNode.mNumKeys] = node.mObjects[i];
                        if (!childNode.mIsLeafNode) {
                            childNode.mChildNodes[childNode.mNumKeys + 1] = rightChildSibling.mChildNodes[0];
                        }
                        childNode.mNumKeys++;


                        node.mKeys[i] = rightChildSibling.mKeys[0];
                        node.mObjects[i] = rightChildSibling.mObjects[0];


                        rightChildSibling.remove(0, LEFT_CHILD_NODE);
                    } else {
                        if (leftChildSibling != null) {
                            int medianKeyIndex = mergeNodes(childNode, leftChildSibling);
                            moveKey(node, i - 1, LEFT_CHILD_NODE, childNode, medianKeyIndex);
                        } else if (rightChildSibling != null) {
                            int medianKeyIndex = mergeNodes(childNode, rightChildSibling);
                            moveKey(node, i, RIGHT_CHILD_NODE, childNode, medianKeyIndex);
                        }
                    }
                }
                delete(childNode, key);
            }
        }
    }


    int mergeNodes(Node dstNode, Node srcNode) {
        int medianKeyIndex;
        if (srcNode.mKeys[0] < dstNode.mKeys[dstNode.mNumKeys - 1]) {
            int i;

            if (!dstNode.mIsLeafNode) {
                dstNode.mChildNodes[srcNode.mNumKeys + dstNode.mNumKeys + 1] = dstNode.mChildNodes[dstNode.mNumKeys];
            }
            for (i = dstNode.mNumKeys; i > 0; i--) {
                dstNode.mKeys[srcNode.mNumKeys + i] = dstNode.mKeys[i - 1];
                dstNode.mObjects[srcNode.mNumKeys + i] = dstNode.mObjects[i - 1];
                if (!dstNode.mIsLeafNode) {
                    dstNode.mChildNodes[srcNode.mNumKeys + i] = dstNode.mChildNodes[i - 1];
                }
            }

            medianKeyIndex = srcNode.mNumKeys;
            dstNode.mKeys[medianKeyIndex] = 0;
            dstNode.mObjects[medianKeyIndex] = null;


            for (i = 0; i < srcNode.mNumKeys; i++) {
                dstNode.mKeys[i] = srcNode.mKeys[i];
                dstNode.mObjects[i] = srcNode.mObjects[i];
                if (!srcNode.mIsLeafNode) {
                    dstNode.mChildNodes[i] = srcNode.mChildNodes[i];
                }
            }
            if (!srcNode.mIsLeafNode) {
                dstNode.mChildNodes[i] = srcNode.mChildNodes[i];
            }
        } else {

            medianKeyIndex = dstNode.mNumKeys;
            dstNode.mKeys[medianKeyIndex] = 0;
            dstNode.mObjects[medianKeyIndex] = null;


            int offset = medianKeyIndex + 1;
            int i;
            for (i = 0; i < srcNode.mNumKeys; i++) {
                dstNode.mKeys[offset + i] = srcNode.mKeys[i];
                dstNode.mObjects[offset + i] = srcNode.mObjects[i];
                if (!srcNode.mIsLeafNode) {
                    dstNode.mChildNodes[offset + i] = srcNode.mChildNodes[i];
                }
            }
            if (!srcNode.mIsLeafNode) {
                dstNode.mChildNodes[offset + i] = srcNode.mChildNodes[i];
            }
        }
        dstNode.mNumKeys += srcNode.mNumKeys;
        return medianKeyIndex;
    }


    void moveKey(Node srcNode, int srcKeyIndex, int childIndex, Node dstNode, int medianKeyIndex) {
        dstNode.mKeys[medianKeyIndex] = srcNode.mKeys[srcKeyIndex];
        dstNode.mObjects[medianKeyIndex] = srcNode.mObjects[srcKeyIndex];
        dstNode.mNumKeys++;

        srcNode.remove(srcKeyIndex, childIndex);

        if (srcNode == mRootNode && srcNode.mNumKeys == 0) {
            mRootNode = dstNode;
        }
    }

    public Object search(int key) {
        return search(mRootNode, key);
    }


    public Object search(Node node, int key) {
        int j = 0;

        while (j < node.mNumKeys && key > node.mKeys[j]) {
            j++;
        }
        if (j < node.mNumKeys && key == node.mKeys[j]) {
            return node.mObjects[j];
        }
        if (node.mIsLeafNode) {
            return null;
        } else {
            return search(node.mChildNodes[j], key);
        }
    }

    public Object search2(int key) {
        return search2(mRootNode, key);
    }

    public Object search2(Node node, int key) {
        while (node != null) {
            int i = 0;
            while (i < node.mNumKeys && key > node.mKeys[i]) {
                i++;
            }
            if (i < node.mNumKeys && key == node.mKeys[i]) {
                return node.mObjects[i];
            }
            if (node.mIsLeafNode) {
                return null;
            } else {
                node = node.mChildNodes[i];
            }
        }
        return null;
    }

    private boolean update(Node node, int key, Object object) {
        while (node != null) {
            int i = 0;
            while (i < node.mNumKeys && key > node.mKeys[i]) {
                i++;
            }
            if (i < node.mNumKeys && key == node.mKeys[i]) {
                node.mObjects[i] = object;
                return true;
            }
            if (node.mIsLeafNode) {
                return false;
            } else {
                node = node.mChildNodes[i];
            }
        }
        return false;
    }


    String printBTree(Node node) {
        StringBuilder string = new StringBuilder();
        if (node != null) {
            if (node.mIsLeafNode) {
                for (int i = 0; i < node.mNumKeys; i++) {
                    string.append(node.mObjects[i]).append(", ");
                }
            } else {
                int i;
                for (i = 0; i < node.mNumKeys; i++) {
                    string.append(printBTree(node.mChildNodes[i]));
                    string.append(node.mObjects[i]).append(", ");
                }
                string.append(printBTree(node.mChildNodes[i]));
            }
        }
        return string.toString();
    }

    public String toString() {
        return printBTree(mRootNode);
    }

    void validate() throws Exception {
        ArrayList<Integer> array = getKeys(mRootNode);
        for (int i = 0; i < array.size() - 1; i++) {
            if (array.get(i) >= array.get(i + 1)) {
                throw new Exception("B-Tree invalid: " + array.get(i) + " greater than " + array.get(i + 1));
            }
        }
    }


    ArrayList<Integer> getKeys(Node node) {
        ArrayList<Integer> array = new ArrayList<>();
        if (node != null) {
            if (node.mIsLeafNode) {
                for (int i = 0; i < node.mNumKeys; i++) {
                    array.add(node.mKeys[i]);
                }
            } else {
                int i;
                for (i = 0; i < node.mNumKeys; i++) {
                    array.addAll(getKeys(node.mChildNodes[i]));
                    array.add(node.mKeys[i]);
                }
                array.addAll(getKeys(node.mChildNodes[i]));
            }
        }
        return array;
    }
}