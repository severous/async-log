import javax.annotation.processing.Processor;
import org.example.async.log.client.annotation.AsyncLogProcessor;

module org.example.async.log.client{
    requires jdk.compiler;
    requires jdk.unsupported;

    exports org.example.async.log.client.annotation;
    provides Processor with AsyncLogProcessor;
}