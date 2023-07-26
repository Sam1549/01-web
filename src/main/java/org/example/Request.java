package org.example;

import org.apache.http.NameValuePair;

import java.util.List;


public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;
    private List<NameValuePair> QueryParams;

    public Request(String method, String path, List<String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    public void setQueryParams(List<NameValuePair> nameValuePairs) {
        this.QueryParams = nameValuePairs;
    }

    public NameValuePair getQueryParam(String name){
        return this.QueryParams.stream().filter(s -> s.getName().equals(name)).findFirst().get();
    }
    public List<NameValuePair> getQueryParams(){
        return this.QueryParams;
    }
    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }



    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", QueryParams=" + QueryParams +
                '}';
    }
}
