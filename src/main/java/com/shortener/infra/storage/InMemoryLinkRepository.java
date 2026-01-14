package com.shortener.infra.storage;

import com.shortener.core.domain.Link;
import com.shortener.core.repository.LinkRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLinkRepository implements LinkRepository {
    private final Map<UUID, Link> storage = new ConcurrentHashMap<>();
    private final Map<String, UUID> shortCodeIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> userLinksIndex = new ConcurrentHashMap<>();

    @Override
    public Optional<Link> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Link> findByShortCode(String shortCode) {
        UUID id = shortCodeIndex.get(shortCode);
        if (id != null) {
            return Optional.ofNullable(storage.get(id));
        }
        return Optional.empty();
    }

    @Override
    public List<Link> findByUserId(UUID userId) {
        Set<UUID> linkIds = userLinksIndex.getOrDefault(userId, Collections.emptySet());
        List<Link> links = new ArrayList<>();
        for (UUID linkId : linkIds) {
            Link link = storage.get(linkId);
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    @Override
    public List<Link> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Link save(Link link) {
        storage.put(link.getId(), link);
        shortCodeIndex.put(link.getShortCode(), link.getId());

        // Обновление индекса пользовательских ссылок
        userLinksIndex.computeIfAbsent(link.getUserId(), k -> new HashSet<>())
                .add(link.getId());

        return link;
    }

    @Override
    public void delete(UUID id) {
        Link link = storage.remove(id);
        if (link != null) {
            shortCodeIndex.remove(link.getShortCode());
            Set<UUID> userLinks = userLinksIndex.get(link.getUserId());
            if (userLinks != null) {
                userLinks.remove(id);
                if (userLinks.isEmpty()) {
                    userLinksIndex.remove(link.getUserId());
                }
            }
        }
    }

    @Override
    public void deleteAll() {
        storage.clear();
        shortCodeIndex.clear();
        userLinksIndex.clear();
    }

    @Override
    public long count() {
        return storage.size();
    }
}
