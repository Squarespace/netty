package io.netty.buffer;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Caller {

    private static final AtomicLong COUNTER = new AtomicLong();
    
    static long timeStamp() {
        return COUNTER.incrementAndGet();
    }
    
    private final long timeStamp = timeStamp();
    
    private final List<Caller> callers = new ArrayList<Caller>(2);
    
    private final Caller parent;
    
    private final Exception stack;
    
    private final String before;
    
    private String after;
    
    private boolean done = false;
    
    public Caller(String before) {
        this(before, null);
    }
    
    public Caller(String before, Caller parent) {
        this.parent = parent;
        
        this.before = before;
        this.stack = new Exception();
    }
    
    public void add(Caller caller) {
        synchronized (Caller.class) {
            if (done) {
                throw new IllegalStateException("caller=" + caller);
            }
            
            callers.add(caller);
        }
    }
    
    public void after(String after) {
        this.after = after;
    }
    
    @Override
    public String toString() {
        synchronized (Caller.class) {
            done = true;
            
            return "--- BEGIN ---\n" + toString(0) + "--- END ---\n";
        }
    }
    
    private String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        
        if (parent != null) {
            
            String prefix = indent(indent);
            
            indent += 2;
            
            sb.append(prefix).append("--- BEGIN PARENT ---\n")
                .append(parent.toString(indent))
                .append(prefix).append("--- END PARENT ---\n");
        }
        
        sb.append(indent(indent)).append(timeStamp).append('\n');
        sb.append(indent(indent)).append("BEFORE: ").append(before).append('\n');
        sb.append(indent(indent)).append("AFTER: ").append(after).append('\n');
        sb.append(toString(stack, indent)).append('\n');
        
        for (Caller element : callers) {
            sb.append(element.toString(indent + 2)).append('\n');
        }

        return sb.toString();
    }
    
    private static String toString(Throwable t, int indent) {
        try {
            StringWriter sw = new StringWriter();
            try {
                PrintWriter pw = new PrintWriter(sw);
                try {
                    t.printStackTrace(pw);
                } finally {
                    pw.close();
                }
            } finally {
                sw.close();
            }
            
            String prefix = indent(indent);
            
            BufferedReader in = new BufferedReader(new StringReader(sw.toString()));
            try {
                
                StringBuilder sb = new StringBuilder();
                
                String line = null;
                while ((line = in.readLine()) != null) {
                    sb.append(prefix).append(line).append('\n');
                }
                
                // Remove the trailing newline
                if (sb.length() > 0) {
                    sb.setLength(sb.length()-1);
                }
                
                return sb.toString();
                
            } finally {
                in.close();
            }
        } catch (Exception err) {
            throw new IllegalStateException(err);
        }
       
    }
    
    private static String indent(int indent) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
        
        return sb.toString();
    }
}
