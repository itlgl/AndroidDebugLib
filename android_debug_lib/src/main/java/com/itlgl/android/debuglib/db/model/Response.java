/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.itlgl.android.debuglib.db.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amitshekhar on 15/11/16.
 */

public class Response {

    public List<Object> rows = new ArrayList<>();
    public List<String> columns = new ArrayList<>();
    public boolean isSuccessful;
    public String error;
    public int dbVersion;

    public Response() {

    }

}
