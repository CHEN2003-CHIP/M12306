package com.atustcchen.javaailongchain4j.Tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component("calculatorTools")
public class CalculatorTools {
    @Tool(name="加法运算")
    public long Sum(long a, long b) {
        return a + b;
    }
    @Tool
    public long Square(long a) {
        return a * a;
    }
    @Tool
    public double SquareRoot(double a) {
        return Math.sqrt(a);
    }
}
