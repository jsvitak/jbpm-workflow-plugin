jBPM 5 workflow plugin for Jenkins CI
=====================================
This plugin allows you to define order of launching of your Jenkins jobs
by using [jBPM](http://www.jboss.org/jbpm) workflow.
jBPM is a flexible lightweight business process suite based on the BPMN 2.0 standard.

Quick start
===========
Plugin is builded by Maven 3. It is also recommended to use Oracle JDK 6.

    $ mvn clean package -DskipTests

This will create `target` directory which contains our package `jbpm-workflow-plugin.hpi`.
This file needs to be deployed into `$JENKINS_HOME/plugins` directory. For testing
you can also download and launch testing Jenkins instance:

    $ mvn hpi:run -Djetty.port=8090

We should specify a different port than 8080, because
port 8080 will be likely used by jBPM applications like Drools Guvnor, Designer or jBPM console.

