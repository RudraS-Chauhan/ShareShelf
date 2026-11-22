package com.shareshelf.dao;

/**
 * The contract every "entity" DAO shares: create one row and read one row back by its key.
 * Specific DAOs add their own queries on top (search, findByOwner, reports, ...).
 *
 * @param <T>  the model class the DAO stores (Student, ResourceItem, Request, Wanted)
 * @param <ID> the type of its primary key (String student id, Integer for generated ids)
 */
public interface Dao<T, ID> {

    /** Stores a new row. @return true if it was stored (false e.g. for a duplicate key). */
    boolean insert(T entity);

    /** @return the row with this key, or null if there is none. */
    T findById(ID id);

    /** @return true if a row with this key exists. */
    default boolean exists(ID id) {
        return findById(id) != null;
    }
}
