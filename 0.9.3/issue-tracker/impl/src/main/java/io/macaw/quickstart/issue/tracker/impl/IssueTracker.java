//
//  This file was auto-generated by Macaw tools 0.3.0-SNAPSHOT version built on Tue, 14 Feb 2017 14:52:15 +0530 
//
/**
 * Copyright © 2015-2016, Macaw Software Inc.
 * All rights reserved.
 * 
 * This software and related documentation are provided under a
 * license agreement containing restrictions on use and
 * disclosure and are protected by intellectual property laws.
 * Except as expressly permitted in your license agreement or
 * allowed by law, you may not use, copy, reproduce, translate,
 * broadcast, modify, license, transmit, distribute, exhibit,
 * perform, publish, or display any part, in any form, or by
 * any means. Reverse engineering, disassembly, or
 * decompilation of this software, unless required by law for
 * interoperability, is prohibited.
 * 
 * The information contained herein is subject to change
 * without notice and is not warranted to be error-free. If you
 * find any errors, please report them to us in writing.
 */
package io.macaw.quickstart.issue.tracker.impl;

import com.cfx.service.client.api.JSONMethodDescriptor;
import com.cfx.service.client.api.ServiceClientContext;
import com.cfx.service.client.api.ServiceInvoker;
import com.cfx.service.client.api.Session;
import io.macaw.quickstart.counter.Counter;
import io.macaw.quickstart.issue.tracker.Issue;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link io.macaw.quickstart.issue.tracker.IssueTracker} service which creates and stores the issues in-memory
 */
public class IssueTracker implements com.cfx.service.api.Service, io.macaw.quickstart.issue.tracker.IssueTracker {

    private static final String ISSUE_ID_PREFIX = "TEST-";
    private static final String COUNTER_SERVICE_NAMESPACE = "io.macaw.services";
    private static final String COUNTER_SERVICE_NAME = "counter";
    private static final String USER_MANAGEMENT_SERVICE_NAMESPACE = "io.macaw.services";
    private static final String USER_MANAGEMENT_SERVICE_NAME = "user-management";

    private final ConcurrentHashMap<String, Issue> issues = new ConcurrentHashMap<>();

    @Inject
    private ServiceClientContext serviceClientContext;

    @Override
    public void initialize(com.cfx.service.api.config.Configuration config) throws com.cfx.service.api.ServiceException {
    }

    @Override
    public void start(com.cfx.service.api.StartContext startContext) throws com.cfx.service.api.ServiceException {
    }

    @Override
    public void stop(com.cfx.service.api.StopContext stopContext) throws com.cfx.service.api.ServiceException {
    }

    @Override
    public Issue createIssue(final io.macaw.quickstart.issue.tracker.Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue, that's being created, cannot be null");
        }
        if (isNullOrEmpty(issue.getSummary())) {
            throw new IllegalArgumentException("Issue, that's being created, should have a summary");
        }
        if (issue.getType() == null) {
            throw new IllegalArgumentException("Issue type is missing on the issue being created");
        }
        if (isNullOrEmpty(issue.getReporter())) {
            throw new IllegalArgumentException("Issue, that's being created, is missing the id of the user who reported it");
        }
        // get hold of the session
        final Session session = this.serviceClientContext.getInvocationContextSession();
        // we first make sure that the reporter indeed is a valid user known to our system
        if (!isValidUserAccount(session, issue.getReporter())) {
            throw new IllegalArgumentException("Issue reporter " + issue.getReporter() + " does not have a user account");
        }
        // create a new issue id, use the counter service to get a new id.
        // lookup the counter service, whose interface, we have in our static classpath
        final Counter counter = this.serviceClientContext.getServiceLocator().locateService(session, Counter.class, COUNTER_SERVICE_NAMESPACE, COUNTER_SERVICE_NAME);
        // get the next id
        final long nextIssueId = counter.increment();
        final String issueId = ISSUE_ID_PREFIX + nextIssueId;
        // add it to our in-memory issues store
        if (this.issues.putIfAbsent(issueId, issue) != null) {
            // this shouldn't ever happen, since we assign the issue ids
            throw new RuntimeException("An issue already exists with id " + issueId);
        }
        issue.setId(issueId);
        return issue;
    }

    @Override
    public void createAccount(final String userId, final String firstName, final String lastName) {
        final Session session = this.serviceClientContext.getInvocationContextSession();
        // lookup the user management service and invoke on it in a "detyped" way (i.e. we *don't* require the interfaces
        // of the user management service, statically in our classpath)
        final ServiceInvoker serviceInvoker = this.serviceClientContext.getServiceLocator().locateServiceInvoker(session, USER_MANAGEMENT_SERVICE_NAMESPACE, USER_MANAGEMENT_SERVICE_NAME);
        // invoke the API to create the user, on the user management service
        final String apiMethodName = "createUser";
        final String[] apiMethodArgTypes = new String[]{String.class.getName(), String.class.getName(), String.class.getName()};
        final String[] apiMethodArgs = new String[]{userId, firstName, lastName};
        try {
            serviceInvoker.invoke(apiMethodName, new JSONMethodDescriptor(apiMethodArgTypes, apiMethodArgs));
        } catch (Exception e) {
            throw new RuntimeException("Failed to assert validity of user account of user " + userId, e);
        }
    }

    @Override
    public io.macaw.quickstart.issue.tracker.Issue getIssue(final String issueId) {
        if (isNullOrEmpty(issueId)) {
            throw new IllegalArgumentException("Issue id cannot be null or empty");
        }
        return this.issues.get(issueId);
    }

    private static boolean isNullOrEmpty(final String val) {
        return val == null || val.trim().isEmpty();
    }

    private boolean isValidUserAccount(final Session session, final String userId) {
        // lookup the user management service and invoke on it in a "detyped" way (i.e. we *don't* require the interfaces
        // of the user management service, statically in our classpath)
        final ServiceInvoker serviceInvoker = this.serviceClientContext.getServiceLocator().locateServiceInvoker(session, USER_MANAGEMENT_SERVICE_NAMESPACE, USER_MANAGEMENT_SERVICE_NAME);
        // invoke the API to fetch the user, on the user management service
        final String apiMethodName = "getUser";
        final String[] apiMethodArgTypes = new String[]{String.class.getName()};
        final String[] apiMethodArgs = new String[]{userId};
        final String invocationResult;
        try {
            // our detyped invocation is of JSON format, the return result is going to be a String type
            invocationResult = (String) serviceInvoker.invoke(apiMethodName, new JSONMethodDescriptor(apiMethodArgTypes, apiMethodArgs));
        } catch (Exception e) {
            throw new RuntimeException("Failed to assert validity of user account of user " + userId, e);
        }
        return invocationResult != null && !invocationResult.equals("null");
    }
}
