package com.zufar.onlinestore.customer.repository.dynamodb;

import java.util.Collection;
import java.util.Optional;

public interface CrudRepository<T> {

    void save(final T entity);

    void save(final Collection<T> entities);

    Optional<T> getById(final String customerId);

    Optional<Collection<T>> getAll();

    void deleteById(final String customerId);

    void deleteByIds(Collection<String> customerIds);

    void update(final String customerId, final T customer);
}
