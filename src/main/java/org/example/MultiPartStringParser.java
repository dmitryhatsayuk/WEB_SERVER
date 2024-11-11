package org.example;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс для парсинга тела запросов http
 * <p>
 * Честно добыт с просторов StackOverflow и доработан,
 * поскольку изначально синтаксис методов был мне не понятен
 */
public class MultiPartStringParser implements org.apache.tomcat.util.http.fileupload.UploadContext {


    private final String postBody;
    private final String boundary;
    private final Map<String, String> parameters = new HashMap<>();

    /**
     * <p>Конструктор объектов класса MultiPartStringParser</p>
     * <p>
     * Конструктор создает объекты на основе содержимого тела полученного запроса
     *
     * @param postBody содержимое тела запроса в виде строки
     */
    public MultiPartStringParser(String postBody) throws Exception {
        this.postBody = postBody;
        this.boundary = postBody.substring(2, postBody.indexOf('\n')).trim();
        final FileItemFactory factory = new DiskFileItemFactory();
        FileUpload upload = new FileUpload(factory);
        List<FileItem> fileItems = upload.parseRequest(this);
        for (FileItem fileItem : fileItems) {
            if (fileItem.isFormField()) {
                parameters.put(fileItem.getFieldName(), fileItem.getString());
            }
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8"; // You should know the actual encoding.
    }

    @Override
    public String getContentType() {
        return "multipart/form-data, boundary=" + this.boundary;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(postBody.getBytes());
    }

    @Override
    public long contentLength() {
        return postBody.length();
    }
}