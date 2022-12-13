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

import java.util.List;
import mindustry.gen.Player;

public interface Interface<I extends Interface<I, V, P>, V extends View<I, P>, P extends Pane> {

    V open(final Player viewer, final State state);

    default V open(final Player viewer) {
        return open(viewer, State.create());
    }

    void addTransformer(final Transform<V, P> transformer);

    List<Transform<V, P>> getTransformers();
}
