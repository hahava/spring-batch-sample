package me.kalin.batch.feat.userregistration.job;

import lombok.RequiredArgsConstructor;
import me.kalin.batch.feat.userregistration.listener.UserRegistrationWriterListener;
import me.kalin.batch.feat.userregistration.model.UserRegistration;
import me.kalin.batch.feat.userregistration.reader.UserRegistrationReader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class UserRegistrationSavingJob {
    private static final int CHUNK_SIZE = 10;
    private static final int SAMPLE_USER_SIZE = 100;
    private static final String CSV = ".csv";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Value("${user.registration.path}")
    private String fileSavingPath;

    @Bean
    public Job writeUserRegistrationInfos() {
        return jobBuilderFactory
                .get("writeUserRegistrationInfo")
                .start(userRegistrationStep())
                .validator(userRegistrationSavingParameter())
                .build();
    }

    @Bean
    public Step userRegistrationStep() {
        return stepBuilderFactory
                .get("userRegistrationStep")
                .<UserRegistration, UserRegistration>chunk(CHUNK_SIZE)
                .reader(new UserRegistrationReader(SAMPLE_USER_SIZE))
                .writer(writeToCsv(null))
                .listener(new UserRegistrationWriterListener<>())
                .build();
    }

    @StepScope
    @Bean
    public FlatFileItemWriter<UserRegistration> writeToCsv(@Value("#{jobParameters['fileName']}") String fileName) {
        return new FlatFileItemWriterBuilder<UserRegistration>()
                .name("writeToCsv")
                .encoding(StandardCharsets.UTF_8.name())
                .resource(new FileSystemResource(getFileName(fileName)))
                .append(true)
                .lineAggregator(new PassThroughLineAggregator<>())
                .headerCallback(writer -> writer.write(String.join(",", UserRegistration.headerName())))
                .build();
    }

    @Bean
    public JobParametersValidator userRegistrationSavingParameter() {
        DefaultJobParametersValidator defaultJobParametersValidator = new DefaultJobParametersValidator();
        defaultJobParametersValidator.setRequiredKeys(new String[]{"fileName"});
        defaultJobParametersValidator.afterPropertiesSet();
        return defaultJobParametersValidator;
    }

    private String getFileName(String fileName) {
        return StringUtils.appendIfMissing(fileSavingPath.concat(fileName), CSV);
    }
}
