package org.opengroup.osdu.entitlements.v2.errors;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@RequestMapping("/error")
@Controller
public class GlobalErrorController implements ErrorController {

    @GetMapping
    @ResponseBody
    public String handleErrorGet(HttpServletRequest request) {
        return getString(request);
    }

    @DeleteMapping
    @ResponseBody
    public String handleErrorDelete(HttpServletRequest request) {
        return getString(request);
    }

    @PostMapping
    @ResponseBody
    public String handleErrorPost(HttpServletRequest request) {
        return getString(request);
    }

    @PatchMapping
    @ResponseBody
    public String handleErrorPatch(HttpServletRequest request) {
        return getString(request);
    }

    @PutMapping
    @ResponseBody
    public String handleErrorPut(HttpServletRequest request) {
        return getString(request);
    }

    private String getString(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
        String err = "";
        if (statusCode == 500) {
            err = "An unknown error has occurred";
        } else {

            err = exception == null ? "An unknown error has occurred" : exception.getMessage();
        }

        return String.format("{\"code\": %s, \"reason:\": \"%s\" \"message\":\"%s\" }",
                statusCode, HttpStatus.resolve(statusCode).getReasonPhrase(), err);
    }

    public String getErrorPath() {
        return "/error";
    }
}