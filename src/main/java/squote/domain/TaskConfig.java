package squote.domain;

import org.springframework.data.annotation.Id;

public record TaskConfig(@Id String taskName, String jsonConfig) {}
