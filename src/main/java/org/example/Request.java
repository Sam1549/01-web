package org.example;

import java.io.BufferedReader;


public class Request {
    private final String method;
    private final String path;
    private final BufferedReader body;

    public Request(String method, String path, BufferedReader body){
        this.method = method;
        this.path = path;
        this.body = body;
    }
    public String getMethod(){
        return method;
    }
    public String getPath(){
        return path;
    }
    @Override
    public String toString(){
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
