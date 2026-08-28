package storage.cloud.cloudstorage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForwardController {

    @GetMapping({"/files", "/files/", "/login", "/login/"})
    public String forwardToIndexPage() {
        return "forward:/index.html";
    }
}
