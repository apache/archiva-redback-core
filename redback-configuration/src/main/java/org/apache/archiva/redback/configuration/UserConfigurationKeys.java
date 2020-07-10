package org.apache.archiva.redback.configuration;
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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Olivier Lamy
 * @since 2.1
 */
public interface UserConfigurationKeys
{
    String USER_MANAGER_IMPL = "user.manager.impl";

    String RBAC_MANAGER_IMPL = "rbac.manager.impl";

    String DEFAULT_ADMIN = "redback.default.admin";

    String DEFAULT_GUEST = "redback.default.guest";

    String DEFAULT_GUEST_ROLE_ID = "redback.default.role.guest.id";

    String EMAIL_FROM_ADDRESS = "email.from.address";

    String EMAIL_FROM_NAME = "email.from.name";

    String EMAIL_FEEDBACK_PATH = "email.feedback.path";

    String APPLICATION_TIMESTAMP = "application.timestamp";

    String PASSWORD_ENCODER = "security.policy.password.encoder";

    String EMAIL_VALIDATION_SUBJECT = "email.validation.subject";

    String REMEMBER_ME_PATH = "security.rememberme.path";

    String REMEMBER_ME_DOMAIN = "security.rememberme.domain";

    String REMEMBER_ME_ENABLED = "security.rememberme.enabled";

    String REMEMBER_ME_TIMEOUT = "security.rememberme.timeout";

    String REMEMBER_ME_SECURE = "security.rememberme.secure";

    String SIGNON_DOMAIN = "security.signon.domain";

    String SIGNON_PATH = "security.signon.path";

    String SIGNON_TIMEOUT = "security.signon.timeout";

    String LDAP_HOSTNAME = "ldap.config.hostname";

    String LDAP_PORT = "ldap.config.port";

    String LDAP_SSL = "ldap.config.ssl";

    String LDAP_CONTEX_FACTORY = "ldap.config.context.factory";

    String LDAP_PASSWORD = "ldap.config.password";

    String LDAP_AUTHENTICATION_METHOD = "ldap.config.authentication.method";

    String LDAP_DN_ATTRIBUTE = "ldap.config.dn";

    String LDAP_BASEDN = "ldap.config.base.dn";

    String LDAP_BINDDN = "ldap.config.bind.dn";

    String LDAP_GROUPS_CLASS = "ldap.config.groups.class";

    String LDAP_GROUPS_BASEDN = "ldap.config.groups.base.dn";

    String LDAP_GROUPS_FILTER = "ldap.config.groups.filter";

    String LDAP_GROUPS_MEMBER = "ldap.config.groups.member";

    String LDAP_GROUPS_ROLE_START_KEY = "ldap.config.groups.role.";

    String LDAP_GROUPS_USE_ROLENAME = "ldap.config.groups.use.rolename";

    String LDAP_GROUPS_USE_DN_AS_MEMBER_VALUE = "ldap.config.groups.useDnAsMemberValue";

    String LDAP_WRITABLE = "ldap.config.writable";

    String LDAP_USER_ID_ATTRIBUTE = "ldap.config.user.attribute";

    String LDAP_GROUP_NAME_ATTRIBUTE = "ldap.config.groups.name.attribute";

    String LDAP_GROUP_DESCRIPTION_ATTRIBUTE = "ldap.config.groups.description.attribute";

    String APPLICATION_URL = "application.url";

    String EMAIL_URL_PATH = "email.url.path";

    String LDAP_MAPPER_USER_ATTRIBUTE_EMAIL = "ldap.config.mapper.attribute.email";

    String LDAP_MAPPER_USER_ATTRIBUTE_FULLNAME = "ldap.config.mapper.attribute.fullname";

    String LDAP_MAPPER_USER_ATTRIBUTE_PASSWORD = "ldap.config.mapper.attribute.password";

    String LDAP_MAPPER_USER_ATTRIBUTE_ID = "ldap.config.mapper.attribute.user.id";

    String LDAP_MAPPER_USER_ATTRIBUTE_OBJECT_CLASS = "ldap.config.mapper.attribute.user.object.class";

    String LDAP_MAPPER_USER_ATTRIBUTE_FILTER = "ldap.config.mapper.attribute.user.filter";

    String LDAP_MAX_RESULT_COUNT = "ldap.config.max.result.count";

    String LDAP_BIND_AUTHENTICATOR_ENABLED = "ldap.bind.authenticator.enabled";

    String LDAP_BIND_AUTHENTICATOR_ALLOW_EMPTY_PASSWORDS = "ldap.bind.authenticator.allowEmptyPasswords";

    String PASSWORD_RETENTION_COUNT = "security.policy.password.previous.count";

    String LOGIN_ATTEMPT_COUNT = "security.policy.allowed.login.attempt";

    String PASSWORD_EXPIRATION_ENABLED = "security.policy.password.expiration.enabled";

    String PASSWORD_EXPIRATION = "security.policy.password.expiration.days";

    String UNLOCKABLE_ACCOUNTS = "security.policy.unlockable.accounts";

    String EMAIL_VALIDATION_TIMEOUT = "email.validation.timeout";

    String EMAIL_VALIDATION_REQUIRED = "email.validation.required";

    String ALPHA_COUNT_MIN = "security.policy.password.rule.alphacount.minimum";

    String ALPHA_COUNT_VIOLATION = "user.password.violation.alpha";

