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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;

public class SessionUtil {

    public static KnowledgeBase getKnowledgeBase(String url) {
        Properties droolsProperties = new Properties();
        droolsProperties.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory
                .newKnowledgeBuilderConfiguration(droolsProperties);

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
                .newKnowledgeBuilder(config);

        Authenticator.setDefault(GuvnorAuthenticator.getInstance());

        kbuilder.add(ResourceFactory.newUrlResource(url), ResourceType.BPMN2);
        if (kbuilder.hasErrors()) {
            Logger.log("Failed to build a business process definition from location "
                    + url.toString() + " due to the following reasons:");
            Logger.log(kbuilder.getErrors().toString());
            return null;
        }

        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }

    public static StatefulKnowledgeSession getStatefulKnowledgeSession(
            KnowledgeBase kbase) {
        StatefulKnowledgeSession ksession = null;
        if ("true".equalsIgnoreCase(PropertiesManager.getPluginProperties().getProperty(
                "persistence.enabled", "false"))) {
            EntityManagerFactory emf = Persistence
                    .createEntityManagerFactory("jwp");
            Environment env = KnowledgeBaseFactory.newEnvironment();
            env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
            env.set(EnvironmentName.TRANSACTION_MANAGER,
                    com.arjuna.ats.jta.TransactionManager.transactionManager());
            ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase,
                    null, env);
        } else {
            ksession = kbase.newStatefulKnowledgeSession();
        }

        return ksession;
    }

    public static class PropertiesManager {

        private static Properties pluginProperties = null;

        public static Properties getPluginProperties() {
            if (pluginProperties == null) {
                Properties pluginProperties = new Properties();
                try {
                    FileInputStream in;
                    in = new FileInputStream("plugin.properties");
                    pluginProperties.load(in);
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return pluginProperties;
        }
    }

    public static class GuvnorAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            Properties pluginProperties = PropertiesManager
                    .getPluginProperties();
            return new PasswordAuthentication(pluginProperties.getProperty(
                    "guvnor.user", "admin"), pluginProperties.getProperty(
                    "guvnor.password", "admin").toCharArray());
        }

        public static GuvnorAuthenticator getInstance() {
            return new GuvnorAuthenticator();
        }
    }
}
