package org.apache.archiva.redback.rest.api;/*
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
 * Keys for error messages that are returned by REST API
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface MessageKeys
{
    String ERR_UNKNOWN = "rb.unknown_error";
    String ERR_USERMANAGER_FAIL = "rb.usermanager_error";
    String ERR_ROLEMANAGER_FAIL = "rb.rolemanager_error";
    String ERR_RBACMANAGER_FAIL = "rb.rbacmanager_error";
    String ERR_KEYMANAGER_FAIL = "rb.keymanager_error";
    String ERR_EMPTY_DATA = "rb.empty_data_received";
    String ERR_INVALID_POST_DATA = "rb.invalid_post_data";
    String ERR_USER_EXISTS = "rb.user.exists";
    String ERR_USER_ID_EMPTY = "rb.user.id.empty";
    String ERR_USER_ID_INVALID = "rb.user.id.invalid";
    String ERR_USER_FULL_NAME_EMPTY = "rb.user.fullname.empty";
    String ERR_USER_EMAIL_EMPTY = "rb.user.email.empty";
    String ERR_USER_EMAIL_INVALID = "rb.user.email.invalid";
    String ERR_USER_ASSIGN_ROLE = "rb.user.role.assign.failure";
    String ERR_USER_NOT_VALIDATED = "rb.user.not_validated";
    String ERR_USER_ADMIN_EXISTS = "rb.user.admin.exists";
    String ERR_USER_ADMIN_BAD_NAME = "rb.user.admin.badname";
    String ERR_USER_NOT_FOUND = "rb.user.not_found";
    String ERR_USER_BAD_PASSWORD = "rb.user.bad.password";
    String ERR_PASSWORD_VIOLATION = "rb.user.password_violation";

    String ERR_LDAP_GENERIC = "rb.ldap.error";
    String ERR_ROLE_MAPPING = "rb.role.mapping.error";
    String ERR_ROLE_MAPPING_NOT_FOUND = "rb.role.mapping.not_found";
    String ERR_ROLE_NOT_FOUND = "rb.role.not_found";
    // A template instance not found. With arguments templateId, resource
    String ERR_ROLE_INSTANCE_NOT_FOUND = "rb.role.instance.not_found";
    String ERR_ROLE_EXISTS = "rb.role.exists";
    // A template instance exists. With arguments templateId, resource
    String ERR_ROLE_INSTANCE_EXISTS = "rb.role.instance.exists";
    String ERR_ROLE_ID_INVALID = "rb.role.invalid_id";
    String ERR_ROLE_DELETION_WITH_PERMANENT_FLAG = "rb.role.deletion_with_permanent_flag";

    String ERR_AUTH_BAD_CODE = "rb.auth.bad_authorization_code";
    String ERR_AUTH_INVALID_CREDENTIALS = "rb.auth.invalid_credentials";
    String ERR_AUTH_FAIL_MSG = "rb.auth.fail";
    String ERR_AUTH_ACCOUNT_LOCKED = "rb.auth.account_locked";
    String ERR_AUTH_PASSWORD_CHANGE_REQUIRED = "rb.auth.password_change_required";
    String ERR_AUTH_UNSUPPORTED_GRANT_TYPE = "rb.auth.unsupported_grant";
    String ERR_AUTH_INVALID_TOKEN = "rb.auth.invalid_token";
    String ERR_AUTH_UNAUTHORIZED_REQUEST = "rb.auth.unauthorized_request";

    String ERR_PASSWD_RESET_FAILED = "rb.passwd.reset.fail";

    String ERR_REGISTRATION_KEY_INVALID = "rb.registration.key.invalid";
    String ERR_REGISTRATION_USER_VALIDATED = "rb.registration.user.validated";
    String ERR_REGISTRATION_ROLE_ASSIGNMENT_FAILED = "rb.registration.role.assignment.failed";

}
