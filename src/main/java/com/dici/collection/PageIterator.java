package com.dici.collection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/// An [Iterator] which iterates through pages of content based on an generic context of continuation token
@RequiredArgsConstructor
public class PageIterator<T, TOKEN> implements Iterator<T> {
    private final PageFetcher<T, TOKEN> pageFetcher;

    private Optional<TOKEN> continuationToken;
    private Iterator<T> items = Collections.emptyIterator();
    private boolean truncated = true;

    public PageIterator(@NonNull PageFetcher<T, TOKEN> pageFetcher, @NonNull Optional<TOKEN> tokenSeed) {
        this.pageFetcher = pageFetcher;
        this.continuationToken = tokenSeed;
    }

    @Override
    public boolean hasNext() {
        if (items.hasNext()) return true;
        if (!truncated) return false;

        // Some AWS APIs return empty pages with a continuation token, so we keep fetching until either there is no token or the page is ot empty
        while (!items.hasNext() && truncated) {
            Page<T, TOKEN> nextPage = pageFetcher.fetchPage(continuationToken);
            items = nextPage.items;
            continuationToken = nextPage.continuationToken;
            truncated = continuationToken.isPresent();
        }
        return items.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        return items.next();
    }

    interface PageFetcher<T, TOKEN> {
        Page<T, TOKEN> fetchPage(Optional<TOKEN> token);
    }

    record Page<T, TOKEN>(Iterator<T> items, Optional<TOKEN> continuationToken) {}
}
