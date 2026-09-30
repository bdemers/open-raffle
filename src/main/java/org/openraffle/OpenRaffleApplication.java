package org.openraffle;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme("open-raffle")
@Viewport("width=device-width, initial-scale=1")
public class OpenRaffleApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(OpenRaffleApplication.class, args);
    }
}
