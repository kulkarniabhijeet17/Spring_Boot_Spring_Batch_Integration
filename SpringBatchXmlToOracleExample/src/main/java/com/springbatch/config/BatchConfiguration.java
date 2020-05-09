package com.springbatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.springbatch.listener.JobCompletionNotificationListener;
import com.springbatch.model.Record;
import com.springbatch.processor.XmlRecordProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Bean
	ItemReader<Record> reader() {
		StaxEventItemReader<Record> xmlFileReader = new StaxEventItemReader<Record>();
		xmlFileReader.setResource(new ClassPathResource("input/record_dump.xml"));
		xmlFileReader.setFragmentRootElementName("record");

		Jaxb2Marshaller recordMarshaller = new Jaxb2Marshaller();
		recordMarshaller.setClassesToBeBound(Record.class);
		xmlFileReader.setUnmarshaller(recordMarshaller);

		return xmlFileReader;
	}

	@Bean
	public XmlRecordProcessor processor() {
		return new XmlRecordProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Record> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Record>()
				.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Record>())
				.sql("insert into RECORD_DUMP_TBL(COUNTRY_CODE,COUNTRY_NAME,COUNTRY_ISO_CODE,REGION_CODE,USER_CREATED,DATE_CREATED,STATE_CODE,STATE_NAME) values (:countryCode, :countryName, :countryIsdCode, :regionCode, :userCreated, :dateCreated, :stateCode, :stateName)")
				.dataSource(dataSource).build();
	}

	@Bean
	public Job importJob(JobCompletionNotificationListener listener, Step step1) {
		return jobBuilderFactory.get("importJob").incrementer(new RunIdIncrementer()).listener(listener).flow(step1)
				.end().build();
	}

	@Bean
	public Step step1(JdbcBatchItemWriter<Record> writer) {
		return stepBuilderFactory.get("step1").<Record, Record>chunk(10).reader(reader()).processor(processor())
				.writer(writer).build();
	}
}