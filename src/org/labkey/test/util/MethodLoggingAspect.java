/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * User: tchadick
 * Date: 10/1/12
 * Time: 3:17 PM
 */

@Aspect
public class MethodLoggingAspect
{
    private static Stack<Long> startTimes = new Stack<>();
    private static Stack<String> methodStack = new Stack<>();
    private static Stack<String> quietMethods = new Stack<>();
    private static Stack<String> quietMethodsArgStrings = new Stack<>();

    @Pointcut(value = "execution(@org.labkey.test.util.LogMethod * *(..))")
    void loggedMethod(){}

    @Before(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void beforeLoggedMethod(JoinPoint joinPoint, LogMethod logMethod)
    {
        MethodSignature signature = (MethodSignature) joinPoint.getStaticPart().getSignature();
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        String method = signature.getName();
        methodStack.push(method);
        startTimes.push(System.currentTimeMillis());

        String argsString = "";

        Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++)
        {
            Annotation[] methodAnnotations = annotations[i];
            Boolean loggedParameter = false;
            for (Annotation annotation : methodAnnotations)
            {
                if (annotation instanceof LoggedParam)
                {
                    loggedParameter = true;
                    break;
                }
            }
            if (loggedParameter)
            {
                String argString = getArgString(joinPoint.getArgs()[i]);

                argsString += (argsString.length() > 0 ? ", " : "") + argString;
            }
        }

        argsString = (argsString.length() > 0 ? "(" + argsString + ")" : "");

        if (logMethod.quiet())
        {
            TestLogger.suppressLogging(true);
            quietMethods.add(method);
            quietMethodsArgStrings.add(argsString);
        }

        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            TestLogger.log(">>" + method + argsString);
            TestLogger.increaseIndent();
        }
    }

    @AfterReturning(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void afterLoggedMethod(JoinPoint joinPoint, LogMethod logMethod)
    {
        methodStack.pop(); // Discard current method
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        Long elapsed = System.currentTimeMillis()-startTimes.pop();
        String method = joinPoint.getStaticPart().getSignature().getName();

        String argString = " done";

        if (logMethod.quiet())
        {
            quietMethods.pop();
            argString = quietMethodsArgStrings.pop();
        }
        if (quietMethods.empty())
            TestLogger.suppressLogging(false);

        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            String elapsedStr = String.format("%dm %d.%ds",
                    TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                    elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
            TestLogger.decreaseIndent();
            TestLogger.log("<<" + method + argString + " [" + elapsedStr + "]"); // Only log on successful return
        }
    }

    @AfterThrowing(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void afterLoggedMethodException(JoinPoint joinPoint, LogMethod logMethod)
    {
        startTimes.pop();
        methodStack.pop();
        TestLogger.decreaseIndent();

        if (logMethod.quiet())
            quietMethods.pop();
        if (quietMethods.empty())
            TestLogger.suppressLogging(false);
    }

    private final int MAX_ARG_LENGTH = 30;
    private String getArgString(Object arg)
    {
        String argString = "";
        if (arg instanceof Object[])
        {
            for (Object nestedArg : (Object[])arg)
            {
                argString += (argString.length() > 0 ? ", " : "") + getArgString(nestedArg);
            }
            argString = "[" + argString + "]";
        }
        else if (arg instanceof Collection)
        {
            for (Object nestedArg : (Collection)arg)
            {
                argString += (argString.length() > 0 ? ", " : "") + getArgString(nestedArg);
            }
            argString = "[" + argString + "]";
        }
        else
        {
            if (arg != null)
            {
                argString = arg.toString();
                if (!(arg instanceof Number || arg instanceof Boolean))
                {
                    if (argString.length() > MAX_ARG_LENGTH)
                        argString = argString.substring(0, MAX_ARG_LENGTH - 3) + "...";
                    argString = "'" + argString + "'";
                }
            }
            else
                argString = "null";
        }
        return argString;
    }
}
