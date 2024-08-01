import javax.annotation.processing.Processor;
import org.example.async.log.client.annotation.AsyncLogAnnotationProcessor;

module org.example.async.log.client{
    requires jdk.compiler;
    requires jdk.unsupported;

    exports org.example.async.log.client.annotation;
    provides Processor with AsyncLogAnnotationProcessor;
}