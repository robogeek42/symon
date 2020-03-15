/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.util;

import com.loomcom.symon.exceptions.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.AbstractQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A FIFO buffer with a bounded maximum size.
 */
public class BlockingFifoRingBuffer<E> implements Iterable<E> {

    private LinkedBlockingQueue<E> fifoBuffer;
    private int maxLength;

    public BlockingFifoRingBuffer(int maxLength) {
        this.fifoBuffer = new LinkedBlockingQueue<>();
        this.maxLength = maxLength;
    }

    public E pop() throws FifoUnderrunException {
        E item = fifoBuffer.remove();
        //System.out.println("Pop "+item);
        return item;
    }

    public boolean isEmpty() {
        return fifoBuffer.isEmpty();
    }

    public void push(E val) {
        if (fifoBuffer.size() == maxLength) {
            // Delete the oldest element.
            E item = fifoBuffer.remove();
            System.out.println("Full - remove "+item);
        }
        fifoBuffer.offer(val);
        //System.out.println("Push "+val);
    }

    public E peek() {
        return fifoBuffer.peek();
    }

    public void reset() {
        fifoBuffer.clear();
    }

    public int length() {
        return fifoBuffer.size();
    }

    public String toString() {
        return "[FifoRingBuffer: size=" + fifoBuffer.size() + "]";
    }

    public Iterator<E> iterator() {
        return fifoBuffer.iterator();
    }
}

