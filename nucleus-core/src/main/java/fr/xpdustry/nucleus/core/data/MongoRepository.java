/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.core.data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;
import org.bson.Document;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MongoRepository<E extends MongoEntity<E, I>, I> {

    private final MongoCollection<Document> collection;
    private final MongoEntity.Codec<E> codec;

    protected MongoRepository(final MongoCollection<Document> collection, final MongoEntity.Codec<E> codec) {
        this.collection = collection;
        this.codec = codec;
    }

    public void save(final E entity) {
        this.collection.replaceOne(
                Filters.eq("_id", entity.getIdentifier()), codec.encode(entity), new ReplaceOptions().upsert(true));
    }

    public void saveAll(final Iterable<E> entities) {
        entities.forEach(this::save);
    }

    public Optional<E> findById(final I id) {
        final var result = this.collection.find(Filters.eq("_id", id)).first();
        return result == null ? Optional.empty() : Optional.of(codec.decode(result));
    }

    public Iterable<E> findAll() {
        return collection.find().map(codec::decode);
    }

    public boolean exists(final E entity) {
        return findById(entity.getIdentifier()).isPresent();
    }

    public long count() {
        return collection.countDocuments();
    }

    public void deleteById(final I id) {
        this.collection.deleteOne(Filters.eq("_id", id));
    }

    public void deleteAll() {
        collection.deleteMany(Filters.empty());
    }

    public void deleteAll(final Iterable<E> entities) {
        final var ids = new ArrayList<I>();
        for (final var entity : entities) {
            ids.add(entity.getIdentifier());
        }
        collection.deleteMany(Filters.in("_id", ids));
    }

    public Supplier<E> lazy(final I id) {
        return new LazySupplier(id);
    }

    protected MongoCollection<Document> getCollection() {
        return this.collection;
    }

    protected MongoEntity.Codec<E> getCodec() {
        return this.codec;
    }

    private final class LazySupplier implements Supplier<E> {

        private final I id;
        private @MonotonicNonNull E entity = null;

        private LazySupplier(final I id) {
            this.id = id;
        }

        @Override
        public E get() {
            if (entity == null) {
                entity = findById(id).orElseThrow();
            }
            return entity;
        }
    }
}
