package uk.ac.ebi.subs.api.services;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Http{

    public HttpResponse<String> post(String url, Map<String,String> headers, String requestBody) throws UnirestException {
        return com.mashape.unirest.http.Unirest.post(url)
                .headers(headers)
                .body(requestBody)
                .asString();

    }

    public HttpResponse<String> get(String url, Map<String,String> headers) throws UnirestException {
        return com.mashape.unirest.http.Unirest.get(url)
                .headers(headers)
                .asString();
    }
}
