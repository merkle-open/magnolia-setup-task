package com.merkle.oss.magnolia.setup.task.common.security.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.jcr.Session;

class CachedSessionProvider implements Function<String, Optional<Session>> {
    private final Function<String, Optional<Session>> wrapped;
    private final Map<String, Session> cache = new HashMap<>();

    public CachedSessionProvider(final Function<String, Optional<Session>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Optional<Session> apply(final String workspace) {
        if (!cache.containsKey(workspace)) {
            cache.put(workspace, wrapped.apply(workspace).orElse(null));
        }
        return Optional.ofNullable(cache.get(workspace));
    }
}
