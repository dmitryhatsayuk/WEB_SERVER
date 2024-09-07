package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс для создания объектов на основе http запросов
 */
public class Request {

    public Request(String meth, String path, String headers, String body, String queryString) {
        this.meth = meth;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.queryString = queryString;
        this.qParams = URLEncodedUtils.parse(URI.create(queryString), StandardCharsets.UTF_8.name());
    }

    protected final String meth;
    protected final String path;
    protected final String headers;
    protected final String body;
    protected final String queryString;
    protected final List<NameValuePair> qParams;


    //для тестов метод toString для класса переопределен
    @Override
    public String toString() {
        return meth + " " + path + "\n" + headers + "\n" + body;
    }

    /**
     * <p>Метод возвращающий значение параметра QueryString</p>
     * Метод мапит лист NameValuePir-ов в простую мапу и ищет по ней значение по ключу
     *
     * @param pName название параметра из QueryString
     * @return Возвращает значение параметра QueryString в виде Strind
     */
    public String getQueryParam(String pName) {
        Map<String, String> pMap = qParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        return pMap.get(pName);
    }

    /**
     * <p>Метод возвращающий содержимое QueryString в виде List</p>
     *
     * @return Метод возвращает целиком содержимое QueryString в виде List<NameValuePair>
     */
    public List<NameValuePair> getQueryParams() {
        return qParams;
    }


}