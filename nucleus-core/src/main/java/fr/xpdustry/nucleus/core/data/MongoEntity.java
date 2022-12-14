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

import org.bson.Document;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public abstract class MongoEntity<E extends MongoEntity<E, I>, I> {

    private @MonotonicNonNull I identifier;

    public I getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("unchecked")
    public E setIdentifier(I identifier) {
        this.identifier = identifier;
        return (E) this;
    }

    public abstract static class Codec<E extends MongoEntity<?, ?>> {

        public abstract Document encode(final E entity);

        public abstract E decode(final Document entity);
    }
}
