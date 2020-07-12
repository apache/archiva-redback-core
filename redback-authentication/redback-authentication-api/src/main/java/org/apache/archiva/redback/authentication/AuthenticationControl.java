package org.apache.archiva.redback.authentication;

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
 * These have the same meaning as for PAM modules
 *
 * <dl>
 * <dt>required</dt>
 *     <dd>If a ‘required’ module returns a status that is not ‘success’,
 *     the operation will ultimately fail, but only after the modules below
 *     it are invoked. This seems senseless at first glance I suppose, but
 *     it serves the purpose of always acting the same way from the point
 *     of view of the user trying to utilize the service. The net effect is
 *     that it becomes impossible for a potential cracker to determine
 *     which module caused the failure – and the less information a
 *     malicious user has about your system, the better. Important to note
 *     is that even if all of the modules in the stack succeed, failure of
 *     one ‘required’ module means the operation will ultimately fail. Of
 *     course, if a required module succeeds, the operation can still fail
 *     if a ‘required’ module later in the stack fails.</dd>
 * <dt>requisite</dt>
 *     <dd>If a ‘requisite’ module fails, the operation not only fails, but
 *     the operation is immediately terminated with a failure without
 *     invoking any other modules: ‘do not pass go, do not collect $200’,
 *     so to speak.</dd>
 * <dt>sufficient</dt>
 *     <dd>If a sufficient module succeeds, it is enough to satisfy the
 *     requirements of sufficient modules in that realm for use of the
 *     service, and modules below it that are also listed as ‘sufficient’
 *     are not invoked. If it fails, the operation fails unless a module
 *     invoked after it succeeds. Important to note is that if a ‘required’
 *     module fails before a ‘sufficient’ one succeeds, the operation will
 *     fail anyway, ignoring the status of any ‘sufficient’ modules.</dd>
 * <dt>optional</dt>
 *     <dd>An ‘optional’ module, according to the pam(8) manpage, will only
 *     cause an operation to fail if it’s the only module in the stack for
 *     that facility.</dd>
 * </dl>
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public enum AuthenticationControl
{
    SUFFICIENT, OPTIONAL, REQUIRED, REQUISITE
}
