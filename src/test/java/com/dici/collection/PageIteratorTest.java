package com.dici.collection;

import com.dici.collection.PageIterator.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.dici.collection.PageIterator.PageFetcher;
import static com.dici.testing.assertj.BetterAssertions.assertThatThrownBy;
import static java.util.Collections.emptyIterator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@MockitoSettings
class PageIteratorTest {
    @Mock private PageFetcher<String, String> pageFetcher;
    private PageIterator<String, String> pageIterator;

    @BeforeEach
    void setUp() {
        pageIterator = new PageIterator<>(pageFetcher, Optional.empty());
    }

    @Test
    void testIteration_noPage() {
        when(pageFetcher.fetchPage(Optional.empty())).thenReturn(new Page<>(emptyIterator(), Optional.empty()));
        assertThat(pageIterator.hasNext()).isFalse();
        assertThatThrownBy(() -> pageIterator.next()).isLike(new NoSuchElementException());
    }

    @Test
    void testIteration_onePage() {
        when(pageFetcher.fetchPage(Optional.empty())).thenReturn(new Page<>(List.of("a", "b").iterator(), Optional.empty()));

        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("a");
        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("b");
        assertThat(pageIterator.hasNext()).isFalse();
    }

    @Test
    void testIteration_multiplePages() {
        when(pageFetcher.fetchPage(Optional.empty())).thenReturn(new Page<>(List.of("a", "b").iterator(), Optional.of("token1")));
        when(pageFetcher.fetchPage(Optional.of("token1"))).thenReturn(new Page<>(List.of("c", "d").iterator(), Optional.empty()));

        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("a");
        assertThat(pageIterator.next()).isEqualTo("b");
        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("c");
        assertThat(pageIterator.next()).isEqualTo("d");
        assertThat(pageIterator.hasNext()).isFalse();
    }

    @Test
    void testIteration_emptyPageThenNonEmptyPage() {
        when(pageFetcher.fetchPage(Optional.empty())).thenReturn(new Page<>(emptyIterator(), Optional.of("token1")));
        when(pageFetcher.fetchPage(Optional.of("token1"))).thenReturn(new Page<>(List.of("a").iterator(), Optional.empty()));

        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("a");
        assertThat(pageIterator.hasNext()).isFalse();
    }

    @Test
    void testIteration_multipleEmptyPagesThenNonEmptyPage() {
        when(pageFetcher.fetchPage(Optional.empty())).thenReturn(new Page<>(emptyIterator(), Optional.of("token1")));
        when(pageFetcher.fetchPage(Optional.of("token1"))).thenReturn(new Page<>(emptyIterator(), Optional.of("token2")));
        when(pageFetcher.fetchPage(Optional.of("token2"))).thenReturn(new Page<>(List.of("a").iterator(), Optional.empty()));

        assertThat(pageIterator.hasNext()).isTrue();
        assertThat(pageIterator.next()).isEqualTo("a");
        assertThat(pageIterator.hasNext()).isFalse();
    }
}
