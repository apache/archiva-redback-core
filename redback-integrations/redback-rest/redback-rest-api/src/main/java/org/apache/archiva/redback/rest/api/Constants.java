package org.apache.archiva.redback.rest.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Constants
{
    String DEFAULT_PAGE_LIMIT = "1000";

    String ERR_UNKNOWN = "redback:unknown_error";
    String ERR_USERMANAGER_FAIL = "redback:usermanager_error";
    String ERR_ROLEMANAGER_FAIL = "redback:rolemanager_error";
    String ERR_RBACMANAGER_FAIL = "redback:rbacmanager_error";
    String ERR_INVALID_POST_DATA = "redback:invalid_post_data";

    String ERR_USER_EXISTS = "redback:user.exists";
    String ERR_USER_ID_EMPTY = "redback:user.id.empty";
    String ERR_USER_ID_INVALID = "redback:user.id.invalid";
    String ERR_USER_FULL_NAME_EMPTY = "redback:user.fullname.empty";
    String ERR_USER_EMAIL_EMPTY = "redback:user.email.empty";
    String ERR_USER_ASSIGN_ROLE = "redback:user.role.assign.failure";
    String ERR_USER_NOT_VALIDATED = "redback:user.not_validated";
    String ERR_USER_ADMIN_EXISTS = "redback:user.admin.exists";
    String ERR_USER_ADMIN_BAD_NAME = "redback:user.admin.badname";
    String ERR_USER_NOT_FOUND = "redback:user.not_found";

    String ERR_PASSWORD_VIOLATION = "redback:user.password_violation";

    String ERR_LDAP_GENERIC = "redback:ldap.error";
    String ERR_ROLE_MAPPING = "redback:role.mapping.error";
    String ERR_ROLE_MAPPING_NOT_FOUND = "redback:role.mapping.not_found";

    String ERR_AUTH_BAD_CODE = "redback:auth.bad_authorization_code";
    String ERR_AUTH_INVALID_CREDENTIALS = "redback:auth.invalid_credentials";
    String ERR_AUTH_FAIL_MSG = "redback:auth.fail";
    String ERR_AUTH_ACCOUNT_LOCKED = "redback:auth.account_locked";
    String ERR_AUTH_PASSWORD_CHANGE_REQUIRED = "redback:auth.password_change_required";
    String ERR_AUTH_UNSUPPORTED_GRANT_TYPE = "redback:auth.unsupported_grant";
    String ERR_AUTH_INVALID_TOKEN = "redback:auth.invalid_token";
    String ERR_AUTH_UNAUTHORIZED_REQUEST = "redback:auth.unauthorized_request";

    String ERR_USER_BAD_PASSWORD = "redback:user.bad.password";




}
