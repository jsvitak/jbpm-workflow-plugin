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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.drools.persistence.jta.JtaTransactionManager;

/**
 * 
 * Own transaction manager to enable correct persistence on JBoss AS 7.
 * 
 * @author Jiri Svitak
 *
 */
public class PluginTransactionManager {

    public static final String JBOSS_USER_TRANSACTION_NAME = "java:jboss/UserTransaction";
    
    private static UserTransaction findUserTransaction() {
        try {
            InitialContext context = new InitialContext();
            return (UserTransaction) context.lookup( JBOSS_USER_TRANSACTION_NAME );
        } catch ( NamingException ex ) {
            JbpmPluginLogger.debug(ex);
            throw new IllegalStateException("Unable to find transaction: " + ex.getMessage(), ex);
        }
    }
    
    public static JtaTransactionManager getTransactionManager() {
        return new JtaTransactionManager(findUserTransaction(), null, null);
    }
}
