package org.example.warehouse;

import java.util.List;

public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalElements;

    public Page(List<T> items, int page, int pageSize, long totalElements) {
        this.items = List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
    }

    public boolean hasNext() {
        return page < getTotalPages();
    }

    @Override
    public String toString() {
        return "Page{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", totalElements=" + totalElements +
                ", totalPages=" + getTotalPages() +
                ", items=" + items.size() +
                '}';
    }
}
