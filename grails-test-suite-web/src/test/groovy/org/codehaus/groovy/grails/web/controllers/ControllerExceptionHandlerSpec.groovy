package org.codehaus.groovy.grails.web.controllers

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import java.sql.BatchUpdateException
import java.sql.SQLException

import javax.xml.soap.SOAPException

import spock.lang.Issue
import spock.lang.Specification

@TestFor(ErrorHandlersController)
class ControllerExceptionHandlerSpec extends Specification {

    void 'Test exception handler which renders a String'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testAction()

        then:
        response.contentAsString == 'A SQLException Was Handled'
    }

    void 'Test exception handler which renders a String from command object action'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testActionWithCommandObject()

        then:
        response.contentAsString == 'A SQLException Was Handled'
    }

    @Issue('GRAILS-11095')
    void 'Test passing command object as argument to action'() {
        when:
        controller.testActionWithCommandObject(new MyCommand(exceptionToThrow: 'java.sql.SQLException'))

        then:
        response.contentAsString == 'A SQLException Was Handled'
    }

    void 'Test exception handler which renders a String from command object closure action'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testClosureActionWithCommandObject()

        then:
        response.contentAsString == 'A SQLException Was Handled'
    }

    void 'Test exception handler which renders a String from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.sql.SQLException'
        controller.testActionWithNonCommandObjectParameter()

        then:
        response.contentAsString == 'A SQLException Was Handled'
    }

    void 'Test exception handler which issues a redirect'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testAction()

        then:
        response.redirectedUrl == '/logging/batchProblem'
    }

    void 'Test exception handler which issues a redirect from a command object action'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testActionWithCommandObject()

        then:
        response.redirectedUrl == '/logging/batchProblem'
    }

    void 'Test exception handler which issues a redirect from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.sql.BatchUpdateException'
        controller.testActionWithNonCommandObjectParameter()

        then:
        response.redirectedUrl == '/logging/batchProblem'
    }

    void 'Test exception handler which returns a model'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testAction()

        then:
        model.problemDescription == 'A Number Was Invalid'
    }

    void 'Test exception handler which returns a model from a command object action'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testActionWithCommandObject()

        then:
        model.problemDescription == 'A Number Was Invalid'
    }

    void 'Test exception handler which returns a model from action with typed parameter'() {
        when:
        params.exceptionToThrow = 'java.lang.NumberFormatException'
        def model = controller.testActionWithNonCommandObjectParameter()

        then:
        model.problemDescription == 'A Number Was Invalid'
    }

    void 'Test throwing an exception that does not have a handler'() {
        when:
        params.exceptionToThrow = 'javax.xml.soap.SOAPException'
        def model = controller.testActionWithNonCommandObjectParameter()

        then:
        thrown SOAPException
    }

    void 'Test throwing an exception that does not have a handler and does match a private method in the parent controller'() {
        when: 'a controller action throws an exception which matches an inherited private method which should not be treated as an exception handler'
        params.exceptionToThrow = 'java.io.IOException'
        def model = controller.testActionWithNonCommandObjectParameter()

        then: 'the method is ignored and the exception is thrown'
        thrown IOException
    }

    void 'Test action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testAction()

        then:
        thrown UnsupportedOperationException
    }

    void 'Test command object action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testActionWithCommandObject()

        then:
        thrown UnsupportedOperationException
    }

    void 'Test typed parameter action throws an exception that does not have a corresponding error handler'() {
        when:
        params.exceptionToThrow = 'java.lang.UnsupportedOperationException'
        controller.testActionWithNonCommandObjectParameter()

        then:
        thrown UnsupportedOperationException
    }

    @Issue('GRAILS-10866')    
    void 'Test exception handler for an Exception class written in Groovy'() {
        when:
        params.exceptionToThrow = MyException.name
        controller.testActionWithNonCommandObjectParameter()

        then:
        response.contentAsString == 'MyException was thrown'
    }
}

@Artefact('Controller')
abstract class SomeAbstractController {
    
    private somePrivateMethodWhichIsNotAnExceptionHandler(IOException e) {
    }
}

@Artefact('Controller')
class ErrorHandlersController extends SomeAbstractController {

    def testAction() {
        def exceptionClass = Class.forName(params.exceptionToThrow)
        throw exceptionClass.newInstance()
    }

    def testActionWithCommandObject(MyCommand co) {
        def exceptionClass = Class.forName(co.exceptionToThrow)
        throw exceptionClass.newInstance()
    }

    def testClosureActionWithCommandObject = { MyCommand co ->
        def exceptionClass = Class.forName(co.exceptionToThrow)
        throw exceptionClass.newInstance()
    }

    def testActionWithNonCommandObjectParameter(String exceptionToThrow) {
        def exceptionClass = Class.forName(exceptionToThrow)
        throw exceptionClass.newInstance()
    }

    def handleSQLException(SQLException e) {
        render 'A SQLException Was Handled'
    }

    // BatchUpdateException extends SQLException
    def handleSQLException(BatchUpdateException e) {
        redirect controller: 'logging', action: 'batchProblem'
    }

    def handleNumberFormatException(NumberFormatException nfe) {
        [problemDescription: 'A Number Was Invalid']
    }
    
    def handleSomeGroovyException(MyException e) {
        render 'MyException was thrown'
    }
}

class MyCommand {
    String exceptionToThrow
}

class MyException extends Exception {
    
}