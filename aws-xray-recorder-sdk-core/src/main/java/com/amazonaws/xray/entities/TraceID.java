/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.entities;

import com.amazonaws.xray.ThreadLocalStorage;
import java.math.BigInteger;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraceID {

    private static final TraceID INVALID = new TraceID(0, BigInteger.ZERO);

    /**
     * Returns a new {@link TraceID} which represents the start of a new trace.
     */
    public static TraceID create() {
        return new TraceID();
    }

    /**
     * Returns the {@link TraceID} parsed out of the {@link String}. If the parse fails, a new {@link TraceID} will be returned,
     * effectively restarting the trace.
     */
    public static TraceID fromString(String xrayTraceId) {
        xrayTraceId = xrayTraceId.trim();

        if (xrayTraceId.length() != TRACE_ID_LENGTH) {
            return TraceID.create();
        }

        // Check version trace id version
        if (xrayTraceId.charAt(0) != VERSION) {
            return TraceID.create();
        }

        // Check delimiters
        if (xrayTraceId.charAt(TRACE_ID_DELIMITER_INDEX_1) != DELIMITER
            || xrayTraceId.charAt(TRACE_ID_DELIMITER_INDEX_2) != DELIMITER) {
            return TraceID.create();
        }

        String startTimePart = xrayTraceId.substring(TRACE_ID_DELIMITER_INDEX_1 + 1, TRACE_ID_DELIMITER_INDEX_2);
        String randomPart = xrayTraceId.substring(TRACE_ID_DELIMITER_INDEX_2 + 1, TRACE_ID_LENGTH);

        final TraceID result;
        try {
            result = new TraceID(Long.valueOf(startTimePart, 16), new BigInteger(randomPart, 16));
        } catch (NumberFormatException e) {
            return TraceID.create();
        }
        return result;
    }

    /**
     * Returns an invalid {@link TraceID} which can be used when an ID is needed outside the context of a trace, for example for
     * an unsampled segment.
     */
    public static TraceID invalid() {
        return INVALID;
    }

    private static final int TRACE_ID_LENGTH = 35;
    private static final int TRACE_ID_DELIMITER_INDEX_1 = 1;
    private static final int TRACE_ID_DELIMITER_INDEX_2 = 10;

    private static final char VERSION = '1';
    private static final char DELIMITER = '-';

    private BigInteger number;
    private long startTime;

    /**
     * @deprecated Use {@link #create()}.
     */
    @Deprecated
    public TraceID() {
        this(Instant.now().getEpochSecond());
    }

    /**
     * @deprecated Use {@link #create()}.
     */
    @Deprecated
    public TraceID(long startTime) {
        number = new BigInteger(96, ThreadLocalStorage.getRandom());
        this.startTime = startTime;
    }

    private TraceID(long startTime, BigInteger number) {
        this.startTime = startTime;
        this.number = number;
    }

    @Override
    public String toString() {
        String paddedNumber = padLeft(number.toString(16), 24);
        String startTime = padLeft(Long.toHexString(this.startTime), 8);
        return "" + VERSION + DELIMITER + startTime + DELIMITER + paddedNumber;
    }

    private static String padLeft(String str, int size) {
        if (str.length() == size) {
            return str;
        }
        StringBuilder padded = new StringBuilder(size);
        for (int i = str.length(); i < size; i++) {
            padded.append('0');
        }
        padded.append(str);
        return padded.toString();
    }

    /**
     * @return the number
     */
    public BigInteger getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     *
     * @deprecated TraceID is effectively immutable and this will be removed
     */
    @Deprecated
    public void setNumber(@Nullable BigInteger number) {
        if (number != null) {
            this.number = number;
        }
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     *
     * @deprecated TraceID is effectively immutable and this will be removed
     */
    @Deprecated
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((number == null) ? 0 : number.hashCode());
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TraceID)) {
            return false;
        }
        TraceID other = (TraceID) obj;
        return number.equals(other.number) && startTime == other.startTime;
    }
}
