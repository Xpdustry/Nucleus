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
package fr.xpdustry.nucleus.testing.ui;

import java.util.HashMap;

public interface State {

    static State create() {
        return new StateImpl(new HashMap<>());
    }

    <T> State with(final StateKey<T> key, final T value);

    State remove(final StateKey<?> key);

    <T> T get(final StateKey<T> key);

    <T> T get(final StateKey<T> key, final T def);

    boolean contains(final StateKey<?> key);

    State copy();
}
