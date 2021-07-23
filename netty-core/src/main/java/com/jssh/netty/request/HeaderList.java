package com.jssh.netty.request;

import java.util.ArrayList;
import java.util.List;

public class HeaderList {

    private List<Header> headers;

    public HeaderList() {
        this.headers = new ArrayList<>();
    }

    public HeaderList(HeaderList headerList) {
        this.headers = new ArrayList<>(headerList.getHeaders());
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return headers != null && headers.size() > 0 ? "{" +
                headers +
                '}' : "";
    }
}
