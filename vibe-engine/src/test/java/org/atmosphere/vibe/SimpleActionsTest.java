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

import org.atmosphere.vibe.Actions.Options;

public class SimpleActionsTest extends ActionsTestTemplate {

    @Override
    protected <T> Actions<T> createActions() {
        return new SimpleActions<>();
    }

    @Override
    protected <T> Actions<T> createActions(Options options) {
        return new SimpleActions<>(options);
    }

}
