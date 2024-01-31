package com.mj.bplustree;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.mj.bplustree.fields.Field;
import com.mj.bplustree.fields.FieldType;
import org.junit.Test;

public class BPlusTreeIntegerTest {

	@Test
	public void testCreate() throws IOException {

		List<Field> tableSpec = new ArrayList<>();
		tableSpec.add(new Field("id", FieldType.integer));

		BPlusTree tree = BPlusTree.create(null, "intindex.db",
				List.of("id"), tableSpec);

		try {

			tree.insert(List.of(4));
			tree.insert(List.of(10));
			tree.insert(List.of(20));
			tree.insert(List.of(30));
			tree.insert(List.of(15));
			tree.insert(List.of(12));
			tree.insert(List.of(17));
			tree.insert(List.of(6));
			tree.insert(List.of(25));
			tree.insert(List.of(6));

			tree.printTree();

			assertTrue(tree.isTreeValid());
		} finally {
			tree.close();
			Files.delete(Paths.get("intindex.db"));
		}
	}

	

	@Test
	public void testInsert100() throws IOException {

		List<Field> tableSpec = new ArrayList<>();
		tableSpec.add(new Field("id", FieldType.integer));

		BPlusTree tree = BPlusTree.create(null, "intindex100.db",
				List.of("id"), tableSpec);
		try {
			for (int i = 1; i <= 100; i++) {
				tree.insert(List.of(i));
			}

			tree.printTree();
			assertTrue(tree.isTreeValid());
		} finally {
			tree.close();
			Files.delete(Paths.get("intindex100.db"));
		}
	}
	

	@Test
	public void testWriteAndValidate1000() throws IOException {

		List<Field> tableSpec = new ArrayList<>();
		tableSpec.add(new Field("id", FieldType.integer));

		BPlusTree tree = BPlusTree.create(null, "intindex1000.db",
				List.of("id"), tableSpec);
		try {

			for (int i = 1; i <= 1000; i++) {
				System.out.println(i);
				tree.insert(List.of(i));
			}

			assertTrue(tree.isTreeValid());
		} finally {
			tree.close();
			Files.delete(Paths.get("intindex1000.db"));
		}
	}

	@Test
	public void testFind() throws IOException {

		List<Field> tableSpec = new ArrayList<>();
		tableSpec.add(new Field("id", FieldType.integer));

		BPlusTree tree = BPlusTree.create(null, "intindex1000.db",
				List.of("id"), tableSpec);

		BPlusTree tree2 = null;
		try {

			for (int i = 1; i <= 1000; i++) {
				tree.insert(List.of(i));
			}
			int l = 153;

			tree2 = BPlusTree.create(null, "intindex1000.db",
					List.of("id"), tableSpec);
			List ptr = tree2.find(List.of(l));
			System.out.println(ptr.get(0));
			assertTrue((int) ptr.get(0) == 153);
		} finally {
			tree.close();
			assert tree2 != null;
			tree2.close();
			Files.delete(Paths.get("intindex1000.db"));
		}
		
	}
	

	@Test
	public void testDelete() throws IOException {
		List<Field> tableSpec = new ArrayList<>();
		tableSpec.add(new Field("id", FieldType.integer));

		BPlusTree tree = BPlusTree.create(null, "intindex100.db",
				List.of("id"), tableSpec);
		BPlusTree tree2 = null ;

		try {

			for (int i = 1; i <= 100; i++) {
				tree.insert(List.of(i));
			}

			tree2 = BPlusTree.create(null, "intindex100.db",
					List.of("id"), tableSpec);

			assertTrue(tree2.isTreeValid());

			tree2.printTree();

			tree2.delete(List.of(73));
			assertTrue(tree2.isTreeValid());
			assertNull(tree2.find(List.of(73)));
		} finally {
			tree.close();
			assert tree2 != null;
			tree2.close();
			Files.delete(Paths.get("intindex100.db"));
		}
	}


}
