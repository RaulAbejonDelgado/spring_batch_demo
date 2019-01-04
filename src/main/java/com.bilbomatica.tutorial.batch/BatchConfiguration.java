package com.bilbomatica.tutorial.batch;

import com.bilbomatica.tutorial.batch.pojo.Person;
import org.codehaus.jettison.json.JSONException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoConfig mongoConfig;

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;

    private String dinamicSlash = "//";

    private final String recordCollectionName = "person";

    @Bean
    public ItemReader<Person> dataReader() throws Exception {

        File xmlDocPath = null;
        try {
            xmlDocPath =  getFilePath();
        } catch (Exception e){
            e.printStackTrace();
        }

        File[] files = new File[xmlDocPath.listFiles().length];
        FlatFileItemReader item = new FlatFileItemReader();
        files = listFiles(xmlDocPath);

        ArrayList<Person> personas = new ArrayList<>();
        for (File f : files) {
            try {
                personas.add((Person) processXML2Object(f));
            } catch (Exception e){
                e.printStackTrace();
            }
            //xmlFileReader.

        }


//        reader.setResource(new ClassPathResource("sample-data.csv"));
//        reader.setLineMapper(new DefaultLineMapper<Person>() {{
//            setLineTokenizer(new DelimitedLineTokenizer() {{
//                setNames(new String[] { "firstName", "lastName" });
//            }});
//            setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
//                setTargetType(Person.class);
//            }});
//        }});
        return personas;
    }

//    @Bean
//    public PersonItemProcessor processor() {
//        return new PersonItemProcessor();
//    }

    @Bean
    public MongoItemWriter<Person> writer() {

        MongoItemWriter<Person> writer = new MongoItemWriter<Person>();
        writer.setCollection(recordCollectionName);
        try {

            writer.setTemplate(mongoConfig.mongoTemplate());
            writer.write((List<? extends Person>) dataReader());


        } catch (Exception e) {

            e.printStackTrace();

        }

        return writer;
    }

    @Bean
    public Job importUserJob(JobCompletionNotificationListener listener) throws Exception {
        return jobBuilderFactory.get("importUserJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1())
                .end()
                .build();
    }

//    @Bean
//    public Step step0() throws Exception {
//        return stepBuilderFactory.get("step1")
//                .<File[], File[]>chunk(1)
//                .reader((ItemReader<? extends Person>) dataReader())
//                //.processor(processor())
//                .writer(writer(dataReader()))
//                .build();
//    }
//    // end::jobstep[]

    @Bean
    public Step step1() throws Exception {
        return stepBuilderFactory.get("step1")
                .<Person, Person>chunk(1)
                .reader(dataReader())
                .writer(writer())
                .build();
    }
    // end::jobstep[]


    // no parameter method for creating the path to our xml file
    private File getFilePath() throws JAXBException {

        File directorio = null;

        directorio = new File(System.getProperty("user.dir") + dinamicSlash + "personas" + dinamicSlash );

        return directorio;
    }

    private static File[] listFiles(File carpeta) {
        File[] archivos = new File[carpeta.listFiles().length];
        int contador = 0;
        for (final File ficheroEntrada : carpeta.listFiles()) {
            if (ficheroEntrada.isDirectory()) {
                listFiles(ficheroEntrada);
            } else {
                if(contador <  carpeta.listFiles().length){
                    archivos[contador] =  ficheroEntrada;
                    contador++;
                }
                System.out.println(ficheroEntrada.getName());
            }
        }
        return archivos;
    }

    // takes a parameter of xml path and returns json as a string
    private Object processXML2Object(File xmlDocPath) throws JSONException, JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(Person.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Person pe = (Person) jaxbUnmarshaller.unmarshal(xmlDocPath);

        return pe;
    }

    // inserts to our mongodb
    private void insertToMongo(ArrayList<Person> objetos){
        //Document doc = Document.parse(jsonString);
        //Person p = (Person) jsonString;
        for(Person p : objetos){
            mongoTemplate.insert(p, recordCollectionName);
        }
    }

}
