package com.mj.bplustree;

import com.mj.bplustree.fields.Field;
import com.mj.bplustree.fields.FieldType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;


public class BPlusTreeString {

    public static char[] alpha = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
            'v', 'w', 'x', 'y', 'z'};


    @Test
    public void testCreate() throws IOException {

        try {

            List<Field> tableSpec = new ArrayList<>();
            tableSpec.add(new Field("name", FieldType.string, 10));

            BPlusTree tree = BPlusTree.create(null, "strindex.db",
                    List.of("name"), tableSpec);

            tree.insert(List.of("Apple"));
            tree.insert(List.of("zip"));
            tree.insert(List.of("ball"));
            tree.insert(List.of("Tree"));
            tree.insert(List.of("small"));
            tree.insert(List.of("color"));
            tree.insert(List.of("hammer"));
            tree.insert(List.of("quorum"));
            tree.insert(List.of("dull"));

            tree.isTreeValid();
            tree.printTree();
        } finally {
            File f = new File("strindex.db");
            f.delete();
        }
    }

    @Test
    public void testCreate20() throws IOException {

        try {

            List<Field> tableSpec = new ArrayList<>();
            tableSpec.add(new Field("name", FieldType.string, 10));

            BPlusTree tree = BPlusTree.create(null, "strindex20.db",
                    Arrays.asList("name"), tableSpec);

            for (int i = 1; i <= 20; i++) {
                String t = genRandomWord();
                tree.insert(Arrays.asList(t));
            }

            assertTrue(tree.isTreeValid());
            tree.printTree();

        } finally {
            File f = new File("strindex20.db");
            f.delete();
        }
    }


    @Test
    public void testCreate1000() throws IOException {

        List<Field> tableSpec = new ArrayList<>();
        tableSpec.add(new Field("name", FieldType.string, 10));

        try {

            BPlusTree tree = BPlusTree.create(null, "strindex1000.db",
                    List.of("name"), tableSpec);

            for (int i = 1; i <= 1000; i++) {

                String t = genRandomWord();
                tree.insert(List.of(t));
            }


            assertTrue(tree.isTreeValid());
            tree.printTree();
        } finally {
            File f = new File("strindex1000.db");
            f.delete();
        }

    }


    @Test
    public void find() throws IOException {
        List<Field> tableSpec = new ArrayList<>();
        tableSpec.add(new Field("name", FieldType.string, 10));

        try {
            BPlusTree tree = BPlusTree.create(null, "strindex.db",
                    Arrays.asList("name"), tableSpec);

            tree.insert(List.of("Apple"));
            tree.insert(List.of("zip"));
            tree.insert(List.of("ball"));
            tree.insert(List.of("Tree"));
            tree.insert(List.of("small"));
            tree.insert(List.of("color"));
            tree.insert(List.of("hammer"));
            tree.insert(List.of("quorum"));
            tree.insert(List.of("dull"));

            BPlusTree tree2 = BPlusTree.create(null, "strindex.db",
                    List.of("name"), tableSpec);

            List k = tree2.find(List.of("small"));
            String s = (String) k.get(0);
            assertTrue(s.contentEquals("small"));
        } finally {
            File f = new File("strindex.db");
            f.delete();
        }
    }

    private String genRandomWord() {

        Random r = new Random();

        int size = 0;

        while (size == 0)
            size = r.nextInt(10);

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < size; i++) {

            int next = r.nextInt(25);
            b.append(alpha[next]);

        }

        System.out.println(b);
        return b.toString();
    }

}
