package it.unimol.newunimol.gestionecompiti.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name:newunimol.events}")
    private String exchangeName;

    @Value("${rabbitmq.queue.assignment.created:assignment.created}")
    private String assignmentCreatedQueue;

    @Value("${rabbitmq.queue.assignment.updated:assignment.updated}")
    private String assignmentUpdatedQueue;

    @Value("${rabbitmq.queue.assignment.deleted:assignment.deleted}")
    private String assignmentDeletedQueue;

    @Value("${rabbitmq.queue.submission.created:submission.created}")
    private String submissionCreatedQueue;

    @Value("${rabbitmq.queue.course.deleted:gestione-compiti.course.deleted}")
    private String courseDeletedQueue;

    @Value("${rabbitmq.queue.assessment.created:gestione-compiti.assessment.created}")
    private String assessmentCreatedQueue;

    @Value("${rabbitmq.queue.student.unenrolled:gestione-compiti.student.unenrolled}")
    private String studentUnenrolledQueue;

    // Routing keys
    public static final String ASSIGNMENT_CREATED = "assignment.created";
    public static final String ASSIGNMENT_UPDATED = "assignment.updated";
    public static final String ASSIGNMENT_DELETED = "assignment.deleted";
    public static final String SUBMISSION_CREATED = "submission.created";
    public static final String COURSE_DELETED_ROUTING_KEY = "course.deleted";
    public static final String ASSESSMENT_CREATED_ROUTING_KEY = "assessment.created";
    public static final String STUDENT_UNENROLLED_ROUTING_KEY = "student.unenrolled";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // Queue eventi in uscita

    @Bean
    public Queue assignmentCreatedQueue() {
        return QueueBuilder.durable(assignmentCreatedQueue).build();
    }

    @Bean
    public Queue assignmentUpdatedQueue() {
        return QueueBuilder.durable(assignmentUpdatedQueue).build();
    }

    @Bean
    public Queue assignmentDeletedQueue() {
        return QueueBuilder.durable(assignmentDeletedQueue).build();
    }

    @Bean
    public Queue submissionCreatedQueue() {
        return QueueBuilder.durable(submissionCreatedQueue).build();
    }

    @Bean
    public Binding assignmentCreatedBinding() {
        return BindingBuilder
                .bind(assignmentCreatedQueue())
                .to(exchange())
                .with(ASSIGNMENT_CREATED);
    }

    @Bean
    public Binding assignmentUpdatedBinding() {
        return BindingBuilder
                .bind(assignmentUpdatedQueue())
                .to(exchange())
                .with(ASSIGNMENT_UPDATED);
    }

    @Bean
    public Binding assignmentDeletedBinding() {
        return BindingBuilder
                .bind(assignmentDeletedQueue())
                .to(exchange())
                .with(ASSIGNMENT_DELETED);
    }

    @Bean
    public Binding submissionCreatedBinding() {
        return BindingBuilder
                .bind(submissionCreatedQueue())
                .to(exchange())
                .with(SUBMISSION_CREATED);
    }

    // Queue eventi in entrata

    @Bean
    public Queue courseDeletedQueue() {
        return QueueBuilder.durable(courseDeletedQueue).build();
    }

    @Bean
    public Binding courseDeletedBinding() {
        return BindingBuilder
                .bind(courseDeletedQueue())
                .to(exchange())
                .with(COURSE_DELETED_ROUTING_KEY);
    }

    @Bean
    public Queue assessmentCreatedQueue() {
        return QueueBuilder.durable(assessmentCreatedQueue).build();
    }

    @Bean
    public Binding assessmentCreatedBinding() {
        return BindingBuilder
                .bind(assessmentCreatedQueue())
                .to(exchange())
                .with(ASSESSMENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Queue studentUnenrolledQueue() {
        return QueueBuilder.durable(studentUnenrolledQueue).build();
    }

    @Bean
    public Binding studentUnenrolledBinding() {
        return BindingBuilder
                .bind(studentUnenrolledQueue())
                .to(exchange())
                .with(STUDENT_UNENROLLED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
