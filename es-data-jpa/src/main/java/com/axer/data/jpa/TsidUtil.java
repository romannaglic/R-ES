/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axer.data.jpa;

import com.github.f4b6a3.tsid.TsidFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating TSIDs.
 */
public class TsidUtil {
    public static final String TSID_NODE_COUNT_PROPERTY =
        "tsid.node.count";
    public static final String TSID_NODE_COUNT_ENV =
        "TSID_NODE_COUNT";

    private static final TsidFactory tsidFactory;

    public static TsidFactory getTsidFactory() {
        return tsidFactory;
    }

    static {
        String nodeCountSetting = System.getProperty(
            TSID_NODE_COUNT_PROPERTY
        );
        if (nodeCountSetting == null) {
            nodeCountSetting = System.getenv(
                TSID_NODE_COUNT_ENV
            );
        }

        int nodeCount = nodeCountSetting != null ?
            Integer.parseInt(nodeCountSetting) :
            256;

        int nodeBits = (int) (Math.log(nodeCount) / Math.log(2));

        tsidFactory = TsidFactory.builder()
            .withRandomFunction(length -> {
                final byte[] bytes = new byte[length];
                ThreadLocalRandom.current().nextBytes(bytes);
                return bytes;
            })
            .withNodeBits(nodeBits)
            .build();
    }
}
