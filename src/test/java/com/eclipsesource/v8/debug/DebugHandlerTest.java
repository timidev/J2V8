/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;

public class DebugHandlerTest {

    private static String script = "// 1 \n"
            + "function foo() {     // 2 \n"
            + "  var x = 7;         // 3 \n"
            + "  var y = x + 1;     // 4 \n"
            + "}                    // 5 \n"
            + "foo();               // 6 \n";
    private V8            v8;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testCreateDebugHandler() {
        DebugHandler handler = new DebugHandler(v8);

        assertNotNull(handler);
        handler.release();
    }

    @Test
    public void testDebugEvents() {
        DebugHandler handler = new DebugHandler(v8);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.setScriptBreakpoint("script", 3);
        handler.addBreakHandler(breakHandler);

        v8.executeScript(script, "script", 0);

        verify(breakHandler).onBreak(eq(DebugEvent.BeforeCompile), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        verify(breakHandler).onBreak(eq(DebugEvent.AfterCompile), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        verify(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
    }

    @Test
    public void testSetBreakpoint() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(1)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
    }

    @Test
    public void testClearBreakpoint() {
        DebugHandler handler = new DebugHandler(v8);
        int breakpointID = handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        handler.clearBreakPoint(breakpointID);

        v8.executeScript(script, "script", 0);
        V8Array breakpoints = handler.getScriptBreakPoints();
        verify(breakHandler, times(0)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        assertEquals(0, breakpoints.length());
        breakpoints.release();
        handler.release();
    }


    @Test
    public void testGetBreakpoints() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        V8Array breakpoints = handler.getScriptBreakPoints();

        assertEquals(1, breakpoints.length());
        V8Object breakpoint = breakpoints.getObject(0);
        assertEquals(1, breakpoint.executeIntegerFunction("number", null));
        breakpoint.release();
        breakpoints.release();
        handler.release();
    }

    @Test
    public void testDisableBreakpoint() {
        DebugHandler handler = new DebugHandler(v8);
        int breakpointID = handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);
        handler.disableScriptBreakPoint(breakpointID);

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(0)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
    }

    @Test
    public void testDisableAllBreakpoints() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        handler.disableAllBreakPoints();

        v8.executeScript(script, "script", 0);
        verify(breakHandler, times(0)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
    }

    @Test
    public void testEnableBreakpoint() {
        DebugHandler handler = new DebugHandler(v8);
        int breakpointID = handler.setScriptBreakpoint("script", 3);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);
        handler.disableScriptBreakPoint(breakpointID);
        handler.enableScriptBreakPoint(breakpointID);

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(1)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
    }

    @Test
    public void testSetBreakpointReturnsID() {
        DebugHandler handler = new DebugHandler(v8);

        int breakpointID = handler.setScriptBreakpoint("script", 3);

        assertEquals(1, breakpointID);
        handler.release();
    }

    @Test
    public void testSetBreakpointByFunction() {
        DebugHandler handler = new DebugHandler(v8);
        v8.executeScript(script, "script", 0);
        V8Function function = (V8Function) v8.get("foo");
        handler.setBreakpoint(function);
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        function.call(null, null);

        verify(breakHandler, times(1)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
        handler.release();
        function.release();
    }

    @Test
    public void testSetBreakpointByFunctionReturnsID() {
        DebugHandler handler = new DebugHandler(v8);
        v8.executeScript(script, "script", 0);
        V8Function function = (V8Function) v8.get("foo");

        int breakpointID = handler.setBreakpoint(function);

        assertEquals(1, breakpointID);
        handler.release();
        function.release();
    }

    @Test
    public void testRemoveBreakHandlerBeforeSet() {
        DebugHandler handler = new DebugHandler(v8);
        BreakHandler breakHandler = mock(BreakHandler.class);

        handler.removeBreakHandler(breakHandler); // Test should not throw NPE
        handler.release();
    }

}
