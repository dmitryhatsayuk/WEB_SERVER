package org.example;

public class Request {
    public Request(String meth, String path, String headers, String body) {
        this.meth = meth;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    protected final String meth;
    protected final String path;
    protected final String headers;
    protected final String body;

    //для тестов метод toString для класса переопределен
    @Override
    public String toString() {

        return meth + " " + path + "\n" + headers + "\n" + body;
    }
}
