package ru.practicum.ewm.util;

import org.springframework.data.domain.PageRequest;

public final class PagingUtil {
    public static PageRequest pageOf(int from, int size) {
        return PageRequest.of(from > 0 ? from / size : 0, size);
    }
}