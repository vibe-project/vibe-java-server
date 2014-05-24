/*
 * Copyright 2014 The Vibe Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atmosphere.vibe;

/**
 * Amorphous data.
 *
 * @author Donghwan Kim
 */
public class Data {

    private String data;

    /**
     * Creates data from a string.
     */
    public Data(String data) {
        this.data = data;
    }

    /**
     * Returns the given type of data.
     * <p/>
     * The allowed types for {@code T} are
     * <ul>
     * <li>{@link String}</li>
     * </ul>
     */
    public <T> T as(Class<T> clazz) {
        if (clazz == String.class) {
            return clazz.cast(data);
        }
        return null;
    }

}
