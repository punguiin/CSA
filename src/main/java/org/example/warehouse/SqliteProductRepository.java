package org.example.warehouse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteProductRepository implements ProductRepository {

    private final Connection connection;

    public SqliteProductRepository(String dbName) {
        this("jdbc:sqlite:" + dbName, true);
    }

    SqliteProductRepository(String jdbcUrl, boolean ownsUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new WarehouseException("Can't open SQLite DB at " + jdbcUrl, e);
        }
        init();
    }

    public static SqliteProductRepository inMemory() {
        return new SqliteProductRepository("jdbc:sqlite::memory:", true);
    }

    @Override
    public int create(Product product) {
        boolean withId = product.getId() != null;
        String sql = withId
                ? "INSERT INTO product(id, name, category, quantity, price_minor) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO product(name, category, quantity, price_minor) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            if (withId) {
                ps.setInt(i++, product.getId());
            }
            ps.setString(i++, product.getName());
            ps.setString(i++, product.getCategory());
            ps.setInt(i++, product.getQuantity());
            ps.setLong(i, product.getPriceMinor());

            if (ps.executeUpdate() < 1) {
                throw new WarehouseException("Insert affected no rows for " + product);
            }
            if (withId) {
                return product.getId();
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    product.setId(id);
                    return id;
                }
            }
            throw new WarehouseException("Insert returned no generated key for " + product);
        } catch (SQLException e) {
            throw new WarehouseException("Can't insert " + product, e);
        }
    }

    @Override
    public Optional<Product> findById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new WarehouseException("Can't read product " + id, e);
        }
    }

    @Override
    public List<Product> findAll() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Product> all = new ArrayList<>();
            while (rs.next()) {
                all.add(map(rs));
            }
            return all;
        } catch (SQLException e) {
            throw new WarehouseException("Can't list products", e);
        }
    }

    @Override
    public boolean update(Product product) {
        if (product.getId() == null) {
            throw new IllegalArgumentException("Can't update a product without an id");
        }
        String sql = "UPDATE product SET name = ?, category = ?, quantity = ?, price_minor = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setLong(4, product.getPriceMinor());
            ps.setInt(5, product.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new WarehouseException("Can't update " + product, e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new WarehouseException("Can't delete product " + id, e);
        }
    }

    @Override
    public Page<Product> search(ProductFilter filter) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        buildWhere(filter, clauses, params);
        String where = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);

        long total = count(where, params);

        List<Product> items = new ArrayList<>();
        String sql = "SELECT * FROM product" + where + " ORDER BY id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = bind(ps, params);
            ps.setInt(i++, filter.getPageSize());
            ps.setInt(i, filter.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new WarehouseException("Search failed for filter on page " + filter.getPage(), e);
        }
        return new Page<>(items, filter.getPage(), filter.getPageSize(), total);
    }

    @Override
    public int count() {
        return (int) count("", List.of());
    }

    @Override
    public int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new WarehouseException("Can't clear products", e);
        }
    }

    private static void buildWhere(ProductFilter f, List<String> clauses, List<Object> params) {
        if (f.getNameContains() != null) {
            clauses.add("LOWER(name) LIKE ?");
            params.add("%" + f.getNameContains().toLowerCase() + "%");
        }
        if (f.getCategory() != null) {
            clauses.add("category = ?");
            params.add(f.getCategory());
        }
        if (f.getMinQuantity() != null) {
            clauses.add("quantity >= ?");
            params.add(f.getMinQuantity());
        }
        if (f.getMaxQuantity() != null) {
            clauses.add("quantity <= ?");
            params.add(f.getMaxQuantity());
        }
        if (f.getMinPriceMinor() != null) {
            clauses.add("price_minor >= ?");
            params.add(f.getMinPriceMinor());
        }
        if (f.getMaxPriceMinor() != null) {
            clauses.add("price_minor <= ?");
            params.add(f.getMaxPriceMinor());
        }
    }

    private long count(String where, List<Object> params) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new WarehouseException("Count failed", e);
        }
    }

    private static int bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            if (p instanceof Integer v) {
                ps.setInt(i++, v);
            } else if (p instanceof Long v) {
                ps.setLong(i++, v);
            } else {
                ps.setString(i++, String.valueOf(p));
            }
        }
        return i;
    }

    private static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getLong("price_minor"));
    }

    private void init() {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS product (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        name        VARCHAR(100) NOT NULL,
                        category    VARCHAR(100) NOT NULL,
                        quantity    INTEGER NOT NULL DEFAULT 0,
                        price_minor INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            throw new WarehouseException("Can't initialise schema", e);
        }
    }
}
