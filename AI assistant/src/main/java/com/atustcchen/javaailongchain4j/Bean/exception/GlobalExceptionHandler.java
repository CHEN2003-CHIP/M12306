package com.atustcchen.javaailongchain4j.Bean.exception;


import com.atustcchen.javaailongchain4j.Bean.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Result handle(Exception e)
    {
        e.printStackTrace();
        return Result.fail();
    }

    @ExceptionHandler(value = LeaseException.class)
    @ResponseBody
    public Result handle(LeaseException e)
    {
        e.printStackTrace();
        return Result.fail(e.getCode(), e.getMessage());
    }

}