    String CHARACTER_LENGTH_MIN = "security.policy.password.rule.characterlength.minimum";

    String CHARACTER_LENGTH_MAX = "security.policy.password.rule.characterlength.maximum";

    String CHARACTER_LENGTH_MISCONFIGURED_VIOLATION = "user.password.violation.length.misconfigured";

    String CHARACTER_LENGTH_VIOLATION = "user.password.violation.length";

    String MINIMUM = "security.policy.password.rule.numericalcount.minimum";

    String NUMERICAL_COUNT_VIOLATION = "user.password.violation.numeric";

    String POLICY_PASSWORD_RULE_ALPHANUMERIC_ENABLED = "security.policy.password.rule.alphanumeric.enabled";

    String POLICY_PASSWORD_RULE_ALPHACOUNT_ENABLED = "security.policy.password.rule.alphacount.enabled";

    String POLICY_PASSWORD_RULE_CHARACTERLENGTH_ENABLED = "security.policy.password.rule.characterlength.enabled";

    String POLICY_PASSWORD_RULE_MUSTHAVE_ENABLED = "security.policy.password.rule.musthave.enabled";

    String POLICY_PASSWORD_RULE_NUMERICALCOUNT_ENABLED = "security.policy.password.rule.numericalcount.enabled";

    String POLICY_PASSWORD_RULE_REUSE_ENABLED = "security.policy.password.rule.reuse.enabled";

    String POLICY_PASSWORD_RULE_NOWHITTESPACE_ENABLED = "security.policy.password.rule.nowhitespace.enabled";

    String REST_BASE_URL = "rest.baseUrl";

    String REST_CSRF_ABSENTORIGIN_DENY = "rest.csrffilter.absentorigin.deny";

    String REST_CSRF_ENABLED = "rest.csrffilter.enabled";

    String REST_CSRF_DISABLE_TOKEN_VALIDATION = "rest.csrffilter.disableTokenValidation";

    /**
     * Encoding used for reading mail templates / Default is UTF-8
     */
    String MAIL_TEMPLATE_ENCODING = "mail.template.encoding";

    /**
     * The locale to use for sending mails and finding mail templates
     */
    String MAIL_DEFAULT_LOCALE = "mail.locale";

    /**
     * Defines, where the key for JWT encryption / decryption is stored.
     * Currently only memory and plainfile are supported
     * {@value}
     */
    String AUTHENTICATION_JWT_KEYSTORETYPE = "authentication.jwt.keystoreType";
    /**
     * Keystore type name for memory keystore: {@value}
     */
    String AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY = "memory";
    /**
     * Keystore type name for plain file keystore: {@value}
     */
    String AUTHENTICATION_JWT_KEYSTORETYPE_PLAINFILE = "plainfile";
    /**
     * Defines the used signature algorithm for JWT signing: {@value}
     */
    String AUTHENTICATION_JWT_SIGALG = "authentication.jwt.signatureAlgorithm";
    /**
     * Defines the maximum number of keys to keep in memory for verificatio: {@value}
     */
    String AUTHENTICATION_JWT_MAX_KEYS = "authentication.jwt.maxInMemoryKeys";

    /**
     * HMAC using SHA-256
     */
    String AUTHENTICATION_JWT_SIGALG_HS256 = "HS256";
    /**
     * HMAC using SHA-384
     */
    String AUTHENTICATION_JWT_SIGALG_HS384 = "HS384";
    /**
     * HMAC using SHA-512
     */
    String AUTHENTICATION_JWT_SIGALG_HS512 = "HS512";
    /**
     * ECDSA using P-256 and SHA-256
     */
    String AUTHENTICATION_JWT_SIGALG_ES256 = "ES256";
    /**
     * ECDSA using P-384 and SHA-384
     */
    String AUTHENTICATION_JWT_SIGALG_ES384 = "ES384";
    /**
     * ECDSA using P-521 and SHA-512
     */
    String AUTHENTICATION_JWT_SIGALG_ES512 = "ES512";
    /**
     * RSASSA-PKCS-v1_5 using SHA-256
     */
    String AUTHENTICATION_JWT_SIGALG_RS256 = "RS256";
    /**
     * RSASSA-PKCS-v1_5 using SHA-384
     */
    String AUTHENTICATION_JWT_SIGALG_RS384 = "RS384";
    /**
     * RSASSA-PKCS-v1_5 using SHA-512
     */
    String AUTHENTICATION_JWT_SIGALG_RS512 = "RS512";
    /**
     * RSASSA-PSS using SHA-256 and MGF1 with SHA-256
     */
    String AUTHENTICATION_JWT_SIGALG_PS256 = "PS256";
    /**
     * RSASSA-PSS using SHA-384 and MGF1 with SHA-384
     */
    String AUTHENTICATION_JWT_SIGALG_PS384 = "PS384";
    /**
     * RSASSA-PSS using SHA-512 and MGF1 with SHA-512
     */
    String AUTHENTICATION_JWT_SIGALG_PS512 = "PS512";


    /**
     * Path to the file where the JWT key is stored: {@value}
     */
    String AUTHENTICATION_JWT_KEYFILE = "authentication.jwt.keyfile";

    /**
     * The lifetime in ms of the generated tokens: {@value}
     */
    String AUTHENTICATION_JWT_LIFETIME_MS = "authentication.jwt.lifetimeMs";

}
