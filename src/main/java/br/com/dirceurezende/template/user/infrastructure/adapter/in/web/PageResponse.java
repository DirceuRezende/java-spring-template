package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.user.application.port.in.PageResult;
import br.com.dirceurezende.template.user.application.port.in.UserView;

import java.util.List;

public record PageResponse(List<UserResponse> items, int page, int size, long total) {

    static PageResponse from(PageResult<UserView> result) {
        var page = result.map(UserResponse::from);
        return new PageResponse(page.items(), page.page(), page.size(), page.total());
    }
}
