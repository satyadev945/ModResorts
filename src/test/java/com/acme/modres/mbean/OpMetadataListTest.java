package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for OpMetadataList class.
 */
public class OpMetadataListTest {

    private OpMetadataList opMetadataList;

    @BeforeEach
    void setUp() {
        opMetadataList = new OpMetadataList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadataList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_singleOpMetadata() {
        OpMetadata op = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_multipleOpMetadata() {
        opMetadataList.add(new OpMetadata("op1", "desc1", "void", 1));
        opMetadataList.add(new OpMetadata("op2", "desc2", "String", 2));
        opMetadataList.add(new OpMetadata("op3", "desc3", "int", 3));
        assertEquals(3, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testGetOpMetadatList_returnsCorrectList() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "String", 2);
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        List<OpMetadata> result = opMetadataList.getOpMetadatList();
        assertEquals(2, result.size());
        assertEquals("op1", result.get(0).getName());
        assertEquals("op2", result.get(1).getName());
    }

    @Test
    void testSetOpMetadatList_replacesExistingList() {
        opMetadataList.add(new OpMetadata("old", "old desc", "void", 0));
        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("new1", "new desc1", "String", 1));
        newList.add(new OpMetadata("new2", "new desc2", "int", 2));
        opMetadataList.setOpMetadatList(newList);
        assertEquals(2, opMetadataList.getOpMetadatList().size());
        assertEquals("new1", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void testSetOpMetadatList_withEmptyList() {
        opMetadataList.add(new OpMetadata("op1", "desc1", "void", 1));
        opMetadataList.setOpMetadatList(new ArrayList<>());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_nullOpMetadata() {
        assertDoesNotThrow(() -> opMetadataList.add(null));
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testSetOpMetadatList_withNull() {
        opMetadataList.setOpMetadatList(null);
        assertNull(opMetadataList.getOpMetadatList());
    }
}
