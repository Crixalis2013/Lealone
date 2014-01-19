/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codefollower.lealone.atomicdb.cql.statements;

import com.codefollower.lealone.atomicdb.auth.Auth;
import com.codefollower.lealone.atomicdb.config.DatabaseDescriptor;
import com.codefollower.lealone.atomicdb.cql.UserOptions;
import com.codefollower.lealone.atomicdb.exceptions.InvalidRequestException;
import com.codefollower.lealone.atomicdb.exceptions.RequestExecutionException;
import com.codefollower.lealone.atomicdb.exceptions.RequestValidationException;
import com.codefollower.lealone.atomicdb.exceptions.UnauthorizedException;
import com.codefollower.lealone.atomicdb.service.ClientState;
import com.codefollower.lealone.atomicdb.transport.messages.ResultMessage;

public class CreateUserStatement extends AuthenticationStatement
{
    private final String username;
    private final UserOptions opts;
    private final boolean superuser;

    public CreateUserStatement(String username, UserOptions opts, boolean superuser)
    {
        this.username = username;
        this.opts = opts;
        this.superuser = superuser;
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        if (username.isEmpty())
            throw new InvalidRequestException("Username can't be an empty string");

        opts.validate();

        // validate login here before checkAccess to avoid leaking user existence to anonymous users.
        state.ensureNotAnonymous();

        if (Auth.isExistingUser(username))
            throw new InvalidRequestException(String.format("User %s already exists", username));
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        if (!state.getUser().isSuper())
            throw new UnauthorizedException("Only superusers are allowed to perform CREATE USER queries");
    }

    public ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException
    {
        DatabaseDescriptor.getAuthenticator().create(username, opts.getOptions());
        Auth.insertUser(username, superuser);
        return null;
    }
}