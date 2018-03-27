package uk.ac.ebi.subs.api.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;

/**
 * Controller with a few convenience redirects to expose the HAL browser shipped as static content.
 *
 * @author Oliver Gierke
 * @soundtrack Miles Davis - So what (Kind of blue)
 */
@BasePathAwareController
public class DocForwards {


    /**
     * Redirects to the actual {@code index.html}.
     *
     * @return
     */
    @RequestMapping(value = "/docs", method = RequestMethod.GET)
    public View browser1(HttpServletRequest request) {
        return getRedirectView(request,"/index.html");
    }

    /**
     * Redirects to the actual {@code index.html}.
     *
     * @return
     */
    @RequestMapping(value = "/docs/", method = RequestMethod.GET)
    public View browser2(HttpServletRequest request) {
        return getRedirectView(request,"index.html");
    }

    /**
     * Returns the View to redirect to to access the HAL browser.
     *
     * @param request must not be {@literal null}.
     * @return
     */
    private View getRedirectView(HttpServletRequest request,String path) {

        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);

        builder.path(path);

        return new RedirectView(builder.build().toUriString());
    }
}
