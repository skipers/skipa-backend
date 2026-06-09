package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.data.domain.Sort;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public record PatentSortOption(
        String field,
        Sort.Direction direction
) {

    public static PatentSortOption parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return new PatentSortOption("applicationNumber", Sort.Direction.ASC);
        }

        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String field = parts[0].trim();
        if (!isSupportedField(field)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Sort.Direction direction = defaultDirection(field);
        if (parts.length == 2 && !parts[1].isBlank()) {
            direction = parseDirection(parts[1]);
        }

        return new PatentSortOption(field, direction);
    }

    public static <T> Comparator<T> comparator(
            PatentSortOption option,
            Function<T, Comparable<?>> valueExtractor,
            Function<T, Long> idExtractor
    ) {
        return option.comparator(valueExtractor, idExtractor);
    }

    public Sort reviewSort() {
        if ("id".equals(field)) {
            return Sort.by(direction, "id");
        }
        return Sort.by(direction, reviewProperty()).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    public <T> Comparator<T> comparator(Function<T, Comparable<?>> valueExtractor, Function<T, Long> idExtractor) {
        return comparator(valueExtractor).thenComparing(idExtractor, Comparator.reverseOrder());
    }

    private <T> Comparator<T> comparator(Function<T, Comparable<?>> valueExtractor) {
        return Comparator
                .comparing(valueExtractor, this::compareValues);
    }

    private String reviewProperty() {
        return switch (field) {
            case "title" -> "patent.title";
            case "applicationNumber" -> "patent.applicationNumber";
            case "applicationDate" -> "patent.applicationDate";
            case "expiryDate" -> "patent.expiryDate";
            case "id" -> "id";
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }

    private static boolean isSupportedField(String field) {
        return switch (field) {
            case "id", "title", "applicationNumber", "applicationDate", "expiryDate", "citationCount" -> true;
            default -> false;
        };
    }

    private static Sort.Direction defaultDirection(String field) {
        return switch (field) {
            case "expiryDate", "applicationDate" -> Sort.Direction.ASC;
            default -> Sort.Direction.DESC;
        };
    }

    private static Sort.Direction parseDirection(String value) {
        try {
            return Sort.Direction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareValues(Comparable left, Comparable right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        int result = left.compareTo(right);
        return direction == Sort.Direction.DESC ? -result : result;
    }
}
