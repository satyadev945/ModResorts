package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OpMetadataListTest {

    @Test
    void testAddOpMetadata() {
        OpMetadataList list = new OpMetadataList();
        OpMetadata op = new OpMetadata("Name", "Desc", "Type", 1);
        list.add(op);
        
        assertEquals(1, list.getOpMetadatList().size());
        assertEquals(op, list.getOpMetadatList().get(0));
    }

    @Test
    void testSetOpMetadatList() {
        OpMetadataList list = new OpMetadataList();
        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("N1", "D1", "T1", 1));
        newList.add(new OpMetadata("N2", "D2", "T2", 2));
        
        list.setOpMetadatList(newList);
        
        assertEquals(2, list.getOpMetadatList().size());
        assertEquals("N1", list.getOpMetadatList().get(0).getName());
    }
}
