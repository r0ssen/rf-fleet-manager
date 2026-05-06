package dk.rfg.fleetmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class DateTimeBindingConfig implements WebMvcConfigurer {

    private static final DateTimeFormatter DATE_TIME_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, OffsetDateTime.class, new Converter<String, OffsetDateTime>() {
            @Override
            public @Nullable OffsetDateTime convert(@Nullable String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                LocalDateTime localDateTime = LocalDateTime.parse(source.trim(), DATE_TIME_LOCAL);
                return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
            }
        });
        registry.addConverter(OffsetDateTime.class, String.class, new Converter<OffsetDateTime, String>() {
            @Override
            public String convert(OffsetDateTime source) {
                return source.toLocalDateTime().format(DATE_TIME_LOCAL);
            }
        });
    }
}

