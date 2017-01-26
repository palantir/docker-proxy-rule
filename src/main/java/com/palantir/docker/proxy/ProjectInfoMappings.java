/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.proxy;

import com.google.common.collect.Multimap;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface ProjectInfoMappings {
    Map<String, String> getHostToIp();
    Multimap<String, String> getIpToHosts();
}
