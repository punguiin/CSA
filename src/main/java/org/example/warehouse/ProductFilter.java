package org.example.warehouse;

public final class ProductFilter {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 1000;

    private final String nameContains;
    private final String category;
    private final Integer minQuantity;
    private final Integer maxQuantity;
    private final Long minPriceMinor;
    private final Long maxPriceMinor;
    private final int page;
    private final int pageSize;

    private ProductFilter(Builder b) {
        this.nameContains = b.nameContains;
        this.category = b.category;
        this.minQuantity = b.minQuantity;
        this.maxQuantity = b.maxQuantity;
        this.minPriceMinor = b.minPriceMinor;
        this.maxPriceMinor = b.maxPriceMinor;
        this.page = b.page;
        this.pageSize = b.pageSize;
    }

    public String getNameContains() {
        return nameContains;
    }

    public String getCategory() {
        return category;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public Long getMinPriceMinor() {
        return minPriceMinor;
    }

    public Long getMaxPriceMinor() {
        return maxPriceMinor;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int offset() {
        return (page - 1) * pageSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String nameContains;
        private String category;
        private Integer minQuantity;
        private Integer maxQuantity;
        private Long minPriceMinor;
        private Long maxPriceMinor;
        private int page = DEFAULT_PAGE;
        private int pageSize = DEFAULT_PAGE_SIZE;

        public Builder name(String nameContains) {
            this.nameContains = blankToNull(nameContains);
            return this;
        }

        public Builder category(String category) {
            this.category = blankToNull(category);
            return this;
        }

        public Builder minQuantity(Integer minQuantity) {
            this.minQuantity = minQuantity;
            return this;
        }

        public Builder maxQuantity(Integer maxQuantity) {
            this.maxQuantity = maxQuantity;
            return this;
        }

        public Builder minPriceMinor(Long minPriceMinor) {
            this.minPriceMinor = minPriceMinor;
            return this;
        }

        public Builder maxPriceMinor(Long maxPriceMinor) {
            this.maxPriceMinor = maxPriceMinor;
            return this;
        }

        public Builder page(int page) {
            this.page = Math.max(1, page);
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
            return this;
        }

        public ProductFilter build() {
            if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
                throw new IllegalArgumentException(
                        "minQuantity " + minQuantity + " > maxQuantity " + maxQuantity);
            }
            if (minPriceMinor != null && maxPriceMinor != null && minPriceMinor > maxPriceMinor) {
                throw new IllegalArgumentException(
                        "minPrice " + minPriceMinor + " > maxPrice " + maxPriceMinor);
            }
            return new ProductFilter(this);
        }

        private static String blankToNull(String s) {
            return (s == null || s.isBlank()) ? null : s;
        }
    }
}
