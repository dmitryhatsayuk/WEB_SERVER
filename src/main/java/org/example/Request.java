package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс для создания объектов на основе http запросов с переданными в запросе параметрами
 */
public class Request {

    protected final String meth;
    protected final String path;
    protected final String headers;
    protected final String body;
    protected final String queryString;
    protected final List<NameValuePair> qParams;

    public Request(String meth, String path, String headers, String body, String queryString) {
        this.meth = meth;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.queryString = queryString;
        this.qParams = URLEncodedUtils.parse(URI.create(queryString), StandardCharsets.UTF_8.name());
    }

    //для тестов метод toString для класса переопределен
    @Override
    public String toString() {
        return meth + " " + path + "\n" + headers + "\n" + body;
    }

    /**
     * <p>Метод возвращающий значение параметра QueryString</p>
     * Метод мапит лист NameValuePair-ов в простую мапу и ищет по ней значение по ключу
     *
     * @param pName название параметра из QueryString
     * @return String
     */
    public String getQueryParam(String pName) {
        Map<String, String> pMap = qParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        return pMap.get(pName);
    }

    /**
     * <p>Метод возвращающий содержимое QueryString в виде List</p>
     *
     * @return List<NameValuePair>
     */
    public List<NameValuePair> getQueryParams() {
        return qParams;
    }

    /**
     * <p>Метод возвращающий содержимое Body в виде List</p>
     * <p>
     * Метод возвращает для всех запросов кроме GET значение параметра в Body
     * в случае если Body представлено в виде x-www-form-urlencoded
     *
     * @param name Название параметра
     * @return LinkedList
     */
    public List<String> getPostParam(String name) {
        if (!meth.equals("GET") && headers.contains("x-www-form-urlencoded")) {
            List<NameValuePair> list = getPostParams();
            List<String> result = new LinkedList<>();
            for (NameValuePair nameValuePair : list) {
                if (nameValuePair.getName().equals(name)) {
                    result.add(nameValuePair.getValue());
                }
            }

            return result;
        } else return null;
    }

    /**
     * <p>Метод возвращающий содержимое Body в виде List</p>
     * <p>
     * Метод возвращающий содержимое Body в виде List для всех запросов кроме GET
     * в случае если Body представлено в виде x-www-form-urlencoded
     *
     * @return List<NameValuePair>
     */
    public List<NameValuePair> getPostParams() {
        if (!meth.equals("GET") && headers.contains("x-www-form-urlencoded")) {
            return URLEncodedUtils.parse(URI.create("?" + body), "UTF-8");
        } else return null;
    }

}