/*
 * The MIT License
 *
 * Copyright (c) 2012, Jiri Svitak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.jbpm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import jenkins.model.Jenkins;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.core.util.StringUtils;
import org.drools.io.ResourceFactory;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.persistence.JpaProcessPersistenceContextManager;

/**
 * 
 * This class handles loading business process definitions from Guvnor and manages jBPM sessions.
 * 
 * @author Jiri Svitak
 *
 */
public class SessionUtil {

    private static final String SESSION_ID_FILE = "jbpm-workflow-plugin-session-id.ser";

    private static class GuvnorAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            JbpmUrlResourceBuilder.DescriptorImpl desc = (JbpmUrlResourceBuilder.DescriptorImpl) Jenkins
                    .getInstance()
                    .getDescriptor(
                            org.jenkinsci.plugins.jbpm.JbpmUrlResourceBuilder.class);
            if (desc == null) {
                throw new IllegalStateException(
                        "Descriptor of JbpmUrlResourceBuilder is null!");
            }
            JbpmPluginLogger.info("Logging into Guvnor as a user "
                    + desc.getGuvnorUserName() + ".");
            return new PasswordAuthentication(desc.getGuvnorUserName(), desc
                    .getGuvnorPassword().toCharArray());
        }

        public static GuvnorAuthenticator getInstance() {
            return new GuvnorAuthenticator();
        }
    }

    public static KnowledgeBase getKnowledgeBase(String url) {
        Properties droolsProperties = new Properties();
        droolsProperties.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory
                .newKnowledgeBuilderConfiguration(droolsProperties);

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
                .newKnowledgeBuilder(config);

        Authenticator.setDefault(GuvnorAuthenticator.getInstance());
        JbpmPluginLogger.info("Logged into Gvunor successfully.");

        kbuilder.add(ResourceFactory.newUrlResource(url), ResourceType.BPMN2);
        if (kbuilder.hasErrors()) {
            JbpmPluginLogger.error(kbuilder.getErrors().toString());
            return null;
        }

        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }

    public static StatefulKnowledgeSession getStatefulKnowledgeSession(
            KnowledgeBase kbase) {
        StatefulKnowledgeSession ksession = null;
        JbpmUrlResourceBuilder.DescriptorImpl desc = (JbpmUrlResourceBuilder.DescriptorImpl) Jenkins
                .getInstance()
                .getDescriptor(
                        org.jenkinsci.plugins.jbpm.JbpmUrlResourceBuilder.class);
        if (desc == null) {
            throw new IllegalStateException(
                    "Descriptor of JbpmUrlResourceBuilder is null!");
        }
        if (desc.isPersistenceEnabled()) {
            JbpmPluginLogger.info("Business process persistence is enabled.");
            int ksessionId = getPersistedSessionId(System
                    .getProperty("jboss.server.temp.dir"));
            ksession = getPersistedSession(kbase, ksessionId);
            persistSessionId(System.getProperty("jboss.server.temp.dir"),
                    ksessionId);
        } else {
            JbpmPluginLogger.info("Business process persistence is disabled.");
            ksession = kbase.newStatefulKnowledgeSession();
        }
        return ksession;
    }

    private static StatefulKnowledgeSession getPersistedSession(
            KnowledgeBase kbase, int ksessionId) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();

        try {

            ClassLoader sessionUtilClassLoader = SessionUtil.class
                    .getClassLoader();
            Thread.currentThread()
                    .setContextClassLoader(sessionUtilClassLoader);

            EntityManagerFactory emf = Persistence
                    .createEntityManagerFactory("org.jbpm.persistence.jpa");
            Environment env = KnowledgeBaseFactory.newEnvironment();
            env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
            env.set(EnvironmentName.TRANSACTION_MANAGER,
                    PluginTransactionManager.getTransactionManager());
            env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER,
                    new JpaProcessPersistenceContextManager(env));

            boolean createNewKnowledgeSession = true;
            StatefulKnowledgeSession ksession = null;

            if (ksessionId > 0) {
                createNewKnowledgeSession = false;
                try {
                    JbpmPluginLogger.debug("Loading knowledge session with id "
                            + ksessionId);
                    ksession = JPAKnowledgeService
                            .loadStatefulKnowledgeSession(ksessionId, kbase,
                                    null, env);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    JbpmPluginLogger.error("Error loading knowledge session : "
                            + e.getMessage());
                    if (e instanceof IllegalStateException) {
                        Throwable cause = ((IllegalStateException) e)
                                .getCause();
                        if (cause instanceof InvocationTargetException) {
                            cause = cause.getCause();
                            String exceptionMsg = "Could not find session data for id "
                                    + ksessionId;
                            if (cause != null
                                    && exceptionMsg.equals(cause.getMessage())) {
                                createNewKnowledgeSession = true;
                            }
                        }
                    }

                    if (!createNewKnowledgeSession) {
                        String exceptionMsg = e.getMessage();
                        if (e.getCause() != null
                                && !StringUtils.isEmpty(e.getCause()
                                        .getMessage())) {
                            exceptionMsg = e.getCause().getMessage();
                        }
                        JbpmPluginLogger.error("Error loading session data: "
                                + exceptionMsg);
                        throw e;
                    }
                }
            }

            if (createNewKnowledgeSession) {
                env = KnowledgeBaseFactory.newEnvironment();
                env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
                env.set(EnvironmentName.TRANSACTION_MANAGER,
                        PluginTransactionManager.getTransactionManager());
                env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER,
                        new JpaProcessPersistenceContextManager(env));
                ksession = JPAKnowledgeService.newStatefulKnowledgeSession(
                        kbase, null, env);
                ksessionId = ksession.getId();
                JbpmPluginLogger.debug("Created new knowledge session with id "
                        + ksessionId);
            }
            return JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null,
                    env);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }

    }

    private static int getPersistedSessionId(String location) {
        File sessionIdStore = new File(location + File.separator
                + SESSION_ID_FILE);
        if (sessionIdStore.exists()) {
            Integer knownSessionId = null;
            FileInputStream fis = null;
            ObjectInputStream in = null;
            try {
                fis = new FileInputStream(sessionIdStore);
                in = new ObjectInputStream(fis);

                knownSessionId = (Integer) in.readObject();

                return knownSessionId.intValue();

            } catch (Exception e) {
                return 0;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }

        } else {
            return 0;
        }
    }

    private static void persistSessionId(String location, int ksessionId) {
        if (location == null) {
            return;
        }
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(location + File.separator
                    + SESSION_ID_FILE);
            out = new ObjectOutputStream(fos);
            out.writeObject(Integer.valueOf(ksessionId));
            out.close();
        } catch (IOException ex) {
            JbpmPluginLogger.warn(ex);
            ex.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
