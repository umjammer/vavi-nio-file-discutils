/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RawAcl.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public class RawAcl implements Iterable<GenericAce> {

    int revision;

    /**
     * @param revision
     * @param size
     */
    public RawAcl(int revision, int size) {
        this.revision = revision;
    }

    /**
     * @return
     */
    public int getRevision() {
        return revision;
    }

    private byte[] binaryForm;

    /**
     * @param buffer
     * @param i
     */
    public void getBinaryForm(byte[] buffer, int i) {
        this.binaryForm = buffer;
    }

    /**
     * @return
     */
    public int getBinaryLength() {
        return binaryForm.length;
    }

    private List<GenericAce> genericAces = new ArrayList<>();

    /** */
    public int getCount() {
        return genericAces.size();
    }

    /** */
    public void insertAce(int index, GenericAce newAce) {
        genericAces.add(index, newAce);
    }

    @Override
    public Iterator<GenericAce> iterator() {
        return genericAces.iterator();
    }
}

/* */
