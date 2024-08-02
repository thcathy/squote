package squote;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Profile("!dev")
public class SchedulingConfiguration {}
